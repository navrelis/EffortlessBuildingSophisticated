#requires -Version 7.0
<#
    report.json / report.md of scripts/test-all-versions.ps1, and the merge of several reports into one
    (-MergeReports: parallel instances with disjoint -Mc sets, or a headless run plus a later client run, each
    write their own report directory; the merge gives the one table for the final report).
#>

Set-StrictMode -Version Latest

function Get-ReportValue {
    <# Property of a report row (a PSCustomObject from a run or from ConvertFrom-Json, or a dictionary), $null when
       it is missing: strict-mode safe for rows of older reports. #>
    param($Row, [Parameter(Mandatory)][string]$Name)
    if ($null -eq $Row) { return $null }
    if ($Row -is [System.Collections.IDictionary]) { return $Row.Contains($Name) ? $Row[$Name] : $null }
    $prop = $Row.PSObject.Properties[$Name]
    return $prop ? $prop.Value : $null
}

function Get-TestReportSummary {
    param([object[]]$Results)
    $rows = @($Results)
    return [ordered]@{
        pass  = @($rows | Where-Object { $_.result -eq 'pass' }).Count
        fail  = @($rows | Where-Object { $_.result -eq 'fail' }).Count
        warn  = @($rows | Where-Object { $_.result -eq 'warn' }).Count
        'n/a' = @($rows | Where-Object { $_.result -eq 'n/a' }).Count
        total = $rows.Count
    }
}

function New-TestReport {
    param(
        [string]$ReportDir, [string]$VersionsDir, $Parameters,
        [object[]]$SbAvailability, [object[]]$SbGametestCoverage, [object[]]$Results, [string[]]$MergedFrom = @(),
        [object[]]$GradleJdks = @()
    )
    $report = [ordered]@{
        generatedAt        = (Get-Date).ToString('o')
        reportDir          = $ReportDir
        versionsDir        = $VersionsDir
        parameters         = $Parameters
        sbAvailability     = @($SbAvailability)
        sbGametestCoverage = @($SbGametestCoverage)
        gradleJdks         = @($GradleJdks)
        results            = @($Results)
        summary            = (Get-TestReportSummary -Results $Results)
    }
    if ($MergedFrom.Count -gt 0) { $report['mergedFrom'] = $MergedFrom }
    return $report
}

