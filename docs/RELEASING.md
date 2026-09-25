# Releasing

How to cut a release for one or more version branches. A release is per-branch (each
`mc/<version>` builds and ships its own jars), but a mod-version bump (e.g. 5.0.0 -> 5.1.0) is
usually rolled out across every branch that's actually released together — check `README.md`'s
support matrix for which branches are released before assuming a version bump applies everywhere.

Short version for a release of every branch (details in the sections below):

```powershell
pwsh scripts/bump-version.ps1 -Version 5.0.0 -WhatIf   # review what would change
pwsh scripts/bump-version.ps1 -Version 5.0.0           # every versions/<mc> worktree: version, patch notes, jars
# review and commit in each worktree, push the branches
# CHANGELOG.md + release/curseforge-changelog-<version>.md on main
pwsh scripts/curseforge-upload.ps1 -ExpectVersion 5.0.0                      # dry run: plan table, ids resolved
pwsh scripts/curseforge-upload.ps1 -Upload -Plan local/curseforge-plan-5.0.0.json
```

## 1. Version bump

`gradle/shared.properties` holds the version of every loader folder of a branch:

```
mod_version=<new version>
```

This is the single source of truth every loader's `settings.gradle` reads `mod_version` from, so
one edit updates the version for every loader folder in that branch. Bump `minecraft_version` /
`minecraft_version_range` too if the release also changes the Minecraft version target (that's a
port, not just a version bump — see `docs/PORTING.md`).

`scripts/bump-version.ps1 -Version <v>` does the whole bump on every worktree under `versions/` (or `-Mc <list>`, or
the worktrees under `-VersionsDir <dir>`):

1. `mod_version` in `gradle/shared.properties` and in any loader folder's `gradle.properties` that overrides it;
2. `changelog/PATCH_NOTES_<old>.md` -> `PATCH_NOTES_<v>.md` (`git mv`), the old version in it replaced (header, jar
   names) and the `*Draft — lead to confirm before release.*` line removed. If `PATCH_NOTES_<v>.md` already exists
   (e.g. hand-written bug-fix notes drafted ahead of the bump), it's left as-is except the draft line is still
   stripped, and `PATCH_NOTES_<old>.md` is left untouched as history — no rename, no rewrite;
3. `README.md` / `TESTING.md`: the old version replaced (jar names), except on lines that record a test run of one
   specific old jar (a `SHA-256` or an abbreviated hash `1234abcd...` on the line), or that name
   `PATCH_NOTES_<old>.md` while that file was kept rather than renamed (previous point); those stay as history and
   the script lists them;
4. unless `-NoBuild`: `gradlew build --no-daemon` in every loader folder with `JAVA_HOME` = the JDK named by that
   folder's `ci_gradle_jdk` (resolved like `scripts/test-all-versions.ps1`: `SB_JDK_<n>`, `~/.gradle/jdks/*-<n>-*`, the
   default `JAVA_HOME`), then the branch's `release.ps1 -NoBuild`: version check inside every jar, the old jar in
   `<loader>/release/` replaced, `SHA256SUMS.txt` rewritten.

It never commits or pushes. `-WhatIf` lists every change without writing anything. To try it without touching a
worktree: `git worktree add --detach local/bump-test/<mc> mc/<mc>` and `-VersionsDir local/bump-test -Mc <mc>`
(remove the worktree afterwards; on Windows the build folders need `rmdir /s /q \\?\<path>` for long paths).

## 2. Build and export

From inside the branch's worktree (`versions/<mc>/` locally):

```powershell
./build-all.ps1
# or, to build, verify and publish in one step:
pwsh ./release.ps1
```

`build-all.ps1` builds every loader folder in turn (`fabric`, `neoforge`, and `forge` where
present, plus any `<loader>-<mc>` folder such as `forge-1.21`) and stops at the first failure. Each
loader's jar lands in `<loader>/build/libs/`.

`release.ps1` (PowerShell 7, run from the branch's worktree root) does the same builds and then
publishes: for each discovered loader it runs `gradlew build` (skip with `-NoBuild` to reuse an
existing `build/libs` jar), locates the main jar
`<loader>/build/libs/<mod_id>-<loader>-<minecraft_version>-<mod_version>.jar` (for a `<loader>-<mc>`
folder: `<mod_id>-<loader>-<mc>-<mod_version>.jar`, e.g. `sophisticatedbuilding-forge-1.21-5.0.0.jar`), verifies
the version embedded in the jar's mod metadata (`fabric.mod.json` / `neoforge.mods.toml` /
`mods.toml`) matches `mod_version` from `gradle/shared.properties`, then clears `<loader>/release/`
and copies the new jar there together with a `<loader>/release/SHA256SUMS.txt`. It prints a
summary table and exits non-zero if any loader fails to build, produces no matching jar, or has a
version mismatch — that's where a version branch's shipped jars live, tracked in git so a release
is recoverable from the branch alone without rebuilding.

