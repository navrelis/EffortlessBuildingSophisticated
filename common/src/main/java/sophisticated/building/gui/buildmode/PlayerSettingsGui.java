package sophisticated.building.gui.buildmode;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import sophisticated.building.ClientConfig;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.config.ConfigValue;
import sophisticated.building.config.NumberConfigValue;
import sophisticated.building.create.catnip.gui.UIRenderHelper;
import sophisticated.building.create.catnip.theme.Color;
import sophisticated.building.gui.SliderValues;
import sophisticated.building.platform.ClientServices;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/**
 * The player's own settings: an editor of the client config ({@link ClientConfig} Visuals and Performance). Toggles
 * for the switches, sliders with the config's ranges for the numbers; every change applies at once, "Done" (or
 * Escape, or the screen's key) writes the loader's client config file, "Reset to defaults" restores every value.
 * Opened from the radial menu ({@code OPEN_PLAYER_SETTINGS}) and the "Open player settings" key.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PlayerSettingsGui extends Screen {

	private static final String LANG = "sophisticatedbuilding.player_settings.";
	private static final int ROW_HEIGHT = 24;
	private static final int TOP = 32;
	private static final int BOTTOM = 36;
	private static final int MAX_ROW_WIDTH = 320;

	protected SettingsList list;
	protected Button resetButton;
	protected Button doneButton;
	private final List<SettingEntry<?>> entries = new ArrayList<>();
	private boolean changed;

	public PlayerSettingsGui() {
		super(Component.translatable("sophisticatedbuilding.screen.player_settings"));
	}

	@Override
	protected void init() {
		double scroll = list != null ? list.getScrollAmount() : 0;
		entries.clear();
		list = new SettingsList(minecraft, width, height - TOP - BOTTOM, TOP, ROW_HEIGHT);

		var visuals = ClientConfig.visuals;
		var performance = ClientConfig.performance;
		list.addHeader(Component.translatable(LANG + "visuals"));
		add(new BooleanEntry("showBlockPreviews", visuals.showBlockPreviews));
		add(new BooleanEntry("onlyShowBlockPreviewsWhenBuilding", visuals.onlyShowBlockPreviewsWhenBuilding));
		add(new BooleanEntry("showMiniBlockPreview", visuals.showMiniBlockPreview));
		add(new IntEntry("maxBlockPreviews", visuals.maxBlockPreviews, 64, 3,
				value -> value == 0 ? Component.translatable(LANG + "value.outline_only") : blocks(value)));
		add(new IntEntry("appearAnimationLength", visuals.appearAnimationLength, 1, 1, PlayerSettingsGui::ticks));
		add(new IntEntry("breakAnimationLength", visuals.breakAnimationLength, 1, 1, PlayerSettingsGui::ticks));
		add(new DoubleEntry("previewScale", visuals.previewScale, 0.05,
				value -> Component.translatable(LANG + "value.percent", Math.round(value * 100))));
		list.addHeader(Component.translatable(LANG + "performance"));
		add(new IntEntry("previewRenderDistance", performance.previewRenderDistance, 8, 1, PlayerSettingsGui::blocks));
		add(new BooleanEntry("enableUpdateThrottling", performance.enableUpdateThrottling));
		add(new IntEntry("maxMiniBlockPreviews", performance.maxMiniBlockPreviews, 64, 3,
				value -> value == 0 ? Component.translatable(LANG + "value.no_limit") : blocks(value)));
		addRenderableWidget(list);
		list.setScrollAmount(scroll);

		int buttonWidth = Math.min(150, (width - 30) / 2);
		int buttonY = height - BOTTOM + 8;
		resetButton = addRenderableWidget(Button.builder(Component.translatable(LANG + "reset"), button -> resetToDefaults())
				.bounds(width / 2 - buttonWidth - 5, buttonY, buttonWidth, 20)
				.tooltip(Tooltip.create(Component.translatable(LANG + "reset.tooltip")))
				.build());
		doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(width / 2 + 5, buttonY, buttonWidth, 20)
				.build());
	}

	private void add(SettingEntry<?> entry) {
		entries.add(entry);
		list.addSetting(entry);
	}

	private static Component blocks(double value) {
		return Component.translatable(LANG + "value.blocks", (int) value);
	}

	private static Component ticks(double value) {
		int ticks = (int) value;
		if (ticks == 0) return CommonComponents.OPTION_OFF;
		return Component.translatable(LANG + "value.ticks", ticks, String.format("%.2f", ticks / 20.0));
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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
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
			super(minecraft, width, height, y, itemHeight);
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
			return getRowRight() + 6;
		}

		@Override
		public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
			Color shadow = new Color(0x60_000000);
			UIRenderHelper.angledGradient(guiGraphics, 90, getX() + width / 2, getY(), width, 5, shadow, Color.TRANSPARENT_BLACK);
			UIRenderHelper.angledGradient(guiGraphics, -90, getX() + width / 2, getY() + getHeight(), width, 5, shadow, Color.TRANSPARENT_BLACK);
			super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
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
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTicks) {
			guiGraphics.drawCenteredString(font, text, left + width / 2, top + (height - font.lineHeight) / 2 + 1, 0xFFFFAA00);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of();
		}
	}

	/** One setting: its translated label on the left, its control on the right; the label shows the tooltip too. */
	public abstract class SettingEntry<T> extends Row {
		public final String key;
		public final ConfigValue<T> config;
		protected final Component label;
		protected final Tooltip tooltip;

		SettingEntry(String key, ConfigValue<T> config) {
			this.key = key;
			this.config = config;
			this.label = Component.translatable(LANG + key);
			this.tooltip = Tooltip.create(Component.translatable(LANG + key + ".tooltip"));
		}

		public abstract AbstractWidget widget();

		abstract void reset();

		protected int controlWidth(int rowWidth) {
			return Math.min(150, rowWidth / 2);
		}

		@Override
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTicks) {
			AbstractWidget widget = widget();
			int controlWidth = controlWidth(width);
			widget.setWidth(controlWidth);
			widget.setX(left + width - controlWidth);
			widget.setY(top + (height - 20) / 2);
			widget.render(guiGraphics, mouseX, mouseY, partialTicks);

			int labelWidth = width - controlWidth - 6;
			int labelY = top + (height - font.lineHeight) / 2 + 1;
			var text = font.plainSubstrByWidth(label.getString(), labelWidth);
			guiGraphics.drawString(font, text, left, labelY, 0xFFFFFF, false);
			if (mouseX >= left && mouseX < left + labelWidth && mouseY >= top && mouseY < top + height) {
				setTooltipForNextRenderPass(tooltip, DefaultTooltipPositioner.INSTANCE, true);
			}
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(widget());
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(widget());
		}
	}

	protected class BooleanEntry extends SettingEntry<Boolean> {
		private final Button button;

		BooleanEntry(String key, ConfigValue<Boolean> config) {
			super(key, config);
			button = Button.builder(CommonComponents.optionStatus(config.get()), b -> set(!config.get()))
					.bounds(0, 0, 150, 20)
					.tooltip(tooltip)
					.build();
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
				super(0, 0, 150, 20, Component.empty(), values.positionOf(config.get().doubleValue()));
				setTooltip(tooltip);
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
		return List.copyOf(entries);
	}
}
