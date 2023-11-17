package baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class BlockUtils {

    private static transient Map<String, Block> resourceCache = new HashMap<>();

    public static String blockToString(Block block) {
        ResourceLocation loc = Block.blockRegistry.getNameForObject(block);
        String name = loc.getResourcePath();
        if (!loc.getResourceDomain().equals("minecraft")) {
            name = loc.toString();
        }
        return name;
    }

    public static Block stringToBlockRequired(String name) {
        Block block = stringToBlockNullable(name);

        if (block == null) {
            throw new IllegalArgumentException(String.format("Invalid block name %s", name));
        }

        return block;
    }

    public static Block stringToBlockNullable(String name) {
        Block block = resourceCache.get(name);
        if (block != null) {
            return block;
        }
        if (resourceCache.containsKey(name)) {
            return null;
        }
        block = Block.getBlockFromName(name.contains(":") ? name : "minecraft:" + name);
        Map<String, Block> copy = new HashMap<>(resourceCache);
        copy.put(name, block);
        resourceCache = copy;
        return block;
    }

    private BlockUtils() {}
}
