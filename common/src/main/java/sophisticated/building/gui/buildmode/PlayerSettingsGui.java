package sophisticated.building.gui.buildmode;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import sophisticated.building.ClientConfig;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.client.gui.GuiGraphics;
import sophisticated.building.config.ConfigValue;
import sophisticated.building.config.NumberConfigValue;
import sophisticated.building.create.catnip.gui.UIRenderHelper;
import sophisticated.building.create.catnip.theme.Color;
import sophisticated.building.gui.SliderValues;
import sophisticated.building.platform.ClientServices;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleFunction;

/**
 * The player's own settings: an editor of the client config ({@link ClientConfig} Visuals and Performance). Toggles
 * for the switches, sliders with the config's ranges for the numbers; every change applies at once, "Done" (or
 * Escape, or the screen's key) writes the loader's client config file, "Reset to defaults" restores every value.
 * Opened from the radial menu ({@code OPEN_PLAYER_SETTINGS}) and the "Open player settings" key.
 * <p>
 * Minecraft 1.16.3: no widget tooltips, no list background switch; the screen draws the hovered setting's tooltip
 * itself and the list draws its own translucent background (like {@code ModifiersScreenList}).
 */
@ParametersAreNonnullByDefault
public class PlayerSettingsGui extends Screen {

	private static final String LANG = "sophisticatedbuilding.player_settings.";
	private static final int ROW_HEIGHT = 24;
	private static final int TOP = 32;
	private static final int BOTTOM = 36;
	private static final int MAX_ROW_WIDTH = 320;
	private static final int TOOLTIP_WIDTH = 250;

	protected SettingsList list;
	protected Button resetButton;
	protected Button doneButton;
	private final List<SettingEntry<?>> entries = new ArrayList<>();
	private boolean changed;
	/** The tooltip of the setting under the mouse this frame (1.16.3 widgets have no tooltips of their own). */
	@Nullable
	private Component hoveredTooltip;

	public PlayerSettingsGui() {
		super(new TranslatableComponent("sophisticatedbuilding.screen.player_settings"));
	}

	@Override
	protected void init() {
		double scroll = list != null ? list.getScrollAmount() : 0;
		entries.clear();
		list = new SettingsList(minecraft, width, height - TOP - BOTTOM, TOP, ROW_HEIGHT);

		ClientConfig.Visuals visuals = ClientConfig.visuals;
		ClientConfig.Performance performance = ClientConfig.performance;
		list.addHeader(new TranslatableComponent(LANG + "visuals"));
		add(new BooleanEntry("showBlockPreviews", visuals.showBlockPreviews));
		add(new BooleanEntry("onlyShowBlockPreviewsWhenBuilding", visuals.onlyShowBlockPreviewsWhenBuilding));
		add(new BooleanEntry("showMiniBlockPreview", visuals.showMiniBlockPreview));
		add(new IntEntry("maxBlockPreviews", visuals.maxBlockPreviews, 64, 3,
				value -> value == 0 ? new TranslatableComponent(LANG + "value.outline_only") : blocks(value)));
		add(new IntEntry("appearAnimationLength", visuals.appearAnimationLength, 1, 1, PlayerSettingsGui::ticks));
		add(new IntEntry("breakAnimationLength", visuals.breakAnimationLength, 1, 1, PlayerSettingsGui::ticks));
		add(new DoubleEntry("previewScale", visuals.previewScale, 0.05,
				value -> new TranslatableComponent(LANG + "value.percent", Math.round(value * 100))));
		list.addHeader(new TranslatableComponent(LANG + "performance"));
		add(new IntEntry("previewRenderDistance", performance.previewRenderDistance, 8, 1, PlayerSettingsGui::blocks));
		add(new BooleanEntry("enableUpdateThrottling", performance.enableUpdateThrottling));
		add(new IntEntry("maxMiniBlockPreviews", performance.maxMiniBlockPreviews, 64, 3,
				value -> value == 0 ? new TranslatableComponent(LANG + "value.no_limit") : blocks(value)));
		addWidget(list);
		list.setScrollAmount(scroll);

		int buttonWidth = Math.min(150, (width - 30) / 2);
		int buttonY = height - BOTTOM + 8;
		resetButton = addButton(new Button(width / 2 - buttonWidth - 5, buttonY, buttonWidth, 20,
				new TranslatableComponent(LANG + "reset"), button -> resetToDefaults()));
		doneButton = addButton(new Button(width / 2 + 5, buttonY, buttonWidth, 20, CommonComponents.GUI_DONE,
				button -> onClose()));
	}

	private void add(SettingEntry<?> entry) {
		entries.add(entry);
		list.addSetting(entry);
	}

	private static Component blocks(double value) {
		return new TranslatableComponent(LANG + "value.blocks", (int) value);
	}

