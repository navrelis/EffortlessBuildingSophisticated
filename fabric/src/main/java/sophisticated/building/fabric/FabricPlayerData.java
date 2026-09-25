package sophisticated.building.fabric;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.PowerLevel;

import javax.annotation.Nullable;

/**
 * The mod's per-player data on Fabric, kept on the player object (fields added by {@code PlayerDataMixin}) and saved
 * with the player in the world save, like NeoForge's power level attachment and persistent data: it survives a server
 * restart, and a new player object (another singleplayer world) starts without the last one's data. Death and respawn
 * copy it to the new player object ({@code ServerPlayerEvents.COPY_FROM}).
 */
public final class FabricPlayerData {

    /** The player NBT key of the mod's data. */
    public static final String TAG = SophisticatedBuilding.MODID;
    private static final String POWER_LEVEL = "power_level";
    private static final String DATA = "data";

    private FabricPlayerData() {
    }

    /** Implemented by {@link Player} through {@code PlayerDataMixin}. */
    public interface Holder {
        @Nullable
        PowerLevel sophisticatedbuilding$getPowerLevel();

        void sophisticatedbuilding$setPowerLevel(@Nullable PowerLevel powerLevel);

        CompoundTag sophisticatedbuilding$getData();

        void sophisticatedbuilding$setData(CompoundTag data);
    }

    public static Holder of(Player player) {
        return (Holder) player;
    }

    public static PowerLevel getOrCreatePowerLevel(Player player) {
        Holder holder = of(player);
        PowerLevel powerLevel = holder.sophisticatedbuilding$getPowerLevel();
        if (powerLevel == null) {
            powerLevel = new PowerLevel();
            holder.sophisticatedbuilding$setPowerLevel(powerLevel);
        }
        return powerLevel;
    }

    /**
     * The mod's tag in the player's save (Minecraft 1.21.6+ saves entities through {@code ValueOutput}: the mixin stores
     * it under {@link #TAG} with {@code CompoundTag.CODEC}), or null when there is nothing to save.
     */
    @Nullable
    public static CompoundTag save(Player player) {
        Holder holder = of(player);
        CompoundTag tag = new CompoundTag();
        PowerLevel powerLevel = holder.sophisticatedbuilding$getPowerLevel();
        if (powerLevel != null) {
            tag.put(POWER_LEVEL, powerLevel.serializeNBT(player.registryAccess()));
        }
        CompoundTag data = holder.sophisticatedbuilding$getData();
        if (!data.isEmpty()) {
            tag.put(DATA, data.copy());
        }
        return tag.isEmpty() ? null : tag;
    }

    /** Reads the data back from the mod's tag of the player's save (empty when the save has none). */
    public static void load(Player player, CompoundTag tag) {
        Holder holder = of(player);
        tag.getCompound(POWER_LEVEL).ifPresent(saved -> {
            PowerLevel powerLevel = new PowerLevel();
            powerLevel.deserializeNBT(player.registryAccess(), saved);
            holder.sophisticatedbuilding$setPowerLevel(powerLevel);
        });
        holder.sophisticatedbuilding$setData(tag.getCompoundOrEmpty(DATA).copy());
    }

    /** Death, respawn, leaving the End: the new player object takes over the old one's data. */
    public static void copy(Player from, Player to) {
        Holder source = of(from);
        Holder target = of(to);
        PowerLevel powerLevel = source.sophisticatedbuilding$getPowerLevel();
        if (powerLevel != null) {
            PowerLevel copy = new PowerLevel();
            copy.setPowerLevel(powerLevel.getPowerLevel());
            target.sophisticatedbuilding$setPowerLevel(copy);
        }
        target.sophisticatedbuilding$setData(source.sophisticatedbuilding$getData().copy());
    }
}
