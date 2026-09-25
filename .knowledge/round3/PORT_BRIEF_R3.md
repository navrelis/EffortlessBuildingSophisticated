# Round-3 port brief (5.0.1 fixes; every branch except mc/1.21.1)

Reference: mc/1.21.1 d8ab383..48261e8 (`git -C versions/1.21.1 log --oneline d8ab383..48261e8`). Guides:
`.knowledge/round3/PORT_NOTES_PLAY.md` (Fabric player data per version, server limit validation + CommonConfigSyncPacket,
Fabric break events + optional Common Protection API, array cap, offhand bag filter, material cost, translated server
messages, test players at their structures) and `.knowledge/round3/PORT_NOTES_GUI.md` (BuildModeHistory, dead
content removed, ~55 translation keys, LangKeysTest, typos).

1. Port everything, closest equivalent where an API is missing (list each difference). Note: test players must stand at
   their test structure or the new server reach check rejects every existing test.
2. Add `changelog/PATCH_NOTES_5.0.1.md` (header "5.0.1 - Minecraft <ver>", Fixes list true for this branch). Do NOT change
   mod_version (the lead bumps all branches to 5.0.1 afterwards with scripts/bump-version.ps1) and do NOT rebuild release jars.
3. Headless verification per loader folder: build + unit tests (incl. LangKeysTest), Fabric runGametest (or the 1.16/1.17
   server test runner), runSmokeServer and runSmokeServer -PsmokeNoSb=true. Fabric branches with the SB port:
   `pwsh scripts/check-fabric-no-sb-bytecode.ps1 -Mc <ver>` must report 0 differing classes.
4. NO game clients. Fix any bug you meet. Commit locally per branch, don't push. Report per branch: commits, counts,
   check lines, differences.
