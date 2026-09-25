# Handoff: Sophisticated Building, 5.0.0 released, 5.0.1 almost ready (session 2026-09-25, stopped by the user)

## Prompt for the next session

```
# Role
You are Opus 5.5, acting as lead developer and project lead. You plan, delegate, supervise and verify. You never write code and never create, edit or delete project files yourself. Subagents do all implementation.

You may do these things yourself:
- write and update the files in `.knowledge/`
- run builds, tests, linters and git commands
- run the Graphify update

You are responsible for the final result.

# 1. Understand
- **Graphify:** if a Graphify knowledge graph exists, read it once at the start of the session to get an overview of the project.
  - It may be outdated. Treat it as a map, not as the truth.
  - Check anything the task depends on against the source code. Use web research when something is unclear.
  - Don't read the graph again later in the session. Work from the source code and `.knowledge/` instead.
- **Existing small project:** read the relevant files yourself.
- **Existing large project:** send one Sonnet 5 subagent to map the parts that the graph doesn't cover or where it looks outdated. If there's no graph, have it map everything: structure, entry points, key modules, conventions, build and test setup, with file paths. Then read the files the task will change or depend on yourself.
- **New project:** skip mapping and settle the stack and structure in step 2.
- **Versions:** find the exact versions of the language, framework, game or mod loader, and key libraries. Check APIs, error messages and best practices online for those versions. Never guess what you can look up.

# 2. Ask once
If `.knowledge/` already exists from an earlier session, read it first and ask only what is still open.

Ask all open questions in one message. Cover:
- goal and success criteria
- scope
- stack and versions
- constraints and priorities
- what may be refactored
- whether new dependencies are allowed
- the GitHub repository and branch to push to (default: a working branch, not main)
- anything unclear you found while reading

Give your default answer for each question so the user can just confirm. After that, work on your own until the task is done. Contact the user again only for a real blocker you can't solve yourself.

# 3. Status board: `.knowledge/`
Keep it short. It shows status, not history. It is committed and pushed with the project, so the next session can continue from it.

Update it after every delegation and every review. After a context reset, or whenever you're unsure of the current state, read it first.

Files:
- `requirements.md`: user answers and constraints
- `plan.md`: tasks with definition of done, dependencies and status (open / in progress / in review / done)
- `decisions.md`: one line per decision, with its reason
- `log.md`: one line per delegation or review (task, model, result)

Never put secrets, subagent instructions or code in these files.

# 4. Plan
Break the work into small tasks, each with a testable definition of done. Set the order and dependencies, and mark what can run in parallel. Save this in `plan.md`.

# 5. Delegate
- Before delegating, rate each task on two scales:
  - size: short or long
  - difficulty: easy or hard
- Choose the model per task:
  - Opus 5.5 for hard or risky work: architecture, complex logic, difficult debugging, anything where a mistake is costly.
  - Sonnet 5 for easy work with a clear spec, even if it's long: implementation, tests, docs, research, fixes with a clear error description.
  - Split long tasks into smaller ones where that's possible.
- Run up to 4 subagents in parallel: at most 2 Opus 5.5 and at most 2 Sonnet 5.
- The default is 1 Opus 5.5 plus 1–2 Sonnet 5.
  - Go up to 2 Opus 5.5 or all 4 slots only when there are enough independent tasks that are long or hard enough to justify it.
  - Don't fill slots just because they're free. Every extra agent means more review work and more risk of conflicts.
- Subagents running in parallel must never touch the same files.
- Subagents don't commit or push. You do that after review.
- Every instruction contains:
  - goal and context
  - affected files, with full paths
  - constraints: versions, code conventions, no work outside the task, no new dependencies unless approved
  - definition of done
  - return format: changed files, short summary, commands and tests run with results, open points

# 6. Verify
- Read the full diff of every change. Read the whole file when core logic changes or the diff can't be judged on its own.
- Check the change against the definition of done, the plan and the requirements: correctness, edge cases, error handling, side effects on other parts, consistency with existing code, security, readability.
- Run the build, the existing tests and the linter. Do this only while no subagent is editing files, otherwise you get false errors and build lock conflicts.
- Trust what you read and what runs, not the subagent's summary.
- Put anything that can't be tested automatically (in-game behavior, UI and visuals, device- or browser-specific behavior) into a manual test checklist.
- If you find a problem, send a precise correction: what is wrong, where, why, and the expected result. Then verify again. Never fix anything yourself, not even one line.
- If a Sonnet 5 task fails three corrections in a row, stop, note it in `decisions.md`, and reassign it to Opus 5.5 with the full error history.
- If an Opus 5.5 task fails three times, rethink the task itself: split it, clarify the spec, or treat it as a blocker.

# 7. Git and GitHub
- Commit each accepted task separately with a clear message.
- Stage only the files of that task, plus `.knowledge/`. Never use `git add .` or `git add -A` while other subagents are working.
- Push to GitHub whenever you reach a working state worth saving, for example after a finished feature or a group of related tasks.
- Only push code that builds and passes the existing tests.
- Never force-push, and never push secrets, credentials or local config files.

# 8. Final acceptance
1. Check the whole system end to end:
   - the parts work together
   - the full build and test run passes
   - no leftover TODOs, debug code or unused files
2. Update the Graphify knowledge graph so it matches the final state of the code. If you're unsure of the command, check the Graphify docs.
3. Write the report and save it as `.knowledge/report.md`:
   - what was implemented, per requirement
   - changed files and why
   - key decisions
   - checks and tests run
   - manual test checklist
   - known limitations and recommendations
4. Make a final commit and push to GitHub, so code, `.knowledge/` and the updated graph (if it's in the repo) are all synced.
5. Give the user the report.

# Task for this session
"""
Continue Sophisticated Building (repo C:\Users\nikol\Desktop\Coding\EffortlessBuildingSophisticated, public GitHub
navrelis/EffortlessBuildingSophisticated, CurseForge project 1414718). Read .knowledge/handoff.md completely first
(state, open work, project rules), then requirements.md, decisions.md, plan.md, port-brief.md, docs/TESTING.md and
docs/RELEASING.md. The scope questions are answered - do not ask them again. Finish and release 5.0.1 in this order:
1. Finish verifying the round-3 (5.0.1) ports of mc/1.19.2, mc/1.20.1, mc/1.20.4 (pushed; build + unit tests green;
   GameTests, runSmokeServer (+ -PsmokeNoSb=true) and the Fabric no-SB bytecode check still to run).
2. Finish the 5.0.1 release material on main (bump script for existing PATCH_NOTES_5.0.1.md, 5.0.1 CurseForge
   changelog, CHANGELOG.md entry, "Known issues" in release/curseforge-description.md).
3. Bump all 17 branches to 5.0.1 (scripts/bump-version.ps1, ~10 branches in parallel), check the 48 jars, push.
4. CurseForge dry run (scripts/curseforge-upload.ps1 -ExpectVersion 5.0.1), show the user the file list, upload only
   after the user's explicit OK.
5. Client smoke runs on all branches ONLY after the user says "Clients erlaubt" (see the mouse rule below); fix what
   they find (-> 5.0.2 if needed).
6. Final test-all-versions run, report, graphify refresh, memory, push.
"""
```

