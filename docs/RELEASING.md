# Releasing

How to cut a release for one or more version branches. A release is per-branch (each
`mc/<version>` builds and ships its own jars), but a mod-version bump (e.g. 4.3.0 -> 4.4.0) is
usually rolled out across every branch that's actually released together — check `README.md`'s
support matrix for which branches are released before assuming a version bump applies everywhere.

## 1. Version bump

On each branch being released, edit `gradle/shared.properties`:

```
mod_version=<new version>
```

This is the single source of truth every loader's `settings.gradle` reads `mod_version` from, so
one edit updates the version for every loader folder in that branch. Bump `minecraft_version` /
`minecraft_version_range` too if the release also changes the Minecraft version target (that's a
port, not just a version bump — see `docs/PORTING.md`).

## 2. Build and export

From inside the branch's worktree (`versions/<mc>/` locally):

```powershell
./build-all.ps1
```

This builds every loader folder in turn (`fabric`, `neoforge`, and `forge` where present) and
stops at the first failure. Each loader's jar lands in `<loader>/build/libs/`.

Copy the built jar(s) into `<loader>/release/` for the branch (create the folder if it doesn't
exist yet) — that's where a version branch's shipped jars live, tracked in git so a release is
recoverable from the branch alone without rebuilding.

To build and export **every** released/in-progress branch in one pass from the hub, use
`scripts/build-all-versions.ps1` (see its `-Mc` filter to restrict to one branch).

## 3. Changelog

Update two places:

- The branch's own `changelog/` with the version-specific patch notes (mirrors the granularity of
  `docs/history/PATCH_NOTES_*.md` from before the multi-version restructuring).
- `main`'s root `CHANGELOG.md` with a summary section for the new mod version, in the same style as
  the existing entries — one paragraph or short bullet list per branch/loader if the release
  differs between them, otherwise one shared summary.

## 4. Tag

Tag convention: **`v<mod version>+<mc>`**, e.g. `v4.3.0+1.21.1`. Tag on the version branch itself
(not on `main`), after the version-bump commit:

```powershell
git -C versions/1.21.1 tag v4.3.0+1.21.1
git -C versions/1.21.1 push origin v4.3.0+1.21.1
```

One tag per (mod version, Minecraft version) pair — a mod version released across several branches
gets one tag per branch, all sharing the same `<mod version>` component.

## 5. Update the hub

Back on `main`: update `README.md`'s support-matrix status column for any branch that moved from
"in progress" to "released", and confirm `CHANGELOG.md` reflects the release (step 3).

## CI

*(placeholder — to be completed later. This section will describe an automated build-and-tag
pipeline once one exists; until then, releases are cut by hand following the steps above.)*
