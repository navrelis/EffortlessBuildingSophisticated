package sophisticated.building.create.catnip.gui.element;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import sophisticated.building.create.catnip.data.Couple;
import sophisticated.building.create.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.gui.element.BoxElement}, MIT License, Copyright (c) 2022
 * The Create Team, see LICENSE_Ponder.txt).
 */
public class BoxElement extends AbstractRenderElement {

	public static final Couple<Color> COLOR_VANILLA_BORDER = Couple.create(
		new Color(0x50_5000ff, true),
		new Color(0x50_28007f, true)
	).map(Color::setImmutable);
	public static final Color COLOR_VANILLA_BACKGROUND = new Color(0xf0_100010, true).setImmutable();
	public static final Color COLOR_BACKGROUND_FLAT = new Color(0xff_000000, true).setImmutable();
	public static final Color COLOR_BACKGROUND_TRANSPARENT = new Color(0xdd_000000, true).setImmutable();

	protected Color background = COLOR_VANILLA_BACKGROUND;
	protected Color borderTop = COLOR_VANILLA_BORDER.getFirst();
	protected Color borderBot = COLOR_VANILLA_BORDER.getSecond();
	protected int borderOffset = 2;

	public <T extends BoxElement> T withBackground(Color color) {
		this.background = color;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends BoxElement> T withBackground(int color) {
		return withBackground(new Color(color, true));
	}

	public <T extends BoxElement> T flatBorder(Color color) {
		this.borderTop = color;
		this.borderBot = color;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends BoxElement> T flatBorder(int color) {
		return flatBorder(new Color(color, true));
	}

	public <T extends BoxElement> T gradientBorder(Couple<Color> colors) {
		this.borderTop = colors.getFirst();
		this.borderBot = colors.getSecond();
		//noinspection unchecked
		return (T) this;
	}

	public <T extends BoxElement> T gradientBorder(Color top, Color bot) {
		this.borderTop = top;
		this.borderBot = bot;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends BoxElement> T gradientBorder(int top, int bot) {
		return gradientBorder(new Color(top, true), new Color(bot, true));
	}

	public <T extends BoxElement> T withBorderOffset(int offset) {
		this.borderOffset = offset;
		//noinspection unchecked
		return (T) this;
	}

	@Override
	public void render(GuiGraphics graphics) {
		renderBox(graphics);
	}

	//total box width = 1 * 2 (outer border) + 1 * 2 (inner color border) + 2 * borderOffset + width
	//defaults to 2 + 2 + 4 + 16 = 24px
	//batch everything together (one GUI draw) to save a bunch of gl calls over ScreenUtils
	protected void renderBox(GuiGraphics graphics) {
		/*
		*          _____________
		*        _|_____________|_
		*       | | ___________ | |
		*       | | |  |      | | |
		*       | | |  |      | | |
		*       | | |--*   |  | | |
		*       | | |      h  | | |
		*       | | |  --w-+  | | |
		*       | | |         | | |
		*       | | |_________| | |
		*       |_|_____________|_|
		*         |_____________|
		*
		* */
		PoseStack ms = graphics.pose();
		Matrix4f model = ms.last().pose();
		int f = borderOffset;
		Color c1 = background.copy().scaleAlpha(alpha);
		Color c2 = borderTop.copy().scaleAlpha(alpha);
		Color c3 = borderBot.copy().scaleAlpha(alpha);
		graphics.drawSpecial(buffers -> {
			VertexConsumer b = buffers.getBuffer(RenderType.gui());

			//outer top
			b.addVertex(model, x - f - 1, y - f - 2, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x - f - 1, y - f - 1, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x + f + 1 + width, y - f - 1, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x + f + 1 + width, y - f - 2, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			//outer left
			b.addVertex(model, x - f - 2, y - f - 1, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x - f - 2, y + f + 1 + height, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x - f - 1, y + f + 1 + height, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x - f - 1, y - f - 1, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			//outer bottom
			b.addVertex(model, x - f - 1, y + f + 1 + height, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x - f - 1, y + f + 2 + height, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x + f + 1 + width, y + f + 2 + height, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x + f + 1 + width, y + f + 1 + height, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			//outer right
			b.addVertex(model, x + f + 1 + width, y - f - 1, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x + f + 1 + width, y + f + 1 + height, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x + f + 2 + width, y + f + 1 + height, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x + f + 2 + width, y - f - 1, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			//inner background - also render behind the inner edges
			b.addVertex(model, x - f - 1, y - f - 1, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x - f - 1, y + f + 1 + height, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x + f + 1 + width, y + f + 1 + height, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			b.addVertex(model, x + f + 1 + width, y - f - 1, z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
			//inner top - includes corners
			b.addVertex(model, x - f - 1, y - f - 1, z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
			b.addVertex(model, x - f - 1, y - f, z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
			b.addVertex(model, x + f + 1 + width, y - f, z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
			b.addVertex(model, x + f + 1 + width, y - f - 1, z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
			//inner left - excludes corners
			b.addVertex(model, x - f - 1, y - f, z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
			b.addVertex(model, x - f - 1, y + f + height, z).setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
			b.addVertex(model, x - f, y + f + height, z).setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
			b.addVertex(model, x - f, y - f, z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
			//inner bottom - includes corners
			b.addVertex(model, x - f - 1, y + f + height, z).setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
			b.addVertex(model, x - f - 1, y + f + 1 + height, z).setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
			b.addVertex(model, x + f + 1 + width, y + f + 1 + height, z).setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
			b.addVertex(model, x + f + 1 + width, y + f + height, z).setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
			//inner right - excludes corners
			b.addVertex(model, x + f + width, y - f, z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
			b.addVertex(model, x + f + width, y + f + height, z).setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
			b.addVertex(model, x + f + 1 + width, y + f + height, z).setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
			b.addVertex(model, x + f + 1 + width, y - f, z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
		});
	}
}
