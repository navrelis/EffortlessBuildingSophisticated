package sophisticated.building.neoforge;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.PowerLevel;

import java.util.function.Supplier;

/**
 * The player's {@code sophisticatedbuilding:power_level} attachment, saved with the player. The
 * serializer writes exactly what {@code AttachmentType.serializable} wrote while {@link PowerLevel}
 * implemented {@code INBTSerializable}, so existing player data keeps loading.
 */
public final class NeoForgeAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SophisticatedBuilding.MODID);

    public static final Supplier<AttachmentType<PowerLevel>> POWER_LEVEL = ATTACHMENT_TYPES.register("power_level",
            () -> AttachmentType.builder(PowerLevel::new).serialize(new PowerLevelSerializer()).build());

    private NeoForgeAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    private static final class PowerLevelSerializer implements IAttachmentSerializer<CompoundTag, PowerLevel> {
        @Override
        public PowerLevel read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            PowerLevel powerLevel = new PowerLevel();
            powerLevel.deserializeNBT(provider, tag);
            return powerLevel;
        }

        @Override
        public CompoundTag write(PowerLevel attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT(provider);
        }
    }
}
