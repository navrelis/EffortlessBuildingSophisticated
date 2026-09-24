#requires -Version 7.0
<#
    The machine-wide "one game window at a time" lock (.knowledge/port-brief.md, "One game window at a time"),
    shared with the agents that start clients by hand: local/game-window.lock under the repo root.

      - The lock is held while its file exists. A file whose LastWriteTime is older than the stale limit
        (20 minutes) is a leftover of a crashed holder and is deleted.
      - It is taken by creating the file atomically (New-Item -ErrorAction Stop fails if it already exists), with
        "<owner> <ISO time>" as content; if that fails, someone else was faster and the caller waits again.
      - The holder deletes it as soon as its client has exited or was killed, also on failure (the callers do
        that in a finally block). While held, a background job touches the file every minute so a long client
        stage (up to -TimeoutMinutes) is never mistaken for a stale lock.
      - Exit-GameWindowLock only deletes the file if it still carries this holder's own content, never another
        holder's lock.
#>

Set-StrictMode -Version Latest

$script:GameWindowLockStaleMinutes = 20

function Enter-GameWindowLock {
    <# Waits until the lock is free (or stale), takes it and returns a handle for Exit-GameWindowLock. Returns
       $null if it could not be taken within -TimeoutMinutes. -PollSeconds / -StaleMinutes / -KeepAliveSeconds
       exist for the offline tests; the defaults are the port-brief rules. #>
    param(
        [Parameter(Mandatory)][string]$LockPath,
        [Parameter(Mandatory)][string]$Owner,
        [double]$TimeoutMinutes = 60,
        [double]$PollSeconds = 30,
        [double]$StaleMinutes = $script:GameWindowLockStaleMinutes,
        [double]$KeepAliveSeconds = 60
    )
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    $announced = $false
    New-Item -ItemType Directory -Path (Split-Path -Parent $LockPath) -Force | Out-Null
    while ($true) {
        if (Test-Path -LiteralPath $LockPath) {
            $age = (Get-Date) - (Get-Item -LiteralPath $LockPath -ErrorAction SilentlyContinue).LastWriteTime
            if ($age.TotalMinutes -ge $StaleMinutes) {
                Write-Host "  window lock is stale ($([int]$age.TotalMinutes) min old): removing it"
                Remove-Item -LiteralPath $LockPath -Force -ErrorAction SilentlyContinue
                continue
            }
        } else {
            $content = "$Owner $((Get-Date).ToString('o'))"
            try {
                New-Item -ItemType File -Path $LockPath -Value $content -ErrorAction Stop | Out-Null
                $keepAlive = Start-Job -ScriptBlock {
                    param($Path, $Content, $IntervalSeconds)
                    while ($true) {
                        Start-Sleep -Seconds $IntervalSeconds
                        # Only ever refresh our own lock; stop if it was removed or taken over.
                        $current = Get-Content -LiteralPath $Path -Raw -ErrorAction SilentlyContinue
                        if (-not $current -or $current.Trim() -ne $Content) { break }
                        (Get-Item -LiteralPath $Path).LastWriteTime = Get-Date
                    }
                } -ArgumentList $LockPath, $content, $KeepAliveSeconds
                return [pscustomobject]@{ Path = $LockPath; Content = $content; KeepAlive = $keepAlive }
            } catch {
                # Someone else created it between the check and New-Item: wait like for any held lock.
            }
        }
        if ((Get-Date) -ge $deadline) { return $null }
        if (-not $announced) {
            $holder = Get-Content -LiteralPath $LockPath -Raw -ErrorAction SilentlyContinue
            Write-Host "  waiting for the game window lock ($LockPath, held by: $("$holder".Trim()))"
            $announced = $true
        }
        Start-Sleep -Seconds $PollSeconds
    }
}

function Exit-GameWindowLock {
    <# Releases a handle from Enter-GameWindowLock: stops its keep-alive job and deletes the lock file if it still
       carries this handle's content. Safe to call twice and with $null. #>
    param($Handle)
    if (-not $Handle) { return }
    if ($Handle.KeepAlive) {
        Stop-Job -Job $Handle.KeepAlive -ErrorAction SilentlyContinue | Out-Null
        Remove-Job -Job $Handle.KeepAlive -Force -ErrorAction SilentlyContinue | Out-Null
        $Handle.KeepAlive = $null
    }
    $current = Get-Content -LiteralPath $Handle.Path -Raw -ErrorAction SilentlyContinue
    if ($current -and $current.Trim() -eq $Handle.Content) {
        Remove-Item -LiteralPath $Handle.Path -Force -ErrorAction SilentlyContinue
    }
}
