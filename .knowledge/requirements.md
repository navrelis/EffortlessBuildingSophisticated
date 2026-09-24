# Requirements (session 2026-09-24)

## Goal
1. Fix the open GitHub issues on navrelis/EffortlessBuildingSophisticated:
   - #1 crash placing Create Aeronautics/Simulated redstone magnets (NPE: BlockPlaceContext.getPlayer() null)
   - #3 items stored in backpacks are not used for placing
   - #4 storage blocks (shulker boxes, Supplementaries sacks, placeable backpacks) lose contents and names when placed with build modes
2. New config option (default OFF) that lets survival players use the radial-menu replace modes
   (replace only air / blocks and air / only blocks / filtered by offhand, and Quick Replace).

## User answers
- Survival replace: the replaced block is mined with survival-breaking rules (best tool from inventory / Tool Swapper, durability, drops to inventory, food exhaustion, protected/unbreakable blocks skipped, mining delay). Quick Replace works in survival too when enabled.
- Fabric has no config files today (values hard-coded in SimpleConfigValue). Add JSON config files on Fabric via Gson (no new dependency) for all existing options plus the new one; server values synced to clients. NeoForge: new entry in the existing per-world SERVER config.
- #3: expected behaviour is "with a Building Upgrade in the backpack, builds take items from it; without it they don't". Verify that this is true on both loaders, fix whatever does not work, then answer the ticket and close it.
- Git: push to `main` (repo convention), release as 4.2.0 (patch notes + exported jars), then comment on #1/#3/#4 and close them.

## Constraints
- Minecraft 1.21.1. Fabric Loader 0.18.6 / Fabric API 0.116.6 (primary), NeoForge 21.1.217 project (compiles against Sophisticated Core 1.21.1-1.5.1.2341 / Backpacks 1.21.1-3.26.3.2158).
- Fabric Sophisticated ports: Core 1.2.9.21.168, Backpacks 3.23.4.3.106, Storage 1.3.7.9.139.
- Both loaders must build; Fabric unit tests must stay green; keep feature parity.
- No new dependencies. Sophisticated integration must stay optional and guarded (Exception | LinkageError).
- Commit trailer per session reminder: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Never stage `graphify-out/` in feature commits; refresh the graph at the end.

## Round 2 (user, 2026-09-24): fix the known limitations from report.md
- NeoForge: cap Building Upgrade supply per build to the tier like Fabric (with held-block anchor), restore the tooltip.
- Survival undo must charge the real item count of multi-item states (double slab 2, candles/pickles/eggs/snow layers/petals N); merge rule limited to real "count" properties.
- Server skipFirst redesign: honour it only when vanilla handled the first block (Disable mode without Quick Replace), no chat spam.
- Spawn protection / adventure-mode checks for every build-mode placement and break (vanilla parity); isPlacingOrBreakingBlocks reset in finally.
- Fabric JSON config: correct and back up files with clamped/invalid values like NeoForge, so warnings don't repeat.
- Investigate the Fabric "No data fixer registered for" startup ERROR; fix if it is ours.
- Automated in-game coverage: Fabric GameTests for the server-side placement/replace/undo rules.
- Release as 4.2.1 (4.2.0 jars may not be published; 4.2.1 includes everything).