	private static Component ticks(double value) {
		int ticks = (int) value;
		if (ticks == 0) return CommonComponents.OPTION_OFF;
		return new TranslatableComponent(LANG + "value.ticks", ticks, String.format("%.2f", ticks / 20.0));
	}

	/** Sets every value back to its config default (applied at once, saved on Done). */
	public void resetToDefaults() {
		for (SettingEntry<?> entry : entries) {
			entry.reset();
		}
		onChanged();
	}

	private void onChanged() {
		changed = true;
		SophisticatedBuildingClient.BLOCK_PREVIEWS.onConfigChanged();
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		hoveredTooltip = null;
		// Before 1.20.2 Screen#render draws no background
		renderBackground(poseStack);
		list.render(poseStack, mouseX, mouseY, partialTicks);
		super.render(poseStack, mouseX, mouseY, partialTicks);
		new GuiGraphics(poseStack).drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
		if (resetButton.isHovered()) {
			hoveredTooltip = new TranslatableComponent(LANG + "reset.tooltip");
		}
		if (hoveredTooltip != null) {
			renderTooltip(poseStack, font.split(hoveredTooltip, TOOLTIP_WIDTH), mouseX, mouseY);
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// The screen's own key closes it again, like the modifier settings key
		if (keyCode != GLFW.GLFW_KEY_UNKNOWN && ClientServices.CLIENT.matchesKey(ClientEvents.keyBindings[ClientEvents.PLAYER_SETTINGS_KEY], keyCode, scanCode)) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	/** Called whenever the screen goes away (Done, Escape, its key, another screen): writes the client config file. */
	@Override
	public void removed() {
		super.removed();
		if (changed) {
			changed = false;
			ClientConfig.save();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	//region Rows

	/** The scrolling list of setting rows (same frame as the modifier settings list). */
	protected class SettingsList extends ContainerObjectSelectionList<Row> {

		SettingsList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, height, y, y + height, itemHeight);
		}

		void addHeader(Component text) {
			addEntry(new HeaderRow(text));
		}

		void addSetting(SettingEntry<?> entry) {
			addEntry(entry);
		}

		@Override
		public int getRowWidth() {
			return Math.min(width - 40, MAX_ROW_WIDTH);
		}

		@Override
		protected int getScrollbarPosition() {
			return getRowLeft() + getRowWidth() + 6;
		}

		/**
		 * Minecraft 1.16.3 lists always draw the dirt background and bands (no setRenderBackground before 1.16.4), so the
		 * rows, the scroll bar and the decorations are drawn here over a translucent dark background instead.
		 */
		@Override
		public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
			GuiGraphics guiGraphics = new GuiGraphics(poseStack);
			Color shadow = new Color(0x60_000000);
			UIRenderHelper.angledGradient(guiGraphics, 90, x0 + width / 2, y0, width, 5, shadow, Color.TRANSPARENT_BLACK);
			UIRenderHelper.angledGradient(guiGraphics, -90, x0 + width / 2, y1, width, 5, shadow, Color.TRANSPARENT_BLACK);
			guiGraphics.fill(x0, y0, x1, y1, 0x80_000000);

			guiGraphics.enableScissor(x0, y0, x1, y1);
			renderList(poseStack, getRowLeft(), y0 + 4 - (int) getScrollAmount(), mouseX, mouseY, partialTicks);
			guiGraphics.disableScissor();
			renderScrollBar(guiGraphics);
			renderDecorations(poseStack, mouseX, mouseY);
		}

		private void renderScrollBar(GuiGraphics guiGraphics) {
			int maxScroll = Math.max(0, getMaxPosition() - (y1 - y0 - 4));
			if (maxScroll <= 0) return;
			int left = getScrollbarPosition();
			int right = left + 6;
			int thumbHeight = (int) ((float) ((y1 - y0) * (y1 - y0)) / getMaxPosition());
			thumbHeight = Math.max(32, Math.min(thumbHeight, y1 - y0 - 8));
			int thumbTop = Math.max(y0, (int) getScrollAmount() * (y1 - y0 - thumbHeight) / maxScroll + y0);
			guiGraphics.fill(left, y0, right, y1, 0xFF_000000);
			guiGraphics.fill(left, thumbTop, right, thumbTop + thumbHeight, 0xFF_808080);
			guiGraphics.fill(left, thumbTop, right - 1, thumbTop + thumbHeight - 1, 0xFF_C0C0C0);
		}
	}

	public abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
	}

	protected class HeaderRow extends Row {
		private final Component text;

		HeaderRow(Component text) {
			this.text = text;
		}

