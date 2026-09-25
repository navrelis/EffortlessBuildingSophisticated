package sophisticated.building.smoketest.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import sophisticated.building.ClientConfig;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.buildmodifier.BaseModifier;
import sophisticated.building.buildmodifier.Mirror;
import sophisticated.building.client.ClientBackpackItemCache;
import sophisticated.building.client.ClientBackpackToolCache;
import sophisticated.building.client.ClientBuildingUpgradeState;
import sophisticated.building.create.CreateClient;
import sophisticated.building.create.catnip.outliner.Outliner;
import sophisticated.building.create.foundation.utility.ghost.GhostBlocks;
import sophisticated.building.gui.buildmode.RadialMenu;
import sophisticated.building.gui.buildmodifier.ModifiersScreen;
import sophisticated.building.platform.Services;
import sophisticated.building.platform.services.IBackpackIntegration;
import sophisticated.building.smoketest.SmokeReport;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.backpack.SmokeAccessorySlots;
import sophisticated.building.smoketest.backpack.SmokeBackpacks;
import sophisticated.building.systems.BuildSettings;
import sophisticated.building.systems.BuilderChain;
import sophisticated.building.utilities.BlockEntry;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * The client smoke scenarios, run in a real client on a fresh superflat singleplayer world. Every action goes through
 * the code a player triggers: key mappings for clicks (so ClientEvents' mouse handling calls the BuilderChain and the
 * vanilla interaction runs next to it), the radial menu for build modes, the modifier screen for the mirror, the
 * packets to the integrated server. Test setup (inventories, backpacks, game mode) and the assertions run on the
 * server thread against the server's world, the source of truth.
 */
final class ClientScenarios {

    /** A fresh world every run (unique name); worlds of earlier runs are deleted. */
    private static final String WORLD_PREFIX = "sb-smoketest-";
    private static final String WORLD_NAME = WORLD_PREFIX + System.currentTimeMillis();
    private static final int LINE_LENGTH = 5;

    private final ClientDriver d;
    private final RadialMenuDriver radial;
    private final GuiScenarios gui;
    private final SmokeReport report = SmokeReport.get();

    /** First air layer above the superflat floor. */
    private int groundY;
    private BlockPos base;
    private boolean worldReady;

    ClientScenarios(ClientDriver driver) {
        this.d = driver;
        this.radial = new RadialMenuDriver(driver);
        this.gui = new GuiScenarios(driver, radial);
    }

    void runAll() {
        check("client.world_joined", this::joinFreshWorld);
        if (!worldReady) return;
        check("client.mod_data_pack_compatible", this::modDataPackCompatible);

        boolean lineSelected = check("client.radial_menu_opens", this::radialMenuSelectsLine);
        if (!lineSelected) {
            // The remaining scenarios still need Line mode
            d.clientRun(() -> SophisticatedBuildingClient.BUILD_MODES.setBuildMode(BuildModeEnum.LINE));
        }
        check("client.buildmode_line_preview", this::linePreview);
        check("client.mini_block_preview", this::miniBlockPreview);
        check("client.place_line", this::placeLine);
        check("client.break_line", this::breakLine);
        check("client.mirror_modifier", this::mirrorModifier);
        check("client.disable_quick_replace_preview", this::disableQuickReplacePreview);
        check("client.place_line_survival", this::placeLineSurvival);
        check("client.undo_redo", this::undoRedo);
        check("client.randomizer_bag_screens", gui::randomizerBagScreens);
        check("client.player_settings_gui", gui::playerSettingsGui);
        check("client.modifier_entry_widgets", gui::modifierEntryWidgets);

        runBackpackScenarios();
    }

    /** Runs one scenario as one named check; a failure is recorded and the next scenario starts from a clean state. */
    private boolean check(String name, Callable<String> scenario) {
        try {
            String detail = scenario.call();
            report.pass(name, detail);
            return true;
        } catch (Throwable error) {
            report.fail(name, SmokeReport.describe(error) + handState());
            recover();
            return false;
        }
    }

