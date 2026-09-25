# Round-2 port brief (every branch except mc/1.21.1)

Reference: mc/1.21.1, everything between fb10ef2 and 766d18f (`git -C versions/1.21.1 log --oneline fb10ef2..766d18f`,
`git -C versions/1.21.1 diff fb10ef2 766d18f -- <path>`). Guides: `.knowledge/round2/PORT_NOTES_COMBINED.md`
(all changes, files, version-specific APIs, expected counts) and `.knowledge/round2/PORT_NOTES_GUI_CURSOR.md`
(incl. the two "MUST GO FIRST" sections: cursor/focus-safe smoke clients).

## Port, in this order
1. MUST GO FIRST: the cursor/focus fix of the smoke client (ClientWindow.keepOffTheCursor, SmokeWindowMixin for every
   loader, early window off via fml.toml for Forge/NeoForge; on old versions find the equivalent of the early
   loading window, e.g. Forge 36-47 `earlyWindowControl`/splash settings, or confirm there is none).
2. Everything else from the reference: config set/save API + PlayerSettingsGui editor (radial button + unbound key),
   bag title fit + bag name, 6 dead widget classes removed (some branches already lack 3), Terrain Mound icons
   (copy icons.png from mc/1.21.1 if the atlas layout matches - check AllIcons), radial highlight + RadialButtonLayout,
   mini preview dead method removed, gameplay fixes (merge-undo refund incl. only the merge kinds that exist on that
   MC version, no charge for failed placements, full-count charge, stuck undo stack, Disable+Quick Replace preview),
   new GameTests (Fabric) / server scenarios, harness checks, TESTING/README/changelog Fixes section.
   Closest-equivalent where an API is missing; list every difference.
3. `pwsh scripts/sync-branch-infra.ps1 -Mc <ver>` (CI detects override folders' smoke harness).

## Test (fast, complete, always with Sophisticated Backpacks + Building Upgrade where SB exists)
- Headless now: build + unit tests every loader folder, Fabric runGametest, runSmokeServer every folder, release jars
  (release.ps1) + SHA256SUMS, CI=true for MDG Legacy, jar content (no smoketest classes).
- NO game clients until the lead says "clients allowed" (user order: the test clients grabbed the user's mouse; the
  fix must be on the branch first, and foreign Minecraft clients of another project are currently holding the mouse).
  When allowed: runSmokeClient per folder under the window lock, one at a time, with the cursor recorder
  (scratchpad\r2gui\proof-run.ps1 pattern: abort if the clip rect changes or our window gets the foreground).
- Fix every bug you encounter (minimal, list each).
- Commit locally per branch, do not push. Report per branch: commits, counts, check lines, differences, bugs fixed.
