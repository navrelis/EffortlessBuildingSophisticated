#requires -Version 7.0
<#
.SYNOPSIS
    Offline regression tests for scripts/lib/TestAllVersions.Common.ps1's log-pattern helpers - no Gradle
    invocation, no game process, just synthetic log files under a temp directory. Run directly:

        pwsh scripts/test-all-versions.offline-tests.ps1

    Exits 0 if every test passes, 1 otherwise (each failing assertion is printed as it happens).

.DESCRIPTION
    Covers the stale-log-line race described in Wait-ForLogPattern's own comment (docs/TESTING.md "Open
    points"): run/logs/latest.log from a PREVIOUS server/client run can still be sitting there, unrotated, when
    the NEXT run's wait starts polling, and that stale file can already contain a line matching the very pattern
    being waited for (e.g. a stale "logged in with entity id" for the client stage, a stale "Done (" for the
    server stage) well before the new game process has logged anything at all. Get-LogBaselineLength +
    Wait-ForLogPattern's -BaselineLength close that race by only ever matching bytes written after the new
    process's own launch. These tests prove that against synthetic fixtures, without launching Minecraft.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

. (Join-Path $PSScriptRoot 'lib/TestAllVersions.Common.ps1')

$script:TestsRun = 0
$script:TestsFailed = 0

function Assert-True {
    param([Parameter(Mandatory)][bool]$Condition, [Parameter(Mandatory)][string]$Message)
    $script:TestsRun++
    if ($Condition) {
        Write-Host "  PASS $Message" -ForegroundColor Green
    } else {
        $script:TestsFailed++
        Write-Host "  FAIL $Message" -ForegroundColor Red
    }
}

function New-TempLogDir {
    $dir = Join-Path ([System.IO.Path]::GetTempPath()) "sb-offline-test-$([guid]::NewGuid().ToString('N'))"
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    return $dir
}

function Remove-TestJob {
    param($Job)
    if (-not $Job) { return }
    Stop-Job -Job $Job -ErrorAction SilentlyContinue | Out-Null
    Remove-Job -Job $Job -Force -ErrorAction SilentlyContinue | Out-Null
}

Write-Host 'Get-LogBaselineLength / Wait-ForLogPattern (stale-log-line race fix)' -ForegroundColor Cyan

# ---------------------------------------------------------------------------------------------------------------
# Test 1: no file yet (the common case: run/logs/latest.log doesn't exist until the game starts logging) ->
# baseline 0, and content written after the wait starts is still picked up normally.
# ---------------------------------------------------------------------------------------------------------------
$dir1 = New-TempLogDir
$job1 = $null
try {
    $log1 = Join-Path $dir1 'latest.log'
    $baseline1 = Get-LogBaselineLength -Path $log1
    Assert-True -Condition ($baseline1 -eq 0) -Message "Get-LogBaselineLength returns 0 for a file that doesn't exist yet"

    $job1 = Start-Job -ScriptBlock {
        param($Path)
        Start-Sleep -Seconds 1
        Set-Content -LiteralPath $Path -Value @('[boot] starting up', '[client] logged in with entity id 42 at (0,64,0)') -Encoding utf8
    } -ArgumentList $log1

    $line1 = Wait-ForLogPattern -Path $log1 -Pattern 'logged in with entity id' -TimeoutMinutes 1 -PollSeconds 1 -BaselineLength $baseline1
    Assert-True -Condition ($null -ne $line1 -and $line1 -match '42') -Message 'matches a fresh join line written after the wait started (no pre-existing file)'
} finally {
    Remove-TestJob -Job $job1
    Remove-Item -LiteralPath $dir1 -Recurse -Force -ErrorAction SilentlyContinue
}

