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
neoforge/           standalone NeoForge (ModDevGradle) Gradle build
forge/              standalone Forge (ForgeGradle) Gradle build, where applicable
<loader>/release/   built jars for that branch
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
release, and `docs/ARCHITECTURE.md` for how the common/platform-services split works inside a
version branch.

## Support matrix

"SB" = Sophisticated Backpacks integration available for that loader/version. "yes" = the mod
works standalone there because Sophisticated Backpacks has no build for that loader/version at
all (nothing to integrate with). "-" = that loader isn't targeted for that branch.

| Branch | Covers MC | Forge | NeoForge | Fabric | Status |
|---|---|---|---|---|---|
| `mc/26.2` | 26.2 | yes | SB | yes | planned |
| `mc/26.1.2` | 26.1, 26.1.1, 26.1.2 (NeoForge: 26.1.2 only) | yes | SB | yes | planned |
| `mc/1.21.11` | 1.21.11 | yes | SB | yes | planned |
| `mc/1.21.10` | 1.21.10 | yes | SB | yes | planned |
| `mc/1.21.8` | 1.21.8 | yes | SB | yes | planned |
| `mc/1.21.5` | 1.21.5 | yes | SB | yes | planned |
| `mc/1.21.4` | 1.21.4 | yes | SB | yes | planned |
| `mc/1.21.1` | 1.21.1 (1.21 planned) | yes | SB | SB (unofficial port) | **fabric + neoforge: released**, forge: in progress |
| `mc/1.20.4` | 1.20.4 | yes | SB | SB (unofficial port) | in progress |
| `mc/1.20.1` | 1.20.1 | SB (jar also runs on NeoForge 1.20.1) | - | SB (unofficial port) | planned |
| `mc/1.19.2` | 1.19, 1.19.1, 1.19.2 | SB | - | SB (unofficial port) | planned |
| `mc/1.18.2` | 1.18.2 | SB | - | yes | planned |
| `mc/1.18.1` | 1.18, 1.18.1 | SB | - | yes | planned |
| `mc/1.17.1` | 1.17.1 | SB | - | yes | planned |
| `mc/1.16.5` | 1.16.4, 1.16.5 | SB | - | yes | planned |
| `mc/1.16.3` | 1.16.3 | SB | - | yes | planned |

Only `mc/1.21.1` (fabric + neoforge) is currently released. `mc/1.21.1` forge and `mc/1.20.4` are
in progress. Every other branch is planned but not started yet — see `docs/PORTING.md` for the
toolchain each one will use and the API breaks to expect.

## Repository layout (main / hub)

```
README.md                this file
CHANGELOG.md              overall changelog across every mod version and Minecraft version
LICENSE.txt                LGPL-3.0-only
docs/
  ARCHITECTURE.md          how a version branch's common/ + loader builds + platform services fit together
  PORTING.md               how to create/port a new mc/<version> branch, toolchain matrix, API-break list
  RELEASING.md             how to cut a release: version bump, build, jars, changelog, tagging
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
