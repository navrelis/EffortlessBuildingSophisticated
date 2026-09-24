#requires -Version 7.0
<#
.SYNOPSIS
    Runs build-all.ps1 inside every versions/<mc> worktree, sequentially, and reports a
    pass/fail table.

.DESCRIPTION
    Iterates the directories under versions/ (each one a git worktree of an mc/<version> branch,
    see scripts/setup-worktrees.ps1), and runs that branch's own build-all.ps1 inside it. Stops
    nothing early - every branch is attempted even if an earlier one fails - and reports a
    pass/fail table at the end. Exits non-zero if any branch's build failed.

.PARAMETER Mc
    Restrict the run to one Minecraft version (must match a versions/<mc> folder name exactly).

.PARAMETER WhatIf
    Standard PowerShell dry run: lists which branches would be built without actually building
    them (no gradlew invocation happens).

.EXAMPLE
    pwsh scripts/build-all-versions.ps1
    Builds every versions/<mc> branch in turn.

.EXAMPLE
    pwsh scripts/build-all-versions.ps1 -Mc 1.21.1 -WhatIf
    Dry run: shows that versions/1.21.1 would be built, without building it.
#>
[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [string]$Mc
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$versionsDir = Join-Path $repoRoot 'versions'

if (-not (Test-Path $versionsDir)) {
    Write-Error "versions/ not found at '$versionsDir'. Run scripts/setup-worktrees.ps1 first."
    exit 1
}

$branches = Get-ChildItem -Path $versionsDir -Directory -ErrorAction SilentlyContinue | Sort-Object Name

if ($Mc) {
    $branches = $branches | Where-Object { $_.Name -eq $Mc }
    if (-not $branches) {
        Write-Error "No versions/$Mc worktree found."
        exit 1
    }
}

if (-not $branches) {
    Write-Warning "No version worktrees found under versions/. Nothing to build."
    exit 0
}

$results = @()

foreach ($branch in $branches) {
    $buildScript = Join-Path $branch.FullName 'build-all.ps1'

    if (-not (Test-Path $buildScript)) {
        Write-Warning "Skipping $($branch.Name): no build-all.ps1 found at versions/$($branch.Name)/build-all.ps1."
        $results += [pscustomobject]@{ Mc = $branch.Name; Status = 'skipped (no build-all.ps1)' }
        continue
    }

    if (-not $PSCmdlet.ShouldProcess("versions/$($branch.Name)", 'Run build-all.ps1')) {
        $results += [pscustomobject]@{ Mc = $branch.Name; Status = 'skipped (WhatIf)' }
        continue
    }

    Write-Host "==> Building versions/$($branch.Name) ..."
    Push-Location $branch.FullName
    try {
        & $buildScript
        if ($LASTEXITCODE -ne 0) {
            $results += [pscustomobject]@{ Mc = $branch.Name; Status = "FAILED (exit $LASTEXITCODE)" }
        } else {
            $results += [pscustomobject]@{ Mc = $branch.Name; Status = 'passed' }
        }
    } catch {
        $results += [pscustomobject]@{ Mc = $branch.Name; Status = "FAILED ($($_.Exception.Message))" }
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "Build summary:"
$results | Format-Table -Property Mc, Status -AutoSize | Out-String -Width 4096 | Write-Host

$failed = $results | Where-Object { $_.Status -like 'FAILED*' }
if ($failed.Count -gt 0) {
    Write-Error "$($failed.Count) branch build(s) failed."
    exit 1
}

exit 0
