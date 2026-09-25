#requires -Version 7.0
# Helpers for scripts/curseforge-upload.ps1: read a release jar's own metadata and turn it into a CurseForge upload
# plan (Minecraft versions, loaders, Java version, relations, display name). Pure functions except where noted
# (Invoke-CurseForgeApi talks to the network); dot-source this file. Offline tests:
# scripts/curseforge-upload.offline-tests.ps1.

Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Every Minecraft release version a jar of this project may declare, oldest first. A version range in a jar's
# metadata is expanded against this list, so only real releases end up on CurseForge. Extend it when a new
# Minecraft version is ported (Expand-McVersionRange fails loudly on a range end it does not know).
$script:KnownMinecraftVersions = @(
    '1.16', '1.16.1', '1.16.2', '1.16.3', '1.16.4', '1.16.5',
    '1.17', '1.17.1',
    '1.18', '1.18.1', '1.18.2',
    '1.19', '1.19.1', '1.19.2', '1.19.3', '1.19.4',
    '1.20', '1.20.1', '1.20.2', '1.20.3', '1.20.4', '1.20.5', '1.20.6',
    '1.21', '1.21.1', '1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8', '1.21.9', '1.21.10', '1.21.11',
    '26.1', '26.1.1', '26.1.2', '26.2'
)

# CurseForge projects of the relations (slug -> project id, checked read-only by the dry run through api.cfwidget.com).
$script:RelationProjects = [ordered]@{
    'sophisticated-backpacks'                        = 422301   # official, Forge and NeoForge
    'sophisticated-backpacks-unofficial-fabric-port' = 979322   # unofficial Fabric port (salandora)
    'fabric-api'                                     = 306612
    'curios'                                         = 309927
    'trinkets'                                       = 341284
}

# Class file major version -> Java version as CurseForge names it ("Java <n>")
$script:ClassMajorToJava = @{ 52 = 8; 53 = 9; 54 = 10; 55 = 11; 60 = 16; 61 = 17; 65 = 21; 69 = 25 }

function Compare-McVersion {
    <# -1 / 0 / 1 for two dotted numeric versions (1.21.10 > 1.21.9, 26.1 > 1.21.11, 1.21 < 1.21.1). #>
    param([string]$A, [string]$B)
    $pa = $A.Split('.') | ForEach-Object { [int]$_ }
    $pb = $B.Split('.') | ForEach-Object { [int]$_ }
    $n = [Math]::Max($pa.Count, $pb.Count)
    for ($i = 0; $i -lt $n; $i++) {
        $x = if ($i -lt $pa.Count) { $pa[$i] } else { -1 }
        $y = if ($i -lt $pb.Count) { $pb[$i] } else { -1 }
        if ($x -lt $y) { return -1 }
        if ($x -gt $y) { return 1 }
    }
    return 0
}

function Assert-KnownMcVersion {
    param([string]$Version, [string]$Context)
    if ($script:KnownMinecraftVersions -notcontains $Version) {
        throw "Minecraft version '$Version' ($Context) is not in the known version list of scripts/lib/CurseForgeRelease.ps1; add it there if it is real."
    }
}

