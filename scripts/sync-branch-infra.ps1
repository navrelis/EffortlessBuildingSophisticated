# Syncs the canonical per-branch CI/build/release infra from templates/branch/ into one or more version-branch
# worktrees (versions/<mc>/). Source of truth lives once, on main, under templates/branch/:
#   .github/workflows/build.yml
#   release.ps1
#   build-all.ps1
#
# This script only copies files and reports what changed — it never runs git add/commit/push in any worktree.
# Commit the result yourself, per branch, after reviewing the diff.
#
# It also makes sure every loader folder (any top-level folder with settings.gradle + gradlew, same discovery
# rule as build-all.ps1 / release.ps1 / .github/workflows/build.yml) in each target worktree has a
# "ci_gradle_jdk=<value>" line in its gradle.properties, appending one (using -GradleJdk, default 21) when it's
# missing. This is the JDK CI uses to RUN Gradle itself, independent of the compile toolchain.
#
# Usage (PowerShell 7, run from anywhere -- the template/worktree locations are resolved from this script's own
# path, i.e. relative to the main checkout root):
#   pwsh ./scripts/sync-branch-infra.ps1                          # sync every worktree under versions/
#   pwsh ./scripts/sync-branch-infra.ps1 -Mc 1.20.4               # sync only versions/1.20.4
#   pwsh ./scripts/sync-branch-infra.ps1 -Mc 1.20.4,1.21.1         # sync a specific set
#   pwsh ./scripts/sync-branch-infra.ps1 -Exclude 1.21.1,1.21.4    # sync every worktree except these
#   pwsh ./scripts/sync-branch-infra.ps1 -Check                    # report-only; exit non-zero if anything differs
#   pwsh ./scripts/sync-branch-infra.ps1 -Mc 1.20.4 -GradleJdk 17  # override the ci_gradle_jdk value to append
#
# -Mc and -Exclude are mutually exclusive as a *selection* mechanism: -Mc names an explicit set of worktrees to
# target; when -Mc is omitted, the target set is "every worktree under versions/" minus whatever -Exclude names.
#
# Idempotent: running it twice in a row with no intervening changes reports everything "unchanged" and makes no
# further edits (including the ci_gradle_jdk line, which is only appended when absent).

[CmdletBinding()]
param(
    [string[]]$Mc,
    [string[]]$Exclude = @(),
    [switch]$Check,
    [int]$GradleJdk = 21
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# scripts/sync-branch-infra.ps1 lives directly under the main checkout root's scripts/ folder.
$mainRoot = Split-Path -Parent $PSScriptRoot
$templateRoot = Join-Path $mainRoot 'templates/branch'
$versionsRoot = Join-Path $mainRoot 'versions'

if (-not (Test-Path $templateRoot)) {
    throw "Template root not found: $templateRoot"
}
if (-not (Test-Path $versionsRoot)) {
    throw "versions/ root not found: $versionsRoot (run scripts/setup-worktrees.ps1 first)"
}

# ---- The set of template files to sync, as paths relative to templateRoot / each worktree root ----------------
$templateFiles = Get-ChildItem -LiteralPath $templateRoot -Recurse -File |
    ForEach-Object { $_.FullName.Substring($templateRoot.Length + 1) -replace '\\', '/' } |
    Sort-Object

if (-not $templateFiles -or $templateFiles.Count -eq 0) {
    throw "No template files found under $templateRoot"
}

# ---- Discover candidate worktrees (any versions/<mc> directory that is actually a git worktree) ----------------
$allMc = Get-ChildItem -LiteralPath $versionsRoot -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName '.git') } |
    Sort-Object Name |
    ForEach-Object { $_.Name }

if ($Mc -and $Mc.Count -gt 0) {
    $targetMc = $Mc
    $missing = $targetMc | Where-Object { $_ -notin $allMc }
    if ($missing) {
        throw "Requested -Mc version(s) not found as worktrees under versions/: $($missing -join ', ')"
    }
} else {
    $targetMc = $allMc | Where-Object { $_ -notin $Exclude }
}

