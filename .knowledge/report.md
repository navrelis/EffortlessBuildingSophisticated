# Report: 5.1.0, reach re-sync after respawn + balanced survival breaking (session 2026-10-19)

Earlier reports (4.3.0/5.0.0 multi-version work, 5.0.1, 5.0.2): `git log -p -- .knowledge/report.md`.

## Result
Released as **5.1.0** on CurseForge (project 1414718): 48 files, ids 9254487-9254534, all 17 branches and all their
loaders. Tags `v5.1.0` (main) and `v5.1.0+<mc>` (17 branches) pushed. GitHub CI green on all 17 release commits.

## Requirement -> implementation
- **Reach upgrades permanent after consumption.** The server always kept the power level (saved, copied on clone).
  The bug was on the client side: after a respawn the new client player was never told its level again, so reach and
  limits acted like level 0 until relog. Fix: `CommonEvents.onPlayerRespawned` sends `PowerLevelPacket` (all branches).
- **Balanced survival breaking (optional, off by default).**
  - On/off: dedicated server -> server config `BalancedBreaking.enabled`; singleplayer/LAN host -> client config
    `Gameplay.balancedBreaking` (toggle in the player settings GUI) or the server config.
  - Scope: survival build-mode mass breaking and survival replace mining. One-block operations, creative and placing
    are exempt.
  - Cost: tool durability x5 per block (`durabilityMultiplier`, 1-20; never breaks the tool when
    stopBeforeToolBreaks applies), exhaustion 0.075 per block, refused at food level <= 6.
  - Cooldown: one operation = one use; uses in the last 30 real-time minutes n; 15 s while n <= 10, then +5 s per use,
    max 120 s. Stored as epoch millis in the player's persistent data, so it survives relog, offline time, restart and
    death (Forge/NeoForge clone hook copies it).
  - Feedback: action-bar refusal (cooldown seconds / too hungry) and a HUD cooldown line; S2C `BalanceStatePacket`.

## Changed files (per branch, same set; reference mc/1.21.1 942d396, 32210c9, ab565a5)
- New: `utilities/BalanceCooldown` (pure logic), `utilities/BalanceRules`, `systems/BalancedBreaking`,
  `network/message/BalanceStatePacket`, `client/ClientBalanceState`; tests `BalanceCooldownTest`, `BalanceRulesTest`,
  Fabric `BalancedBreakingGameTest` (9).
- Changed: `ServerConfig` (BalancedBreaking section), `ClientConfig` (Gameplay), `CommonEvents` (respawn resync,
  `onPlayerCloned`), `ServerBlockPlacer`/`BlockPlacerHelper`/`BreakToolHelper`/`ToolSelector` (balanced flag,
  uses needed per tool), `RenderHandler` (HUD), `PlayerSettingsGui` (toggle), `ClientBreakCountdown`, `ClientEvents`,
  `PacketHandler`, `en_us.json`, Forge/NeoForge events (clone hook), smoke scenario `server.balanced_breaking` and its
  registration per loader, `TESTING.md`.
- Release: `changelog/PATCH_NOTES_5.1.0.md`, `gradle/shared.properties`, release jars + SHA256SUMS (17 branches);
  main: `CHANGELOG.md`, `release/curseforge-changelog-5.1.0.md`, `release/curseforge-description.md`, `README.md`.
- Version adaptations only: Optional `getLongArray` (1.21.5+), `level().getServer()` (1.21.9+), HUD via
  GuiGraphicsExtractor (26.x), FriendlyByteBuf packet + Consumer `hurtAndBreak` (<= 1.20.4), TranslatableComponent
  (<= 1.18.2), Java 8 rewrites without records/var/pattern instanceof (1.16.x), 1.16 server-test runner.

## Key decisions (details in decisions.md)
- Dedicated server: server config only; integrated server: host's client setting OR server config.
- Real-time epoch millis in player data (survives offline time); linear, predictable cooldown formula.
- One-block operations exempt; checks and use recording at request time.
- Split pure cooldown logic (Sonnet) from the integration (Opus) to keep Opus on the subtle part.

## Checks run
- Lead review of every port: new classes diffed against 1.21.1/1.19.2 (identical except the adaptations above);
  changed hunks compared line by line against the reference port.
- `test-all-versions` (build, gametest, server, smoke runSmokeServer), all 17 branches, two parallel instances:
  **161 pass / 0 fail / 31 n/a** (local/test-reports/v510-final). Unit tests 150 (Fabric) / 136 (Forge, NeoForge);
  Fabric GameTests 46-48 per branch; dedicated servers reach Done (SB verified where it exists);
  `server.balanced_breaking` passed in every smoke run.
- 48 release jars: version 5.1.0, balance classes present, no test/smoketest classes, no nested jars.
- CurseForge plan metadata identical to 5.0.1 apart from the version (MC versions, loaders, Java, relations).
- GitHub CI green on all 17 release commits.

## Manual test checklist (needs a game client)
- Die with reach upgrades -> after respawn reach and limits are correct without relog.
- HUD cooldown line position and colour; action-bar texts (cooldown / too hungry); the pending outline vanishes on
  refusal.
- Player settings GUI "Gameplay / Balanced Breaking" row + tooltip; toggling in singleplayer takes effect at once;
  LAN guest follows the host.
- Dedicated NeoForge/Forge server: config reload flips the state; real death keeps the cooldown; relog/offline keeps it.
- Client preview tool icons/counts with balanced breaking on.

## Model usage
Work units (long = 2, short = 1): Opus 2 (A3 balanced-breaking integration) / Sonnet 16 (A1, A2, A5, F1, F2, B1, B2,
B2a, B2b, graph refresh) -> Opus ~11%. No precision overrides.

## Known limitations and recommendations
- Redo of a balanced break is not balanced (undo/redo bypass the balance rules).
- The break animation of `hurtAndBreak(..., MAINHAND)` uses the main-hand slot even if the tool came from the
  inventory/backpack (cosmetic, same as the existing mineBlock path).
- 5.1.0 client and server must match (new S2C packet).
- Client smoke runs (`runSmokeClient`) were not run this release; do the manual checklist above in a client.
- `graphify update .` must not be used on this repo (it drops `versions/1.21.1`); use the incremental Python recipe.
- Knowledge graph: incremental AST refresh of the 28 changed versions/1.21.1 files (5220 nodes, 15644 edges,
  213 communities; old communities kept as they were, 4 new ones labelled: Balanced Breaking Rules & System,
  BalanceCooldown & Tests, Balanced Breaking GameTest (Fabric), Balance State Sync). manifest.json was already stale
  for ~100 other 1.21.1 files; a future incremental run will re-extract them.
