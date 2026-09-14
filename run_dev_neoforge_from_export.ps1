#Requires -Version 5.1
<#
  Copies the NeoForge mod JAR from ExportedJars into DevInstance_NeoForge/run/mods (replacing any older
  sophisticatedbuilding-neoforge*.jar), then launches Gradle runClientExported.

  Requires a built artifact under ExportedJars (e.g. run rebuild_all_and_export_jar.ps1 first).

  Uses NeoForge ModDevGradle clientExported run (loadedMods = []) so the game loads mods from the DevInstance
  mods folder only, not the dev classpath copy of the mod.
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = $PSScriptRoot
$ExportedJars = Join-Path $RepoRoot 'ExportedJars'
$NeoForgeProject = Join-Path $RepoRoot 'Neoforge-21.1.217-1.21.1'
$ModsDir = Join-Path $RepoRoot 'DevInstance_NeoForge\run\mods'
$JarPrefix = 'sophisticatedbuilding-neoforge'

function Get-LatestExportedJarOrThrow {
    param(
        [Parameter(Mandatory)][string]$Directory,
        [Parameter(Mandatory)][string]$FilterPrefix,
        [Parameter(Mandatory)][string]$Hint
    )
    if (-not (Test-Path -LiteralPath $Directory)) {
        throw "ExportedJars folder not found: $Directory. Build and export first ($Hint)."
    }
    $candidates = Get-ChildItem -LiteralPath $Directory -Filter "$FilterPrefix*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '(?i)-(sources|javadoc)\.jar$' }
    $jar = $candidates | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) {
        throw "No $FilterPrefix*.jar in $Directory ($Hint)."
    }
    return $jar.FullName
}

function Remove-OldExportedModCopies {
    param(
        [Parameter(Mandatory)][string]$Directory,
        [Parameter(Mandatory)][string]$Prefix
    )
    if (-not (Test-Path -LiteralPath $Directory)) {
        return
    }
    Get-ChildItem -LiteralPath $Directory -Filter "$Prefix*.jar" -File -ErrorAction SilentlyContinue |
        ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue }
}

$srcJar = Get-LatestExportedJarOrThrow $ExportedJars $JarPrefix 'rebuild_all_and_export_jar.ps1'

New-Item -ItemType Directory -Force -Path $ModsDir | Out-Null
Remove-OldExportedModCopies $ModsDir $JarPrefix

$destPath = Join-Path $ModsDir (Split-Path -Leaf $srcJar)
Copy-Item -LiteralPath $srcJar -Destination $destPath -Force

Write-Host "Using: $srcJar" -ForegroundColor Cyan
Write-Host "Copied to: $destPath" -ForegroundColor Cyan
Write-Host "Starting NeoForge dev client (runClientExported)..." -ForegroundColor Green

$gradlew = Join-Path $NeoForgeProject 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradlew)) {
    throw "Missing $gradlew"
}

Push-Location -LiteralPath $NeoForgeProject
try {
    & .\gradlew.bat runClientExported --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle runClientExported failed (exit $LASTEXITCODE)"
    }
}
finally {
    Pop-Location
}
