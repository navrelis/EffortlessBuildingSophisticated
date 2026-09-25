#requires -Version 7.0
<#
.SYNOPSIS
    Offline tests for scripts/lib/CurseForgeRelease.ps1 (the metadata derivation of scripts/curseforge-upload.ps1):
    version range expansion, game version id resolution against synthetic API lists, token lookup, and the real
    release jars of the version branches (taken with `git show` from the local mc/* branches into a temp directory;
    no worktree is touched, no network call). Run directly:

        pwsh scripts/curseforge-upload.offline-tests.ps1

    Exits 0 if every test passes, 1 otherwise.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
. (Join-Path $PSScriptRoot 'lib/CurseForgeRelease.ps1')
$RepoRoot = Split-Path $PSScriptRoot -Parent

$script:failures = 0
$script:passes = 0
function Assert-Equal($Expected, $Actual, [string]$Message) {
    $e = ($Expected | ForEach-Object { "$_" }) -join '|'
    $a = ($Actual | ForEach-Object { "$_" }) -join '|'
    if ($e -ceq $a) { $script:passes++ } else { $script:failures++; Write-Host "FAIL: $Message`n  expected: $e`n  actual:   $a" -ForegroundColor Red }
}
function Assert-Throws([scriptblock]$Block, [string]$Message) {
    try { & $Block; $script:failures++; Write-Host "FAIL (no exception): $Message" -ForegroundColor Red } catch { $script:passes++ }
}

#region Version ranges
Assert-Equal @('1.16.3') (Expand-McVersionRange '[1.16.3]') 'maven exact'
Assert-Equal @('1.21', '1.21.1') (Expand-McVersionRange '[1.21,1.21.1]') 'maven inclusive pair'
Assert-Equal @('26.1', '26.1.1', '26.1.2') (Expand-McVersionRange '[26.1,26.1.2]') 'maven 26.x range'
Assert-Equal @('1.21.10', '1.21.11') (Expand-McVersionRange '[1.21.10,1.21.11]') 'numeric compare 1.21.10 > 1.21.9'
Assert-Equal @('1.19') (Expand-McVersionRange '[1.19,1.19.1)') 'maven exclusive upper'
Assert-Equal @('1.19.1', '1.19.2') (Expand-McVersionRange '(1.19,1.19.2]') 'maven exclusive lower'
Assert-Equal @('1.16.3', '1.20.1') (Expand-McVersionRange '[1.16.3],[1.20.1]') 'maven union'
Assert-Equal @('1.21.1') (Expand-McVersionRange '1.21.1') 'fabric exact'
Assert-Equal @('1.16.4', '1.16.5') (Expand-McVersionRange '>=1.16.4 <=1.16.5') 'fabric pair'
Assert-Equal @('1.19', '1.19.1', '1.19.2') (Expand-McVersionRange '>=1.19 <=1.19.2') 'fabric 1.19 line'
Assert-Equal @('1.20.1', '1.20.2', '1.20.3', '1.20.4', '1.20.5', '1.20.6') (Expand-McVersionRange '~1.20.1') 'fabric tilde'
Assert-Equal @('1.21.4', '1.21.5') (Expand-McVersionRange @('1.21.4', '1.21.5')) 'fabric array (OR)'
Assert-Equal @('1.18', '1.18.1', '1.18.2') (Expand-McVersionRange '1.18.x') 'fabric x wildcard'
Assert-Throws { Expand-McVersionRange '[1.21,)' } 'open maven range refused'
Assert-Throws { Expand-McVersionRange '>=1.21' } 'fabric range without upper bound refused'
Assert-Throws { Expand-McVersionRange '*' } 'wildcard refused'
Assert-Throws { Expand-McVersionRange '[1.99.9]' } 'unknown version refused'
Assert-Equal -1 (Compare-McVersion '1.21.9' '1.21.10') 'compare 1.21.9 < 1.21.10'
Assert-Equal 1 (Compare-McVersion '26.1' '1.21.11') 'compare 26.1 > 1.21.11'
Assert-Equal -1 (Compare-McVersion '1.21' '1.21.1') 'compare 1.21 < 1.21.1'
$sorted = @('26.1', '1.21.10', '1.16.3', '1.21.9', '1.21') | Sort-Object { Get-McVersionSortKey $_ }
Assert-Equal @('1.16.3', '1.21', '1.21.9', '1.21.10', '26.1') $sorted 'sort key'
#endregion

#region Game version ids (synthetic API lists shaped like /api/game/version-types and /api/game/versions)
$types = @(
    [pscustomobject]@{ id = 1; name = 'Bukkit'; slug = 'bukkit' },
    [pscustomobject]@{ id = 2; name = 'Java'; slug = 'java' },
    [pscustomobject]@{ id = 70886; name = 'Minecraft 1.16'; slug = 'minecraft-1-16' },
    [pscustomobject]@{ id = 83806; name = 'Minecraft 26.1'; slug = 'minecraft-26-1' },
    [pscustomobject]@{ id = 68441; name = 'Modloader'; slug = 'modloader' },
    [pscustomobject]@{ id = 75208; name = 'Environment'; slug = 'environment' }
)
$versions = @(
    [pscustomobject]@{ id = 8056; gameVersionTypeID = 70886; name = '1.16.3' },
    [pscustomobject]@{ id = 9999; gameVersionTypeID = 1; name = '1.16.3' },
    [pscustomobject]@{ id = 15933; gameVersionTypeID = 83806; name = '26.1' },
    [pscustomobject]@{ id = 16083; gameVersionTypeID = 1; name = '26.1' },
    [pscustomobject]@{ id = 7498; gameVersionTypeID = 68441; name = 'Forge' },
    [pscustomobject]@{ id = 10150; gameVersionTypeID = 68441; name = 'NeoForge' },
    [pscustomobject]@{ id = 4458; gameVersionTypeID = 2; name = 'Java 8' },
    [pscustomobject]@{ id = 9638; gameVersionTypeID = 75208; name = 'Client' },
    [pscustomobject]@{ id = 9639; gameVersionTypeID = 75208; name = 'Server' }
)
$r = Resolve-GameVersionIds -Names @('1.16.3', 'Forge', 'NeoForge', 'Java 8', 'Client', 'Server') -GameVersions $versions -VersionTypes $types
Assert-Equal @(8056, 7498, 10150, 4458, 9638, 9639) $r.Ids 'ids: Minecraft types only (not Bukkit), loaders, Java, environment'
Assert-Equal @() $r.Missing 'ids: nothing missing'
$r = Resolve-GameVersionIds -Names @('26.1', 'Fabric', 'Java 25') -GameVersions $versions -VersionTypes $types
Assert-Equal @(15933) $r.Ids 'ids: 26.1 from its Minecraft type'
Assert-Equal @('Fabric', 'Java 25') $r.Missing 'ids: missing names reported'
$dup = $versions + @([pscustomobject]@{ id = 1; gameVersionTypeID = 68441; name = 'Forge' })
Assert-Throws { Resolve-GameVersionIds -Names @('Forge') -GameVersions $dup -VersionTypes $types } 'ids: ambiguous name refused'
#endregion

#region Token lookup (never printed; only checks that the right source wins)
$tmpRoot = Join-Path ([IO.Path]::GetTempPath()) ("cf-offline-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path (Join-Path $tmpRoot 'local') -Force | Out-Null
$savedEnv = $env:CURSEFORGE_TOKEN
try {
    $env:CURSEFORGE_TOKEN = $null
    Assert-Equal $true ($null -eq (Get-CurseForgeToken -RepoRoot $tmpRoot)) 'token: none -> null'
    $env:CURSEFORGE_TOKEN = 'from-env'
    Assert-Equal 'from-env' (Get-CurseForgeToken -RepoRoot $tmpRoot) 'token: env'
    Set-Content -LiteralPath (Join-Path $tmpRoot 'local/curseforge-token.txt') -Value @('', '  from-file  ')
    Assert-Equal 'from-file' (Get-CurseForgeToken -RepoRoot $tmpRoot) 'token: file wins, trimmed, first non-empty line'
} finally {
    $env:CURSEFORGE_TOKEN = $savedEnv
}
#endregion

#region Real release jars from the version branches
$jarDir = Join-Path $tmpRoot 'jars'
New-Item -ItemType Directory -Path $jarDir -Force | Out-Null
function Get-BranchJar([string]$Branch, [string]$Folder) {
    $path = @(git -C $RepoRoot ls-tree -r --name-only "refs/heads/$Branch" | Where-Object { $_ -like "$Folder/release/*.jar" }) | Select-Object -First 1
    if (-not $path) { return $null }
    $target = Join-Path $jarDir ([IO.Path]::GetFileName($path))
    $psi = [System.Diagnostics.ProcessStartInfo]::new('git', @('-C', $RepoRoot, 'show', "refs/heads/${Branch}:$path"))
    $psi.RedirectStandardOutput = $true; $psi.UseShellExecute = $false
    $proc = [System.Diagnostics.Process]::Start($psi)
    $out = [IO.File]::Create($target)
    try { $proc.StandardOutput.BaseStream.CopyTo($out) } finally { $out.Dispose() }
    $proc.WaitForExit()
    return $target
}

# Branch, folder -> expected loaders, Minecraft versions, Java, relations (slug:type initial)
$cases = @(
    @{ B = 'mc/1.16.3'; F = 'forge'; Loaders = 'Forge'; Mc = '1.16.3'; Java = 8; Rel = 'sophisticated-backpacks:o|curios:o' },
    @{ B = 'mc/1.16.5'; F = 'fabric'; Loaders = 'Fabric'; Mc = '1.16.4|1.16.5'; Java = 8; Rel = 'fabric-api:r' },
    @{ B = 'mc/1.16.5'; F = 'forge-1.16.4'; Loaders = 'Forge'; Mc = '1.16.4'; Java = 8; Rel = 'sophisticated-backpacks:o|curios:o' },
    @{ B = 'mc/1.17.1'; F = 'fabric'; Loaders = 'Fabric'; Mc = '1.17.1'; Java = 16; Rel = 'fabric-api:r' },
    @{ B = 'mc/1.19.2'; F = 'fabric'; Loaders = 'Fabric'; Mc = '1.19|1.19.1|1.19.2'; Java = 17; Rel = 'fabric-api:r|sophisticated-backpacks-unofficial-fabric-port:o|trinkets:o' },
    @{ B = 'mc/1.19.2'; F = 'forge-1.19'; Loaders = 'Forge'; Mc = '1.19|1.19.1'; Java = 17; Rel = 'sophisticated-backpacks:o|curios:o' },
    @{ B = 'mc/1.19.4'; F = 'forge'; Loaders = 'Forge'; Mc = '1.19.4'; Java = 17; Rel = '' },
    @{ B = 'mc/1.20.1'; F = 'forge'; Loaders = 'Forge|NeoForge'; Mc = '1.20.1'; Java = 17; Rel = 'sophisticated-backpacks:o|curios:o' },
    @{ B = 'mc/1.20.4'; F = 'neoforge'; Loaders = 'NeoForge'; Mc = '1.20.4'; Java = 17; Rel = 'sophisticated-backpacks:o|curios:o' },
    @{ B = 'mc/1.21.1'; F = 'fabric'; Loaders = 'Fabric'; Mc = '1.21|1.21.1'; Java = 21; Rel = 'fabric-api:r|sophisticated-backpacks-unofficial-fabric-port:o|trinkets:o' },
    @{ B = 'mc/1.21.1'; F = 'forge-1.21'; Loaders = 'Forge'; Mc = '1.21'; Java = 21; Rel = '' },
    @{ B = 'mc/1.21.10'; F = 'fabric'; Loaders = 'Fabric'; Mc = '1.21.10'; Java = 21; Rel = 'fabric-api:r' },
    @{ B = 'mc/26.1.2'; F = 'neoforge-26.1'; Loaders = 'NeoForge'; Mc = '26.1|26.1.1'; Java = 25; Rel = 'sophisticated-backpacks:o|curios:o' },
    @{ B = 'mc/26.1.2'; F = 'forge'; Loaders = 'Forge'; Mc = '26.1|26.1.1|26.1.2'; Java = 25; Rel = '' }
)
try {
    foreach ($c in $cases) {
        $jar = Get-BranchJar $c.B $c.F
        if (-not $jar) { Write-Host "SKIP: no release jar in $($c.B) $($c.F)"; continue }
        $label = "$($c.B) $($c.F)"
        $info = Get-ReleaseJarInfo -Path $jar
        Assert-Equal $c.Loaders ($info.Loaders -join '|') "$label loaders"
        Assert-Equal $c.Mc ($info.McVersions -join '|') "$label Minecraft versions"
        Assert-Equal $c.Java $info.Java "$label Java"
        $rel = (Get-ReleaseRelations $info | ForEach-Object { "$($_.slug):$($_.type.Substring(0,1))" }) -join '|'
        Assert-Equal $c.Rel $rel "$label relations"
        $entry = New-ReleasePlanEntry -Info $info -Environment
        $mcLabel = if ($info.McVersions.Count -eq 1) { $info.McVersions[0] } else { "$($info.McVersions[0])-$($info.McVersions[-1])" }
        Assert-Equal "Sophisticated Building $($info.ModVersion) - $($c.Loaders -replace '\|', '/') $mcLabel" $entry.DisplayName "$label display name"
        Assert-Equal (@($info.McVersions) + @($info.Loaders) + @("Java $($c.Java)", 'Client', 'Server')) $entry.GameVersionNames "$label game version names"
        Assert-Equal $true ($info.ModVersion -match '^\d+\.\d+\.\d+$') "$label mod version read from the jar"
    }
} finally {
    Remove-Item -LiteralPath $tmpRoot -Recurse -Force -ErrorAction SilentlyContinue
}
#endregion

Write-Host "$($script:passes) passed, $($script:failures) failed"
if ($script:failures -gt 0) { exit 1 }
