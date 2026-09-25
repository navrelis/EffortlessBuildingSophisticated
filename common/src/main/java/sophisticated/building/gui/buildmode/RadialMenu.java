package sophisticated.building.gui.buildmode;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import sophisticated.building.utilities.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import sophisticated.building.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.buildmode.ModeOptions.ActionEnum;
import sophisticated.building.buildmode.ModeOptions.OptionEnum;
import sophisticated.building.create.foundation.item.ItemDescription;
import sophisticated.building.create.foundation.item.TooltipHelper;
import sophisticated.building.create.foundation.utility.Components;
import sophisticated.building.create.foundation.utility.Lang;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static sophisticated.building.buildmode.ModeOptions.getBuildSpeed;
import static sophisticated.building.buildmode.ModeOptions.getCircleStart;
import static sophisticated.building.buildmode.ModeOptions.getCubeFill;
import static sophisticated.building.buildmode.ModeOptions.getFill;
import static sophisticated.building.buildmode.ModeOptions.getLineThickness;
import static sophisticated.building.buildmode.ModeOptions.getRaisedEdge;

/**
 * Initially from Chisels and Bits by AlgorithmX2
 * https://github.com/AlgorithmX2/Chisels-and-Bits/blob/1.12/src/main/java/mod/chiselsandbits/client/gui/ChiselsAndBitsMenu.java
 */

public class RadialMenu extends Screen {

	public static final RadialMenu instance = new RadialMenu();

	private final Color radialButtonColor = new Color(0f, 0f, 0f, .5f);
	private final Color sideButtonColor = new Color(.5f, .5f, .5f, .5f);
	private final Color highlightColor = new Color(.6f, .8f, 1f, .6f);
	private final Color selectedColor = new Color(0f, .5f, 1f, .5f);
	private final Color highlightSelectedColor = new Color(0.2f, .7f, 1f, .7f);

	private final int whiteTextColor = 0xffffffff;
	private final int watermarkTextColor = 0x88888888;
	private final int descriptionTextColor = 0xdd888888;
	private final int optionTextColor = 0xeeeeeeff;

	private final double ringInnerEdge = 30;
	private final double ringOuterEdge = 65;
	private final double categoryLineWidth = 1;
	private final double textDistance = 75;
	private final double buttonDistance = 105;
	private final float fadeSpeed = 0.3f;
	private final int buildModeDescriptionHeight = 100;
	private final int actionDescriptionWidth = 200;
	// Space above an option row's first button for its label, and kept free at the bottom right for the power level text
	private static final double OPTION_LABEL_HEADROOM = 14;
	private static final double OPTION_BOTTOM_RESERVE = 24;

	/** A side button as last drawn (screen GUI coordinates), for tests. */
	public static final class SideButton {
		public final ActionEnum action;
		public final double left;
		public final double top;
		public final double right;
		public final double bottom;

		public SideButton(ActionEnum action, double left, double top, double right, double bottom) {
			this.action = action;
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
		}

		public ActionEnum action() {
			return action;
		}

		public double left() {
			return left;
		}

		public double top() {
			return top;
		}

		public double right() {
			return right;
		}

		public double bottom() {
			return bottom;
		}
	}

	private RadialButtonLayout.Result optionLayout;
	private List<SideButton> sideButtons = Collections.emptyList();

	public BuildModeEnum switchTo = null;
	public ActionEnum doAction = null;
	public boolean performedActionUsingMouse;

	private float visibility;

	// Accumulated mouse offset from the screen center, used for radial selection.
	// On some platforms/configurations opening a Screen warps the OS cursor to the
	// window center, making mouseHandler.xpos()/ypos() unreliable for absolute
	// position. We track deltas via mouseMoved() and use those as the offset from
	// the screen center. On the very first frame we seed from the scaled standard
	// mouseX/mouseY passed by Minecraft (which are correct pre-warp).
	private double accumulatedMouseX;
	private double accumulatedMouseY;
	private double lastRawMouseX;
	private double lastRawMouseY;
	private boolean mouseInitialized;

	public RadialMenu() {
		super(new TranslatableComponent("sophisticatedbuilding.screen.radial_menu"));
	}

