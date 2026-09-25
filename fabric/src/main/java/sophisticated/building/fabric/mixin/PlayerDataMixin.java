package sophisticated.building.fabric.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.fabric.FabricPlayerData;

/** The mod's per-player data on the player object, saved and loaded with the player (see {@link FabricPlayerData}). */
@Mixin(Player.class)
abstract class PlayerDataMixin implements FabricPlayerData.Holder {

    @Unique
    private PowerLevel sophisticatedbuilding$powerLevel;
    @Unique
    private CompoundTag sophisticatedbuilding$data = new CompoundTag();

    @Override
    public PowerLevel sophisticatedbuilding$getPowerLevel() {
        return sophisticatedbuilding$powerLevel;
    }

    @Override
    public void sophisticatedbuilding$setPowerLevel(PowerLevel powerLevel) {
        sophisticatedbuilding$powerLevel = powerLevel;
    }

    @Override
    public CompoundTag sophisticatedbuilding$getData() {
        return sophisticatedbuilding$data;
    }

    @Override
    public void sophisticatedbuilding$setData(CompoundTag data) {
        sophisticatedbuilding$data = data;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void sophisticatedbuilding$save(ValueOutput output, CallbackInfo ci) {
        FabricPlayerData.save((Player) (Object) this, output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void sophisticatedbuilding$load(ValueInput input, CallbackInfo ci) {
        FabricPlayerData.load((Player) (Object) this, input);
    }
}
