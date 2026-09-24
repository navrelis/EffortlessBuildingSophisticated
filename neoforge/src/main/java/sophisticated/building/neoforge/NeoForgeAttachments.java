package sophisticated.building.neoforge;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
 * implemented {@code INBTSerializable} (the attachment's compound holds {@link PowerLevel#SAVE_KEY}),
 * so existing player data keeps loading.
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

    // NeoForge 21.6+ serializes attachments through value inputs/outputs; each attachment gets its own child of the
    // holder's attachment compound, as the CompoundTag of the earlier serializer.
    private static final class PowerLevelSerializer implements IAttachmentSerializer<PowerLevel> {
        @Override
        public PowerLevel read(IAttachmentHolder holder, ValueInput input) {
            PowerLevel powerLevel = new PowerLevel();
            powerLevel.setPowerLevel(input.getIntOr(PowerLevel.SAVE_KEY, 0));
            return powerLevel;
        }

        @Override
        public boolean write(PowerLevel attachment, ValueOutput output) {
            output.putInt(PowerLevel.SAVE_KEY, attachment.getPowerLevel());
            return true;
        }
    }
}
