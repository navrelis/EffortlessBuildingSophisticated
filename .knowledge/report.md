# Report: 5.1.1 for 1.16.5, client smoke checks on all branches, 1.19.4 Forge pack fix (session 2026-10-19, sessions 3-5)

Earlier reports (4.3.0/5.0.0, 5.0.1, 5.0.2, 5.1.0): `git log -p -- .knowledge/report.md`.

## Result
- **5.1.1 released for mc/1.16.5 only** (3 jars). Tags `v5.1.1+1.16.5` (c4c7ba1) and `v5.1.1` (main 4771203) pushed. CurseForge project 1414718:
  fabric-1.16.5 id 9255707, forge-1.16.4 id 9255708, forge-1.16.5 id 9255709 (uploaded by this session from the approved plan; the upload log
  went 170 -> 173 entries). Note for the record: the team lead's later message "do not retry the upload" crossed with the upload; it had already
  gone through. No second attempt, no other upload.
- **Every Forge/NeoForge client folder on all 17 branches now passes `runSmokeClient`** (first time since 2026-09-25).
- **5.1.2 released for Forge 1.19.4 only** (user chose the version and approved the upload after the dry run): fix 2bda9e7, release 6c35ea7 on mc/1.19.4, main docs 4cef5d1, tags `v5.1.2+1.19.4` / `v5.1.2` pushed, CurseForge file id 9256239 (log 174 entries). The Fabric 1.19.4 jar is unchanged and stays 5.1.0 on CurseForge.

## Requirement -> implementation
- **K4 real bug (1.16.5):** `CommonConfigSyncPacket` was missing from `PacketHandler` CLIENTBOUND since 5.0.1 ("Unknown payload" on join, the client got no
  server limits). Fix 85d1399 (one registration, same as 1.16.3). Verified: runSmokeClient forge-1.16.4 24/24 (client.no_mod_errors, no
  common_config_sync error), forge 1.16.5 24/24, fabric 16/16; server run build/gametest/server/runSmokeServer 10 pass / 0 fail / 2 n/a.
- **K2 (1.21.1):** client checks `client.respawn_power_level`, `client.balanced_breaking`, `client.balanced_settings_toggle` + TESTING.md rows
  (29386a7). runSmokeClient fabric 27/27, neoforge 27/27, forge 19/19, forge-1.21 19/19.
- **K5 harness fix (dev-only, no release):** `SmokeWindowMixin` constructor `@Inject` was rejected by Mixin on Forge/NeoForge (harness bug since
  2026-09-25, Forge clients crashed at start). Form chosen per folder by the runtime Mixin version (client log) and javap of `At`:

  | Runtime Mixin | Form | Folders (commit) |
  |---|---|---|
  | 0.8.2 (1.16.3) | unchanged, accepted (24/24) | 1.16.3 forge |
  | 0.8.4 / 0.8.5 | `@Redirect` of the same INVOKE | 1.16.5 328c397, 1.17.1 2eaf855, 1.18.1 d229d05, 1.18.2 0b171a6, 1.19.2 0da9a88, 1.19.4 b0fe588, 1.20.1 8c798b5, 1.20.4 de87d9b |
  | 0.8.7 | `@At(unsafe = true)` | 1.21.1 2874661, 1.21.4 584ad8a, 1.21.5 6992721, 1.21.8 c034d72, 1.21.10 684ce99, 1.21.11 21c5618, 26.1.2 f63bbe2, 26.2 eb5b51b |
  | static `createGlfwWindow` | inject works, unchanged | 26.1.2 + 26.2 NeoForge (24/24; the K3 26.2 failure was the overload) |

  1.19.2: the old target `glfwCreateWindow` never matched (Forge 43's Window ctor calls `EarlyProgressVisualization.handOffWindow`); fixed.
  Each folder verified by its own runSmokeClient (reports in `local/test-reports/k5-port/`). `forge-<mc>` siblings compile `forge/src/smoketest`.
- **1.19.4 Forge pack bug (found by the K5 client run):** Forge 45 reads `forge:server_data_pack_format` / `forge:client_resources_pack_format`
  (`ForgeHooks.makePackFormatKey`); our `pack.mcmeta` used the pre-1.19.3 names, so the data pack was flagged TOO_NEW. Fix: both keys in
  `forge/src/main/templates/pack.mcmeta` and `forge/src/smoketest/resources/pack.mcmeta`, README.md and TESTING.md updated (2bda9e7). Only 1.19.4
  affected (Forge 43 and older read the old keys; 1.20+ use `supported_formats`/`min_format`). The world still loaded; the pack was only shown
  as made for a newer version.

## Changed files and why
- mc/1.16.5: `PacketHandler` (K4), `SmokeWindowMixin` forge (K5), release bump + 3 jars + SHA256SUMS + `changelog/PATCH_NOTES_5.1.1.md`.
- mc/1.21.1: `ClientScenarios`, `GuiScenarios`, TESTING.md (K2); forge/neoforge `SmokeWindowMixin` (K5).
- 15 more branches: `SmokeWindowMixin` forge/neoforge only (dev-only source set, not in release jars).
- mc/1.19.4: two `pack.mcmeta`, README.md, TESTING.md.
- main: `CHANGELOG.md` 5.1.1, `release/curseforge-changelog-5.1.1.md`, `release/curseforge-description.md`, `.knowledge/`, graphify-out.

## Key decisions (details in decisions.md)
- 5.1.1 only for 1.16.5 (client-side packet registration; server unchanged; precedent 5.0.2).
- K5 form by runtime Mixin version, not by Minecraft version; 1.16.3 left as is (verified).
- 1.19.4 fix held back from a release until the user decides (needs a new upload yes; version 5.1.1 vs 5.1.2 open).

## Checks run
- runSmokeClient on every Forge/NeoForge/Fabric folder touched (see above), client logs read for `MIXIN Subsystem Version`.
- 1.16.5 3 jars: version 5.1.1, no smoketest/gametest classes, no nested jars, common_config_sync in PacketHandler; CurseForge dry run
  metadata identical to the 5.1.0 plan entries apart from Path/Sha256/Source; SHAs match SHA256SUMS.txt. GitHub CI green on c4c7ba1.
- 1.19.4 full run after the pack fix (local/test-reports/p194-pack): fabric build 150 unit, gametest 47, server (SB verified), runSmokeServer 13;
  forge build 136 unit, server Done, runSmokeServer 7 -> 7 pass / 0 fail / 1 n/a. Forge client 16/16 incl. client.mod_data_pack_compatible.

## Manual test checklist (needs a human in a client)
- Forge 1.19.4 with the fix: data pack screen shows the mod pack as compatible.
- Dedicated NeoForge/Forge server: config reload flips balanced state; real death keeps cooldown; relog/offline keeps it.
- LAN guest follows the host's balanced setting.
- 1.21.1 balanced_refused screenshot: the red pending outline is still visible right after the refusal (check waits 60 ticks); verify
  "pending outline vanishes on refusal" by eye.

## Model usage
Work units (long = 2, short = 1), this session series: Opus 0 / Sonnet ~22 (K2 rows, K4, K5 port edits across 20+ files, 1.19.2 mixin fix,
1.19.4 pack fix + docs, K6 notes, graph refresh) -> Opus 0%. No precision overrides.

## Known limitations and recommendations
- `scripts/bump-version.ps1` rewrites TESTING.md history headings that name the old version ("### R3 (5.1.0 fixes ...)"); fix the script or
  check the headings after every bump (done by hand for 1.16.5).
- `graphify update .` must not be used (drops versions/1.21.1); use the incremental Python recipe.
