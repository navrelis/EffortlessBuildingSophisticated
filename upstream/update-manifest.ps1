#requires -Version 7.0
<#
.SYNOPSIS
    Re-discovers the newest Sophisticated Backpacks / Sophisticated Core RELEASE files per
    (loader, Minecraft version) from CurseForge and diffs them against upstream/manifest.json.

.DESCRIPTION
    Pages through the CurseForge website API for the same four project ids manifest.json already
    tracks - Sophisticated Backpacks official (422301), Sophisticated Core official (618298),
    the unofficial Fabric port of Backpacks (979322), and of Core (979317) - using
    https://www.curseforge.com/api/v1/mods/<project>/files?pageIndex=N&pageSize=50&sort=dateCreated&sortDescending=true
    with a browser User-Agent (same approach fetch-upstream.ps1 uses). For every (loader, game
    version) pair it finds a releaseType 1 (RELEASE) file for, it keeps the newest one. A file's
    loader is read from its gameVersions tags (Forge / NeoForge / Fabric); a file with none of
    those tags is treated as Forge, matching CurseForge's convention from before NeoForge existed.
    Fabric-port files are always loader "fabric" (both port projects are Fabric-only).

    For each (loader, mc) pair, if the discovered Backpacks file id already matches the current
    manifest entry, that entry is kept as-is (no download). Otherwise the Backpacks jar is
    downloaded (into upstream/<loader>/<mc>/, the same place fetch-upstream.ps1 keeps it, so nothing
    is fetched twice) to compute its SHA1 and read its declared `sophisticatedcore` dependency range
    (from META-INF/mods.toml, META-INF/neoforge.mods.toml, or fabric.mod.json, depending on loader),
    then the newest Core RELEASE tagged for that same (loader, mc) pair is picked if it satisfies
    that range - otherwise the newest available Core for that pair is used anyway and a warning is
    recorded, mirroring how the existing manifest already documents the neoforge 26.1 / 26.1.1 gap.

    Dry run by default: prints a diff summary (new Minecraft versions, new/changed files) against
    the current manifest.json and changes nothing. Pass -Write to actually update manifest.json.

.PARAMETER Write
    Write the recomputed manifest.json instead of only reporting a diff.

.PARAMETER PageSize
    CurseForge files-per-page for discovery. Default 50 (matches the URL pattern documented above).

.EXAMPLE
    pwsh upstream/update-manifest.ps1
    Dry run: reports what would change without touching manifest.json or downloading anything
    beyond what's needed to compute that diff.

.EXAMPLE
    pwsh upstream/update-manifest.ps1 -Write
    Recomputes and writes upstream/manifest.json.
