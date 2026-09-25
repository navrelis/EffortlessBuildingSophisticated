#requires -Version 7.0
<#
.SYNOPSIS
    Offline regression tests for scripts/lib/TestAllVersions.Common.ps1's log-pattern helpers, the game window
    lock (lib/GameWindowLock.ps1) and the report merge (lib/TestAllVersions.Report.ps1, -MergeReports) - no Gradle
    invocation, no game process, just synthetic files under a temp directory. Run directly:

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
. (Join-Path $PSScriptRoot 'lib/GameWindowLock.ps1')
. (Join-Path $PSScriptRoot 'lib/TestAllVersions.Report.ps1')

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

# ===============================================================================================================
# Game window lock (lib/GameWindowLock.ps1): the machine-wide one-window-at-a-time lock shared with the agents.
# Short poll/stale/keep-alive values; the real defaults are the port-brief rules (30 s poll, 20 min stale).
# ===============================================================================================================
Write-Host ''
Write-Host 'Enter-GameWindowLock / Exit-GameWindowLock' -ForegroundColor Cyan

# Test 5: free lock -> taken at once with "<owner> <time>" content, released by Exit.
$dir5 = New-TempLogDir
try {
    $lock5 = Join-Path $dir5 'game-window.lock'
    $handle5 = Enter-GameWindowLock -LockPath $lock5 -Owner 'offline-test-5' -TimeoutMinutes 0.1 -PollSeconds 1
    Assert-True -Condition ($null -ne $handle5 -and (Test-Path $lock5)) -Message 'a free lock is taken at once'
    Assert-True -Condition ((Get-Content -LiteralPath $lock5 -Raw) -match '^offline-test-5 \d{4}-') -Message 'lock content is "<owner> <ISO time>"'
    Exit-GameWindowLock -Handle $handle5
    Assert-True -Condition (-not (Test-Path $lock5)) -Message 'Exit-GameWindowLock deletes its own lock'
    Exit-GameWindowLock -Handle $handle5
    Assert-True -Condition (-not (Test-Path $lock5)) -Message 'a second Exit-GameWindowLock on the same handle is harmless'
} finally {
    Remove-Item -LiteralPath $dir5 -Recurse -Force -ErrorAction SilentlyContinue
}

# Test 6: stale lock (older than the stale limit) -> removed and taken over without waiting.
$dir6 = New-TempLogDir
try {
    $lock6 = Join-Path $dir6 'game-window.lock'
    Set-Content -LiteralPath $lock6 -Value 'crashed-agent 2026-01-01T00:00:00' -NoNewline
    (Get-Item -LiteralPath $lock6).LastWriteTime = (Get-Date).AddMinutes(-25)
    $sw6 = [System.Diagnostics.Stopwatch]::StartNew()
    $handle6 = Enter-GameWindowLock -LockPath $lock6 -Owner 'offline-test-6' -TimeoutMinutes 0.1 -PollSeconds 1 -StaleMinutes 20
    $sw6.Stop()
    Assert-True -Condition ($null -ne $handle6 -and (Get-Content -LiteralPath $lock6 -Raw) -match '^offline-test-6 ') -Message 'a 25 min old lock is stale: removed and taken over'
    Assert-True -Condition ($sw6.Elapsed.TotalSeconds -lt 3) -Message 'taking over a stale lock does not wait'
    Exit-GameWindowLock -Handle $handle6
} finally {
    Remove-Item -LiteralPath $dir6 -Recurse -Force -ErrorAction SilentlyContinue
}

