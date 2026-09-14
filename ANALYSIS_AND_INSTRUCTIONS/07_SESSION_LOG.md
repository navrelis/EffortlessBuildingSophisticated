# 07 – Session log

## 2026-09-14 – Analysis session (Fable 5.1, orchestrator)

Done
- Confirmed the repo layout, build config and runtime flow (see `01`).
- Verified via the CurseForge site API that the screenshot versions are the newest Fabric 1.21.1 files of all three ports; downloaded them (sha1 in `02`) into `Fabric-0.18.6-1.21.1/other_mods/` (production jars) and `DevInstance_Fabric/run/mods/`. The previous `other_mods` jars were Loom-remapped copies (manifest `Fabric-Mapping-Namespace: named`), moved to `other_mods/_named_dev_copies_old/`.
- Decompiled SC 1.2.9.21.168 and SB 3.23.4.3.106 (Vineflower 1.11.1, from the Loom-remapped jars in `.gradle/loom-cache/remapped_mods/.../unspecified/`) to trace the upgrade, wrapper, menu, storage and inventory-provider code paths (`02`).
- Read the user's real instance logs (`C:\Users\nikol\curseforge\minecraft\Instances\Nytheria*`): the exact jar set from the screenshot plus Trinkets/Accessories; integration registers without errors.
- Root causes RC1–RC4 written up in `03`; radial menu / unfinished features in `04`; task contract in `05`.
- Built the graphify knowledge graph over both loader projects (images/sounds excluded): 4,647 nodes, 13,322 edges, 165 communities; `graphify-out/graph.html`, `graph.json`, `GRAPH_REPORT.md`, `manifest.json`. Graph health: 2,006 dangling-endpoint edges (external Minecraft/loader symbols), 51 self-loops and ~1.3k parallel edges collapsed – expected for Java AST extraction, graph is usable. Semantic extraction covered the 11 text documents (licenses, patch notes, CI workflows) with one subagent.

Decisions
- Fabric is the primary target; NeoForge gets parity for RC1–RC3 only (T9).
- Fabric keeps the "clamp backpack contribution to the upgrade's max blocks" semantics (matches the item tooltip); NeoForge keeps its "tier gates access" semantics.
- `PlayerSettingsGui` stays untouched (deferred, needs product decision).
- Line thickness and diagonal-wall fill are completed because the UI/lang/icons already exist and the documentation promises them.
- The radial menu no longer cancels an in-progress build; switching the mode does.

Open / needs the user
- Whether to remove the second Enabled toggle in the Building Upgrade tab now that Core shows a per-slot switch (kept for now).
- Whether the NeoForge gameplay semantics should be unified with Fabric (not done).
- In-game confirmation on the Nytheria instance after the 4.1.0 jar is built (the analysis is code-based; no interactive game session was possible here).

How the graph was built (for re-runs)
```
PY=$(cat graphify-out/.graphify_python)
# detect → drop image/video categories → AST extract (sequential) → one doc-extraction subagent →
# merge → build/cluster → label → export html → manifest. Incremental: "$PY" -m graphify update
```

## Implementer notes (Sonnet 5)

_(to be filled by the implementing agent: per task – what changed, commands run, results, deviations)_
