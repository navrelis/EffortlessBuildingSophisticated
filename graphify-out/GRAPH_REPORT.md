# Graph Report - EffortlessBuildingSophisticated  (2026-09-14)

## Corpus Check
- Large corpus: 564 files · ~166,490 words. Semantic extraction will be expensive (many Claude tokens). Consider running on a subfolder.

## Summary
- 4647 nodes · 13322 edges · 165 communities (128 shown, 29 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 173 edges (avg confidence: 0.83)
- Token cost: 73,523 input · 0 output

## Community Hubs (Navigation)
- Build Mode Packets & Client Dispatch
- Randomizer Bag Menus (MenuBase)
- Compat Helper & Item Utilities
- Client Screens & GUI Textures
- Power Level & Client Backpack Cache
- Create GUI Widgets (Indicator, Tooltip)
- Attachment Handler (Power Level API)
- GUI Textures, Keys & Vertex Helpers
- Build Mode Enum & Base Mode
- Two/Three-Click Modes & Array Modifier
- Cube Build Mode
- Client Events & Client Config
- Create Theme System
- Disabled & Single Modes
- Slot GUI Scroll List
- Player Settings GUI (unfinished)
- Fabric Common Events & Commands
- Ghost Block Rendering (CreateClient)
- Virtual Render World (Fabric)
- Virtual Render World (NeoForge)
- Virtual Chunk Structure Data
- Icons & Partial Item Rendering
- Fabric Bootstrap & Randomizer Container
- Mode Option Actions (ActionEnum)
- VoxelShaper Utility
- Icon Buttons & Modifier Entries
- Cylinder & Diagonal Line Modes
- Create Simi Screens & Widgets
- Label & ScrollInput Widgets
- BlockHelper Utility
- Virtual Chunk Sections
- ScrollInput Widget
- VirtualRenderWorld Level Overrides
- Radial Mirror Modifier
- Connected Texture Behaviour
- Server Block Placer
- Build Modifiers Registry
- Camera Angle Animation Service
- Virtual Chunk Source
- Floor Mode & Build Mode Keys
- Misc: MenuBase
- Misc: ThreeClicksBuildMode
- Misc: ActionEnum
- Misc: Color
- Misc: org.joml.Matrix4f
- Misc: CTType
- Misc: net.minecraft.client.renderer.RenderType
- Misc: CustomLightingSettings
- Misc: BuilderChain
- Misc: BuilderChain
- Misc: BuildingUpgradeContainer
- Misc: net.neoforged.bus.api.SubscribeEvent
- Misc: Builder
- Misc: RadialMenu
- Misc: BuilderFilter
- Misc: GuiScrollPane
- Misc: ModifiersScreenList
- Misc: ModifiersScreen
- Misc: Entry
- Misc: Sophisticated Building Update 4.0.0
- Misc: net.minecraft.core.Direction
- Misc: net.neoforged.neoforge.common.ModConfigSpec.Builder
- Misc: BuildingUpgradeSettingsTab
- Misc: AllGuiTextures
- Misc: net.minecraft.world.phys.BlockHitResult
- Misc: AllGuiTextures
- Misc: GuiScrollPane
- Misc: AllSpecialTextures
- Misc: AbstractSimiScreen
- Misc: BuildingUpgradeItem
- Misc: AbstractSimiScreen
- Misc: AbstractSimiContainerScreen
- Misc: BuildingUpgradeWrapper
- Misc: CatnipRenderHelper
- Misc: AbstractSimiContainerScreen
- Misc: BuildingUpgradeItem
- Misc: net.minecraft.nbt.CompoundTag
- Misc: BuildSettings
- Misc: InventoryHelper
- Misc: PowerLevel
- Misc: BuildSettings
- Misc: RenderHandler
- Misc: RadialMenu
- Misc: Builder
- Misc: ModifiersScreen
- Misc: BlockPreviews
- Misc: Color
- Misc: AllIcons
- Misc: CommonConfig
- Misc: Mirror
- Misc: FabricClientEvents
- Misc: BaseModifierEntry
- Misc: BuildingUpgradeHelper
- Misc: ServerConfig
- Misc: Mirror
- Misc: RenderHandler
- Misc: TerrainMound
- Misc: net.minecraft.world.level.block.Block
- Misc: net.minecraft.world.level.material.FluidState
- Misc: TerrainMound
- Misc: RadialMirror
- Misc: BuildingUpgradeHelper
- Misc: Line
- Misc: Wall
- Misc: GuiCollapsibleScrollEntry
- Misc: Line
- Misc: Wall
- Misc: GuiCollapsibleScrollEntry
- Misc: CompoundTag
- Misc: net.minecraft.world.level.entity.LevelEntityGetter
- Misc: Sphere
- Misc: OptionEnum
- Misc: BuildingUpgradeWrapper
- Misc: Sphere
- Misc: OmegaRandomizerBagItem
- Misc: SingleItemLootModifier.java
- Misc: ClientEvents
- Misc: IScrollEntry
- Misc: OmegaRandomizerBagScreen
- Misc: ModeOptions
- Misc: ClientEvents
- Misc: ItemHandlerWrapper
- Misc: Cone
- Misc: AllIcons
- Misc: GNU Lesser General Public License v3
- Misc: Cone
- Misc: MenuBase
- Misc: Circle
- Misc: ClientEvents
- Misc: net.minecraft.server.packs.resources.ResourceManager
- Misc: ModifierSettingsPacket
- Misc: Circle
- Misc: AllIcons
- Misc: GuiNumberField
- Misc: IScrollEntry
- Misc: Array
- Misc: net.minecraft.tags.TagKey
- Misc: Debug
- Misc: Array
- Misc: CuriosCompatHelper
- Misc: Debug
- Misc: FixedStack
- Misc: net.minecraft.core.RegistryAccess
- Misc: ResetableLazy
- Misc: ArrayEntry
- Misc: GuiCheckBoxFixed
- Misc: net.neoforged.fml.common.EventBusSubscriber
- Misc: ResetableLazy
- Misc: ArrayEntry
- Misc: GuiCheckBoxFixed
- Misc: RemovedGuiUtils.java
- Misc: State
- Misc: TooltipArea
- Misc: SophisticatedBuildingFabric
- Misc: gradlew script
- Misc: gradlew script
- Misc: IClearableMenu

## God Nodes (most connected - your core abstractions)
1. `BlockSet` - 142 edges
2. `SophisticatedBuilding` - 123 edges
3. `SophisticatedBuilding` - 116 edges
4. `BlockEntry` - 92 edges
5. `VirtualRenderWorld` - 86 edges
6. `VirtualRenderWorld` - 75 edges
7. `AttachmentHandler` - 69 edges
8. `ActionEnum` - 65 edges
9. `AttachmentHandler` - 64 edges
10. `BlockSet` - 56 edges

## Surprising Connections (you probably didn't know these)
- `Fabric Build GitHub Actions Workflow` --semantically_similar_to--> `NeoForge Build GitHub Actions Workflow`  [INFERRED] [semantically similar]
  Fabric-0.18.6-1.21.1/.github/workflows/build.yml → Neoforge-21.1.217-1.21.1/.github/workflows/build.yml
- `Sophisticated Building Update 4.0.0` --references--> `Sophisticated Building Update 4.0.0`  [AMBIGUOUS]
  PATCH_NOTES_4.0.0.md → ExportedJars/PATCH_NOTES_4.0.0.md
- `NeoForge Loader` --conceptually_related_to--> `NeoForge Build GitHub Actions Workflow`  [INFERRED]
  PATCH_NOTES_4.0.0.md → Neoforge-21.1.217-1.21.1/.github/workflows/build.yml
- `Fabric Loader + Fabric API` --conceptually_related_to--> `Fabric Build GitHub Actions Workflow`  [INFERRED]
  PATCH_NOTES_4.0.0.md → Fabric-0.18.6-1.21.1/.github/workflows/build.yml
- `sophisticatedbuilding-neoforge-<version>.jar` --shares_data_with--> `NeoForge Build GitHub Actions Workflow`  [INFERRED]
  ExportedJars/PATCH_NOTES_4.0.0.md → Neoforge-21.1.217-1.21.1/.github/workflows/build.yml

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Dual-loader (Fabric/NeoForge) LGPL license and hardfork provenance chain** — license_gnu_lgpl_v3, fabric_0_18_6_1_21_1_template_license_lgpl, neoforge_21_1_217_1_21_1_template_license_lgpl, license_effortless_building_original_mod, license_effortlessbuildingsophisticated_hardfork [INFERRED 0.85]
- **Placement preview crash fix: MyPlaceContext, BlockEntry, BuilderChain resolving Player through getStateForPlacement** — patch_notes_4_0_0_myplacecontext, patch_notes_4_0_0_blockentry, patch_notes_4_0_0_builderchain, patch_notes_4_0_0_getstateforplacement, patch_notes_4_0_0_blockplacecontext_getplayer [EXTRACTED 1.00]
- **Parallel Fabric/NeoForge CI build pipelines producing versioned jars** — fabric_0_18_6_1_21_1_github_workflows_build_build_workflow, neoforge_21_1_217_1_21_1_github_workflows_build_build_workflow, exportedjars_patch_notes_4_0_0_sophisticatedbuilding_fabric_jar, exportedjars_patch_notes_4_0_0_sophisticatedbuilding_neoforge_jar [INFERRED 0.85]

## Communities (165 total, 29 thin omitted)

### Community 0 - "Build Mode Packets & Client Dispatch"
Cohesion: 0.02
Nodes (87): Handler, IsQuickReplacingPacket, Context, Override, Type, Handler, IsUsingBuildModePacket, Context (+79 more)

### Community 1 - "Randomizer Bag Menus (MenuBase)"
Cohesion: 0.03
Nodes (55): DiamondRandomizerBagContainer, Override, Slot, GoldenRandomizerBagContainer, Override, Slot, Override, Slot (+47 more)

### Community 2 - "Compat Helper & Item Utilities"
Cohesion: 0.04
Nodes (42): ExtractionCountMode, EXACTLY, UPTO, ItemHelper, MutableInt, Override, TemplateSlot, Override (+34 more)

### Community 3 - "Client Screens & GUI Textures"
Cohesion: 0.04
Nodes (44): bind(), render(), DiamondRandomizerBagScreen, Override, GuiIconButton, OnPress, Override, GuiNumberField (+36 more)

### Community 4 - "Power Level & Client Backpack Cache"
Cohesion: 0.04
Nodes (49): PowerLevel, ClientBackpackItemCache, AbstractBlockBreakQueue, InteractionResultHolder, Override, TooltipContext, UseOnContext, CompressedBlockItem (+41 more)

### Community 5 - "Create GUI Widgets (Indicator, Tooltip)"
Cohesion: 0.03
Nodes (37): Indicator, Override, State, GREEN, OFF, ON, RED, YELLOW (+29 more)

### Community 6 - "Attachment Handler (Power Level API)"
Cohesion: 0.04
Nodes (14): AttachmentHandler, PowerLevel, CuriosCompatHelper, IInteractionChecker, CompoundTag, FixedStack, UndoRedo, AttachmentHandler (+6 more)

### Community 7 - "GUI Textures, Keys & Vertex Helpers"
Cohesion: 0.05
Nodes (37): com.mojang.blaze3d.vertex.BufferBuilder, AllGuiTextures, ARRAY_ENTRY, ARROW_DOWN, ARROW_UP, CHECKMARK, ENABLE_BUTTON_BACKGROUND, MIRROR_ENTRY (+29 more)

### Community 8 - "Build Mode Enum & Base Mode"
Cohesion: 0.04
Nodes (35): BaseBuildMode, Override, BuildModeEnum, CIRCLE, CONE, CUBE, CYLINDER, DIAGONAL_LINE (+27 more)

### Community 9 - "Two/Three-Click Modes & Array Modifier"
Cohesion: 0.05
Nodes (15): SophisticatedBuildingClient, BlockEntry, BlockPlacerHelper, PlaceChecker, SophisticatedBuildingClient, BlockEntry, BlockPlacerHelper, BlockSet (+7 more)

### Community 10 - "Cube Build Mode"
Cohesion: 0.05
Nodes (22): Cube, BlockPos, Override, DiagonalWall, BlockPos, Override, BlockPos, Override (+14 more)

### Community 11 - "Client Events & Client Config"
Cohesion: 0.04
Nodes (28): ClientConfig, Performance, Visuals, ClientEvents, CompatHelper, ClientBlockUtilities, SurvivalHelper, BuildModeEnum (+20 more)

### Community 12 - "Create Theme System"
Cohesion: 0.06
Nodes (13): ColorHolder, Key, Theme, FluidFormatter, javax.annotation.Nonnull, ColorHolder, Key, Theme (+5 more)

### Community 13 - "Disabled & Single Modes"
Cohesion: 0.05
Nodes (20): Disabled, Override, Override, Single, IBuildMode, BlockPreviews, BlockPos, BlockSet (+12 more)

### Community 14 - "Slot GUI Scroll List"
Cohesion: 0.06
Nodes (5): com.mojang.blaze3d.vertex.Tesselator, Override, SlotGui, Override, SlotGui

### Community 15 - "Player Settings GUI (unfinished)"
Cohesion: 0.06
Nodes (17): Entry, Override, PlayerSettingsGui, ShaderType, DISSOLVE_BLUE, DISSOLVE_ORANGE, ShaderTypeEntry, ShaderTypeList (+9 more)

### Community 16 - "Fabric Common Events & Commands"
Cohesion: 0.06
Nodes (27): com.mojang.brigadier.CommandDispatcher, FabricCommonEvents, ItemStack, Player, BackpackItemCountPacket, Handler, Context, Override (+19 more)

### Community 17 - "Ghost Block Rendering (CreateClient)"
Cohesion: 0.07
Nodes (15): CreateClient, GhostBlockParams, BlockPos, DefaultGhostBlockRenderer, GhostBlockRenderer, Override, Pose, TransparentGhostBlockRenderer (+7 more)

### Community 18 - "Virtual Render World (Fabric)"
Cohesion: 0.07
Nodes (8): LevelLightEngine, MapId, MutableBlockPos, Override, VirtualChunkSource, VirtualRenderWorld, VirtualChunkSection, net.minecraft.world.level.block.entity.BlockEntity

### Community 19 - "Virtual Render World (NeoForge)"
Cohesion: 0.08
Nodes (6): LevelLightEngine, MapId, MutableBlockPos, Override, VirtualChunkSource, VirtualRenderWorld

### Community 20 - "Virtual Chunk Structure Data"
Cohesion: 0.09
Nodes (12): it.unimi.dsi.fastutil.longs.LongSet, it.unimi.dsi.fastutil.shorts.ShortList, Entry, Override, Provider, TicksToSave, Types, VirtualChunk (+4 more)

### Community 21 - "Icons & Partial Item Rendering"
Cohesion: 0.10
Nodes (19): com.mojang.blaze3d.vertex.PoseStack, com.mojang.blaze3d.vertex.VertexConsumer, Vec3, PartialItemModelRenderer, Vec3, CustomRenderedItemModel, Override, PartialItemModelRenderer (+11 more)

### Community 22 - "Fabric Bootstrap & Randomizer Container"
Cohesion: 0.06
Nodes (27): FabricBootstrap, Override, RandomizerBagContainer, PacketHandlerClient, ClientProxy, IContainerFactory, Items, CompressedBlockItem (+19 more)

### Community 23 - "Mode Option Actions (ActionEnum)"
Cohesion: 0.07
Nodes (35): ActionEnum, CIRCLE_START_CENTER, CIRCLE_START_CORNER, CUBE_FULL, CUBE_HOLLOW, CUBE_SKELETON, DISABLE_BUILD_MODE_TOGGLE, FAST_SPEED (+27 more)

### Community 24 - "VoxelShaper Utility"
Cohesion: 0.12
Nodes (12): DefaultRotationValues, HorizontalRotationValues, Override, Vec3, VoxelShaper, DefaultRotationValues, HorizontalRotationValues, Override (+4 more)

### Community 25 - "Icon Buttons & Modifier Entries"
Cohesion: 0.06
Nodes (17): IconButton, Override, IconButton, Override, MirrorEntry, IconButton, Override, RadialMirrorEntry (+9 more)

### Community 26 - "Cylinder & Diagonal Line Modes"
Cohesion: 0.08
Nodes (13): Cylinder, BlockPos, Override, DiagonalLine, Override, BlockPos, Override, Pyramid (+5 more)

### Community 27 - "Create Simi Screens & Widgets"
Cohesion: 0.10
Nodes (10): AbstractSimiWidget, Override, AbstractSimiWidget, Override, net.createmod.catnip.gui.TickableGuiEventListener, net.minecraft.client.gui.components.AbstractWidget, net.minecraft.client.gui.components.events.GuiEventListener, net.minecraft.client.gui.components.ObjectSelectionList (+2 more)

### Community 28 - "Label & ScrollInput Widgets"
Cohesion: 0.08
Nodes (8): Override, Label, Override, ScrollInput, StepContext, Label, Override, LabeledScrollInput

### Community 29 - "BlockHelper Utility"
Cohesion: 0.09
Nodes (12): BlockHelper, CompoundTag, ItemStack, ItemHandlerHelper, BlockHelper, CompoundTag, ItemStack, net.minecraft.server.level.ServerLevel (+4 more)

### Community 30 - "Virtual Chunk Sections"
Cohesion: 0.09
Nodes (7): Entry, Override, Provider, TicksToSave, Types, VirtualChunkSection, VirtualChunk

### Community 31 - "ScrollInput Widget"
Cohesion: 0.09
Nodes (7): Override, ScrollInput, StepContext, Override, Label, Override, LabeledScrollInput

### Community 32 - "VirtualRenderWorld Level Overrides"
Cohesion: 0.11
Nodes (21): dev.engine_room.flywheel.api.visualization.VisualizationLevel, it.unimi.dsi.fastutil.objects.Object2ShortMap, net.minecraft.core.Holder, net.minecraft.core.SectionPos, net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundSource, net.minecraft.world.entity.Entity, net.minecraft.world.flag.FeatureFlagSet (+13 more)

### Community 33 - "Radial Mirror Modifier"
Cohesion: 0.11
Nodes (12): BlockEntry, BlockSet, Override, Vec3, RadialMirror, Color, Vec3, ModifierRenderer (+4 more)

### Community 34 - "Connected Texture Behaviour"
Cohesion: 0.15
Nodes (7): ConnectedTextureBehaviour, BlockUtilities, ConnectedTextureBehaviour, CTContext, BlockUtilities, net.minecraft.world.level.block.state.BlockState, net.minecraft.world.level.BlockAndTintGetter

### Community 35 - "Server Block Placer"
Cohesion: 0.10
Nodes (8): DelayedEntry, BlockEntry, BlockSet, ServerBlockPlacer, DelayedEntry, BlockEntry, BlockSet, ServerBlockPlacer

### Community 36 - "Build Modifiers Registry"
Cohesion: 0.09
Nodes (5): BaseModifier, BuildModifiers, CompoundTag, BuildModifiers, CompoundTag

### Community 37 - "Camera Angle Animation Service"
Cohesion: 0.08
Nodes (9): CameraAngleAnimationService, Mode, EXPONENTIAL, LINEAR, CameraAngleAnimationService, Mode, EXPONENTIAL, LINEAR (+1 more)

### Community 38 - "Virtual Chunk Source"
Cohesion: 0.11
Nodes (13): Override, VirtualChunk, VirtualChunkSource, it.unimi.dsi.fastutil.longs.Long2ObjectMap, java.util.function.BooleanSupplier, Override, VirtualChunk, VirtualChunkSource (+5 more)

### Community 39 - "Floor Mode & Build Mode Keys"
Cohesion: 0.09
Nodes (11): Floor, BlockPos, Override, Vec3, Floor, BlockPos, Override, Vec3 (+3 more)

### Community 40 - "Misc: MenuBase"
Cohesion: 0.11
Nodes (6): IClearableMenu, Override, MenuBase, Override, GhostItemMenu, Override

### Community 41 - "Misc: ThreeClicksBuildMode"
Cohesion: 0.10
Nodes (11): Cylinder, BlockPos, Override, DiagonalWall, BlockPos, Override, HeightCriteria, BlockEntry (+3 more)

### Community 42 - "Misc: ActionEnum"
Cohesion: 0.06
Nodes (34): ActionEnum, CIRCLE_START_CENTER, CIRCLE_START_CORNER, CUBE_FULL, CUBE_HOLLOW, CUBE_SKELETON, DISABLE_BUILD_MODE_TOGGLE, FAST_SPEED (+26 more)

### Community 43 - "Misc: Color"
Cohesion: 0.08
Nodes (14): BuildModeCategoryEnum, BASIC, CIRCULAR, DIAGONAL, ROOF, TERRAIN, Color, Override (+6 more)

### Community 44 - "Misc: org.joml.Matrix4f"
Cohesion: 0.12
Nodes (10): Create, bind(), render(), BlockEntityRenderHelper, Create, BlockEntityRenderHelper, ScreenElement, net.minecraft.client.multiplayer.ClientLevel (+2 more)

### Community 45 - "Misc: CTType"
Cohesion: 0.12
Nodes (11): Base, Override, CTSpriteShiftEntry, CTType, Base, Override, CTSpriteShiftEntry, net.createmod.catnip.render.SpriteShiftEntry (+3 more)

### Community 46 - "Misc: net.minecraft.client.renderer.RenderType"
Cohesion: 0.12
Nodes (9): com.mojang.blaze3d.vertex.VertexFormat, RenderTypes, BuildRenderTypes, Mode, RenderTypes, BuildRenderTypes, Mode, net.minecraft.client.renderer.RenderStateShard (+1 more)

### Community 47 - "Misc: CustomLightingSettings"
Cohesion: 0.10
Nodes (12): Builder, CustomLightingSettings, Matrix4f, Override, Vector3f, Builder, CustomLightingSettings, Matrix4f (+4 more)

### Community 48 - "Misc: BuilderChain"
Cohesion: 0.11
Nodes (11): AbilitiesState, CAN_BREAK, CAN_PLACE_AND_BREAK, NONE, BuilderChain, BuildingState, BREAKING, IDLE (+3 more)

### Community 49 - "Misc: BuilderChain"
Cohesion: 0.11
Nodes (11): AbilitiesState, CAN_BREAK, CAN_PLACE_AND_BREAK, NONE, BuilderChain, BuildingState, BREAKING, IDLE (+3 more)

### Community 50 - "Misc: BuildingUpgradeContainer"
Cohesion: 0.12
Nodes (10): BuildingUpgradeContainer, Override, UpgradeContainerType, SophisticatedBackpacksIntegration, BuildingUpgradeContainer, Override, UpgradeContainerType, SophisticatedBackpacksIntegration (+2 more)

### Community 51 - "Misc: net.neoforged.bus.api.SubscribeEvent"
Cohesion: 0.10
Nodes (15): BreakEvent, Clone, EntityPlaceEvent, CommonEvents, Post, Pre, ModBusEvents, net.neoforged.bus.api.SubscribeEvent (+7 more)

### Community 52 - "Misc: Builder"
Cohesion: 0.12
Nodes (4): Builder, ContextRequirement, CTContext, CTType

### Community 53 - "Misc: RadialMenu"
Cohesion: 0.14
Nodes (6): MenuButton, MenuRegion, Override, MenuButton, MenuRegion, RadialMenu

### Community 54 - "Misc: BuilderFilter"
Cohesion: 0.08
Nodes (12): Override, MaterialCostOverlay, BuilderFilter, Layer, Override, MaterialCostOverlay, BuilderFilter, net.neoforged.bus.api.IEventBus (+4 more)

### Community 56 - "Misc: ModifiersScreenList"
Cohesion: 0.14
Nodes (3): Entry, Override, ModifiersScreenList

### Community 57 - "Misc: ModifiersScreen"
Cohesion: 0.13
Nodes (8): BaseModifierEntry, Label, MiniButton, Override, BoxWidget, ModifiersScreenList, Override, ModifiersScreen

### Community 58 - "Misc: Entry"
Cohesion: 0.15
Nodes (3): Entry, Override, ModifiersScreenList

### Community 59 - "Misc: Sophisticated Building Update 4.0.0"
Cohesion: 0.10
Nodes (24): Placement Preview Crash Fix (compatibility), Radial Build Menu (Alt key), Sophisticated Building Update 4.0.0, sophisticatedbuilding-fabric-<version>.jar, sophisticatedbuilding-neoforge-<version>.jar, Fabric Build GitHub Actions Workflow, gradlew (Fabric Gradle wrapper), JDK 21 (Temurin) (+16 more)

### Community 60 - "Misc: net.minecraft.core.Direction"
Cohesion: 0.14
Nodes (5): MyPlaceContext, PredicateTraceResult, MyPlaceContext, net.minecraft.core.Direction, net.minecraft.world.item.context.BlockPlaceContext

### Community 61 - "Misc: net.neoforged.neoforge.common.ModConfigSpec.Builder"
Cohesion: 0.14
Nodes (14): Performance, Visuals, MaxBlocksPerAxis, MaxBlocksPlacedAtOnce, MaxMirrorRadius, Reach, Memory, Validation (+6 more)

### Community 62 - "Misc: BuildingUpgradeSettingsTab"
Cohesion: 0.12
Nodes (13): BuildingUpgradeSettingsTab, Override, Position, Toggle, SophisticatedBackpacksClientIntegration, BuildingUpgradeSettingsTab, Override, Position (+5 more)

### Community 63 - "Misc: AllGuiTextures"
Cohesion: 0.08
Nodes (24): AllGuiTextures, BUTTON, BUTTON_DOWN, BUTTON_HOVER, HOTSLOT, HOTSLOT_ACTIVE, HOTSLOT_ARROW, HOTSLOT_SUPER_ACTIVE (+16 more)

### Community 64 - "Misc: net.minecraft.world.phys.BlockHitResult"
Cohesion: 0.14
Nodes (8): BlockPos, Vec3, PredicateTraceResult, RaycastHelper, BlockPos, Vec3, RaycastHelper, net.minecraft.world.phys.BlockHitResult

### Community 65 - "Misc: AllGuiTextures"
Cohesion: 0.08
Nodes (24): AllGuiTextures, BUTTON, BUTTON_DOWN, BUTTON_HOVER, HOTSLOT, HOTSLOT_ACTIVE, HOTSLOT_ARROW, HOTSLOT_SUPER_ACTIVE (+16 more)

### Community 67 - "Misc: AllSpecialTextures"
Cohesion: 0.09
Nodes (19): AllSpecialTextures, BLANK, CHECKERED, CUTOUT_CHECKERED, GLUE, HIGHLIGHT_CHECKERED, SELECTION, THIN_CHECKERED (+11 more)

### Community 68 - "Misc: AbstractSimiScreen"
Cohesion: 0.14
Nodes (4): AbstractSimiScreen, Deprecated, Override, SuppressWarnings

### Community 69 - "Misc: BuildingUpgradeItem"
Cohesion: 0.16
Nodes (9): BuildingUpgradeItem, Override, TooltipContext, UpgradeConflictDefinition, net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult, net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig, net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup, net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase (+1 more)

### Community 70 - "Misc: AbstractSimiScreen"
Cohesion: 0.14
Nodes (4): AbstractSimiScreen, Deprecated, Override, SuppressWarnings

### Community 71 - "Misc: AbstractSimiContainerScreen"
Cohesion: 0.15
Nodes (4): AbstractSimiContainerScreen, Deprecated, Override, SuppressWarnings

### Community 72 - "Misc: BuildingUpgradeWrapper"
Cohesion: 0.16
Nodes (4): BuildingUpgradeWrapper, Override, net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper, net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper

### Community 73 - "Misc: CatnipRenderHelper"
Cohesion: 0.18
Nodes (3): CatnipRenderHelper, CatnipRenderHelper, net.minecraft.world.phys.AABB

### Community 74 - "Misc: AbstractSimiContainerScreen"
Cohesion: 0.15
Nodes (4): AbstractSimiContainerScreen, Deprecated, Override, SuppressWarnings

### Community 75 - "Misc: BuildingUpgradeItem"
Cohesion: 0.15
Nodes (8): BuildingUpgradeItem, IUpgradeCountLimitConfig, Override, TooltipContext, UpgradeConflictDefinition, UpgradeGroup, UpgradeType, UpgradeItemBase

### Community 76 - "Misc: net.minecraft.nbt.CompoundTag"
Cohesion: 0.17
Nodes (8): CompoundTag, ListTag, NBTUtils, CompoundTag, ListTag, NBTUtils, net.minecraft.nbt.CompoundTag, net.minecraft.nbt.ListTag

### Community 77 - "Misc: BuildSettings"
Cohesion: 0.16
Nodes (7): BuildSettings, ActionEnum, ReplaceMode, BLOCKS_AND_AIR, FILTERED_BY_OFFHAND, ONLY_AIR, ONLY_BLOCKS

### Community 78 - "Misc: InventoryHelper"
Cohesion: 0.18
Nodes (4): InventoryHelper, Deprecated, ItemStack, ItemUsageTracker

### Community 79 - "Misc: PowerLevel"
Cohesion: 0.20
Nodes (3): CompoundTag, PowerLevel, net.neoforged.neoforge.common.util.INBTSerializable

### Community 80 - "Misc: BuildSettings"
Cohesion: 0.16
Nodes (7): BuildSettings, ActionEnum, ReplaceMode, BLOCKS_AND_AIR, FILTERED_BY_OFFHAND, ONLY_AIR, ONLY_BLOCKS

### Community 81 - "Misc: RenderHandler"
Cohesion: 0.18
Nodes (5): com.mojang.blaze3d.vertex.ByteBufferBuilder, BufferSource, ItemStack, RenderHandler, net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext

### Community 84 - "Misc: ModifiersScreen"
Cohesion: 0.18
Nodes (4): BoxWidget, ModifiersScreenList, Override, ModifiersScreen

### Community 85 - "Misc: BlockPreviews"
Cohesion: 0.19
Nodes (4): BlockPreviews, BlockPos, BlockSet, PlacedBlocksEntry

### Community 87 - "Misc: AllIcons"
Cohesion: 0.13
Nodes (10): AllIcons, OptionEnum, BUILD_SPEED, CIRCLE_START, CUBE_FILL, FILL, LINE_THICKNESS, RAISED_EDGE (+2 more)

### Community 88 - "Misc: CommonConfig"
Cohesion: 0.19
Nodes (8): CommonConfig, MaxBlocksPerAxis, MaxBlocksPlacedAtOnce, MaxMirrorRadius, Reach, SimpleConfigValue, CommonConfig, net.minecraft.core.HolderLookup.Provider

### Community 89 - "Misc: Mirror"
Cohesion: 0.21
Nodes (5): BlockEntry, BlockSet, Override, Vec3, Mirror

### Community 90 - "Misc: FabricClientEvents"
Cohesion: 0.16
Nodes (4): FabricClientEvents, Override, SophisticatedBuildingFabricClient, net.fabricmc.api.ClientModInitializer

### Community 91 - "Misc: BaseModifierEntry"
Cohesion: 0.15
Nodes (7): MiniButton, BaseModifierEntry, Label, MiniButton, Override, MiniButton, net.createmod.catnip.gui.widget.ElementWidget

### Community 93 - "Misc: ServerConfig"
Cohesion: 0.15
Nodes (7): Memory, ServerConfig, Validation, ServerConfig, UndoSet, FixedStack, net.neoforged.neoforge.common.util.BlockSnapshot

### Community 94 - "Misc: Mirror"
Cohesion: 0.21
Nodes (5): BlockEntry, BlockSet, Override, Vec3, Mirror

### Community 95 - "Misc: RenderHandler"
Cohesion: 0.20
Nodes (4): BufferSource, ItemStack, Post, RenderHandler

### Community 96 - "Misc: TerrainMound"
Cohesion: 0.23
Nodes (4): ActionEnum, BlockPos, Override, TerrainMound

### Community 97 - "Misc: net.minecraft.world.level.block.Block"
Cohesion: 0.14
Nodes (5): Override, UncontainableBlockItem, Override, UncontainableBlockItem, net.minecraft.world.level.block.Block

### Community 98 - "Misc: net.minecraft.world.level.material.FluidState"
Cohesion: 0.22
Nodes (6): Override, VirtualChunkSection, Override, VirtualChunkSection, net.minecraft.world.level.chunk.LevelChunkSection, net.minecraft.world.level.material.FluidState

### Community 99 - "Misc: TerrainMound"
Cohesion: 0.23
Nodes (4): ActionEnum, BlockPos, Override, TerrainMound

### Community 100 - "Misc: RadialMirror"
Cohesion: 0.18
Nodes (5): BlockEntry, BlockSet, Override, Vec3, RadialMirror

### Community 101 - "Misc: BuildingUpgradeHelper"
Cohesion: 0.25
Nodes (3): BuildingUpgradeHelper, Item, ItemStack

### Community 102 - "Misc: Line"
Cohesion: 0.24
Nodes (5): Criteria, BlockPos, Override, Vec3, Line

### Community 103 - "Misc: Wall"
Cohesion: 0.22
Nodes (5): Criteria, BlockPos, Override, Vec3, Wall

### Community 105 - "Misc: Line"
Cohesion: 0.24
Nodes (5): Criteria, BlockPos, Override, Vec3, Line

### Community 106 - "Misc: Wall"
Cohesion: 0.22
Nodes (5): Criteria, BlockPos, Override, Vec3, Wall

### Community 109 - "Misc: net.minecraft.world.level.entity.LevelEntityGetter"
Cohesion: 0.25
Nodes (7): Override, VirtualLevelEntityGetter, Override, VirtualLevelEntityGetter, net.minecraft.util.AbortableIterationConsumer, net.minecraft.world.level.entity.EntityTypeTest, net.minecraft.world.level.entity.LevelEntityGetter

### Community 110 - "Misc: Sphere"
Cohesion: 0.29
Nodes (3): BlockPos, Override, Sphere

### Community 111 - "Misc: OptionEnum"
Cohesion: 0.15
Nodes (9): OptionEnum, BUILD_SPEED, CIRCLE_START, CUBE_FILL, FILL, LINE_THICKNESS, RAISED_EDGE, TERRAIN_NOISE (+1 more)

### Community 113 - "Misc: Sphere"
Cohesion: 0.29
Nodes (3): BlockPos, Override, Sphere

### Community 114 - "Misc: OmegaRandomizerBagItem"
Cohesion: 0.21
Nodes (3): ContainerProvider, Override, OmegaRandomizerBagItem

### Community 115 - "Misc: SingleItemLootModifier.java"
Cohesion: 0.29
Nodes (8): com.mojang.serialization.MapCodec, it.unimi.dsi.fastutil.objects.ObjectArrayList, Override, SingleItemLootModifier, net.minecraft.world.level.storage.loot.LootContext, net.minecraft.world.level.storage.loot.predicates.LootItemCondition, net.neoforged.neoforge.common.loot.IGlobalLootModifier, net.neoforged.neoforge.common.loot.LootModifier

### Community 116 - "Misc: ClientEvents"
Cohesion: 0.18
Nodes (6): ComputeCameraAngles, Load, ClientEvents, Post, Unload, net.neoforged.neoforge.client.event.RenderLevelStageEvent

### Community 120 - "Misc: ClientEvents"
Cohesion: 0.23
Nodes (4): ClientEvents, Key, Post, Pre

### Community 121 - "Misc: ItemHandlerWrapper"
Cohesion: 0.29
Nodes (3): ItemHandlerWrapper, Override, net.neoforged.neoforge.items.IItemHandlerModifiable

### Community 122 - "Misc: Cone"
Cohesion: 0.29
Nodes (3): Cone, BlockPos, Override

### Community 123 - "Misc: AllIcons"
Cohesion: 0.25
Nodes (4): AllIcons, DelegatedStencilElement, Override, Vec3

### Community 124 - "Misc: GNU Lesser General Public License v3"
Cohesion: 0.24
Nodes (11): GNU LGPL v3 (Fabric GUI textures license), Navrelis (hardfork maintainer), Requioss (original mod creator), GNU LGPL v3 (Fabric special textures license), GNU LGPL v3 (Fabric project template license), Effortless Building (original mod by Requios), EffortlessBuildingSophisticated (hardfork, maintained by Navrelis), GNU Lesser General Public License v3 (+3 more)

### Community 125 - "Misc: Cone"
Cohesion: 0.29
Nodes (3): Cone, BlockPos, Override

### Community 127 - "Misc: Circle"
Cohesion: 0.40
Nodes (3): Circle, BlockPos, Override

### Community 128 - "Misc: ClientEvents"
Cohesion: 0.27
Nodes (4): ClientEvents, CommonEvents, ModBusEvents, net.minecraft.world.level.LevelAccessor

### Community 129 - "Misc: net.minecraft.server.packs.resources.ResourceManager"
Cohesion: 0.31
Nodes (6): ClientResourceReloadListener, Override, ClientResourceReloadListener, Override, net.minecraft.server.packs.resources.ResourceManager, net.minecraft.server.packs.resources.ResourceManagerReloadListener

### Community 130 - "Misc: ModifierSettingsPacket"
Cohesion: 0.29
Nodes (6): ClientHandler, Context, Override, Type, ModifierSettingsPacket, ServerHandler

### Community 131 - "Misc: Circle"
Cohesion: 0.40
Nodes (3): Circle, BlockPos, Override

### Community 132 - "Misc: AllIcons"
Cohesion: 0.27
Nodes (4): AllIcons, DelegatedStencilElement, Override, Vec3

### Community 135 - "Misc: Array"
Cohesion: 0.31
Nodes (4): Array, BlockSet, Override, Vec3i

### Community 136 - "Misc: net.minecraft.tags.TagKey"
Cohesion: 0.33
Nodes (3): TagDependentIngredientItem, TagDependentIngredientItem, net.minecraft.tags.TagKey

### Community 138 - "Misc: Array"
Cohesion: 0.31
Nodes (4): Array, BlockSet, Override, Vec3i

### Community 142 - "Misc: net.minecraft.core.RegistryAccess"
Cohesion: 0.38
Nodes (3): IPartialSafeNBT, IPartialSafeNBT, net.minecraft.core.RegistryAccess

### Community 146 - "Misc: net.neoforged.fml.common.EventBusSubscriber"
Cohesion: 0.33
Nodes (4): CommonEvents, Unload, ModBusEvents, net.neoforged.fml.common.EventBusSubscriber

### Community 151 - "Misc: State"
Cohesion: 0.33
Nodes (6): State, GREEN, OFF, ON, RED, YELLOW

### Community 154 - "Misc: SophisticatedBuildingFabric"
Cohesion: 0.50
Nodes (3): Override, SophisticatedBuildingFabric, net.fabricmc.api.ModInitializer

### Community 155 - "Misc: gradlew script"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 156 - "Misc: gradlew script"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Ambiguous Edges - Review These
- `Sophisticated Building Update 4.0.0` → `Sophisticated Building Update 4.0.0`  [AMBIGUOUS]
  PATCH_NOTES_4.0.0.md · relation: references

## Knowledge Gaps
- **266 isolated node(s):** `ARRAY_ENTRY`, `MIRROR_ENTRY`, `RADIAL_MIRROR_ENTRY`, `ENABLE_BUTTON_BACKGROUND`, `CHECKMARK` (+261 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 892 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **29 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Sophisticated Building Update 4.0.0` and `Sophisticated Building Update 4.0.0`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **Why does `BlockSet` connect `Disabled & Single Modes` to `Build Mode Packets & Client Dispatch`, `Randomizer Bag Menus (MenuBase)`, `Power Level & Client Backpack Cache`, `Attachment Handler (Power Level API)`, `Misc: Array`, `Build Mode Enum & Base Mode`, `Two/Three-Click Modes & Array Modifier`, `Cube Build Mode`, `Client Events & Client Config`, `Misc: Array`, `Cylinder & Diagonal Line Modes`, `Radial Mirror Modifier`, `Server Block Placer`, `Build Modifiers Registry`, `Misc: ThreeClicksBuildMode`, `Misc: BuilderChain`, `Misc: BuilderChain`, `Misc: BuilderFilter`, `Misc: net.minecraft.nbt.CompoundTag`, `Misc: BlockPreviews`, `Misc: Mirror`, `Misc: ServerConfig`, `Misc: Mirror`, `Misc: RadialMirror`?**
  _High betweenness centrality (0.027) - this node is a cross-community bridge._
- **Why does `BuildingUpgradeWrapper` connect `Misc: BuildingUpgradeWrapper` to `Compat Helper & Item Utilities`, `Misc: BuildingUpgradeItem`, `Misc: BuildingUpgradeHelper`, `Misc: BuildingUpgradeWrapper`, `Misc: BuildingUpgradeItem`, `Misc: BuildingUpgradeContainer`, `Misc: BuildingUpgradeHelper`?**
  _High betweenness centrality (0.025) - this node is a cross-community bridge._
- **Why does `AllGuiTextures` connect `Misc: AllGuiTextures` to `Icon Buttons & Modifier Entries`, `Misc: org.joml.Matrix4f`, `Create GUI Widgets (Indicator, Tooltip)`?**
  _High betweenness centrality (0.024) - this node is a cross-community bridge._
- **What connects `ARRAY_ENTRY`, `MIRROR_ENTRY`, `RADIAL_MIRROR_ENTRY` to the rest of the system?**
  _266 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Build Mode Packets & Client Dispatch` be split into smaller, more focused modules?**
  _Cohesion score 0.023598196675119754 - nodes in this community are weakly interconnected._
- **Should `Randomizer Bag Menus (MenuBase)` be split into smaller, more focused modules?**
  _Cohesion score 0.034239130434782605 - nodes in this community are weakly interconnected._