    /** What the client and the server think the player holds: a mismatch explains many placement failures. */
    private String handState() {
        if (!worldReady) return "";
        try {
            String client = d.client(() -> d.mc.player == null ? "-" : d.mc.player.getMainHandItem().toString());
            String server = d.server(server1 -> ClientDriver.serverPlayer(server1).getMainHandItem().toString());
            return " [main hand: client " + client + ", server " + server + "]";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private void recover() {
        try {
            d.clientRun(() -> {
                KeyMapping.releaseAll();
                // A menu screen is closed like a player closes it, so the server closes the menu too
                if (d.mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> && d.mc.player != null) d.mc.player.closeContainer();
                if (d.mc.screen != null) d.mc.setScreen(null);
                SophisticatedBuildingClient.BUILDER_CHAIN.cancel();
            });
            d.waitTicks(5);
        } catch (Throwable error) {
            SmokeTest.LOGGER.warn("Could not reset the client after a failed scenario", error);
        }
    }

    //region World

    private String joinFreshWorld() {
        d.waitUntilRealtime("the title screen", 600, () -> d.mc.getOverlay() == null && d.mc.screen != null && d.mc.level == null);
        d.clientRun(() -> {
            // An unfocused window must not pause the game (options changed in memory only)
            d.mc.options.pauseOnLostFocus = false;
            d.mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            d.mc.options.renderDistance().set(6);

            deleteOldWorlds(d.mc.gameDirectory.toPath().resolve("saves"));

            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_RANDOMTICKING).set(0, null);
            LevelSettings settings = new LevelSettings(WORLD_NAME, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    rules, WorldDataConfiguration.DEFAULT);
            // Creating the world blocks this task until the integrated server runs; the harness keeps polling below
            d.mc.execute(() -> d.mc.createWorldOpenFlows().createFreshLevel(WORLD_NAME, settings, new WorldOptions(20260924L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    d.mc.screen));
        });
        d.waitUntilRealtime("the new world to load", 300, () -> {
            failOnErrorScreen();
            return d.mc.level != null && d.mc.player != null && d.mc.screen == null && d.mc.getSingleplayerServer() != null;
        });
        d.waitTicks(20);

        String detail = d.server(server -> {
            ServerLevel level = server.overworld();
            level.setDayTime(6000);
            BlockPos spawn = level.getSharedSpawnPos();
            groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
            base = new BlockPos(spawn.getX() + 4, groundY, spawn.getZ() + 4);
            // A clean building site: air above the superflat floor for every lane
            for (int x = base.getX() - 14; x <= base.getX() + 16; x++) {
                for (int z = base.getZ() - 6; z <= lane(8).getZ() + 12; z++) {
                    for (int y = groundY; y <= groundY + 10; y++) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
            ServerPlayer player = ClientDriver.serverPlayer(server);
            return "Joined fresh superflat world '" + WORLD_NAME + "' as " + player.getGameProfile().getName()
                    + " (" + player.gameMode.getGameModeForPlayer() + "), ground y=" + groundY + ", site at " + base.toShortString();
        });
        worldReady = true;
        return detail;
    }

    /** A failure screen (world creation failed, disconnected) never goes away by itself: fail at once. */
    private void failOnErrorScreen() {
        var screen = d.mc.screen;
        if (screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen
                || screen instanceof net.minecraft.client.gui.screens.AlertScreen
                || screen instanceof net.minecraft.client.gui.screens.ErrorScreen) {
            throw new AssertionError("The game shows a failure screen: " + screen.getClass().getSimpleName() + " '"
                    + screen.getTitle().getString() + "'");
        }
    }

    /** The mod's data pack is enabled in the world and not flagged incompatible (wrong pack_format). */
    private String modDataPackCompatible() {
        return d.server(server -> {
            List<String> packs = new ArrayList<>();
            String modPack = null;
            for (var pack : server.getPackRepository().getSelectedPacks()) {
                String entry = pack.getId() + (pack.getCompatibility().isCompatible() ? "" : " (" + pack.getCompatibility() + ")");
                packs.add(entry);
                if (pack.getId().contains(sophisticated.building.SophisticatedBuilding.MODID) && !pack.getId().contains("smoketest")) {
                    modPack = entry;
                    if (!pack.getCompatibility().isCompatible()) {
                        throw new AssertionError("The mod's data pack is flagged " + pack.getCompatibility() + ": " + packs);
                    }
                }
            }
            if (modPack == null) {
                // Fabric serves every mod's data through one combined pack
                return "No separate data pack for the mod (loader combines mod data); selected packs: " + packs;
            }
            return "Mod data pack " + modPack + " is enabled and compatible; selected packs: " + packs;
        });
    }

    private static void deleteOldWorlds(Path saves) {
        if (!Files.isDirectory(saves)) return;
        try (Stream<Path> worlds = Files.list(saves)) {
            for (Path world : worlds.filter(p -> p.getFileName().toString().startsWith(WORLD_PREFIX)).toList()) {
                deleteWorld(world);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not list " + saves, e);
        }
    }

    private static void deleteWorld(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not delete the old smoke test world " + dir, e);
        }
    }

    /** Start of lane k: every scenario builds in its own row so they cannot disturb each other. */
    private BlockPos lane(int k) {
        return base.offset(0, 0, 10 * k);
    }

    //endregion

    //region Creative scenarios

    private String radialMenuSelectsLine() {
        giveHotbar(new ItemStack(Items.STONE, 64));
        radial.open();
        d.screenshot("radial_menu");
        boolean visible = d.client(() -> d.mc.screen instanceof RadialMenu);
        if (!visible) throw new AssertionError("The radial menu closed while the radial key was held");
        radial.hover(BuildModeEnum.LINE);
        radial.clickAndRelease();
        BuildModeEnum active = d.client(SophisticatedBuildingClient.BUILD_MODES::getBuildMode);
        if (active != BuildModeEnum.LINE) throw new AssertionError("Clicked Line in the radial menu but the build mode is " + active);
        return "Radial key opened the menu, it rendered (hit-test highlighted LINE under the mouse), a click selected LINE, releasing the key closed it";
    }

    private String linePreview() {
        d.clientRun(() -> ModeOptions.performAction(d.mc.player, ModeOptions.ActionEnum.THICKNESS_1));
        giveHotbar(new ItemStack(Items.STONE, 64));
        BlockPos start = lane(0);
        firstClick(start, new Vec3(start.getX() + 2.5, groundY, start.getZ() + 3.5), groundTop(start));
        Preview preview = aimSecond(groundTop(start.east(LINE_LENGTH - 1)), LINE_LENGTH);
        d.screenshot("line_preview");
        List<BlockPos> expected = row(start, LINE_LENGTH);
        preview.expectExactly(expected, LINE_LENGTH, 0);
        return "Line preview after the first click: " + preview.valid.size() + " valid blocks " + expected.getFirst().toShortString()
                + ".." + expected.getLast().toShortString() + ", state " + preview.state;
    }

    /**
     * On the line preview of the previous check: the mini block previews (a small ghost of the stone in each outlined
     * position) are drawn by default, gone with showMiniBlockPreview off, and gone when the line has more blocks than
     * maxMiniBlockPreviews. Both are client settings of the player settings screen.
     */
    private String miniBlockPreview() {
        if (d.client(SophisticatedBuildingClient.BUILDER_CHAIN::getBuildingState) != BuilderChain.BuildingState.PLACING) {
            throw new AssertionError("Needs the line preview of client.buildmode_line_preview; " + d.client(this::chainState));
        }
        List<BlockPos> line = row(lane(0), LINE_LENGTH);
        boolean showBefore = d.client(() -> ClientConfig.visuals.showMiniBlockPreview.get());
        int maxBefore = d.client(() -> ClientConfig.performance.maxMiniBlockPreviews.get());
        try {
            int shown = ghostsWith(line, true, 0);
            d.screenshot("mini_preview_on");
            int hidden = ghostsWith(line, false, 0);
            d.screenshot("mini_preview_off");
            int capped = ghostsWith(line, true, LINE_LENGTH - 1);
            expectEquals("mini previews with showMiniBlockPreview on (no limit)", LINE_LENGTH, shown);
            expectEquals("mini previews with showMiniBlockPreview off", 0, hidden);
            expectEquals("mini previews of " + LINE_LENGTH + " blocks with maxMiniBlockPreviews " + (LINE_LENGTH - 1), 0, capped);
            return "Line preview: " + shown + " mini block previews by default, " + hidden + " with showMiniBlockPreview off, "
                    + capped + " with maxMiniBlockPreviews " + (LINE_LENGTH - 1);
        } finally {
            d.clientRun(() -> {
                ClientConfig.visuals.showMiniBlockPreview.set(showBefore);
                ClientConfig.performance.maxMiniBlockPreviews.set(maxBefore);
                SophisticatedBuildingClient.BLOCK_PREVIEWS.onConfigChanged();
            });
        }
    }

    /** Sets the two mini preview settings (in memory, like the settings screen before saving) and counts the ghosts. */
    private int ghostsWith(List<BlockPos> positions, boolean show, int max) {
        d.clientRun(() -> {
            ClientConfig.visuals.showMiniBlockPreview.set(show);
            ClientConfig.performance.maxMiniBlockPreviews.set(max);
            SophisticatedBuildingClient.BLOCK_PREVIEWS.onConfigChanged();
        });
        d.waitTicks(5);
        return d.client(() -> countGhosts(positions));
    }

    /** Client thread: ghost blocks shown at these positions (BlockPreviews keys them by position). */
    private static int countGhosts(List<BlockPos> positions) {
        try {
            Field field = GhostBlocks.class.getDeclaredField("ghosts");
            field.setAccessible(true);
            Map<?, ?> ghosts = (Map<?, ?>) field.get(CreateClient.GHOST_BLOCKS);
            int count = 0;
            for (BlockPos pos : positions) {
                if (ghosts.containsKey(pos.toShortString())) count++;
            }
            return count;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot read the ghost blocks", e);
        }
    }

    private String placeLine() {
        BlockPos start = lane(0);
        List<BlockPos> expected = row(start, LINE_LENGTH);
        if (d.client(SophisticatedBuildingClient.BUILDER_CHAIN::getBuildingState) != BuilderChain.BuildingState.PLACING) {
            // The preview scenario failed before its first click; do the first click here
            firstClick(start, new Vec3(start.getX() + 2.5, groundY, start.getZ() + 3.5), groundTop(start));
            aimSecond(groundTop(start.east(LINE_LENGTH - 1)), LINE_LENGTH);
        }
        int stoneBefore = d.server(server -> ClientDriver.count(ClientDriver.serverPlayer(server).getInventory(), Items.STONE));
        d.rightClick();
        waitForBlocks(expected, Blocks.STONE, "the line");
        d.waitTicks(10);
        expectOnly(expected, Blocks.STONE, start.west(), start.east(LINE_LENGTH), start.above(), start.north(), start.south());
        int stoneAfter = d.server(server -> ClientDriver.count(ClientDriver.serverPlayer(server).getInventory(), Items.STONE));
        expectEquals("stone in the creative inventory (nothing consumed)", stoneBefore, stoneAfter);
        d.screenshot("line_placed");
        return "Second click placed " + expected.size() + " stone " + expected.getFirst().toShortString() + ".." + expected.getLast().toShortString()
                + " (server world), creative inventory unchanged (" + stoneAfter + ")";
    }

    private String breakLine() {
        BlockPos start = lane(0);
        List<BlockPos> line = row(start, LINE_LENGTH);
        placeDirectly(line, Blocks.STONE);
        firstBreakClick(start);
        Preview preview = aimSecond(groundTop(start.east(LINE_LENGTH - 1)), LINE_LENGTH);
        preview.expectExactly(line, LINE_LENGTH, 0);
        d.leftClick();
        waitForBlocks(line, Blocks.AIR, "the line to be broken");
        return "Creative mass break: two left clicks broke the " + LINE_LENGTH + " stone line (preview " + preview.valid.size() + " blocks)";
    }

    private String mirrorModifier() {
        BlockPos start = lane(1).east(2);
        // Mirror plane between x = start-3 and start-2 (the plane sits on the lower corner of lane(1)'s block)
        Vec3 plane = Vec3.atLowerCornerOf(lane(1));
        giveHotbar(new ItemStack(Items.STONE, 64));

        d.clientRun(ClientEvents::openModifierSettings);
        d.waitUntil("the modifier screen to open", 40, () -> d.mc.screen instanceof ModifiersScreen);
        d.waitTicks(5);
        int before = d.client(() -> SophisticatedBuildingClient.BUILD_MODIFIERS.getModifierSettingsList().size());
        d.clientRun(() -> {
            AbstractWidget addMirror = widget((ModifiersScreen) d.mc.screen, "addMirrorButton");
            d.mc.screen.mouseClicked(addMirror.getX() + addMirror.getWidth() / 2.0, addMirror.getY() + addMirror.getHeight() / 2.0, 0);
        });
        Mirror mirror = d.client(() -> {
            List<BaseModifier> list = SophisticatedBuildingClient.BUILD_MODIFIERS.getModifierSettingsList();
            if (list.size() != before + 1 || !(list.getLast() instanceof Mirror added)) {
                throw new AssertionError("Clicking 'Add Mirror' did not add a mirror (modifiers: " + list + ")");
            }
            // What the mirror entry's position and axis inputs set
            added.position = plane;
            added.mirrorX = true;
            added.mirrorY = false;
            added.mirrorZ = false;
            added.radius = 20;
            added.enabled = true;
            return added;
        });
        // Rebuild the screen so its mirror entry shows the values set above
        d.clientRun(() -> d.mc.screen.init(d.mc, d.mc.screen.width, d.mc.screen.height));
        d.screenshot("modifiers_screen");
        // Closing the screen saves the modifiers (ModifierSettingsPacket to the server)
        d.clientRun(() -> d.mc.screen.onClose());
        d.waitUntil("the modifier screen to close", 40, () -> d.mc.screen == null);

        try {
            List<BlockPos> original = row(start, 3);
            List<BlockPos> mirrored = new ArrayList<>();
            for (BlockPos pos : original) {
                mirrored.add(new BlockPos((int) Math.floor(2 * plane.x - pos.getX() - 0.5), pos.getY(), pos.getZ()));
            }
            List<BlockPos> expected = new ArrayList<>(original);
            expected.addAll(mirrored);

            firstClick(start, new Vec3(start.getX() + 2.5, groundY, start.getZ() + 3.5), groundTop(start));
            Preview preview = aimSecond(groundTop(start.east(2)), expected.size());
            preview.expectExactly(expected, expected.size(), 0);
            d.rightClick();
            waitForBlocks(expected, Blocks.STONE, "the line and its mirror image");
            // View from the side: the line, the mirror plane and the mirror image
            d.teleport(new Vec3(plane.x, groundY, start.getZ() + 9.5));
            d.lookAt(new Vec3(plane.x, groundY + 0.5, start.getZ() + 0.5));
            d.screenshot("mirror_placed");
            return "Mirror added through the modifier screen (plane x=" + plane.x + "); a 3 block line placed "
                    + original.getFirst().toShortString() + ".." + original.getLast().toShortString() + " and its mirror image "
                    + mirrored.getFirst().toShortString() + ".." + mirrored.getLast().toShortString();
        } finally {
            d.clientRun(() -> {
                SophisticatedBuildingClient.BUILD_MODIFIERS.removeModifierSettings(mirror);
                SophisticatedBuildingClient.BUILD_MODIFIERS.save();
            });
        }
    }

    /**
     * Disable mode on one block: without Quick Replace vanilla places it (no preview of the mod), with Quick Replace the
     * mod replaces the block looked at and shows it (ghost block and outline) like the other modes.
     */
    private String disableQuickReplacePreview() {
        BlockPos target = lane(9);
        giveHotbar(new ItemStack(Items.OAK_PLANKS, 64));
        placeDirectly(List.of(target), Blocks.STONE);
        radial.select(BuildModeEnum.DISABLED);
        try {
            d.teleport(new Vec3(target.getX() + 0.5, groundY, target.getZ() + 3.5));
            Vec3 aim = Vec3.atCenterOf(target).add(0, 0.5, 0);
            d.lookAt(aim);

            // Plain Disable mode: vanilla's click, no outline of the mod (an earlier "single" outline has faded by now)
            d.clientRun(() -> SophisticatedBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.ONLY_AIR));
            for (int i = 0; i < 20; i++) {
                d.aimNow(aim);
                d.waitTicks(1);
            }
            if (d.client(() -> liveOutline(SINGLE_OUTLINE))) {
                throw new AssertionError("Plain Disable mode shows the mod's single block outline; " + d.client(this::chainState));
            }
            d.screenshot("disable_plain");

            d.clientRun(() -> SophisticatedBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.BLOCKS_AND_AIR));
            try {
                d.waitUntil("the Quick Replace preview of the stone block looked at", 40, () -> {
                    d.aimNow(aim);
                    var blocks = SophisticatedBuildingClient.BUILDER_CHAIN.getBlocks();
                    return blocks.size() == 1 && target.equals(blocks.firstPos) && liveOutline(SINGLE_OUTLINE);
                });
            } catch (AssertionError timeout) {
                throw new AssertionError(timeout.getMessage() + "; " + d.client(this::chainState));
            }
            d.screenshot("disable_quick_replace_preview");
            return "Disable mode on " + target.toShortString() + ": no outline of the mod without Quick Replace, with Quick Replace "
                    + "the replaced block's preview and outline are shown";
        } finally {
            d.clientRun(() -> SophisticatedBuildingClient.BUILD_SETTINGS.setReplaceMode(BuildSettings.ReplaceMode.ONLY_AIR));
            radial.select(BuildModeEnum.LINE);
        }
    }

    /** The id BlockPreviews gives the outline of a one-block preview. */
    private static final String SINGLE_OUTLINE = "single";

    /** Client thread: the outline is shown this tick (not fading out). */
    private static boolean liveOutline(Object id) {
        var entry = Outliner.getInstance().getOutlines().get(id);
        return entry != null && !entry.isFading();
    }

    //endregion

    //region Survival scenarios

    private void enterSurvival() {
        String name = d.server(server -> {
            ServerPlayer player = ClientDriver.serverPlayer(server);
            player.setGameMode(GameType.SURVIVAL);
            return player.getGameProfile().getName();
        });
        // The mod's own command, as an operator would run it: survival reach 32, 32 blocks per axis
        d.command("powerlevel set " + name + " 3");
        d.waitUntil("the client to be in survival with power level 3", 60,
                () -> d.mc.gameMode.getPlayerMode() == GameType.SURVIVAL && AttachmentHandler.getPowerLevel(d.mc.player) == 3);
    }

    private String placeLineSurvival() {
        enterSurvival();
        giveHotbar(new ItemStack(Items.OAK_PLANKS, 64), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.IRON_AXE));
        BlockPos start = lane(2);
        List<BlockPos> expected = row(start, LINE_LENGTH);
        buildLine(start, LINE_LENGTH);
        waitForBlocks(expected, Blocks.OAK_PLANKS, "the survival line");
        d.waitTicks(10);
        int planks = serverCount(Items.OAK_PLANKS);
        expectEquals("oak planks left after placing " + LINE_LENGTH, 64 - LINE_LENGTH, planks);
        return "Survival line of " + LINE_LENGTH + " oak planks placed, " + LINE_LENGTH + " planks consumed (64 -> " + planks + ")";
    }

    private String undoRedo() {
        BlockPos start = lane(2);
        List<BlockPos> line = row(start, LINE_LENGTH);
        if (!allAre(line, Blocks.OAK_PLANKS)) throw new AssertionError("The survival line to undo is not there");
        int before = serverCount(Items.OAK_PLANKS);

        // The radial menu's Undo button and the Ctrl+Z key both run this action
        d.clientRun(() -> ModeOptions.performAction(d.mc.player, ModeOptions.ActionEnum.UNDO));
        waitForBlocks(line, Blocks.AIR, "undo to remove the line");
        d.waitTicks(5);
        int afterUndo = serverCount(Items.OAK_PLANKS);
        expectEquals("oak planks after undo (mined back with the axe)", before + LINE_LENGTH, afterUndo);

        d.clientRun(() -> ModeOptions.performAction(d.mc.player, ModeOptions.ActionEnum.REDO));
        waitForBlocks(line, Blocks.OAK_PLANKS, "redo to restore the line");
        d.waitTicks(5);
        int afterRedo = serverCount(Items.OAK_PLANKS);
        expectEquals("oak planks after redo", before, afterRedo);
        return "Undo removed the " + LINE_LENGTH + " blocks and gave the planks back (" + before + " -> " + afterUndo
                + "), redo restored them and charged them again (-> " + afterRedo + ")";
    }

    //endregion

    //region Sophisticated Backpacks scenarios

    private void runBackpackScenarios() {
        IBackpackIntegration integration = Services.backpacks();
        if (integration == IBackpackIntegration.NONE) {
            SmokeTest.LOGGER.info("No Sophisticated Backpacks integration in this loader build: no sb.* scenarios");
            return;
        }
        Optional<SmokeBackpacks> fixture = SmokeBackpacks.find();
        if (fixture.isEmpty()) {
            report.fail("sb.fixture", "This loader build has the Sophisticated Backpacks integration but its smoke source set registers no SmokeBackpacks fixture");
            return;
        }
        SmokeBackpacks backpacks = fixture.get();
        try {
            enterSurvival();
        } catch (Throwable error) {
            report.fail("sb.setup", error);
            return;
        }
        BackpackHolder holder = new BackpackHolder();
        check("sb.hud_count_synced", () -> hudCountSynced(backpacks, holder));
        check("sb.upgrade_supplies_blocks", () -> upgradeSuppliesBlocks(backpacks, holder));
        check("sb.tier_cap", () -> tierCap(backpacks, holder));
        check("sb.disabled_upgrade_ignored", () -> disabledUpgradeIgnored(backpacks, holder));
        check("sb.tool_swapper_tools", () -> toolSwapperTools(backpacks));
        check("sb.worn_backpack_chest", () -> wornBackpack(backpacks, null, lane(7)));
        String accessoryReason = d.server(server -> {
            // Probe the accessory slot with an empty backpack; the scenario equips its own one
            ServerPlayer player = ClientDriver.serverPlayer(server);
            player.getInventory().clearContent();
            String reason = backpacks.equipInAccessorySlot(player, backpacks.createBackpack(0, false, false, List.of()));
            clearAccessory(backpacks, player);
            return reason;
        });
        if (accessoryReason != null) {
            report.skip("sb.worn_backpack", accessoryReason + "; the worn case is covered by sb.worn_backpack_chest (backpack in the chest armor slot)");
        } else {
            check("sb.worn_backpack", () -> wornBackpack(backpacks, "accessory", lane(8)));
        }
        check("sb.upgrade_settings_tab", () -> gui.upgradeSettingsTab(backpacks));
    }

    private static final class BackpackHolder {
        int slot = 1;
    }

    private ItemStack backpackInSlot(ServerPlayer player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) throw new AssertionError("The backpack is no longer in inventory slot " + slot);
        return stack;
    }

