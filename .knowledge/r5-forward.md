(Copy of local/toolchains-fwd/REPORT.md; sub-documents stay in local/toolchains-fwd/, git-ignored.)

# R5: Forward-port toolchain research, 1.21.10 -> 1.21.11 -> 26.1.2 -> 26.2

Research + proof builds only, no mod code touched. Everything lives under this folder
(`local/toolchains-fwd/`, git-ignored). Detailed evidence is in the linked sub-documents; this
file is the concise index plus the cross-cutting synthesis and risk ranking.

Work was split across parallel background agents and, after the session's 10-agent cap forced
four of them to stop early, finished directly and sequentially: SB/Core API survey, Forge MDK/jar
research, the NeoForge 26.1.2+26.2 hello-world builds, and the old-draft review were all completed
by the orchestrating session itself, reusing the stopped agents' partial work (downloaded MDK/jars,
scaffolded Gradle files) wherever it existed.

## Documents

| Doc | Covers |
|---|---|
| `notes/fabric-1.21.10-1.21.11.md` | Fabric hello-world builds + vanilla API diff, 1.21.8->1.21.10->1.21.11 |
| `notes/fabric-26.1.2-26.2.md` | Fabric hello-world builds (non-remap, unobfuscated) + vanilla API diff, 1.21.11->26.1.2->26.2 |
| `notes/neoforge-1.21.10-1.21.11.md` | NeoForge hello-world builds (+ SB/Core API call) + vanilla/NeoForge API diff, 1.21.8->1.21.10->1.21.11 |
| `notes/neoforge-26.1.2-26.2.md` | NeoForge hello-world builds (+ SB/Core API call) + NeoForge-specific API diff, 1.21.11->26.1.2->26.2 |
| `sb-core-survey/SURVEY.md` | Sophisticated Backpacks/Core NeoForge Java API, javap'd across all 8 versions 1.21.5->26.2 |
| `forge-research/REPORT.md` (+ `DOWNLOADS.md`) | Forge 60/61/64/65.x toolchain (MDK build.gradle diffs) + API changes, no FG7 build ever run |
| `old-draft-review/FINDINGS.md` | What's mineable from the abandoned `local/reference/Fabric-0.19.2-1.21.11` draft |

## 1. Toolchain versions (all verified live against real repositories, not assumed)

| MC | Fabric Loader / API | Fabric Loom | NeoForge | Parchment | Forge | Gradle (per loader) | Java |
|---|---|---|---|---|---|---|---|
| 1.21.10 | 0.19.5 / `0.138.4+1.21.10` | 1.17.21 (remap), Gradle 9.5.1 | 21.10.64 | 2025.10.12 | 60.1.15 | Fabric 9.5.1, NeoForge 9.2.1, Forge 9.3.1 | 21 |
| 1.21.11 | 0.19.5 / `0.141.6+1.21.11` | 1.17.21 (remap), Gradle 9.5.1 | 21.11.45 | 2025.12.20 | 61.2.1 | same, Forge 9.5.0 | 21 |
| 26.1.2 | 0.19.5 / `0.155.3+26.1.2` | 1.18.2 (non-remap), Gradle 9.8.0/JDK25 | 26.1.2.109 | none (unobfuscated) | 64.1.3 | Fabric 9.8.0, NeoForge 9.2.1, Forge 9.5.0 | 25 |
| 26.2 | 0.19.5 / `0.161.0+26.2` | 1.18.2 (non-remap), Gradle 9.8.0/JDK25 | 26.2.0.88 | none | 65.1.3 | same | 25 |

None of `docs/PORTING.md`'s existing version numbers needed correction -- every one was confirmed
as still the latest stable release on its real Maven/distribution at time of writing (2026-09-24).
One plugin-id detail was clarified rather than corrected: Fabric's `-remap` vs non-`-remap` Loom
plugin split is real and version-gated as the doc describes, and the 26.x "no mappings line, plain
dependencies, `jar` not `remapJar`" description was independently confirmed live against the
official `FabricMC/fabric-example-mod` 26.1.2/26.2 branches.

All eight hello-world Gradle projects (`{1.21.10,1.21.11,26.1.2,26.2}/{fabric,neoforge}`) built
green with `gradlew build --no-daemon`; the two NeoForge hello-worlds at each step additionally
compile a class calling a real Sophisticated Core/Backpacks method
(`BackpackWrapper.fromStack(ItemStack)` -> `IStorageWrapper.getContentsUuid()`/`.getUpgradeHandler()`)
resolved live from CurseMaven using the exact coordinates in `upstream/manifest.json`, with no
local-jar fallback needed anywhere. Forge was **never built** (ForgeGradle explicitly off-limits
while another agent ran a cold FG7 build elsewhere); Forge evidence is MDK `build.gradle` diffs
plus `javap` on the real `-universal.jar`/`-userdev.jar` downloaded directly from
`maven.minecraftforge.net`.

## 2. Corrections and clarifications to `docs/PORTING.md`

These are the findings worth updating the doc with -- each backed by direct decompiled-source or
`javap` evidence, not inference:

