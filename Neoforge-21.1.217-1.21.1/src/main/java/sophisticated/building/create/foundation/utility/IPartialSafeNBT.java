package sophisticated.building.create.foundation.utility;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

public interface IPartialSafeNBT {
	void writeSafe(CompoundTag compound, RegistryAccess registryAccess);
}
