#requires -Version 7.0
<#
.SYNOPSIS
    Uploads the release jars of every version branch to CurseForge (project 1414718, Effortless Building Sophisticated)
    through the CurseForge upload API. Dry run by default.

.DESCRIPTION
    1. Collects the release jars: every `<folder>/release/*.jar` tracked on the local `mc/*` branches (`git show`, no
       worktree is touched; -Remote uses `origin/mc/*`), or the jars in -JarDir. They are copied to
       local/curseforge-release/<mod version>/.
    2. Reads each jar's own metadata (scripts/lib/CurseForgeRelease.ps1): mod version, loader (the 1.20.1 Forge jar is
       listed for Forge and NeoForge), the exact Minecraft versions it declares (ranges expanded), the Java version of
       its classes, and its relations: Fabric API (required, Fabric jars), Sophisticated Backpacks (optional; the
       official project for Forge/NeoForge, the unofficial Fabric port for Fabric) where the jar integrates it, Curios /
       Trinkets (optional) where that integration uses them.
    3. -DryRun (default): prints the plan table and writes it to local/curseforge-plan-<mod version>.json. Without a
       token that is all. With a token it also resolves every game version name to its CurseForge id (read-only:
       GET /api/game/version-types and /api/game/versions) and checks the relation slugs (api.cfwidget.com); the plan
       then records the ids. Nothing is uploaded.
    4. -Upload -Plan <plan file>: uploads exactly the files of a plan the user has reviewed (the plan must be complete:
       every id resolved; every jar must still have the SHA-256 recorded in the plan). Asks for confirmation (type
       UPLOAD) unless -Yes. Resumable: every uploaded file is recorded in local/curseforge-upload-log.json right away;
       files already recorded (same project, file name and SHA-256) are skipped. Stops at the first error.

    Token: local/curseforge-token.txt (git-ignored, first non-empty line) or $env:CURSEFORGE_TOKEN. The token is only
    sent as the X-Api-Token header to minecraft.curseforge.com; it is never printed or logged.

.EXAMPLE
    pwsh scripts/curseforge-upload.ps1                         # dry run: plan table (ids resolved if a token exists)
    pwsh scripts/curseforge-upload.ps1 -ExpectVersion 5.0.0   # also fails unless every jar is 5.0.0
    pwsh scripts/curseforge-upload.ps1 -Upload -Plan local/curseforge-plan-5.0.0.json
