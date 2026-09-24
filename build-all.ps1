# Builds every loader folder of this branch with its own Gradle wrapper; stops at the first failure.
# Usage: ./build-all.ps1 [gradle tasks...]   (default: build)
$ErrorActionPreference = 'Stop'
[string[]]$tasks = if ($args.Count -gt 0) { $args } else { @('build') }
$loaders = @('fabric', 'neoforge')

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