#>
[CmdletBinding()]
param(
    [switch]$Write,
    [int]$PageSize = 50
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$manifestPath = Join-Path $scriptDir "manifest.json"
$userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"

$sourceProjects = [ordered]@{
    sophisticatedBackpacksOfficial = [ordered]@{ id = 422301; loaders = @("forge", "neoforge"); url = "https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks"; author = "P3pp3rF1y" }
    sophisticatedCoreOfficial      = [ordered]@{ id = 618298; loaders = @("forge", "neoforge"); url = "https://www.curseforge.com/minecraft/mc-mods/sophisticated-core"; author = "P3pp3rF1y" }
    sophisticatedBackpacksFabric   = [ordered]@{ id = 979322; loaders = @("fabric"); url = "https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks-unofficial-fabric-port"; author = "salandora"; note = "Unofficial Fabric port" }
    sophisticatedCoreFabric        = [ordered]@{ id = 979317; loaders = @("fabric"); url = "https://www.curseforge.com/minecraft/mc-mods/sophisticated-core-unofficial-fabric-port"; author = "salandora"; note = "Unofficial Fabric port" }
}

# curse.maven coordinate slugs are stable per project (derived from the mod's CurseForge URL slug).
$curseSlug = @{
    422301 = "sophisticated-backpacks"
    618298 = "sophisticated-core"
    979322 = "sophisticated-backpacks-unofficial-fabric-port"
    979317 = "sophisticated-core-unofficial-fabric-port"
}

# ---------------------------------------------------------------------------------------------
# CurseForge discovery
# ---------------------------------------------------------------------------------------------

function Get-AllReleaseFiles {
    param([int]$ProjectId)

    $files = New-Object System.Collections.Generic.List[object]
    $pageIndex = 0
    while ($true) {
        $uri = "https://www.curseforge.com/api/v1/mods/$ProjectId/files?pageIndex=$pageIndex&pageSize=$PageSize&sort=dateCreated&sortDescending=true"
        $resp = Invoke-WebRequest -Uri $uri -UserAgent $userAgent -TimeoutSec 30
        $json = $resp.Content | ConvertFrom-Json

        if (-not $json.data -or $json.data.Count -eq 0) { break }
        foreach ($f in $json.data) {
            if ($f.releaseType -eq 1) { $files.Add($f) }
        }

        $pageIndex++
        $seen = $pageIndex * $PageSize
        if ($seen -ge $json.pagination.totalCount) { break }
        Start-Sleep -Milliseconds 150
    }
    Write-Host "  project $ProjectId : $($files.Count) RELEASE file(s) found"
    return $files
}

# Builds a map "loader|mc" -> newest file, from a raw file list. $ForcedLoader, when set, skips
# tag-based loader detection (used for the Fabric-only port projects).
function Build-NewestMap {
    param(
        [System.Collections.Generic.List[object]]$Files,
        [string]$ForcedLoader
    )

    $map = @{}
    foreach ($f in $Files) {
        $tags = @($f.gameVersions)

        # A file can be tagged for more than one loader at once (e.g. old Forge/NeoForge 1.20.1
        # files that predate the two loaders splitting apart) - record it under every loader tag
        # it actually carries, not just one.
        $loaders = @()
        if ($ForcedLoader) {
            $loaders = @($ForcedLoader)
        } else {
            if ($tags -contains "Forge") { $loaders += "forge" }
            if ($tags -contains "NeoForge") { $loaders += "neoforge" }
            if ($loaders.Count -eq 0) {
                # Files without a loader tag predate NeoForge's split from Forge on CurseForge.
                $loaders = @("forge")
            }
        }

        $mcTags = $tags | Where-Object { $_ -match '^[0-9]+(\.[0-9]+){1,2}$' }
        foreach ($loader in $loaders) {
            foreach ($mc in $mcTags) {
                $key = "$loader|$mc"
                $existing = $map[$key]
                if (-not $existing -or ([datetime]$f.dateCreated) -gt ([datetime]$existing.dateCreated)) {
                    $map[$key] = $f
                }
            }
        }
    }
    return $map
}

function Merge-Map {
    param([hashtable]$Into, [hashtable]$From)
    foreach ($k in $From.Keys) { $Into[$k] = $From[$k] }
    return $Into
}

# ---------------------------------------------------------------------------------------------
# Version range parsing - handles the two textual forms already used in manifest.json:
#   Maven-style intervals from mods.toml / neoforge.mods.toml, e.g. "[1.5.1,)", "[1.18.2-0.6.0,)"
#   Space-separated predicates from fabric.mod.json, e.g. ">=1.21.1-1.2.9.15 <1.22"
# Best-effort: an unparseable range returns $null (caller falls back to "newest available + warn").
# ---------------------------------------------------------------------------------------------

function Compare-DottedVersion {
    param([string]$A, [string]$B)

    $ta = ($A -replace '\.\+$', '') -split '[.\-]'
    $tb = ($B -replace '\.\+$', '') -split '[.\-]'
    $len = [Math]::Max($ta.Count, $tb.Count)
    for ($i = 0; $i -lt $len; $i++) {
        $sa = if ($i -lt $ta.Count) { $ta[$i] } else { "0" }
        $sb = if ($i -lt $tb.Count) { $tb[$i] } else { "0" }
        $na = 0; $nb = 0
        $aIsNum = [long]::TryParse($sa, [ref]$na)
        $bIsNum = [long]::TryParse($sb, [ref]$nb)
        if ($aIsNum -and $bIsNum) {
            if ($na -ne $nb) { return [Math]::Sign($na - $nb) }
        } else {
            $c = [string]::Compare($sa, $sb, [System.StringComparison]::Ordinal)
            if ($c -ne 0) { return [Math]::Sign($c) }
        }
    }
    return 0
}

function Test-VersionInRange {
    param([string]$Version, [string]$Range)

    if ([string]::IsNullOrWhiteSpace($Range)) { return $null }
    $Range = $Range.Trim()

    if ($Range -match '^([\[\(])\s*([^,\]\)]*)\s*,\s*([^,\]\)]*)\s*([\]\)])$') {
        $lowerIncl = ($matches[1] -eq '[')
        $lower = $matches[2].Trim()
        $upper = $matches[3].Trim()
        $upperIncl = ($matches[4] -eq ']')

        if ($lower) {
            $cmp = Compare-DottedVersion -A $Version -B $lower
            if ($lowerIncl) { if ($cmp -lt 0) { return $false } } else { if ($cmp -le 0) { return $false } }
        }
        if ($upper) {
            $cmp = Compare-DottedVersion -A $Version -B $upper
            if ($upperIncl) { if ($cmp -gt 0) { return $false } } else { if ($cmp -ge 0) { return $false } }
        }
        return $true
    }

    if ($Range -match '(>=|<=|>|<|=)') {
        $predicates = $Range -split '\s+' | Where-Object { $_ }
        foreach ($p in $predicates) {
            if ($p -notmatch '^(>=|<=|>|<|=)(.+)$') { return $null }
            $op = $matches[1]; $bound = $matches[2]
            $cmp = Compare-DottedVersion -A $Version -B $bound
            $ok = switch ($op) {
                ">=" { $cmp -ge 0 }
                "<=" { $cmp -le 0 }
                ">"  { $cmp -gt 0 }
                "<"  { $cmp -lt 0 }
                "="  { $cmp -eq 0 }
            }
            if (-not $ok) { return $false }
        }
        return $true
    }

    return $null
}

# ---------------------------------------------------------------------------------------------
# Jar inspection - reads a single entry out of a jar (zip) without extracting the rest.
# ---------------------------------------------------------------------------------------------

Add-Type -AssemblyName System.IO.Compression.FileSystem

function Get-JarEntryText {
    param([string]$JarPath, [string]$EntryName)

    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $EntryName } | Select-Object -First 1
        if (-not $entry) { return $null }
        $reader = New-Object System.IO.StreamReader($entry.Open())
        try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
    } finally {
        $zip.Dispose()
    }
}

