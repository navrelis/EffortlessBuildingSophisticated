# Builds and publishes a release jar for every loader folder of this branch. Generic: loader discovery matches
# build-all.ps1 and .github/workflows/build.yml (any top-level folder containing settings.gradle and gradlew), so
# this script can be copied unchanged onto other Minecraft version branches.
#
# For each discovered loader:
#   1. runs "gradlew build" (skip with -NoBuild to reuse an existing build/libs jar)
#   2. locates the main jar <loader>/build/libs/<mod_id>-<loader>-<minecraft_version>-<mod_version>.jar (never the
#      -sources jar). A folder named "<loader>-<minecraft>" (e.g. forge-1.21: a second jar of one loader for another
#      Minecraft version of the branch) is expected to build <mod_id>-<loader>-<minecraft>-<mod_version>.jar
#   3. verifies the version embedded in the jar's mod metadata (fabric.mod.json / neoforge.mods.toml / mods.toml)
#      equals mod_version from gradle/shared.properties
#   4. clears old jars out of <loader>/release/, copies the new jar there, writes <loader>/release/SHA256SUMS.txt
#   5. prints a summary table
#
# Exits non-zero if any loader fails to build, produces no matching jar, or has a version mismatch.
#
# Usage: pwsh ./release.ps1 [-NoBuild]

[CmdletBinding()]
param(
    [switch]$NoBuild
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = $PSScriptRoot

function Read-Properties {
    param([string]$Path)
    $props = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
        $idx = $trimmed.IndexOf('=')
        if ($idx -lt 0) { continue }
        $key = $trimmed.Substring(0, $idx).Trim()
        $value = $trimmed.Substring($idx + 1).Trim()
        $props[$key] = $value
    }
    return $props
}

# ---- Shared metadata ---------------------------------------------------------------------------------------
$sharedPropsPath = Join-Path $root 'gradle/shared.properties'
if (-not (Test-Path $sharedPropsPath)) { throw "Not found: $sharedPropsPath" }
$sharedProps = Read-Properties -Path $sharedPropsPath

$modId = $sharedProps['mod_id']
$modVersion = $sharedProps['mod_version']
$mcVersion = $sharedProps['minecraft_version']
if (-not $modId -or -not $modVersion -or -not $mcVersion) {
    throw "mod_id / mod_version / minecraft_version missing from gradle/shared.properties"
}

Write-Host "Releasing $modId $modVersion (Minecraft $mcVersion)"

# ---- Discover loaders (same rule as build-all.ps1 and the CI workflow) ---------------------------------------
$loaders = Get-ChildItem -LiteralPath $root -Directory |
    Where-Object {
        (Test-Path (Join-Path $_.FullName 'settings.gradle')) -and (Test-Path (Join-Path $_.FullName 'gradlew'))
    } |
    Sort-Object Name |
    ForEach-Object { $_.Name }

if (-not $loaders -or $loaders.Count -eq 0) {
    throw "No loader folders discovered (looked for settings.gradle + gradlew under $root)."
}
Write-Host "Discovered loaders: $($loaders -join ', ')"
Write-Host ''

$metadataFiles = [ordered]@{
    'fabric.mod.json'             = 'json'
    'META-INF/neoforge.mods.toml' = 'toml'
    'META-INF/mods.toml'          = 'toml'
}

$results = [System.Collections.Generic.List[object]]::new()
$hadError = $false

foreach ($loader in $loaders) {
    $loaderDir = Join-Path $root $loader
    $row = [ordered]@{
        Loader = $loader
        Status = 'pending'
        Jar    = ''
        SHA256 = ''
    }

    try {
        if (-not $NoBuild) {
            Write-Host "==> $loader : gradlew build"
            Push-Location $loaderDir
            try {
                if ($IsWindows -or $env:OS -eq 'Windows_NT') {
                    & .\gradlew.bat build --no-daemon --stacktrace
                } else {
                    & ./gradlew build --no-daemon --stacktrace
                }
                if ($LASTEXITCODE -ne 0) {
                    throw "gradlew build failed (exit code $LASTEXITCODE)"
                }
            } finally {
                Pop-Location
            }
        }

        # A folder "<loader>-<minecraft>" (e.g. forge-1.21) builds that loader's jar for another Minecraft version
        # of the branch: sophisticatedbuilding-forge-1.21-<version>.jar, not ...-forge-1.21-<minecraft_version>-...
        $jarLoader = $loader
        $jarMc = $mcVersion
        if ($loader -match '^([a-z]+)-(\d+(?:\.\d+)+)$') {
            $jarLoader = $Matches[1]
            $jarMc = $Matches[2]
        }
        $expectedName = "$modId-$jarLoader-$jarMc-$modVersion.jar"
        $jarPath = Join-Path $loaderDir "build/libs/$expectedName"
        if (-not (Test-Path $jarPath)) {
            throw "Expected jar not found: build/libs/$expectedName (run without -NoBuild, or check archivesName/mod_version)"
        }

        # ---- Verify the embedded mod version matches shared.properties -------------------------------------
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
        try {
            $checked = $false
            foreach ($entryName in $metadataFiles.Keys) {
                $entry = $zip.GetEntry($entryName)
                if ($null -eq $entry) { continue }
                $checked = $true

                $stream = $entry.Open()
                $reader = New-Object System.IO.StreamReader($stream)
                $content = $reader.ReadToEnd()
                $reader.Close()
                $stream.Close()

                $embeddedVersion = $null
                if ($metadataFiles[$entryName] -eq 'json') {
                    $embeddedVersion = ($content | ConvertFrom-Json).version
                } else {
                    # Top-level (unindented) "version = "..."" line; avoids matching "loaderVersion"/"versionRange".
                    $m = [regex]::Match($content, '(?m)^version\s*=\s*"([^"]+)"')
                    if ($m.Success) { $embeddedVersion = $m.Groups[1].Value }
                }

                if ($embeddedVersion -ne $modVersion) {
                    throw "version mismatch in ${entryName}: jar has '$embeddedVersion', shared.properties has '$modVersion'"
                }
            }
            if (-not $checked) {
                throw "no recognized mod metadata file (fabric.mod.json / neoforge.mods.toml / mods.toml) found inside $expectedName"
            }
        } finally {
            $zip.Dispose()
        }

        # ---- Publish: clear old jars, copy the new one, write the checksum file ----------------------------
        $releaseDir = Join-Path $loaderDir 'release'
        New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null
        Get-ChildItem -LiteralPath $releaseDir -Filter '*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force

        $destJar = Join-Path $releaseDir $expectedName
        Copy-Item -LiteralPath $jarPath -Destination $destJar -Force

        $hash = (Get-FileHash -LiteralPath $destJar -Algorithm SHA256).Hash.ToLowerInvariant()
        $sumsPath = Join-Path $releaseDir 'SHA256SUMS.txt'
        Set-Content -LiteralPath $sumsPath -Value "$hash  $expectedName" -Encoding utf8NoBOM

        $row.Status = 'ok'
        $row.Jar = $expectedName
        $row.SHA256 = $hash
    } catch {
        $row.Status = 'FAILED'
        $row.Jar = $_.Exception.Message
        $hadError = $true
        Write-Error "${loader}: $($_.Exception.Message)"
    }

    $results.Add([pscustomobject]$row)
}

Write-Host ''
$results | Format-Table -AutoSize | Out-String | Write-Host

if ($hadError) {
    Write-Error 'release.ps1 finished with errors.'
    exit 1
}

Write-Host 'release.ps1 finished successfully.'
exit 0