if (-not $targetMc -or $targetMc.Count -eq 0) {
    throw "No target worktrees resolved (allMc=$($allMc -join ', '); Exclude=$($Exclude -join ', '))"
}

Write-Host "Template root : $templateRoot"
Write-Host "Template files: $($templateFiles -join ', ')"
Write-Host "Target worktrees: $($targetMc -join ', ')"
Write-Host "Mode: $(if ($Check) { 'CHECK (report-only)' } else { 'SYNC (writes files)' })"
Write-Host ''

$anyDiff = $false
$rows = [System.Collections.Generic.List[object]]::new()

foreach ($mcVersion in $targetMc) {
    $worktreeRoot = Join-Path $versionsRoot $mcVersion

    foreach ($relPath in $templateFiles) {
        $srcPath = Join-Path $templateRoot $relPath
        $dstPath = Join-Path $worktreeRoot $relPath

        $srcContent = Get-Content -LiteralPath $srcPath -Raw
        $status = $null

        if (-not (Test-Path $dstPath)) {
            $status = 'added'
        } else {
            $dstContent = Get-Content -LiteralPath $dstPath -Raw
            $status = if ($dstContent -eq $srcContent) { 'unchanged' } else { 'changed' }
        }

        if ($status -ne 'unchanged') { $anyDiff = $true }

        if (-not $Check -and $status -ne 'unchanged') {
            $dstDir = Split-Path -Parent $dstPath
            if (-not (Test-Path $dstDir)) {
                New-Item -ItemType Directory -Path $dstDir -Force | Out-Null
            }
            Copy-Item -LiteralPath $srcPath -Destination $dstPath -Force
        }

        $rows.Add([pscustomobject]@{
            Mc     = $mcVersion
            File   = $relPath
            Status = $status
        })
    }

    # ---- ci_gradle_jdk in every loader folder's gradle.properties ---------------------------------------------
    $loaders = Get-ChildItem -LiteralPath $worktreeRoot -Directory -ErrorAction SilentlyContinue |
        Where-Object {
            (Test-Path (Join-Path $_.FullName 'settings.gradle')) -and (Test-Path (Join-Path $_.FullName 'gradlew'))
        } |
        Sort-Object Name |
        ForEach-Object { $_.Name }

    foreach ($loader in $loaders) {
        $propsPath = Join-Path (Join-Path $worktreeRoot $loader) 'gradle.properties'
        $hasLine = $false
        if (Test-Path $propsPath) {
            $hasLine = (Select-String -LiteralPath $propsPath -Pattern '^ci_gradle_jdk=' -Quiet) -eq $true
        }

        if ($hasLine) {
            $rows.Add([pscustomobject]@{ Mc = $mcVersion; File = "$loader/gradle.properties (ci_gradle_jdk)"; Status = 'unchanged' })
            continue
        }

        $anyDiff = $true
        if ($Check) {
            $rows.Add([pscustomobject]@{ Mc = $mcVersion; File = "$loader/gradle.properties (ci_gradle_jdk)"; Status = 'missing' })
            continue
        }

        if (-not (Test-Path $propsPath)) {
            New-Item -ItemType File -Path $propsPath -Force | Out-Null
        }
        Add-Content -LiteralPath $propsPath -Value "ci_gradle_jdk=$GradleJdk" -Encoding utf8NoBOM
        $rows.Add([pscustomobject]@{ Mc = $mcVersion; File = "$loader/gradle.properties (ci_gradle_jdk)"; Status = "added ($GradleJdk)" })
    }
}

Write-Host ''
$rows | Format-Table -AutoSize | Out-String | Write-Host

if ($Check) {
    if ($anyDiff) {
        Write-Host 'sync-branch-infra.ps1 -Check: differences found (see table above).'
        exit 1
    }
    Write-Host 'sync-branch-infra.ps1 -Check: every target worktree matches the templates.'
    exit 0
}

Write-Host 'sync-branch-infra.ps1: sync complete. Nothing was committed -- review and commit per branch.'
exit 0
