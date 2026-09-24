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
