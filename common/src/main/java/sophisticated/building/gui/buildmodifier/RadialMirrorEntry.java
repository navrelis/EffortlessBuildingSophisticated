package sophisticated.building.gui.buildmodifier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import sophisticated.building.AllGuiTextures;
import sophisticated.building.AllIcons;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.buildmodifier.BaseModifier;
import sophisticated.building.buildmodifier.RadialMirror;
import sophisticated.building.create.foundation.gui.widget.IconButton;
import sophisticated.building.create.foundation.gui.widget.ScrollInput;
import sophisticated.building.create.foundation.utility.Components;
import sophisticated.building.gui.elements.LabeledScrollInput;
import sophisticated.building.utilities.MathHelper;

import java.text.DecimalFormat;
import java.util.Vector;

public class RadialMirrorEntry extends BaseModifierEntry<RadialMirror> {

	protected Vector<ScrollInput> positionInputs;
	protected IconButton playerPositionButton;
	protected IconButton toggleOffsetButton;
	protected ScrollInput slicesInput;
	protected IconButton alternateButton;
	protected ScrollInput radiusInput;
	protected IconButton showLinesButton;
	protected IconButton showAreasButton;
	protected DecimalFormat df = new DecimalFormat("#.#");

	public RadialMirrorEntry(ModifiersScreen screen, BaseModifier radialMirror) {
		super(screen, (RadialMirror) radialMirror, Component.translatable("sophisticatedbuilding.gui.modifier.radial_mirror"), AllGuiTextures.RADIAL_MIRROR_ENTRY);

		positionInputs = new Vector<>();

		//Position
		//ScrollInput works with double the value, so we can have 0.5 increments
		for (int i = 0; i < 3; i++) {
			final int index = i;
			var scrollInput = new LabeledScrollInput(0, 0, 36, 18)
				.showControlScrollsSlowerTooltip()
				.titled(Component.translatable(i == 0 ? "sophisticatedbuilding.gui.modifier.x_position" : i == 1 ? "sophisticatedbuilding.gui.modifier.y_position" : "sophisticatedbuilding.gui.modifier.z_position"))
				.format(integer -> Component.literal(df.format(integer / 2.0)))
				.withStepFunction(stepContext -> stepContext.shift ? 20 : stepContext.control ? 1 : 2)
				.calling(value -> {
					modifier.position = MathHelper.with(modifier.position, index, value / 2.0);
					onValueChanged();
				});
			scrollInput.setState((int) (MathHelper.get(modifier.position, index) * 2.0));
			positionInputs.add(scrollInput);
		}
		listeners.addAll(positionInputs);

		//Player position button
		playerPositionButton = new IconButton(0, 0, AllIcons.I_PLAYER)
			.withCallback(() -> {
				modifier.position = Vec3.atLowerCornerOf(Minecraft.getInstance().player.blockPosition());
				onValueChanged();
			});
		playerPositionButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.player_position"));
		listeners.add(playerPositionButton);

		//Toggle offset button
		toggleOffsetButton = new IconButton(0, 0, AllIcons.I_BLOCK_CENTER)
			.withCallback(() -> {
				if (modifier.position.x == Math.floor(modifier.position.x)) {
					modifier.position = new Vec3(
							Math.floor(modifier.position.x) + 0.5,
							Math.floor(modifier.position.y) + 0.5,
							Math.floor(modifier.position.z) + 0.5
					);
				}
				else {
					modifier.position = new Vec3(
							Math.floor(modifier.position.x),
							Math.floor(modifier.position.y),
							Math.floor(modifier.position.z)
					);
				}
				onValueChanged();
			});
		listeners.add(toggleOffsetButton);

		//Slices
		slicesInput = new LabeledScrollInput(0, 0, 27, 18)
			.withRange(3, 1000)
			.titled(Component.translatable("sophisticatedbuilding.gui.modifier.slices"))
			.calling(value -> {
				modifier.slices = value;
				onValueChanged();
			});
		slicesInput.setState(modifier.slices);
		listeners.add(slicesInput);

		//Alternate
		alternateButton = new IconButton(0, 0, AllIcons.I_ALTERNATE_OFF)
			.withCallback(() -> {
				modifier.alternate = !modifier.alternate;
				onValueChanged();
			});
		listeners.add(alternateButton);

		//Radius
		radiusInput = new LabeledScrollInput(0, 0, 27, 18)
			.withRange(0, AttachmentHandler.getMaxMirrorRadius(Minecraft.getInstance().player, false))
			.titled(Minecraft.getInstance().player.isCreative() ?
					Component.translatable("sophisticatedbuilding.gui.modifier.radius") :
					Component.translatable("sophisticatedbuilding.gui.modifier.radius_upgrade"))
			.calling(value -> {
				modifier.radius = value;
				onValueChanged();
			});
		radiusInput.setState(modifier.radius);
		listeners.add(radiusInput);

		//Show lines button
		showLinesButton = new IconButton(0, 0, AllIcons.I_SHOW_LINES)
			.withCallback(() -> {
				modifier.drawLines = !modifier.drawLines;
				onValueChanged();
			});
		listeners.add(showLinesButton);

		//Show areas button
		showAreasButton = new IconButton(0, 0, AllIcons.I_SHOW_AREAS)
			.withCallback(() -> {
				modifier.drawPlanes = !modifier.drawPlanes;
				onValueChanged();
			});
		listeners.add(showAreasButton);

		for (ScrollInput positionInput : positionInputs) {
			positionInput.onChanged();
		}
		radiusInput.onChanged();
	}

