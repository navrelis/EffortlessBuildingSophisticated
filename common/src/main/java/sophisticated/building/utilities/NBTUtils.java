package sophisticated.building.utilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * NBT helper utilities to replace Catnip's NBTHelper.
 */
public class NBTUtils {

    /**
     * Writes a list of objects to a ListTag using the provided serializer.
     * 
     * @param list The list of objects to serialize
     * @param serializer Function to convert each object to a CompoundTag
     * @return A ListTag containing all serialized objects
     */
    public static <T> ListTag writeCompoundList(Collection<T> list, Function<T, CompoundTag> serializer) {
        ListTag listTag = new ListTag();
        for (T item : list) {
            listTag.add(serializer.apply(item));
        }
        return listTag;
    }

    /**
     * Reads a list of objects from a ListTag using the provided deserializer.
     * 
     * @param listTag The ListTag to read from
     * @param deserializer Function to convert each CompoundTag to an object
     * @return A list of deserialized objects
     */
    public static <T> List<T> readCompoundList(ListTag listTag, Function<CompoundTag, T> deserializer) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            Tag tag = listTag.get(i);
            if (tag instanceof CompoundTag compoundTag) {
                result.add(deserializer.apply(compoundTag));
            }
        }
        return result;
    }
}