To build and export **every** released/in-progress branch in one pass from the hub, use
`scripts/build-all-versions.ps1` (see its `-Mc` filter to restrict to one branch).

## 3. Changelog

Update two places:

- The branch's own `changelog/` with the version-specific patch notes (mirrors the granularity of
  `docs/history/PATCH_NOTES_*.md` from before the multi-version restructuring).
- `main`'s root `CHANGELOG.md` with a summary section for the new mod version, in the same style as
  the existing entries — one paragraph or short bullet list per branch/loader if the release
  differs between them, otherwise one shared summary.
- `main`'s `release/curseforge-changelog-<mod version>.md`: the text every CurseForge file of the release gets as its
  changelog (markdown, players first; `scripts/curseforge-upload.ps1` reads it into the plan).

## 4. Tag

Tag convention: **`v<mod version>+<mc>`**, e.g. `v5.0.0+1.21.1`. Tag on the version branch itself
(not on `main`), after the version-bump commit:

```powershell
git -C versions/1.21.1 tag v5.0.0+1.21.1
git -C versions/1.21.1 push origin v5.0.0+1.21.1
```

One tag per (mod version, Minecraft version) pair — a mod version released across several branches
gets one tag per branch, all sharing the same `<mod version>` component.

## 5. Update the hub

Back on `main`: update `README.md`'s support-matrix status column for any branch that moved from
"in progress" to "released", and confirm `CHANGELOG.md` reflects the release (step 3).

## 6. Upload to CurseForge

