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
