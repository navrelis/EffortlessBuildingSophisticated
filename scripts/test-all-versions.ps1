#requires -Version 7.0
<#
.SYNOPSIS
    One command that tests every Minecraft version branch and every loader: build, in-world GameTests, a
    dedicated server smoke boot, a client smoke boot (quickplay into a throwaway world), and the
    runSmokeServer/runSmokeClient scenario harness contract - then prints/writes a pass/fail report.

.DESCRIPTION
    For each versions/<mc> worktree (see scripts/setup-worktrees.ps1) and each loader folder inside it (any
    top-level folder with settings.gradle + gradlew, same rule as build-all.ps1/release.ps1/CI), runs the
    requested -Stages in order:

      build    - gradlew build --no-daemon (compile, unit tests, checkCommonIsLoaderNeutral). Pass iff exit 0;
                 detail always includes the parsed build/test-results/test/*.xml tests/failures/errors counts.
      gametest - only if the loader has src/gametest (Fabric's convention; NeoForge/Forge don't use this
                 mechanism on this branch, see docs/ARCHITECTURE.md - reported "n/a" for them). Runs
                 gradlew runGametest; pass iff the log contains "All N required tests passed". GameTest
                 classes/methods whose name contains "Backpack" are additionally listed as SB gametest coverage.
      server   - accepts the EULA in <loader>/run/eula.txt, starts gradlew runServer in the background, waits
                 for "Done (" in run/logs/latest.log, checks for mod-related ERROR/Exception lines, and - where
                 this (version, loader) ships a Sophisticated Backpacks integration (detected the same way
                 common/ does: a META-INF/services/...IBackpackIntegration registration under the loader's own
                 resources) - REQUIRES that sophisticatedbackpacks and sophisticatedcore both show up as loaded
                 mods and that "Registered Sophisticated Backpacks upgrade containers" is logged; missing SB
                 evidence where SB is expected is a stage FAILURE, not just a missing nice-to-have. Stops the
                 server with "stop" on stdin, falling back to killing the process tree it started.
      client   - starts gradlew runClient with a generated Gradle init script that appends
                 --quickPlaySingleplayer <world> to the runClient task's program args (a throwaway copy of the
                 server stage's world, deleted afterwards - never touches a tracked file or an existing save).
                 Waits for the join line, then 30s more; fails on mod-related ERROR/Exception lines or a new
                 crash report; kills the process tree it started.
      smoke    - contract for an in-game scenario harness another agent adds later (see docs/TESTING.md): if
                 the loader build has a runSmokeServer/runSmokeClient Gradle task, runs it with
                 -PsmoketestOut=<dir>, waits for <dir>/smoketest-result.json and records every check. Checks
                 named "sb.*" are Sophisticated Backpacks functional checks; where SB is available for that
                 (version, loader), at least one sb.* check must run and all must pass, or the stage FAILS.
                 Where SB is available but the harness task doesn't exist yet, the stage is reported "warn"
                 ("missing harness"), not silently "n/a" - "n/a" is reserved for loaders with no SB to test.

    Every process this script starts is tracked and killed (by PID, via its own process tree only) on timeout,
    on a failure with -KeepGoing:$false, or in the top-level finally block (including Ctrl+C, which PowerShell
    delivers as a terminating exception through the same try/finally). Every gradlew invocation passes
    --no-daemon and this script never runs "gradlew --stop" - that would kill every Gradle daemon of that
    version on the machine, including other agents' builds (see docs/TESTING.md). Stages run strictly
    sequentially, one Minecraft instance and one Gradle invocation at a time - never two in parallel, and never
    two ForgeGradle builds in parallel against a cold cache (docs/PORTING.md "Gotchas").

.PARAMETER Mc
    Restrict the run to these Minecraft versions (must match <VersionsDir>/<mc> folder names). Default: every
    worktree under -VersionsDir.

.PARAMETER Loader
    Restrict the run to these loader folder names (e.g. fabric, neoforge, forge). Default: every loader folder
    discovered in each selected version.

.PARAMETER Stages
    Which stages to run, in this fixed order regardless of the order given: build, gametest, server, client,
    smoke. Default: all five.

.PARAMETER VersionsDir
    Root directory holding one <mc> subfolder per version worktree, each laid out like versions/<mc> (see
    scripts/setup-worktrees.ps1). Defaults to versions/ under the repo root. Point this at an isolated worktree
    (e.g. a detached `git worktree add --detach local/versions-test/1.21.1 mc/1.21.1`, then
    -VersionsDir local/versions-test) to test without touching a versions/<mc> worktree another agent is
    currently building in.

.PARAMETER ReportDir
    Where to write report.json / report.md and per-stage logs. Default: local/test-reports/<timestamp>.

.PARAMETER TimeoutMinutes
    Per-stage timeout applied uniformly to build/gametest/server/client/smoke (each stage's own wait, e.g. the
    server's wait for "Done (" or the client's wait for the join line, plus a short fixed grace period after -
    see docs/TESTING.md). Default: 20.

.PARAMETER KeepGoing
    Default on: record a stage failure and keep going with the rest. Pass -KeepGoing:$false to stop the whole
    run at the first failure.

.PARAMETER Headless
    Skip the client stage and the runSmokeClient half of the smoke stage (both open a window). server,
    runSmokeServer and everything else still runs.

.PARAMETER WhatIf
    Standard PowerShell dry run: reports which (version, loader, stage) combinations would run, without
    starting any Gradle process.

.EXAMPLE
    pwsh scripts/test-all-versions.ps1 -Mc 1.21.1 -VersionsDir local/versions-test
    Runs every stage for every loader of the 1.21.1 worktree at local/versions-test/1.21.1.

.EXAMPLE
    pwsh scripts/test-all-versions.ps1 -Headless -KeepGoing:$false
    Runs build/gametest/server/smoke (no windows) for every version and loader, stopping at the first failure.
#>
[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [string[]]$Mc,
    [string[]]$Loader,
    # No [ValidateSet] here on purpose: values are comma-split (see Split-ArrayParam below) before validation,
    # which has to happen in the script body, after that split - a ValidateSet on the raw parameter would
    # reject a single comma-joined token (e.g. -Stages build,gametest) before the split ever runs.
    [string[]]$Stages = @('build', 'gametest', 'server', 'client', 'smoke'),
    [string]$VersionsDir,
    [string]$ReportDir,
    [int]$TimeoutMinutes = 20,
    [switch]$KeepGoing = $true,
    [switch]$Headless
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'lib/TestAllVersions.Common.ps1')
. (Join-Path $PSScriptRoot 'lib/GameWindow.ps1')
$script:GameWindowLibPath = (Join-Path $PSScriptRoot 'lib/GameWindow.ps1')

function Start-WindowMoveJob {
    <# Hands the "wait for the window and move it to the second monitor" half off to a background job so it
       doesn't block the caller's own log-polling wait. Only ever touches a window belonging to
       $GameProcessId's own descendant tree. Call Set-MinecraftMuted separately, BEFORE starting the game
       process (options.txt is read at boot) - this only starts the window-move watcher. #>
    param([Parameter(Mandatory)][int]$GameProcessId)
    return Start-Job -ScriptBlock {
        param($LibPath, $TargetPid)
        . $LibPath
        Move-MinecraftWindowToSecondary -ProcessId $TargetPid -TimeoutSeconds 180
    } -ArgumentList $script:GameWindowLibPath, $GameProcessId
}

function Stop-WindowMoveJob {
    <# Collects the job's own result (Move-MinecraftWindowToSecondary's $true/$false, plus any of its
       Write-Host/Write-Warning output) so it's visible in this stage's own console/log instead of silently
       vanishing when the job is torn down, then stops and removes it. #>
    param($Job)
    if (-not $Job) { return }
    try {
        $moved = Receive-Job -Job $Job -ErrorAction SilentlyContinue -Wait -Timeout 5 6>&1 5>&1 4>&1 3>&1 2>&1 |
            Out-String
        if ($moved) { Write-Host "  (window move job) $($moved.Trim())" }
    } catch {}
    Stop-Job -Job $Job -ErrorAction SilentlyContinue | Out-Null
    Remove-Job -Job $Job -Force -ErrorAction SilentlyContinue | Out-Null
}

# Array parameters (-Mc, -Loader, -Stages) only collect multiple bare tokens into one array when PowerShell
# itself is doing the invoking; called via `pwsh -File` from a foreign shell (bash, cmd), a second bare token
# after e.g. -Stages is silently DROPPED rather than added to the array. Comma-splitting every element here
# makes both forms work uniformly: -Stages build,gametest,server (robust from any shell) and, from inside
# PowerShell, -Stages build gametest server.
function Split-ArrayParam {
    param([string[]]$Values)
    if (-not $Values) { return @() }
    return @($Values | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}
$Mc = Split-ArrayParam -Values $Mc
$Loader = Split-ArrayParam -Values $Loader
$Stages = Split-ArrayParam -Values $Stages
if (-not $Stages -or $Stages.Count -eq 0) { $Stages = @('build', 'gametest', 'server', 'client', 'smoke') }
$validStages = @('build', 'gametest', 'server', 'client', 'smoke')
$invalidStages = @($Stages | Where-Object { $validStages -notcontains $_ })
if ($invalidStages.Count -gt 0) {
    Write-Error "Invalid -Stages value(s): $($invalidStages -join ', '). Valid stages: $($validStages -join ', ')."
    exit 1
}

if (-not $VersionsDir) { $VersionsDir = Join-Path $repoRoot 'versions' }
elseif (-not [System.IO.Path]::IsPathRooted($VersionsDir)) { $VersionsDir = Join-Path $repoRoot $VersionsDir }

if (-not (Test-Path $VersionsDir)) {
    Write-Error "'$VersionsDir' not found. Run scripts/setup-worktrees.ps1 first (or pass -VersionsDir at an existing worktree root)."
    exit 1
}

if ($Headless -and ($Stages -contains 'client')) {
    $Stages = @($Stages | Where-Object { $_ -ne 'client' })
}
# Fixed execution order regardless of what order -Stages listed them in.
$stageOrder = @('build', 'gametest', 'server', 'client', 'smoke')
$Stages = @($stageOrder | Where-Object { $Stages -contains $_ })

if (-not $ReportDir) {
    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $ReportDir = Join-Path $repoRoot "local/test-reports/$timestamp"
}
# Report bookkeeping directories are created even under -WhatIf (harmless, and report.json/report.md still
# need somewhere to land describing what WOULD have run).
New-Item -ItemType Directory -Path $ReportDir -Force -WhatIf:$false | Out-Null
$ReportDir = (Resolve-Path $ReportDir).Path

$branches = @(Get-VersionWorktrees -VersionsDir $VersionsDir -Mc $Mc)
if (-not $branches -or $branches.Count -eq 0) {
    if ($Mc) {
        Write-Error "No matching version worktree(s) under '$VersionsDir' for -Mc $($Mc -join ', ')."
    } else {
        Write-Warning "No version worktrees found under '$VersionsDir'. Nothing to test."
    }
    exit 1
}

Write-Host "test-all-versions: $($branches.Count) version(s), stages: $($Stages -join ', '), report -> $ReportDir" -ForegroundColor Cyan

# ---------------------------------------------------------------------------------------------------------------
# Stage implementations. Each returns one (or, for smoke, several) result row(s) from New-StageResult.
# WhatIf is checked via $WhatIfPreference, which PowerShell propagates down into these functions automatically
# because the top-level script declared SupportsShouldProcess.
# ---------------------------------------------------------------------------------------------------------------

$script:SbGametestCoverage = [System.Collections.Generic.List[object]]::new()

function Invoke-BuildStage {
    param([string]$Mc, [string]$LoaderName, [string]$LoaderDir, [string]$StageLogDir, [int]$TimeoutMinutes)
    if ($WhatIfPreference) {
        return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'build' -Result 'n/a' -Detail 'skipped (-WhatIf)'
    }
    Write-Host "==> [$Mc/$LoaderName] build"
    $logPath = Join-Path $StageLogDir 'build.console.log'
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $proc = Start-GradleProcess -LoaderDir $LoaderDir -TaskArgs @('build', '--no-daemon', '--stacktrace') -LogPath $logPath
    $wait = Wait-GradleProcess -Process $proc -TimeoutMinutes $TimeoutMinutes
    $sw.Stop()

    $junit = Get-JUnitSummary -ResultsDir (Join-Path $LoaderDir 'build/test-results/test')
    $junitDetail = "unit tests=$($junit.Tests) failures=$($junit.Failures) errors=$($junit.Errors) skipped=$($junit.Skipped)"

    Copy-IfExists -Path (Join-Path $LoaderDir 'build/test-results/test') -Destination (Join-Path $StageLogDir 'test-results') | Out-Null

    if ($wait.TimedOut) {
        return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'build' -Result 'fail' `
            -Detail "timed out after $TimeoutMinutes min; $junitDetail" -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath
    }
    $result = if ($wait.ExitCode -eq 0) { 'pass' } else { 'fail' }
    $detail = if ($wait.ExitCode -eq 0) { $junitDetail } else { "exit $($wait.ExitCode); $junitDetail" }
    return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'build' -Result $result -Detail $detail -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath
}

function Invoke-GametestStage {
    param([string]$Mc, [string]$LoaderName, [string]$LoaderDir, [string]$StageLogDir, [int]$TimeoutMinutes, [bool]$HasGametest)
    if (-not $HasGametest) {
        return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'gametest' -Result 'n/a' -Detail 'no src/gametest for this loader'
    }
    if ($WhatIfPreference) {
        return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'gametest' -Result 'n/a' -Detail 'skipped (-WhatIf)'
    }
    Write-Host "==> [$Mc/$LoaderName] gametest"
    $logPath = Join-Path $StageLogDir 'gametest.console.log'
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $proc = Start-GradleProcess -LoaderDir $LoaderDir -TaskArgs @('runGametest', '--no-daemon', '--stacktrace') -LogPath $logPath
    $wait = Wait-GradleProcess -Process $proc -TimeoutMinutes $TimeoutMinutes
    $sw.Stop()

    $junitPath = Join-Path $LoaderDir 'build/gametest/junit.xml'
    Copy-IfExists -Path $junitPath -Destination (Join-Path $StageLogDir 'gametest.junit.xml') | Out-Null
    $sbTests = @(Get-JUnitTestNames -XmlPath $junitPath | Where-Object { $_.ClassName -match 'Backpack' -or $_.Name -match 'Backpack' })
    foreach ($t in $sbTests) {
        $script:SbGametestCoverage.Add([pscustomobject]@{
            mc = $Mc; loader = $LoaderName; test = "$($t.ClassName)#$($t.Name)"; failed = ($t.Failed -or $t.Errored)
        })
    }

    $logContent = if (Test-Path $logPath) { Get-Content -LiteralPath $logPath -Raw } else { '' }
    $match = [regex]::Match($logContent, 'All\s+(\d+)\s+required tests passed')
    $extra = @{ sbGametestCount = $sbTests.Count }

    if ($wait.TimedOut) {
        return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'gametest' -Result 'fail' `
            -Detail "timed out after $TimeoutMinutes min" -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath -Extra $extra
    }
    if ($match.Success) {
        return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'gametest' -Result 'pass' `
            -Detail "$($match.Value) (SB-related tests: $($sbTests.Count))" -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath -Extra $extra
    }
    return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'gametest' -Result 'fail' `
        -Detail "exit $($wait.ExitCode); 'All N required tests passed' not found in log" -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath -Extra $extra
}

function Invoke-ServerStage {
    param([string]$Mc, [string]$LoaderName, [string]$LoaderDir, [string]$StageLogDir, [int]$TimeoutMinutes, [bool]$HasSB)
    if ($WhatIfPreference) {
        return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'server' -Result 'n/a' -Detail 'skipped (-WhatIf)'
    }
    Write-Host "==> [$Mc/$LoaderName] server (SB expected: $HasSB)"

    $runDir = Join-Path $LoaderDir 'run'
    New-Item -ItemType Directory -Path $runDir -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $runDir 'eula.txt') -Value 'eula=true' -Encoding ascii -Force

    # A random free port every run: the default 25565 is a real collision risk when another Minecraft server
    # (another agent's dev run, a manually-started instance, ...) is already listening on it on this machine.
    $serverPort = Get-FreeTcpPort
    Set-ServerPort -RunDir $runDir -Port $serverPort

    $latestLog = Join-Path $runDir 'logs/latest.log'
    $logPath = Join-Path $StageLogDir 'server.console.log'
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    # log4j2 rotates a pre-existing run/logs/latest.log into a dated .log.gz once the new session's own logging
    # inits - but that can lag behind this stage's first poll (gradlew.bat/JVM startup), so an unrotated stale
    # latest.log from an earlier server run can still contain "Done (" when we start looking. Capture its length
    # NOW, before launch, so Wait-ForLogPattern only ever matches against bytes written after this point (see its
    # own comment) - never a stale "Done (" left over from a previous run in the same run dir.
    $logBaseline = Get-LogBaselineLength -Path $latestLog
    $proc = Start-GradleProcess -LoaderDir $LoaderDir -TaskArgs @('runServer', '--no-daemon') -LogPath $logPath -RedirectInput

    $doneLine = Wait-ForLogPattern -Path $latestLog -Pattern 'Done \(' -TimeoutMinutes $TimeoutMinutes -Process $proc -BaselineLength $logBaseline
    Start-Sleep -Seconds 3   # let a couple more lines (mod init tail, SB registration) land after "Done ("
    $fullLog = if (Test-Path $latestLog) { Get-Content -LiteralPath $latestLog -Raw } else { '' }

    $stoppedCleanly = $false
    if (-not $proc.HasExited) {
        try {
            $proc.StandardInput.WriteLine('stop')
            $proc.StandardInput.Flush()
            $stoppedCleanly = $proc.WaitForExit(60000)
        } catch { $stoppedCleanly = $false }
    } else {
        $stoppedCleanly = $true
    }
    if (-not $stoppedCleanly -and -not $proc.HasExited) {
        Stop-ProcessTree -ProcessId $proc.Id
        $proc.WaitForExit(10000) | Out-Null
    }
    Unregister-TrackedProcess -Process $proc
    $sw.Stop()

    $lines = $fullLog -split "`r?`n"
    $badLines = @($lines | Where-Object { $_ -match '(ERROR|Exception)' -and $_ -match 'sophisticatedbuilding|sophisticated\.building' })
    $sbLinePresent = $fullLog -match 'Registered Sophisticated Backpacks upgrade containers'
    $sbModsLoaded = $false
    if ($HasSB) {
        $sbModsLoaded = (Test-ModListedInLog -LogContent $fullLog -ModId 'sophisticatedbackpacks') -and
                        (Test-ModListedInLog -LogContent $fullLog -ModId 'sophisticatedcore')
    }

    $problems = [System.Collections.Generic.List[string]]::new()
    if (-not $doneLine) { $problems.Add("server never reached 'Done (' within $TimeoutMinutes min") }
    if ($badLines.Count -gt 0) { $problems.Add("$($badLines.Count) ERROR/Exception line(s) mentioning the mod") }
    if ($HasSB) {
        if (-not $sbModsLoaded) { $problems.Add('sophisticatedbackpacks/sophisticatedcore not both listed as loaded mods') }
        if (-not $sbLinePresent) { $problems.Add("'Registered Sophisticated Backpacks upgrade containers' missing") }
    } elseif ($sbLinePresent) {
        $problems.Add('unexpected SB registration line on a loader with no SB integration shipped')
    }

    $result = if ($problems.Count -eq 0) { 'pass' } else { 'fail' }
    $detail = if ($problems.Count -eq 0) {
        if ($HasSB) { 'reached Done; SB verified (backpacks+core loaded, upgrade containers registered)' } else { 'reached Done; no SB expected' }
    } else {
        $problems -join '; '
    }

    Copy-IfExists -Path $latestLog -Destination (Join-Path $StageLogDir 'server.latest.log') | Out-Null
    return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'server' -Result $result -Detail $detail -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath `
        -Extra @{ sbAvailable = $HasSB; sbVerified = ($HasSB -and $sbModsLoaded -and $sbLinePresent); stoppedCleanly = $stoppedCleanly; serverPort = $serverPort }
}

function New-QuickPlayInitScript {
    param([string]$ReportDir)
    $initScriptPath = Join-Path $ReportDir 'quickplay-init.gradle'
    if (-not (Test-Path $initScriptPath)) {
        $content = @'
// Generated by scripts/test-all-versions.ps1 - appends --quickPlaySingleplayer to the client dev run's program
// arguments, hooked generically by task name ("runClient") rather than the three loader-specific DSL paths
// (loom.runs.client.programArgs / neoForge.runs.client.programArguments / minecraft.runs.client.args).
//
// Two different shapes were found by actually running this against all three loaders on 1.21.1 (see
// docs/TESTING.md "client stage"):
//   - Loom (fabric) and ForgeGradle 7 (forge): runClient's `args` is a plain list of program-argument tokens,
//     so JavaExec#args(...) (which APPENDS, not replaces) is enough on its own.
//   - ModDevGradle (neoforge): runClient's `args` is a single "@<path>\clientRunProgramArgs.txt" token -
//     net.neoforged.devlaunch.Main reads its real program arguments from THAT FILE, not from extra tokens on
//     the command line. Appending '--quickPlaySingleplayer' as a second bare CLI token after the @file made
//     devlaunch.Main try to load a class literally named "--quickPlaySingleplayer" (ClassNotFoundException) -
//     confirmed by running it for real. The file itself already ends with a "# User Supplied Program
//     Arguments" section (one token per line) generated by MDG specifically for this purpose, so the fix is to
//     append our two tokens there instead, right before the task actually executes (the file is a Gradle task
//     output that already exists by the time runClient's own doFirst runs).
allprojects {
    def worldName = project.findProperty('quickPlayWorld')
    if (worldName) {
        tasks.matching { it.name == 'runClient' }.configureEach {
            doFirst {
                def argsFileToken = args.find { it.toString().startsWith('@') }
                if (argsFileToken) {
                    def argsFile = new File(argsFileToken.toString().substring(1))
                    argsFile << "\n--quickPlaySingleplayer\n${worldName}\n"
                    logger.lifecycle("[test-all-versions] runClient quickplay world: ${worldName} (via ${argsFile})")
                } else {
                    args('--quickPlaySingleplayer', worldName.toString())
                    logger.lifecycle("[test-all-versions] runClient quickplay world: ${worldName} (via task args)")
                }
            }
        }
    }
}
'@
        Set-Content -LiteralPath $initScriptPath -Value $content -Encoding utf8
    }
    return $initScriptPath
}

function Invoke-ClientStage {
    param([string]$Mc, [string]$LoaderName, [string]$LoaderDir, [string]$StageLogDir, [int]$TimeoutMinutes, [string]$ReportDir)
    if ($WhatIfPreference) {
        return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'client' -Result 'n/a' -Detail 'skipped (-WhatIf)'
    }
    # Once a loader has its own runSmokeClient, that harness's own join-and-check already covers "does the
    # client boot and join without errors" (and more) - running the plain client stage on top would just open
    # a second, redundant Minecraft window. The plain client stage is a FALLBACK for loaders that don't have
    # the richer harness yet (still every loader today, since it doesn't exist on this branch yet).
    if (Test-HasSmokeClientTask -LoaderDir $LoaderDir) {
        return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'client' -Result 'n/a' `
            -Detail 'skipped: runSmokeClient covers join-and-check for this loader (see smoke stage)'
    }
    Write-Host "==> [$Mc/$LoaderName] client"

    $runDir = Join-Path $LoaderDir 'run'
    New-Item -ItemType Directory -Path $runDir -Force | Out-Null

    # Reuse the server stage's freshly generated dedicated-server world (run/world) as a throwaway client
    # singleplayer save (structurally compatible) - copied under a random name in run/saves/, deleted at the
    # end of this stage. Never touches run/saves/<a save that was already there> or any tracked file.
    #
    # NOTE: --quickPlaySingleplayer <name> only opens an EXISTING save - confirmed the hard way. A name with no
    # matching save does NOT create a fresh world; it shows "Failed to Quick Play - Could not find world with
    # the provided identifier" and sits on that screen (a fresh-world attempt was tried and reverted - see
    # docs/TESTING.md "client stage"). The guard below (Failed to Quick Play / Could not find world) exists
    # specifically so a future regression here fails the stage immediately instead of leaving a window hanging.
    $worldSource = Join-Path $runDir 'world'
    $tempWorldName = "test-all-versions-$([guid]::NewGuid().ToString('N').Substring(0, 8))"
    $tempWorldPath = Join-Path $runDir "saves/$tempWorldName"
    $quickPlayUsed = $false
    if (Test-Path $worldSource) {
        New-Item -ItemType Directory -Path (Split-Path -Parent $tempWorldPath) -Force | Out-Null
        Copy-Item -LiteralPath $worldSource -Destination $tempWorldPath -Recurse -Force
        Remove-Item -LiteralPath (Join-Path $tempWorldPath 'session.lock') -Force -ErrorAction SilentlyContinue
        $quickPlayUsed = $true
    }

    $initScriptPath = New-QuickPlayInitScript -ReportDir $ReportDir
    $taskArgs = @('--init-script', $initScriptPath, 'runClient', '--no-daemon', '--stacktrace')
    if ($quickPlayUsed) {
        $taskArgs = @('--init-script', $initScriptPath, "-PquickPlayWorld=$tempWorldName", 'runClient', '--no-daemon', '--stacktrace')
    }

    $latestLog = Join-Path $runDir 'logs/latest.log'
    $crashDir = Join-Path $runDir 'crash-reports'
    $preCrashFiles = @()
    if (Test-Path $crashDir) {
        $preCrashFiles = @(Get-ChildItem -LiteralPath $crashDir -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
    }

    $logPath = Join-Path $StageLogDir 'client.console.log'
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    # Mute BEFORE the game process starts (options.txt is read at boot). The window move has to wait for the
    # window to exist, so that half runs in a background job instead of blocking this stage's own log poll.
    Set-MinecraftMuted -RunDir $runDir
    # Same stale-log race as the server stage: capture latest.log's pre-launch length so the join-line wait below
    # can never match a "logged in with entity id" (or "Loaded N advancements") line left over from an earlier
    # client run in this same run dir, in the window before log4j2 gets around to rotating it away.
    $logBaseline = Get-LogBaselineLength -Path $latestLog
    $proc = Start-GradleProcess -LoaderDir $LoaderDir -TaskArgs $taskArgs -LogPath $logPath
    $moveJob = Start-WindowMoveJob -GameProcessId $proc.Id

    # "Loaded N advancements" happens very early in client bootstrap (well before any world join), so it must
    # NOT be accepted as a join signal when quickplay is actually in play - it would match almost immediately
    # and let the stage "pass" having barely started the client, long before it ever joined the copied world.
    # It's only the right (and only) signal when there's no world to join at all (no quickplay world copied).
    #
    # Also watched for in the SAME wait: quickplay's own failure toast ("Failed to Quick Play - Could not find
    # world with the provided identifier"), which otherwise just sits on screen doing nothing until the stage
    # timeout - if quickplay ever again points at a world that doesn't exist (e.g. the server stage didn't
    # produce one), this ends the stage immediately with that message instead of leaving a window hanging for
    # the full -TimeoutMinutes.
    $joinPattern = if ($quickPlayUsed) { 'logged in with entity id' } else { 'Loaded \d+ advancements' }
    $quickPlayFailurePattern = 'Failed to Quick Play|Could not find world'
    $matchedLine = Wait-ForLogPattern -Path $latestLog -Pattern "($joinPattern|$quickPlayFailurePattern)" -TimeoutMinutes $TimeoutMinutes -Process $proc -BaselineLength $logBaseline
    $quickPlayFailed = [bool]($matchedLine -and ($matchedLine -match $quickPlayFailurePattern))
    $joinLine = if ($quickPlayFailed) { $null } else { $matchedLine }
    if ($joinLine) { Start-Sleep -Seconds 30 }
    $fullLog = if (Test-Path $latestLog) { Get-Content -LiteralPath $latestLog -Raw } else { '' }

    if (-not $proc.HasExited) {
        Stop-ProcessTree -ProcessId $proc.Id
        $proc.WaitForExit(10000) | Out-Null
    }
    Unregister-TrackedProcess -Process $proc
    Stop-WindowMoveJob -Job $moveJob
    $sw.Stop()

    $lines = $fullLog -split "`r?`n"
    $badLines = @($lines | Where-Object { $_ -match '(ERROR|Exception)' -and $_ -match 'sophisticatedbuilding|sophisticated\.building' })
    $newCrashFiles = @()
    if (Test-Path $crashDir) {
        $newCrashFiles = @(Get-ChildItem -LiteralPath $crashDir -File -ErrorAction SilentlyContinue |
            Where-Object { $preCrashFiles -notcontains $_.FullName } | Select-Object -ExpandProperty FullName)
    }

    if (Test-Path $tempWorldPath) { Remove-Item -LiteralPath $tempWorldPath -Recurse -Force -ErrorAction SilentlyContinue }

    # Classify each new crash report: a Forge-only, non-mod crash matching the known LootModifierManager /
    # early-world-tick race (see docs/TESTING.md "process safety") is a known upstream issue, not evidence this
    # client run is broken - reported as a warning, still clearly visible, instead of failing the stage outright.
    $knownUpstreamCrashes = @()
    $unclassifiedCrashFiles = @()
    foreach ($cf in $newCrashFiles) {
        $crashContent = Get-Content -LiteralPath $cf -Raw -ErrorAction SilentlyContinue
        $classification = Get-CrashReportClassification -CrashReportContent $crashContent
        if ($classification -eq 'known-upstream') {
            $knownUpstreamCrashes += (Split-Path -Leaf $cf)
        } else {
            $unclassifiedCrashFiles += $cf
        }
    }

    $problems = [System.Collections.Generic.List[string]]::new()
    $warnings = [System.Collections.Generic.List[string]]::new()
    if ($quickPlayFailed) {
        $problems.Add("quickplay failed: $($matchedLine.Trim())")
    } elseif (-not $joinLine) {
        $problems.Add("never reached the world/title within $TimeoutMinutes min")
    }
    if ($badLines.Count -gt 0) { $problems.Add("$($badLines.Count) ERROR/Exception line(s) mentioning the mod") }
    if ($unclassifiedCrashFiles.Count -gt 0) { $problems.Add("$($unclassifiedCrashFiles.Count) new crash report(s)") }
    if ($knownUpstreamCrashes.Count -gt 0) {
        $warnings.Add("$($knownUpstreamCrashes.Count) known-upstream crash report(s) (LootModifierManager race, no mod frames): $($knownUpstreamCrashes -join ', ')")
    }

    $result = if ($problems.Count -gt 0) { 'fail' } elseif ($warnings.Count -gt 0) { 'warn' } else { 'pass' }
    $detail = if ($problems.Count -gt 0) {
        $problems -join '; '
    } elseif ($warnings.Count -gt 0) {
        $snippet = $joinLine.Trim()
        if ($snippet.Length -gt 80) { $snippet = $snippet.Substring(0, 80) }
        "quickplay=$quickPlayUsed; $snippet; $($warnings -join '; ')"
    } else {
        $snippet = $joinLine.Trim()
        if ($snippet.Length -gt 80) { $snippet = $snippet.Substring(0, 80) }
        "quickplay=$quickPlayUsed; $snippet"
    }

    Copy-IfExists -Path $latestLog -Destination (Join-Path $StageLogDir 'client.latest.log') | Out-Null
    foreach ($cf in $newCrashFiles) {
        Copy-IfExists -Path $cf -Destination (Join-Path $StageLogDir "crash-reports/$(Split-Path -Leaf $cf)") | Out-Null
    }

    return New-StageResult -Mc $Mc -Loader $LoaderName -Stage 'client' -Result $result -Detail $detail -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath `
        -Extra @{ quickPlayUsed = $quickPlayUsed; quickPlayFailed = $quickPlayFailed; knownUpstreamCrashes = $knownUpstreamCrashes.Count }
}

function Invoke-SmokeStage {
    param([string]$Mc, [string]$LoaderName, [string]$LoaderDir, [string]$StageLogDir, [int]$TimeoutMinutes, [bool]$HasSB, [bool]$Headless)
    $rows = [System.Collections.Generic.List[object]]::new()
    $targets = @('runSmokeServer', 'runSmokeClient')
    if ($Headless) { $targets = @('runSmokeServer') }

    foreach ($target in $targets) {
        $stageName = "smoke ($target)"
        if ($WhatIfPreference) {
            $rows.Add((New-StageResult -Mc $Mc -Loader $LoaderName -Stage $stageName -Result 'n/a' -Detail 'skipped (-WhatIf)'))
            continue
        }
        Write-Host "==> [$Mc/$LoaderName] $target"
        $outDir = Join-Path $StageLogDir "smoke-$target"
        New-Item -ItemType Directory -Path $outDir -Force | Out-Null
        $resultJsonPath = Join-Path $outDir 'smoketest-result.json'
        $logPath = Join-Path $StageLogDir "$target.console.log"

        # runSmokeClient opens a window like the plain client stage does; runSmokeServer (headless) doesn't.
        if ($target -eq 'runSmokeClient') {
            Set-MinecraftMuted -RunDir (Join-Path $LoaderDir 'run')
        }
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $proc = Start-GradleProcess -LoaderDir $LoaderDir -TaskArgs @($target, "-PsmoketestOut=$outDir", '--no-daemon', '--stacktrace') -LogPath $logPath
        $moveJob = if ($target -eq 'runSmokeClient') { Start-WindowMoveJob -GameProcessId $proc.Id } else { $null }
        $wait = Wait-GradleProcess -Process $proc -TimeoutMinutes $TimeoutMinutes
        Stop-WindowMoveJob -Job $moveJob
        $sw.Stop()

        $logContent = if (Test-Path $logPath) { Get-Content -LiteralPath $logPath -Raw } else { '' }
        if (Test-GradleTaskMissing -LogContent $logContent -TaskName $target) {
            if ($HasSB) {
                $rows.Add((New-StageResult -Mc $Mc -Loader $LoaderName -Stage $stageName -Result 'warn' `
                    -Detail "missing harness: SB is available for this loader but no sb.* check has run yet ($target does not exist)" `
                    -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath))
            } else {
                $rows.Add((New-StageResult -Mc $Mc -Loader $LoaderName -Stage $stageName -Result 'n/a' -Detail "no $target task (harness not added yet)" `
                    -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath))
            }
            continue
        }

        if (-not (Test-Path $resultJsonPath)) {
            $why = if ($wait.TimedOut) { 'timed out' } else { "exit $($wait.ExitCode)" }
            $rows.Add((New-StageResult -Mc $Mc -Loader $LoaderName -Stage $stageName -Result 'fail' `
                -Detail "smoketest-result.json not produced ($why)" -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath))
            continue
        }

        $parsed = $null
        try { $parsed = Get-Content -LiteralPath $resultJsonPath -Raw | ConvertFrom-Json } catch { $parsed = $null }
        if (-not $parsed) {
            $rows.Add((New-StageResult -Mc $Mc -Loader $LoaderName -Stage $stageName -Result 'fail' -Detail 'smoketest-result.json unparsable' `
                -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath))
            continue
        }

        # Get-JsonProp instead of dot access throughout: under Set-StrictMode a smoketest-result.json that's
        # missing an expected key (partial/older harness output) would otherwise throw PropertyNotFoundException
        # instead of being treated as a (probably failing) result.
        $allChecks = @(Get-JsonProp -Obj $parsed -Name 'checks' -Default @())
        $sbChecks = @($allChecks | Where-Object { (Get-JsonProp -Obj $_ -Name 'name' -Default '') -like 'sb.*' })
        $failedChecks = @($allChecks | Where-Object { -not (Get-JsonProp -Obj $_ -Name 'passed' -Default $false) })

        $problems = [System.Collections.Generic.List[string]]::new()
        $overallPassed = Get-JsonProp -Obj $parsed -Name 'passed' -Default $false
        if (-not $overallPassed -or $failedChecks.Count -gt 0) {
            $names = ($failedChecks | Select-Object -First 5 | ForEach-Object { Get-JsonProp -Obj $_ -Name 'name' -Default '?' }) -join ', '
            $problems.Add("$($failedChecks.Count)/$($allChecks.Count) check(s) failed: $names")
        }
        if ($HasSB) {
            if ($sbChecks.Count -eq 0) {
                $problems.Add('SB is available for this loader but no sb.* check ran ("SB not tested")')
            } else {
                $sbFailed = @($sbChecks | Where-Object { -not (Get-JsonProp -Obj $_ -Name 'passed' -Default $false) })
                if ($sbFailed.Count -gt 0) {
                    $sbFailedNames = ($sbFailed | ForEach-Object { Get-JsonProp -Obj $_ -Name 'name' -Default '?' }) -join ', '
                    $problems.Add("$($sbFailed.Count) sb.* check(s) failed: $sbFailedNames")
                }
            }
        }

        $result = if ($problems.Count -eq 0) { 'pass' } else { 'fail' }
        $detail = if ($problems.Count -eq 0) { "$($allChecks.Count) check(s) passed (sb.*: $($sbChecks.Count))" } else { $problems -join '; ' }

        Copy-IfExists -Path $resultJsonPath -Destination (Join-Path $StageLogDir "$target.smoketest-result.json") | Out-Null
        $screenshots = @(Get-JsonProp -Obj $parsed -Name 'screenshots' -Default @())
        foreach ($shot in $screenshots) { Copy-IfExists -Path $shot -Destination (Join-Path $StageLogDir "$target-screenshots/$(Split-Path -Leaf $shot)") | Out-Null }

        $rows.Add((New-StageResult -Mc $Mc -Loader $LoaderName -Stage $stageName -Result $result -Detail $detail `
            -DurationSeconds $sw.Elapsed.TotalSeconds -LogPath $logPath -Extra @{ sbChecks = $sbChecks.Count; totalChecks = $allChecks.Count }))
    }
    return $rows
}

# ---------------------------------------------------------------------------------------------------------------
# Main loop - strictly sequential: one (version, loader, stage) at a time, never two Minecraft instances and
# never two ForgeGradle builds running together.
# ---------------------------------------------------------------------------------------------------------------

$results = [System.Collections.Generic.List[object]]::new()
$sbAvailability = [System.Collections.Generic.List[object]]::new()
$stopRequested = $false

try {
    foreach ($branch in $branches) {
        if ($stopRequested) { break }
        $mcName = $branch.Name
        $mcDir = $branch.FullName
        $loaders = @(Get-LoaderFolders -McDir $mcDir -Loader $Loader)
        if (-not $loaders -or $loaders.Count -eq 0) {
            Write-Warning "No loader folders discovered under $mcDir (looked for settings.gradle + gradlew)."
            continue
        }

        foreach ($loaderName in $loaders) {
            if ($stopRequested) { break }
            $loaderDir = Join-Path $mcDir $loaderName
            $hasSB = Test-HasBackpackIntegration -LoaderDir $loaderDir
            $hasGametest = Test-HasGametest -LoaderDir $loaderDir
            $sbAvailability.Add([pscustomobject]@{ mc = $mcName; loader = $loaderName; sbAvailable = $hasSB })

            $stageLogDir = Join-Path $ReportDir "$mcName/$loaderName"
            New-Item -ItemType Directory -Path $stageLogDir -Force -WhatIf:$false | Out-Null

            foreach ($stage in $Stages) {
                if ($stopRequested) { break }
                $stageResults = switch ($stage) {
                    'build'    { @(Invoke-BuildStage -Mc $mcName -LoaderName $loaderName -LoaderDir $loaderDir -StageLogDir $stageLogDir -TimeoutMinutes $TimeoutMinutes) }
                    'gametest' { @(Invoke-GametestStage -Mc $mcName -LoaderName $loaderName -LoaderDir $loaderDir -StageLogDir $stageLogDir -TimeoutMinutes $TimeoutMinutes -HasGametest $hasGametest) }
                    'server'   { @(Invoke-ServerStage -Mc $mcName -LoaderName $loaderName -LoaderDir $loaderDir -StageLogDir $stageLogDir -TimeoutMinutes $TimeoutMinutes -HasSB $hasSB) }
                    'client'   { @(Invoke-ClientStage -Mc $mcName -LoaderName $loaderName -LoaderDir $loaderDir -StageLogDir $stageLogDir -TimeoutMinutes $TimeoutMinutes -ReportDir $ReportDir) }
                    'smoke'    { @(Invoke-SmokeStage -Mc $mcName -LoaderName $loaderName -LoaderDir $loaderDir -StageLogDir $stageLogDir -TimeoutMinutes $TimeoutMinutes -HasSB $hasSB -Headless:$Headless) }
                }
                foreach ($row in $stageResults) {
                    $results.Add($row)
                    $color = switch ($row.result) { 'pass' { 'Green' } 'fail' { 'Red' } 'warn' { 'Yellow' } default { 'Gray' } }
                    Write-Host ("    {0,-10} {1}" -f $row.result, $row.detail) -ForegroundColor $color
                    if ($row.result -eq 'fail' -and -not $KeepGoing) {
                        $stopRequested = $true
                    }
                }
            }
        }
    }
} finally {
    Stop-AllTrackedProcesses
}

# ---------------------------------------------------------------------------------------------------------------
# Report: console table, report.json, report.md.
# ---------------------------------------------------------------------------------------------------------------

Write-Host ''
Write-Host 'Sophisticated Backpacks availability:' -ForegroundColor Cyan
$sbAvailability | Format-Table -Property mc, loader, sbAvailable -AutoSize | Out-String -Width 4096 | Write-Host

Write-Host 'Results:' -ForegroundColor Cyan
$results | Format-Table -Property mc, loader, stage, result, detail, durationSeconds -AutoSize | Out-String -Width 4096 | Write-Host

if ($script:SbGametestCoverage.Count -gt 0) {
    Write-Host 'SB-named GameTests:' -ForegroundColor Cyan
    $script:SbGametestCoverage | Format-Table -AutoSize | Out-String -Width 4096 | Write-Host
} else {
    Write-Host 'SB-named GameTests: none found (no "*Backpack*" class/method under any src/gametest run).' -ForegroundColor DarkYellow
}

$summary = [ordered]@{
    pass  = @($results | Where-Object { $_.result -eq 'pass' }).Count
    fail  = @($results | Where-Object { $_.result -eq 'fail' }).Count
    warn  = @($results | Where-Object { $_.result -eq 'warn' }).Count
    'n/a' = @($results | Where-Object { $_.result -eq 'n/a' }).Count
    total = $results.Count
}
Write-Host ("Summary: {0} pass, {1} fail, {2} warn, {3} n/a (of {4})" -f $summary.pass, $summary.fail, $summary.warn, $summary.'n/a', $summary.total) -ForegroundColor Cyan

$reportObj = [ordered]@{
    generatedAt        = (Get-Date).ToString('o')
    reportDir           = $ReportDir
    versionsDir         = $VersionsDir
    parameters          = [ordered]@{
        mc              = $Mc
        loader          = $Loader
        stages          = $Stages
        timeoutMinutes  = $TimeoutMinutes
        keepGoing       = [bool]$KeepGoing
        headless        = [bool]$Headless
        whatIf          = [bool]$WhatIfPreference
    }
    sbAvailability      = $sbAvailability
    sbGametestCoverage  = $script:SbGametestCoverage
    results             = $results
    summary             = $summary
}
$reportJsonPath = Join-Path $ReportDir 'report.json'
# Always actually written, even under -WhatIf: the report itself (including which stages WOULD have run) is
# the whole point of a dry run, not an action the dry run should suppress.
$reportObj | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportJsonPath -Encoding utf8 -WhatIf:$false

$md = [System.Text.StringBuilder]::new()
[void]$md.AppendLine('# test-all-versions report')
[void]$md.AppendLine()
[void]$md.AppendLine("Generated: $($reportObj.generatedAt)")
[void]$md.AppendLine('Versions dir: `' + $VersionsDir + '`')
[void]$md.AppendLine("Stages: $($Stages -join ', ')")
[void]$md.AppendLine()
[void]$md.AppendLine('## Sophisticated Backpacks availability')
[void]$md.AppendLine()
[void]$md.AppendLine('| mc | loader | SB |')
[void]$md.AppendLine('|---|---|---|')
foreach ($row in $sbAvailability) {
    [void]$md.AppendLine("| $($row.mc) | $($row.loader) | $($row.sbAvailable) |")
}
[void]$md.AppendLine()
[void]$md.AppendLine('## Results')
[void]$md.AppendLine()
[void]$md.AppendLine('| mc | loader | stage | result | detail | duration (s) | log |')
[void]$md.AppendLine('|---|---|---|---|---|---|---|')
foreach ($row in $results) {
    $relLog = ''
    if ($row.logPath) {
        try { $relLog = [System.IO.Path]::GetRelativePath($ReportDir, $row.logPath) } catch { $relLog = $row.logPath }
    }
    $detailEsc = ($row.detail -replace '\|', '\|')
    [void]$md.AppendLine("| $($row.mc) | $($row.loader) | $($row.stage) | $($row.result) | $detailEsc | $($row.durationSeconds) | $relLog |")
}
if ($script:SbGametestCoverage.Count -gt 0) {
    [void]$md.AppendLine()
    [void]$md.AppendLine('## SB-named GameTests')
    [void]$md.AppendLine()
    [void]$md.AppendLine('| mc | loader | test | failed |')
    [void]$md.AppendLine('|---|---|---|---|')
    foreach ($row in $script:SbGametestCoverage) {
        [void]$md.AppendLine("| $($row.mc) | $($row.loader) | $($row.test) | $($row.failed) |")
    }
}
[void]$md.AppendLine()
[void]$md.AppendLine("## Summary")
[void]$md.AppendLine()
[void]$md.AppendLine("$($summary.pass) pass, $($summary.fail) fail, $($summary.warn) warn, $($summary.'n/a') n/a (of $($summary.total))")

$reportMdPath = Join-Path $ReportDir 'report.md'
Set-Content -LiteralPath $reportMdPath -Value $md.ToString() -Encoding utf8 -WhatIf:$false

Write-Host ''
Write-Host "Report written to $reportJsonPath and $reportMdPath" -ForegroundColor Cyan

if ($summary.fail -gt 0) {
    exit 1
}
exit 0
