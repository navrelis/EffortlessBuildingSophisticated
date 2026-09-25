#requires -Version 7.0
<#
.SYNOPSIS
    Sets a new mod version on every version branch worktree and rebuilds its release jars.

.DESCRIPTION
    For each worktree versions/<mc>/ (or -Mc, or the worktrees under -VersionsDir):
      1. mod_version=<Version> in gradle/shared.properties and in every loader folder's gradle.properties that
         overrides it.
      2. changelog/PATCH_NOTES_<old>.md -> changelog/PATCH_NOTES_<Version>.md (`git mv`); the old version in it
         becomes the new one (header "Sophisticated Building Update - <Version> (Minecraft <mc>)", jar names), and the
         "*Draft - lead to confirm before release.*" line is removed. If PATCH_NOTES_<Version>.md already exists
         (e.g. hand-written ahead of the bump), it's left as-is except the draft line is still stripped, and
         PATCH_NOTES_<old>.md is left untouched as history (no rename, no rewrite).
      3. README.md and TESTING.md: the old version becomes the new one (jar names), except on lines that record a
         test run of a specific old jar (a line naming a SHA-256 or an abbreviated hash "1234abcd..."), or that name
         PATCH_NOTES_<old>.md while that file was kept rather than renamed (see 2): those stay as history and are
         listed.
      4. Unless -NoBuild: builds every loader folder (`gradlew build --no-daemon`, JAVA_HOME = the JDK its
         gradle.properties names in ci_gradle_jdk, resolved as in scripts/test-all-versions.ps1), then runs the
         branch's `release.ps1 -NoBuild`, which checks the version inside each jar, replaces the old jar in
         <loader>/release/ and rewrites SHA256SUMS.txt.
    Nothing is committed or pushed; review the diff per worktree and commit it there.

    -WhatIf shows every change (files, lines, renames, builds) without writing anything.