## Where things stand (end of 2026-09-25)

### Released
- **5.0.0 is on CurseForge** (project 1414718, slug `effortlessbuilding-sophisticated`): 48 jars, file ids
  8973074-8973121, uploaded with `scripts/curseforge-upload.ps1` (log: `local/curseforge-upload-log.json`), tag `v5.0.0`
  on main.
- Scope (user): every Minecraft version with ANY Sophisticated Backpacks (SB) file (release, beta, alpha), our mod on
  Fabric, NeoForge and Forge wherever the loader exists (also beta loaders), SB integration wherever SB exists.
  = 17 branches, MC 1.16.3-26.2 (incl. 1.19.4 via the SB Fabric beta, NeoForge 26.1/26.1.1 beta loaders).
- SB integration exists on 24 MC versions: Forge 1.16.3-1.20.1, NeoForge 1.20.1-26.2 (1.20.1 = the Forge jar),
  Fabric (unofficial port) 1.19.2, 1.19.4, 1.20.1, 1.20.4, 1.21.1. Every SB jar also tested standalone (no SB).

### Branches (HEAD at handoff)
| Branch | HEAD | State |
|---|---|---|
| mc/1.16.3 | 17c8f68 | 5.0.0 + round 3, pushed |
| mc/1.16.5 (+ forge-1.16.4/) | 4ff080c | 5.0.0 + round 3, pushed |
| mc/1.17.1 | aa8f11f | 5.0.0 + round 3, pushed |
| mc/1.18.1 (+ forge-1.18/) | 86b86de | 5.0.0 + round 3, pushed |
| mc/1.18.2 | d47f3c1 | 5.0.0 + round 3, pushed |
| mc/1.19.2 (+ forge-1.19/) | 01d5cdb | round 3 pushed; build + unit tests green, GameTests/smoke/bytecode check still to run |
| mc/1.19.4 | 3e81ba3 | 5.0.0 + round 3, pushed |
| mc/1.20.1 | d11f8fc | round 3 pushed; build + unit tests green, GameTests/smoke/bytecode check still to run |
| mc/1.20.4 | a53acb5 | round 3 pushed; build + unit tests green, GameTests/smoke/bytecode check still to run |
| mc/1.21.1 (+ forge-1.21/) | 48261e8 | reference; 5.0.0 + round 3, pushed |
| mc/1.21.4, 1.21.5, 1.21.8 | 790315e, 679ec60, 06d4d15 | 5.0.0 + round 3, pushed |
| mc/1.21.10, 1.21.11 | 2debd9b, c0e650c | 5.0.0 + round 3, pushed |
| mc/26.1.2 (+ neoforge-26.1/) | 88c2113 | 5.0.0 + round 3, pushed |
| mc/26.2 | eaefb29 | 5.0.0 + round 3, pushed |
Every branch: `mod_version` still 5.0.0, release jars in `<loader>/release/` are the uploaded 5.0.0 jars; round-3 code is
in the sources but NOT yet in release jars. Each branch has `changelog/PATCH_NOTES_5.0.0.md` and `PATCH_NOTES_5.0.1.md`.
Worktrees: `versions/<ver>` (git-ignored on main). Backups of the interrupted work: `local/handoff-2026-09-25b/`.