#>
[CmdletBinding(DefaultParameterSetName = 'DryRun')]
param(
    [Parameter(ParameterSetName = 'DryRun')] [switch]$DryRun,
    [Parameter(ParameterSetName = 'Upload', Mandatory)] [switch]$Upload,
    [Parameter(ParameterSetName = 'Upload', Mandatory)] [string]$Plan,
    [Parameter(ParameterSetName = 'Upload')] [switch]$Yes,
    [Parameter(ParameterSetName = 'DryRun')] [string]$JarDir,
    [Parameter(ParameterSetName = 'DryRun')] [switch]$Remote,
    [Parameter(ParameterSetName = 'DryRun')] [string]$ExpectVersion,
    [Parameter(ParameterSetName = 'DryRun')] [string]$Changelog,
    [Parameter(ParameterSetName = 'DryRun')] [ValidateSet('release', 'beta', 'alpha')] [string]$ReleaseType = 'release',
    [Parameter(ParameterSetName = 'DryRun')] [switch]$NoEnvironment,
    [int]$ProjectId = 1414718
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
. (Join-Path $PSScriptRoot 'lib/CurseForgeRelease.ps1')

$RepoRoot = Split-Path $PSScriptRoot -Parent
$ApiBase = 'https://minecraft.curseforge.com'
$LogPath = Join-Path $RepoRoot 'local/curseforge-upload-log.json'

function Invoke-CurseForgeGet([string]$Path, [string]$Token) {
    # Read-only API call; the token only travels in the header
    return Invoke-RestMethod -Method Get -Uri "$ApiBase$Path" -Headers @{ 'X-Api-Token' = $Token } -TimeoutSec 60
}

function Get-FileSha256([string]$Path) { (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() }

function Get-BranchReleaseJars([string]$Destination, [switch]$FromRemote) {
    $pattern = if ($FromRemote) { 'refs/remotes/origin/mc/*' } else { 'refs/heads/mc/*' }
    $refs = @(git -C $RepoRoot for-each-ref --format='%(refname:short)' $pattern)
    if ($LASTEXITCODE -ne 0 -or $refs.Count -eq 0) { throw "No branches match $pattern" }
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    $jars = @()
    foreach ($ref in $refs) {
        $paths = @(git -C $RepoRoot ls-tree -r --name-only $ref | Where-Object { $_ -match '^[^/]+/release/[^/]+\.jar$' })
        foreach ($p in $paths) {
            $target = Join-Path $Destination ([IO.Path]::GetFileName($p))
            if ($jars | Where-Object { $_.Target -eq $target }) { throw "Two branches ship a jar named $([IO.Path]::GetFileName($p))" }
            # git show writes the blob byte for byte through cmd's binary-safe redirection
            $psi = [System.Diagnostics.ProcessStartInfo]::new('git', @('-C', $RepoRoot, 'show', "${ref}:$p"))
            $psi.RedirectStandardOutput = $true; $psi.UseShellExecute = $false
            $proc = [System.Diagnostics.Process]::Start($psi)
            $out = [IO.File]::Create($target)
            try { $proc.StandardOutput.BaseStream.CopyTo($out) } finally { $out.Dispose() }
            $proc.WaitForExit()
            if ($proc.ExitCode -ne 0) { throw "git show ${ref}:$p failed" }
            $jars += [pscustomobject]@{ Branch = $ref; Source = $p; Target = $target }
        }
    }
    return $jars
}

function Format-PlanTable($Entries) {
    $Entries | ForEach-Object {
        [pscustomobject]@{
            File      = $_.File
            Name      = $_.DisplayName
            Minecraft = ($_.McVersions -join ', ')
            Loaders   = ($_.Loaders -join ', ')
            Java      = $_.Java
            Relations = (($_.Relations | ForEach-Object { "$($_.slug) ($($_.type -replace 'Dependency', ''))" }) -join '; ')
            Ids       = if ($_.PSObject.Properties['GameVersionIds'] -and $_.GameVersionIds) { ($_.GameVersionIds -join ',') } else { '(unresolved)' }
        }
    } | Format-Table -AutoSize -Wrap | Out-String -Width 400
}

#region Upload
if ($Upload) {
    $planFile = if ([IO.Path]::IsPathRooted($Plan)) { $Plan } else { Join-Path (Get-Location) $Plan }
    if (-not (Test-Path -LiteralPath $planFile)) { throw "Plan file not found: $planFile (run the dry run first)" }
    $planData = Get-Content -LiteralPath $planFile -Raw | ConvertFrom-Json
    if ([int]$planData.ProjectId -ne $ProjectId) { throw "The plan is for project $($planData.ProjectId), not $ProjectId" }
    if (-not $planData.Resolved) { throw 'The plan has unresolved game version ids; run the dry run with a token and review it again' }
    $token = Get-CurseForgeToken -RepoRoot $RepoRoot
    if (-not $token) { throw 'No CurseForge token: create local/curseforge-token.txt (git-ignored) or set CURSEFORGE_TOKEN' }
    $changelogText = [string]$planData.ChangelogText
    if (-not $changelogText.Trim()) { throw 'The plan has no changelog text' }

    foreach ($e in $planData.Entries) {
        if (-not (Test-Path -LiteralPath $e.Path)) { throw "Jar of the plan is missing: $($e.Path)" }
        if ((Get-FileSha256 $e.Path) -ne $e.Sha256) { throw "$($e.File) changed since the plan was made (SHA-256 differs); make a new plan" }
    }

    $log = @()
    if (Test-Path -LiteralPath $LogPath) { $log = @(Get-Content -LiteralPath $LogPath -Raw | ConvertFrom-Json) }
    $pending = @($planData.Entries | Where-Object {
        $e = $_
        -not ($log | Where-Object { [int]$_.ProjectId -eq $ProjectId -and $_.File -eq $e.File -and $_.Sha256 -eq $e.Sha256 })
    })
    Write-Host "Plan: $($planData.Entries.Count) files for project $ProjectId, $($planData.Entries.Count - $pending.Count) already uploaded (log), $($pending.Count) to upload."
    if ($pending.Count -eq 0) { Write-Host 'Nothing to upload.'; return }
    $pending | ForEach-Object { Write-Host ("  {0}  ->  {1}" -f $_.File, $_.DisplayName) }
    if (-not $Yes) {
        $answer = Read-Host "Type UPLOAD to upload these $($pending.Count) files to CurseForge project $ProjectId"
        if ($answer -ne 'UPLOAD') { Write-Host 'Aborted, nothing uploaded.'; return }
    }

    foreach ($e in $pending) {
        $metadata = [ordered]@{
            changelog     = $changelogText
            changelogType = 'markdown'
            displayName   = $e.DisplayName
            gameVersions  = @($e.GameVersionIds | ForEach-Object { [int]$_ })
            releaseType   = $e.ReleaseType
        }
        if (@($e.Relations).Count -gt 0) {
            $metadata.relations = @{ projects = @($e.Relations | ForEach-Object { @{ slug = $_.slug; type = $_.type } }) }
        }
        $json = $metadata | ConvertTo-Json -Depth 6 -Compress
        Write-Host "Uploading $($e.File) ..."
        try {
            $response = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/projects/$ProjectId/upload-file" `
                -Headers @{ 'X-Api-Token' = $token } -Form @{ metadata = $json; file = Get-Item -LiteralPath $e.Path } -TimeoutSec 300
        } catch {
            $detail = $_.ErrorDetails.Message
            throw "Upload of $($e.File) failed: $($_.Exception.Message) $detail (files uploaded so far are in $LogPath; rerun to continue)"
        }
        $log += [pscustomobject]@{
            ProjectId = $ProjectId; File = $e.File; Sha256 = $e.Sha256; DisplayName = $e.DisplayName
            FileId = $response.id; UploadedAt = (Get-Date).ToString('o')
        }
        New-Item -ItemType Directory -Force -Path (Split-Path $LogPath) | Out-Null
        ConvertTo-Json -InputObject @($log) -Depth 4 | Set-Content -LiteralPath $LogPath -Encoding utf8
        Write-Host "  uploaded, CurseForge file id $($response.id)"
    }
    Write-Host 'All files of the plan are uploaded.'
    return
}
#endregion

#region Dry run
$staging = $null
if ($JarDir) {
    $jarFiles = @(Get-ChildItem -LiteralPath $JarDir -Filter '*.jar' | Where-Object { $_.Name -notlike '*-sources.jar' })
    $sources = @{}
} else {
    $staging = Join-Path $RepoRoot 'local/curseforge-release/staging'
    if (Test-Path -LiteralPath $staging) { Remove-Item -LiteralPath $staging -Recurse -Force }
    $collected = Get-BranchReleaseJars -Destination $staging -FromRemote:$Remote
    $jarFiles = @($collected | ForEach-Object { Get-Item -LiteralPath $_.Target })
    $sources = @{}; foreach ($c in $collected) { $sources[[IO.Path]::GetFileName($c.Target)] = "$($c.Branch):$($c.Source)" }
}
if ($jarFiles.Count -eq 0) { throw 'No release jars found' }

$infos = @($jarFiles | ForEach-Object { Get-ReleaseJarInfo -Path $_.FullName })
$versions = @($infos | ForEach-Object { $_.ModVersion } | Sort-Object -Unique)
if ($versions.Count -ne 1) { throw "The jars have different mod versions: $($versions -join ', ')" }
$modVersion = $versions[0]
if ($ExpectVersion -and $modVersion -ne $ExpectVersion) { throw "The jars are version $modVersion, expected $ExpectVersion" }

# Final location per mod version (the staging copy moves there, so a plan keeps pointing at the jars it hashed)
if ($staging) {
    $final = Join-Path $RepoRoot "local/curseforge-release/$modVersion"
    if (Test-Path -LiteralPath $final) { Remove-Item -LiteralPath $final -Recurse -Force }
    Move-Item -LiteralPath $staging -Destination $final
    $infos = @(Get-ChildItem -LiteralPath $final -Filter '*.jar' | ForEach-Object { Get-ReleaseJarInfo -Path $_.FullName })
}

# Oldest Minecraft version first: CurseForge lists the newest upload on top
$entries = @($infos |
    Sort-Object @{ Expression = { Get-McVersionSortKey $_.McVersions[0] } }, Loader |
    ForEach-Object { New-ReleasePlanEntry -Info $_ -ReleaseType $ReleaseType -Environment:(-not $NoEnvironment) })

$changelogPath = if ($Changelog) { $Changelog } else { Join-Path $RepoRoot "release/curseforge-changelog-$modVersion.md" }
$changelogText = if (Test-Path -LiteralPath $changelogPath) { Get-Content -LiteralPath $changelogPath -Raw } else { $null }

$token = Get-CurseForgeToken -RepoRoot $RepoRoot
$resolved = $false
$notes = @()
if ($token) {
    Write-Host 'Token found: resolving game version ids (read-only API calls) ...'
    # Invoke-RestMethod returns a JSON array as one object; enumerate it
    $types = @(Invoke-CurseForgeGet '/api/game/version-types' $token | ForEach-Object { $_ })
    $gameVersions = @(Invoke-CurseForgeGet '/api/game/versions' $token | ForEach-Object { $_ })
    $allResolved = $true
    foreach ($e in $entries) {
        $r = Resolve-GameVersionIds -Names $e.GameVersionNames -GameVersions $gameVersions -VersionTypes $types
        $missing = @($r.Missing)
        # Client/Server tags and the Java version are informational: drop them when CurseForge has no such entry
        $optional = @($missing | Where-Object { $_ -in 'Client', 'Server' -or $_ -like 'Java *' })
        foreach ($o in $optional) { $notes += "$($e.File): no CurseForge game version '$o', left out" }
        $required = @($missing | Where-Object { $_ -notin $optional })
        if ($required.Count -gt 0) { $allResolved = $false; $notes += "$($e.File): UNRESOLVED $($required -join ', ')" }
        $e | Add-Member -NotePropertyName GameVersionIds -NotePropertyValue @($r.Ids) -Force
        $e | Add-Member -NotePropertyName GameVersionNamesUsed -NotePropertyValue @($e.GameVersionNames | Where-Object { $_ -notin $missing }) -Force
    }
    $resolved = $allResolved
} else {
    $notes += 'No token (local/curseforge-token.txt or CURSEFORGE_TOKEN): game version ids not resolved; the plan cannot be uploaded.'
}

# Relation slugs: read-only check against the public api.cfwidget.com (slug -> project id)
$slugs = @($entries | ForEach-Object { $_.Relations } | ForEach-Object { $_.slug } | Sort-Object -Unique)
$slugCheck = [ordered]@{}
foreach ($slug in $slugs) {
    $expected = $RelationProjects[$slug]
    try {
        $p = Invoke-RestMethod -Uri "https://api.cfwidget.com/minecraft/mc-mods/$slug" -TimeoutSec 60
        $slugCheck[$slug] = if ($p.PSObject.Properties['id'] -and [int]$p.id -eq $expected) { "ok ($expected, $($p.title))" } else { "MISMATCH (expected $expected)" }
    } catch {
        $slugCheck[$slug] = "not checked ($($_.Exception.Message))"
    }
}

$planOut = Join-Path $RepoRoot "local/curseforge-plan-$modVersion.json"
$planObject = [ordered]@{
    ProjectId     = $ProjectId
    ModVersion    = $modVersion
    CreatedAt     = (Get-Date).ToString('o')
    Resolved      = $resolved
    ChangelogFile = $changelogPath
    ChangelogText = $changelogText
    Entries       = @($entries | ForEach-Object {
        [ordered]@{
            File = $_.File; Path = $_.Path; Sha256 = Get-FileSha256 $_.Path; Source = $sources[$_.File]
            DisplayName = $_.DisplayName; ReleaseType = $_.ReleaseType
            McVersions = $_.McVersions; Loaders = $_.Loaders; Java = $_.Java
            GameVersionNames = if ($_.PSObject.Properties['GameVersionNamesUsed']) { $_.GameVersionNamesUsed } else { $_.GameVersionNames }
            GameVersionIds = if ($_.PSObject.Properties['GameVersionIds']) { $_.GameVersionIds } else { @() }
            Relations = @($_.Relations | ForEach-Object { [ordered]@{ slug = $_.slug; type = $_.type } })
        }
    })
}
New-Item -ItemType Directory -Force -Path (Split-Path $planOut) | Out-Null
ConvertTo-Json -InputObject $planObject -Depth 6 | Set-Content -LiteralPath $planOut -Encoding utf8

Write-Host ''
Write-Host "Dry run: $($entries.Count) jars, mod version $modVersion, CurseForge project $ProjectId, release type $ReleaseType"
Write-Host (Format-PlanTable $entries)
Write-Host 'Relation slugs:'
$slugCheck.GetEnumerator() | ForEach-Object { Write-Host ("  {0,-48} {1}" -f $_.Key, $_.Value) }
Write-Host ("Changelog: " + $(if ($changelogText) { "$changelogPath ($($changelogText.Length) chars)" } else { "MISSING ($changelogPath)" }))
foreach ($n in $notes) { Write-Host "Note: $n" }
Write-Host "Plan written to $planOut"
if ($resolved -and $changelogText) {
    Write-Host "Review it, then upload with: pwsh scripts/curseforge-upload.ps1 -Upload -Plan $planOut"
} else {
    Write-Host 'The plan is not uploadable yet (unresolved ids or no changelog).'
}
#endregion
