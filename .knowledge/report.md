# Report: 5.0.2 hotfix, NeoForge BlockDropsEvent crash with Apothic Enchanting (session 2026-10-02)

Earlier reports (4.3.0/5.0.0 multi-version work, 5.0.1): `git log -p -- .knowledge/report.md`.

## Result
Released as **5.0.2** on CurseForge (project 1414718), 26 files, ids 9041129-9041156, for the 8 branches
mc/1.21.1, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 (all their loaders). Branches 1.16.3-1.20.4 stay on
5.0.1 (not affected). Tags `v5.0.2+<mc>` on the 8 branches and `v5.0.2` on main. CI green on all 8 branches (fix and
release commits).

## Requirement -> implementation
- **Crash** (user log: sophisticatedbuilding-neoforge-1.21.1-5.0.1, NeoForge 21.1.250, Apothic Enchanting 1.6.2;
  `UnsupportedOperationException` in `BoonComponent` during `ApothEnchEvents.blockDrops`): root cause
  `NeoForgeBlockEventHelper.onBlockDropsCollected` posted `BlockDropsEvent` with an immutable `List.of()`; listeners
  may add/remove drops. Fixed on every affected NeoForge folder (incl. `neoforge-26.1` on mc/26.1.2).
- **Correct drop semantics** (beyond the crash): the event now receives the real drops as mutable `ItemEntity`
  carriers (as NeoForge's `CommonHooks.handleBlockDrops`); what survives the event goes through the mod's existing drop
  callback (inventory/backpack); cancelled = no items, no XP. Fabric/Forge unchanged.

## Changed files (per branch, same set)
- `common/.../platform/services/IBlockEventHelper.java`: `onBlockDropsCollected` takes and returns the drops.
- `common/.../create/foundation/utility/BlockHelper.java`: `destroyBlockAs` uses the returned drops (player path).
- `neoforge/.../platform/NeoForgeBlockEventHelper.java` (+ `neoforge-26.1/` on 26.1.2): the fix.
- `fabric/.../FabricBlockEventHelper.java`, `forge/.../ForgeBlockEventHelper.java`: new signature, `return drops`.
- Smoke harness (dev-only): `ServerScenarios.DETAILS` public; new `NeoForgeDropsEventScenarios` with
  `server.drops_event_add/remove/cancel`; registered via `@GameTest` (1.21.1, 1.21.4) or NeoForge-only
  `test_instance`/`test_environment` JSON + `SmokeTestNeoForge` (1.21.5+). `TESTING.md` row.
- Release: `changelog/PATCH_NOTES_5.0.2.md`, `gradle/shared.properties`, release jars + SHA256SUMS (8 branches);
  main: `CHANGELOG.md`, `release/curseforge-changelog-5.0.2.md`, one line in `release/curseforge-description.md`.
- Knowledge graph: AST-only incremental refresh of `versions/1.21.1` + hub scripts (5083 nodes, 14965 edges,
  209 communities, 21 new communities hand-labelled).

## Key decisions (details in decisions.md)
- Real drops as ItemEntity carriers, never added to the world; cancel = nothing. Matches NeoForge's own flow.
- 5.0.2 only for the 8 affected branches, uploaded with `-JarDir` (no noise for unchanged versions).

## Checks run
- Regression proof: with `List.of()` restored, `drops_event_add/remove` fail with the user's
  `UnsupportedOperationException` (1.21.1, 1.21.5, 26.2); with the fix all pass.
- Lead: 1.21.1 fabric/forge/neoforge build + NeoForge runSmokeServer (15/15). `test-all-versions`
  (build, gametest, server, smoke runSmokeServer) on the 7 other branches: 30 pass + 43 pass, 0 fail; every NeoForge
  folder ran the 3 drops checks. Agents additionally ran `-PsmokeNoSb=true` on all NeoForge folders (pass).
- 26 release jars: version 5.0.2, no smoketest/gametest classes, no nested jars, NeoForge jars contain the fix.
- CurseForge dry run then upload (user OK), 26/26 recorded in `local/curseforge-upload-log.json`.

## Manual test checklist
- [ ] Apothic Enchanting (1.21.1 NeoForge, e.g. the reporting user's pack): tool with Boon, survival break with a build
  mode -> no crash, Boon extra drops arrive in inventory/backpack.
- [ ] Any auto-smelt/telekinesis-style enchant on a NeoForge 1.21.x pack: drops modified as with vanilla breaking.
- [ ] Undo of a survival build still gives the blocks back (NeoForge).

## Known limitations / recommendations
- On a cancelled drops event `spawnAfterBreak` still runs (NeoForge skips it); only matters for blocks like infested
  stone. Left as is (no report, minimal change).
- Not tested against the real Apothic Enchanting jar; the smoke checks simulate a listener that adds/removes/cancels.
- Client smoke runs on round-2/3 code are still open (need the user's "Clients erlaubt", see handoff).
