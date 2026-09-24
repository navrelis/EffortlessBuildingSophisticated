package sophisticated.building.buildmodifier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.utilities.BlockSet;

public abstract class BaseModifier {
    public boolean enabled = true;

    public abstract void findCoordinates(BlockSet blocks, Player player);

    public abstract void onPowerLevelChanged(int powerLevel);

    public CompoundTag serializeNBT() {
        CompoundTag compound = new CompoundTag();
        compound.putString("type", this.getClass().getSimpleName());
        compound.putBoolean("enabled", enabled);
        return compound;
    }

    public void deserializeNBT(CompoundTag compound) {
        enabled = compound.getBoolean("enabled");
    }
}