function Expand-McVersionRange {
    <#
    .SYNOPSIS
        The exact Minecraft releases a jar declares. Accepts a Maven version range (mods.toml / neoforge.mods.toml:
        "[1.16.4]", "[1.21,1.21.1]", "[26.1,26.1.2]", "(a,b)", "[a,b)") or a Fabric version predicate
        (fabric.mod.json: "1.21.1", ">=1.16.4 <=1.16.5", "~1.20.1", "1.21.x", or an array of those, OR-ed).
        Open-ended ranges are refused: a release must name the versions it was tested on.
    #>
    param([Parameter(Mandatory)] $Range)

    if ($Range -is [System.Array]) {
        $all = foreach ($r in $Range) { Expand-McVersionRange -Range $r }
        return @($script:KnownMinecraftVersions | Where-Object { $all -contains $_ })
    }
    $text = ([string]$Range).Trim()
    if ($text -eq '' -or $text -eq '*') { throw "Minecraft range '$text' is open; a release must declare its versions" }

    # Maven range(s): [a,b] (a,b) [a] ...; several ranges separated by commas between brackets
    if ($text -match '^[\[\(]') {
        $result = @()
        foreach ($m in [regex]::Matches($text, '([\[\(])\s*([^\]\),]*)\s*(?:,\s*([^\]\)]*))?\s*([\]\)])')) {
            $open = $m.Groups[1].Value; $lo = $m.Groups[2].Value.Trim(); $hasComma = $m.Groups[3].Success
            $hi = if ($hasComma) { $m.Groups[3].Value.Trim() } else { $lo }; $close = $m.Groups[4].Value
            if ($lo -eq '' -or $hi -eq '') { throw "Minecraft range '$text' is open-ended; a release must declare its versions" }
            Assert-KnownMcVersion $lo "range $text"; Assert-KnownMcVersion $hi "range $text"
            $result += $script:KnownMinecraftVersions | Where-Object {
                $c1 = Compare-McVersion $_ $lo; $c2 = Compare-McVersion $_ $hi
                ($c1 -gt 0 -or ($c1 -eq 0 -and $open -eq '[')) -and ($c2 -lt 0 -or ($c2 -eq 0 -and $close -eq ']'))
            }
        }
        if ($result.Count -eq 0) { throw "Minecraft range '$text' matches no known version" }
        return @($script:KnownMinecraftVersions | Where-Object { $result -contains $_ })
    }

    # Fabric predicate: space-separated constraints, all must hold
    $constraints = $text -split '\s+' | Where-Object { $_ }
    $matching = $script:KnownMinecraftVersions
    $bounded = $false
    foreach ($c in $constraints) {
        if ($c -match '^(>=|<=|>|<|=|~|\^)?(.+)$') {
            $op = $Matches[1]; $v = $Matches[2]
            if ($v -match '^(.+)\.x$') {
                $prefix = $Matches[1]
                $matching = @($matching | Where-Object { $_ -eq $prefix -or $_.StartsWith("$prefix.") })
                $bounded = $true
                continue
            }
            Assert-KnownMcVersion $v "predicate $text"
            switch ($op) {
                '>=' { $matching = @($matching | Where-Object { (Compare-McVersion $_ $v) -ge 0 }) }
                '>'  { $matching = @($matching | Where-Object { (Compare-McVersion $_ $v) -gt 0 }) }
                '<=' { $matching = @($matching | Where-Object { (Compare-McVersion $_ $v) -le 0 }); $bounded = $true }
                '<'  { $matching = @($matching | Where-Object { (Compare-McVersion $_ $v) -lt 0 }); $bounded = $true }
                '~'  {
                    # ~1.20.1: >=1.20.1 and < next minor (1.21)
                    $parts = $v.Split('.')
                    $minor = "$($parts[0]).$($parts[1])"
                    $matching = @($matching | Where-Object { (Compare-McVersion $_ $v) -ge 0 -and ($_ -eq $minor -or $_.StartsWith("$minor.")) })
                    $bounded = $true
                }
                '^'  { throw "Minecraft predicate '$text' (^) is too wide for a release" }
                default { $matching = @($matching | Where-Object { $_ -eq $v }); $bounded = $true }
            }
        } else {
            throw "Cannot parse Minecraft predicate '$text'"
        }
    }
    if (-not $bounded) { throw "Minecraft predicate '$text' has no upper bound; a release must declare its versions" }
    if ($matching.Count -eq 0) { throw "Minecraft predicate '$text' matches no known version" }
    return @($matching)
}

