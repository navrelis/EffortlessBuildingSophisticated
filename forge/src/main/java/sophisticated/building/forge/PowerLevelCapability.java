package sophisticated.building.forge;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.attachment.PowerLevel;

/**
 * The player's {@code sophisticatedbuilding:power_level} capability, saved with the player: Forge's
 * counterpart of the NeoForge attachment, with the same content. Like the attachment, the power level
 * only exists once it was read or set, and a set stores the given instance.
 */
@Mod.EventBusSubscriber(modid = SophisticatedBuilding.MODID)
public final class PowerLevelCapability {

    private static final Capability<Data> POWER_LEVEL = CapabilityManager.get(new CapabilityToken<>() {
    });

    private PowerLevelCapability() {
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(AttachmentHandler.POWER_LEVEL_CAP, new Provider());
        }
    }

    /** The player's power level, created with defaults on first access. */
    public static PowerLevel get(Player player) {
        Data data = data(player);
        if (data == null) {
            return new PowerLevel();
        }
        if (data.powerLevel == null) {
            data.powerLevel = new PowerLevel();
        }
        return data.powerLevel;
    }

    public static void set(Player player, PowerLevel powerLevel) {
        Data data = data(player);
        if (data != null) {
            data.powerLevel = powerLevel;
        }
    }

    public static boolean has(Player player) {
        Data data = data(player);
        return data != null && data.powerLevel != null;
    }

    @Nullable
    private static Data data(Player player) {
        return player.getCapability(POWER_LEVEL).resolve().orElse(null);
    }

    /** The capability value: the power level, null until it is first read or set. */
    @AutoRegisterCapability
    public static final class Data {
        @Nullable
        private PowerLevel powerLevel;
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final Data data = new Data();
        private final LazyOptional<Data> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            return POWER_LEVEL.orEmpty(capability, optional);
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            return data.powerLevel == null ? new CompoundTag() : data.powerLevel.serializeNBT(provider);
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
            if (!tag.isEmpty()) {
                PowerLevel powerLevel = new PowerLevel();
                powerLevel.deserializeNBT(provider, tag);
                data.powerLevel = powerLevel;
            }
        }
    }
}
