package sophisticated.building.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.buildmodifier.BaseModifier;
import sophisticated.building.buildmodifier.Mirror;
import sophisticated.building.buildmodifier.RadialMirror;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.List;

public class ModifierRenderer {

	protected static final Color colorX = new Color(255, 72, 52);
	protected static final Color colorY = new Color(67, 204, 51);
	protected static final Color colorZ = new Color(52, 247, 255);
	protected static final Color colorRadial = new Color(52, 247, 255);
	protected static final int lineAlpha = 200;
	protected static final int planeAlpha = 50;
	protected static final Vec3 epsilon = new Vec3(0.001, 0.001, 0.001); //prevents z-fighting

	public static void render(PoseStack ms, BufferSource buffer) {
        List<BaseModifier> modifierSettingsList = SophisticatedBuildingClient.BUILD_MODIFIERS.getModifierSettingsList();

        for (BaseModifier modifierSettings : modifierSettingsList) {
            if (modifierSettings == null) continue;
            if (modifierSettings instanceof Mirror) {
                renderMirror(ms, buffer, (Mirror) modifierSettings);
            } else if (modifierSettings instanceof RadialMirror) {
                renderRadialMirror(ms, buffer, (RadialMirror) modifierSettings);
            }
        }
    }

    //Mirror lines and areas
    private static void renderMirror(PoseStack ms, BufferSource buffer, Mirror m) {

        if (m != null && m.enabled && (m.mirrorX || m.mirrorY || m.mirrorZ)) {
            Vec3 pos = m.position.add(epsilon);
            int radius = m.radius;

            if (m.mirrorX) {
                Vec3 posA = new Vec3(pos.x, pos.y - radius, pos.z - radius);
                Vec3 posB = new Vec3(pos.x, pos.y + radius, pos.z + radius);

                drawMirrorPlane(ms, buffer, posA, posB, colorX, m.drawLines, m.drawPlanes, true);
            }
            if (m.mirrorY) {
                Vec3 posA = new Vec3(pos.x - radius, pos.y, pos.z - radius);
                Vec3 posB = new Vec3(pos.x + radius, pos.y, pos.z + radius);

                drawMirrorPlaneY(ms, buffer, posA, posB, colorY, m.drawLines, m.drawPlanes);
            }
            if (m.mirrorZ) {
                Vec3 posA = new Vec3(pos.x - radius, pos.y - radius, pos.z);
                Vec3 posB = new Vec3(pos.x + radius, pos.y + radius, pos.z);

                drawMirrorPlane(ms, buffer, posA, posB, colorZ, m.drawLines, m.drawPlanes, true);
            }

            //Draw axis coordinated colors if two or more axes are enabled
            //(If only one is enabled the lines are that planes color)
            if (m.drawLines && ((m.mirrorX && m.mirrorY) || (m.mirrorX && m.mirrorZ) || (m.mirrorY && m.mirrorZ))) {
                drawMirrorLines(ms, buffer, m);
            }
        }
    }

    //Radial mirror lines and areas
    private static void renderRadialMirror(PoseStack ms, BufferSource buffer, RadialMirror r) {

		if (r != null && r.enabled) {
			Vec3 pos = r.position.add(epsilon);
			int radius = r.radius;

			float angle = 2f * ((float) Math.PI) / r.slices;
			Vec3 relStartVec = new Vec3(radius, 0, 0);
			if (r.slices % 4 == 2) relStartVec = relStartVec.yRot(angle / 2f);

			for (int i = 0; i < r.slices; i++) {
				Vec3 relNewVec = relStartVec.yRot(angle * i);
				Vec3 newVec = pos.add(relNewVec);

				Vec3 posA = new Vec3(pos.x, pos.y - radius, pos.z);
				Vec3 posB = new Vec3(newVec.x, pos.y + radius, newVec.z);
				drawMirrorPlane(ms, buffer, posA, posB, colorRadial, r.drawLines, r.drawPlanes, false);
			}
		}
	}