1. **"`RenderLevelStageEvent` replaced by `AddFramePassEvent`" is Forge-only, and happens earlier
   than 26.1.** Confirmed: NeoForge keeps `RenderLevelStageEvent` completely unchanged all the way
   through 26.2 (`AddFramePassEvent` does not exist in the NeoForge 26.1.2 or 26.2 universal jar at
   all -- `notes/neoforge-26.1.2-26.2.md`). Forge, however, already replaces it with
   `AddFramePassEvent` starting at **Forge 60.x / MC 1.21.10** -- three release-steps earlier than
   the doc implies, and this mod's Forge branch already has a `LevelRendererMixin` workaround for
   the *lack* of a working Forge render hook on 1.21.5, which can likely be **retired** once ported
   to 1.21.10+ (`forge-research/REPORT.md`).
2. **Forge's event bus 7 rewrite lands at 1.21.10, and it's bigger than a mappings detail.** The
   doc's FG7 gotchas mention `eventbus-validator` as a "26.x MDK" thing; in fact it's needed from
   1.21.10 on, and more importantly the entire `@SubscribeEvent`/`IEventBus.register(this)`
   registration model this mod's Forge loader folder currently uses is compiled against a class
   (`net.minecraftforge.eventbus.api.SubscribeEvent`) that's absent from the 1.21.10+ universal jar
   -- every Forge event subscriber needs rewriting at the very first forward step, independent of
   any individual event's own shape (`forge-research/REPORT.md`).
3. **`GuiGraphics.renderOutline` is renamed to `submitOutline` at 1.21.10 only, then reverts back
   to `renderOutline` (original immediate-draw behavior) at 1.21.11.** Not documented anywhere;
   found via real decompiled NeoFormRuntime source diffing (`notes/neoforge-1.21.10-1.21.11.md`).
   `common/src/main/java/.../gui/elements/LabeledScrollInput.java` calls this method directly today
   and needs a version-specific shim/branch, not a single rename, if 1.21.10 and 1.21.11 are ever
   handled by shared code.
4. **`GuiGraphics` -> `GuiGraphicsExtractor` is a real, if partial, transition that starts
   *before* 26.1** on Fabric specifically: the abandoned old 1.21.11 draft's real compile log shows
   `graphics.pose()` already returning `Matrix3x2fStack` (not `PoseStack`) at 1.21.11 in Create-
   vendored code (`old-draft-review/FINDINGS.md`) -- lower-confidence (older yarn mapping build,
   single source, contradicts this session's own decompiled-source finding that the full
   `GuiGraphicsExtractor` rewrite is a 26.1 change), but worth a direct, explicit re-check before
   relying on "`GuiGraphics` is untouched at 1.21.11" for the real port.
5. **`BreakBlockEvent`'s full shape**: confirmed real (NeoForge-only, matches the doc), but two
   details the doc omits: it moves to a **new package**
   (`net.neoforged.neoforge.event.level.block`, not just a rename within `event.level`), and it
   **drops** the old `getExpToDrop()`/`setExpToDrop(int)`/`getResult()`/`setResult(Result)`
   accessors entirely -- there's no direct equivalent if a future feature needs to adjust XP drop
   or result on a NeoForge block-break hook (`notes/neoforge-26.1.2-26.2.md`).
6. **A previously-uncaptured, mod-relevant NeoForge-wide rewrite**: NeoForge replaces its classic
   capability system (`IItemHandler`/`ItemStackHandler`, `IEnergyStorage`, `IFluidHandlerItem`)
   with a new `net.neoforged.neoforge.transfer` Resource/ResourceHandler API, **exactly at the
   1.21.8 -> 1.21.10 step**, and Sophisticated Core/Backpacks' NeoForge builds adopt it
   simultaneously across `IStorageWrapper`, `BackpackWrapper`, `UpgradeHandler`, and
   `ToolSwapperUpgradeWrapper` (`sb-core-survey/SURVEY.md`). Not in `docs/PORTING.md` at all today.
   Practical impact on this mod is limited (most renames change the method name too, so they fail
   loudly at compile time) except `IStorageWrapper.getInventoryForUpgradeProcessing()`/
   `.getInventoryForInputOutput()`, which keep their name but silently change return type --
   currently unused by this mod's own call sites, but worth a grep re-check at port time.
7. **A repeat of the exact 4.1.1-incident shape**: `PlayerInventoryProvider.runOnBackpacks`
   flip-flops `boolean` (1.21.5-1.21.11) -> `void` (26.1/26.1.1, regression) -> `boolean` again
   (26.1.2/26.2). Already fully covered by this mod's existing `BackpackScanCompat`
   reflection/`MethodHandle` guard -- **no new code needed**, but strong independent confirmation
   that NeoForge SB support should start at 26.1.2, exactly as the branch matrix already says
   (`sb-core-survey/SURVEY.md`, section 5's full verdict).

## 3. Key risks per version step (ranked)

### 1.21.10 (first forward step from 1.21.8)
1. **Forge event bus 7** forces a full rewrite of every `@SubscribeEvent` method in the Forge
   loader folder -- the single biggest item in this whole research pass, and it's a prerequisite
   before anything else on Forge can be fixed.
2. **1.21.9's input/GUI refactor is already fully baked in**: `KeyMapping.Category`,
   `MouseButtonEvent`, `SubmitNodeCollector`, `BlockEntityRenderState` all exist by 1.21.10, and
   `RenderType`'s static-factory API (`solid()`/`cutout()`/`translucent()`, extending
   `RenderStateShard`) is already gutted -- contradicts "1.21.10: hotfix, no break of note" in the
   current doc; this mod's large GUI/rendering layer needs real work at this very first step, not
   a coast.
