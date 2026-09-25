# Report: Sophisticated Building 4.3.0 on every Minecraft version (sessions 2026-09-24/25)

## Result
Sophisticated Building 4.3.0 exists for every Minecraft version that has a Sophisticated Backpacks (SB) release,
1.16.3 to 26.2, on Forge, NeoForge and Fabric wherever the loader exists, with SB integration wherever SB exists for
that loader and version. 16 branches `mc/<ver>`, all pushed, CI green on every branch. Support matrix with every jar,
range and dependency floor: README.md on main.

Final all-versions run (`scripts/test-all-versions.ps1`, parallel recipe from docs/TESTING.md, merged report in
`test-all-versions-final.md`): **196 pass, 0 fail, 0 warn, 74 n/a** (n/a = stage does not apply, e.g. GameTests on
Forge/NeoForge, or the plain client stage where `runSmokeClient` covers it). Per loader folder: build + unit tests
(Fabric 77, Forge/NeoForge 65), Fabric GameTests / 1.16.x server tests (17-18), dedicated dev server with SB evidence
where SB exists, `runSmokeServer` (3 checks, 9 with 6 `sb.*` on SB folders) and `runSmokeClient` (13 checks, 21 with
8 `sb.*` on SB folders, incl. the GUI checks: randomizer bag screens, player settings screen, modifier entry widgets,
SB upgrade settings tab). Every client ran muted on the second monitor, one window at a time.

| Branch (HEAD) | Jars (MC range) | SB |
|---|---|---|
| mc/1.16.3 (deb5e36) | fabric [1.16.3]; forge [1.16.3] | forge (SB 1.16.4-1.0.0.94, no Tool Swapper) |
| mc/1.16.5 (6a08ee2) | fabric [1.16.4,1.16.5]; forge [1.16.5]; forge-1.16.4/ [1.16.4] | forge, forge-1.16.4 |
| mc/1.17.1 (5b627fe) | fabric, forge [1.17.1] | forge |
| mc/1.18.1 (f36a787) | fabric [1.18,1.18.1]; forge [1.18.1]; forge-1.18/ [1.18] | forge, forge-1.18 |
| mc/1.18.2 (af0e315) | fabric, forge [1.18.2] | forge |
| mc/1.19.2 (68d0b45) | fabric [1.19,1.19.2]; forge [1.19.2]; forge-1.19/ [1.19,1.19.1] | fabric (1.19.2), forge, forge-1.19 |
| mc/1.20.1 (b2fa212) | fabric, forge [1.20.1] (Forge jar also runs on NeoForge 1.20.1, server smoke only) | fabric, forge |
| mc/1.20.4 (c2395fa) | fabric, neoforge, forge [1.20.4] | fabric, neoforge |
| mc/1.21.1 (fb10ef2) | fabric + neoforge [1.21,1.21.1]; forge [1.21.1]; forge-1.21/ [1.21] | fabric, neoforge |
| mc/1.21.4 (4e364ae), 1.21.5 (2961a48), 1.21.8 (49a4ed2), 1.21.10 (f4e9eac), 1.21.11 (a330541) | fabric, neoforge, forge [that version] | neoforge |
| mc/26.1.2 (1dc1803) | fabric + forge [26.1,26.1.2]; neoforge [26.1.2] | neoforge |
| mc/26.2 (07f41d4) | fabric, neoforge, forge [26.2] | neoforge |

## This session (2026-09-25, continuation)
- mc/1.16.5 finished (pb7): 1.16.4 verdict = Fabric jar widened to 1.16.4-1.16.5 (built and run on 1.16.4, real
  server), Forge 1.16.4 needs its own jar (SB 1.16.4 API differs) -> `forge-1.16.4/`. Real Forge 35/36 servers with SB
  and Fabric 1.16.4/1.16.5 servers reach Done. Dev-only fix: Forge 36.2 dev clients crashed baking models (Architectury
  Loom left SRG names in BlockMath) -> `fixDevMinecraft` task. Pushed as a new branch.
- mc/1.16.3 finished (pb8): merged final 1.16.5 (without forge-1.16.4 and fixDevMinecraft), GUI checks rewritten for
  SB 1.0.0.94, real Forge 34 server with SB. Pushed as a new branch.
- H5 GUI smoke checks verified and pushed on the 13 other branches (h5b-h5e); no src/main bug found. Creative tab on
  1.17.1/1.18.x lists all 16 items (temporary check) -> handoff finding closed.
