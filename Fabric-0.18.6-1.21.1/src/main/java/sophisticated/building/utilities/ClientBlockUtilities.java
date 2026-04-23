package sophisticated.building.utilities;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sophisticated.building.attachment.AttachmentHandler;

@Environment(EnvType.CLIENT)
public class ClientBlockUtilities {

    public static boolean determineIfLookingAtInteractiveObject(Minecraft mc, Level level) {
        if (mc == null || level == null || mc.hitResult == null) {
            return false;
        }

        if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
            var blockHitResult = (BlockHitResult) mc.hitResult;
            var blockPos = blockHitResult.getBlockPos();
            var blockState = level.getBlockState(blockPos);

            // Menu providers capture true GUI-bearing blocks (chests, crafting table, etc.).
            return blockState.getMenuProvider(level, blockPos) != null;
        }

        return mc.hitResult.getType() == HitResult.Type.ENTITY;
    }

    public static void playSoundIfFurtherThanNormal(Player player, BlockEntry blockEntry, boolean breaking) {

        if (Minecraft.getInstance().hitResult != null && Minecraft.getInstance().hitResult.getType() == HitResult.Type.BLOCK)
            return;

        if (blockEntry == null || blockEntry.newBlockState == null)
            return;

        SoundType soundType = blockEntry.newBlockState.getSoundType();
        SoundEvent soundEvent = breaking ? soundType.getBreakSound() : soundType.getPlaceSound();
        player.level().playSound(player, player.blockPosition(), soundEvent, SoundSource.BLOCKS, 0.6f, soundType.getPitch());
    }

    public static BlockHitResult getLookingAtFar(Player player) {
        Level world = player.level();

        //base distance off of player ability (config)
        float raytraceRange = AttachmentHandler.getPlacementReach(player, false);

        Vec3 look = player.getLookAngle();
        Vec3 start = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        Vec3 end = new Vec3(player.getX() + look.x * raytraceRange, player.getY() + player.getEyeHeight() + look.y * raytraceRange, player.getZ() + look.z * raytraceRange);

        return world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }
}

