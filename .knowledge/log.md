# Log (multi-version session; earlier sessions: see git history)

- 2026-09-24 lead: CurseForge file lists read (SB official 1072 files, Fabric port 29); scope questions answered by user.
- 2026-09-24 R1 map -> Sonnet, R2 upstream download -> Sonnet, R3 toolchain research -> Opus: started.
- 2026-09-24 R1 review: accepted. ~159/269 files identical, rest loader-API adapters; drift: NeoForge-only Curios dedup, CompatHelper caching, 3 lang keys missing on Fabric, 4 recipe file names; Catnip only used in RenderHandler ghost draw, vanilla FallbackPreviewRenderer exists in the 1.21.11 draft; SB code isolated in ~10 files (excludable where SB is absent).
- 2026-09-24 R2 review: accepted without corrections (28 pairs, 50 jars verified, fetch script idempotent; old Forge 1.16-1.18.1 bundle Core; NeoForge 26.1/26.1.1 Core gap recorded). Follow-up for F6: manifest refresh script.
- 2026-09-24 F1 restructure mc/1.21.1 (worktree versions/1.21.1) -> Opus, started.
- 2026-09-24 R3 review: accepted. Proof builds for 10 toolchains; FG6 needs Gradle 8.4-8.8 while Loom 1.18 needs Gradle>=9.7/JDK25 -> decision: standalone Gradle build per loader folder (F1 redirected); merges per matrix; 26.2 drops MultiBufferSource/Tesselator.
- 2026-09-24 F1 review: accepted without corrections (verified: Fabric 77 tests, 17/17 GameTests, NeoForge 65 tests; count/persistent-data diffs checked). Catnip render/GUI classes vendored (MIT) instead of the fallback renderer to keep the look. Commit dac2817 pushed to mc/1.21.1.
- 2026-09-24 F2 Forge 1.21.1 -> Opus, F4 deps + 4.3.0 -> Sonnet: started (disjoint folders).
