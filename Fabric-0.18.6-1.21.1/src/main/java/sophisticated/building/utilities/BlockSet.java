package sophisticated.building.utilities;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.AttachmentHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//Common
public class BlockSet extends HashMap<BlockPos, BlockEntry> implements Iterable<BlockEntry> {
    public static boolean logging = false; // Disabled by default to prevent lag from spam
    private static int limitWarningCooldown = 0; // Cooldown to prevent log spam

    public BlockPos firstPos;
    public BlockPos lastPos;
    public boolean skipFirst;

    public BlockSet() {
        super();
    }

    public BlockSet(BlockSet blockSet) {
        super(blockSet);
        this.firstPos = blockSet.firstPos;
        this.lastPos = blockSet.lastPos;
        this.skipFirst = blockSet.skipFirst;
    }

    public BlockSet(List<BlockEntry> blockEntries, BlockPos firstPos, BlockPos lastPos, boolean skipFirst) {
        super();
        for (BlockEntry blockEntry : blockEntries) {
            add(blockEntry);
        }
        this.firstPos = firstPos;
        this.lastPos = lastPos;
        this.skipFirst = skipFirst;
    }

    public void setStartPos(BlockEntry startPos) {
        clear();
        add(startPos);
        firstPos = startPos.blockPos;
        lastPos = startPos.blockPos;
    }

    public void add(BlockEntry blockEntry) {
        if (!containsKey(blockEntry.blockPos)) {
            //check if we are clientside
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                if (!ClientSide.isFull(this))
                    put(blockEntry.blockPos, blockEntry);
            } else {
                put(blockEntry.blockPos, blockEntry);
            }
        } else {
            if (logging) SophisticatedBuilding.log("BlockSet already contains block at " + blockEntry.blockPos);
        }
    }

    public HashSet<BlockPos> getCoordinates() {
        return new HashSet<>(keySet());
    }

    public BlockEntry getFirstBlockEntry() {
        return get(firstPos);
    }

    public BlockEntry getLastBlockEntry() {
        return get(lastPos);
    }

    @NotNull
    @Override
    public Iterator<BlockEntry> iterator() {
        return this.values().iterator();
    }

    public static void encode(FriendlyByteBuf buf, BlockSet block) {
        List<BlockEntry> entries = block.values().stream().filter(be -> !be.invalid).toList();

        // 1. Build Palette
        List<Pair<BlockState, Item>> palette = new ArrayList<>();
        Map<Pair<BlockState, Item>, Integer> paletteMap = new HashMap<>();

        for (BlockEntry entry : entries) {
            Pair<BlockState, Item> pair = Pair.of(entry.newBlockState, entry.item);
            if (!paletteMap.containsKey(pair)) {
                paletteMap.put(pair, palette.size());
                palette.add(pair);
            }
        }

        // 2. Write Palette
        buf.writeVarInt(palette.size());
        for (Pair<BlockState, Item> pair : palette) {
            buf.writeNullable(pair.getFirst(), (buffer, state) -> buffer.writeNbt(NbtUtils.writeBlockState(state)));
            buf.writeVarInt(Item.getId(pair.getSecond()));
        }

        // 3. Write Blocks
        buf.writeVarInt(entries.size());
        BlockPos lastPos = BlockPos.ZERO;

        for (BlockEntry entry : entries) {
            // Delta compression for position
            BlockPos pos = entry.blockPos;
            buf.writeVarInt(pos.getX() - lastPos.getX());
            buf.writeVarInt(pos.getY() - lastPos.getY());
            buf.writeVarInt(pos.getZ() - lastPos.getZ());
            lastPos = pos;

            // Palette index
            Pair<BlockState, Item> pair = Pair.of(entry.newBlockState, entry.item);
            buf.writeVarInt(paletteMap.get(pair));
        }

        buf.writeBlockPos(block.firstPos);
        buf.writeBlockPos(block.lastPos);
        buf.writeBoolean(block.skipFirst);
    }

    public static BlockSet decode(FriendlyByteBuf buf) {
        // 1. Read Palette
        int paletteSize = buf.readVarInt();
        List<Pair<BlockState, Item>> palette = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            BlockState state = buf.readNullable(buffer -> {
                var nbt = buffer.readNbt();
                return nbt == null ? null : NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), nbt);
            });
            Item item = Item.byId(buf.readVarInt());
            palette.add(Pair.of(state, item));
        }

        // 2. Read Blocks
        int blockSize = buf.readVarInt();
        List<BlockEntry> entries = new ArrayList<>(blockSize);
        BlockPos lastPos = BlockPos.ZERO;

        for (int i = 0; i < blockSize; i++) {
            int dx = buf.readVarInt();
            int dy = buf.readVarInt();
            int dz = buf.readVarInt();
            BlockPos pos = lastPos.offset(dx, dy, dz);
            lastPos = pos;

            int paletteIndex = buf.readVarInt();
            Pair<BlockState, Item> pair = palette.get(paletteIndex);

            BlockEntry entry = new BlockEntry(pos);
            entry.newBlockState = pair.getFirst();
            entry.item = pair.getSecond();
            entries.add(entry);
        }

        BlockPos firstPos = buf.readBlockPos();
        BlockPos lastPosMeta = buf.readBlockPos();
        boolean skipFirst = buf.readBoolean();

        return new BlockSet(entries, firstPos, lastPosMeta, skipFirst);
    }

    @Environment(EnvType.CLIENT)
    public static class ClientSide {
        private static int cachedLimit = -1;
        private static long lastLimitCheck = 0;
        
        public static boolean isFull(BlockSet blockSet) {
            //Limit number of blocks you can place
            //Cache the limit for 20 ticks to avoid repeated calls
            long currentTime = System.currentTimeMillis();
            if (cachedLimit < 0 || currentTime - lastLimitCheck > 1000) { // Refresh every second
                cachedLimit = AttachmentHandler.getMaxBlocksPlacedAtOnce(net.minecraft.client.Minecraft.getInstance().player, false);
                lastLimitCheck = currentTime;
            }
            
            if (blockSet.size() >= cachedLimit) {
                // Only log once per second to prevent spam
                if (logging && limitWarningCooldown <= 0) {
                    SophisticatedBuilding.log("BlockSet limit reached (" + cachedLimit + " blocks max).");
                    limitWarningCooldown = 20; // 20 ticks = 1 second
                }
                return true;
            }
            return false;
        }
        
        public static void tickCooldown() {
            if (limitWarningCooldown > 0) limitWarningCooldown--;
        }
        
        public static void invalidateCache() {
            cachedLimit = -1;
        }
    }
}

