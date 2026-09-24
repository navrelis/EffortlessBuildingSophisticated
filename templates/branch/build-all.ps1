# Builds every loader folder of this branch with its own Gradle wrapper; stops at the first failure.
# Loaders are discovered generically (any top-level folder containing settings.gradle and gradlew), the same rule
# used by release.ps1 and .github/workflows/build.yml, so this script can be copied unchanged onto other
# Minecraft version branches.
# Usage: ./build-all.ps1 [gradle tasks...]   (default: build)
$ErrorActionPreference = 'Stop'
[string[]]$tasks = if ($args.Count -gt 0) { $args } else { @('build') }

$loaders = Get-ChildItem -LiteralPath $PSScriptRoot -Directory |
    Where-Object {
        (Test-Path (Join-Path $_.FullName 'settings.gradle')) -and (Test-Path (Join-Path $_.FullName 'gradlew'))
    } |
    Sort-Object Name |
    ForEach-Object { $_.Name }

if (-not $loaders -or $loaders.Count -eq 0) {
    throw "No loader folders discovered (looked for settings.gradle + gradlew under $PSScriptRoot)."
}

foreach ($loader in $loaders) {
    $dir = Join-Path $PSScriptRoot $loader
    Write-Host "==> $loader : gradlew $($tasks -join ' ')"
    Push-Location $dir
    try {
        if ($IsWindows -or $env:OS -eq 'Windows_NT') {
            & .\gradlew.bat @tasks
        } else {
            & ./gradlew @tasks
        }
        if ($LASTEXITCODE -ne 0) {
            throw "$loader build failed (exit code $LASTEXITCODE)"
        }
    } finally {
        Pop-Location
    }
}
Write-Host "All loader builds succeeded."