# Test 7: a fresh lock held by someone else -> waits, times out with $null, leaves the other holder's lock alone.
$dir7 = New-TempLogDir
try {
    $lock7 = Join-Path $dir7 'game-window.lock'
    Set-Content -LiteralPath $lock7 -Value 'other-agent 2026-09-25T00:00:00' -NoNewline
    $sw7 = [System.Diagnostics.Stopwatch]::StartNew()
    $handle7 = Enter-GameWindowLock -LockPath $lock7 -Owner 'offline-test-7' -TimeoutMinutes 0.05 -PollSeconds 1 -StaleMinutes 20
    $sw7.Stop()
    Assert-True -Condition ($null -eq $handle7) -Message 'a held fresh lock is not taken (timeout returns $null)'
    Assert-True -Condition ($sw7.Elapsed.TotalSeconds -ge 2.5) -Message "it waited until the timeout ($([math]::Round($sw7.Elapsed.TotalSeconds, 1)) s)"
    Assert-True -Condition ((Get-Content -LiteralPath $lock7 -Raw) -eq 'other-agent 2026-09-25T00:00:00') -Message "the other holder's lock is untouched"
} finally {
    Remove-Item -LiteralPath $dir7 -Recurse -Force -ErrorAction SilentlyContinue
}

# Test 8: a held lock released during the wait -> taken as soon as it is gone.
$dir8 = New-TempLogDir
$job8 = $null
try {
    $lock8 = Join-Path $dir8 'game-window.lock'
    Set-Content -LiteralPath $lock8 -Value 'other-agent 2026-09-25T00:00:00' -NoNewline
    $job8 = Start-Job -ScriptBlock { param($Path) Start-Sleep -Seconds 2; Remove-Item -LiteralPath $Path -Force } -ArgumentList $lock8
    $sw8 = [System.Diagnostics.Stopwatch]::StartNew()
    $handle8 = Enter-GameWindowLock -LockPath $lock8 -Owner 'offline-test-8' -TimeoutMinutes 0.5 -PollSeconds 1 -StaleMinutes 20
    $sw8.Stop()
    Assert-True -Condition ($null -ne $handle8 -and (Get-Content -LiteralPath $lock8 -Raw) -match '^offline-test-8 ') -Message 'the lock is taken once the other holder released it'
    Assert-True -Condition ($sw8.Elapsed.TotalSeconds -ge 1.5 -and $sw8.Elapsed.TotalSeconds -lt 15) -Message "it waited for the release ($([math]::Round($sw8.Elapsed.TotalSeconds, 1)) s)"
    Exit-GameWindowLock -Handle $handle8
} finally {
    Remove-TestJob -Job $job8
    Remove-Item -LiteralPath $dir8 -Recurse -Force -ErrorAction SilentlyContinue
}

# Test 9: released in finally when the stage throws; Exit never deletes a lock that is no longer ours.
$dir9 = New-TempLogDir
try {
    $lock9 = Join-Path $dir9 'game-window.lock'
    $threw = $false
    try {
        $handle9 = Enter-GameWindowLock -LockPath $lock9 -Owner 'offline-test-9' -TimeoutMinutes 0.1 -PollSeconds 1
        try {
            throw 'simulated client stage failure'
        } finally {
            Exit-GameWindowLock -Handle $handle9
        }
    } catch {
        $threw = $true
    }
    Assert-True -Condition ($threw -and -not (Test-Path $lock9)) -Message 'a stage that throws still releases the lock in its finally block'

    $handle9b = Enter-GameWindowLock -LockPath $lock9 -Owner 'offline-test-9b' -TimeoutMinutes 0.1 -PollSeconds 1
    Set-Content -LiteralPath $lock9 -Value 'someone-else 2026-09-25T00:00:00' -NoNewline
    Exit-GameWindowLock -Handle $handle9b
    Assert-True -Condition ((Test-Path $lock9) -and (Get-Content -LiteralPath $lock9 -Raw) -match '^someone-else') -Message 'Exit-GameWindowLock leaves a lock with foreign content alone'
} finally {
    Remove-Item -LiteralPath $dir9 -Recurse -Force -ErrorAction SilentlyContinue
}