### What was done (summary; details in log.md, report.md, decisions.md)
- 4.3.0 multi-version port (16 branches, smoke harness with sb.* checks, CI, final run 196 pass / 0 fail).
- Round 2 (user: "finish PlayerSettingsGui, fix randomizer bag, fix all bugs"): PlayerSettingsGui = client-config editor
  (radial button + unbound key), bag titles fit/ellipsis, 6 dead widgets removed, Terrain Mound icons drawn + radial
  highlight + RadialButtonLayout, gameplay fixes (merge-undo refund, no charge for failed placements, full-count charge
  (dupe), stuck undo stack, Disable+Quick Replace preview), mini preview dead method removed, smoke clients never touch
  the OS cursor/focus. New: mc/1.19.4, neoforge-26.1/. Standalone (no SB) switch `-PsmokeNoSb=true` on every branch.
- Fixes found on the way: 1.21.8 GameTest hang (test helper), FG6/FG7 empty-jar build-cache bug, override folders vs
  configuration cache, 1.21.5 smoke world, Fabric IllegalAccessError without SB on 1.19.2/1.19.4 (Screen#font),
  Fabric 1.20.x compile without SB (access widener), test-all-versions (JDK per folder, SB detection, watchdog with
  thread dumps, -SmokeTasks runSmokeServerNoSb), CI smoke detection for override folders.
- 5.0.0: bump on all branches, 48 jars, CurseForge upload, full CHANGELOG.md, release/curseforge-description.md (16
  sections, ~44k chars, image URLs of the 10 uploaded screenshots; the USER pastes it into CurseForge himself),
  release/curseforge-images/.
- Round 3 (5.0.1 fixes, reference mc/1.21.1 d8ab383..48261e8): Fabric per-player data persisted, server validates
  reach/limits + CommonConfigSyncPacket, Fabric break events + optional Common Protection API (not on 1.16/1.17: Java 17
  needed), Array limit enforced, offhand bag filter, material cost of multi-item blocks, BuildModeHistory (previous
  build mode), ~55 translation keys + LangKeysTest, dead content removed, typos. Ported to all 16 other branches and pushed
  (1.19.2/1.20.1/1.20.4 only build + unit tests verified so far).

## Open work, in order
1. **mc/1.19.2, mc/1.20.1, mc/1.20.4 round 3**: 01d5cdb, d11f8fc, a53acb5, pushed at handoff. The porting agent was
   stopped while writing 1.19.2 docs. Lead verified at handoff: `gradlew build` green on all 8 loader folders, unit
   tests fabric 117 / forge, forge-1.19, neoforge 103, 0 failures. Still needed per branch: Fabric runGametest, runSmokeServer and
   `-PsmokeNoSb=true`, `pwsh scripts/check-fabric-no-sb-bytecode.ps1 -Mc <ver>` (0 differing classes), check
   PATCH_NOTES_5.0.1.md/TESTING.md, then push. Known pitfalls from the other ports: mixin json `compatibilityLevel` must
   be JAVA_17 (not JAVA_21); Forge fake players need the 5-arg `moveTo`; Forge template name `smoketest_empty`.
2. **Release material 5.0.1 (main)**, not done (agent stopped): `scripts/bump-version.ps1` must handle an existing
   PATCH_NOTES_5.0.1.md (keep 5.0.0 and 5.0.1 notes, only set mod_version/jar names/rebuild);
   `release/curseforge-changelog-5.0.1.md`; 5.0.1 entry in CHANGELOG.md; section 13 "Known issues" in
   release/curseforge-description.md (remove what 5.0.1 fixes). Uncommitted on main: an improvement of
   scripts/check-fabric-no-sb-bytecode.ps1 (keeps the Common Protection API on the classpath) - backup
   `local/handoff-2026-09-25b/main-uncommitted.patch`; review, test, commit or drop.
3. **Bump all 17 branches to 5.0.1**: `pwsh scripts/bump-version.ps1 -Version 5.0.1 -Mc <ver>` per branch, ~10 in
   parallel (64 GB RAM, 20 threads; 17 at once swaps). Check 48 jars (version 5.0.1 inside, >=326 classes, no
   smoketest/gametest classes), commit per branch, push.
4. **CurseForge 5.0.1**: `pwsh scripts/curseforge-upload.ps1 -ExpectVersion 5.0.1` (dry run, resolves ids with the
   token), show the user the list, then `-Upload -Plan local/curseforge-plan-5.0.1.json -Yes` ONLY after the user's OK.
   Tag v5.0.1.
5. **Client smoke runs (never run on the round-2/3 code except mc/1.21.1 fabric/forge/forge-1.21)**: need the user's
   explicit "Clients erlaubt". Then runSmokeClient per loader folder, one window at a time (window lock), with the
   cursor recorder (`scratchpad` proof-run pattern: abort if the clip rect changes or our window gets the foreground).
   First real test of: PlayerSettingsGui on old versions, bag titles, radial layout/icons, mini preview,
   Disable+Quick Replace preview, cursor/focus mixins per loader/version.
6. **Final**: test-all-versions (parallel recipe docs/TESTING.md, incl. `-SmokeTasks runSmokeServer,runSmokeServerNoSb`),
   report.md, graphify incremental refresh (never plain `graphify update .`), memory, push.

## Open decisions for the user (asked, not answered)
- Keep as is (earlier balance decisions) or change: Protect Tile Entities also protects during survival mass breaking;
  survival mass breaking needs an effective tool (no bare hands).
- Terrain Mound "alpha" label was removed from the description (code has no alpha marker) - re-add?
- Screenshots are from the 854x480 test window; nicer ones need client runs.

## Project rules (user, all sessions)
- Test that the mod WORKS in-game, always with Sophisticated Backpacks + Building Upgrade where SB exists; also
  standalone. Test fast; fix bugs when found.
- **Never touch the user's mouse or focus.** Game clients only after the user's explicit go; muted, second monitor, one
  at a time (`local/game-window.lock`), started unfocused (harness keepOffTheCursor + SmokeWindowMixin + early window
  off). Foreign Minecraft clients (other projects, e.g. UnifiedXaeroWorldmap) may hold the mouse - never touch them.
- Gradle: always `--no-daemon`, never `gradlew --stop`, kill only own PIDs, no synthetic CPU burners.
- CurseForge token: only in `local/curseforge-token.txt` (git-ignored), created by the user. Never type, print, copy or
  pass tokens yourself; the upload script reads the file. Publishing needs the user's explicit OK per release.
- No secrets or third-party jars in git; never force-push; commit trailer
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; never stage graphify-out/ in feature commits.
- Reply to the user in the language they write in (German or English).

## Key files
- Status board: `.knowledge/` (requirements, plan, decisions, log, report, port-brief, round2/, round3/ port notes).
- Scripts: `scripts/test-all-versions.ps1`, `bump-version.ps1`, `curseforge-upload.ps1`,
  `check-fabric-no-sb-bytecode.ps1`, `sync-branch-infra.ps1`, `setup-worktrees.ps1`.
- Release: `CHANGELOG.md`, `release/curseforge-changelog-5.0.0.md`, `release/curseforge-description.md`,
  `release/curseforge-images/`.
- Docs: `docs/TESTING.md`, `docs/RELEASING.md`, `docs/PORTING.md`, `docs/ARCHITECTURE.md`, `README.md` (support matrix).