	protected static void drawMirrorPlane(PoseStack ms, BufferSource renderTypeBuffer, Vec3 posA, Vec3 posB, Color c, boolean drawLines, boolean drawPlanes, boolean drawVerticalLines) {

//        GL11.glColor4d(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
		Matrix4f matrixPos = ms.last().pose();

		if (drawPlanes) {
			VertexConsumer buffer = RenderHandler.beginPlanes(renderTypeBuffer);

			buffer.addVertex(matrixPos, (float) posA.x, (float) posA.y, (float) posA.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
			buffer.addVertex(matrixPos, (float) posA.x, (float) posB.y, (float) posA.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
			buffer.addVertex(matrixPos, (float) posB.x, (float) posA.y, (float) posB.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
			buffer.addVertex(matrixPos, (float) posB.x, (float) posB.y, (float) posB.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
			//backface (using triangle strip)
			buffer.addVertex(matrixPos, (float) posA.x, (float) posA.y, (float) posA.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
			buffer.addVertex(matrixPos, (float) posA.x, (float) posB.y, (float) posA.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);

			RenderHandler.endPlanes(renderTypeBuffer);
		}

		if (drawLines) {
			VertexConsumer buffer = RenderHandler.beginLines(renderTypeBuffer);

			Vec3 middle = posA.add(posB).scale(0.5);
			line(buffer, ms.last(), posA.x, middle.y, posA.z, posB.x, middle.y, posB.z, c);
			if (drawVerticalLines) {
				line(buffer, ms.last(), middle.x, posA.y, middle.z, middle.x, posB.y, middle.z, c);
			}

			RenderHandler.endLines(renderTypeBuffer);
		}
	}

	protected static void drawMirrorPlaneY(PoseStack matrixStack, BufferSource renderTypeBuffer, Vec3 posA, Vec3 posB, Color c, boolean drawLines, boolean drawPlanes) {

//        GL11.glColor4d(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
		Matrix4f matrixPos = matrixStack.last().pose();

		if (drawPlanes) {
			VertexConsumer buffer = RenderHandler.beginPlanes(renderTypeBuffer);

			buffer.addVertex(matrixPos, (float) posA.x, (float) posA.y, (float) posA.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
			buffer.addVertex(matrixPos, (float) posA.x, (float) posA.y, (float) posB.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
			buffer.addVertex(matrixPos, (float) posB.x, (float) posA.y, (float) posA.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
			buffer.addVertex(matrixPos, (float) posB.x, (float) posA.y, (float) posB.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
			//backface (using triangle strip)
			buffer.addVertex(matrixPos, (float) posA.x, (float) posA.y, (float) posA.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);
			buffer.addVertex(matrixPos, (float) posA.x, (float) posA.y, (float) posB.z).setColor(c.getRed(), c.getGreen(), c.getBlue(), planeAlpha);

			RenderHandler.endPlanes(renderTypeBuffer);
		}

		if (drawLines) {
			VertexConsumer buffer = RenderHandler.beginLines(renderTypeBuffer);

			Vec3 middle = posA.add(posB).scale(0.5);
			line(buffer, matrixStack.last(), middle.x, middle.y, posA.z, middle.x, middle.y, posB.z, c);
			line(buffer, matrixStack.last(), posA.x, middle.y, middle.z, posB.x, middle.y, middle.z, c);

			RenderHandler.endLines(renderTypeBuffer);
		}
	}

	protected static void drawMirrorLines(PoseStack matrixStack, BufferSource renderTypeBuffer, Mirror m) {

//        GL11.glColor4d(100, 100, 100, 255);
		VertexConsumer buffer = RenderHandler.beginLines(renderTypeBuffer);
		PoseStack.Pose pose = matrixStack.last();

		Vec3 pos = m.position.add(epsilon);

		line(buffer, pose, pos.x - m.radius, pos.y, pos.z, pos.x + m.radius, pos.y, pos.z, colorX);
		line(buffer, pose, pos.x, pos.y - m.radius, pos.z, pos.x, pos.y + m.radius, pos.z, colorY);
		line(buffer, pose, pos.x, pos.y, pos.z - m.radius, pos.x, pos.y, pos.z + m.radius, colorZ);

		RenderHandler.endLines(renderTypeBuffer);
	}

	/**
	 * One mirror line. The line shader draws a camera-facing quad along the direction passed as vertex normal, so
	 * both vertices carry the normalized line direction.
	 */
	private static void line(VertexConsumer buffer, PoseStack.Pose pose, double x1, double y1, double z1, double x2, double y2, double z2, Color c) {
		Vector3f direction = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
		buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha)
				.setNormal(pose, direction.x(), direction.y(), direction.z());
		buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(c.getRed(), c.getGreen(), c.getBlue(), lineAlpha)
				.setNormal(pose, direction.x(), direction.y(), direction.z());
	}
}

