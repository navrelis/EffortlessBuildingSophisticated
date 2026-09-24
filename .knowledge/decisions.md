# Decisions

- #1 is already fixed in code since 4.0.0 (MyPlaceContext receives the real player; crash log came from an older build). Add defensive try/catch around third-party getStateForPlacement. Reason: other mods can still throw during preview, and that runs in the client tick.
- #4 fix places with the real inventory stack's data components (vanilla BlockItem.place order: BLOCK_ENTITY_DATA, applyComponentsFromItemStack, setPlacedBy with the player). Reason: the server currently places `new ItemStack(item)`, which drops all data.
- Survival replace reuses the survival-breaking tool/durability/drops/delay code. Reason: user choice; consistent balance with 4.1.0 mass breaking.
- Fabric gets real JSON config files (Gson, bundled with Minecraft). Reason: Fabric had no config file at all, so a "config option" would otherwise be impossible.
- Fabric work first, NeoForge parity afterwards in one task. Reason: avoids two agents editing mirrored logic at the same time; Fabric is the primary target.
- T3 runs in a git worktree so it can build in parallel with T1 without half-written code from the other agent breaking its compile.
- Fabric server config sync skips server-only values (whitelist). Reason: clients never need them and names should not leak to every joining player.
- Undo of a survival break now costs the block's item (derived from the state). Reason: it previously crashed (new ItemStack(null)); a free re-place would duplicate items since the drops were kept.