	@Override
	public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTicks) {
		super.extractContent(guiGraphics, mouseX, mouseY, hovering, partialTicks);

		//draw position inputs
		for (int i = 0; i < 3; i++) {
			ScrollInput input = positionInputs.get(i);
			input.setX(left + 49 + 38 * i);
			input.setY(top + 19);
			input.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		}

		//draw player position button
		playerPositionButton.setX(left + 163);
		playerPositionButton.setY(top + 19);
		playerPositionButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);

		//draw toggle offset button
		toggleOffsetButton.setX(left + 183);
		toggleOffsetButton.setY(top + 19);
		toggleOffsetButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);

		//draw slices input
		slicesInput.setX(left + 49);
		slicesInput.setY(top + 41);
		slicesInput.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);

		//draw alternate button
		alternateButton.setX(left + 78);
		alternateButton.setY(top + 41);
		alternateButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);

		//draw radius input
		radiusInput.setX(left + 134);
		radiusInput.setY(top + 41);
		radiusInput.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);

		//draw show lines button
		showLinesButton.setX(left + 163);
		showLinesButton.setY(top + 41);
		showLinesButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);

		//draw show areas button
		showAreasButton.setX(left + 183);
		showAreasButton.setY(top + 41);
		showAreasButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public void onValueChanged() {
		super.onValueChanged();

		//Position
		for (int i = 0; i < 3; i++) {
			ScrollInput input = positionInputs.get(i);
			input.setState((int) (MathHelper.get(modifier.position, i) * 2.0));
		}

		//Toggle offset button
		if (modifier.position.x == Math.floor(modifier.position.x)) {
			toggleOffsetButton.setIcon(AllIcons.I_BLOCK_CENTER);
			toggleOffsetButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.block_center"));
		}
		else {
			toggleOffsetButton.setIcon(AllIcons.I_BLOCK_CORNER);
			toggleOffsetButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.block_corner"));
		}

		//Toggle alternate button
		if (modifier.alternate) {
			alternateButton.setIcon(AllIcons.I_ALTERNATE_ON);
			alternateButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.alternate_on"));
		}
		else {
			alternateButton.setIcon(AllIcons.I_ALTERNATE_OFF);
			alternateButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.alternate_off"));
		}

		//Show lines button
		if (modifier.drawLines) {
			showLinesButton.setIcon(AllIcons.I_SHOW_LINES);
			showLinesButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.show_lines"));
		}
		else {
			showLinesButton.setIcon(AllIcons.I_HIDE_LINES);
			showLinesButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.show_lines"));
		}

		//Show areas button
		if (modifier.drawPlanes) {
			showAreasButton.setIcon(AllIcons.I_SHOW_AREAS);
			showAreasButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.show_areas"));
		}
		else {
			showAreasButton.setIcon(AllIcons.I_HIDE_AREAS);
			showAreasButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.show_areas"));
		}
	}
}

