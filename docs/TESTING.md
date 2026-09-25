# Testing across every version and loader

`scripts/test-all-versions.ps1` is the one command that tests every Minecraft version branch and every loader:
build, in-world GameTests, a dedicated server smoke boot, a client smoke boot, and the in-game scenario harness
(`runSmokeServer`/`runSmokeClient`, see "`runSmokeServer` / `runSmokeClient` contract" below) - which exists on
`mc/1.21.1` today and is ported onto each other branch in turn. It prints a pass/fail table and writes
`report.json`/`report.md` plus per-stage logs to a report directory.

```powershell
# from the repo root, PowerShell 7 (pwsh)
pwsh scripts/test-all-versions.ps1                                   # everything, every versions/<mc> worktree
pwsh scripts/test-all-versions.ps1 -Mc 1.21.1                        # one version, every loader
pwsh scripts/test-all-versions.ps1 -Mc 1.21.1 -Loader fabric,neoforge -Stages build,gametest
pwsh scripts/test-all-versions.ps1 -Headless -KeepGoing:$false       # no windows, stop at first failure
pwsh scripts/test-all-versions.ps1 -WhatIf                           # dry run: which stages WOULD run
pwsh scripts/test-all-versions.ps1 -Mc 1.21.4 -Stages smoke -SmokeTasks runSmokeServer   # only the headless harness
pwsh scripts/test-all-versions.ps1 -MergeReports <dir1>,<dir2>      # combine reports into one (see "Parallel runs")
```

`-Mc`, `-Loader`, `-Stages`, `-SmokeTasks` and `-MergeReports` accept comma-separated values in one token
(`-Stages build,gametest`) - the robust form from any shell. Space-separated bare tokens after one of these flags
(`-Stages build gametest`) only bind correctly to the array when PowerShell itself is doing the invoking; from
an external shell (`pwsh -File ... -Stages build gametest` from bash/cmd) the second token is silently dropped,
which is exactly why the script accepts (and this doc recommends) the comma-separated form.

See the script's own comment-based help (`Get-Help scripts/test-all-versions.ps1 -Full`) for every parameter.

## Stages, in order, and how each one decides pass/fail

Stages always run in this fixed order regardless of the order given to `-Stages`: `build`, `gametest`,
`server`, `client`, `smoke`. Within one instance: one Gradle invocation at a time, sequentially, for every
(version, loader) combination. Several instances on disjoint `-Mc` sets may run side by side (see "Parallel runs"),
with at most one game window open machine-wide; never start them while a ForgeGradle cache is still cold
(`docs/PORTING.md` "Gotchas").

1. **build** - `gradlew build --no-daemon --stacktrace`. Pass iff the process exits 0. The detail always
   includes `build/test-results/test/*.xml`'s summed tests/failures/errors/skipped counts, whether the stage
   passed or not.

2. **gametest** - only for a loader with a `src/gametest` folder (Fabric's own convention on this branch; see
   `docs/ARCHITECTURE.md`). NeoForge and Forge don't use this mechanism here, so this stage reports `n/a` for
   them - that is expected, not a gap. Runs `gradlew runGametest --no-daemon --stacktrace`; pass iff the
   captured log matches `All \d+ required tests passed`, and the matched count is recorded. GameTest classes or
   method names containing `Backpack` are additionally collected as SB gametest coverage and listed in their
   own report section/table (currently empty on this branch - none of the existing GameTest classes are
   Backpack-related; see "Sophisticated Backpacks coverage" below).