- Build fixes: override exclusion decided at copy time on forge-1.21 and forge-1.19 (configuration cache compiled
  duplicates); FG7 merge-source-sets empty-jar fix on all 8 FG7 branches (processResources deleted classes restored
  from the build cache; release jars were never affected); 1.21.5 Forge smoke server now deletes its kept world
  (1.21.5 test framework does not clear test areas).
- test-all-versions: strict-mode safe JUnit parsing (1.16.x report), Gradle JDK per loader folder from `ci_gradle_jdk`
  (JDK 25 for 26.x), SB detection for older FML log formats, SB Core required only where the folder declares it,
  forge-<mc> override folders count as SB folders. 81 offline tests.
- Hub docs: README support matrix (one row per jar, from jar metadata), CHANGELOG 4.3.0, PORTING toolchain rows and
  gotchas, TESTING (JDK per folder, SB detection).

## Open points
- Fabric GameTest servers hung twice, only while three test instances ran in parallel (1.21.8 during a batch, 1.18.2
  at startup); both pass alone and in CI. If it recurs: take a thread dump (jstack) of the hung server before the
  20-minute timeout kills it. Workaround: run gametest stages with fewer parallel instances.
- Findings from the original code, waiting for the user's decision: PlayerSettingsGui is an unfinished stub; randomizer
  bag titles overflow their GUI texture; 6 unused widget classes in gui/elements (3 already gone on 1.21.10+).
- NeoForge smoke server skips `sb.worn_backpack` on Curios 12+ (fake player has no Curios slot); the client run covers it.
- On SB 1.17.1/1.18 the backpack screen is taller than the 854x480 smoke window (SB layout; checks pass).
- Real Forge 1.16.3/1.16.4 servers need a Java 8 older than 8u321 (modlauncher 8.0.x), documented.
- NeoForge 1.20.1 with the Forge 1.20.1 jar: server smoke only, no client run.

---

# Report: Sophisticated Building 4.2.0 + 4.2.1 (session 2026-09-24)

## Round 2 (4.2.1): the known limitations fixed
The user asked to fix the limitations listed at the end of the 4.2.0 report. Status per item:

| 4.2.0 limitation | 4.2.1 result | Commit |
|---|---|---|
| Survival undo charged 1 item for multi-item blocks (dupe) | Undo/redo charge the real count (double slab 2, candles/pickles/eggs/snow layers/petals N), all or nothing; restoring over the same block without mining charges only the difference. Redo has its own entry point, normal building unchanged. | 1ee43a6 |
| Merge rule accepted any +1 integer property (crop age etc.) | Only slab->double and +1 on CANDLES/PICKLES/EGGS/LAYERS/FLOWER_AMOUNT | 1ee43a6 |
| Server `skipFirst` compared by identity (never matched) | Means "vanilla handled the first block" (Disable mode without Quick Replace); compared by value; server honours it only while vanilla was not cancelled; single-block Disable clicks are not sent | 1ee43a6 |
| Normal placements skipped spawn protection | Spawn protection, world border and adventure-mode checks for every build-mode placement and break in every game mode; placement flag reset in `finally` | 1ee43a6 |
| NeoForge supply not capped by tier | NeoForge caps per build like Fabric, keeps the held-block anchor, tooltip restored, dead helper removed | 662285e |
| Fabric configs warned on every load / dropped unknown keys silently | Corrected automatically with `<name>.json.bak` (`-1`, `-2` ...) backup; unknown keys reported once | 4c81b1b |
| Only a login smoke test | 17 Fabric GameTests (`gradlew runGametest`) run the server-side rules in a real world: storage data, survival replace on/off/bedrock/no tool/delay, merges, undo counts and retry, skipFirst, world border, adventure mode, stack data on removal | 6ff2cb2, dc659fd |
| "No data fixer registered for" ERROR | Traced to the unofficial Sophisticated Backpacks Fabric port (`ModItems`: `EntityType.Builder...build("")`), not this mod; documented as a known issue | - |
| Found by the GameTests | Failed undo/redo entries now stay on their stack and are retried (message when nothing could be done); single-item inventory removal keeps stack data | dc659fd |

Release 4.2.1 (78e1a21): both jars exported, Fabric 77 unit tests + 17/17 GameTests, NeoForge build green, runtime smoke test on both loaders (world loads, player joins, no errors from this mod, no config backups created for valid files).

Remaining, deliberately unchanged:
- Survival mod-breaking refuses blocks with hardness > 0 without a tool (4.1.0 balance design in `ToolSelector`).
- Undoing a snow-layer merge (no mining) removes the extra layer without refunding it (loss of 1 snow, no dupe).
- Spawn protection cannot be exercised in the GameTest server (its check always returns false there); covered by the world-border test and code review.
- Disable mode + Quick Replace on a single block shows no preview outline (`BlockPreviews` hides single-block Disable previews); functional behaviour is correct.
- A failed normal placement (not undo) still counts toward the item charge, as before 4.2.0.

