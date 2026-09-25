package sophisticated.building.smoketest.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
import sophisticated.building.AllIcons;
import sophisticated.building.ClientConfig;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.buildmodifier.Array;
import sophisticated.building.buildmodifier.BaseModifier;
import sophisticated.building.client.ClientBuildingUpgradeState;
import sophisticated.building.config.ConfigValue;
import sophisticated.building.config.NumberConfigValue;
import sophisticated.building.gui.BagTitle;
import sophisticated.building.gui.DiamondRandomizerBagScreen;
import sophisticated.building.gui.GoldenRandomizerBagScreen;
import sophisticated.building.gui.OmegaRandomizerBagScreen;
import sophisticated.building.gui.RandomizerBagScreen;
import sophisticated.building.gui.SliderValues;
import sophisticated.building.gui.TitleFit;
import sophisticated.building.gui.buildmode.PlayerSettingsGui;
import sophisticated.building.gui.buildmode.RadialMenu;
import sophisticated.building.gui.buildmodifier.BaseModifierEntry;
import sophisticated.building.gui.buildmodifier.ModifiersScreen;
import sophisticated.building.gui.buildmodifier.ModifiersScreenList;
import sophisticated.building.gui.elements.LabeledScrollInput;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.item.AbstractRandomizerBagItem;
import sophisticated.building.item.OmegaRandomizerBagItem;
import sophisticated.building.platform.ClientServices;
import sophisticated.building.platform.Services;
import sophisticated.building.smoketest.backpack.SmokeBackpackScreens;
import sophisticated.building.smoketest.backpack.SmokeBackpacks;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The GUI scenarios of the client smoke run: the mod's screens opened the way a player opens them (using an item, a
 * key mapping, a click), driven with real mouse clicks, wheel turns and key presses through MouseHandler and
 * KeyboardHandler, rendered (screenshots) and checked against what reached the integrated server.
 */
final class GuiScenarios {

    /** Key of the modifier settings in the per-player data (ModifierSettingsPacket.DATA_KEY). */
    private static final String MODIFIERS_DATA_KEY = SophisticatedBuilding.MODID + ":buildModifiers";
    /** Hotbar slot of the stone the bag scenarios put into a bag (the bag itself is in the selected slot 0). */
    private static final int STONE_HOTBAR_SLOT = 1;
    /** The anvil name of the renamed bag (far wider than any bag texture). */
    private static final String RENAMED_BAG = "My favourite randomizer bag for the castle walls and towers";

    private final ClientDriver d;
    private final RadialMenuDriver radial;

    GuiScenarios(ClientDriver driver, RadialMenuDriver radial) {
        this.d = driver;
        this.radial = radial;
    }

    //region Randomizer bags

    private record Bag(String name, Supplier<? extends AbstractRandomizerBagItem> item, Class<? extends AbstractContainerScreen<?>> screen) {
    }

    /** Every randomizer bag: sneak + use opens its screen, a stone put into it survives closing and reopening. */
    String randomizerBagScreens() {
        List<String> results = new ArrayList<>();
        for (Bag bag : List.of(
                new Bag("randomizer_bag", SophisticatedBuilding.RANDOMIZER_BAG_ITEM, RandomizerBagScreen.class),
                new Bag("golden_randomizer_bag", SophisticatedBuilding.GOLDEN_RANDOMIZER_BAG_ITEM, GoldenRandomizerBagScreen.class),
                new Bag("diamond_randomizer_bag", SophisticatedBuilding.DIAMOND_RANDOMIZER_BAG_ITEM, DiamondRandomizerBagScreen.class),
                new Bag("omega_randomizer_bag", SophisticatedBuilding.OMEGA_RANDOMIZER_BAG_ITEM, OmegaRandomizerBagScreen.class))) {
            try {
                results.add(bagScreen(bag));
            } catch (AssertionError | RuntimeException error) {
                throw new AssertionError(bag.name + ": " + error.getMessage(), error);
            }
        }
        try {
            results.add(renamedBagTitle());
        } catch (AssertionError | RuntimeException error) {
            throw new AssertionError("renamed bag: " + error.getMessage(), error);
        }
        return String.join("; ", results);
    }