Project: [effortlessbuilding-sophisticated](https://www.curseforge.com/minecraft/mc-mods/effortlessbuilding-sophisticated)
(id 1414718). `scripts/curseforge-upload.ps1` uploads every release jar of every branch through the CurseForge upload
API (`POST https://minecraft.curseforge.com/api/projects/<id>/upload-file`).

**Token.** Create an API token on CurseForge (account settings, API tokens) and put it into
`local/curseforge-token.txt` (git-ignored; first non-empty line) or `$env:CURSEFORGE_TOKEN`. The script sends it only
as the `X-Api-Token` header and never prints or logs it.

**Dry run (default).** `pwsh scripts/curseforge-upload.ps1 [-ExpectVersion 5.0.0]`

- Collects `<folder>/release/*.jar` of every local `mc/*` branch with `git show` (no worktree is touched; `-Remote`
  uses `origin/mc/*`, `-JarDir <dir>` takes the jars of one folder) into `local/curseforge-release/<mod version>/`.
- Reads every jar's own metadata (`scripts/lib/CurseForgeRelease.ps1`):
  - Minecraft versions: the `minecraft` dependency of `fabric.mod.json` / `mods.toml` / `neoforge.mods.toml`, expanded
    to the exact releases it names (`[1.16.4,1.16.5]`, `>=1.19 <=1.19.2`, `[26.1,26.1.2]`; open ranges are refused, and
    a version missing from the script's list of Minecraft versions fails loudly — add new versions there);
  - loader: Fabric / Forge / NeoForge from the metadata; the Forge 1.20.1 jar is listed for Forge and NeoForge (NeoForge
    1.20.1 loads Forge mods; tested there);
  - Java: the class file version of the mod's classes (Java 8, 16, 17, 21, 25);
  - environment: Client and Server (the mod is needed on both sides);
  - relations: Fabric API `requiredDependency` on Fabric jars; where the jar ships the Sophisticated Backpacks
    integration (`META-INF/services/...IBackpackIntegration`): `sophisticated-backpacks` (Forge/NeoForge) or
    `sophisticated-backpacks-unofficial-fabric-port` (Fabric) as `optionalDependency`, plus `curios` (Forge/NeoForge,
    worn backpacks) or `trinkets` (Fabric, declared in `suggests`) as `optionalDependency`;
  - display name `Sophisticated Building <version> - <Loader> <Minecraft>` (`1.16.4-1.16.5` for a range,
    `Forge/NeoForge 1.20.1`); release type `release` (`-ReleaseType` changes it for all files).
- With a token it resolves every game version name to its CurseForge id (read-only `GET /api/game/version-types` and
  `/api/game/versions`; Minecraft versions only from `minecraft-*` version types, loaders from `modloader`, Java from
  `java`, Client/Server from `environment`; a name CurseForge lists twice for the same kind stops the run) and checks
  each relation slug against its project id through `api.cfwidget.com`.
- Prints the plan table and writes `local/curseforge-plan-<mod version>.json` (jar paths, SHA-256, ids, relations, the
  changelog text from `release/curseforge-changelog-<mod version>.md`). Files are uploaded oldest Minecraft version
  first, so the newest version ends up on top of the CurseForge file list.

**Upload.** `pwsh scripts/curseforge-upload.ps1 -Upload -Plan local/curseforge-plan-<version>.json`

- Uploads exactly the files of that plan, and only if the plan is complete (every id resolved, a changelog) and every
  jar still has the SHA-256 recorded in the plan (otherwise make a new dry run). Asks for `UPLOAD` before the first
  file (`-Yes` skips the question).
- Resumable: each uploaded file is recorded at once in `local/curseforge-upload-log.json` (project, file, SHA-256,
  CurseForge file id); a rerun skips recorded files. The run stops at the first error and says so.

**Tests.** `pwsh scripts/curseforge-upload.offline-tests.ps1`: version range expansion, id resolution against
synthetic API lists, token lookup, and the real release jars of the branches (taken with `git show` into a temp
folder): loaders, Minecraft versions, Java, relations, display names. No network, no token.

## CI

Every version branch's CI is generic and identical: it lives once, as templates, on `main`, and is
copied verbatim onto each `mc/<version>` branch. There is no per-branch hand-editing of these
files — a branch that needs different behavior (a different `ci_gradle_jdk`, for instance) expresses
that through data (a `gradle.properties` line), not by forking the workflow or the scripts.

### The three infra files

`templates/branch/` on `main` holds the canonical copies of:

- `.github/workflows/build.yml` — the GitHub Actions workflow.
- `build-all.ps1` — same script described in step 2 above.
- `release.ps1` — same script described in step 2 above.

All three are loader-generic: they discover loader folders at runtime as "any top-level folder
containing both `settings.gradle` and `gradlew`" (today `fabric`, `neoforge`, `forge` across
various branches). None of them hard-codes a loader list, so the same three files apply unchanged
to every `mc/*` branch regardless of which loaders it targets.

### What `.github/workflows/build.yml` does

Runs on push/PR to `mc/**` and on manual dispatch, on `ubuntu-24.04` runners (pinned explicitly
rather than `ubuntu-latest`, since the `ubuntu-latest` label is scheduled to move from 24.04 to
26.04 in October 2026 — pinning keeps CI reproducible across that migration; bump the pin
deliberately in `templates/branch` when ready instead of picking up an unplanned runner change).

1. A `discover` job builds a loader matrix: for each loader folder, its name, its `ci_gradle_jdk`
   (see below), whether it has a `src/gametest` folder, and whether it has a `src/smoketest`
   folder (the in-game scenario harness, see `docs/TESTING.md` "runSmokeServer / runSmokeClient
   contract").
2. A `build` job runs once per loader in that matrix: `gradlew build --no-daemon --stacktrace`,
   then `gradlew runGametest --no-daemon --stacktrace` for loaders that have a gametest source set,
   then `gradlew runSmokeServer -PsmoketestOut=<dir> --no-daemon --stacktrace` for loaders that
   have a smoketest source set (headless — `runSmokeClient` needs a display, so it doesn't run in
   CI), then uploads the built jar (excluding `-sources`), test reports, and (`if: always()`, so a
   failing smoke run's result is still captured) the smoke test result/log as workflow artifacts.

Actions are pinned to the latest major release that runs on the Node 24 runner (GitHub deprecated
the Node 20 runtime across actions in 2025–2026): `actions/checkout@v7`, `actions/setup-java@v6`,
`actions/upload-artifact@v7`, `gradle/actions/setup-gradle@v6`. Bump these in `templates/branch`
and re-sync, not per branch, when a newer major ships.

### `ci_gradle_jdk`

Each loader's `gradle.properties` sets `ci_gradle_jdk`: the JDK **CI uses to run Gradle itself**,
independent of the compile toolchain (which `settings.gradle`'s foojay resolver auto-provisions).
Older branches need a different value here — e.g. ForgeGradle 6 needs JDK 17, Loom 1.18 needs
JDK 25 — because that Gradle/plugin combination cannot run on JDK 21. CI (and `sync-branch-infra.ps1`,
below) read/write `ci_gradle_jdk` per loader and default to 21 if the key is absent.

### Keeping branches in sync: `scripts/sync-branch-infra.ps1`

The workflow for changing CI/build/release behavior is always:

1. Edit the canonical files under `templates/branch/` on `main`.
2. Run `pwsh scripts/sync-branch-infra.ps1` (optionally `-Mc <version>[,<version>...]` to target
   specific branches, or `-Exclude <version>[,<version>...]` to sync every worktree except some) to
   copy the templates into each `versions/<mc>/` worktree and, for every loader folder found there,
   make sure `gradle.properties` has a `ci_gradle_jdk` line (appending one from `-GradleJdk`,
   default 21, if it's missing).
3. Review the diff summary the script prints (added/changed/unchanged per file), then `cd` into
   each affected `versions/<mc>/` worktree and commit the result there yourself — the script never
   runs `git add`/`commit`/`push` in any worktree.

`pwsh scripts/sync-branch-infra.ps1 -Check` is report-only: it makes no changes and exits non-zero
if any target worktree's infra files differ from the templates or is missing `ci_gradle_jdk` in a
loader's `gradle.properties`. This is meant to be wired into CI or a lint step later, to catch a
branch that drifted from the templates.