function Read-ZipEntryText {
    param([System.IO.Compression.ZipArchive]$Zip, [string]$Name)
    $entry = $Zip.GetEntry($Name)
    if (-not $entry) { return $null }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

function Get-TomlDependencies {
    <# The [[dependencies.<mod>]] blocks of a mods.toml / neoforge.mods.toml as objects (ModId, VersionRange, Required). #>
    param([string]$Toml)
    $deps = @()
    $current = $null
    foreach ($raw in ($Toml -split "`r?`n")) {
        $line = ($raw -replace '#.*$', '').Trim()
        if ($line -match '^\[\[dependencies\.') {
            if ($current) { $deps += [pscustomobject]$current }
            $current = @{ ModId = $null; VersionRange = $null; Required = $true }
            continue
        }
        if ($line -match '^\[') {
            if ($current) { $deps += [pscustomobject]$current; $current = $null }
            continue
        }
        if (-not $current) { continue }
        if ($line -match '^modId\s*=\s*"([^"]*)"') { $current.ModId = $Matches[1] }
        elseif ($line -match '^versionRange\s*=\s*"([^"]*)"') { $current.VersionRange = $Matches[1] }
        elseif ($line -match '^mandatory\s*=\s*(true|false)') { $current.Required = $Matches[1] -eq 'true' }
        elseif ($line -match '^type\s*=\s*"([^"]*)"') { $current.Required = $Matches[1].ToLowerInvariant() -eq 'required' }
    }
    if ($current) { $deps += [pscustomobject]$current }
    return $deps
}

function Get-JarJavaVersion {
    <# The Java version the mod's own classes are compiled for (class file major version of sophisticated/building/**). #>
    param([System.IO.Compression.ZipArchive]$Zip)
    $entry = $Zip.Entries | Where-Object { $_.FullName -like 'sophisticated/building/*.class' } | Select-Object -First 1
    if (-not $entry) { throw 'No sophisticated/building class in the jar' }
    $stream = $entry.Open()
    try {
        $buf = [byte[]]::new(8)
        $read = 0
        while ($read -lt 8) { $n = $stream.Read($buf, $read, 8 - $read); if ($n -le 0) { break }; $read += $n }
    } finally { $stream.Dispose() }
    if ($buf[0] -ne 0xCA -or $buf[1] -ne 0xFE) { throw "Not a class file: $($entry.FullName)" }
    $major = ($buf[6] -shl 8) -bor $buf[7]
    if (-not $script:ClassMajorToJava.ContainsKey($major)) { throw "Unknown class file major version $major" }
    return $script:ClassMajorToJava[$major]
}

function Get-ReleaseJarInfo {
    <#
    .SYNOPSIS
        Everything the upload needs about one release jar, read from the jar itself: mod version, loader(s),
        Minecraft versions (exact list), Java version, Sophisticated Backpacks / Curios / Trinkets integration.
    #>
    param([Parameter(Mandatory)][string]$Path)
    $file = Get-Item -LiteralPath $Path
    $zip = [System.IO.Compression.ZipFile]::OpenRead($file.FullName)
    try {
        $names = @($zip.Entries | ForEach-Object { $_.FullName })
        $info = [ordered]@{
            File = $file.Name; Path = $file.FullName; ModVersion = $null; Loader = $null; Loaders = @()
            McRange = $null; McVersions = @(); Java = $null
            BackpacksIntegration = $names -contains 'META-INF/services/sophisticated.building.platform.services.IBackpackIntegration'
            Curios = $false; Trinkets = $false; FabricApiModId = $null
        }
        $info.Java = Get-JarJavaVersion $zip

        $fabricJson = Read-ZipEntryText $zip 'fabric.mod.json'
        if ($fabricJson) {
            $meta = $fabricJson | ConvertFrom-Json -AsHashtable
            $info.Loader = 'Fabric'; $info.Loaders = @('Fabric')
            $info.ModVersion = $meta['version']
            $info.McRange = $meta['depends']['minecraft']
            foreach ($id in 'fabric-api', 'fabric') { if ($meta['depends'].ContainsKey($id)) { $info.FabricApiModId = $id; break } }
            foreach ($section in 'suggests', 'recommends', 'depends') {
                if ($meta.ContainsKey($section) -and $meta[$section].ContainsKey('trinkets')) { $info.Trinkets = $true }
            }
        } else {
            $tomlName = @('META-INF/neoforge.mods.toml', 'META-INF/mods.toml') | Where-Object { $names -contains $_ } | Select-Object -First 1
            if (-not $tomlName) { throw "$($file.Name): no fabric.mod.json, neoforge.mods.toml or mods.toml" }
            $toml = Read-ZipEntryText $zip $tomlName
            if ($toml -match '(?m)^\s*version\s*=\s*"([^"]+)"') { $info.ModVersion = $Matches[1] }
            $deps = Get-TomlDependencies $toml
            $mc = $deps | Where-Object { $_.ModId -eq 'minecraft' } | Select-Object -First 1
            if (-not $mc) { throw "$($file.Name): no minecraft dependency in $tomlName" }
            $info.McRange = $mc.VersionRange
            if ($deps | Where-Object { $_.ModId -eq 'neoforge' }) { $info.Loader = 'NeoForge'; $info.Loaders = @('NeoForge') }
            elseif ($deps | Where-Object { $_.ModId -eq 'forge' }) { $info.Loader = 'Forge'; $info.Loaders = @('Forge') }
            else { throw "$($file.Name): neither a forge nor a neoforge dependency in $tomlName" }
            $info.Curios = [bool]($names | Where-Object { $_ -match '(?i)curios' -and $_ -like 'sophisticated/building/*' })
        }
        $info.McVersions = @(Expand-McVersionRange -Range $info.McRange)

        # Minecraft 1.20.1: NeoForge 1.20.1 is a Forge fork that loads Forge 1.20.1 mods (javafml, net.minecraftforge
        # packages); the Forge jar was run on NeoForge 1.20.1-47.1.106 (server smoke test), so it is listed for both.
        if ($info.Loader -eq 'Forge' -and @($info.McVersions).Count -eq 1 -and $info.McVersions[0] -eq '1.20.1') {
            $info.Loaders = @('Forge', 'NeoForge')
        }
        return [pscustomobject]$info
    } finally {
        $zip.Dispose()
    }
}

function Get-ReleaseRelations {
    <# CurseForge relations of a jar: optional Sophisticated Backpacks where integrated, Fabric API required on Fabric. #>
    param([Parameter(Mandatory)] $Info)
    $relations = [System.Collections.Generic.List[object]]::new()
    if ($Info.Loader -eq 'Fabric') {
        $relations.Add([pscustomobject]@{ slug = 'fabric-api'; type = 'requiredDependency' })
    }
    if ($Info.BackpacksIntegration) {
        $slug = if ($Info.Loader -eq 'Fabric') { 'sophisticated-backpacks-unofficial-fabric-port' } else { 'sophisticated-backpacks' }
        $relations.Add([pscustomobject]@{ slug = $slug; type = 'optionalDependency' })
        if ($Info.Curios) { $relations.Add([pscustomobject]@{ slug = 'curios'; type = 'optionalDependency' }) }
        if ($Info.Trinkets) { $relations.Add([pscustomobject]@{ slug = 'trinkets'; type = 'optionalDependency' }) }
    }
    return $relations.ToArray()
}

function Get-ReleaseDisplayName {
    <# "Sophisticated Building 5.0.0 - Fabric 1.21.1"; several versions: "... - Forge 1.19-1.19.1"; 1.20.1 Forge: "Forge/NeoForge 1.20.1". #>
    param([Parameter(Mandatory)] $Info, [string]$ModName = 'Sophisticated Building')
    $versions = @($Info.McVersions)
    $mc = if ($versions.Count -eq 1) { $versions[0] } else { "$($versions[0])-$($versions[-1])" }
    return "$ModName $($Info.ModVersion) - $($Info.Loaders -join '/') $mc"
}

function New-ReleasePlanEntry {
    <# One upload: the jar's facts plus the CurseForge metadata derived from them (game version names, not ids yet). #>
    param([Parameter(Mandatory)] $Info, [string]$ReleaseType = 'release', [switch]$Environment)
    $gameVersionNames = @($Info.McVersions) + @($Info.Loaders) + @("Java $($Info.Java)")
    if ($Environment) { $gameVersionNames += @('Client', 'Server') }
    return [pscustomobject]@{
        File             = $Info.File
        Path             = $Info.Path
        DisplayName      = Get-ReleaseDisplayName $Info
        McVersions       = @($Info.McVersions)
        Loaders          = @($Info.Loaders)
        Java             = $Info.Java
        GameVersionNames = $gameVersionNames
        Relations        = @(Get-ReleaseRelations $Info)
        ReleaseType      = $ReleaseType
    }
}

function Resolve-GameVersionIds {
    <#
    .SYNOPSIS
        Maps game version names (Minecraft versions, loaders, "Java <n>", "Client"/"Server") to CurseForge ids using
        the /api/game/versions and /api/game/version-types lists. Minecraft versions only match version types whose
        slug starts with "minecraft-" (not Bukkit etc.); loaders the type "modloader"; Java "java"; Client/Server
        "environment". Returns @{ Ids; Missing }.
    #>
    param([Parameter(Mandatory)][string[]]$Names, [Parameter(Mandatory)] $GameVersions, [Parameter(Mandatory)] $VersionTypes)
    $typeSlug = @{}
    foreach ($t in $VersionTypes) { $typeSlug[[int]$t.id] = [string]$t.slug }
    $ids = @(); $missing = @()
    foreach ($name in $Names) {
        $kind = if ($name -match '^\d+(\.\d+)+$') { 'mc' } elseif ($name -match '^Java \d+$') { 'java' } elseif ($name -in 'Client', 'Server') { 'environment' } else { 'modloader' }
        $candidates = @($GameVersions | Where-Object {
            $slug = $typeSlug[[int]$_.gameVersionTypeId]
            [string]$_.name -eq $name -and $slug -and (
                ($kind -eq 'mc' -and $slug -like 'minecraft-*') -or
                ($kind -ne 'mc' -and $slug -eq $kind))
        })
        if ($candidates.Count -eq 0) { $missing += $name; continue }
        if ($candidates.Count -gt 1) { throw "CurseForge has several game versions named '$name' of the same kind ($(($candidates | ForEach-Object { $_.id }) -join ', ')); resolve by hand" }
        $ids += [int]$candidates[0].id
    }
    return @{ Ids = $ids; Missing = $missing }
}

function Get-CurseForgeToken {
    <# The API token from local/curseforge-token.txt (first non-empty line) or $env:CURSEFORGE_TOKEN; $null if neither. Never printed. #>
    param([Parameter(Mandatory)][string]$RepoRoot)
    $file = Join-Path $RepoRoot 'local/curseforge-token.txt'
    if (Test-Path -LiteralPath $file) {
        $line = Get-Content -LiteralPath $file | Where-Object { $_.Trim() } | Select-Object -First 1
        if ($line) { return $line.Trim() }
    }
    if ($env:CURSEFORGE_TOKEN) { return $env:CURSEFORGE_TOKEN.Trim() }
    return $null
}

function Get-McVersionSortKey {
    <# A string that sorts Minecraft versions in release order (1.21.9 < 1.21.10 < 26.1). #>
    param([Parameter(Mandatory)][string]$Version)
    $parts = @($Version.Split('.') | ForEach-Object { [int]$_ })
    while ($parts.Count -lt 4) { $parts += 0 }
    return ($parts | ForEach-Object { $_.ToString('0000') }) -join '.'
}