Additional manual checks for 4.2.1: Disable mode plain / with mirror / with Quick Replace (creative and survival), multi-item undo cycles (double slab, 3 candles, 4 pickles), building over crops/composter/respawn anchor, NeoForge Building Upgrade tier cap and anchor, a Fabric config with an out-of-range value (backup created once, quiet afterwards).

---

# 4.2.0 report

## What was implemented, per requirement

| Requirement | Result |
|---|---|
| GitHub #1: crash placing Create Aeronautics/Simulated redstone magnets | The crash itself was already fixed in 4.0.0 (the report came from an older build; `MyPlaceContext` now gets the real player). 4.2.0 adds a safety net: an exception from another mod's `getStateForPlacement` during the preview marks that entry invalid and logs once per block, instead of crashing the client tick. Both loaders. |
| GitHub #3: backpack items not used | Audited on both loaders: with an **enabled Building Upgrade** the backpack supplies blocks, without one it does not (the Refill upgrade is not involved). Bugs found and fixed: Fabric reserved the last held block even when no backpack had that item; NeoForge counted Curios-worn backpacks twice (HUD and extraction doubled); tick-time sync caught only `LinkageError`; NeoForge tooltip promised a cap NeoForge does not apply. |
| GitHub #4: storage blocks lose contents/name | Placement now uses a real inventory stack as template (main hand, offhand, inventory) and applies its data like vanilla `BlockItem.place` (BLOCK_STATE, BLOCK_ENTITY_DATA incl. op check, block-entity components, `setPlacedBy` with the player). Survival consumes that exact stack; creative copies it. Both loaders. |
| Survival replace config option (default off) | `SurvivalReplace.enabled` (Fabric `config/sophisticatedbuilding-server.json`, NeoForge per-world `sophisticatedbuilding-server.toml`), default `false`. When on: radial-menu replace modes and Quick Replace in survival; replaced blocks are mined with survival-breaking rules (best tool incl. Tool Swapper, durability, drops to inventory, hunger, protected/unbreakable skipped, mining delay + "Replacing N blocks" countdown); undo/redo never creates blocks or items for free; same-block merges (slab to double slab, +1 candle/pickle/egg) still cost one item. |
| Fabric config files (needed for the option) | Fabric had no config files. Now `config/sophisticatedbuilding-{common,server,client}.json` with the NeoForge names, comments and ranges; missing keys added, out-of-range values clamped, broken JSON left untouched; server values synced to clients on join (whitelist excluded) and restored on disconnect. |
| Release | 4.2.0: version bump, `PATCH_NOTES_4.2.0.md`, both jars exported to `ExportedJars/`. |

## Commits (main)
- `9449987` Fabric JSON config files + sync (T1)
- `b0b9f30` #4 storage-block data, #1 safety net, survival undo-of-break NPE (T3)
- `43f9d93` survival replace on Fabric (T2)
- `f7038e7` NeoForge parity for T2/T3 + config entry (T6)
- `c606573` Building Upgrade follow-ups from the #3 audit (T5b)
- `a8eb63e` release 4.2.0 (T7)
- final commit: graph refresh, report, test-log ignore rule