	/** The side buttons (actions and build mode options) as last drawn. */
	public List<SideButton> sideButtons() {
		return sideButtons;
	}

	/** Side buttons stay this far from the centre: clear of the ring and of the mode name drawn around it. */
	private double minSideButtonInner() {
		return ringOuterEdge + 20;
	}

	private static int[] columns(int count) {
		int[] columns = new int[count];
		for (int i = 0; i < count; i++) columns[i] = i;
		return columns;
	}

	private static List<int[]> optionColumns(OptionEnum[] options) {
		List<int[]> rows = new ArrayList<>();
		for (OptionEnum option : options) rows.add(columns(option.actions.length));
		return rows;
	}

	public boolean isVisible() {
		return Minecraft.getInstance().screen instanceof RadialMenu;
	}

	@Override
	protected void init() {
		super.init();
		performedActionUsingMouse = false;
		visibility = 0f;
		accumulatedMouseX = 0;
		accumulatedMouseY = 0;
		mouseInitialized = false;
	}

	@Override
	public void tick() {
		super.tick();

		if (!ClientEvents.isKeybindDown(0)) {
			onClose();
		}
	}

	@Override
	public void render(PoseStack poseStack, final int mouseX, final int mouseY, final float partialTicks) {
		GuiGraphics guiGraphics = new GuiGraphics(poseStack);
		BuildModeEnum currentBuildMode = SophisticatedBuildingClient.BUILD_MODES.getBuildMode();

		PoseStack ms = guiGraphics.pose();
		ms.pushPose();

		visibility += fadeSpeed * partialTicks;
		if (visibility > 1f) visibility = 1f;

		final int startColor = (int) (visibility * 98) << 24;
		final int endColor = (int) (visibility * 128) << 24;

		guiGraphics.fillGradient(0, 0, width, height, startColor, endColor);

		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
		GuiGraphics.beginPositionColor();
		final Tesselator tesselator = Tesselator.getInstance();
		final BufferBuilder buffer = tesselator.getBuilder();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);

		final double middleX = width / 2.0;
		final double middleY = height / 2.0;

		// On some platforms/configurations opening a Screen warps the OS cursor to
		// the window center, making mouseHandler.xpos()/ypos() unreliable for
		// absolute position. We instead accumulate deltas via mouseMoved() and use
		// those as the offset from the screen center. On the very first frame we
		// seed from the scaled standard mouseX/mouseY passed by Minecraft.
		if (!mouseInitialized) {
			accumulatedMouseX = mouseX - middleX;
			accumulatedMouseY = mouseY - middleY;
			lastRawMouseX = minecraft.mouseHandler.xpos();
			lastRawMouseY = minecraft.mouseHandler.ypos();
			mouseInitialized = true;
		}

		int mouseXX = (int) (middleX + accumulatedMouseX);
		int mouseYY = (int) (middleY + accumulatedMouseY);

		final double mouseXCenter = accumulatedMouseX;
		final double mouseYCenter = accumulatedMouseY;
		double mouseRadians = Math.atan2(mouseYCenter, mouseXCenter);

		final double quarterCircle = Math.PI / 2.0;

		if (mouseRadians < -quarterCircle) {
			mouseRadians = mouseRadians + Math.PI * 2;
		}

		final ArrayList<MenuRegion> modes = new ArrayList<MenuRegion>();
		final ArrayList<MenuButton> buttons = new ArrayList<MenuButton>();

		//Add build modes
		for (final BuildModeEnum mode : BuildModeEnum.values()) {
			modes.add(new MenuRegion(mode));
		}

		//Add actions
		boolean canReplace = AttachmentHandler.canReplaceBlocks(minecraft.player);