function Get-RequiredCoreRange {
    param([string]$JarPath, [string]$Loader)

    try {
        if ($Loader -eq "fabric") {
            $text = Get-JarEntryText -JarPath $JarPath -EntryName "fabric.mod.json"
            if (-not $text) { return $null }
            $json = $text | ConvertFrom-Json
            $dep = $json.depends.sophisticatedcore
            if (-not $dep) { return $null }
            if ($dep -is [array]) { return ($dep -join " ") }
            return [string]$dep
        }

        $entryName = if ($Loader -eq "neoforge") { "META-INF/neoforge.mods.toml" } else { "META-INF/mods.toml" }
        $text = Get-JarEntryText -JarPath $JarPath -EntryName $entryName
        if (-not $text) { return $null }

        # Split into [[dependencies.<modid>]] blocks and find the one declaring sophisticatedcore.
        $blocks = $text -split '(?=\[\[dependencies\.)'
        foreach ($block in $blocks) {
            if ($block -match 'modId\s*=\s*"sophisticatedcore"' -and $block -match 'versionRange\s*=\s*"([^"]+)"') {
                return $matches[1]
            }
        }
        return $null
    } catch {
        Write-Warning "  could not read dependency range from $JarPath : $_"
        return $null
    }
}

function Get-Sha1 {
    param([string]$Path)
    return (Get-FileHash -Path $Path -Algorithm SHA1).Hash.ToLowerInvariant()
}

