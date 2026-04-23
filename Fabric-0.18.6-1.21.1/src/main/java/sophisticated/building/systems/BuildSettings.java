package sophisticated.building.systems;

import net.minecraft.client.Minecraft;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.network.message.IsQuickReplacingPacket;

@Environment(EnvType.CLIENT)
public class BuildSettings {
    public enum ReplaceMode {
        ONLY_AIR,
        BLOCKS_AND_AIR,
        ONLY_BLOCKS,
        FILTERED_BY_OFFHAND
    }

    private ReplaceMode replaceMode = ReplaceMode.ONLY_AIR;
    private boolean protectTileEntities = true;

    public boolean isQuickReplacing() {
        return getReplaceMode() != ReplaceMode.ONLY_AIR;
    }

    public void setReplaceMode(ReplaceMode replaceMode) {
        this.replaceMode = replaceMode;
        ClientPlayNetworking.send(new IsQuickReplacingPacket(isQuickReplacing()));
    }

    public ReplaceMode getReplaceMode() {
        if (!canReplaceBlocks()) return ReplaceMode.ONLY_AIR;
        return replaceMode;
    }

    public ModeOptions.ActionEnum getReplaceModeActionEnum() {
        return switch (getReplaceMode()) {
            case ONLY_AIR -> ModeOptions.ActionEnum.REPLACE_ONLY_AIR;
            case BLOCKS_AND_AIR -> ModeOptions.ActionEnum.REPLACE_BLOCKS_AND_AIR;
            case ONLY_BLOCKS -> ModeOptions.ActionEnum.REPLACE_ONLY_BLOCKS;
            case FILTERED_BY_OFFHAND -> ModeOptions.ActionEnum.REPLACE_FILTERED_BY_OFFHAND;
        };
    }

    public void toggleProtectTileEntities() {
        protectTileEntities = !protectTileEntities;
    }

    public boolean shouldReplaceAir() {
        return getReplaceMode() == ReplaceMode.ONLY_AIR || getReplaceMode() == ReplaceMode.BLOCKS_AND_AIR;
    }

    public boolean shouldReplaceBlocks() {
        return getReplaceMode() == ReplaceMode.ONLY_BLOCKS || getReplaceMode() == ReplaceMode.BLOCKS_AND_AIR;
    }

    public boolean shouldReplaceFiltered() {
        return getReplaceMode() == ReplaceMode.FILTERED_BY_OFFHAND;
    }

    public boolean shouldProtectTileEntities() {
        return protectTileEntities;
    }

    public boolean shouldOffsetStartPosition() {
        return getReplaceMode() != ReplaceMode.ONLY_AIR;
    }

    private boolean canReplaceBlocks(){
        return Minecraft.getInstance().player != null && AttachmentHandler.canReplaceBlocks(Minecraft.getInstance().player);
    }
}