.EXAMPLE
    pwsh scripts/bump-version.ps1 -Version 5.0.0 -WhatIf
    pwsh scripts/bump-version.ps1 -Version 5.0.0 -Mc 1.21.1
    pwsh scripts/bump-version.ps1 -Version 5.0.0 -VersionsDir local/bump-test -Mc 1.20.1
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)][ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version,
    [string[]]$Mc,
    [string]$VersionsDir,
    [switch]$NoBuild
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
. (Join-Path $PSScriptRoot 'lib/TestAllVersions.Common.ps1')

$RepoRoot = Split-Path $PSScriptRoot -Parent
$root = if ($VersionsDir) { (Resolve-Path -LiteralPath $VersionsDir).Path } else { Join-Path $RepoRoot 'versions' }
$worktrees = @(Get-ChildItem -LiteralPath $root -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'gradle/shared.properties') })
if ($Mc) {
    $Mc = @($Mc | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    $missing = @($Mc | Where-Object { -not ($worktrees.Name -contains $_) })
    if ($missing.Count) { throw "No worktree for: $($missing -join ', ') under $root" }
    $worktrees = @($worktrees | Where-Object { $Mc -contains $_.Name })
}
if ($worktrees.Count -eq 0) { throw "No version worktrees under $root" }

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
function Read-Text([string]$Path) { [IO.File]::ReadAllText($Path) }
function Write-Text([string]$Path, [string]$Text) { [IO.File]::WriteAllText($Path, $Text, $utf8NoBom) }
# The old version as a whole version (not part of 14.3.0 or 4.3.0.1)
function Get-VersionRegex([string]$V) { '(?<![\d.])' + [regex]::Escape($V) + '(?![\d]|\.\d)' }

function Get-LoaderFolders([string]$Worktree) {
    @(Get-ChildItem -LiteralPath $Worktree -Directory | Where-Object {
        (Test-Path -LiteralPath (Join-Path $_.FullName 'settings.gradle')) -and (Test-Path -LiteralPath (Join-Path $_.FullName 'gradlew'))
    } | Sort-Object Name)
}

$summary = @()
foreach ($wt in $worktrees) {
    $dir = $wt.FullName
    $shared = Join-Path $dir 'gradle/shared.properties'
    $old = $null
    foreach ($line in (Get-Content -LiteralPath $shared)) { if ($line -match '^\s*mod_version\s*=\s*(\S+)\s*$') { $old = $Matches[1] } }
    if (-not $old) { throw "$($wt.Name): no mod_version in gradle/shared.properties" }
    Write-Host "== $($wt.Name): $old -> $Version"
    $changes = @()
    if ($old -eq $Version) { Write-Host '   already at this version (files unchanged; jars are still rebuilt unless -NoBuild)' }
    $rx = Get-VersionRegex $old

    if ($old -ne $Version) {
        # 1. mod_version (shared, then per-folder overrides)
        $propFiles = @($shared) + @(Get-LoaderFolders $dir | ForEach-Object { Join-Path $_.FullName 'gradle.properties' } | Where-Object { Test-Path -LiteralPath $_ })
        foreach ($p in $propFiles) {
            $text = Read-Text $p
            if ($text -notmatch '(?m)^\s*mod_version\s*=') { continue }
            $new = [regex]::Replace($text, '(?m)^(\s*mod_version\s*=\s*)\S+', "`${1}$Version")
            if ($new -ne $text) {
                $changes += "mod_version in $([IO.Path]::GetRelativePath($dir, $p))"
                if ($PSCmdlet.ShouldProcess($p, "mod_version=$Version")) { Write-Text $p $new }
            }
        }

        # 2. patch notes
        $oldNotes = Join-Path $dir "changelog/PATCH_NOTES_$old.md"
        $newNotes = Join-Path $dir "changelog/PATCH_NOTES_$Version.md"
        $patchNotesRenamed = $false
        if (Test-Path -LiteralPath $newNotes) {
            # Notes for the new version already exist (e.g. hand-written bug-fix notes drafted ahead of the bump):
            # leave them as-is except stripping the draft marker, and leave the old notes untouched as history.
            $text = Read-Text $newNotes
            $draftRemoved = [bool]([regex]::Match($text, '(?m)^\*Draft\b[^\n]*\*\r?\n(\r?\n)?')).Success
            $new = [regex]::Replace($text, '(?m)^\*Draft\b[^\n]*\*\r?\n(\r?\n)?', '')
            $historyNote = if (Test-Path -LiteralPath $oldNotes) { ", PATCH_NOTES_$old.md kept as history" } else { '' }
            $draftNote = if ($draftRemoved) { ' (draft line removed)' } else { '' }
            $changes += "changelog/PATCH_NOTES_$Version.md exists: kept$historyNote$draftNote"
            if ($draftRemoved -and $PSCmdlet.ShouldProcess($newNotes, 'remove draft line')) { Write-Text $newNotes $new }
        } elseif (Test-Path -LiteralPath $oldNotes) {
            $patchNotesRenamed = $true
            $text = Read-Text $oldNotes
            $count = [regex]::Matches($text, $rx).Count
            $new = [regex]::Replace($text, $rx, $Version)
            $new = [regex]::Replace($new, '(?m)^\*Draft\b[^\n]*\*\r?\n(\r?\n)?', '')
            $changes += "changelog/PATCH_NOTES_$old.md -> PATCH_NOTES_$Version.md ($count version mentions, draft line removed)"
            if ($PSCmdlet.ShouldProcess($oldNotes, "git mv to PATCH_NOTES_$Version.md and update")) {
                git -C $dir mv "changelog/PATCH_NOTES_$old.md" "changelog/PATCH_NOTES_$Version.md"
                if ($LASTEXITCODE -ne 0) { throw "git mv failed in $dir" }
                Write-Text $newNotes $new
            }
        } else {
            Write-Warning "$($wt.Name): no changelog/PATCH_NOTES_$old.md or PATCH_NOTES_$Version.md"
        }

        # 3. README / TESTING: jar names; lines that record a run of a specific old jar stay, and (when the old
        # patch notes file was kept rather than renamed, above) lines naming PATCH_NOTES_<old>.md stay too
        $patchNotesOldRx = 'PATCH_NOTES_' + [regex]::Escape($old) + '(\.md)?\b'
        foreach ($doc in 'README.md', 'TESTING.md') {
            $p = Join-Path $dir $doc
            if (-not (Test-Path -LiteralPath $p)) { continue }
            $lines = (Read-Text $p) -split '(?<=\n)'
            $replaced = 0; $kept = @()
            for ($i = 0; $i -lt $lines.Count; $i++) {
                if ($lines[$i] -notmatch $rx) { continue }
                if ($lines[$i] -match 'SHA-256|\b[0-9a-f]{8}\.\.\.') { $kept += "   kept (test record) $doc`:$($i + 1): $($lines[$i].Trim())"; continue }
                if (-not $patchNotesRenamed -and $lines[$i] -match $patchNotesOldRx) { $kept += "   kept (PATCH_NOTES_$old.md kept as history) $doc`:$($i + 1): $($lines[$i].Trim())"; continue }
                $lines[$i] = [regex]::Replace($lines[$i], $rx, $Version); $replaced++
            }
            if ($replaced -gt 0) {
                $changes += "$doc ($replaced lines)"
                if ($PSCmdlet.ShouldProcess($p, "replace $old with $Version on $replaced lines")) { Write-Text $p ($lines -join '') }
            }
            $kept | ForEach-Object { Write-Host $_ }
        }
    }
    $changes | ForEach-Object { Write-Host "   $_" }

    # 4. release jars
    if (-not $NoBuild) {
        foreach ($loader in (Get-LoaderFolders $dir)) {
            $jdk = Resolve-LoaderGradleJdk -LoaderDir $loader.FullName
            if ($jdk.Error) { throw "$($wt.Name)/$($loader.Name): $($jdk.Error)" }
            $jdkText = Format-GradleJdk $jdk
            if ($PSCmdlet.ShouldProcess("$($wt.Name)/$($loader.Name)", "gradlew build --no-daemon ($jdkText)")) {
                Write-Host "   building $($loader.Name) ($jdkText)"
                $saved = $env:JAVA_HOME
                try {
                    if ($jdk.JavaHome) { $env:JAVA_HOME = $jdk.JavaHome }
                    Push-Location $loader.FullName
                    try {
                        $gradlew = if ($IsWindows) { '.\gradlew.bat' } else { './gradlew' }
                        & $gradlew build --no-daemon --quiet
                        if ($LASTEXITCODE -ne 0) { throw "$($wt.Name)/$($loader.Name): gradlew build failed (exit $LASTEXITCODE)" }
                    } finally { Pop-Location }
                } finally { $env:JAVA_HOME = $saved }
            }
        }
        if ($PSCmdlet.ShouldProcess($wt.Name, 'release.ps1 -NoBuild (verify versions, replace release jars, SHA256SUMS)')) {
            & pwsh -NoProfile -File (Join-Path $dir 'release.ps1') -NoBuild
            if ($LASTEXITCODE -ne 0) { throw "$($wt.Name): release.ps1 failed" }
        }
    }
    $summary += [pscustomobject]@{ Worktree = $wt.Name; From = $old; To = $Version; Changes = $changes.Count; Jars = if ($NoBuild) { 'not rebuilt' } elseif ($WhatIfPreference) { 'would rebuild' } else { 'rebuilt' } }
}
Write-Host ''
$summary | Format-Table -AutoSize | Out-String | Write-Host
if (-not $WhatIfPreference) { Write-Host 'Nothing was committed: review `git status` / `git diff` in each worktree and commit there.' }
