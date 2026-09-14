#Requires -Version 5.1
<#
  Builds Sophisticated Building for NeoForge 1.21.1 and Fabric 1.21.1, then copies the main mod JARs
  (and the current PATCH_NOTES_*.md) into ExportedJars at the repository root.

  Removes prior exported JARs matching the mod basename so version bumps do not leave stale files.
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = $PSScriptRoot
$ExportedJars = Join-Path $RepoRoot 'ExportedJars'
$NeoForgeProject = Join-Path $RepoRoot 'Neoforge-21.1.217-1.21.1'
$FabricProject = Join-Path $RepoRoot 'Fabric-0.18.6-1.21.1'
# Picks the newest PATCH_NOTES_*.md at the repo root by version-like sort of the filename, so a
# version bump only needs a new PATCH_NOTES_<version>.md file, not an edit here.
$PatchNotesSrc = Get-ChildItem -LiteralPath $RepoRoot -Filter 'PATCH_NOTES_*.md' -File -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName

# archives_base_name: NeoForge uses "${mod_id}-neoforge", Fabric uses "${mod_id}-fabric"
$JarPrefixes = @(
    'sophisticatedbuilding'
)

function Remove-OldExportedModJars {
    param([Parameter(Mandatory)][string]$Directory)
    if (-not (Test-Path -LiteralPath $Directory)) {
        return
    }
    foreach ($prefix in $JarPrefixes) {
        Get-ChildItem -LiteralPath $Directory -Filter "$prefix*.jar" -File -ErrorAction SilentlyContinue |
            ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue }
    }
    # Also drop stale exported patch notes so only the current version's file remains.
    Get-ChildItem -LiteralPath $Directory -Filter 'PATCH_NOTES_*.md' -File -ErrorAction SilentlyContinue |
        ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue }
}

function Get-MainModJarOrThrow {
    param(
        [Parameter(Mandatory)][string]$LibsDir,
        [Parameter(Mandatory)][string]$BaseName,
        [Parameter(Mandatory)][string]$Hint
    )
    if (-not (Test-Path -LiteralPath $LibsDir)) {
        throw "Missing build output folder: $LibsDir ($Hint)"
    }
    $candidates = Get-ChildItem -LiteralPath $LibsDir -Filter "$BaseName*.jar" -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '(?i)-(sources|javadoc)\.jar$' }
    $jar = $candidates | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) {
        throw "Could not find $BaseName*.jar under $LibsDir ($Hint)"
    }
    return $jar.FullName
}

function Invoke-GradleStop {
    param([Parameter(Mandatory)][string]$ProjectDir)
    $gradlew = Join-Path $ProjectDir 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $gradlew)) {
        return
    }
    Push-Location -LiteralPath $ProjectDir
    try {
        & .\gradlew.bat --stop 2>$null
    }
    finally {
        Pop-Location
    }
}

function Invoke-GradleNeoForgeBuild {
    param([Parameter(Mandatory)][string]$ProjectDir)
    $gradlew = Join-Path $ProjectDir 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $gradlew)) {
        throw "Missing $gradlew"
    }
    Push-Location -LiteralPath $ProjectDir
    try {
        & .\gradlew.bat build --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle NeoForge build failed in $ProjectDir (exit $LASTEXITCODE)"
        }
    }
    finally {
        Pop-Location
    }
}

function Invoke-GradleFabricRemapJar {
    param([Parameter(Mandatory)][string]$ProjectDir)
    $gradlew = Join-Path $ProjectDir 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $gradlew)) {
        throw "Missing $gradlew"
    }
    Push-Location -LiteralPath $ProjectDir
    try {
        & .\gradlew.bat remapJar --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle Fabric remapJar failed in $ProjectDir (exit $LASTEXITCODE)"
        }
    }
    finally {
        Pop-Location
    }
}

# --- Build ---
Invoke-GradleStop $NeoForgeProject

Invoke-GradleNeoForgeBuild $NeoForgeProject
Invoke-GradleFabricRemapJar $FabricProject

$neoJar = Get-MainModJarOrThrow (Join-Path $NeoForgeProject 'build\libs') 'sophisticatedbuilding-neoforge' 'NeoForge'
$fabricJar = Get-MainModJarOrThrow (Join-Path $FabricProject 'build\libs') 'sophisticatedbuilding-fabric' 'Fabric'

$toPublish = @($neoJar, $fabricJar)

# --- Export folder ---
New-Item -ItemType Directory -Force -Path $ExportedJars | Out-Null
Remove-OldExportedModJars $ExportedJars

foreach ($f in $toPublish) {
    Copy-Item -LiteralPath $f -Destination $ExportedJars -Force
}

if ($PatchNotesSrc -and (Test-Path -LiteralPath $PatchNotesSrc)) {
    Copy-Item -LiteralPath $PatchNotesSrc -Destination $ExportedJars -Force
}
else {
    Write-Warning "No PATCH_NOTES_*.md found at $RepoRoot - skipped copy to ExportedJars."
}

Write-Host "Done. Copied $($toPublish.Count) JAR(s) and patch notes to:" -ForegroundColor Green
Write-Host "  $ExportedJars"
