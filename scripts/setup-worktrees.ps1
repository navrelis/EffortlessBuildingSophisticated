#requires -Version 7.0
<#
.SYNOPSIS
    Fetches origin and creates a git worktree under versions/<mc> for every origin/mc/<version>
    branch that doesn't already have one.

.DESCRIPTION
    For each remote branch matching origin/mc/<version>, ensures a local branch mc/<version>
    tracking it exists and is checked out as a worktree at versions/<mc>. Branches (and their
    worktrees) that already exist are left untouched - this script never modifies or removes an
    existing worktree's content. Safe to re-run any time (idempotent): with nothing new on the
    remote, it creates no new worktrees.

    A local-only branch (e.g. a new version branch that hasn't been pushed yet) is never touched
    by the default run, since this script only looks at origin/mc/* - its worktree is left exactly
    as it is.

.PARAMETER Prune
    Also removes worktrees whose mc/<version> branch is no longer on origin/mc/*. A worktree is
    only removed if `git status --porcelain` reports it clean (no uncommitted changes, no
    untracked files) - anything with local changes is left in place and reported instead of
    removed, however old the branch.

.EXAMPLE
    pwsh scripts/setup-worktrees.ps1
    Creates versions/<mc> for every origin/mc/* branch that doesn't already have a worktree.

.EXAMPLE
    pwsh scripts/setup-worktrees.ps1 -Prune
    Same, then removes any clean worktree whose branch no longer exists on origin.
#>
[CmdletBinding()]
param(
    [switch]$Prune
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

Push-Location $repoRoot
try {
    Write-Host "Fetching origin..."
    git fetch origin --prune 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "git fetch origin failed (exit code $LASTEXITCODE)."
    }

    # Every origin/mc/<version> branch, e.g. "origin/mc/1.21.1".
    $remoteRefs = git for-each-ref --format='%(refname:short)' 'refs/remotes/origin/mc/*'
    if (-not $remoteRefs) {
        Write-Warning "No origin/mc/* branches found on the remote."
        $remoteRefs = @()
    }

    $versionsDir = Join-Path $repoRoot 'versions'
    if (-not (Test-Path $versionsDir)) {
        New-Item -ItemType Directory -Path $versionsDir | Out-Null
    }

    # Parse `git worktree list --porcelain` into objects with Path/Branch.
    function Get-Worktrees {
        $porcelain = git worktree list --porcelain
        $worktrees = New-Object System.Collections.Generic.List[object]
        $current = $null
        foreach ($line in $porcelain) {
            if ($line -match '^worktree (.+)$') {
                if ($current) { $worktrees.Add($current) }
                $current = [pscustomobject]@{ Path = $matches[1]; Branch = $null }
            } elseif ($line -match '^branch refs/heads/(.+)$') {
                if ($current) { $current.Branch = $matches[1] }
            }
        }
        if ($current) { $worktrees.Add($current) }
        return $worktrees
    }

    $worktrees = Get-Worktrees
    $results = @()

    foreach ($remoteRef in $remoteRefs) {
        $branchName = $remoteRef -replace '^origin/', ''   # "mc/1.21.1"
        $mc = $branchName -replace '^mc/', ''              # "1.21.1"
        $worktreePath = Join-Path $versionsDir $mc

        $existing = $worktrees | Where-Object { $_.Branch -eq $branchName }
        if ($existing) {
            $lastCommit = (git -C $existing.Path log -1 --format='%h %s' 2>$null)
            $results += [pscustomobject]@{ Branch = $branchName; Path = $existing.Path; Status = 'exists'; LastCommit = $lastCommit }
            continue
        }

        if (Test-Path $worktreePath) {
            # Something is already at this path but git doesn't know it as a worktree for this
            # branch - never touch it, it might be uncommitted local work in progress.
            $results += [pscustomobject]@{ Branch = $branchName; Path = $worktreePath; Status = 'SKIPPED (path exists, not registered as this branch''s worktree)'; LastCommit = '' }
            continue
        }

        Write-Host "Creating worktree for $branchName at versions/$mc ..."
        git show-ref --verify --quiet "refs/heads/$branchName"
        $localBranchExists = ($LASTEXITCODE -eq 0)

        if ($localBranchExists) {
            git worktree add $worktreePath $branchName | Out-Null
        } else {
            git worktree add --track -b $branchName $worktreePath $remoteRef | Out-Null
        }
        if ($LASTEXITCODE -ne 0) {
            $results += [pscustomobject]@{ Branch = $branchName; Path = $worktreePath; Status = 'FAILED (git worktree add)'; LastCommit = '' }
            continue
        }

        $lastCommit = (git -C $worktreePath log -1 --format='%h %s' 2>$null)
        $results += [pscustomobject]@{ Branch = $branchName; Path = $worktreePath; Status = 'created'; LastCommit = $lastCommit }
        $worktrees = Get-Worktrees
    }

    Write-Host ""
    Write-Host "versions/<mc> worktree status (origin/mc/* branches):"
    if ($results.Count -gt 0) {
        $results | Format-Table -Property Branch, Path, Status, LastCommit -AutoSize | Out-String -Width 4096 | Write-Host
    } else {
        Write-Host "  (none)"
    }

    if ($Prune) {
        Write-Host "Pruning worktrees whose mc/* branch used to track origin but was deleted there..."
        # Only ever prune a branch that WAS tracking an origin/mc/<version> branch and that branch
        # is now gone (git marks this "[gone]" in %(upstream:track)) - never a branch that simply
        # has no upstream configured yet (e.g. a new branch nobody pushed yet). Comparing against
        # "is it currently in origin/mc/*" alone would be wrong: it can't tell "never pushed" apart
        # from "was pushed, now deleted upstream", and would delete an un-pushed branch's worktree.
        $goneBranches = git for-each-ref --format='%(refname:short)|%(upstream:track)' 'refs/heads/mc/*' |
            Where-Object { $_ -match '\[gone\]' } |
            ForEach-Object { ($_ -split '\|')[0] }

        $worktrees = Get-Worktrees
        $pruned = $false
        foreach ($wt in $worktrees) {
            if (-not $wt.Branch) { continue }
            if ($wt.Branch -notlike 'mc/*') { continue }
            if ($goneBranches -notcontains $wt.Branch) { continue }

            $status = git -C $wt.Path status --porcelain 2>$null
            if ($status) {
                Write-Warning "Not pruning $($wt.Path) (branch $($wt.Branch)): it has uncommitted changes."
                continue
            }

            Write-Host "Removing worktree $($wt.Path) (branch $($wt.Branch) is no longer on origin)..."
            git worktree remove $wt.Path
            $pruned = $true
        }
        if (-not $pruned) {
            Write-Host "  Nothing to prune."
        }
    }
} finally {
    Pop-Location
}
