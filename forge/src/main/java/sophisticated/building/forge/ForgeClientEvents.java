package sophisticated.building.forge;

import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.FramePassManager;
import net.minecraftforge.client.event.AddFramePassEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
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

    /**
     * Forge 58 has no render stage event, but again a frame pass event (Forge 55 had none): one frame pass after the
     * vanilla ones (inserted after the late debug pass, so after the translucent blocks, particles, clouds and weather)
     * draws the block previews, mirror/array lines and ghost blocks, then the outlines, into the main target. The camera
     * rotation is already on the model view stack while the frame graph runs, so the handlers start from an identity
     * pose (as on Fabric and NeoForge).
     */
    @SubscribeEvent
    public static void onAddFramePass(AddFramePassEvent event) {
        event.addPass(SophisticatedBuilding.asResource("previews"), new FramePassManager.PassDefinition() {
            @Override
            public void extracts(LevelTargetBundle bundle, FramePass pass, DeltaTracker tracker) {
                bundle.main = pass.readsAndWrites(bundle.main);
            }

            @Override
            public void executes() {
                RenderHandler.onRenderWorld(new PoseStack());
                RenderHandler.onRenderOutlines(new PoseStack());
            }
        });
    }
}