		// Left side, rows from the top, columns counted from the ring outwards: player settings above modifier
		// settings; protect/mini preview/modifier settings/undo/redo; the replace modes
		List<List<ActionEnum>> leftRows = new ArrayList<>();
		leftRows.add(Collections.singletonList(ActionEnum.OPEN_PLAYER_SETTINGS));
		List<ActionEnum> actionRow = new ArrayList<>(Arrays.asList(ActionEnum.REDO, ActionEnum.UNDO, ActionEnum.OPEN_MODIFIER_SETTINGS, ActionEnum.TOGGLE_MINI_PREVIEW));
		if (canReplace) actionRow.add(ActionEnum.TOGGLE_PROTECT_TILE_ENTITIES);
		leftRows.add(actionRow);
		if (canReplace) {
			leftRows.add(Arrays.asList(ActionEnum.REPLACE_FILTERED_BY_OFFHAND, ActionEnum.REPLACE_ONLY_BLOCKS, ActionEnum.REPLACE_BLOCKS_AND_AIR, ActionEnum.REPLACE_ONLY_AIR));
		}
		List<int[]> leftColumns = new ArrayList<>();
		leftColumns.add(new int[]{2});
		for (int r = 1; r < leftRows.size(); r++) leftColumns.add(columns(leftRows.get(r).size()));
		RadialButtonLayout.Result left = RadialButtonLayout.layout(false, leftColumns, width, height, buttonDistance, minSideButtonInner(),
				-39, 26, 0, 0);
		int leftIndex = 0;
		for (List<ActionEnum> row : leftRows) {
			for (ActionEnum action : row) {
				RadialButtonLayout.Cell cell = left.cells().get(leftIndex++);
				buttons.add(new MenuButton(action, cell.x(), cell.y(), Direction.UP));
			}
		}

		//Add buildmode dependent options (right side, one labelled row per option; they move in and wrap on narrow screens)
		OptionEnum[] options = currentBuildMode.options;
		optionLayout = RadialButtonLayout.layout(true, optionColumns(options), width, height, buttonDistance, minSideButtonInner(),
				-13, 39, OPTION_LABEL_HEADROOM, OPTION_BOTTOM_RESERVE);
		int optionIndex = 0;
		for (OptionEnum option : options) {
			for (ActionEnum action : option.actions) {
				RadialButtonLayout.Cell cell = optionLayout.cells().get(optionIndex++);
				buttons.add(new MenuButton(action, cell.x(), cell.y(), Direction.DOWN));
			}
		}
		sideButtons = buttons.stream()
				.map(b -> new SideButton(b.action, middleX + b.x1, middleY + b.y1, middleX + b.x2, middleY + b.y2))
				.collect(Collectors.toList());

		switchTo = null;
		doAction = null;

		//Draw buildmode backgrounds
		drawRadialButtonBackgrounds(currentBuildMode, buffer, middleX, middleY, mouseXCenter, mouseYCenter, mouseRadians,
				quarterCircle, modes);

		//Draw action backgrounds
		drawSideButtonBackgrounds(buffer, middleX, middleY, mouseXCenter, mouseYCenter, buttons);

		buffer.end();
		BufferUploader.end(buffer);
		GuiGraphics.endPositionColor();
		RenderSystem.disableBlend();

		ms.translate(0, 0, 200);
		
		drawIcons(guiGraphics, middleX, middleY, modes, buttons);

		drawTexts(guiGraphics, currentBuildMode, middleX, middleY, modes, buttons, options, mouseXX, mouseYY);

