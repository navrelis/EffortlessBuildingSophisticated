#requires -Version 7.0
<#
.SYNOPSIS
    Downloads (or verifies) the Sophisticated Backpacks + Sophisticated Core jars listed in
    upstream/manifest.json into upstream/<loader>/<mc>/, exactly as CurseForge names them.

.DESCRIPTION
    Reads upstream/manifest.json (generated alongside this script) and, for every manifest
    entry, makes sure both the Backpacks jar and (when present) the matching Core jar exist
    on disk with the expected file size and SHA1. Missing or hash-mismatched files are
    (re-)downloaded from CurseForge. Existing files whose size+SHA1 already match are left
    untouched, so re-running this script with no changes does nothing (idempotent).

.PARAMETER Loader
    Only process entries for this loader: forge, neoforge, or fabric. Default: all.

.PARAMETER Mc
    Only process entries for this exact Minecraft version string (e.g. "1.21.1"). Default: all.

.PARAMETER Force
    Re-download every matched jar even if it already exists with the correct size/SHA1.

.EXAMPLE
    pwsh upstream/fetch-upstream.ps1
    Downloads anything missing, verifies everything else.

.EXAMPLE
    pwsh upstream/fetch-upstream.ps1 -Loader fabric -Mc 1.21.1 -Force
    Forces a fresh re-download of the fabric/1.21.1 backpacks + core jars.
#>
[CmdletBinding()]
param(
    [ValidateSet("forge", "neoforge", "fabric")]
    [string]$Loader,

    [string]$Mc,

    [switch]$Force
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$manifestPath = Join-Path $scriptDir "manifest.json"

if (-not (Test-Path $manifestPath)) {
    Write-Error "Manifest not found at '$manifestPath'. Nothing to fetch."
    exit 1
}

try {
    $manifest = Get-Content -Raw -Path $manifestPath | ConvertFrom-Json
} catch {
    Write-Error "Failed to parse manifest.json: $_"
    exit 1
}

$userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"

function Get-Sha1([string]$Path) {
    return (Get-FileHash -Path $Path -Algorithm SHA1).Hash.ToLowerInvariant()
}

function Test-FileMatches([string]$Path, [long]$ExpectedSize, [string]$ExpectedSha1) {
    if (-not (Test-Path $Path)) { return $false }
    $actualSize = (Get-Item $Path).Length
    if ($actualSize -ne $ExpectedSize) { return $false }
    $actualSha1 = Get-Sha1 -Path $Path
    return ($actualSha1 -eq $ExpectedSha1.ToLowerInvariant())
}

function Invoke-DownloadFile([int]$Project, [long]$FileId, [string]$Dest, [long]$ExpectedSize, [string]$ExpectedSha1) {
    $url = "https://www.curseforge.com/api/v1/mods/$Project/files/$FileId/download"
    $destDir = Split-Path -Parent $Dest
    if (-not (Test-Path $destDir)) {
        New-Item -ItemType Directory -Force -Path $destDir | Out-Null
    }

    $maxAttempts = 3
    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
        Write-Host "    downloading (attempt $attempt/$maxAttempts): $url"
        try {
            Invoke-WebRequest -Uri $url -OutFile $Dest -UserAgent $userAgent -MaximumRedirection 5 | Out-Null
        } catch {
            Write-Warning "    download request failed: $_"
            Start-Sleep -Seconds 2
            continue
        }

        if (-not (Test-Path $Dest)) {
            Write-Warning "    download did not produce a file"
            Start-Sleep -Seconds 2
            continue
        }

        if (Test-FileMatches -Path $Dest -ExpectedSize $ExpectedSize -ExpectedSha1 $ExpectedSha1) {
            return $true
        } else {
            $actualSize = (Get-Item $Dest).Length
            Write-Warning "    verification failed after download (size $actualSize, expected $ExpectedSize); retrying"
            Remove-Item -Force -Path $Dest -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

function Sync-Artifact([string]$Kind, [PSCustomObject]$FileInfo, [string]$DestDir) {
    # $Kind is "backpacks" or "core" only used for logging.
    $dest = Join-Path $DestDir $FileInfo.fileName
    $needsDownload = $Force -or -not (Test-FileMatches -Path $dest -ExpectedSize $FileInfo.size -ExpectedSha1 $FileInfo.sha1)

    if (-not $needsDownload) {
        Write-Host "  [ok]       $Kind already present and verified: $($FileInfo.fileName)"
        return [PSCustomObject]@{ Status = "ok"; File = $FileInfo.fileName }
    }

    if (Test-Path $dest) {
        Write-Host "  [mismatch] $Kind exists but size/sha1 differ (or -Force set); re-downloading: $($FileInfo.fileName)"
    } else {
        Write-Host "  [missing]  $Kind not present; downloading: $($FileInfo.fileName)"
    }

    $ok = Invoke-DownloadFile -Project $FileInfo.project -FileId $FileInfo.fileId -Dest $dest -ExpectedSize $FileInfo.size -ExpectedSha1 $FileInfo.sha1
    if ($ok) {
        Write-Host "  [fetched]  $Kind verified after download: $($FileInfo.fileName)"
        return [PSCustomObject]@{ Status = "fetched"; File = $FileInfo.fileName }
    } else {
        Write-Error "  [FAILED]   $Kind could not be downloaded/verified: $($FileInfo.fileName)"
        return [PSCustomObject]@{ Status = "failed"; File = $FileInfo.fileName }
    }
}

$entries = $manifest.entries
if ($Loader) { $entries = $entries | Where-Object { $_.loader -eq $Loader } }
if ($Mc) { $entries = $entries | Where-Object { $_.mc -eq $Mc } }

if (-not $entries -or $entries.Count -eq 0) {
    Write-Warning "No manifest entries matched -Loader '$Loader' -Mc '$Mc'."
    exit 1
}

Write-Host "Processing $($entries.Count) manifest entr$(if ($entries.Count -eq 1) {'y'} else {'ies'})..."
Write-Host ""

$results = @()
foreach ($entry in $entries) {
    $destDir = Join-Path $scriptDir (Join-Path $entry.loader $entry.mc)
    Write-Host "== $($entry.loader) / $($entry.mc) =="

    $results += Sync-Artifact -Kind "backpacks" -FileInfo $entry.backpacks -DestDir $destDir

    if ($entry.core) {
        $results += Sync-Artifact -Kind "core" -FileInfo $entry.core -DestDir $destDir
    } else {
        Write-Host "  [n/a]      no separate Sophisticated Core file for this pair (see manifest warnings)"
    }
    Write-Host ""
}

$failed = $results | Where-Object { $_.Status -eq "failed" }
$fetched = $results | Where-Object { $_.Status -eq "fetched" }
$ok = $results | Where-Object { $_.Status -eq "ok" }

Write-Host "Summary: $($ok.Count) already OK, $($fetched.Count) (re)downloaded, $($failed.Count) failed."

if ($failed.Count -gt 0) {
    Write-Error "$($failed.Count) file(s) failed to download/verify. See output above."
    exit 1
}

exit 0
