package sophisticated.building.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * One of the mod's network payloads: Minecraft 1.20.1 has no vanilla {@code CustomPacketPayload} yet, so this is
 * its counterpart (the same two methods). A payload writes its body to a {@link FriendlyByteBuf}, is read back by
 * its reading constructor and is identified by its {@link #id()}, the same ids as on the other Minecraft versions.
 */
public interface ModPayload {

    void write(FriendlyByteBuf buf);

    ResourceLocation id();
}