    private String bagScreen(Bag bag) {
        Item bagItem = bag.item.get();
        hold(new ItemStack(bagItem), new ItemStack(Items.STONE, 64));

        openBag(bag);
        int stoneSlot = d.client(() -> inventorySlotIndex(menu(), STONE_HOTBAR_SLOT));
        // Pick up the stone, drop one into the bag's first slot (a template slot takes one), put the rest back
        clickSlot(stoneSlot);
        expectCarried("after picking up the stone", 64);
        clickSlot(0);
        expectCarried("after dropping one stone into bag slot 0", 63);
        if (!d.client(() -> menu().getSlot(0).getItem().is(Items.STONE))) {
            throw new AssertionError("Bag slot 0 shows " + d.client(() -> menu().getSlot(0).getItem()) + " after the click, expected 1 stone");
        }
        clickSlot(stoneSlot);
        expectCarried("after putting the rest back", 0);
        closeScreen(bag.name + " screen");

        String stored = d.server(server -> {
            ServerPlayer player = ClientDriver.serverPlayer(server);
            ItemStack template = bagInventory(player).getStackInSlot(0);
            int stone = ClientDriver.count(player.getInventory(), Items.STONE);
            if (!template.is(Items.STONE) || template.getCount() != 1 || stone != 63) {
                throw new AssertionError("After closing, the server bag's slot 0 holds " + template + " and the player " + stone
                        + " stone; expected 1 stone in the bag and 63 in the inventory");
            }
            return template.getCount() + " " + template.getHoverName().getString();
        });

        openBag(bag);
        d.waitUntil("the reopened bag to show the stone in slot 0", 40, () -> menu().getSlot(0).getItem().is(Items.STONE));
        String title = titleFits(bag.name);
        d.pointAt(4, 4);
        d.screenshot(bag.name);
        String weights = bagItem instanceof OmegaRandomizerBagItem omega ? "; " + omegaWeights(omega) : "";
        closeScreen(bag.name + " screen");
        return bag.name + ": " + bag.screen.getSimpleName() + " opened by sneak + use, " + stored + " template kept after closing (server)"
                + " and shown after reopening, " + title + weights;
    }

