# ANALYSIS_AND_INSTRUCTIONS

Single source of truth for everything learned about this repository and every
instruction handed to the implementing agent. Nothing in this folder is game
code. It exists so that a fresh session (human or AI) can pick up the work
without re-deriving the analysis.

## Roles

| Role | Who | Does |
|------|-----|------|
| Analyst / commander / reviewer | Claude Fable 5.1 (this session's orchestrator) | Reads the repo, decompiles upstream jars, writes the documents in this folder, dispatches exactly ONE implementation agent, reviews its diff, sends corrections. Never edits game code. |
| Implementer | ONE Claude Sonnet 5 subagent | Executes `05_IMPLEMENTATION_TASKS.md` task by task, builds, tests, commits and pushes. |

## Reading order

1. `01_REPO_OVERVIEW.md` – what the repository is, how it is laid out, how it builds and runs, gotchas (gitignore, dev instances, mappings).
2. `02_UPSTREAM_SOPHISTICATED_FABRIC_1.21.1.md` – exact upstream versions, CurseForge ids, hashes, and the API facts (from decompiled jars) that the fixes rely on.
3. `03_ROOT_CAUSE_BUILDING_UPGRADE.md` – why the Building Upgrade "has no effect when turned on" with the current Sophisticated ports. Evidence chain and root causes RC1–RC4.
4. `04_RADIAL_MENU_AND_UNFINISHED_FEATURES.md` – analysis of the Alt-key radial menu and the alpha/beta features that are not finished (line thickness, diagonal wall fill, player settings screen, terrain modes).
5. `05_IMPLEMENTATION_TASKS.md` – the ordered, testable task list for the implementer. This is the contract.
6. `06_REVIEW_CHECKLIST.md` – what the reviewer verifies after implementation.
7. `07_SESSION_LOG.md` – chronological log of what was done, decided, and left open.
8. `08_SURVIVAL_BREAKING_ANALYSIS.md` – how breaking works today, why survival is blocked, why
   Disable mode can drift, the verified Tool Swapper API, and the balance design (D1–D8).
9. `09_SURVIVAL_BREAKING_TASKS.md` – the ordered task contract for survival mass breaking
   (T-S1 … T-S9), still shipped as 4.1.0.
10. `10_UPSTREAM_API_BREAK_4.1.1.md` – root-cause record and task contract (T-U1 … T-U4) for the
    4.1.1 hotfix: SophisticatedBackpacks 3.26.0 changed `PlayerInventoryProvider.runOnBackpacks`'s
    return type from `void` to `boolean`, a binary-incompatible change that crashed the NeoForge
    server tick loop with `NoSuchMethodError`.

## Standing rules for this repository (apply to every session)

- Work on branch `main`, remote `origin` = `https://github.com/navrelis/EffortlessBuildingSophisticated.git`. Commit after every completed task group and push immediately (`git push origin main`).
- Commit trailer: `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`.
- After every major change, rebuild the knowledge graph: `graphify-out/` is produced by the graphify skill (`/graphify . --update` for incremental, or the full pipeline). Query it with `graphify query "<question>"` before grepping blindly.
- Two loader projects must keep building: `Fabric-0.18.6-1.21.1` and `Neoforge-21.1.217-1.21.1`. Fabric is the primary target of the current work.
- `.gitignore` ignores `**/*.md` globally. Markdown that must be tracked needs an explicit negation (this folder, patch notes, `graphify-out/GRAPH_REPORT.md`). See task T0 in `05_IMPLEMENTATION_TASKS.md`.
- In the Bash tool on this machine, `python` resolves to a Windows Store stub. Use the interpreter path stored in `graphify-out/.graphify_python` for any Python work. Python cannot open `/c/...` MSYS paths; pass `C:/...` paths.