function Invoke-JarDownload {
    param([int]$ProjectId, [long]$FileId, [string]$Dest)

    $destDir = Split-Path -Parent $Dest
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Force -Path $destDir | Out-Null }
    if (Test-Path $Dest) { return }

    $url = "https://www.curseforge.com/api/v1/mods/$ProjectId/files/$FileId/download"
    Write-Host "  downloading: $url"
    Invoke-WebRequest -Uri $url -OutFile $Dest -UserAgent $userAgent -MaximumRedirection 5 | Out-Null
}

# ---------------------------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------------------------

if (-not (Test-Path $manifestPath)) {
    Write-Error "Manifest not found at '$manifestPath'."
    exit 1
}
$oldManifest = Get-Content -Raw -Path $manifestPath | ConvertFrom-Json
$oldEntries = @($oldManifest.entries)
$oldSkipped = @($oldManifest.skipped)

Write-Host "Discovering RELEASE files from CurseForge..."
$backpacksOfficialFiles = Get-AllReleaseFiles -ProjectId 422301
$coreOfficialFiles      = Get-AllReleaseFiles -ProjectId 618298
$backpacksFabricFiles   = Get-AllReleaseFiles -ProjectId 979322
$coreFabricFiles        = Get-AllReleaseFiles -ProjectId 979317

$backpacksMap = Build-NewestMap -Files $backpacksOfficialFiles
$backpacksMap = Merge-Map -Into $backpacksMap -From (Build-NewestMap -Files $backpacksFabricFiles -ForcedLoader "fabric")

$coreMap = Build-NewestMap -Files $coreOfficialFiles
$coreMap = Merge-Map -Into $coreMap -From (Build-NewestMap -Files $coreFabricFiles -ForcedLoader "fabric")

Write-Host ""
Write-Host "Building manifest entries for $($backpacksMap.Keys.Count) (loader, mc) pair(s)..."

$newEntries = New-Object System.Collections.Generic.List[object]
$warnings = New-Object System.Collections.Generic.List[string]
$newPairs = New-Object System.Collections.Generic.List[string]
$changedPairs = New-Object System.Collections.Generic.List[string]

