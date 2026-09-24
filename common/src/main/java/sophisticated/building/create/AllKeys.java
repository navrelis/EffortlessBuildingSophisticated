package sophisticated.building.create;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class AllKeys {

	public static boolean isKeyDown(int key) {
		return InputConstants.isKeyDown(Minecraft.getInstance()
			.getWindow(), key);
	}

	public static boolean isMouseButtonDown(int button) {
		return GLFW.glfwGetMouseButton(Minecraft.getInstance()
			.getWindow()
			.handle(), button) == 1;
	}

	public static boolean ctrlDown() {
		return Minecraft.getInstance().hasControlDown();
	}

	public static boolean shiftDown() {
		return Minecraft.getInstance().hasShiftDown();
	}

	public static boolean altDown() {
		return Minecraft.getInstance().hasAltDown();
	}

}
