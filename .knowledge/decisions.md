# Decisions

- #1 is already fixed in code since 4.0.0 (MyPlaceContext receives the real player; crash log came from an older build). Add defensive try/catch around third-party getStateForPlacement. Reason: other mods can still throw during preview, and that runs in the client tick.
- #4 fix places with the real inventory stack's data components (vanilla BlockItem.place order: BLOCK_ENTITY_DATA, applyComponentsFromItemStack, setPlacedBy with the player). Reason: the server currently places `new ItemStack(item)`, which drops all data.
- Survival replace reuses the survival-breaking tool/durability/drops/delay code. Reason: user choice; consistent balance with 4.1.0 mass breaking.
- Fabric gets real JSON config files (Gson, bundled with Minecraft). Reason: Fabric had no config file at all, so a "config option" would otherwise be impossible.
- Fabric work first, NeoForge parity afterwards in one task. Reason: avoids two agents editing mirrored logic at the same time; Fabric is the primary target.
- T3 runs in a git worktree so it can build in parallel with T1 without half-written code from the other agent breaking its compile.
- Fabric server config sync skips server-only values (whitelist). Reason: clients never need them and names should not leak to every joining player.
- Undo of a survival break now costs the block's item (derived from the state). Reason: it previously crashed (new ItemStack(null)); a free re-place would duplicate items since the drops were kept.
- Server skipFirst uses identity (==) and never matches after decoding; left as is because Quick Replace in Disable mode relies on the server handling the first position. Reason: changing it would break creative/survival quick replace in Disable mode.
- Survival: same-block targets are placed only as one-step merges (slab->double, +1 candle/pickle/egg), never mined; other same-block cases skipped. Server now rejects survival overwrites of non-replaceable blocks when survival replace is off. Reason: prevents mine-1-pay-1-get-2 dupes and closes a modified-client overwrite hole.
- NeoForge keeps its uncapped Building Upgrade supply (tier only gates access); its tooltip is reworded instead of adding a cap + anchor. Reason: least-risk honest fix; a cap on NeoForge would need the Fabric anchor port and changes balance without being asked.
- Version 4.2.1 for the limitation fixes. Reason: 4.2.0 jars may already be published by the user; a new version is safe either way.
- The Fabric startup ERROR 'No data fixer registered for' is left alone: it comes from the unofficial Sophisticated Backpacks Fabric port (empty EntityType id), not from this mod.
- Survival mod-breaking still refuses hardness>0 blocks without a tool (4.1.0 design, documented in ToolSelector); not changed. Reason: deliberate balance decision from the survival-breaking design, not a defect.
- (Round 2, supersedes the NeoForge 'uncapped supply' decision above) NeoForge now caps Building Upgrade supply per build and keeps the held-block anchor like Fabric; tooltip restored. Reason: user asked to remove the loader difference.

## Multi-version session (2026-09-24)
- Scope = every MC version with an SB release, on every loader that exists for it; SB integration only where SB exists for that loader+version. Reason: user answer "every mc version where the SB release exists", original goal "available on Forge, NeoForge and Fabric".
- Layout per version branch: MultiLoader-Template style (`common/` + loader folders, platform Services). Reason: ~40 builds; hand parity of two full copies already produced drift (Curios dedup, lang keys, recipe names).
- Both config backends kept (Fabric JSON, NeoForge/Forge TOML) behind one common API. Reason: existing user config files keep working.
- SB integration behind a common interface with a no-op default, SB classes per loader folder. Reason: Fabric port and official SB APIs differ; loaders/versions without SB must compile without it.
- Create stack (Flywheel/Ponder/Catnip/Vanillin) dropped in favour of the vanilla preview renderer from the 1.21.11 draft. Reason: only one Catnip call site; the stack does not exist for most target versions or Forge; smaller jars.
- Upstream jars git-ignored, manifest + fetch script committed on main. Reason: third-party binaries, public repo.
- Builds must work from a clean clone (CurseMaven/Modrinth/public mavens only). Reason: CI and future-proofing; the old Fabric build depended on loom-cache files.
- Recipe file names unified to the NeoForge names (`decompress_compressed_*`). Reason: one common resource set.
- Standalone Gradle build per loader folder (common/ shared via srcDir, shared props in one root file), no root multi-project build. Reason: FG6 (Gradle 8.4-8.8) and Loom 1.18 (Gradle >=9.7, JDK 25) cannot share one Gradle build on older branches; same pattern on every branch.
- Vendored the needed Catnip classes (MIT) instead of the 1.21.11 fallback renderer. Reason: the fallback draws opaque ghosts without checkerboard faces; vendoring keeps the 4.2.1 look with no runtime dependency.
- Declared minimum loader versions = oldest version the code was actually run on. Reason: an untested low floor becomes a NoSuchMethodError crash for players.
- One canonical copy of per-branch infra (CI workflow, release.ps1, build-all.ps1) in templates/branch on main, copied by scripts/sync-branch-infra.ps1. Reason: 16 branches would otherwise drift.

## Session 2026-09-25, round 2 (user: "finish PlayerSettingsGui, fix randomizer bag, fix all bugs, all versions")
- PlayerSettingsGui becomes the in-game editor of the existing client config (Visuals + Performance: preview toggles, preview scale, appear/break animation length, render distance, limits, throttling). Reason: its "shader type/speed" controls had no feature behind them (no shader exists since the Create stack was dropped); the client config is the real per-player setting set. Opened from the radial menu (existing OPEN_PLAYER_SETTINGS action) and a keybinding (unbound by default, like other optional keys). Saves to the loader's client config file (Fabric JSON, Forge/NeoForge TOML) on Done/close.
- Randomizer bag titles: scaled down to fit the texture width (min scale), below that ellipsis + full title as tooltip. Reason: keeps the full name readable in every language and for renamed bags.
- The 6 unused widget classes in gui/elements are deleted on every branch. Reason: dead code, no screen uses them.
- The three bugs 4.2.1 left unchanged are fixed: snow-layer merge undo refunds the layer; a failed normal placement is not charged; Disable mode + Quick Replace on a single block shows its preview outline. Reason: user asked to fix all bugs.
- Order: reference on mc/1.21.1 (GUI + gameplay in parallel worktrees, lead merges), then ports per branch by the agents that did H5, then a full test-all-versions run.

## Round 3 (2026-09-25, after the 5.0.0 upload): bugs found while writing the description -> 5.0.1
- Fix: Fabric per-player data (power level, modifier settings) persisted; server-side validation of reach/build limits
  with the common config synced; Fabric claim/protection mods consulted (Fabric API break event, optional protection
  API); Array per-axis limit enforced; offhand-bag filter accepts only the bag's blocks; previous build mode tracks radial
  choices; dead lang/tooltips for unregistered items removed; hard-coded English -> translation keys; double-slab cost.
- Keep (earlier balance decisions): Protect Tile Entities also protects during survival mass breaking; survival mass
  breaking needs an effective tool. User may override.