function Write-TestReportFiles {
    <# Writes <ReportDir>/report.json and report.md for a report from New-TestReport; returns both paths. #>
    param([Parameter(Mandatory)]$Report, [Parameter(Mandatory)][string]$ReportDir)
    New-Item -ItemType Directory -Path $ReportDir -Force -WhatIf:$false | Out-Null
    $jsonPath = Join-Path $ReportDir 'report.json'
    $Report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding utf8 -WhatIf:$false

    $summary = $Report.summary
    $md = [System.Text.StringBuilder]::new()
    [void]$md.AppendLine('# test-all-versions report')
    [void]$md.AppendLine()
    [void]$md.AppendLine("Generated: $($Report.generatedAt)")
    if ($Report.Contains('mergedFrom')) {
        [void]$md.AppendLine('Merged from:')
        foreach ($source in $Report.mergedFrom) { [void]$md.AppendLine('- `' + $source + '`') }
    } else {
        $params = $Report.parameters
        $stages = @()
        if ($params -is [System.Collections.IDictionary]) {
            if ($params.Contains('stages')) { $stages = @($params['stages']) }
        } elseif ($params -and $params.PSObject.Properties['stages']) {
            $stages = @($params.stages)
        }
        [void]$md.AppendLine('Versions dir: `' + $Report.versionsDir + '`')
        [void]$md.AppendLine("Stages: $($stages -join ', ')")
    }
    [void]$md.AppendLine()
    [void]$md.AppendLine('## Sophisticated Backpacks availability')
    [void]$md.AppendLine()
    [void]$md.AppendLine('| mc | loader | SB |')
    [void]$md.AppendLine('|---|---|---|')
    foreach ($row in $Report.sbAvailability) {
        [void]$md.AppendLine("| $($row.mc) | $($row.loader) | $($row.sbAvailable) |")
    }
    # Gradle JDK per (version, loader): which JDK each loader folder's Gradle ran on (ci_gradle_jdk). Reports written
    # before this existed have no gradleJdks key.
    $gradleJdks = @()
    if ($Report.Contains('gradleJdks')) { $gradleJdks = @($Report.gradleJdks | Where-Object { $null -ne $_ }) }
    if ($gradleJdks.Count -gt 0) {
        [void]$md.AppendLine()
        [void]$md.AppendLine('## Gradle JDK')
        [void]$md.AppendLine()
        [void]$md.AppendLine('| mc | loader | ci_gradle_jdk | JAVA_HOME | source |')
        [void]$md.AppendLine('|---|---|---|---|---|')
        foreach ($row in $gradleJdks) {
            $requested = Get-ReportValue -Row $row -Name 'requested'
            $err = Get-ReportValue -Row $row -Name 'error'
            $homeText = if ($err) { "NOT FOUND: $err" } else { "$(Get-ReportValue -Row $row -Name 'javaHome')" }
            $homeText = $homeText -replace '\|', '\|'
            [void]$md.AppendLine("| $($row.mc) | $($row.loader) | $(if ($null -ne $requested) { $requested } else { '-' }) | $homeText | $(Get-ReportValue -Row $row -Name 'source') |")
        }
    }
    [void]$md.AppendLine()
    [void]$md.AppendLine('## Results')
    [void]$md.AppendLine()
    [void]$md.AppendLine('| mc | loader | stage | result | detail | duration (s) | log |')
    [void]$md.AppendLine('|---|---|---|---|---|---|---|')
    foreach ($row in $Report.results) {
        $relLog = ''
        if ($row.logPath) {
            try { $relLog = [System.IO.Path]::GetRelativePath($ReportDir, $row.logPath) } catch { $relLog = $row.logPath }
        }
        $detailEsc = ("$($row.detail)" -replace '\|', '\|')
        [void]$md.AppendLine("| $($row.mc) | $($row.loader) | $($row.stage) | $($row.result) | $detailEsc | $($row.durationSeconds) | $relLog |")
    }
    if (@($Report.sbGametestCoverage).Count -gt 0) {
        [void]$md.AppendLine()
        [void]$md.AppendLine('## SB-named GameTests')
        [void]$md.AppendLine()
        [void]$md.AppendLine('| mc | loader | test | failed |')
        [void]$md.AppendLine('|---|---|---|---|')
        foreach ($row in $Report.sbGametestCoverage) {
            [void]$md.AppendLine("| $($row.mc) | $($row.loader) | $($row.test) | $($row.failed) |")
        }
    }
    [void]$md.AppendLine()
    [void]$md.AppendLine('## Summary')
    [void]$md.AppendLine()
    [void]$md.AppendLine("$($summary.pass) pass, $($summary.fail) fail, $($summary.warn) warn, $($summary.'n/a') n/a (of $($summary.total))")

    $mdPath = Join-Path $ReportDir 'report.md'
    Set-Content -LiteralPath $mdPath -Value $md.ToString() -Encoding utf8 -WhatIf:$false
    return [pscustomobject]@{ Json = $jsonPath; Markdown = $mdPath }
}