3. **server** - accepts the EULA (writes `eula=true` to `<loader>/run/eula.txt`), pins `server-port` in
   `<loader>/run/server.properties` to a freshly-chosen free TCP port (never the fixed default 25565 - a real
   collision risk when another Minecraft server, e.g. another agent's dev run, is already listening on it on
   the same machine), starts `gradlew runServer --no-daemon` in the background, and waits for `Done (` in
   `<loader>/run/logs/latest.log` (falling back to the Gradle console log). It then checks:
   - no `ERROR`/`Exception` log line mentioning `sophisticatedbuilding` or `sophisticated.building`;
   - **where this (version, loader) ships a Sophisticated Backpacks integration** (see "SB availability"
     below) - `sophisticatedbackpacks` shows up as a loaded mod, and `sophisticatedcore` too where the loader
     folder's mod metadata declares a dependency on it, AND `Registered Sophisticated Backpacks upgrade
     containers` is logged in `latest.log`. **Missing SB evidence where SB is expected fails the stage** - this
     proves the mod actually works with Sophisticated Backpacks in the dev runtime, not just that the server
     started.
     - Where the mod list comes from: `run/logs/latest.log`, plus the stage's Gradle console log and
       `run/logs/debug.log` (only if this run wrote it). Accepted formats (`Test-ModListedInLog`): Fabric
       Loader's `Loading N mods:` list (`\t- <modid> <version>`), FML's `Name Version (<modid>)` mod list and
       ` - <modid> (jar(...))` ModList dump (NeoForge 21+), and FML's DEBUG line `Found valid mod file <jar> with
       {<modid>[,<modid>]} mods - versions {...}`. Forge 36-47 and NeoForge 20.4 print only the last one, and only
       at DEBUG, so it is in the console and `debug.log` but never in `latest.log` (that is why the check used to
       fail on 1.20.1/forge and 1.20.4/neoforge although both mods were loaded). A jar can carry several mods:
       Sophisticated Backpacks 1.18.1 is `{sophisticatedbackpacks,sophisticatedcore}`.
     - When Core is required (`Test-SbCoreRequired`): iff the loader folder's mod metadata
       (`src/main/templates` or `src/main/resources`: `META-INF/neoforge.mods.toml`, `META-INF/mods.toml` or
       `fabric.mod.json`; for a `<loader>-<mc>` folder without its own, the base folder's) declares a
       `modId="sophisticatedcore"` dependency or has a `"sophisticatedcore"` key. That follows the SB build the
       folder is made for: SB 1.16.x (`mc/1.16.3`, `mc/1.16.5` incl. `forge-1.16.4`), 1.17.1 and 1.18
       (`forge-1.18`) have no Core mod and declare none; SB 1.18.1 ships Core inside its jar and 1.18.2 and later
       have a separate Core mod, and those folders declare it.
   Stops the server by writing `stop` to its stdin (works because the process is launched with
   `RedirectStandardInput`, not through cmd.exe's own `>` file redirection, which would otherwise cut it off
   from a controllable stdin pipe), falling back to killing the process tree it started if that doesn't exit
   within 60s.

4. **client** - skipped (`n/a`, "skipped: runSmokeClient covers join-and-check for this loader") for a loader
   whose build already defines a `runSmokeClient` task (detected by a cheap text search of that loader's
   `*.gradle` files for the literal task name - no extra Gradle invocation) - that harness's own join-and-check
   already covers "does the client boot and join without errors", so running the plain stage on top would only
   open a second, redundant Minecraft window. It's the fallback for a loader that doesn't have the richer
   harness yet, which is every loader today (the harness doesn't exist anywhere on this branch yet). Otherwise:
   starts `gradlew runClient` with a generated Gradle init script
   (`<ReportDir>/quickplay-init.gradle`) that appends `--quickPlaySingleplayer <world>` to the `runClient`
   task's program arguments (see "Quickplay init script" below), pointed at a throwaway copy of the server
   stage's freshly-generated world (`<loader>/run/world` copied to
   `<loader>/run/saves/test-all-versions-<random>`, deleted again at the end of the stage - never touches a
   tracked file or a save that was already there). If there's no server-stage world to copy from (e.g. `client`
   run without `server` first), it launches without quickplay and only checks that the client reaches the title
   screen. Waits for `logged in with entity id` (quickplay case) or `Loaded \d+ advancements` (title-screen
   fallback) in `run/logs/latest.log`, then 30s more; fails on a mod-related `ERROR`/`Exception` log line or a
   new file under `run/crash-reports` created during the run. Kills the process tree it started (no clean
   "stop" concept for a client).

5. **smoke** - the contract described below for `runSmokeServer`/`runSmokeClient`.

## SB availability

Detected exactly the way `common/`'s own `Services.backpacks()` fallback works (see `docs/ARCHITECTURE.md`
"Optional integration: Sophisticated Backpacks"): a loader ships Sophisticated Backpacks support iff it
registers
`<loader>/src/main/resources/META-INF/services/sophisticated.building.platform.services.IBackpackIntegration`.
A `<loader>-<mc>` folder (`forge-1.16.4`, `forge-1.18`, `forge-1.19`, `forge-1.21`) compiles its base folder's
resources, so it ships the base folder's registration (`forge-1.21` has none, like `forge/` on `mc/1.21.1`).
Every report shows an "SB" column/table per (version, loader) from this same check, independent of whether any
stage actually ran.

On `mc/1.21.1`: `fabric` and `neoforge` ship it (unofficial CurseForge Fabric port / official Modrinth NeoForge
build respectively, see `upstream/README.md`); `forge` does not (no Sophisticated Backpacks build exists for
Forge 1.21.1 yet).

## Sophisticated Backpacks coverage, end to end

SB availability alone only says a loader *could* have SB support - the script separately proves it actually
*works*, at three layers:

- **server stage** (always runs when `-Stages` includes `server`): SB mods loaded + upgrade containers
  registered, as above - the *load-and-wire-up* check. A loader with SB available FAILS this stage if that
  evidence is missing.
- **Fabric GameTests named `*Backpack*`**: in-world proof at the block-placement level (e.g. a Building Upgrade
  actually pulling blocks from a backpack). None exist yet on this branch - every current GameTest class
  (`InventoryHelperGameTest`, `MergeGameTest`, `ProtectionGameTest`, `SkipFirstGameTest`, `StorageDataGameTest`,
  `SurvivalReplaceGameTest`, `UndoGameTest`) is generic placement-mode coverage, not backpack-specific. Adding
  Backpack-named GameTests is the natural way to close this gap (see "Open points" below) - the report's SB
  gametest table is ready for them as soon as they exist.
- **smoke stage `sb.*` checks** (functional, scenario-level - see the contract below).

### `sb.*` check naming contract

Once the scenario harness (`runSmokeServer`/`runSmokeClient`) exists, any check in its
`smoketest-result.json` whose `name` starts with `sb.` is a **Sophisticated Backpacks functional check** -
something that only makes sense to run where SB is actually available for that (version, loader). Suggested
names for the harness to use (not enforced by the script beyond the `sb.` prefix - any name matching `sb.*`
counts):

| name | what it proves |
|---|---|
| `sb.upgrade_supplies_blocks` | a Building Upgrade in a backpack actually feeds blocks into a build |
| `sb.disabled_upgrade_ignored` | a disabled/removed Building Upgrade is not used as a block source |
| `sb.tier_cap` | the upgrade respects the backpack tier's supply-rate/slot cap |
| `sb.hud_count_synced` | the in-game HUD block count stays in sync with what the backpack actually supplied |
| `sb.tool_swapper_tools` | Tool Swapper tools stored in a backpack are usable/swappable as expected |
| `sb.worn_backpack` | a backpack worn via Curios/Trinkets (not just held/placed) still supplies blocks |

**Where SB is available for a (version, loader):**
- once the harness task exists, its result **must contain at least one `sb.*` check, and every `sb.*` check
  must pass** - "no `sb.*` check ran" is treated as `"SB not tested"` and **fails** the smoke stage, exactly
  like any other failed check;
- **while the harness task doesn't exist yet**, the stage is reported **`warn` ("missing harness")**, not
  silently `n/a` - it's a visible gap in the report, not something that quietly disappears. `n/a` is reserved
  for a (version, loader) that genuinely has no SB to test (e.g. `forge` on `mc/1.21.1`) and no harness task
  either.

## `runSmokeServer` / `runSmokeClient` contract

The harness exists today on `mc/1.21.1` (`common/src/smoketest`, `common/src/smoketestBackpacks`,
`<loader>/src/smoketest`, `gradle/smoketest.gradle` - see `.knowledge/port-brief.md` "Smoke harness adoption"
for how it's ported onto each other branch). This section is still written as a contract - what any branch's
harness must provide - because `test-all-versions.ps1`'s smoke stage and CI's `discover`/`build` jobs
(`templates/branch/.github/workflows/build.yml`) understand it generically, by task name and by the presence of
a `src/smoketest` folder, with nothing branch-specific to change once a branch adds it.

CI runs the headless half of this contract on every push/PR: the `discover` job's loader matrix adds
`has_smoke` (true iff `<loader>/src/smoketest` exists, or `<loader>/build.gradle` wires the smoke server run itself: a
non-comment line naming `runSmokeServer` or the `smokeServer` run - that covers a `<loader>-<mc>` folder that compiles
`../<loader>/src/smoketest` without overriding any of it, such as `neoforge-26.1` on `mc/26.1.2`), and the `build` job
then runs
`gradlew runSmokeServer -PsmoketestOut=<dir> --no-daemon --stacktrace` for loaders where it's true - only
`runSmokeServer`, since `runSmokeClient` opens a window a CI runner doesn't have. The result JSON and its log
are uploaded as the `<loader>-smoketest` workflow artifact with `if: always()`, so a failing smoke run's
`smoketest-result.json` (and, per this contract, its checks) is still there to inspect; a failing
`runSmokeServer` fails the CI job like any other step. A loader without the harness skips both the run step
and the upload step cleanly (`has_smoke` false). `test-all-versions.ps1` needs no such detection for `runSmokeServer`: it
runs the task in every loader folder and reports `n/a` (or `warn` where SB is expected) when Gradle says the task does
not exist.

- If `<loader>/build.gradle` (or wherever the harness wires it up) defines a Gradle task named
  `runSmokeServer` and/or `runSmokeClient`, the smoke stage runs it as
  `gradlew <task> -PsmoketestOut=<dir> --no-daemon --stacktrace`, where `<dir>` is a directory the script
  creates under the report dir.
- The harness must write `<dir>/smoketest-result.json` before the task's process exits, matching this schema:

  ```json
  {
    "passed": true,
    "checks": [
      { "name": "sb.upgrade_supplies_blocks", "passed": true, "detail": "supplied 12 blocks in 3 ticks" },
      { "name": "wall_mode_places_correctly", "passed": true, "detail": "" }
    ],
    "screenshots": ["C:/.../smoketest-out/wall_mode.png"]
  }
  ```
  - `passed` (bool, required): overall pass/fail for the whole scenario run.
  - `checks` (array, required, may be empty): one entry per named assertion; `name` (string), `passed` (bool),
    `detail` (string, free-form, e.g. a reason on failure).
  - `screenshots` (array of absolute paths, optional): copied into the report dir if present.
- If the task doesn't exist, the stage tells that apart from "the task exists and failed" by grepping the
  captured Gradle log for Gradle's own `Task '<name>' not found` / `Cannot locate tasks that match` messages -
  it does **not** run a separate `gradlew tasks` probe first (one Gradle invocation per target, not two).
- If `smoketest-result.json` is missing or unparsable after the task finishes (or times out), the stage FAILS
  outright (this is different from "task doesn't exist" - the harness ran and didn't hold up its end of the
  contract).
- `-SmokeTasks runSmokeServer,runSmokeClient` (default both) picks the harness tasks; `-Headless` removes
  `runSmokeClient` (opens a window) but still runs `runSmokeServer`.

## Quickplay init script

The client stage needs `--quickPlaySingleplayer <world>` on the `runClient` task's program arguments, without
editing any tracked `build.gradle`. The spec that led to this script named three loader-specific DSL paths -
`loom.runs.client.programArgs` (Fabric/Loom), `neoForge.runs.client.programArguments` (NeoForge/MDG),
`minecraft.runs.client.args` (Forge/FG7) - as the way to do that per loader. The generated init script
(`<ReportDir>/quickplay-init.gradle`) instead hooks the **task by name** generically:

```groovy
allprojects {
    def worldName = project.findProperty('quickPlayWorld')
    if (worldName) {
        tasks.matching { it.name == 'runClient' }.configureEach {
            args '--quickPlaySingleplayer', worldName.toString()
        }
    }
}
```

This works because every one of those three DSL paths ultimately configures the same kind of thing: a
JavaExec-derived task named `runClient`, and `JavaExec#args(...)` *appends* to the existing argument list
rather than replacing it - so one hook, keyed on the task name instead of the loader-specific extension, works
unchanged across Loom, ModDevGradle and ForgeGradle 7. This was verified against all three build setups on the
`mc/1.21.1` branch (fabric, neoforge, forge all pick up the init script and append the flag). It's simpler and
more robust than three separate DSL paths that would each need to keep working as those plugins evolve; the
tradeoff is that it depends on the run task being named exactly `runClient` and being JavaExec-based, which is
true for Loom, MDG and FG7 today but would need revisiting if a future loader's plugin does it differently (see
"Open points").