		@Override
		public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTicks) {
			new GuiGraphics(poseStack).drawCenteredString(font, text, left + width / 2, top + (height - font.lineHeight) / 2 + 1, 0xFFFFAA00);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return Collections.emptyList();
		}
	}

	/** One setting: its translated label on the left, its control on the right; label and control show the tooltip. */
	public abstract class SettingEntry<T> extends Row {
		public final String key;
		public final ConfigValue<T> config;
		protected final Component label;
		protected final Component tooltip;

		SettingEntry(String key, ConfigValue<T> config) {
			this.key = key;
			this.config = config;
			this.label = new TranslatableComponent(LANG + key);
			this.tooltip = new TranslatableComponent(LANG + key + ".tooltip");
		}

		public abstract AbstractWidget widget();

		abstract void reset();

		protected int controlWidth(int rowWidth) {
			return Math.min(150, rowWidth / 2);
		}

		@Override
		public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTicks) {
			AbstractWidget widget = widget();
			int controlWidth = controlWidth(width);
			widget.setWidth(controlWidth);
			widget.x = left + width - controlWidth;
			widget.y = top + (height - 20) / 2;
			widget.render(poseStack, mouseX, mouseY, partialTicks);

			int labelWidth = width - controlWidth - 6;
			int labelY = top + (height - font.lineHeight) / 2 + 1;
			String text = font.plainSubstrByWidth(label.getString(), labelWidth);
			new GuiGraphics(poseStack).drawString(font, text, left, labelY, 0xFFFFFF, false);
			boolean overLabel = mouseX >= left && mouseX < left + labelWidth && mouseY >= top && mouseY < top + height;
			if (overLabel || widget.isHovered()) {
				hoveredTooltip = tooltip;
			}
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return Collections.singletonList(widget());
		}
	}

	protected class BooleanEntry extends SettingEntry<Boolean> {
		private final Button button;

		BooleanEntry(String key, ConfigValue<Boolean> config) {
			super(key, config);
			button = new Button(0, 0, 150, 20, CommonComponents.optionStatus(config.get()), b -> set(!config.get()));
		}

		private void set(boolean value) {
			config.set(value);
			button.setMessage(CommonComponents.optionStatus(value));
			onChanged();
		}

		@Override
		public AbstractWidget widget() {
			return button;
		}

		@Override
		void reset() {
			config.set(config.getDefault());
			button.setMessage(CommonComponents.optionStatus(config.get()));
		}
	}

	/** A slider over a number setting's config range; values snap to the step. */
	public abstract class NumberEntry<T extends Number> extends SettingEntry<T> {
		public final SliderValues values;
		private final DoubleFunction<Component> format;
		private final Slider slider;

		NumberEntry(String key, NumberConfigValue<T> config, double step, double exponent, DoubleFunction<Component> format) {
			super(key, config);
			this.values = new SliderValues(config.getMin().doubleValue(), config.getMax().doubleValue(), step, exponent);
			this.format = format;
			this.slider = new Slider();
		}

		protected abstract T toConfig(double value);

		@Override
		public AbstractWidget widget() {
			return slider;
		}

		@Override
		void reset() {
			config.set(config.getDefault());
			slider.show(config.get().doubleValue());
		}

		protected class Slider extends AbstractSliderButton {
			Slider() {
				super(0, 0, 150, 20, TextComponent.EMPTY, values.positionOf(config.get().doubleValue()));
				updateMessage();
			}

			void show(double value) {
				this.value = values.positionOf(value);
				updateMessage();
			}

			@Override
			protected void updateMessage() {
				setMessage(format.apply(config.get().doubleValue()));
			}

			@Override
			protected void applyValue() {
				double snapped = values.valueAt(value);
				// The knob jumps to the snapped value, so integer settings move in whole steps
				value = values.positionOf(snapped);
				if (config.get().doubleValue() != snapped) {
					config.set(toConfig(snapped));
					onChanged();
				}
				updateMessage();
			}

			/** Arrow keys move one step (the vanilla slider moves by one pixel, less than a step of a wide range). */
			@Override
			public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
				if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
					double current = config.get().doubleValue();
					double next = values.snap(current + (keyCode == GLFW.GLFW_KEY_LEFT ? -values.step() : values.step()));
					value = Mth.clamp(values.positionOf(next), 0, 1);
					applyValue();
					return true;
				}
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
	}

	protected class IntEntry extends NumberEntry<Integer> {
		IntEntry(String key, NumberConfigValue<Integer> config, int step, double exponent, DoubleFunction<Component> format) {
			super(key, config, step, exponent, format);
		}

		@Override
		protected Integer toConfig(double value) {
			return (int) Math.round(value);
		}
	}

	protected class DoubleEntry extends NumberEntry<Double> {
		DoubleEntry(String key, NumberConfigValue<Double> config, double step, DoubleFunction<Component> format) {
			super(key, config, step, 1, format);
		}

		@Override
		protected Double toConfig(double value) {
			return value;
		}
	}

	//endregion

	/** The rows, for the smoke test: every setting by its config key. */
	public List<SettingEntry<?>> settingEntries() {
		return Collections.unmodifiableList(new ArrayList<>(entries));
	}
}
