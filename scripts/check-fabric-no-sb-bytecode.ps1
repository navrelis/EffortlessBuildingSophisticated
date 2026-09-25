<#
.SYNOPSIS
    Checks that a branch's Fabric jar does not depend on access wideners of optional mods (Sophisticated
    Backpacks/Core and the Porting Lib modules nested in the Fabric SB port).

.DESCRIPTION
    Loom applies the transitive access wideners of every mod on the compile classpath to the Minecraft jar the mod is
    compiled against. The unofficial Fabric Sophisticated Core port nests Porting Lib, whose wideners make vanilla members
    public (e.g. Screen#font on 1.19.x). Code that uses such a member then compiles to different bytecode than it would
    against vanilla - a direct field access from an inner class instead of a synthetic accessor, or a call javac would
    otherwise reject - and players without Sophisticated Backpacks get IllegalAccessError at runtime.

    The script compiles the branch's Fabric main sources (common/src/main + fabric/src/main) a second time in a scratch
    build under local/no-sb-bytecode/<mc>/ against Minecraft + Fabric Loader + Fabric API only (plus the mod's own
    access widener), leaving out the source files that need an optional mod's API (they import net.p3pp3rf1y,
    Porting Lib, Trinkets, Cardinal Components or Curios, and every file that references one of those classes; the
    Common Protection API, a plain API jar without access wideners, stays on the classpath where the branch uses it). Then it
    compares the bytecode (javap -c -p without constant pool indices) of every class both builds produce. Any
    difference means a class relies on an optional mod's widener: fix it in the source (e.g. a private accessor method)
    and run the check again.

    One member is widened in the scratch build only: RenderType$CompositeState. javac rejects the protected nested class,
    but it is ACC_PUBLIC in the class file, so the widening changes no bytecode (the check stays exact).

    Exit code 0 = no differing class, 1 = differing classes or a failed build. One Gradle invocation at a time, always
    --no-daemon. The scratch folder is kept (git-ignored) so a second run is fast; -Clean deletes its build output.

.PARAMETER Mc
    The Minecraft version of the branch (the versions/<mc> worktree).

.PARAMETER VersionsDir
    Directory that holds the <mc> worktrees (default: versions/ next to this script's folder).

.PARAMETER Clean
    Delete the scratch build's build/ and .gradle/ folders after the check.

.EXAMPLE
    pwsh scripts/check-fabric-no-sb-bytecode.ps1 -Mc 1.19.2
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string] $Mc,
    [string] $VersionsDir,
    [switch] $Clean
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$mainRoot = Split-Path -Parent $PSScriptRoot
if (-not $VersionsDir) { $VersionsDir = Join-Path $mainRoot 'versions' }
$branch = Join-Path $VersionsDir $Mc
$fabric = Join-Path $branch 'fabric'
if (-not (Test-Path (Join-Path $fabric 'build.gradle'))) { throw "No Fabric build at $fabric" }

function Read-Properties([string] $Path) {
    $props = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $t = $line.Trim()
        if ($t -eq '' -or $t.StartsWith('#')) { continue }
        $i = $t.IndexOf('=')
        if ($i -gt 0) { $props[$t.Substring(0, $i).Trim()] = $t.Substring($i + 1).Trim() }
    }
    return $props
}

$shared = Read-Properties (Join-Path $branch 'gradle/shared.properties')
$fabricProps = Read-Properties (Join-Path $fabric 'gradle.properties')
$buildText = Get-Content -Raw (Join-Path $fabric 'build.gradle')
if ($buildText -notmatch "id\s+'fabric-loom'\s+version\s+'([^']+)'") { throw 'Loom version not found in fabric/build.gradle' }
$loom = $Matches[1]
$minecraft = $shared['minecraft_version']
$java = $shared['java_version']
$loader = $fabricProps['fabric_loader_version']
$api = $fabricProps['fabric_api_version']
$cpa = $fabricProps['common_protection_api_version']
Write-Host "mc/$Mc Fabric: Minecraft $minecraft, Java $java, Loom $loom, Fabric Loader $loader, Fabric API $api"

