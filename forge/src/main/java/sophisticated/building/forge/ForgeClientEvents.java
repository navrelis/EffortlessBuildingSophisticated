package sophisticated.building.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.gui.GuiGraphics;
import sophisticated.building.render.RenderHandler;

/**
 * Client game events, forwarded to the loader-neutral handlers.
 */
@Mod.EventBusSubscriber(modid = SophisticatedBuilding.MODID, value = Dist.CLIENT)
public class ForgeClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ClientEvents.onClientTickPre();
        } else {
            ClientEvents.onClientTickPost();
            sophisticated.building.create.events.ClientEvents.onTick();
        }
    }

    @SubscribeEvent
    public static void onKeyPress(InputEvent.KeyInputEvent event) {
        ClientEvents.onKeyPress();
    }

    @SubscribeEvent
    public static void onGuiOpen(ScreenOpenEvent event) {
        ClientEvents.onGuiOpen(event.getScreen());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        ClientEvents.onLoggingOut();
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
        ClientEvents.onLoggingIn();
    }

    @SubscribeEvent
    public static void onLoadWorld(WorldEvent.Load event) {
        sophisticated.building.create.events.ClientEvents.onLoadWorld(event.getWorld());
    }

    @SubscribeEvent
    public static void onUnloadWorld(WorldEvent.Unload event) {
        sophisticated.building.create.events.ClientEvents.onUnloadWorld(event.getWorld());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelLastEvent event) {
        // Forge 1.18.1 has no render stages (RenderLevelStageEvent arrives with Forge 40 for 1.18.2): once the whole
        // level is drawn, first the block previews, mirror/array lines and ghost blocks, then the outlines, in the same
        // order as the stages on the other loaders.
        RenderHandler.onRenderWorld(event.getPoseStack());
        RenderHandler.onRenderOutlines(event.getPoseStack());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGameOverlayEvent.Post event) {
        // Once per frame, after the whole HUD (Forge 1.18.1 also posts this event for single HUD elements)
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            RenderHandler.onRenderGui(new GuiGraphics(event.getMatrixStack()));
        }
    }
}