    private String hudCountSynced(SmokeBackpacks backpacks, BackpackHolder holder) {
        d.server(server -> {
            ServerPlayer player = ClientDriver.serverPlayer(server);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(Items.STONE, 1));
            player.getInventory().setItem(holder.slot, backpacks.createBackpack(1, true, false, List.of(new ItemStack(Items.STONE, 64))));
            return null;
        });
        d.selectHotbarSlot(0);
        d.waitUntil("the client to learn the Building Upgrade (tier 1, 32 blocks) and the backpack's 64 stone", 100,
                () -> ClientBuildingUpgradeState.getTier() == 1 && ClientBuildingUpgradeState.getMaxBlocks() == 32
                        && ClientBackpackItemCache.getCount(Items.STONE) == 64);
        return "Server synced the upgrade state (tier " + d.client(ClientBuildingUpgradeState::getTier) + ", max "
                + d.client(ClientBuildingUpgradeState::getMaxBlocks) + ") and the backpack count (stone "
                + d.client(() -> ClientBackpackItemCache.getCount(Items.STONE)) + ") to the client cache; "
                + backpacks.describe();
    }

    private String upgradeSuppliesBlocks(SmokeBackpacks backpacks, BackpackHolder holder) {
        int inBackpackBefore = d.server(server -> backpacks.count(backpackInSlot(ClientDriver.serverPlayer(server), holder.slot), Items.STONE));
        int heldBefore = serverCount(Items.STONE);
        if (heldBefore != 1) throw new AssertionError("Expected the player to hold exactly 1 stone, holds " + heldBefore);

        BlockPos start = lane(3);
        List<BlockPos> expected = row(start, LINE_LENGTH);
        Preview preview = buildLine(start, LINE_LENGTH);
        preview.expectExactly(expected, LINE_LENGTH, 0);
        waitForBlocks(expected, Blocks.STONE, "the line built from the backpack");
        d.waitTicks(10);

        int held = serverCount(Items.STONE);
        int inBackpack = d.server(server -> backpacks.count(backpackInSlot(ClientDriver.serverPlayer(server), holder.slot), Items.STONE));
        expectEquals("stone held (kept as the build anchor)", 1, held);
        expectEquals("stone left in the backpack", inBackpackBefore - LINE_LENGTH, inBackpack);
        d.waitUntil("the client's backpack count to follow (" + inBackpack + ")", 60, () -> ClientBackpackItemCache.getCount(Items.STONE) == inBackpack);
        return "Holding 1 stone, a " + LINE_LENGTH + " block line was placed with stone from the backpack (backpack "
                + inBackpackBefore + " -> " + inBackpack + ", held 1 -> " + held + "), client HUD count updated";
    }

    private String tierCap(SmokeBackpacks backpacks, BackpackHolder holder) {
        int cap = 32;
        int inBackpackBefore = d.server(server -> backpacks.count(backpackInSlot(ClientDriver.serverPlayer(server), holder.slot), Items.STONE));
        if (inBackpackBefore <= cap) throw new AssertionError("Need more than " + cap + " stone in the backpack, has " + inBackpackBefore);
        radial.select(BuildModeEnum.FLOOR);
        try {
            BlockPos start = lane(4);
            List<BlockPos> floor = new ArrayList<>();
            for (int x = 0; x < 6; x++) {
                for (int z = 0; z < 6; z++) {
                    floor.add(start.offset(x, 0, z));
                }
            }
            firstClick(start, new Vec3(start.getX() + 8.5, groundY, start.getZ() + 8.5), groundTop(start));
            Preview preview = aimSecond(groundTop(start.offset(5, 0, 5)), floor.size());
            if (preview.valid.size() + preview.invalid.size() != floor.size()) {
                throw new AssertionError("Expected a 6x6 floor preview (36 blocks), got " + preview.valid.size() + " valid + " + preview.invalid.size() + " invalid" + "; " + preview.context());
            }
            d.rightClick();
            d.waitUntilServer("the capped floor to be placed", 200, server -> countBlocks(server.overworld(), floor, Blocks.STONE) >= cap);
            d.waitTicks(20);
            int placed = d.server(server -> countBlocks(server.overworld(), floor, Blocks.STONE));
            int held = serverCount(Items.STONE);
            int inBackpack = d.server(server -> backpacks.count(backpackInSlot(ClientDriver.serverPlayer(server), holder.slot), Items.STONE));
            expectEquals("blocks placed from a 36 block floor with a tier 1 upgrade (cap 32)", cap, placed);
            expectEquals("stone left in the backpack", inBackpackBefore - cap, inBackpack);
            expectEquals("stone held", 1, held);
            return "Tier 1 Building Upgrade capped a 36 block floor at " + placed + " blocks (client preview " + preview.valid.size()
                    + " valid / " + preview.invalid.size() + " invalid), backpack " + inBackpackBefore + " -> " + inBackpack;
        } finally {
            radial.select(BuildModeEnum.LINE);
        }
    }

    private String disabledUpgradeIgnored(SmokeBackpacks backpacks, BackpackHolder holder) {
        int held = 3;
        int inBackpackBefore = d.server(server -> {
            ServerPlayer player = ClientDriver.serverPlayer(server);
            ItemStack backpack = backpackInSlot(player, holder.slot);
            backpacks.setBuildingUpgradeEnabled(backpack, false);
            player.getInventory().setItem(0, new ItemStack(Items.STONE, held));
            return backpacks.count(backpack, Items.STONE);
        });
        d.waitUntil("the client to see no active Building Upgrade", 100, () -> !ClientBuildingUpgradeState.hasUpgrade());

        BlockPos start = lane(5);
        List<BlockPos> line = row(start, LINE_LENGTH);
        Preview preview = buildLine(start, LINE_LENGTH);
        try {
            d.waitUntilServer("the held stone to be placed", 100, server -> countBlocks(server.overworld(), line, Blocks.STONE) >= held);
        } catch (AssertionError timeout) {
            throw new AssertionError(timeout.getMessage() + "; world: " + worldState(line) + "; client preview " + preview.valid.size()
                    + " valid / " + preview.invalid.size() + " invalid; " + preview.context());
        }
        d.waitTicks(20);
        int placed = d.server(server -> countBlocks(server.overworld(), line, Blocks.STONE));
        int heldAfter = serverCount(Items.STONE);
        int inBackpack = d.server(server -> backpacks.count(backpackInSlot(ClientDriver.serverPlayer(server), holder.slot), Items.STONE));
        expectEquals("blocks placed with the upgrade disabled (only the " + held + " held stone)", held, placed);
        expectEquals("stone held after the build", 0, heldAfter);
        expectEquals("stone in the backpack (untouched)", inBackpackBefore, inBackpack);
        return "Upgrade disabled: a " + LINE_LENGTH + " block line placed only the " + placed + " held stone, backpack untouched ("
                + inBackpack + "); client preview " + preview.valid.size() + " valid / " + preview.invalid.size() + " invalid";
    }

    private String toolSwapperTools(SmokeBackpacks backpacks) {
        BlockPos start = lane(6);
        List<BlockPos> line = row(start, LINE_LENGTH);
        d.server(server -> {
            ServerPlayer player = ClientDriver.serverPlayer(server);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(Items.STICK));
            player.getInventory().setItem(1, backpacks.createBackpack(0, false, true, List.of(new ItemStack(Items.DIAMOND_PICKAXE))));
            return null;
        });
        placeDirectly(line, Blocks.STONE);
        d.selectHotbarSlot(0);
        d.waitUntil("the client to learn the pickaxe in the backpack's Tool Swapper", 100,
                () -> ClientBackpackToolCache.snapshot().stream().anyMatch(stack -> stack.is(Items.DIAMOND_PICKAXE)));

        firstBreakClick(start);
        Preview preview = aimSecond(groundTop(start.east(LINE_LENGTH - 1)), LINE_LENGTH);
        preview.expectExactly(line, LINE_LENGTH, 0);
        d.leftClick();
        waitForBlocks(line, Blocks.AIR, "the survival break with the backpack's pickaxe", 300);
        d.waitTicks(5);

        int damage = d.server(server -> backpacks.find(ClientDriver.serverPlayer(server).getInventory().getItem(1), Items.DIAMOND_PICKAXE).getDamageValue());
        int cobblestone = serverCount(Items.COBBLESTONE);
        expectEquals("damage of the pickaxe in the backpack", LINE_LENGTH, damage);
        expectEquals("cobblestone dropped into the inventory", LINE_LENGTH, cobblestone);
        return "Survival mass break of " + LINE_LENGTH + " stone with only a stick in hand used the diamond pickaxe from the Tool Swapper backpack (damage "
                + damage + "), drops in the inventory (" + cobblestone + " cobblestone)";
    }

    private String wornBackpack(SmokeBackpacks backpacks, String accessory, BlockPos start) {
        String slotName = d.server(server -> {
            ServerPlayer player = ClientDriver.serverPlayer(server);
            player.getInventory().clearContent();
            clearAccessory(backpacks, player);
            player.getInventory().setItem(0, new ItemStack(Items.STONE, 1));
            ItemStack backpack = backpacks.createBackpack(1, true, false, List.of(new ItemStack(Items.STONE, 64)));
            if (accessory == null) {
                player.setItemSlot(EquipmentSlot.CHEST, backpack);
                return "chest armor slot";
            }
            String reason = backpacks.equipInAccessorySlot(player, backpack);
            if (reason != null) throw new AssertionError("Could not equip the backpack in an accessory slot: " + reason);
            return SmokeAccessorySlots.find().map(SmokeAccessorySlots::describe).orElse("accessory slot");
        });
        d.selectHotbarSlot(0);
        // Up to 100 ticks: the Fabric port rescans Trinkets slots for backpacks that often
        d.waitUntil("the client to learn the worn backpack's upgrade and stone", 200,
                () -> ClientBuildingUpgradeState.getTier() == 1 && ClientBackpackItemCache.getCount(Items.STONE) == 64);

        List<BlockPos> expected = row(start, LINE_LENGTH);
        buildLine(start, LINE_LENGTH);
        waitForBlocks(expected, Blocks.STONE, "the line built from the worn backpack");
        d.waitTicks(10);
        int inBackpack = d.server(server -> {
            ServerPlayer player = ClientDriver.serverPlayer(server);
            ItemStack worn = accessory == null ? player.getItemBySlot(EquipmentSlot.CHEST) : backpacks.getFromAccessorySlot(player);
            if (worn.isEmpty()) throw new AssertionError("The worn backpack is gone");
            int count = backpacks.count(worn, Items.STONE);
            if (accessory == null) player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            else clearAccessory(backpacks, player);
            return count;
        });
        expectEquals("stone left in the worn backpack", 64 - LINE_LENGTH, inBackpack);
        expectEquals("stone held", 1, serverCount(Items.STONE));
        return "Backpack worn in the " + slotName + " supplied a " + LINE_LENGTH + " block line (64 -> " + inBackpack + "), held stone kept";
    }

    private static void clearAccessory(SmokeBackpacks backpacks, ServerPlayer player) {
        SmokeAccessorySlots.find().ifPresent(slots -> slots.equip(player, ItemStack.EMPTY));
    }

    //endregion

    //region Building steps

    /** Stands at feet, aims at the first target and right-clicks: the first click of a two-click build mode. */
    private void firstClick(BlockPos start, Vec3 feet, Vec3 aim) {
        d.teleport(feet);
        d.lookAt(aim);
        try {
            d.waitUntil("the BuilderChain to take " + start.toShortString() + " as start position", 20, () -> {
                d.aimNow(aim);
                BlockEntry startPos = SophisticatedBuildingClient.BUILDER_CHAIN.getStartPosForPlacing();
                return startPos != null && startPos.blockPos.equals(start);
            });
        } catch (AssertionError timeout) {
            throw new AssertionError(timeout.getMessage() + " (aiming at " + aim + "); " + d.client(this::chainState));
        }
        d.rightClick();
        expectBuildingState(BuilderChain.BuildingState.PLACING, "after the first right click");
    }

    /** Stands next to the row starting at start, aims at the top of its first block and left-clicks (first click of a break). */
    private void firstBreakClick(BlockPos start) {
        d.teleport(new Vec3(start.getX() + 2.5, groundY, start.getZ() + 3.5));
        Vec3 aim = Vec3.atCenterOf(start).add(0, 0.5, 0);
        d.lookAt(aim);
        try {
            d.waitUntil("the BuilderChain to target " + start.toShortString() + " for breaking", 20, () -> {
                d.aimNow(aim);
                BuilderChain chain = SophisticatedBuildingClient.BUILDER_CHAIN;
                return start.equals(chain.getStartPosForBreaking()) && chain.getAbilitiesState() != BuilderChain.AbilitiesState.NONE;
            });
        } catch (AssertionError timeout) {
            throw new AssertionError(timeout.getMessage() + "; " + d.client(this::chainState));
        }
        d.leftClick();
        expectBuildingState(BuilderChain.BuildingState.BREAKING, "after the first left click");
    }

    /** Client-thread snapshot of what the BuilderChain works with, for failure messages. */
    private String chainState() {
        BuilderChain chain = SophisticatedBuildingClient.BUILDER_CHAIN;
        BlockEntry start = chain.getStartPosForPlacing();
        return "BuilderChain start " + (start == null ? "none" : start.blockPos.toShortString()) + ", state " + chain.getBuildingState()
                + ", abilities " + chain.getAbilitiesState() + ", mode " + SophisticatedBuildingClient.BUILD_MODES.getBuildMode()
                + ", game mode " + d.mc.gameMode.getPlayerMode() + ", power level " + AttachmentHandler.getPowerLevel(d.mc.player)
                + ", player at " + d.mc.player.position() + " yaw " + d.mc.player.getYRot() + " pitch " + d.mc.player.getXRot()
                + ", crosshair " + (d.mc.hitResult == null ? "none" : d.mc.hitResult.getType() + " " + d.mc.hitResult.getLocation())
                + ", screen " + (d.mc.screen == null ? "none" : d.mc.screen.getClass().getSimpleName());
    }

    /** Turns to the second target and returns the preview the BuilderChain computed from it. */
    private Preview aimSecond(Vec3 aim, int expectedBlocks) {
        d.lookAt(aim);
        try {
            d.waitUntil("a preview of " + expectedBlocks + " blocks", 20, () -> {
                d.aimNow(aim);
                return SophisticatedBuildingClient.BUILDER_CHAIN.getBlocks().size() == expectedBlocks;
            });
        } catch (AssertionError timeout) {
            // The caller's assertion reports the preview it got, with the BuilderChain state
        }
        d.waitTicks(1);
        return preview();
    }

    /** A whole Line build from start along +x: first click, aim, second click. Returns the preview before the second click. */
    private Preview buildLine(BlockPos start, int length) {
        firstClick(start, new Vec3(start.getX() + 2.5, groundY, start.getZ() + 3.5), groundTop(start));
        Preview preview = aimSecond(groundTop(start.east(length - 1)), length);
        d.rightClick();
        return preview;
    }

    private Preview preview() {
        return d.client(() -> {
            BuilderChain chain = SophisticatedBuildingClient.BUILDER_CHAIN;
            List<BlockPos> valid = new ArrayList<>();
            List<BlockPos> invalid = new ArrayList<>();
            for (BlockEntry entry : chain.getBlocks()) {
                (entry.invalid ? invalid : valid).add(entry.blockPos);
            }
            return new Preview(valid, invalid, chain.getBuildingState(), chainState());
        });
    }

    private record Preview(List<BlockPos> valid, List<BlockPos> invalid, BuilderChain.BuildingState state, String context) {
        void expectExactly(List<BlockPos> positions, int validCount, int invalidCount) {
            List<BlockPos> all = new ArrayList<>(valid);
            all.addAll(invalid);
            if (all.size() != positions.size() || !all.containsAll(positions)) {
                throw new AssertionError("Preview should cover " + positions.size() + " blocks " + shortList(positions) + " but covers "
                        + all.size() + " " + shortList(all) + "; " + context);
            }
            if (valid.size() != validCount || invalid.size() != invalidCount) {
                throw new AssertionError("Preview should have " + validCount + " valid / " + invalidCount + " invalid blocks, has "
                        + valid.size() + " / " + invalid.size() + "; " + context);
            }
        }
    }

    private void expectBuildingState(BuilderChain.BuildingState expected, String when) {
        BuilderChain.BuildingState state = d.client(SophisticatedBuildingClient.BUILDER_CHAIN::getBuildingState);
        if (state != expected) throw new AssertionError("BuilderChain should be " + expected + " " + when + " but is " + state + "; " + d.client(this::chainState));
    }

    //endregion

    //region Helpers

    /** Clears the inventory and fills the hotbar from slot 0, then selects slot 0. */
    private void giveHotbar(ItemStack... stacks) {
        d.server(server -> {
            ServerPlayer player = ClientDriver.serverPlayer(server);
            player.getInventory().clearContent();
            for (int i = 0; i < stacks.length; i++) {
                player.getInventory().setItem(i, stacks[i].copy());
            }
            return null;
        });
        d.selectHotbarSlot(0);
        Item first = stacks[0].getItem();
        d.waitUntil("the client to hold " + first, 40, () -> d.mc.player.getMainHandItem().is(first));
    }

    private int serverCount(Item item) {
        return d.server(server -> ClientDriver.count(ClientDriver.serverPlayer(server).getInventory(), item));
    }

    private void placeDirectly(List<BlockPos> positions, Block block) {
        d.serverRun(server -> positions.forEach(pos -> server.overworld().setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL)));
        d.waitUntil("the client to see the prepared blocks", 40, () -> positions.stream().allMatch(pos -> d.mc.level.getBlockState(pos).is(block)));
    }

    private void waitForBlocks(List<BlockPos> positions, Block block, String what) {
        waitForBlocks(positions, block, what, 120);
    }

    private void waitForBlocks(List<BlockPos> positions, Block block, String what, int timeoutTicks) {
        try {
            d.waitUntilServer(what, timeoutTicks, server -> positions.stream().allMatch(pos -> server.overworld().getBlockState(pos).is(block)));
        } catch (AssertionError timeout) {
            List<String> actual = d.server(server -> positions.stream()
                    .map(pos -> pos.toShortString() + "=" + server.overworld().getBlockState(pos).getBlock().getName().getString())
                    .toList());
            throw new AssertionError(timeout.getMessage() + "; world: " + actual);
        }
    }

    private List<String> worldState(List<BlockPos> positions) {
        return d.server(server -> positions.stream()
                .map(pos -> pos.toShortString() + "=" + server.overworld().getBlockState(pos).getBlock().getName().getString())
                .toList());
    }

    private boolean allAre(List<BlockPos> positions, Block block) {
        return d.server(server -> positions.stream().allMatch(pos -> server.overworld().getBlockState(pos).is(block)));
    }

    /** The positions hold the block and the given neighbours are still air. */
    private void expectOnly(List<BlockPos> positions, Block block, BlockPos... mustStayAir) {
        if (!allAre(positions, block)) throw new AssertionError("Not every expected position holds " + block);
        for (BlockPos pos : mustStayAir) {
            if (!d.serverBlock(pos).isAir()) throw new AssertionError("Unexpected block at " + pos.toShortString() + ": " + d.serverBlock(pos));
        }
    }

    private static int countBlocks(ServerLevel level, List<BlockPos> positions, Block block) {
        int count = 0;
        for (BlockPos pos : positions) {
            if (level.getBlockState(pos).is(block)) count++;
        }
        return count;
    }

    /** Positions start, start+x, ... (count blocks along +x). */
    private static List<BlockPos> row(BlockPos start, int count) {
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(start.east(i));
        return list;
    }

    /** The center of the top face of the floor block under pos (aiming there targets pos for placing). */
    private Vec3 groundTop(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, groundY, pos.getZ() + 0.5);
    }

    private static void expectEquals(String what, Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError(what + ": expected " + expected + " but was " + actual);
    }

    private static String shortList(List<BlockPos> positions) {
        return positions.stream().map(BlockPos::toShortString).toList().toString();
    }

    private static AbstractWidget widget(Object screen, String fieldName) {
        try {
            Field field = screen.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (AbstractWidget) field.get(screen);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("The screen has no widget field " + fieldName, e);
        }
    }

    //endregion
}