# Test 10: keep-alive - a held lock is refreshed, so a long client stage never looks stale to others.
$dir10 = New-TempLogDir
try {
    $lock10 = Join-Path $dir10 'game-window.lock'
    $handle10 = Enter-GameWindowLock -LockPath $lock10 -Owner 'offline-test-10' -TimeoutMinutes 0.1 -PollSeconds 1 -KeepAliveSeconds 1
    (Get-Item -LiteralPath $lock10).LastWriteTime = (Get-Date).AddMinutes(-19)
    Start-Sleep -Seconds 4
    $age10 = ((Get-Date) - (Get-Item -LiteralPath $lock10).LastWriteTime).TotalSeconds
    Assert-True -Condition ($age10 -lt 10) -Message "the keep-alive job refreshes the held lock (age $([math]::Round($age10, 1)) s)"
    Exit-GameWindowLock -Handle $handle10
    Assert-True -Condition (-not (Test-Path $lock10)) -Message 'released after the keep-alive stopped'
} finally {
    Remove-Item -LiteralPath $dir10 -Recurse -Force -ErrorAction SilentlyContinue
}

# ===============================================================================================================
# Report merge (lib/TestAllVersions.Report.ps1 and the -MergeReports mode of test-all-versions.ps1)
# ===============================================================================================================
Write-Host ''
Write-Host 'Merge-TestReports / -MergeReports' -ForegroundColor Cyan