function Merge-TestReports {
    <# Combines the report.json of several report directories (or report.json paths) into one report object.
       Results are keyed by (mc, loader, stage): when the same key appears in more than one report, the row of
       the most recently generated report wins (a re-run replaces the earlier result) and the others are dropped;
       rows of a -WhatIf report only fill keys no real run has.
       Rows are ordered by mc, loader, then the fixed stage order. Every row gets a "source" (its report dir). #>
    param([Parameter(Mandatory)][string[]]$ReportPaths, [string]$ReportDir)
    $reports = foreach ($path in $ReportPaths) {
        $jsonPath = if (Test-Path -LiteralPath $path -PathType Container) { Join-Path $path 'report.json' } else { $path }
        if (-not (Test-Path -LiteralPath $jsonPath)) { throw "No report.json at '$jsonPath'." }
        $parsed = Get-Content -LiteralPath $jsonPath -Raw | ConvertFrom-Json
        # ConvertFrom-Json already turns the ISO timestamp into a DateTime
        $generatedAt = $parsed.generatedAt
        if ($generatedAt -isnot [datetime]) {
            $generatedAt = [datetimeoffset]::Parse("$generatedAt", [cultureinfo]::InvariantCulture).UtcDateTime
        }
        [pscustomobject]@{
            Source      = (Split-Path -Parent (Resolve-Path -LiteralPath $jsonPath).Path)
            GeneratedAt = $generatedAt.ToUniversalTime()
            Report      = $parsed
        }
    }
    $ordered = @($reports | Sort-Object GeneratedAt)

    $rows = [ordered]@{}
    $sb = [ordered]@{}
    $coverage = [ordered]@{}
    $jdks = [ordered]@{}
    foreach ($entry in $ordered) {
        $params = $entry.Report.PSObject.Properties['parameters']
        $isDryRun = $params -and $params.Value -and $params.Value.PSObject.Properties['whatIf'] -and [bool]$params.Value.whatIf
        foreach ($row in @($entry.Report.results)) {
            if ($null -eq $row) { continue }
            $key = "$($row.mc)|$($row.loader)|$($row.stage)"
            # A -WhatIf report only fills gaps; it never replaces a row of a real run
            if ($isDryRun -and $rows.Contains($key)) { continue }
            $copy = [ordered]@{}
            foreach ($prop in $row.PSObject.Properties) { $copy[$prop.Name] = $prop.Value }
            $copy['source'] = $entry.Source
            $rows[$key] = [pscustomobject]$copy
        }
        foreach ($row in @($entry.Report.sbAvailability)) {
            if ($null -ne $row) { $sb["$($row.mc)|$($row.loader)"] = $row }
        }
        foreach ($row in @($entry.Report.sbGametestCoverage)) {
            if ($null -ne $row) { $coverage["$($row.mc)|$($row.loader)|$($row.test)"] = $row }
        }
        foreach ($row in @(Get-ReportValue -Row $entry.Report -Name 'gradleJdks')) {
            if ($null -ne $row) { $jdks["$($row.mc)|$($row.loader)"] = $row }
        }
    }

    $stageRank = @{ 'build' = 0; 'gametest' = 1; 'server' = 2; 'client' = 3; 'smoke (runSmokeServer)' = 4; 'smoke (runSmokeClient)' = 5 }
    $versionKey = { param($mc) ($mc -split '\.' | ForEach-Object { '{0:D4}' -f [int]($_ -replace '\D', '0') }) -join '.' }
    $sortedRows = @($rows.Values | Sort-Object `
        @{ Expression = { & $versionKey $_.mc } }, loader, `
        @{ Expression = { if ($stageRank.ContainsKey($_.stage)) { $stageRank[$_.stage] } else { 99 } } })
    $sortedSb = @($sb.Values | Sort-Object @{ Expression = { & $versionKey $_.mc } }, loader)
    $sortedJdks = @($jdks.Values | Sort-Object @{ Expression = { & $versionKey $_.mc } }, loader)

    return New-TestReport -ReportDir $ReportDir -VersionsDir '' -Parameters ([ordered]@{ merge = $true }) `
        -SbAvailability $sortedSb -SbGametestCoverage @($coverage.Values) -Results $sortedRows `
        -MergedFrom @($ordered | ForEach-Object { $_.Source }) -GradleJdks $sortedJdks
}
