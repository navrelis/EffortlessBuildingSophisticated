package sophisticated.building.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.render.RenderHandler;

/**
 * Client game events, forwarded to the loader-neutral handlers. The HUD layers are added in
 * {@link SophisticatedBuildingForgeClient}.
 */
@Mod.EventBusSubscriber(modid = SophisticatedBuilding.MODID, value = Dist.CLIENT)
public class ForgeClientEvents {

    @SubscribeEvent
    public static void onClientTickPre(TickEvent.ClientTickEvent.Pre event) {
        ClientEvents.onClientTickPre();
    }

    @SubscribeEvent
    public static void onClientTickPost(TickEvent.ClientTickEvent.Post event) {
        ClientEvents.onClientTickPost();
        sophisticated.building.create.events.ClientEvents.onTick();
    }

    @SubscribeEvent
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

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Block previews, mirror/array lines and ghost blocks after the translucent blocks; the outlines
        // after the particles, as on NeoForge. The camera rotation is already on the model view stack at
        // both stages, so the handlers start from an identity pose (NeoForge passes the same).
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            RenderHandler.onRenderWorld(new PoseStack());
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            RenderHandler.onRenderOutlines(new PoseStack());
        }
    }
}