function New-FixtureReport {
    param([string]$Dir, [datetime]$GeneratedAt, [object[]]$Rows, [object[]]$Sb)
    $report = New-TestReport -ReportDir $Dir -VersionsDir 'versions' -Parameters ([ordered]@{ stages = @('build') }) `
        -SbAvailability $Sb -SbGametestCoverage @() -Results $Rows
    $report.generatedAt = $GeneratedAt.ToString('o')
    Write-TestReportFiles -Report $report -ReportDir $Dir | Out-Null
}

$dir11 = New-TempLogDir
try {
    $older = Join-Path $dir11 'a'
    $newer = Join-Path $dir11 'b'
    New-FixtureReport -Dir $older -GeneratedAt (Get-Date).AddHours(-2) -Rows @(
        (New-StageResult -Mc '1.21.4' -Loader 'fabric' -Stage 'build' -Result 'fail' -Detail 'first try'),
        (New-StageResult -Mc '1.21.10' -Loader 'neoforge' -Stage 'smoke (runSmokeServer)' -Result 'pass' -Detail '9 checks'),
        (New-StageResult -Mc '1.20.4' -Loader 'fabric' -Stage 'smoke (runSmokeServer)' -Result 'pass' -Detail 'x | y')
    ) -Sb @([pscustomobject]@{ mc = '1.21.4'; loader = 'fabric'; sbAvailable = $false })
    New-FixtureReport -Dir $newer -GeneratedAt (Get-Date).AddHours(-1) -Rows @(
        (New-StageResult -Mc '1.21.4' -Loader 'fabric' -Stage 'build' -Result 'pass' -Detail 're-run'),
        (New-StageResult -Mc '1.20.4' -Loader 'fabric' -Stage 'build' -Result 'pass' -Detail '77 tests')
    ) -Sb @([pscustomobject]@{ mc = '1.21.4'; loader = 'fabric'; sbAvailable = $false },
            [pscustomobject]@{ mc = '1.20.4'; loader = 'fabric'; sbAvailable = $true })

    # Newest first on purpose: the merge orders the reports by generatedAt, not by argument order
    $merged = Merge-TestReports -ReportPaths @($newer, $older) -ReportDir (Join-Path $dir11 'merged')
    $rows11 = @($merged.results)
    $keys11 = @($rows11 | ForEach-Object { "$($_.mc)/$($_.loader)/$($_.stage)" })
    Assert-True -Condition ($rows11.Count -eq 4) -Message "4 rows after merging 3 + 2 with one duplicate key (got $($rows11.Count))"
    $rerun = @($rows11 | Where-Object { $_.mc -eq '1.21.4' -and $_.stage -eq 'build' })
    Assert-True -Condition ($rerun.Count -eq 1 -and $rerun[0].result -eq 'pass' -and $rerun[0].source -like '*b') -Message 'the newer report wins for a duplicate (version, loader, stage)'
    Assert-True -Condition (($keys11 -join ',') -eq '1.20.4/fabric/build,1.20.4/fabric/smoke (runSmokeServer),1.21.4/fabric/build,1.21.10/neoforge/smoke (runSmokeServer)') -Message "rows sorted by version (1.21.10 after 1.21.4), loader, stage: $($keys11 -join ', ')"
    Assert-True -Condition (@($merged.sbAvailability).Count -eq 2) -Message 'SB availability de-duplicated per (version, loader)'
    Assert-True -Condition ($merged.summary.pass -eq 4 -and $merged.summary.fail -eq 0 -and $merged.summary.total -eq 4) -Message 'summary recomputed from the merged rows'
    Assert-True -Condition (@($merged.mergedFrom).Count -eq 2) -Message 'both source report directories listed in mergedFrom'

    # A newer -WhatIf report only fills gaps, it never replaces a real result
    $dryRun = Join-Path $dir11 'dry'
    $dryReport = New-TestReport -ReportDir $dryRun -VersionsDir 'versions' -Parameters ([ordered]@{ whatIf = $true }) `
        -SbAvailability @() -SbGametestCoverage @() -Results @(
            (New-StageResult -Mc '1.21.4' -Loader 'fabric' -Stage 'build' -Result 'n/a' -Detail 'skipped (-WhatIf)'),
            (New-StageResult -Mc '1.21.5' -Loader 'fabric' -Stage 'build' -Result 'n/a' -Detail 'skipped (-WhatIf)'))
    Write-TestReportFiles -Report $dryReport -ReportDir $dryRun | Out-Null
    $mergedDry = Merge-TestReports -ReportPaths @($older, $newer, $dryRun) -ReportDir (Join-Path $dir11 'merged-dry')
    $build1214 = @($mergedDry.results | Where-Object { $_.mc -eq '1.21.4' -and $_.stage -eq 'build' })
    Assert-True -Condition ($build1214.Count -eq 1 -and $build1214[0].result -eq 'pass') -Message 'a newer -WhatIf row does not replace a real result'
    Assert-True -Condition (@($mergedDry.results | Where-Object { $_.mc -eq '1.21.5' }).Count -eq 1) -Message 'a -WhatIf row fills a key no real run has'

    # End to end through the script's -MergeReports mode (no Gradle, no game)
    $out11 = Join-Path $dir11 'final'
    $scriptPath = Join-Path $PSScriptRoot 'test-all-versions.ps1'
    & pwsh -NoProfile -File $scriptPath -MergeReports "$older,$newer" -ReportDir $out11 *> $null
    $exit11 = $LASTEXITCODE
    Assert-True -Condition ($exit11 -eq 0) -Message "test-all-versions.ps1 -MergeReports exits 0 without a failed row (exit $exit11)"
    $md11Path = Join-Path $out11 'report.md'
    $md11 = if (Test-Path $md11Path) { Get-Content -LiteralPath $md11Path -Raw } else { '' }
    Assert-True -Condition ($md11 -match 'Merged from:' -and $md11 -match '4 pass, 0 fail, 0 warn, 0 n/a \(of 4\)') -Message 'report.md written with the sources and the merged summary'
    Assert-True -Condition ($md11.Contains('x \| y')) -Message 'a "|" inside a detail is escaped in the markdown table'
    $json11Path = Join-Path $out11 'report.json'
    $json11 = if (Test-Path $json11Path) { Get-Content -LiteralPath $json11Path -Raw | ConvertFrom-Json } else { $null }
    Assert-True -Condition ($null -ne $json11 -and @($json11.results).Count -eq 4) -Message 'report.json written with the 4 merged rows'

    # A failed row makes the merge exit 1 (as a normal run with a failed stage does)
    $failing = Join-Path $dir11 'c'
    New-FixtureReport -Dir $failing -GeneratedAt (Get-Date) -Rows @(
        (New-StageResult -Mc '1.19.2' -Loader 'forge' -Stage 'build' -Result 'fail' -Detail 'broken')
    ) -Sb @()
    & pwsh -NoProfile -File $scriptPath -MergeReports "$older,$newer,$failing" -ReportDir (Join-Path $dir11 'final2') *> $null
    Assert-True -Condition ($LASTEXITCODE -eq 1) -Message "-MergeReports exits 1 when a merged row failed (exit $LASTEXITCODE)"
} finally {
    Remove-Item -LiteralPath $dir11 -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ''
Write-Host 'Get-JUnitTestNames / Get-JUnitSummary (JUnit XML shapes)' -ForegroundColor Cyan

# ---------------------------------------------------------------------------------------------------------------
# Test 12: the server test runner of the 1.16.x branches (no GameTest API) writes <testcase name time/> without a
# classname and a <testsuite> with only tests + failures; Gradle's writer has every attribute. Both parse under
# Set-StrictMode (dot access to a missing attribute used to throw PropertyNotFoundException).
# ---------------------------------------------------------------------------------------------------------------
$dir12 = New-TempLogDir
try {
    $legacy = Join-Path $dir12 'junit.xml'
    Set-Content -LiteralPath $legacy -Encoding utf8 -Value @(
        '<?xml version="1.0" encoding="UTF-8"?>'
        '<testsuite name="sophisticatedbuilding-servertests" tests="3" failures="1">'
        '  <testcase name="mergegametest.slabmergestodouble" time="0.0"/>'
        '  <testcase name="backpackgametest.upgradesupplies" time="0.05"/>'
        '  <testcase name="protectiongametest.worldborderskipsoutside" time="0.0">'
        '    <failure message="placed outside the border"/>'
        '  </testcase>'
        '</testsuite>'
    )
    $names12 = @(Get-JUnitTestNames -XmlPath $legacy)
    Assert-True -Condition ($names12.Count -eq 3) -Message "a testcase without classname is parsed (got $($names12.Count) of 3)"
    Assert-True -Condition ($names12[0].ClassName -eq '' -and $names12[0].Name -eq 'mergegametest.slabmergestodouble') -Message 'missing classname reads as empty, name is kept'
    Assert-True -Condition ($names12[2].Failed -and -not $names12[0].Failed) -Message 'failure element detected per testcase'
    $sb12 = @($names12 | Where-Object { $_.ClassName -match 'Backpack' -or $_.Name -match 'Backpack' })
    Assert-True -Condition ($sb12.Count -eq 1) -Message 'the gametest stage''s Backpack filter works on name-only testcases'

    $resultsDir12 = Join-Path $dir12 'results'
    New-Item -ItemType Directory -Path $resultsDir12 | Out-Null
    Copy-Item -LiteralPath $legacy -Destination (Join-Path $resultsDir12 'legacy.xml')
    Set-Content -LiteralPath (Join-Path $resultsDir12 'TEST-gradle.xml') -Encoding utf8 -Value @(
        '<?xml version="1.0" encoding="UTF-8"?>'
        '<testsuite name="sophisticated.building.FooTest" tests="5" skipped="1" failures="0" errors="0" timestamp="2026-09-25T10:00:00" hostname="x" time="0.1">'
        '  <testcase name="a()" classname="sophisticated.building.FooTest" time="0.0"/>'
        '</testsuite>'
    )
    $summary12 = Get-JUnitSummary -ResultsDir $resultsDir12
    Assert-True -Condition ($summary12.Tests -eq 8 -and $summary12.Failures -eq 1 -and $summary12.Errors -eq 0 -and $summary12.Skipped -eq 1 -and $summary12.FileCount -eq 2) -Message "Get-JUnitSummary sums both shapes, missing counters as 0 (tests $($summary12.Tests), failures $($summary12.Failures), skipped $($summary12.Skipped))"
} finally {
    Remove-Item -LiteralPath $dir12 -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ''
if ($script:TestsFailed -gt 0) {
    Write-Host "$($script:TestsFailed) of $($script:TestsRun) offline test(s) FAILED" -ForegroundColor Red
    exit 1
}
Write-Host "All $($script:TestsRun) offline test(s) passed" -ForegroundColor Green
exit 0