# ---------------------------------------------------------------------------------------------------------------
# Test 2: THE BUG, append case. A stale latest.log from a previous run already contains a line matching the
# pattern before the new process even launches. Without the baseline fix, Wait-ForLogPattern scans from byte 0
# every poll and would return that stale line on its very first poll - before the (simulated) new process has
# appended anything. With the fix it must skip the stale line and only match once fresh content is appended.
# ---------------------------------------------------------------------------------------------------------------
$dir2 = New-TempLogDir
$job2 = $null
try {
    $log2 = Join-Path $dir2 'latest.log'
    Set-Content -LiteralPath $log2 -Value @(
        '[boot] previous run starting up',
        '[client] logged in with entity id 7 at (100,64,100)',
        '[client] previous run tail'
    ) -Encoding utf8

    $baseline2 = Get-LogBaselineLength -Path $log2
    Assert-True -Condition ($baseline2 -gt 0) -Message "Get-LogBaselineLength captures the stale file's pre-launch length"

    # Fixture sanity check: an offset-0 scan (the old, buggy behavior) really would match this immediately -
    # otherwise this test would trivially pass for the wrong reason.
    $staleContent = Get-Content -LiteralPath $log2 -Raw
    $staleWouldMatch = [bool](($staleContent -split "`r?`n") | Where-Object { $_ -match 'logged in with entity id' })
    Assert-True -Condition $staleWouldMatch -Message 'fixture sanity check: the stale content does contain a matching line (the race is real)'

    $job2 = Start-Job -ScriptBlock {
        param($Path)
        Start-Sleep -Seconds 1
        Add-Content -LiteralPath $Path -Value '[client] logged in with entity id 999 at (200,64,200)' -Encoding utf8
    } -ArgumentList $log2

    $line2 = Wait-ForLogPattern -Path $log2 -Pattern 'logged in with entity id' -TimeoutMinutes 1 -PollSeconds 1 -BaselineLength $baseline2
    Assert-True -Condition ($null -ne $line2 -and $line2 -match '999') -Message 'matches the fresh (post-launch) join line, not the stale one'
    Assert-True -Condition ($null -eq $line2 -or $line2 -notmatch '\b7\b') -Message 'never returns the stale pre-launch line'
} finally {
    Remove-TestJob -Job $job2
    Remove-Item -LiteralPath $dir2 -Recurse -Force -ErrorAction SilentlyContinue
}

# ---------------------------------------------------------------------------------------------------------------
# Test 3: rotation case. log4j2 actually rotating latest.log away replaces it with a fresh, SHORTER file. The
# offset must reset to 0 the first time the file is observed shorter than its pre-launch baseline, so content in
# the new (small) file is still scanned from its own start.
# ---------------------------------------------------------------------------------------------------------------
$dir3 = New-TempLogDir
$job3 = $null
try {
    $log3 = Join-Path $dir3 'latest.log'
    Set-Content -LiteralPath $log3 -Value @(
        '[boot] previous (longer) run starting up',
        '[server] Done (1.234s)! For help, type "help"',
        '[server] previous run tail line one',
        '[server] previous run tail line two',
        '[server] previous run tail line three'
    ) -Encoding utf8

    $baseline3 = Get-LogBaselineLength -Path $log3
    Assert-True -Condition ($baseline3 -gt 0) -Message 'Get-LogBaselineLength captures the pre-rotation stale length'

    $job3 = Start-Job -ScriptBlock {
        param($Path)
        Start-Sleep -Seconds 1
        # Simulate log4j2's rotation: the old file is replaced by a fresh, much smaller one.
        Set-Content -LiteralPath $Path -Value '[server] Done (0.456s)! For help, type "help"' -Encoding utf8
    } -ArgumentList $log3

    $line3 = Wait-ForLogPattern -Path $log3 -Pattern 'Done \(' -TimeoutMinutes 1 -PollSeconds 1 -BaselineLength $baseline3
    Assert-True -Condition ($null -ne $line3 -and $line3 -match '0\.456s') -Message 'matches the post-rotation line once the file is observed shorter (offset reset to 0)'
} finally {
    Remove-TestJob -Job $job3
    Remove-Item -LiteralPath $dir3 -Recurse -Force -ErrorAction SilentlyContinue
}

# ---------------------------------------------------------------------------------------------------------------
# Test 4: negative control. Stale-only content, nothing ever written after launch - the wait must NOT return a
# match promptly (it should still be polling after a few real seconds), proving the stale line is never matched
# rather than happening to be beaten by a same-tick append in tests 2/3 above.
# ---------------------------------------------------------------------------------------------------------------
$dir4 = New-TempLogDir
$job4 = $null
try {
    $log4 = Join-Path $dir4 'latest.log'
    Set-Content -LiteralPath $log4 -Value '[server] Done (1.234s)! For help, type "help"' -Encoding utf8
    $baseline4 = Get-LogBaselineLength -Path $log4

    $job4 = Start-Job -ScriptBlock {
        param($LibPath, $Path, $Baseline)
        . $LibPath
        Wait-ForLogPattern -Path $Path -Pattern 'Done \(' -TimeoutMinutes 1 -PollSeconds 1 -BaselineLength $Baseline
    } -ArgumentList (Join-Path $PSScriptRoot 'lib/TestAllVersions.Common.ps1'), $log4, $baseline4

    $completed = Wait-Job -Job $job4 -Timeout 4
    Assert-True -Condition ($null -eq $completed) -Message 'still polling after 4s on stale-only content (never matched the stale line immediately)'
} finally {
    Remove-TestJob -Job $job4
    Remove-Item -LiteralPath $dir4 -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ''
if ($script:TestsFailed -gt 0) {
    Write-Host "$($script:TestsFailed) of $($script:TestsRun) offline test(s) FAILED" -ForegroundColor Red
    exit 1
}
Write-Host "All $($script:TestsRun) offline test(s) passed" -ForegroundColor Green
exit 0