		ms.popPose();
	}

	private void drawRadialButtonBackgrounds(BuildModeEnum currentBuildMode, BufferBuilder buffer, double middleX, double middleY,
											 double mouseXCenter, double mouseYCenter, double mouseRadians, double quarterCircle, ArrayList<MenuRegion> modes) {
		if (!modes.isEmpty()) {
			final int totalModes = Math.max(3, modes.size());
			final double fragment = Math.PI * 0.005;
			final double fragment2 = Math.PI * 0.0025;
			final double radiansPerObject = 2.0 * Math.PI / totalModes;

			for (int i = 0; i < modes.size(); i++) {
				MenuRegion menuRegion = modes.get(i);
				final double beginRadians = i * radiansPerObject - quarterCircle;
				final double endRadians = (i + 1) * radiansPerObject - quarterCircle;

				menuRegion.x1 = Math.cos(beginRadians);
				menuRegion.x2 = Math.cos(endRadians);
				menuRegion.y1 = Math.sin(beginRadians);
				menuRegion.y2 = Math.sin(endRadians);

				final double x1m1 = Math.cos(beginRadians + fragment) * ringInnerEdge;
				final double x2m1 = Math.cos(endRadians - fragment) * ringInnerEdge;
				final double y1m1 = Math.sin(beginRadians + fragment) * ringInnerEdge;
				final double y2m1 = Math.sin(endRadians - fragment) * ringInnerEdge;

				final double x1m2 = Math.cos(beginRadians + fragment2) * ringOuterEdge;
				final double x2m2 = Math.cos(endRadians - fragment2) * ringOuterEdge;
				final double y1m2 = Math.sin(beginRadians + fragment2) * ringOuterEdge;
				final double y2m2 = Math.sin(endRadians - fragment2) * ringOuterEdge;

				final boolean isSelected = currentBuildMode.ordinal() == i;
				final boolean isMouseInQuad = inTriangle(x1m1, y1m1, x2m2, y2m2, x2m1, y2m1, mouseXCenter, mouseYCenter)
						|| inTriangle(x1m1, y1m1, x1m2, y1m2, x2m2, y2m2, mouseXCenter, mouseYCenter);
				final boolean isHighlighted = beginRadians <= mouseRadians && mouseRadians <= endRadians && isMouseInQuad;

				Color color = radialButtonColor;
				if (isSelected) color = selectedColor;
				if (isHighlighted) color = highlightColor;
				if (isSelected && isHighlighted) color = highlightSelectedColor;

				if (isHighlighted) {
					menuRegion.highlighted = true;
					switchTo = menuRegion.mode;
				}

				buffer.vertex((float)(middleX + x1m1), (float)(middleY + y1m1), 20f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
				buffer.vertex((float)(middleX + x2m1), (float)(middleY + y2m1), 20f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
				buffer.vertex((float)(middleX + x2m2), (float)(middleY + y2m2), 20f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
				buffer.vertex((float)(middleX + x1m2), (float)(middleY + y1m2), 20f).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

				//Category line
				color = menuRegion.mode.category.color;
				final double categoryLineOuterEdge = ringInnerEdge + categoryLineWidth;

				final double x1m3 = Math.cos(beginRadians + fragment) * categoryLineOuterEdge;
				final double x2m3 = Math.cos(endRadians - fragment) * categoryLineOuterEdge;
				final double y1m3 = Math.sin(beginRadians + fragment) * categoryLineOuterEdge;
				final double y2m3 = Math.sin(endRadians - fragment) * categoryLineOuterEdge;

				buffer.vertex((float)(middleX + x1m1), (float)(middleY + y1m1), 200).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
				buffer.vertex((float)(middleX + x2m1), (float)(middleY + y2m1), 200).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
				buffer.vertex((float)(middleX + x2m3), (float)(middleY + y2m3), 200).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
				buffer.vertex((float)(middleX + x1m3), (float)(middleY + y1m3), 200).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
			}
		}
	}

	private void drawSideButtonBackgrounds(BufferBuilder buffer, double middleX, double middleY, double mouseXCenter, double mouseYCenter, ArrayList<MenuButton> buttons) {
		for (final MenuButton btn : buttons) {

			final boolean isHighlighted = btn.x1 <= mouseXCenter && btn.x2 >= mouseXCenter && btn.y1 <= mouseYCenter && btn.y2 >= mouseYCenter;

			boolean isSelected =
					btn.action == getBuildSpeed() ||
							btn.action == getFill() ||
							btn.action == getCubeFill() ||
							btn.action == getRaisedEdge() ||
							btn.action == getLineThickness() ||
							btn.action == getCircleStart() ||
							btn.action == ModeOptions.getTerrainNoise() ||
							btn.action == ModeOptions.getTerrainType() ||
							btn.action == SophisticatedBuildingClient.BUILD_SETTINGS.getReplaceModeActionEnum() ||
					btn.action == ActionEnum.TOGGLE_PROTECT_TILE_ENTITIES && SophisticatedBuildingClient.BUILD_SETTINGS.shouldProtectTileEntities() ||
					btn.action == ActionEnum.TOGGLE_MINI_PREVIEW && SophisticatedBuildingClient.BLOCK_PREVIEWS.isMiniBlockPreviewEnabled();

			Color color = sideButtonColor;
			if (isSelected) color = selectedColor;
			if (isHighlighted) color = highlightColor;
			if (isSelected && isHighlighted) color = highlightSelectedColor;

			if (isHighlighted) {
				btn.highlighted = true;
				doAction = btn.action;
			}

			buffer.vertex((float)(middleX + btn.x1), (float)(middleY + btn.y1), 200).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
			buffer.vertex((float)(middleX + btn.x1), (float)(middleY + btn.y2), 200).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
			buffer.vertex((float)(middleX + btn.x2), (float)(middleY + btn.y2), 200).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
			buffer.vertex((float)(middleX + btn.x2), (float)(middleY + btn.y1), 200).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
		}
	}

	private void drawIcons(GuiGraphics guiGraphics, double middleX, double middleY,
						   ArrayList<MenuRegion> modes, ArrayList<MenuButton> buttons) {
		PoseStack ms = guiGraphics.pose();
		ms.pushPose();

		//Draw buildmode icons
		for (final MenuRegion menuRegion : modes) {

			final double x = (menuRegion.x1 + menuRegion.x2) * 0.5 * (ringOuterEdge * 0.55 + 0.45 * ringInnerEdge);
			final double y = (menuRegion.y1 + menuRegion.y2) * 0.5 * (ringOuterEdge * 0.55 + 0.45 * ringInnerEdge);

			menuRegion.mode.icon.render(guiGraphics, (int) (middleX + x - 8), (int) (middleY + y - 8));
		}

		//Draw action icons
		for (final MenuButton button : buttons) {

			final double x = (button.x1 + button.x2) / 2 + 0.01;
			final double y = (button.y1 + button.y2) / 2 + 0.01;

			button.action.icon.render(guiGraphics, (int) (middleX + x - 8), (int) (middleY + y - 8));
		}

		ms.popPose();
	}

	private void drawTexts(GuiGraphics guiGraphics, BuildModeEnum currentBuildMode, double middleX, double middleY, ArrayList<MenuRegion> modes, ArrayList<MenuButton> buttons, OptionEnum[] options, int mouseX, int mouseY) {
		//Draw option strings
		for (int i = 0; i < currentBuildMode.options.length; i++) {
			OptionEnum option = options[i];
			String label = I18n.get(option.name);
			// Above the row's first button; moved left if it would run past the screen edge
			int labelX = (int) Math.min(middleX + optionLayout.inner() - 9, width - 2 - font.width(label));
			guiGraphics.drawString(font, label, labelX, (int) (middleY + optionLayout.rowFirstY()[i] - 24), optionTextColor);
		}

		String credits = "Sophisticated Building";
		guiGraphics.drawString(font, credits, width - font.width(credits) - 4, height - 10, watermarkTextColor);

		//Draw power level info
		String powerLevelValue = minecraft.player.isCreative() ? "Creative" : String.valueOf(AttachmentHandler.getPowerLevel(minecraft.player));
		String powerLevelText = I18n.get("key.sophisticatedbuilding.power_level") + ": " + powerLevelValue;
		guiGraphics.drawString(font, powerLevelText, width - font.width(powerLevelText) - 4, height - 22, minecraft.player.isCreative() ? watermarkTextColor : ChatFormatting.DARK_PURPLE.getColor());

		//if hover over power level info (and not over a button or mode, whose tooltip wins), show tooltip
		if (doAction == null && switchTo == null && mouseX >= width - font.width(powerLevelText) - 14 && mouseX <= width && mouseY >= height - 24 && mouseY <= height) {
			ArrayList<Component> tooltip = new ArrayList<Component>();
			tooltip.add(Components.literal(powerLevelText).withStyle(ChatFormatting.DARK_PURPLE));
			int placementReach = AttachmentHandler.getPlacementReach(minecraft.player, false);
			tooltip.add(Components.translatable("key.sophisticatedbuilding.placement_reach").withStyle(ChatFormatting.GRAY).append(": " + (placementReach == 0 ? "vanilla" : placementReach + " blocks")));
			tooltip.add(Components.translatable("key.sophisticatedbuilding.max_blocks_per_axis").withStyle(ChatFormatting.GRAY).append(": " + AttachmentHandler.getMaxBlocksPerAxis(minecraft.player, false)));
			tooltip.add(Components.translatable("key.sophisticatedbuilding.max_blocks_placed_at_once").withStyle(ChatFormatting.GRAY).append(": " + AttachmentHandler.getMaxBlocksPlacedAtOnce(minecraft.player, false)));
			tooltip.add(Components.translatable("key.sophisticatedbuilding.max_mirror_radius").withStyle(ChatFormatting.GRAY).append(": " + AttachmentHandler.getMaxMirrorRadius(minecraft.player, false) + " blocks"));

			if (AttachmentHandler.canIncreasePowerLevel(minecraft.player) && !minecraft.player.isCreative()) {
				tooltip.add(Components.literal(""));
				tooltip.add(Components.translatable("key.sophisticatedbuilding.next_power_level").withStyle(ChatFormatting.DARK_AQUA).append(": " + AttachmentHandler.getNextPowerLevel(minecraft.player)));
				tooltip.add(Components.translatable("key.sophisticatedbuilding.placement_reach").withStyle(ChatFormatting.GRAY).append(": " + AttachmentHandler.getPlacementReach(minecraft.player, true) + " blocks"));
				tooltip.add(Components.translatable("key.sophisticatedbuilding.max_blocks_per_axis").withStyle(ChatFormatting.GRAY).append(": " + AttachmentHandler.getMaxBlocksPerAxis(minecraft.player, true)));
				tooltip.add(Components.translatable("key.sophisticatedbuilding.max_blocks_placed_at_once").withStyle(ChatFormatting.GRAY).append(": " + AttachmentHandler.getMaxBlocksPlacedAtOnce(minecraft.player, true)));
				tooltip.add(Components.translatable("key.sophisticatedbuilding.max_mirror_radius").withStyle(ChatFormatting.GRAY).append(": " + AttachmentHandler.getMaxMirrorRadius(minecraft.player, true) + " blocks"));
				tooltip.add(Components.literal(""));
				tooltip.addAll(TooltipHelper.cutTextComponent(Components.translatable("key.sophisticatedbuilding.next_power_level_how"), ChatFormatting.GRAY, ChatFormatting.WHITE));
			}

			guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
		}

		//Draw buildmode text
		for (final MenuRegion menuRegion : modes) {

			if (menuRegion.highlighted) {
				final double x = (menuRegion.x1 + menuRegion.x2) * 0.5;
				final double y = (menuRegion.y1 + menuRegion.y2) * 0.5;

				int fixed_x = (int) (x * textDistance);
				int fixed_y = (int) (y * textDistance) - font.lineHeight / 2;
				String text = I18n.get(menuRegion.mode.getNameKey());

				if (x <= -0.2) {
					fixed_x -= font.width(text);
				} else if (-0.2 <= x && x <= 0.2) {
					fixed_x -= font.width(text) / 2;
				}

				guiGraphics.drawString(font, text, (int) middleX + fixed_x, (int) middleY + fixed_y, whiteTextColor);

				//Draw description
				text = I18n.get(menuRegion.mode.getDescriptionKey());
				guiGraphics.drawString(font, text, (int) ((int) middleX - font.width(text) / 2f), (int) middleY + buildModeDescriptionHeight, descriptionTextColor);
			}
		}

		//Draw action text
		for (final MenuButton button : buttons) {
			if (button.highlighted) {

				ArrayList<Component> tooltip = new ArrayList<Component>();
				tooltip.add(Components.literal(button.name).withStyle(ChatFormatting.AQUA));

				//Add description when holding shift
				if (!button.description.isEmpty()) {
					tooltip.add(TooltipHelper.holdShift(ItemDescription.Palette.Blue, hasShiftDown()));
					if (hasShiftDown()) {
						tooltip.addAll(TooltipHelper.cutStringTextComponent(button.description, ChatFormatting.GRAY, ChatFormatting.WHITE));
					}
				}

				//Add keybind in brackets
				MutableComponent keybind = findKeybind(button);
				if (keybind != null)
					tooltip.add(Lang.translateDirect("tooltip.keybind", keybind.withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
				guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
			}
		}
	}

	private MutableComponent findKeybind(MenuButton button) {

		int keybindingIndex = -1;
		if (button.action == ActionEnum.OPEN_MODIFIER_SETTINGS) keybindingIndex = 1;
		if (button.action == ActionEnum.UNDO) keybindingIndex = 2;
		if (button.action == ActionEnum.REDO) keybindingIndex = 3;
		if (button.action == ActionEnum.OPEN_PLAYER_SETTINGS) keybindingIndex = ClientEvents.PLAYER_SETTINGS_KEY;

		if (keybindingIndex != -1) {
			KeyMapping keyMap = ClientEvents.keyBindings[keybindingIndex];
			if (keyMap.isUnbound()) return null;

			return Components.keybind(keyMap.getName());
		}
		return null;
	}

	private boolean inTriangle(final double x1, final double y1, final double x2, final double y2,
							   final double x3, final double y3, final double x, final double y) {
		final double ab = (x1 - x) * (y2 - y) - (x2 - x) * (y1 - y);
		final double bc = (x2 - x) * (y3 - y) - (x3 - x) * (y2 - y);
		final double ca = (x3 - x) * (y1 - y) - (x1 - x) * (y3 - y);
		return sign(ab) == sign(bc) && sign(bc) == sign(ca);
	}

	private int sign(final double n) {
		return n > 0 ? 1 : -1;
	}

	@Override
	public void mouseMoved(double xpos, double ypos) {
		if (!mouseInitialized) return;
		double rawX = minecraft.mouseHandler.xpos();
		double rawY = minecraft.mouseHandler.ypos();
		double dx = (rawX - lastRawMouseX) * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
		double dy = (rawY - lastRawMouseY) * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
		lastRawMouseX = rawX;
		lastRawMouseY = rawY;
		accumulatedMouseX += dx;
		accumulatedMouseY += dy;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		performAction(true);

		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void onClose() {
		super.onClose();
		//After onClose so it can open another screen
		if (!performedActionUsingMouse) performAction(false);
	}

	private void performAction(boolean fromMouseClick) {

		if (switchTo != null) {
			playRadialMenuSound();

			SophisticatedBuildingClient.BUILD_MODES.setBuildMode(switchTo);

			if (fromMouseClick) performedActionUsingMouse = true;
		}

		//Perform button action
		ModeOptions.ActionEnum action = doAction;
		if (action != null) {
			playRadialMenuSound();

			ModeOptions.performAction(minecraft.player, action);

			if (fromMouseClick) performedActionUsingMouse = true;
		}
	}

	public static void playRadialMenuSound() {
		final float volume = 0.1f;
		if (volume >= 0.0001f) {
			SimpleSoundInstance sound = new SimpleSoundInstance(SoundEvents.UI_BUTTON_CLICK, SoundSource.MASTER, volume,
					1.0f, Minecraft.getInstance().player.blockPosition());
			Minecraft.getInstance().getSoundManager().play(sound);
		}
	}

	private static class MenuButton {

		public final ActionEnum action;
		public double x1, x2;
		public double y1, y2;
		public boolean highlighted;
		public String name;
		public String description = "";
		public Direction textSide;

		public MenuButton(final ActionEnum action, final double x, final double y,
						  final Direction textSide) {
			this.name = I18n.get(action.getNameKey());

			if (I18n.exists(action.getDescriptionKey())) {
				this.description = I18n.get(action.getDescriptionKey());
			}

			this.action = action;
			x1 = x - 10;
			x2 = x + 10;
			y1 = y - 10;
			y2 = y + 10;
			this.textSide = textSide;
		}

	}

	static class MenuRegion {

		public final BuildModeEnum mode;
		public double x1, x2;
		public double y1, y2;
		public boolean highlighted;

		public MenuRegion(final BuildModeEnum mode) {
			this.mode = mode;
		}

	}

}
