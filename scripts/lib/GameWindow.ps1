#requires -Version 7.0
<#
    Dot-sourceable helpers so a Minecraft client/smoke window our own tooling starts doesn't steal the user's
    main monitor or make noise: mute it in its own dev run's options.txt before launch, and move its window to
    the first non-primary monitor (without stealing focus) once it appears.

    Scope, on purpose: Move-MinecraftWindowToSecondary only ever enumerates and moves a window that belongs to
    a process in the given PID's OWN descendant tree (the java.exe the tracked Gradle process eventually spawns)
    - never any other window on the machine, and never anything found by name/title matching. It never throws
    if the window doesn't show up in time; it just returns $false so the caller can carry on.
#>

Set-StrictMode -Version Latest

if (-not ('TestAllVersions.Win32' -as [type])) {
    Add-Type -Namespace TestAllVersions -Name Win32 -MemberDefinition @'
[DllImport("user32.dll")] public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
[DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);
[DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
[DllImport("user32.dll")] public static extern int GetWindowTextLength(IntPtr hWnd);
[DllImport("user32.dll")] public static extern IntPtr GetWindow(IntPtr hWnd, uint uCmd);
[DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);
public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
'@
}

# GW_OWNER: a top-level window (Minecraft's main window) has no owner.
$script:GW_OWNER = 4
# Reposition only (keep current size and z-order), and never steal focus/activation from the user.
$script:SWP_NOSIZE = 0x0001
$script:SWP_NOZORDER = 0x0004
$script:SWP_NOACTIVATE = 0x0010

function Set-MinecraftMuted {
    <# Ensures <RunDir>/options.txt sets the master volume to 0 (soundCategory_master:0.0) and disables
       pause-on-lost-focus (pauseOnLostFocus:false), preserving every other existing line, so a client/smoke
       window this tooling starts never makes noise and keeps ticking once it's moved to the second monitor
       without activating it (SWP_NOACTIVATE - the window is never focused, and Minecraft pauses an unfocused
       window by default, which would otherwise stall world tick and the join/wait checks that depend on it).
       Creates the file if it doesn't exist yet (Minecraft fills in the rest of the defaults on first real boot). #>
    param([Parameter(Mandatory)][string]$RunDir)
    New-Item -ItemType Directory -Path $RunDir -Force | Out-Null
    $optionsPath = Join-Path $RunDir 'options.txt'
    $lines = @()
    if (Test-Path -LiteralPath $optionsPath) {
        $lines = @(Get-Content -LiteralPath $optionsPath | Where-Object { $_ -notmatch '^(soundCategory_master|pauseOnLostFocus):' })
    }
    $lines += 'soundCategory_master:0.0'
    $lines += 'pauseOnLostFocus:false'
    Set-Content -LiteralPath $optionsPath -Value $lines -Encoding utf8
}

function Get-DescendantProcessIds {
    <# BFS over Win32_Process ParentProcessId links, starting at $RootProcessId. Used to find the java.exe the
       tracked Gradle process (cmd.exe -> gradlew.bat -> java.exe) eventually spawns - never touches anything
       outside this one process's own descendant tree. #>
    param([Parameter(Mandatory)][int]$RootProcessId)
    $all = @(Get-CimInstance Win32_Process -Property ProcessId, ParentProcessId, Name -ErrorAction SilentlyContinue)
    $byParent = @{}
    foreach ($p in $all) {
        $key = [string]$p.ParentProcessId
        if (-not $byParent.ContainsKey($key)) { $byParent[$key] = [System.Collections.Generic.List[object]]::new() }
        $byParent[$key].Add($p)
    }
    $result = [System.Collections.Generic.List[int]]::new()
    $queue = [System.Collections.Generic.Queue[int]]::new()
    $queue.Enqueue($RootProcessId)
    $result.Add($RootProcessId)
    while ($queue.Count -gt 0) {
        $current = $queue.Dequeue()
        $children = $byParent[[string]$current]
        if (-not $children) { continue }
        foreach ($c in $children) {
            $cid = [int]$c.ProcessId
            if (-not $result.Contains($cid)) {
                $result.Add($cid)
                $queue.Enqueue($cid)
            }
        }
    }
    return $result
}

function Get-FirstNonPrimaryScreen {
    Add-Type -AssemblyName System.Windows.Forms -ErrorAction SilentlyContinue
    $screens = [System.Windows.Forms.Screen]::AllScreens
    if ($screens.Count -le 1) { return $null }
    return @($screens | Where-Object { -not $_.Primary })[0]
}

function Move-MinecraftWindowToSecondary {
    <#
        Waits (up to $TimeoutSeconds) for a visible, top-level, owner-less window belonging to a descendant
        process of $ProcessId, then moves it (SetWindowPos, SWP_NOACTIVATE|SWP_NOZORDER|SWP_NOSIZE - keeps
        current size, never steals focus) to the first non-primary monitor's top-left + 40px. Returns $true on
        success, $false (with a warning, never a throw) if there's only one monitor, or the window never shows
        up in time.
    #>
    param([Parameter(Mandatory)][int]$ProcessId, [int]$TimeoutSeconds = 180)

    $screen = Get-FirstNonPrimaryScreen
    if (-not $screen) {
        Write-Warning 'Move-MinecraftWindowToSecondary: only one monitor detected - leaving the window where it is.'
        return $false
    }
    $targetX = $screen.Bounds.X + 40
    $targetY = $screen.Bounds.Y + 40

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $pids = @(Get-DescendantProcessIds -RootProcessId $ProcessId)
        $pidSet = [System.Collections.Generic.HashSet[uint32]]::new()
        foreach ($p in $pids) { [void]$pidSet.Add([uint32]$p) }

        $foundHwnd = [IntPtr]::Zero
        $callback = [TestAllVersions.Win32+EnumWindowsProc]{
            param($hWnd, $lParam)
            if (-not [TestAllVersions.Win32]::IsWindowVisible($hWnd)) { return $true }
            if ([TestAllVersions.Win32]::GetWindowTextLength($hWnd) -eq 0) { return $true }
            if ([TestAllVersions.Win32]::GetWindow($hWnd, $script:GW_OWNER) -ne [IntPtr]::Zero) { return $true }
            $owningPid = 0
            [void][TestAllVersions.Win32]::GetWindowThreadProcessId($hWnd, [ref]$owningPid)
            if ($pidSet.Contains([uint32]$owningPid)) {
                $script:_foundHwnd = $hWnd
                return $false   # stop enumerating
            }
            return $true
        }
        $script:_foundHwnd = [IntPtr]::Zero
        [void][TestAllVersions.Win32]::EnumWindows($callback, [IntPtr]::Zero)
        $foundHwnd = $script:_foundHwnd

        if ($foundHwnd -ne [IntPtr]::Zero) {
            $flags = $script:SWP_NOSIZE -bor $script:SWP_NOZORDER -bor $script:SWP_NOACTIVATE
            $moved = [TestAllVersions.Win32]::SetWindowPos($foundHwnd, [IntPtr]::Zero, $targetX, $targetY, 0, 0, $flags)
            if ($moved) {
                Write-Host "Move-MinecraftWindowToSecondary: moved window of PID tree $ProcessId to ($targetX, $targetY) on $($screen.DeviceName)."
                return $true
            } else {
                Write-Warning "Move-MinecraftWindowToSecondary: found a window but SetWindowPos failed for PID tree $ProcessId."
                return $false
            }
        }
        Start-Sleep -Milliseconds 500
    }
    Write-Warning "Move-MinecraftWindowToSecondary: no window appeared for PID tree $ProcessId within $TimeoutSeconds s."
    return $false
}