## Muted, second-monitor game windows

Every window a `client`/`runSmokeClient` launch opens is muted and moved off the main monitor, so running this
script doesn't make noise or steal the screen. `scripts/lib/GameWindow.ps1` (dot-sourced by
`test-all-versions.ps1`, alongside `scripts/lib/TestAllVersions.Common.ps1`):

- `Set-MinecraftMuted -RunDir <dir>` - sets `soundCategory_master:0.0` and `pauseOnLostFocus:false` in
  `<dir>/options.txt` (creating the file if it doesn't exist yet), preserving every other line.
  `pauseOnLostFocus:false` matters as much as the mute: the window is moved without activating it
  (`SWP_NOACTIVATE`, below), so it's never focused, and Minecraft pauses an unfocused window by default - which
  would otherwise stall world tick and break every join/wait check that depends on it. Called synchronously,
  **before** the game process starts (`options.txt` is read at boot).
- `Move-MinecraftWindowToSecondary -ProcessId <pid> [-TimeoutSeconds 180]` - waits for a visible, top-level,
  owner-less window belonging to a *descendant* of `<pid>` (walks `Win32_Process` parent/child links to find
  the `java.exe` the tracked `cmd.exe`/`gradlew.bat` process eventually spawns), then moves it
  (`SetWindowPos`, `SWP_NOACTIVATE | SWP_NOZORDER | SWP_NOSIZE` - repositions only, keeps current size, never
  steals focus) to the first non-primary monitor (`[System.Windows.Forms.Screen]::AllScreens`), top-left +
  40px. Never touches any window outside that one process's own descendant tree - never matches by name/title,
  so it can't reach another Minecraft instance or unrelated window on the same machine. Returns `$false` (with
  a warning, never a throw) if there's only one monitor or the window never appears in time, so it can never
  fail the stage it's called from.

