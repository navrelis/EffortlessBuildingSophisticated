#requires -Version 7.0
<#
.SYNOPSIS
    Mutes a Minecraft dev run and moves its window to the second monitor once it appears.

.DESCRIPTION
    A thin CLI wrapper around scripts/lib/GameWindow.ps1, meant to be started in the background right after
    launching a `gradlew runClient`/`runSmokeClient`-style process: it mutes -RunDir's options.txt FIRST (call
    this before the client actually starts reading options.txt, i.e. right after starting the Gradle process,
    not after), then watches -GradlePid's own process tree until a top-level window shows up and moves it to
    the first non-primary monitor (SWP_NOACTIVATE - never steals focus). Never touches any other window or
    process. If there's only one monitor, or the window never appears within -TimeoutSeconds, this exits
    cleanly (warning only, no error) - it never fails the caller's build.

.PARAMETER RunDir
    The dev run directory whose options.txt gets muted (e.g. <loader>/run).

.PARAMETER GradlePid
    Process id of the Gradle process this script's own caller started (e.g. the tracked cmd.exe/gradlew.bat
    process) - its descendant java.exe is where the actual game window lives.

.PARAMETER TimeoutSeconds
    How long to wait for the window to appear before giving up. Default: 180.

.EXAMPLE
    pwsh scripts/move-game-window.ps1 -RunDir fabric/run -GradlePid 12345
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$RunDir,
    [Parameter(Mandatory)][int]$GradlePid,
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib/GameWindow.ps1')

Set-MinecraftMuted -RunDir $RunDir
Move-MinecraftWindowToSecondary -ProcessId $GradlePid -TimeoutSeconds $TimeoutSeconds | Out-Null