foreach ($key in ($backpacksMap.Keys | Sort-Object)) {
    $loader, $mc = $key -split '\|', 2
    $bFile = $backpacksMap[$key]

    $oldEntry = $oldEntries | Where-Object { $_.loader -eq $loader -and $_.mc -eq $mc } | Select-Object -First 1

    if ($oldEntry -and $oldEntry.backpacks.fileId -eq $bFile.id) {
        $newEntries.Add($oldEntry)
        continue
    }

    if ($oldEntry) { $changedPairs.Add("$key (backpacks $($oldEntry.backpacks.fileId) -> $($bFile.id))") }
    else { $newPairs.Add($key) }

    $destDir = Join-Path $scriptDir (Join-Path $loader $mc)
    $bDest = Join-Path $destDir $bFile.fileName
    Invoke-JarDownload -ProjectId $bFile.projectId -FileId $bFile.id -Dest $bDest
    $bSize = (Get-Item $bDest).Length
    $bSha1 = Get-Sha1 -Path $bDest

    $range = Get-RequiredCoreRange -JarPath $bDest -Loader $loader

    $coreEntry = $null
    $coreFile = $coreMap["$loader|$mc"]

    if ($coreFile) {
        $satisfies = if ($range) { Test-VersionInRange -Version $coreFile.displayName -Range $range } else { $null }
        if ($range -and $satisfies -eq $false) {
            $warnings.Add("No Core release satisfies required range '$range' for $loader $mc; picked newest available Core instead: $($coreFile.fileName)")
        } elseif (-not $range) {
            $warnings.Add("Could not determine required Core range for $loader $mc (backpacks file $($bFile.fileName)); picked newest available Core instead: $($coreFile.fileName)")
        }

        $cDestDir = Join-Path $scriptDir (Join-Path $loader $mc)
        $cDest = Join-Path $cDestDir $coreFile.fileName
        Invoke-JarDownload -ProjectId $coreFile.projectId -FileId $coreFile.id -Dest $cDest
        $cSize = (Get-Item $cDest).Length
        $cSha1 = Get-Sha1 -Path $cDest

        $coreEntry = [ordered]@{
            project  = $coreFile.projectId
            fileId   = $coreFile.id
            fileName = $coreFile.fileName
            size     = $cSize
            sha1     = $cSha1
        }
    } else {
        $warnings.Add("No Core RELEASE available for $loader $mc. Sophisticated Core is bundled inside the Backpacks jar for this version (no separate dependency declared, predates the Core split).")
    }

    $backpacksSlug = $curseSlug[[int]$bFile.projectId]
    $curseMaven = [ordered]@{
        backpacks = "curse.maven:${backpacksSlug}-$($bFile.projectId):$($bFile.id)"
    }
    if ($coreEntry) {
        $coreSlug = $curseSlug[[int]$coreEntry.project]
        $curseMaven.core = "curse.maven:${coreSlug}-$($coreEntry.project):$($coreEntry.fileId)"
    }

    $entry = [ordered]@{
        loader    = $loader
        mc        = $mc
        backpacks = [ordered]@{
            project     = $bFile.projectId
            fileId      = $bFile.id
            fileName    = $bFile.fileName
            releaseDate = $bFile.dateCreated
            size        = $bSize
            sha1        = $bSha1
        }
        curseMaven = $curseMaven
    }
    if ($range) { $entry.backpacks.requiresCore = $range }
    if ($coreEntry) { $entry.core = $coreEntry }

    $newEntries.Add([pscustomobject]$entry)
}

# ---------------------------------------------------------------------------------------------
# Diff summary
# ---------------------------------------------------------------------------------------------

Write-Host ""
Write-Host "=== Diff summary vs. current manifest.json ==="
if ($newPairs.Count -eq 0 -and $changedPairs.Count -eq 0) {
    Write-Host "No changes: every (loader, mc) pair's newest RELEASE file matches the current manifest."
} else {
    if ($newPairs.Count -gt 0) {
        Write-Host "New (loader, mc) pairs (not in the current manifest): $($newPairs.Count)"
        $newPairs | ForEach-Object { Write-Host "  + $_" }
    }
    if ($changedPairs.Count -gt 0) {
        Write-Host "Updated Backpacks files for existing pairs: $($changedPairs.Count)"
        $changedPairs | ForEach-Object { Write-Host "  ~ $_" }
    }
}
if ($warnings.Count -gt 0) {
    Write-Host ""
    Write-Host "Warnings:"
    $warnings | ForEach-Object { Write-Host "  ! $_" }
}

# Anything the old manifest had that discovery no longer sees at all (e.g. delisted) - keep it but
# flag it, rather than silently dropping data.
$newKeys = $newEntries | ForEach-Object { "$($_.loader)|$($_.mc)" }
$missing = $oldEntries | Where-Object { "$($_.loader)|$($_.mc)" -notin $newKeys }
foreach ($m in $missing) {
    Write-Warning "'$($m.loader) $($m.mc)' was in the manifest but discovery found no RELEASE file for it any more; keeping the existing entry."
    $newEntries.Add($m)
}

if (-not $Write) {
    Write-Host ""
    Write-Host "Dry run (default) - manifest.json not written. Pass -Write to update it."
    exit 0
}

$manifest = [ordered]@{
    generated       = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    description     = $oldManifest.description
    sourceProjects  = $sourceProjects
    entries         = ($newEntries | Sort-Object { $_.loader }, { $_.mc })
    skipped         = $oldSkipped
    warnings        = $warnings
}

$manifest | ConvertTo-Json -Depth 10 | Set-Content -Path $manifestPath -Encoding utf8
Write-Host ""
Write-Host "Wrote $manifestPath ($($newEntries.Count) entries)."