## Changed files and why (main ones)
- Fabric `config/*` (ConfigSpec, ConfigFile, ModConfigs, SimpleConfigValue), `ServerConfig/CommonConfig/ClientConfig`, `ServerConfigSyncPacket`, `PacketHandler(Client)`, `FabricBootstrap`, `FabricClientEvents`, `FabricCommonEvents`: config files and sync.
- Both loaders: `ServerBlockPlacer` (template placement, replace actions, delay), `BlockPlacerHelper` + `BlockHelper` (apply item data with the player), `PlacementTemplates` + `TemplateSelector` (which stack is the template, exact-stack consumption), `ItemUsageTracker` (individually consumed / take-back counts), `InventoryHelper` (bulk removal skips stacks with data; Fabric anchor fix), `ReplaceRules` + `BlockUtilities` (pure replace/merge decisions), `BuilderChain` (survival preview, null-state entries), `BlockEntry` (#1 safety net), `PowerLevel` (gate), `BreakCountdownPacket` + `ClientBreakCountdown` + `RenderHandler` + `en_us.json` (replace countdown HUD).
- NeoForge `BuildingUpgradeHelper` (Curios dedup), `CommonEvents` (wider catches), `ServerConfig` (new entry).
- Tests: `ConfigSpecTest`, `TemplateSelectorTest`, `ReplaceRulesTest`, `BackpackContributionTest` additions.

## Key decisions
See `decisions.md`. Most important: survival replace reuses survival-breaking rules; the server now refuses survival overwrites of non-replaceable blocks when replace is off (closes a modified-client hole); same-block targets are only placed as one-step merges; NeoForge keeps its uncapped Building Upgrade supply and gets an honest tooltip; server `skipFirst` identity quirk left as is because Quick Replace in Disable mode depends on it.

## Checks and tests run
- Fabric `gradlew build`: green after every task; final 60 tests, 0 failures (was 23 at session start).
- NeoForge `gradlew build`: green after T6, T5b, T7 (no test source set).
- `rebuild_all_and_export_jar.ps1`: both 4.2.0 jars built; versions verified inside the jars.
- Runtime smoke test with the exported jars (both loaders, dev instances, quick-play into a world): world loads, player joins, no errors from this mod; Fabric config files generated with `SurvivalReplace.enabled: false`; NeoForge server toml has `[SurvivalReplace] enabled = false`.
- Diff scan: no TODO/FIXME/debug output added.
- Graphify graph refreshed incrementally (5634 nodes, 14992 edges, 255 communities; existing labels kept).

## Manual in-game test checklist (not automatable here)
1. #4 survival: hold 3 different filled + renamed shulkers (selected slot, slot 5, slot 20), Line mode, place 3 -> each keeps its contents and name, none left in inventory.
2. #4 mixed: 2 empty shulkers in hand + 1 filled elsewhere, place 3 -> two empty, one filled, inventory empty.
3. #4 creative: filled shulker, 3x3 wall -> all 9 have the contents, held box stays. Also a Supplementaries sack with items and a named furnace.
4. #4 undo: undo a placed filled shulker -> drops with contents; redo after picking it up -> contents back, item consumed.
5. #1: with Create Aeronautics/Simulated, preview and place redstone magnets in several modes -> no crash, at most one warning per block type in the log.
6. #3 Fabric (Nytheria, Trinkets back slot): enabled Building Upgrade, blocks in the backpack -> builds take them; disabled -> not; holding the last block of an item the backpack does not contain -> it can be placed.
7. #3 NeoForge with Curios: backpack worn in a Curios slot -> HUD count equals the real backpack contents (not doubled).
8. Survival replace OFF: no replace buttons in the radial menu; building works as in 4.1.1.
9. Survival replace ON (edit server config, rejoin): Blocks-and-air over stone/air/grass -> stone mined with the pickaxe (durability down, cobblestone to inventory, hunger), air/grass filled, countdown shown, blocks appear when it ends. Only blocks / filtered by offhand / Quick Replace in Disable mode behave accordingly.
10. No pickaxe over stone/obsidian -> targets red, nothing mined or charged. Tool Swapper backpack pickaxe is used; `stopBeforeToolBreaks` respected.
11. 3 blocks for 5 targets -> only 3 mined and replaced, exactly 3 consumed. Spawn protection (non-op, dedicated server) -> protected targets skipped.
12. Slab onto the same slab (config off and on) -> double slab for 1 slab; stairs onto stairs facing another way -> red, nothing changes.
13. Undo after a survival replace -> new blocks mined (drops to inventory), old blocks return only if their items are in the inventory.
14. Creative replace unchanged (instant, no drops, no countdown); mass breaking and its countdown still work.

## Known limitations and recommendations
- Only a login smoke test ran automatically; the gameplay items above still need an in-game pass (Nytheria instance for Fabric).
- Survival undo of a break charges `asItem()` once, so undoing a broken double slab / several candles costs 1 item for a block worth more (small dupe per undo cycle). It crashed before 4.2.0. Recommend a follow-up that counts items from the state (slab type, candle count).
- The merge rule (one integer property +1) also accepts states like crop age from a modified client; each costs a full item, no duplication.
- Fabric JSON configs: a rewrite for missing keys drops user-added unknown keys; a clamped or wrong-typed value warns on every load until fixed.
- NeoForge Building Upgrade supply is not capped by tier and NeoForge has no held-block anchor (by design; tooltip reworded). Decide later whether both loaders should share the Fabric cap.
- Server `skipFirst` compares `BlockPos` by identity and never matches after decoding; left as is (Quick Replace in Disable mode relies on it). Worth a deliberate redesign.
- Normal survival placements still skip the spawn-protection `mayInteract` check (unchanged from before).
- Fabric log shows a pre-existing `No data fixer registered for` ERROR at init (also in 4.1.x logs and the user's instance); harmless, not from this release.