# ---- Scratch build ------------------------------------------------------------------------------------------------
$scratch = Join-Path $mainRoot "local/no-sb-bytecode/$Mc"
$sFabric = Join-Path $scratch 'fabric'
New-Item -ItemType Directory -Force -Path $sFabric, (Join-Path $scratch 'gradle'), (Join-Path $scratch 'common/src') | Out-Null
Remove-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $scratch 'common/src/main'), (Join-Path $sFabric 'src')
Copy-Item -Recurse (Join-Path $branch 'common/src/main') (Join-Path $scratch 'common/src/main')
New-Item -ItemType Directory -Force -Path (Join-Path $sFabric 'src') | Out-Null
Copy-Item -Recurse (Join-Path $fabric 'src/main') (Join-Path $sFabric 'src/main')
Copy-Item -Force (Join-Path $branch 'gradle/shared.properties') (Join-Path $scratch 'gradle/shared.properties')
foreach ($f in 'settings.gradle', 'gradlew', 'gradlew.bat') { Copy-Item -Force (Join-Path $fabric $f) (Join-Path $sFabric $f) }
Copy-Item -Recurse -Force (Join-Path $fabric 'gradle') $sFabric

# Source files that need an optional mod's API, plus (transitively) every file that references one of them
$optionalApi = '^\s*import\s+(static\s+)?(net\.p3pp3rf1y|io\.github\.fabricators_of_create|dev\.emi\.trinkets|org\.ladysnake|dev\.onyxstudios|top\.theillusivec4)\.'
$roots = @((Join-Path $scratch 'common/src/main/java'), (Join-Path $sFabric 'src/main/java'))
$sources = foreach ($r in $roots) {
    Get-ChildItem -Recurse -File -Filter *.java $r | ForEach-Object {
        $text = Get-Content -Raw $_.FullName
        $package = if ($text -match '(?m)^\s*package\s+([\w.]+)\s*;') { $Matches[1] } else { '' }
        [pscustomobject]@{ Root = $r; File = $_.FullName; Name = $_.BaseName; Package = $package; Text = $text }
    }
}
# A file uses an excluded class C (package p) if it imports p.C (or p.C.Inner / static members of it), or if it is in
# package p or imports p.* and names C
function Test-Uses($s, $c) {
    $fqn = [regex]::Escape("$($c.Package).$($c.Name)")
    if ($s.Text -match "(?m)^\s*import\s+(static\s+)?$fqn(\.[\w*]+)*\s*;") { return $true }
    $samePackage = $s.Package -eq $c.Package
    $wildcard = $s.Text -match "(?m)^\s*import\s+$([regex]::Escape($c.Package))\.\*\s*;"
    return ($samePackage -or $wildcard) -and $s.Text -match "\b$([regex]::Escape($c.Name))\b"
}
$excluded = @{}
foreach ($s in $sources) { if ($s.Text -match "(?m)$optionalApi") { $excluded[$s.File] = $s } }
do {
    $added = 0
    foreach ($s in $sources) {
        if ($excluded.ContainsKey($s.File)) { continue }
        foreach ($c in @($excluded.Values)) {
            if (Test-Uses $s $c) { $excluded[$s.File] = $s; $added++; break }
        }
    }
} while ($added -gt 0)
$excludePatterns = foreach ($s in $excluded.Values) {
    $rel = $s.File.Substring($s.Root.Length + 1) -replace '\\', '/'
    "            exclude '$rel'"
}
Write-Host "Left out (need an optional mod's API): $($excluded.Count) source files"
$excluded.Values | ForEach-Object { Write-Host "  $($_.File.Substring($scratch.Length + 1))" }

# The mod's own access widener (it ships in the jar), plus the scratch-only CompositeState line
$awLines = @()
$modAw = Get-ChildItem -File (Join-Path $fabric 'src/main/resources') -Filter *.accesswidener -ErrorAction SilentlyContinue | Select-Object -First 1
if ($modAw) { $awLines = Get-Content $modAw.FullName } else { $awLines = @("accessWidener`tv2`tnamed") }
if (-not ($awLines -match 'class\s+net/minecraft/client/renderer/RenderType\$CompositeState\s*$')) {
    $awLines += '# scratch only: javac rejects the protected nested class; it is ACC_PUBLIC in the class file (no bytecode change)'
    $awLines += "accessible`tclass`tnet/minecraft/client/renderer/RenderType`$CompositeState"
}
Set-Content -Path (Join-Path $sFabric 'no-sb.accesswidener') -Value $awLines -Encoding utf8

# The Common Protection API (Patbox, maven.nucleoid.xyz) stays on the classpath: it is a plain API jar, no wideners
$cpaRepository = ''; $cpaDependency = ''
if ($cpa) {
    $cpaRepository = "repositories {`n    exclusiveContent {`n        forRepository { maven { url = 'https://maven.nucleoid.xyz' } }`n        filter { includeGroup('eu.pb4') }`n    }`n}`n`n"
    $cpaDependency = "    modCompileOnly('eu.pb4:common-protection-api:$cpa') { transitive = false }`n"
}