`test-all-versions.ps1` calls `Set-MinecraftMuted` synchronously right before starting a client/`runSmokeClient`
process, then hands `Move-MinecraftWindowToSecondary` off to a background `Start-Job` (`Start-WindowMoveJob` /
`Stop-WindowMoveJob`) so waiting for the window doesn't block the stage's own log-polling wait; the job's
result is collected and logged when the stage cleans it up.

`scripts/move-game-window.ps1` is a thin standalone CLI wrapper around the same two functions
(`-RunDir`/`-GradlePid`/`-TimeoutSeconds`), for anything that starts a `gradlew runClient`-style process outside
this script and wants the same treatment: call it in the background right after starting that process.

Verified for real: `Set-MinecraftMuted` against a scratch directory, `Move-MinecraftWindowToSecondary` against
a real window (moved Notepad to `(-2520, 44)` = the detected secondary monitor's bounds + 40px), and a full
`server` + `client` run in a detached worktree, confirming `options.txt` ended up muted.

## Parallel runs

A strictly sequential run over every branch and loader takes 5+ hours. The headless stages of different versions can
run side by side; game windows stay one at a time.

What makes concurrent instances safe (they must get **disjoint** `-Mc` sets):

- **Report directory**: the default is `local/test-reports/<timestamp>-<process id>-<random>`, unique even for
  instances started in the same second; `-ReportDir` sets it explicitly. Everything an instance writes (stage logs,
  copied results, the generated `quickplay-init.gradle`) lives inside its own report directory.
- **Version locks**: `local/test-locks/<mc>.lock` (content: the instance's process id) is held while an instance
  tests that version and released when it moves on (and in `finally`). A second instance that reaches a locked
  version records a failing `version lock` row and skips it; a lock whose process id is no longer running is taken
  over. So an overlapping `-Mc` list never has two instances building in one worktree.
- **Window lock**: every stage that opens a window (`client`, `runSmokeClient`) first takes the machine-wide
  `local/game-window.lock` the agents use (`.knowledge/port-brief.md` "One game window at a time",
  `scripts/lib/GameWindowLock.ps1`): wait while it is younger than 20 minutes, delete it when older, create it
  atomically with `<owner> <ISO time>`, delete it when the client has exited or was killed, also in `finally`. A
  background job refreshes it every minute while held, so a long client stage never looks stale to anyone else.
  `-WindowLockTimeoutMinutes` (default 120) bounds the wait; after that the stage fails.
- **Server ports**: every server stage picks a free TCP port (`Get-FreeTcpPort`); worlds copied for quickplay get
  random names inside the version's own `run/` folder.

Recipe for the final run over the 16 branches (three headless instances, then the windows, then one merged report):

```powershell
# 1) Headless, in parallel (three terminals); warm ForgeGradle caches first (one Forge build per toolchain).
pwsh scripts/test-all-versions.ps1 -Mc 1.16.3,1.16.5,1.17.1,1.18.1,1.18.2,1.19.2 -Stages build,gametest,server,smoke -SmokeTasks runSmokeServer -ReportDir local/test-reports/final-a
pwsh scripts/test-all-versions.ps1 -Mc 1.20.1,1.20.4,1.21.1,1.21.4,1.21.5 -Stages build,gametest,server,smoke -SmokeTasks runSmokeServer -ReportDir local/test-reports/final-b
pwsh scripts/test-all-versions.ps1 -Mc 1.21.8,1.21.10,1.21.11,26.1.2,26.2 -Stages build,gametest,server,smoke -SmokeTasks runSmokeServer -ReportDir local/test-reports/final-c

# 2) One instance for everything that opens a window, over all versions (serialized by the window lock).
pwsh scripts/test-all-versions.ps1 -Stages client,smoke -SmokeTasks runSmokeClient -ReportDir local/test-reports/final-windows

# 3) One report.
pwsh scripts/test-all-versions.ps1 -MergeReports local/test-reports/final-a,local/test-reports/final-b,local/test-reports/final-c,local/test-reports/final-windows -ReportDir local/test-reports/final
```

The `client` stage in step 2 only runs for loaders without `runSmokeClient` (it is skipped as `n/a` where the
harness covers join-and-check) and uses the world a `server` stage left in `run/world`, which step 1 created.

`-MergeReports` (merge mode, runs nothing else, `scripts/lib/TestAllVersions.Report.ps1`) combines the `report.json`
of the given directories into one `report.json`/`report.md`: rows are keyed by (version, loader, stage), the newest
report wins for a duplicate key (a re-run replaces the earlier result), rows of a `-WhatIf` report only fill keys no
real run has, every row gets its `source` directory, and the summary is recomputed. It exits 1 if a merged row
failed.

## Gradle JDK per loader folder

Gradle does not run on one JDK for every branch: each loader folder's `gradle.properties` names the JDK its Gradle
needs in `ci_gradle_jdk=<n>` (the key the CI workflow reads too): 21 on every branch up to `mc/1.21.11`, 25 on
`mc/26.1.2` and `mc/26.2` (Loom 1.18 and the 26.x toolchains need Gradle on JDK 25). The script resolves it once per
loader folder (`Resolve-LoaderGradleJdk` in `scripts/lib/TestAllVersions.Common.ps1`) and sets `JAVA_HOME` in the
environment of each Gradle child process only (`Start-GradleProcess -JavaHome`); its own environment, the shell's
and every other process keep theirs. Resolution order for `ci_gradle_jdk=<n>`:

1. the environment variable `SB_JDK_<n>` (e.g. `SB_JDK_25`): a JDK home with `bin/java`. It is an explicit choice, so
   a path without `bin/java` or with another major version (read from its `release` file) is an error, never
   silently replaced by another JDK;
2. a Gradle-provisioned JDK under `<GRADLE_USER_HOME or ~/.gradle>/jdks/*-<n>-*` with `bin/java` and a matching
   `release` file (e.g. `eclipse_adoptium-25-amd64-windows.2`; the foojay toolchain resolver of the branches puts
   them there), the last one by name if several match;
3. this process's `JAVA_HOME`, if its `release` file says JDK `<n>`.

If none matches, no Gradle process starts for that folder: every requested stage fails with
`ci_gradle_jdk=<n> in <folder>/gradle.properties: JDK <n> for Gradle not found: ...` (what was tried and
`Set SB_JDK_<n> to a JDK <n> home`). A folder without `ci_gradle_jdk` inherits `JAVA_HOME` unchanged. The console
prints the JDK before every stage (`[26.2/forge] build: Gradle on JDK 25: C:\...\eclipse_adoptium-25-amd64-windows.2
(Gradle jdks folder)`), every row in `report.json` has a `gradleJdk` field, and `report.json` `gradleJdks` /
`report.md` "Gradle JDK" list the version, `JAVA_HOME` and source per (version, loader).

Running the 26.x branches needs nothing special when a JDK 25 is in `~/.gradle/jdks` (a first 26.x build through
Gradle's toolchain provisioning puts one there); otherwise point `SB_JDK_25` at one for that command only:

```powershell
pwsh scripts/test-all-versions.ps1 -Mc 26.1.2,26.2 -Stages build,gametest,server,smoke -SmokeTasks runSmokeServer -ReportDir local/test-reports/final-26
$env:SB_JDK_25 = 'C:\path\to\jdk-25'; pwsh scripts/test-all-versions.ps1 -Mc 26.1.2,26.2 ...; Remove-Item Env:SB_JDK_25
```

The resolution order, the error cases, the child-only `JAVA_HOME` and the report fields are covered by
`pwsh scripts/test-all-versions.offline-tests.ps1` (no Gradle, no game; 95 tests, together with the log-pattern,
window-lock, report-merge, JUnit-parsing, SB mod-list and watchdog tests).

## Hung stages: no-progress watchdog and thread dumps

`-StallMinutes` (default 10, `0` = off) is a no-progress watchdog for `build`, `gametest` and the smoke tasks: when the
stage's console log (`<stage>.console.log`, which carries the Gradle output and the game's own console output) has not
grown for that long while the stage's Gradle process still runs, the stage is ended early as failed ("no output for
N min (stalled), killed") instead of sitting until `-TimeoutMinutes`. Before any such kill - stall, stage timeout, a
`server` stage that never reached `Done (`, a `client` stage that never joined - the script takes thread dumps of
every `java.exe` in the stage's **own** process tree (found through the parent-process chain of the `cmd.exe` it
started; the Gradle client, the Gradle daemon and the game JVM): `jcmd <pid> Thread.print -l` of the JDK that runs that
process (`jstack -l` as fallback), 2 rounds 15 s apart, each bounded to 30 s. The files land next to the stage log as
`<stage>.threaddump-<game|gradle|java>-pid<pid>-round<n>.txt` (first lines: time, pid, command line), are named in the
row's detail and listed in its `threadDumps` field in `report.json`.

The first hang caught this way (1.21.8 `runGametest`, see `docs/PORTING.md` "Gotchas") showed the server thread parked in
`ServerLevel.waitForChunkAndEntities` under `PlayerList.placeNewPlayer`, called by the game tests' own player helper.

## Process safety

- Every `gradlew` invocation passes `--no-daemon` and the script never runs `gradlew --stop` - that kills every
  Gradle daemon of that Gradle version on the machine, including any other agent's in-progress build, not just
  this script's own.
- Every process the script starts is tracked by its `System.Diagnostics.Process` handle and, on timeout, on a
  failure with `-KeepGoing:$false`, or in the top-level `finally` block (which also runs on Ctrl+C - PowerShell
  delivers that as a terminating exception through the same `try/finally`), is killed **by PID only**, via
  `taskkill /PID <pid> /T /F` (kills exactly that process's own tree: `cmd.exe -> gradlew.bat -> java.exe`) -
  never by process name or pattern, which could otherwise hit an unrelated Java process on the same machine
  (another agent's build, or an unrelated project's own JVM).
- The dedicated server stage always runs on a freshly-chosen free TCP port (see `Get-FreeTcpPort` /
  `Set-ServerPort` in `scripts/lib/TestAllVersions.Common.ps1`), not the fixed default 25565, for the same
  reason - a real collision was hit while developing this script (another agent's dev server was already
  listening on 25565 on the same machine).
- A missing `versions/<mc>` (or `-VersionsDir <root>/<mc>`) worktree fails with a clear message pointing at
  `scripts/setup-worktrees.ps1`, rather than silently skipping it.

### Testing without touching a worktree another agent is using

`-VersionsDir` points the script at any directory laid out like `versions/` (one `<mc>` subfolder per version,
each shaped like a version branch - see `docs/ARCHITECTURE.md`). To test in isolation from a `versions/<mc>`
worktree someone else is actively building in, create a second, detached worktree of the same branch under
`local/` (git-ignored) and point `-VersionsDir` at its parent:

```powershell
git worktree add --detach local/versions-test/1.21.1 mc/1.21.1
pwsh scripts/test-all-versions.ps1 -Mc 1.21.1 -VersionsDir local/versions-test
git worktree remove --force local/versions-test/1.21.1   # when done
```

This is exactly how this script's own real run (see the report below) was produced - `versions/1.21.1` was in
use by another agent's build at the time.

## Open points

- No Backpack-named Fabric GameTests exist yet on `mc/1.21.1` - the SB gametest coverage table is implemented
  and ready, but currently always empty. Worth adding once the in-world backpack-supply behavior has GameTest
  coverage.
- `runSmokeServer`/`runSmokeClient` exist on `mc/1.21.1` (see the contract section above) and the full contract
  (JSON schema, `sb.*` requirement, `n/a` vs `warn` distinction) has been exercised end to end there:
  `test-all-versions.ps1 -Mc 1.21.1` reports smoke client 17/17 checks (7 `sb.*`) on fabric and neoforge, 10/10
  on forge; `runSmokeServer` run standalone with the exact command line CI uses
  (`gradlew runSmokeServer -PsmoketestOut=<dir> --no-daemon --stacktrace`) passes 9/9 checks (6 `sb.*`) on
  fabric and neoforge and 3/3 (no SB shipped) on forge. Every other branch still hits the "missing harness"
  `warn` path until the harness is ported onto it too.
- The quickplay init script's task-name hook (`tasks.matching { it.name == 'runClient' }`) assumes the client
  run task is named exactly `runClient` and is JavaExec-based; true for Loom/MDG/FG7 today, but would need a
  loader-specific fallback if a future toolchain does it differently.
- A single `-TimeoutMinutes` applies uniformly to every stage (build/gametest/server-ready-wait/client-join-wait
  each get the full budget, plus their own small fixed grace periods after - 3s post-"Done (" for the server,
  30s post-join for the client, 60s stop-grace for the server, 10s kill-grace elsewhere). Per-stage timeout
  overrides would be a reasonable follow-up if one stage's needs diverge a lot from the others' in practice.
- `Get-FreeTcpPort`'s freed-port handoff to the dedicated server has a small inherent TOCTOU race (something
  else could grab the same port between the check and the server's own bind) - rare enough in practice not to
  warrant a bind-retry loop yet, but worth watching if it ever flakes.
- **Forge's client stage sometimes crashes** with
  `java.lang.IllegalStateException: Can not retrieve LootModifierManager until resources have loaded once.`
  (`net.minecraftforge.common.ForgeInternalHandler.getLootModifierManager`, triggered from a flowing-water
  block-drop loot lookup during the very first world ticks after joining). The stack trace has **no**
  `sophisticated.building`/`sophisticatedbuilding` frames anywhere - this is a Forge/vanilla-only race, not a
  mod bug, and not a bug in this script: it looks like a timing race between Forge's resource-reload-completion
  flag and the integrated server starting to tick fluids, made more likely to actually land by the client stage
  joining a world immediately via quickplay (no time spent on menus first, unlike normal manual play). It does
  not always reproduce (seen fail, fail, pass, fail across four runs). `Get-CrashReportClassification`
  (`scripts/lib/TestAllVersions.Common.ps1`) recognizes this exact signature - and *only* this exact
  signature, and only when there is no `sophisticated.building`/`sophisticatedbuilding` frame anywhere in the
  crash report's actual trace section (never its "-- System Details --" mod-list footer, which mentions every
  loaded mod regardless of relevance - see the function's own comment for why that distinction matters) - and
  reports it as `warn` ("known-upstream crash report(s)..."), not `fail`. Any other crash, or this same
  exception text WITH a mod frame in the trace, still fails the stage normally; nothing is ever silently
  downgraded.
  - **Quick-playing into a fresh, never-before-seen world name was tried as a fix** (the theory being that the
    crash is specific to joining an *existing* save) and reverted: `--quickPlaySingleplayer <name>` does **not**
    create a new world for an unrecognized name - it shows "Failed to Quick Play - Could not find world with
    the provided identifier" and sits there doing nothing (confirmed the hard way, watching the actual window).
    The client stage went back to copying the server stage's world, as before. `Invoke-ClientStage` now also
    watches for that exact failure text (`Failed to Quick Play` / `Could not find world`) in the same wait as
    the join line, and fails the stage immediately with that message instead of waiting out the full
    `-TimeoutMinutes` with a window stuck on that screen - covered by an offline test against a synthetic log
    (no client launch) alongside `Get-CrashReportClassification`'s own tests against the real saved crash
    report above. A harness that genuinely wants a from-scratch world (unlike this stage's fixed-purpose smoke
    boot) should create it in-game itself, the way a real player would, rather than relying on this flag.
- The "stop" on stdin reliably stops Fabric's and Forge's `runServer` (confirmed against the real run below),
  but NeoForge's ModDevGradle `runServer` task did not consume it in testing (`stoppedCleanly: false` in
  `report.json`) - the stage still passes, just ~60s slower per NeoForge server run while it waits out the
  stdin grace period before falling back to killing the process tree. Worth a follow-up look at whether MDG's
  run config needs `standardInput = System.in` (or equivalent) wired up explicitly to forward it.