    /**
     * The title of the open bag screen stays inside its texture, computed with the screen's own {@link BagTitle} layout.
     */
    private String titleFits(String name) {
        return d.client(() -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) d.mc.screen;
            int imageWidth = widget(screen, "imageWidth", Integer.class);
            TitleFit fit = BagTitle.fit(d.mc.font, screen.getTitle(), imageWidth);
            float drawn = fit.drawnWidth(d.mc.font::width);
            int available = BagTitle.availableWidth(imageWidth);
            if (drawn > available) {
                throw new AssertionError(name + " title '" + fit.text() + "' is " + drawn + " px wide, the texture leaves " + available);
            }
            return String.format(java.util.Locale.ROOT, "title %s at scale %.2f (%.0f of %d px)", fit.truncated() ? "cut" : "whole", fit.scale(), drawn, available);
        });
    }

    /**
     * A bag renamed in an anvil to a long name: the screen shows that name, cut off with "..." at the minimum scale so it
     * stays inside the texture, and the full name as a tooltip while the mouse is over the title.
     */
    private String renamedBagTitle() {
        Bag bag = new Bag("renamed_randomizer_bag", SophisticatedBuilding.RANDOMIZER_BAG_ITEM, RandomizerBagScreen.class);
        ItemStack renamed = new ItemStack(bag.item.get());
        renamed.setHoverName(new TextComponent(RENAMED_BAG));
        hold(renamed, new ItemStack(Items.STONE, 64));
        openBag(bag);
        String shown = d.client(() -> d.mc.screen.getTitle().getString());
        if (!shown.equals(RENAMED_BAG)) throw new AssertionError("The renamed bag's screen is titled '" + shown + "', expected its name");
        String title = titleFits(bag.name);
        TitleFit fit = d.client(() -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) d.mc.screen;
            return BagTitle.fit(d.mc.font, screen.getTitle(), widget(screen, "imageWidth", Integer.class));
        });
        if (!fit.truncated()) throw new AssertionError("A " + RENAMED_BAG.length() + " character name was expected to be cut off: " + fit);
        Point titlePoint = d.client(() -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) d.mc.screen;
            return new Point(widget(screen, "leftPos", Integer.class) + BagTitle.X + 20, widget(screen, "topPos", Integer.class) + BagTitle.Y + 3);
        });
        d.pointAt(titlePoint.x(), titlePoint.y());
        boolean hovered = d.client(() -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) d.mc.screen;
            return BagTitle.isHovered(widget(screen, "leftPos", Integer.class), widget(screen, "topPos", Integer.class),
                    widget(screen, "imageWidth", Integer.class), d.mc.font, titlePoint.x(), titlePoint.y());
        });
        if (!hovered) throw new AssertionError("The pointer at " + titlePoint + " is not over the title");
        d.waitTicks(2);
        // The pointer rests on the title: the screenshot shows the cut title and the full name as a tooltip
        d.screenshot("renamed_bag_title");
        closeScreen(bag.name + " screen");
        return "renamed bag: '" + fit.text() + "' shown for its " + RENAMED_BAG.length() + " character name, " + title + ", full name as tooltip on hover";
    }

    /** The Omega bag's weights UI: the wheel over a slot raises its weight, Reset sets it back; both reach the server's bag. */
    private String omegaWeights(OmegaRandomizerBagItem omega) {
        int before = serverWeight(omega);
        Point slot = d.client(() -> slotCenter(0));
        d.scrollAt(slot.x(), slot.y(), 1.0);
        d.waitUntilServer("the server bag to store weight 2 for slot 0 (was " + before + ")", 40, server -> weight(server, omega) == 2);
        // The pointer stays on the slot: the screenshot shows the weight badge and its tooltip
        d.screenshot("omega_randomizer_bag_weights");
        Point reset = d.client(() -> {
            for (GuiEventListener child : d.mc.screen.children()) {
                if (child instanceof Button button && button.getMessage().getString().equals("Reset")) {
                    return center(button);
                }
            }
            throw new AssertionError("The Omega bag screen has no Reset button");
        });
        d.clickAt(reset.x(), reset.y());
        d.waitUntilServer("Reset to set the server bag's slot 0 weight back to 1", 40, server -> weight(server, omega) == 1);
        return "weights UI: mouse wheel over slot 0 set weight " + before + " -> 2, Reset -> 1 (server bag data)";
    }

    private int serverWeight(OmegaRandomizerBagItem omega) {
        return d.server(server -> weight(server, omega));
    }

    private static int weight(MinecraftServer server, OmegaRandomizerBagItem omega) {
        return omega.getSlotWeight(ClientDriver.serverPlayer(server).getMainHandItem(), 0);
    }

    private static IItemHandler bagInventory(ServerPlayer player) {
        ItemStack bag = player.getMainHandItem();
        if (!(bag.getItem() instanceof AbstractRandomizerBagItem bagItem)) throw new AssertionError("The player no longer holds the bag: " + bag);
        return bagItem.getBagInventory(bag);
    }

    /** Sneak + use with the bag in hand while looking at the sky, as the bag's tooltip tells the player. */
    private void openBag(Bag bag) {
        lookAtSky();
        KeyMapping sneak = d.mc.options.keyShift;
        InputConstants.Key key = d.client(() -> ClientServices.CLIENT.getBoundKey(sneak));
        try {
            d.clientRun(() -> KeyMapping.set(key, true));
            d.waitUntilServer("the server to see the player sneaking", 40, server -> ClientDriver.serverPlayer(server).isShiftKeyDown());
            d.rightClick();
            d.waitUntil("the " + bag.name + " screen to open", 60, () -> bag.screen.isInstance(d.mc.screen));
        } finally {
            d.clientRun(() -> KeyMapping.set(key, false));
        }
        d.waitUntil("the bag menu's contents to arrive", 40, () -> menu().containerId != 0 && !menu().getSlot(inventorySlotIndex(menu(), 0)).getItem().isEmpty());
        d.waitTicks(2);
    }

    //endregion

    //region Radial menu icons

    /**
     * Every icon the radial menu draws (build modes and every action/option button) has pixels in the icon atlas, and
     * the Terrain Mound options (noise and terrain type, the last atlas cells to be drawn) show in the menu: Terrain
     * Mound selected in the radial menu, the menu reopened, its option buttons hovered (screenshot), the active type
     * clicked again and the previous build mode restored. With the menu open, every build mode's side buttons (actions
     * and options) lie fully inside the window, clear of the ring, without overlapping each other.
     */
    String radialOptionIcons() {
        String atlas = d.client(() -> {
            List<String> empty = new ArrayList<>();
            int drawn = 0;
            try (Resource resource = d.mc.getResourceManager().getResource(AllIcons.ICON_ATLAS); InputStream in = resource.getInputStream();
                 NativeImage image = NativeImage.read(in)) {
                for (BuildModeEnum mode : BuildModeEnum.values()) {
                    if (iconPixels(image, mode.icon) == 0) empty.add("mode " + mode); else drawn++;
                }
                for (ModeOptions.ActionEnum action : ModeOptions.ActionEnum.values()) {
                    if (iconPixels(image, action.icon) == 0) empty.add("action " + action); else drawn++;
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read the icon atlas " + AllIcons.ICON_ATLAS, e);
            }
            if (!empty.isEmpty()) throw new AssertionError("Radial menu icons with an empty atlas cell: " + empty);
            return drawn + " icons (" + BuildModeEnum.values().length + " modes, " + ModeOptions.ActionEnum.values().length + " actions) have pixels";
        });

        BuildModeEnum before = d.client(SophisticatedBuildingClient.BUILD_MODES::getBuildMode);
        ModeOptions.ActionEnum typeBefore = d.client(ModeOptions::getTerrainType);
        String layout = "";
        radial.select(BuildModeEnum.TERRAIN_MOUND);
        try {
            radial.open();
            radial.hoverButton(ModeOptions.ActionEnum.TERRAIN_NOISE_ON);
            for (ModeOptions.ActionEnum type : ModeOptions.OptionEnum.TERRAIN_TYPE.actions) {
                radial.hoverButton(type);
            }
            radial.hoverNothing();
            d.screenshot("radial_terrain_options");
            radial.hoverButton(ModeOptions.ActionEnum.TERRAIN_MOUNTAIN);
            d.screenshot("radial_terrain_mountain");
            radial.hoverNothing();
            String inside = sideButtonsInsideForEveryMode();
            d.clientRun(() -> SophisticatedBuildingClient.BUILD_MODES.setBuildMode(BuildModeEnum.TERRAIN_MOUND));
            // Click the type that was active, so the option stays as it was
            radial.hoverButton(typeBefore);
            radial.clickAndRelease();
            layout = inside;
        } finally {
            if (d.client(() -> d.mc.screen instanceof RadialMenu)) radial.clickAndRelease();
            radial.select(before);
        }
        return atlas + "; Terrain Mound shows its noise and terrain type option buttons (all 7 hovered in the radial menu),"
                + " build mode restored to " + before + "; " + layout;
    }

    /**
     * With the radial menu open: switches through every build mode (the menu shows that mode's options) and checks the
     * side buttons as the menu drew them. Restores nothing (the caller sets the mode it needs next).
     */
    private String sideButtonsInsideForEveryMode() {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        String window = d.client(() -> RadialMenu.instance.width + "x" + RadialMenu.instance.height + " GUI (scale " + d.mc.getWindow().getGuiScale() + ")");
        for (BuildModeEnum mode : BuildModeEnum.values()) {
            d.clientRun(() -> SophisticatedBuildingClient.BUILD_MODES.setBuildMode(mode));
            int expected = 0;
            for (ModeOptions.OptionEnum option : mode.options) expected += option.actions.length;
            int options = expected;
            d.waitUntil("the radial menu to draw the " + mode + " options", 40, () -> RadialMenu.instance.sideButtons().stream()
                    .filter(b -> List.of(mode.options).stream().anyMatch(o -> List.of(o.actions).contains(b.action()))).count() >= options);
            d.waitTicks(1);
            checked += d.client(() -> {
                RadialMenu menu = RadialMenu.instance;
                List<RadialMenu.SideButton> buttons = menu.sideButtons();
                double ringClearance = widget(menu, "ringOuterEdge", Double.class);
                for (RadialMenu.SideButton b : buttons) {
                    if (b.left() < 0 || b.top() < 0 || b.right() > menu.width || b.bottom() > menu.height) {
                        problems.add(mode + " " + b.action() + " outside the " + menu.width + "x" + menu.height + " window: " + b);
                    }
                    double innerEdge = Math.min(Math.abs(b.left() - menu.width / 2.0), Math.abs(b.right() - menu.width / 2.0));
                    if (innerEdge < ringClearance) problems.add(mode + " " + b.action() + " reaches into the ring: " + b);
                    for (RadialMenu.SideButton o : buttons) {
                        if (o != b && b.left() < o.right() && o.left() < b.right() && b.top() < o.bottom() && o.top() < b.bottom()) {
                            problems.add(mode + " " + b.action() + " overlaps " + o.action());
                        }
                    }
                }
                return buttons.size();
            });
        }
        if (!problems.isEmpty()) throw new AssertionError("Radial menu side buttons: " + problems);
        return "all " + checked + " side buttons of the " + BuildModeEnum.values().length + " build modes inside the " + window
                + ", clear of the ring, no overlaps";
    }

    /** Non-transparent pixels of an icon's 16x16 atlas cell (the cell position is private to AllIcons). */
    private static int iconPixels(NativeImage atlas, AllIcons icon) {
        int x0 = widget(icon, "iconX", Integer.class);
        int y0 = widget(icon, "iconY", Integer.class);
        int count = 0;
        for (int y = y0; y < y0 + 16; y++) {
            for (int x = x0; x < x0 + 16; x++) {
                if ((atlas.getPixelRGBA(x, y) >>> 24) != 0) count++;
            }
        }
        return count;
    }

    //endregion

    //region Player settings and modifier widgets

    /**
     * PlayerSettingsGui, the client config editor, the way a player uses it: opened with the radial menu's player
     * settings button, one switch flipped with a click and one slider dragged, Done; the loader's client config holds
     * the new values in memory and in its file. Then opened with its key (bound for the test), "Reset to defaults",
     * closed with the same key: memory and file are back at the defaults.
     */
    String playerSettingsGui() {
        ConfigValue<Boolean> onlyWhenBuilding = ClientConfig.visuals.onlyShowBlockPreviewsWhenBuilding;
        NumberConfigValue<Integer> appear = ClientConfig.visuals.appearAnimationLength;
        boolean flagBefore = d.client(onlyWhenBuilding::get);
        int appearBefore = d.client(appear::get);
        int appearTarget = appearBefore == 20 ? 40 : 20;
        KeyMapping settingsKey = ClientEvents.keyBindings[ClientEvents.PLAYER_SETTINGS_KEY];
        try {
            // Radial menu: hold its key, point at the player settings button (above the modifier settings), click
            radial.open();
            radial.hoverButton(ModeOptions.ActionEnum.OPEN_PLAYER_SETTINGS);
            d.screenshot("radial_player_settings");
            radial.clickAndRelease();
            PlayerSettingsGui screen = waitForSettingsScreen("the radial menu's player settings button");

            PlayerSettingsGui.SettingEntry<?> flagRow = settingRow(screen, "onlyShowBlockPreviewsWhenBuilding");
            Point toggle = d.client(() -> center(flagRow.widget()));
            d.clickAt(toggle.x(), toggle.y());
            d.waitUntil("the switch click to flip onlyShowBlockPreviewsWhenBuilding", 20, () -> onlyWhenBuilding.get() != flagBefore);

            PlayerSettingsGui.SettingEntry<?> appearRow = settingRow(screen, "appearAnimationLength");
            SliderValues values = ((PlayerSettingsGui.NumberEntry<?>) appearRow).values;
            double[] drag = d.client(() -> {
                AbstractWidget slider = appearRow.widget();
                double track = slider.getWidth() - 8;
                double from = slider.x + 4 + values.positionOf(appear.get()) * track;
                double to = slider.x + 4 + values.positionOf(appearTarget) * track;
                double y = slider.y + slider.getHeight() / 2.0;
                return new double[]{from, to, y};
            });
            d.dragTo(drag[0], drag[2], drag[1], drag[2]);
            d.waitUntil("dragging the Appear Animation slider to set " + appearTarget + " ticks", 20, () -> appear.get() == appearTarget);
            d.waitTicks(2);
            d.screenshot("player_settings");

            Point done = d.client(() -> center(widget(d.mc.screen, "doneButton", AbstractWidget.class)));
            d.clickAt(done.x(), done.y());
            d.waitUntil("Done to close the player settings", 40, () -> d.mc.screen == null);
            String file = expectOnDisk(String.valueOf(!flagBefore), String.valueOf(appearTarget));

            // The key (unbound by default; bound for the test) opens the screen, Reset restores the defaults, the key closes it
            bindKey(settingsKey, GLFW.GLFW_KEY_F7);
            d.pressKey(GLFW.GLFW_KEY_F7);
            PlayerSettingsGui again = waitForSettingsScreen("the Open Player Settings key");
            if (!d.client(() -> settingRow(again, "appearAnimationLength").widget().getMessage().getString()).startsWith(appearTarget + " ")) {
                throw new AssertionError("The reopened screen does not show the saved Appear Animation value " + appearTarget);
            }
            Point reset = d.client(() -> center(widget(d.mc.screen, "resetButton", AbstractWidget.class)));
            d.clickAt(reset.x(), reset.y());
            d.waitUntil("Reset to defaults to restore both values", 20,
                    () -> onlyWhenBuilding.get().equals(onlyWhenBuilding.getDefault()) && appear.get().equals(appear.getDefault()));
            d.pressKey(GLFW.GLFW_KEY_F7);
            d.waitUntil("the Open Player Settings key to close the screen again", 40, () -> d.mc.screen == null);
            expectOnDisk(String.valueOf(onlyWhenBuilding.getDefault()), String.valueOf(appear.getDefault()));

            return "Radial menu button opened PlayerSettingsGui; a click switched onlyShowBlockPreviewsWhenBuilding " + flagBefore + " -> "
                    + !flagBefore + ", a drag set the Appear Animation slider " + appearBefore + " -> " + appearTarget + " ticks, Done saved"
                    + " both (in memory and in " + file + "); the Open Player Settings key reopened it, Reset to defaults restored "
                    + onlyWhenBuilding.getDefault() + " / " + appear.getDefault() + " and the key closed and saved it";
        } finally {
            d.clientRun(() -> {
                if (d.mc.screen != null) d.mc.setScreen(null);
                bindKeyNow(settingsKey, InputConstants.UNKNOWN);
                if (!onlyWhenBuilding.get().equals(flagBefore) || appear.get() != appearBefore) {
                    onlyWhenBuilding.set(flagBefore);
                    appear.set(appearBefore);
                    ClientConfig.save();
                }
            });
        }
    }

    private PlayerSettingsGui waitForSettingsScreen(String how) {
        d.waitUntil("PlayerSettingsGui to open through " + how, 40, () -> d.mc.screen instanceof PlayerSettingsGui);
        // The list lays its rows out while rendering
        d.waitUntil("the settings rows to be laid out", 40, () -> d.mc.screen instanceof PlayerSettingsGui screen
                && settingRow(screen, "appearAnimationLength").widget().y > 0);
        d.waitTicks(2);
        return (PlayerSettingsGui) d.client(() -> d.mc.screen);
    }

    private static PlayerSettingsGui.SettingEntry<?> settingRow(PlayerSettingsGui screen, String key) {
        for (PlayerSettingsGui.SettingEntry<?> entry : screen.settingEntries()) {
            if (entry.key.equals(key)) return entry;
        }
        throw new AssertionError("PlayerSettingsGui has no row for " + key);
    }

    private void bindKey(KeyMapping mapping, int glfwKey) {
        d.clientRun(() -> bindKeyNow(mapping, InputConstants.Type.KEYSYM.getOrCreate(glfwKey)));
    }

    private static void bindKeyNow(KeyMapping mapping, InputConstants.Key key) {
        mapping.setKey(key);
        KeyMapping.resetMapping();
    }

    /**
     * Waits until the client config file holds the two values (onlyShowBlockPreviewsWhenBuilding, appearAnimationLength):
     * {@code config/sophisticatedbuilding-client.json} on Fabric, {@code config/sophisticatedbuilding-client.toml} on
     * NeoForge and Forge. Returns the file name.
     */
    private String expectOnDisk(String flag, String appear) {
        Path config = d.mc.gameDirectory.toPath().resolve("config");
        Path json = config.resolve("sophisticatedbuilding-client.json");
        Path toml = config.resolve("sophisticatedbuilding-client.toml");
        d.waitUntilRealtime("the client config file to hold onlyShowBlockPreviewsWhenBuilding=" + flag + " and appearAnimationLength=" + appear, 10,
                () -> flag.equals(diskValue(json, toml, "onlyShowBlockPreviewsWhenBuilding")) && appear.equals(diskValue(json, toml, "appearAnimationLength")));
        return (Files.exists(json) ? json : toml).getFileName().toString();
    }

    private static String diskValue(Path json, Path toml, String key) {
        try {
            if (Files.exists(json)) {
                JsonObject visuals = new JsonParser().parse(Files.readString(json)).getAsJsonObject().getAsJsonObject("Visuals");
                return visuals.has(key) ? visuals.get(key).getAsString() : null;
            }
            if (Files.exists(toml)) {
                Matcher matcher = Pattern.compile("(?m)^\\s*" + key + "\\s*=\\s*([^\\s#]+)").matcher(Files.readString(toml));
                return matcher.find() ? matcher.group(1) : null;
            }
            return null;
        } catch (IOException | RuntimeException e) {
            // Being rewritten right now; the caller polls again
            return null;
        }
    }

    /**
     * The modifier screen's entry widgets: "Add Array", the entry's enable checkbox and its Count number input (mouse
     * wheel), the close button; closing sends the settings to the server, which stores them in the player's data.
     */
    String modifierEntryWidgets() {
        d.clientRun(ClientEvents::openModifierSettings);
        d.waitUntil("the modifier screen to open", 40, () -> d.mc.screen instanceof ModifiersScreen);
        d.waitTicks(3);
        int before = d.client(() -> SophisticatedBuildingClient.BUILD_MODIFIERS.getModifierSettingsList().size());
        Point add = d.client(() -> center(widget(d.mc.screen, "addArrayButton", AbstractWidget.class)));
        d.clickAt(add.x(), add.y());
        Array array = d.client(() -> {
            List<BaseModifier> list = SophisticatedBuildingClient.BUILD_MODIFIERS.getModifierSettingsList();
            if (list.size() != before + 1 || !(list.get(list.size() - 1) instanceof Array added)) {
                throw new AssertionError("Clicking 'Add Array' did not add an array (modifiers: " + list + ")");
            }
            return added;
        });
        try {
            BaseModifierEntry<?> entry = d.client(() -> arrayEntry(array));
            AbstractWidget enable = d.client(() -> widget(entry, "enableButton", AbstractWidget.class));
            LabeledScrollInput count = d.client(() -> widget(entry, "countInput", LabeledScrollInput.class));
            // The list lays its entries out while rendering
            d.waitUntil("the array entry to be laid out", 40, () -> enable.x > 0 && count.y > 0);

            boolean enabledBefore = d.client(() -> array.enabled);
            Point checkbox = d.client(() -> center(enable));
            d.clickAt(checkbox.x(), checkbox.y());
            d.waitUntil("the enable checkbox to switch the array " + (enabledBefore ? "off" : "on"), 20, () -> array.enabled != enabledBefore);

            int countBefore = d.client(() -> array.count);
            Point countCenter = d.client(() -> center(count));
            // The number input reacts to the wheel only while the pointer hovers it (hover state comes from rendering)
            d.pointAt(countCenter.x(), countCenter.y());
            d.waitUntil("the Count input to be hovered", 20, count::isHovered);
            d.scrollAt(countCenter.x(), countCenter.y(), 1.0);
            d.waitUntil("the mouse wheel to raise the Count input", 20, () -> array.count == countBefore + 1);
            d.screenshot("modifier_widgets");

            Point close = d.client(() -> center(widget(d.mc.screen, "closeButton", AbstractWidget.class)));
            d.clickAt(close.x(), close.y());
            d.waitUntil("the close button to close the modifier screen", 40, () -> d.mc.screen == null);

            boolean enabledAfter = !enabledBefore;
            int countAfter = countBefore + 1;
            d.waitUntilServer("the server to store the array (enabled=" + enabledAfter + ", count=" + countAfter + ")", 40, server -> {
                CompoundTag stored = storedLastModifier(server);
                return stored != null && stored.getString("type").equals("Array") && stored.getBoolean("enabled") == enabledAfter
                        && stored.getInt("count") == countAfter;
            });
            return "Modifier screen: 'Add Array' added an array, its enable checkbox switched it " + (enabledAfter ? "on" : "off")
                    + ", the mouse wheel on Count set " + countBefore + " -> " + countAfter + ", the close button closed the screen and the"
                    + " server stored the array with these values (player data " + MODIFIERS_DATA_KEY + ")";
        } finally {
            d.clientRun(() -> {
                SophisticatedBuildingClient.BUILD_MODIFIERS.removeModifierSettings(array);
                SophisticatedBuildingClient.BUILD_MODIFIERS.save();
            });
        }
    }

    private static CompoundTag storedLastModifier(MinecraftServer server) {
        CompoundTag data = Services.PLATFORM.getPersistentData(ClientDriver.serverPlayer(server)).getCompound(MODIFIERS_DATA_KEY);
        ListTag list = data.getList("modifierSettingsList", Tag.TAG_COMPOUND);
        return list.isEmpty() ? null : list.getCompound(list.size() - 1);
    }

    /** The entry of the modifier screen's list that edits the modifier. */
    private BaseModifierEntry<?> arrayEntry(Array array) {
        for (var entry : widget(d.mc.screen, "list", ModifiersScreenList.class).children()) {
            if (entry instanceof BaseModifierEntry<?> modifierEntry && modifierEntry.modifier == array) return modifierEntry;
        }
        throw new AssertionError("The modifier screen shows no entry for the added array");
    }

    //endregion

    //region Sophisticated Backpacks

    /**
     * The backpack screen with a Building Upgrade: use the backpack, open the upgrade's settings tab, click its enable
     * toggle; the server's upgrade is disabled and the client's upgrade state follows.
     */
    String upgradeSettingsTab(SmokeBackpacks backpacks) {
        SmokeBackpackScreens screens = SmokeBackpackScreens.find().orElseThrow(() -> new AssertionError(
                "This loader build has the Sophisticated Backpacks integration but its smoke source set registers no SmokeBackpackScreens"));
        d.server(server -> {
            ServerPlayer player = ClientDriver.serverPlayer(server);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, backpacks.createBackpack(1, true, false, List.of(new ItemStack(Items.STONE, 64))));
            return null;
        });
        d.selectHotbarSlot(0);
        d.waitUntil("the client to hold the backpack and know its enabled Building Upgrade", 100,
                () -> !d.mc.player.getMainHandItem().isEmpty() && ClientBuildingUpgradeState.hasUpgrade());

        lookAtSky();
        d.rightClick();
        d.waitUntil("the backpack screen to open", 60, () -> screens.isStorageScreen(d.mc.screen));
        d.waitTicks(3);
        String screenName = d.client(() -> d.mc.screen.getClass().getSimpleName());

        SmokeBackpackScreens.Point icon = d.client(() -> screens.buildingTabIcon(d.mc.screen));
        if (icon == null) throw new AssertionError(screenName + " shows no Building Upgrade settings tab");
        d.clickAt(icon.x(), icon.y());
        d.waitUntil("the Building Upgrade settings tab to open", 20, () -> screens.isBuildingTabOpen(d.mc.screen));
        d.waitTicks(2);

        SmokeBackpackScreens.Point toggle = d.client(() -> screens.buildingTabToggle(d.mc.screen));
        if (toggle == null) throw new AssertionError("The open Building Upgrade settings tab has no enable toggle");
        d.clickAt(toggle.x(), toggle.y());
        d.waitUntilServer("the server's Building Upgrade to be disabled", 40,
                server -> !backpacks.isBuildingUpgradeEnabled(ClientDriver.serverPlayer(server).getInventory().getItem(0)));
        d.waitTicks(2);
        d.screenshot("sb_upgrade_settings_tab");
        closeScreen("backpack screen");
        d.waitUntil("the client to see no active Building Upgrade", 100, () -> !ClientBuildingUpgradeState.hasUpgrade());
        return "Using the backpack opened " + screenName + ", the Building Upgrade tab opened on a click, its toggle disabled the upgrade"
                + " on the server (stored on the upgrade) and the client's upgrade state followed";
    }

    //endregion

    //region Helpers

    /** A point on the screen in GUI coordinates. */
    private record Point(double x, double y) {
    }

    /** Clears the inventory, fills the hotbar from slot 0 and selects slot 0. */
    private void hold(ItemStack... stacks) {
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

    /** Turns the head up until the crosshair hits nothing, so a right click uses the held item (no block, no build). */
    private void lookAtSky() {
        d.lookAt(d.client(() -> d.mc.player.getEyePosition().add(0, 20, 4)));
        d.waitUntil("the crosshair to point at the sky", 20, () -> d.mc.hitResult == null || d.mc.hitResult.getType() == HitResult.Type.MISS);
    }

    /** Escape, as a player closes a screen; for a menu the server closes it too. */
    private void closeScreen(String what) {
        d.pressKey(GLFW.GLFW_KEY_ESCAPE);
        d.waitUntil("the " + what + " to close on Escape", 40, () -> d.mc.screen == null);
        d.waitUntilServer("the server to close the " + what + "'s menu", 40, server -> {
            ServerPlayer player = ClientDriver.serverPlayer(server);
            return player.containerMenu == player.inventoryMenu;
        });
    }

    private AbstractContainerMenu menu() {
        if (!(d.mc.screen instanceof AbstractContainerScreen<?> screen)) throw new AssertionError("No container screen is open (" + d.mc.screen + ")");
        return screen.getMenu();
    }

    /** Index of the menu slot that shows the player's inventory slot. */
    private int inventorySlotIndex(AbstractContainerMenu menu, int inventorySlot) {
        for (Slot slot : menu.slots) {
            if (slot.container == d.mc.player.getInventory() && slot.getContainerSlot() == inventorySlot) return slot.index;
        }
        throw new AssertionError("The menu has no slot for inventory slot " + inventorySlot);
    }

    private void clickSlot(int index) {
        Point point = d.client(() -> slotCenter(index));
        d.clickAt(point.x(), point.y());
    }

    private void expectCarried(String when, int count) {
        ItemStack carried = d.client(() -> menu().getCarried().copy());
        if (carried.getCount() != count || count > 0 && !carried.is(Items.STONE)) {
            throw new AssertionError("The mouse carries " + carried + " " + when + ", expected " + (count == 0 ? "nothing" : count + " stone"));
        }
    }

    /** Centre of a menu slot in GUI coordinates (slot positions are relative to the container's top left corner). */
    private Point slotCenter(int index) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) d.mc.screen;
        Slot slot = screen.getMenu().getSlot(index);
        int left = widget(screen, "leftPos", Integer.class);
        int top = widget(screen, "topPos", Integer.class);
        return new Point(left + slot.x + 8, top + slot.y + 8);
    }

    private static Point center(AbstractWidget widget) {
        return new Point(widget.x + widget.getWidth() / 2.0, widget.y + widget.getHeight() / 2.0);
    }

    /** A field of the object or one of its superclasses (screens keep their widgets in protected fields). */
    private static <T> T widget(Object owner, String name, Class<T> type) {
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(owner);
                return type.cast(value);
            } catch (NoSuchFieldException ignored) {
                // declared further up
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError(owner.getClass().getSimpleName() + " has no field " + name);
    }

    //endregion
}
