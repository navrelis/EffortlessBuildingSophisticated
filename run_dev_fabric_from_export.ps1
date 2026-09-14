#Requires -Version 5.1
<#
  Copies the Fabric mod JAR from ExportedJars into DevInstance_Fabric/run/mods (replacing any older
  sophisticatedbuilding-fabric*.jar), then launches Gradle runClientExported.

  Requires a built artifact under ExportedJars (e.g. run rebuild_all_and_export_jar.ps1 first).

  Uses a dedicated Loom run (clientExported) with an empty exportedStub source set so the project mod is not
  injected from sources; only mods under DevInstance_Fabric/run/mods (including the copied export) are used.
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = $PSScriptRoot
$ExportedJars = Join-Path $RepoRoot 'ExportedJars'
$FabricProject = Join-Path $RepoRoot 'Fabric-0.18.6-1.21.1'
$ModsDir = Join-Path $RepoRoot 'DevInstance_Fabric\run\mods'
$JarPrefix = 'sophisticatedbuilding-fabric'

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
Write-Host "Starting Fabric dev client (runClientExported)..." -ForegroundColor Green

$gradlew = Join-Path $FabricProject 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradlew)) {
    throw "Missing $gradlew"
}

Push-Location -LiteralPath $FabricProject
try {
    & .\gradlew.bat runClientExported --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle runClientExported failed (exit $LASTEXITCODE)"
    }
}
finally {
    Pop-Location
}
