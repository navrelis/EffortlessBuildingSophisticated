# Sophisticated Building

Advanced building tools for Minecraft — mirror, array, and shape-based block placement modes
(line, wall, floor, fill, filter, diagonal line/wall, randomizer bag, ...), with mass survival
breaking and an optional [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks)
integration (a Building Upgrade item that feeds a build straight from a backpack). Originally a
hard fork of *Effortless Building* by Requioss, maintained by Navrelis. Mod id `sophisticatedbuilding`,
license LGPL-3.0-only (see `LICENSE.txt`).

Public repository: <https://github.com/navrelis/EffortlessBuildingSophisticated>.

## How this repository works

`main` (this branch) is the **hub**: it has no game code of its own. Every Minecraft version the
mod supports lives entirely on its own branch, `mc/<version>` (e.g. `mc/1.21.1`, `mc/1.20.4`),
each with:

```
common/            loader-neutral code and assets — see docs/ARCHITECTURE.md
fabric/            standalone Fabric (Loom) Gradle build
neoforge/          standalone NeoForge (ModDevGradle) Gradle build, where applicable
forge/             standalone Forge Gradle build (ForgeGradle, ModDevGradle Legacy or Architectury Loom,
                   depending on the Minecraft version — see docs/PORTING.md)
<loader>-<mc>/     extra build for an older Minecraft version the main jar of that loader cannot run on
                   (forge-1.21/ on mc/1.21.1, forge-1.19/ on mc/1.19.2, forge-1.18/ on mc/1.18.1,
                   forge-1.16.4/ on mc/1.16.5)
<folder>/release/  built jars for that branch
```

A version branch is self-contained: clone it (or check it out as a worktree, see below) and build
it without touching any other branch. `main` only holds the pieces that describe and tie the
branches together — docs, the changelog, the upstream-jar manifest, and helper scripts.

Locally, each version branch is checked out as a **git worktree** under `versions/<mc>/` of this
same checkout (`versions/1.21.1` = branch `mc/1.21.1`, and so on). `versions/` is git-ignored from
`main` — it exists only on disk, never as tracked content here — because each worktree already
tracks itself on its own branch.

## Quick start

```powershell
# from the main checkout, PowerShell 7 (pwsh)
git fetch origin
pwsh scripts/setup-worktrees.ps1              # creates versions/<mc> for every origin/mc/* branch
pwsh upstream/fetch-upstream.ps1 -Mc 1.21.1   # run from the main checkout root — upstream/ exists only on main,
                                               # not inside a versions/<mc> worktree
cd versions/1.21.1
./build-all.ps1                                # builds every loader folder of that branch
```

See `docs/PORTING.md` for how to create a new version branch, `docs/RELEASING.md` for cutting a
release, `docs/ARCHITECTURE.md` for how the common/platform-services split works inside a version
branch, and `docs/TESTING.md` for `scripts/test-all-versions.ps1`, the one command that builds and
smoke-tests every version branch and loader.

## Support matrix

