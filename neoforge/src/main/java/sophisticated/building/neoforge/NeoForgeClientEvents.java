package sophisticated.building.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.render.RenderHandler;

/**
 * Client game events, forwarded to the loader-neutral handlers.
 */
@EventBusSubscriber(modid = SophisticatedBuilding.MODID, value = Dist.CLIENT)
public class NeoForgeClientEvents {

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        ClientEvents.onClientTickPre();
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        ClientEvents.onClientTickPost();
        sophisticated.building.create.events.ClientEvents.onTick();
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onKeyPress(InputEvent.Key event) {
        ClientEvents.onKeyPress();
    }

    @SubscribeEvent
    public static void onGuiOpen(ScreenEvent.Opening event) {
        ClientEvents.onGuiOpen(event.getNewScreen());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientEvents.onLoggingOut();
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientEvents.onLoggingIn();
    }

    @SubscribeEvent
    public static void onLoadWorld(LevelEvent.Load event) {
        sophisticated.building.create.events.ClientEvents.onLoadWorld(event.getLevel());
    }

    @SubscribeEvent
    public static void onUnloadWorld(LevelEvent.Unload event) {
        sophisticated.building.create.events.ClientEvents.onUnloadWorld(event.getLevel());
    }

    // Block previews, mirror/array lines and ghost blocks after the translucent blocks; the outlines
    // after the particles, where Catnip drew its outliner on NeoForge (one event per stage since NeoForge 21.6; since
    // 26.1 the particles are drawn in two passes, the outlines follow the second, translucent one).
    @SubscribeEvent
    public static void onRenderLevelAfterTranslucentBlocks(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        RenderHandler.onRenderWorld(event.getPoseStack());
    }

    @SubscribeEvent
    public static void onRenderLevelAfterParticles(RenderLevelStageEvent.AfterTranslucentParticles event) {
        RenderHandler.onRenderOutlines(event.getPoseStack());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        RenderHandler.onRenderGui(event.getGuiGraphics());
    }
}