$gradle = @"
// Generated by scripts/check-fabric-no-sb-bytecode.ps1: the Fabric main sources of mc/$Mc compiled against Minecraft +
// Fabric API only, to compare their bytecode with the dev build (which has the optional mods' access wideners).
plugins {
    id 'fabric-loom' version '$loom'
}

def commonDir = file('../common')

java { toolchain.languageVersion = JavaLanguageVersion.of($java) }

sourceSets {
    main {
        java.srcDir "`${commonDir}/src/main/java"
        java {
$($excludePatterns -join "`n")
        }
    }
}

$($cpaRepository)dependencies {
    minecraft "com.mojang:minecraft:$minecraft"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:$loader"
    modImplementation "net.fabricmc.fabric-api:fabric-api:$api"
$($cpaDependency)    compileOnly 'com.google.code.findbugs:jsr305:3.0.2'
    compileOnly 'javax.annotation:javax.annotation-api:1.3.2'
}

loom {
    interfaceInjection { enableDependencyInterfaceInjection = false }
    accessWidenerPath = file('no-sb.accesswidener')
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.release = $java
    options.compilerArgs += ['-Xmaxerrs', '5000']
}
"@
Set-Content -Path (Join-Path $sFabric 'build.gradle') -Value $gradle -Encoding utf8
Set-Content -Path (Join-Path $sFabric 'gradle.properties') -Value "org.gradle.jvmargs=-Xmx3G`norg.gradle.daemon=false" -Encoding utf8

function Invoke-Gradle([string] $Dir, [string] $Log) {
    $p = Start-Process -FilePath (Join-Path $Dir 'gradlew.bat') -ArgumentList 'compileJava', '--no-daemon' -WorkingDirectory $Dir `
        -RedirectStandardOutput $Log -RedirectStandardError "$Log.err" -PassThru -NoNewWindow -Wait
    return $p.ExitCode
}

Write-Host 'Compiling the dev build (fabric compileJava) ...'
if ((Invoke-Gradle $fabric (Join-Path $scratch 'dev-compile.log')) -ne 0) { Write-Host "Dev compile FAILED, see $scratch\dev-compile.log"; exit 1 }
Write-Host 'Compiling the scratch build against Minecraft + Fabric API only ...'
if ((Invoke-Gradle $sFabric (Join-Path $scratch 'no-sb-compile.log')) -ne 0) {
    Get-Content (Join-Path $scratch 'no-sb-compile.log') | Select-String 'error:' -Context 0, 1 | Select-Object -First 20 | ForEach-Object { Write-Host $_ }
    Write-Host "No-SB compile FAILED (a member the mod needs is only reachable through an optional mod's widener), see $scratch\no-sb-compile.log"
    exit 1
}

# ---- Bytecode comparison ------------------------------------------------------------------------------------------
$javap = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/javap.exe'))) { Join-Path $env:JAVA_HOME 'bin/javap.exe' } else { 'javap' }
$a = Join-Path $sFabric 'build/classes/java/main'
$b = Join-Path $fabric 'build/classes/java/main'
$compared = 0; $differing = @()
foreach ($cls in Get-ChildItem -Recurse -File -Filter *.class $a) {
    $rel = $cls.FullName.Substring($a.Length + 1)
    $other = Join-Path $b $rel
    if (-not (Test-Path $other)) { continue }
    $compared++
    if ((Get-FileHash $cls.FullName).Hash -eq (Get-FileHash $other).Hash) { continue }
    $da = (& $javap -c -p $cls.FullName) -replace '#\d+(, *\d+)?', ''
    $db = (& $javap -c -p $other) -replace '#\d+(, *\d+)?', ''
    if (Compare-Object $da $db -SyncWindow 0) { $differing += $rel }
}
if ($Clean) { Remove-Item -Recurse -Force -ErrorAction SilentlyContinue (Join-Path $sFabric 'build'), (Join-Path $sFabric '.gradle') }
Write-Host "Compared $compared classes: $($differing.Count) differ"
if ($differing.Count -gt 0) {
    $differing | ForEach-Object { Write-Host "  DIFF $_" }
    Write-Host "Show one with: javap -c -p <class> in both $a and $b"
    exit 1
}
Write-Host "OK: the Fabric classes of mc/$Mc compile to the same bytecode without the optional mods' access wideners."
exit 0
