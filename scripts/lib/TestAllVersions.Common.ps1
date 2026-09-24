#requires -Version 7.0
<#
    Helper functions for scripts/test-all-versions.ps1. Dot-sourced by the main script; not meant to be run
    directly. Kept in its own file purely for readability - nothing here is reused anywhere else.

    Process-safety rules these helpers exist to enforce (see docs/TESTING.md):
      - every gradlew invocation passes --no-daemon (never starts a daemon, never needs `gradlew --stop`, which
        would kill every daemon of that Gradle version on the machine, including other agents' builds)
      - every started process is tracked in $script:TrackedProcesses and only ITS OWN tracked PIDs are ever
        killed - by PID, via taskkill /T /F (kills that process's own tree), never by name/pattern matching
      - nothing here touches versions/1.21.4 or versions/1.20.4, and nothing here matches processes by name
        (which could accidentally hit another project's Java processes) - only PIDs this script itself started
#>

Set-StrictMode -Version Latest

# ---------------------------------------------------------------------------------------------------------------
# Process tracking - every process we Start() goes in here so timeout/Ctrl+C/finally can clean it up by PID only.
# ---------------------------------------------------------------------------------------------------------------

$script:TrackedProcesses = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()

function Register-TrackedProcess {
    param([Parameter(Mandatory)][System.Diagnostics.Process]$Process)
    $script:TrackedProcesses.Add($Process)
}

function Unregister-TrackedProcess {
    param([Parameter(Mandatory)][System.Diagnostics.Process]$Process)
    [void]$script:TrackedProcesses.Remove($Process)
}

function Stop-ProcessTree {
    <#
        Kills exactly one process tree by PID (taskkill /T kills the given PID and everything it spawned -
        cmd.exe -> gradlew.bat -> java.exe). Never touches any other process. Safe to call on an already-exited
        process (taskkill just reports "not found", which we ignore).
    #>
    param([Parameter(Mandatory)][int]$ProcessId)
    try {
        & taskkill.exe /PID $ProcessId /T /F 2>&1 | Out-Null
    } catch {
        # Process already gone, or taskkill unavailable - nothing more we can do for this one PID.
    }
}

function Stop-AllTrackedProcesses {
    <# Called from the main script's finally block and from the Ctrl+C handler. Kills only what WE started. #>
    foreach ($proc in @($script:TrackedProcesses)) {
        try {
            if (-not $proc.HasExited) {
                Write-Host "  (cleanup) stopping tracked process PID $($proc.Id) ..." -ForegroundColor DarkYellow
                Stop-ProcessTree -ProcessId $proc.Id
            }
        } catch {
            # Process object may already be disposed - fine, ignore.
        }
    }
    $script:TrackedProcesses.Clear()
}

# ---------------------------------------------------------------------------------------------------------------
# Gradle process launch. Output is redirected to a log file by cmd.exe itself (`> log 2>&1`), not by .NET pipes -
# this lets us ALSO redirect stdin for the server "stop" command without juggling async output-read events.
# ---------------------------------------------------------------------------------------------------------------

function Start-GradleProcess {
    <#
        Launches "cmd.exe /c <gradlewBat> <TaskArgs...> > <LogPath> 2>&1". TaskArgs is a real string ARRAY
        (one Gradle CLI token per element, e.g. @('build','--no-daemon','--stacktrace')) added one at a time to
        ProcessStartInfo.ArgumentList, which lets .NET do Win32 argv quoting/escaping per element - this is
        what makes an already-quoted path (e.g. an --init-script argument with a space in it) come through
        intact instead of getting mangled by hand-rolled string concatenation.
    #>
    param(
        [Parameter(Mandatory)][string]$LoaderDir,
        [Parameter(Mandatory)][string[]]$TaskArgs,
        [Parameter(Mandatory)][string]$LogPath,
        [switch]$RedirectInput
    )
    $gradlewBat = Join-Path $LoaderDir 'gradlew.bat'
    if (-not (Test-Path $gradlewBat)) {
        throw "gradlew.bat not found at '$gradlewBat'."
    }

    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = 'cmd.exe'
    $psi.WorkingDirectory = $LoaderDir
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardInput = [bool]$RedirectInput
    [void]$psi.ArgumentList.Add('/c')
    [void]$psi.ArgumentList.Add($gradlewBat)
    foreach ($a in $TaskArgs) { [void]$psi.ArgumentList.Add($a) }
    [void]$psi.ArgumentList.Add('>')
    [void]$psi.ArgumentList.Add($LogPath)
    [void]$psi.ArgumentList.Add('2>&1')

    $proc = [System.Diagnostics.Process]::new()
    $proc.StartInfo = $psi
    [void]$proc.Start()
    Register-TrackedProcess -Process $proc
    return $proc
}

function Wait-GradleProcess {
    <# Synchronous wait with a timeout, for build/gametest/smoke. Kills the tree and returns TimedOut=$true if
       the timeout elapses. #>
    param(
        [Parameter(Mandatory)][System.Diagnostics.Process]$Process,
        [Parameter(Mandatory)][int]$TimeoutMinutes
    )
    $timedOut = -not $Process.WaitForExit([int]([Math]::Max(1, $TimeoutMinutes) * 60000))
    if ($timedOut) {
        Stop-ProcessTree -ProcessId $Process.Id
        # Give the kill a moment to land so ExitCode below doesn't throw on a still-live handle.
        try { $Process.WaitForExit(5000) | Out-Null } catch {}
    }
    $exitCode = $null
    try { $exitCode = $Process.ExitCode } catch { $exitCode = -1 }
    Unregister-TrackedProcess -Process $Process
    return [pscustomobject]@{ TimedOut = $timedOut; ExitCode = $exitCode }
}

# ---------------------------------------------------------------------------------------------------------------
# Log polling - used for server "Done (" / client "logged in with entity id" waits, without re-reading the whole
# file from scratch every poll.
# ---------------------------------------------------------------------------------------------------------------

function Get-LogBaselineLength {
    <#
        Byte length of $Path right now, or 0 if it doesn't exist yet. Call this immediately BEFORE starting the
        next gradlew run/server/client process, and pass the result as Wait-ForLogPattern's -BaselineLength - see
        that function's own comment for why this is needed (a previous stage's run/logs/latest.log can still be
        sitting there, unrotated, when the new process's log-pattern wait starts polling).
    #>
    param([Parameter(Mandatory)][string]$Path)
    try {
        if (Test-Path -LiteralPath $Path) { return (Get-Item -LiteralPath $Path).Length }
    } catch {
        # Vanishes/locked between Test-Path and Get-Item - treat as "nothing there yet".
    }
    return 0
}

function Wait-ForLogPattern {
    <#
        Polls $Path (created late - e.g. run/logs/latest.log doesn't exist until the game starts logging) for a
        line matching $Pattern, up to $TimeoutMinutes. Returns the matched line, or $null on timeout. Also stops
        early (returns $null) if $Process exits before the pattern shows up.

        -BaselineLength (byte length of $Path immediately BEFORE the new process was started, via
        Get-LogBaselineLength - 0 if the file didn't exist yet) exists to close a real race: run/logs/latest.log
        gets rotated away by log4j2 (renamed into a dated .log.gz, replaced by a fresh, much smaller file) when a
        new session starts logging, but that rotation happens on the NEW game JVM's own log4j2 init - which can
        lag well behind this wait's first poll (gradlew.bat itself has to start, resolve/launch the game JVM,
        etc). Scanning the WHOLE file from byte 0 on every poll, as this used to do unconditionally, could then
        match a line already sitting in the STILL-UNROTATED previous run's latest.log (e.g. a stale "logged in
        with entity id" from an earlier client stage, or a stale "Done (" from an earlier server stage) before
        the new process has logged anything at all - a false-positive "reached the join line"/"server ready"
        long before this run's own game state actually got there.

        Fix: content at or before byte offset $BaselineLength is treated as stale and never scanned - matches
        only ever come from bytes written after the new process's launch. $BaselineLength is captured as an
        exact file-length snapshot (Get-LogBaselineLength) taken before anything new is written, so seeking to
        it always lands exactly on a line boundary (the true end-of-file at capture time) - never mid-line -
        which is what makes seek-and-scan-the-rest safe without any extra bookkeeping for a partial leading
        line. If the file's current length ever drops below the tracked offset, that IS the rotation happening
        (the old file was replaced by a smaller new one), so the offset resets to 0 and the new file is scanned
        from its own start onward. This handles both shapes seen in practice: pure in-place appending (offset
        never moves; the stale prefix is just permanently skipped) and rotate-then-grow (offset resets to 0 the
        first time the file is observed shorter than it was pre-launch). Re-reads the tail on every poll rather
        than keeping a live stream position across polls - log files here are at most a few MB and this only
        runs for a few minutes at a 2s poll interval, so this is cheap enough to not be worth a stickier
        offset-tracking scheme.
    #>
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$Pattern,
        [Parameter(Mandatory)][int]$TimeoutMinutes,
        [System.Diagnostics.Process]$Process,
        [int]$PollSeconds = 2,
        [long]$BaselineLength = 0
    )
    $staleOffset = [Math]::Max(0, $BaselineLength)
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    while ((Get-Date) -lt $deadline) {
        if (Test-Path -LiteralPath $Path) {
            try {
                $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
                try {
                    if ($stream.Length -lt $staleOffset) {
                        # Shorter than it was before launch: log4j2 rotated it away and started a fresh file: the
                        # whole thing (from here on, including this and every later poll) is new content.
                        $staleOffset = 0
                    }
                    if ($staleOffset -gt 0) {
                        [void]$stream.Seek($staleOffset, [System.IO.SeekOrigin]::Begin)
                    }
                    $reader = [System.IO.StreamReader]::new($stream)
                    $content = $reader.ReadToEnd()
                    foreach ($line in ($content -split "`r?`n")) {
                        if ($line -match $Pattern) {
                            return $line
                        }
                    }
                } finally {
                    $stream.Dispose()
                }
            } catch {
                # File locked mid-write by the JVM for an instant, or mid-rotation - just retry next poll.
            }
        }
        if ($Process -and $Process.HasExited) {
            return $null
        }
        Start-Sleep -Seconds $PollSeconds
    }
    return $null
}

# ---------------------------------------------------------------------------------------------------------------
# Loader / branch discovery (mirrors build-all.ps1 / release.ps1 / .github/workflows/build.yml).
# ---------------------------------------------------------------------------------------------------------------

function Get-VersionWorktrees {
    param([Parameter(Mandatory)][string]$VersionsDir, [string[]]$Mc)
    if (-not (Test-Path $VersionsDir)) {
        return @()
    }
    $dirs = @(Get-ChildItem -LiteralPath $VersionsDir -Directory -ErrorAction SilentlyContinue | Sort-Object Name)
    if ($Mc -and $Mc.Count -gt 0) {
        $dirs = @($dirs | Where-Object { $Mc -contains $_.Name })
    }
    return $dirs
}

function Get-LoaderFolders {
    <# Any top-level folder with settings.gradle + gradlew, same rule as build-all.ps1/release.ps1/CI. #>
    param([Parameter(Mandatory)][string]$McDir, [string[]]$Loader)
    $found = Get-ChildItem -LiteralPath $McDir -Directory -ErrorAction SilentlyContinue |
        Where-Object {
            (Test-Path (Join-Path $_.FullName 'settings.gradle')) -and (Test-Path (Join-Path $_.FullName 'gradlew'))
        } |
        Sort-Object Name |
        ForEach-Object { $_.Name }
    if ($Loader -and $Loader.Count -gt 0) {
        $found = $found | Where-Object { $Loader -contains $_ }
    }
    return @($found)
}

function Test-HasGametest {
    param([Parameter(Mandatory)][string]$LoaderDir)
    return Test-Path (Join-Path $LoaderDir 'src/gametest')
}

function Test-HasBackpackIntegration {
    <# SB availability contract: a META-INF/services registration for IBackpackIntegration under this loader's
       own resources - see docs/ARCHITECTURE.md "Optional integration: Sophisticated Backpacks". #>
    param([Parameter(Mandatory)][string]$LoaderDir)
    $svc = Join-Path $LoaderDir 'src/main/resources/META-INF/services/sophisticated.building.platform.services.IBackpackIntegration'
    return Test-Path -LiteralPath $svc
}

function Test-ModListedInLog {
    <#
        True if $ModId shows up in the loader's own mod-list log lines. Covers both formats seen on the 1.21.1
        branch:
          - Fabric Loader: "Loading N mods:" followed by "\t- <modid> <version>" lines
          - FML (NeoForge/Forge) ModDiscoverer: "Name Version (<modid>)" lines
    #>
    param([Parameter(Mandatory)][string]$LogContent, [Parameter(Mandatory)][string]$ModId)
    if ($LogContent -match "(?m)^\s*-\s*$([regex]::Escape($ModId))\b") { return $true }
    if ($LogContent -match "\($([regex]::Escape($ModId))\)") { return $true }
    return $false
}

# ---------------------------------------------------------------------------------------------------------------
# JUnit XML parsing (build/test-results/test/*.xml and the fabric gametest report).
# ---------------------------------------------------------------------------------------------------------------

function Get-JUnitSummary {
    <# Sums tests/failures/errors/skipped across every matching *.xml under $ResultsDir. #>
    param([Parameter(Mandatory)][string]$ResultsDir)
    $summary = [pscustomobject]@{ Tests = 0; Failures = 0; Errors = 0; Skipped = 0; FileCount = 0 }
    if (-not (Test-Path $ResultsDir)) { return $summary }
    $files = Get-ChildItem -LiteralPath $ResultsDir -Filter '*.xml' -File -ErrorAction SilentlyContinue
    foreach ($file in $files) {
        try {
            [xml]$xml = Get-Content -LiteralPath $file.FullName -Raw
        } catch {
            continue
        }
        # XPath instead of dot-property access: under Set-StrictMode, "$xml.testsuites" throws
        # PropertyNotFoundException when the root element is actually <testsuite> (no wrapping <testsuites>),
        # which is exactly the shape Gradle's own JUnit XML report writer uses.
        $suites = @($xml.SelectNodes('//testsuite'))
        foreach ($suite in $suites) {
            $summary.Tests += [int]($suite.tests ?? 0)
            $summary.Failures += [int]($suite.failures ?? 0)
            $summary.Errors += [int]($suite.errors ?? 0)
            $summary.Skipped += [int]($suite.skipped ?? 0)
            $summary.FileCount++
        }
    }
    return $summary
}

function Get-JUnitTestNames {
    <# classname+name of every <testcase> in one JUnit XML file (used for the "Backpack" gametest filter). #>
    param([Parameter(Mandatory)][string]$XmlPath)
    if (-not (Test-Path -LiteralPath $XmlPath)) { return @() }
    try {
        [xml]$xml = Get-Content -LiteralPath $XmlPath -Raw
    } catch {
        return @()
    }
    $cases = @($xml.SelectNodes('//testcase'))
    return $cases | ForEach-Object {
        [pscustomobject]@{
            ClassName = [string]$_.classname
            Name      = [string]$_.name
            Failed    = ($null -ne $_.SelectSingleNode('failure'))
            Errored   = ($null -ne $_.SelectSingleNode('error'))
        }
    }
}

function Get-CrashReportClassification {
    <#
        Classifies a crash report's content as 'known-upstream' (a specific, previously-confirmed Forge-only
        engine race with no Sophisticated Building frames anywhere in it - see docs/TESTING.md "process
        safety") or 'unclassified' (anything else, including a crash that merely LOOKS similar but does
        reference the mod - never auto-classify one of those away). Only ever downgrades a crash from "fail"
        to "warn" when BOTH the exact signature matches AND no sophisticated.building/sophisticatedbuilding
        frame appears anywhere in the report - matching the signature alone is not enough.
    #>
    param([Parameter(Mandatory)][AllowEmptyString()][string]$CrashReportContent)
    if (-not $CrashReportContent) { return 'unclassified' }

    # Only the crash/stack-trace portion counts for "no mod frames" - a Minecraft crash report's
    # "-- System Details --" footer lists every loaded mod (and "Active/Available Data Packs" lists every
    # data pack a mod contributes) regardless of whether that mod had anything to do with the actual crash, so
    # searching the WHOLE file would find "sophisticatedbuilding" in the mod list of nearly every crash on this
    # branch and never classify anything - confirmed against a real saved crash report, which mentions
    # sophisticatedbuilding twice in "Active/Available Data Packs" and once in the System Details mod table,
    # with zero sophisticated.building stack frames anywhere in the actual trace.
    $systemDetailsMarker = [regex]::Match($CrashReportContent, '(?m)^-- System Details --\s*$')
    $traceSection = if ($systemDetailsMarker.Success) { $CrashReportContent.Substring(0, $systemDetailsMarker.Index) } else { $CrashReportContent }

    $isKnownForgeSignature = $traceSection -match 'IllegalStateException: Can not retrieve LootModifierManager until resources have loaded once' -and
        $traceSection -match 'ForgeInternalHandler\.getLootModifierManager'
    $mentionsMod = $traceSection -match 'sophisticatedbuilding|sophisticated\.building'
    if ($isKnownForgeSignature -and -not $mentionsMod) {
        return 'known-upstream'
    }
    return 'unclassified'
}

function Get-FreeTcpPort {
    <# A random free TCP port on loopback, so the server stage never collides with another Minecraft server
       already listening on 25565 - a real risk on a machine where other agents/processes may be running their
       own dev servers at the same time. Small inherent TOCTOU race (freed right before Minecraft binds it),
       but far safer than always using the fixed default port. #>
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try { return $listener.LocalEndpoint.Port } finally { $listener.Stop() }
}

function Set-ServerPort {
    <# Ensures <runDir>/server.properties pins server-port to $Port, preserving every other existing line
       (or creating a minimal file - the dedicated server fills in the rest of the defaults for any key that's
       still missing). #>
    param([Parameter(Mandatory)][string]$RunDir, [Parameter(Mandatory)][int]$Port)
    $propsPath = Join-Path $RunDir 'server.properties'
    $lines = @()
    if (Test-Path -LiteralPath $propsPath) {
        $lines = @(Get-Content -LiteralPath $propsPath | Where-Object { $_ -notmatch '^server-port=' })
    }
    $lines += "server-port=$Port"
    Set-Content -LiteralPath $propsPath -Value $lines -Encoding utf8
}

function Test-HasSmokeClientTask {
    <# Cheap, side-effect-free heuristic (no Gradle invocation) for whether this loader's build defines a
       runSmokeClient task, used to skip the plain "does it boot and join" client stage once the richer smoke
       harness already covers that ground - avoids launching a second, redundant Minecraft client window just
       to re-check what runSmokeClient's own join already verifies. Text search rather than an actual
       `gradlew tasks` probe: cheap, and a false negative (misses a dynamically-registered task) only costs a
       redundant-but-harmless plain client run, which is exactly the documented fallback behavior; a false
       positive is very unlikely since nothing else would have a reason to mention that exact task name. #>
    param([Parameter(Mandatory)][string]$LoaderDir)
    $buildFiles = @(Get-ChildItem -LiteralPath $LoaderDir -Filter '*.gradle' -File -ErrorAction SilentlyContinue)
    foreach ($f in $buildFiles) {
        try {
            if ((Get-Content -LiteralPath $f.FullName -Raw -ErrorAction SilentlyContinue) -match 'runSmokeClient') {
                return $true
            }
        } catch {}
    }
    return $false
}

function Test-GradleTaskMissing {
    <# Detects Gradle's own "no such task" failure in a captured log, so the smoke stage can tell "harness not
       added yet" apart from "harness ran and failed". #>
    param([Parameter(Mandatory)][string]$LogContent, [Parameter(Mandatory)][string]$TaskName)
    if ($LogContent -match "Task '$([regex]::Escape($TaskName))' not found") { return $true }
    if ($LogContent -match 'Cannot locate tasks? that match') { return $true }
    return $false
}

# ---------------------------------------------------------------------------------------------------------------
# Misc helpers
# ---------------------------------------------------------------------------------------------------------------

function Get-JsonProp {
    <# Safe property read for a ConvertFrom-Json result: under Set-StrictMode, "$obj.name" throws
       PropertyNotFoundException if that key isn't present in the JSON (e.g. an incomplete/older
       smoketest-result.json). Reading through the Properties collection instead never throws - a missing key
       just yields $Default. #>
    param($Obj, [Parameter(Mandatory)][string]$Name, $Default = $null)
    if ($null -eq $Obj) { return $Default }
    $prop = $Obj.PSObject.Properties[$Name]
    if ($null -eq $prop) { return $Default }
    return $prop.Value
}

function Copy-IfExists {
    param([string]$Path, [string]$Destination)
    if ($Path -and (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path (Split-Path -Parent $Destination) -Force | Out-Null
        Copy-Item -LiteralPath $Path -Destination $Destination -Force -Recurse -ErrorAction SilentlyContinue
        return $true
    }
    return $false
}

function New-StageResult {
    param(
        [string]$Mc, [string]$Loader, [string]$Stage, [string]$Result, [string]$Detail = '',
        [double]$DurationSeconds = 0, [string]$LogPath = '', [hashtable]$Extra = $null
    )
    $obj = [ordered]@{
        mc              = $Mc
        loader          = $Loader
        stage           = $Stage
        result          = $Result          # pass | fail | warn | n/a
        detail          = $Detail
        durationSeconds = [Math]::Round($DurationSeconds, 1)
        logPath         = $LogPath
    }
    if ($Extra) {
        foreach ($key in $Extra.Keys) { $obj[$key] = $Extra[$key] }
    }
    return [pscustomobject]$obj
}