One row per jar. Each branch builds its jars from its loader folders and keeps them in `<folder>/release/`. Jar names
are `sophisticatedbuilding-<loader>-<minecraft>-4.3.0.jar`. The Minecraft range, the minimum loader version and the
optional Sophisticated Backpacks (SB) range are the ones declared in the jar's metadata (`fabric.mod.json`,
`META-INF/mods.toml` or `META-INF/neoforge.mods.toml`). Every declared version was run with the in-game smoke harness
(see `docs/TESTING.md` and the branch's `TESTING.md`).

"SB" column: "yes" = the jar has the Sophisticated Backpacks integration (Building Upgrade, backpack supply, tool list)
and uses it when SB is installed; the range is the declared optional dependency. "-" = no SB build exists for that
loader and Minecraft version, so the jar has no integration (the Building Upgrade items stay as inert placeholders and
their recipes are disabled). Fabric SB means the unofficial Fabric port of Sophisticated Backpacks; NeoForge and Forge
SB mean the official builds.

| Branch | Folder | Minecraft | Requires (minimum) | SB |
|---|---|---|---|---|
| `mc/26.2` | `fabric` | 26.2 | Fabric Loader 0.19.5, Fabric API 0.161.0 | - |
| | `neoforge` | 26.2 | NeoForge 26.2.0.88 | yes (Backpacks 3.26.2+, Core 1.5.0+) |
| | `forge` | 26.2 | Forge 65.1.3 | - |
| `mc/26.1.2` | `fabric` | 26.1, 26.1.1, 26.1.2 | Fabric Loader 0.19.5, Fabric API 0.155.3 | - |
| | `neoforge` | 26.1.2 only (NeoForge 26.1 and 26.1.1 were beta-only) | NeoForge 26.1.2.109 | yes (Backpacks 3.26.2+, Core 1.5.0+) |
| | `forge` | 26.1, 26.1.1, 26.1.2 | Forge 62.0.9 | - |
| `mc/1.21.11` | `fabric` | 1.21.11 | Fabric Loader 0.19.5, Fabric API 0.141.6 | - |
| | `neoforge` | 1.21.11 | NeoForge 21.11.45 | yes (Backpacks 3.26.2+, Core 1.5.0+) |
| | `forge` | 1.21.11 | Forge 61.2.1 | - |
| `mc/1.21.10` | `fabric` | 1.21.10 | Fabric Loader 0.19.5, Fabric API 0.138.4 | - |
| | `neoforge` | 1.21.10 | NeoForge 21.10.64 | yes (Backpacks 3.26.2+, Core 1.5.0+) |
| | `forge` | 1.21.10 | Forge 60.1.15 | - |
| `mc/1.21.8` | `fabric` | 1.21.8 | Fabric Loader 0.19.5, Fabric API 0.136.1 | - |
| | `neoforge` | 1.21.8 | NeoForge 21.8.54 | yes (Backpacks 3.26.2+, Core 1.5.0+) |
| | `forge` | 1.21.8 | Forge 58.1.22 | - |
| `mc/1.21.5` | `fabric` | 1.21.5 | Fabric Loader 0.19.5, Fabric API 0.128.2 | - |
| | `neoforge` | 1.21.5 | NeoForge 21.5.98 | yes (Backpacks 3.27.2+, Core 1.5.0+) |
| | `forge` | 1.21.5 | Forge 55.0.24 | - |
| `mc/1.21.4` | `fabric` | 1.21.4 | Fabric Loader 0.19.5, Fabric API 0.119.4 | - |
| | `neoforge` | 1.21.4 | NeoForge 21.4.157 | yes (Backpacks 3.27.2+, Core 1.5.0+) |
| | `forge` | 1.21.4 | Forge 54.1.5 | - |
| `mc/1.21.1` | `fabric` | 1.21, 1.21.1 | Fabric Loader 0.18.6, Fabric API 0.108.0 | yes on 1.21.1 (no Fabric port exists for 1.21) |
| | `neoforge` | 1.21, 1.21.1 | NeoForge 21.0.167 | yes (Backpacks 3.20.26+, Core 0.7.13+) |
| | `forge` | 1.21.1 | Forge 52.1.2 | - |
| | `forge-1.21` | 1.21 | Forge 51.0.33 | - |
| `mc/1.20.4` | `fabric` | 1.20.4 | Fabric Loader 0.19.5, Fabric API 0.97.3 | yes |
| | `neoforge` | 1.20.4 | NeoForge 20.4.251 | yes (Backpacks 3.20.6+, Core 0.6.21+) |
| | `forge` | 1.20.4 | Forge 49.2.9 | - |
| `mc/1.20.1` | `fabric` | 1.20.1 | Fabric Loader 0.19.5, Fabric API 0.92.12 | yes |
| | `forge` | 1.20.1; the same jar also runs on NeoForge 1.20.1 (server smoke test on 1.20.1-47.1.106) | Forge 47.1.3 | yes (Backpacks 3.26.3+, Core 1.5.1+), also on NeoForge 1.20.1 |
| `mc/1.19.2` | `fabric` | 1.19, 1.19.1, 1.19.2 | Fabric Loader 0.19.5, Fabric API 0.58.0 (mod id `fabric`) | yes on 1.19.2 (the Fabric port exists for 1.19.2 only) |
| | `forge` | 1.19.2 | Forge 43.5.2 | yes (Backpacks 1.19.2-3.20.2.1035+, Core 1.19.2-0.6.4.730+) |
| | `forge-1.19` | 1.19, 1.19.1 | Forge 41.1.0 | yes (Backpacks 1.19-3.18.9.661+, Core 1.19-0.4.10.87+, 1.19 builds only) |
| `mc/1.18.2` | `fabric` | 1.18.2 | Fabric Loader 0.19.5, Fabric API 0.77.0 | - |
| | `forge` | 1.18.2 | Forge 40.3.12 | yes (Backpacks 1.18.2-3.20.3+, Core 1.18.2-0.6.4+) |
| `mc/1.18.1` | `fabric` | 1.18, 1.18.1 | Fabric Loader 0.19.5, Fabric API 0.44.0 (mod id `fabric`) | - |
| | `forge` | 1.18.1 | Forge 39.1.2 | yes (Backpacks 1.18.1-3.15.15+, which contains Sophisticated Core) |
| | `forge-1.18` | 1.18 | Forge 38.0.17 | yes (Backpacks 1.18-3.12.1+, before the Core split) |
| `mc/1.17.1` | `fabric` | 1.17.1 | Fabric Loader 0.19.5, Fabric API 0.46.1 (mod id `fabric`) | - |
| | `forge` | 1.17.1 | Forge 37.1.1 | yes (Backpacks 1.17.1-3.12.3+) |
| `mc/1.16.5` | `fabric` | 1.16.4, 1.16.5 | Fabric Loader 0.19.5, Fabric API 0.42.0 (mod id `fabric`) | - |
| | `forge` | 1.16.5 | Forge 36.2.42 | yes (Backpacks 1.16.5-3.15.20+; no separate Sophisticated Core on 1.16.x) |
| | `forge-1.16.4` | 1.16.4 | Forge 35.1.37 | yes (Backpacks 1.16.4 builds from 1.16.4-3.0.0.289) |
| `mc/1.16.3` | `fabric` | 1.16.3 | Fabric Loader 0.19.5, Fabric API 0.25.0 (mod id `fabric`) | - |
| | `forge` | 1.16.3 | Forge 34.1.42 | yes (Backpacks 1.16.4-1.0.0.94+, the build tagged for 1.16.3; it has no Tool Swapper, so mass breaking never takes tools from backpacks) |

There is no NeoForge jar for Minecraft 1.20.1 and older (NeoForge 1.20.1 loads the Forge jar, see above; NeoForge
does not exist before 1.20.1). Minecraft versions not listed (1.19.3, 1.19.4, 1.20, 1.20.2, 1.20.3, 1.20.5, 1.20.6,
1.21.2, 1.21.3, 1.21.6, 1.21.7, 1.21.9) are not targeted: Sophisticated Backpacks has no release (non-beta) build for
them, see `upstream/README.md`.

See `docs/PORTING.md` for the toolchain of each branch and the API breaks between versions, and each branch's
`README.md` and `changelog/PATCH_NOTES_4.3.0.md` for its differences to `mc/1.21.1`.

## Repository layout (main / hub)

```
README.md                this file
CHANGELOG.md              overall changelog across every mod version and Minecraft version
LICENSE.txt                LGPL-3.0-only
docs/
  ARCHITECTURE.md          how a version branch's common/ + loader builds + platform services fit together
  PORTING.md               how to create/port a new mc/<version> branch, toolchain matrix, API-break list
  RELEASING.md             how to cut a release: version bump, build, jars, changelog, tagging
  TESTING.md               scripts/test-all-versions.ps1: build/gametest/server/client/smoke stages,
                           the runSmokeServer/runSmokeClient contract, SB coverage, reading the report
  history/                 superseded analysis notes and per-version patch notes, unchanged
templates/
  branch/                  canonical per-branch CI/build/release infra (.github/workflows/build.yml,
                           release.ps1, build-all.ps1), synced onto every mc/<version> branch — see
                           docs/RELEASING.md's CI section
scripts/
  setup-worktrees.ps1      creates/prunes the versions/<mc> git worktrees from origin/mc/* branches
  build-all-versions.ps1   runs build-all.ps1 inside every versions/<mc> worktree
  sync-branch-infra.ps1    copies templates/branch/ into one or more versions/<mc> worktrees, reports
                           a diff summary, never commits; -Check exits non-zero on drift
  test-all-versions.ps1    builds, GameTests, smoke-boots and (where present) runs the in-game smoke
                           harness for every versions/<mc> worktree and loader — see docs/TESTING.md
upstream/
  manifest.json            reproducible list of Sophisticated Backpacks/Core CurseForge files used for reference
  fetch-upstream.ps1        downloads the jars manifest.json describes
  update-manifest.ps1       re-discovers newest CurseForge releases and diffs them against the manifest
  README.md                 details on the upstream jars and how the manifest is built
.github/workflows/
  upstream-watch.yml        weekly check for new Sophisticated Backpacks releases, files a GitHub issue
.knowledge/                 project knowledge board (owned by the project lead, not touched by porting work)
graphify-out/                knowledge-graph output for this repository (see GRAPH_REPORT.md)
```

`versions/` and `local/` are git-ignored working directories, not tracked content — see the
"How this repository works" section above and `docs/PORTING.md`.