3. **NeoForge's capability/transfer-API rewrite** (finding #6 above) ripples through the
   Sophisticated Core/Backpacks NeoForge API surface at exactly this step; low direct impact on
   this mod's own call sites today, but worth re-grepping once the real port starts.
4. Good news: `AddFramePassEvent` already existing on Forge at this step means the
   `LevelRendererMixin` workaround can likely be dropped, a net simplification.

### 1.21.11
1. **`ResourceLocation` -> `Identifier`, confirmed real and pervasive** across vanilla, Fabric,
   NeoForge, and Forge alike (every loader's `javap`/decompiled-source evidence agrees) --
   networking (`CustomPacketPayload.Type`, `ChannelBuilder.named`), key mappings
   (`KeyMapping.Category`), data components (`DataComponents.ITEM_MODEL`), and SB/Core's own
   `UpgradeContainerRegistry`. Do this as one cross-cutting pass per branch, not loader-by-loader
   or class-by-class.
2. **`GuiGraphics.renderOutline`/`submitOutline` flip-flop** (finding #3) -- a real landmine for
   any attempt to merge 1.21.10 and 1.21.11 into one branch; `docs/PORTING.md`'s merge-verdicts
   process should treat this pair as needing a runtime test, not just a compile check.
3. `BakedQuad` becomes a `Record` with JOML `Vector3fc` positions and packed-`long` UVs (was
   raw arrays) -- directly hits this mod's ghost-block-preview quad-reading code.

### 26.1.2 (first NeoForge/26.x step; Fabric/Forge already cover 26.1/26.1.1)
1. **`MultiBufferSource`/`Tesselator` still exist, unchanged, at 26.1.2** -- the big rendering
   rewrite does not land until 26.2. `GuiGraphics` is fully replaced by `GuiGraphicsExtractor` +
   `Screen#extractRenderState` starting here, though (mechanical but touches ~40 files).
2. **NeoForge SB integration must start here, not at 26.1/26.1.1** -- confirmed independently by
   both the `runOnBackpacks` void-regression and `BackpackWrapper`'s missing methods/constructors
   at 26.1/26.1.1 (evidence of a stale beta-era Core build, not a real feature regression) --
   `sb-core-survey/SURVEY.md` section 5.
3. Forge's FG plugin range bump (`[7.0.17,8)`), Java 21->25, and the mappings block disappearing
   entirely are all confirmed exactly as `docs/PORTING.md` already states -- no surprises there.

### 26.2 (final forward step)
1. **`MultiBufferSource` and `Tesselator` removed outright** -- the single largest rendering rewrite
   in this entire research pass, loader-neutral (hits Fabric, NeoForge, and presumably Forge's own
   vertex-submission path identically since it's a vanilla/Vulkan-backend change, not a loader
   API). This mod's vendored Catnip outliner and `GhostBlockRenderer` are built directly on
   `MultiBufferSource`/immediate-mode `VertexConsumer` acquisition and need a genuine
   architecture change to a deferred `SubmitNodeCollector`/`LevelRenderEvents.COLLECT_SUBMITS`
   submission model -- not a rename, a redesign of how this mod draws.
2. `RenderType` survives but loses its imperative `draw(MeshData)`/`mode()`/`bufferSize()` surface
   in favor of a `prepare()`/`PreparedRenderType` GPU-buffer split -- this mod's own custom
   `RenderType` definitions (`OutlineRenderTypes`/`BuildRenderTypes`/`GuiRenderTypes`) need
   updating even though `RenderType` itself isn't gone.
3. `BlockStateModel`/`BlockModelPart` (renamed `BlockStateModelPart`) both move packages
   (`renderer.block.model` -> `renderer.block.dispatch`), compounding risk #1 for the ghost-block
   renderer specifically.

## 4. What's still open / not covered by this research pass

- No actual Forge build was run anywhere in this range (by design) -- the exact shape of a real
  Forge-60.x event subscriber (what replaces `@SubscribeEvent`) needs a real MDK example-mod
  reference once the port starts; the minimal proof MDKs here don't demonstrate one.
- `Screen.hasControlDown()`'s fate at 1.21.10/1.21.11 is flagged as open (medium confidence, single
  older-mapping source) -- worth an explicit, direct check against this session's own confirmed
  decompiled sources before the real port relies on it still existing.
- `LayeredDraw`'s fate at 1.21.11 is flagged as open (lower confidence) for the same reason.
