package carpet.helpers;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IntIdentityHashBiMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockStateFlatteningMap {
    public static final Map<Map<String, String>, Integer> DEFLATTENING_MAP = new HashMap<>(4096);
    public static final Map<String, Integer> CACHED_MAPPINGS = new HashMap<>(514);

    private static final Pattern BBS_PATTERN = Pattern.compile("(?<block>[a-zA-z:_]+)(\\[(?<props>.+)\\])");
    private static final Pattern PROP_PATTERN = Pattern.compile("(?<key>[a-zA-z:_]+)=(?<value>[a-zA-z:_]+)");

    /*
    public static String toDefaultPropertyValue(String value) {
        switch (value) {
            // boolean properties
            case "true":
            case "false":
                return "false";
            // direction properties
            case "south":
            case "north":
            case "east":
            case "west":
            case "up":
            case "down":
                return "south";
        }
        for (int i = 0; i < value.length(); i ++) {
            if (!Character.isDigit(value.charAt(i))) return null; // not an int property
        }
        // int property
        return "0";
    }

    public static boolean isDefaultValue(String value) {
        return Objects.equals(value, toDefaultPropertyValue(value));
    }
     */

    public static int getDistanceFromDefault(String value) {
        boolean isInt = true;
        for (int i = 0; i < value.length(); i ++) {
            if (!Character.isDigit(value.charAt(i))) {
                isInt = false;
                break;
            }
        }
        // default int property - prefer smallest int values
        if (isInt) return 1 + Integer.parseInt(value);
        switch (value) {
            case "false": // default value for boolean
            case "south": // default value for direction
                return 1;
        }
        return 32;
    }

    private static int getBlockStateMapDistance(Map<String, String> from, Map<String, String> to) {
        // 2147483647 distance for different block
        // 16 distance for every different entry
        // except for 1 distance for every missing entry with a default value
        if (!Objects.equals(from.get(null), to.get(null))) return Integer.MAX_VALUE;
        int distance = 0;
        // iterates through keys of from
        for (String key: from.keySet()) {
            if (key == null) continue;
            if (to.containsKey(key)) {
                // value comparison - both have this key
                // present identical: +0
                // present different: +32
                if (!Objects.equals(from.get(key), to.get(key))) distance += 32;
            } else {
                // default value check
                distance += getDistanceFromDefault(from.get(key));
            }
        }
        for (String key: to.keySet()) {
            if (key == null || from.containsKey(key)) continue; // already checked in previous loop
            // missing vs value
            distance += getDistanceFromDefault(to.get(key));
        }
        return distance;
    }

    public static Map<String, String> resolveJsonToBlockStateMap(String json) {
        try {
            json = json.replace('\'', '\"');
            NBTTagCompound compound = JsonToNBT.getTagFromJson(json);
            NBTTagCompound properties = compound.getCompoundTag("Properties");
            Map<String, String> blockStateMap = new HashMap<>();
            String blockName = compound.getString("Name");
            blockStateMap.put(null, blockName);
            for (String key: properties.getKeySet()) {
                blockStateMap.put(key, properties.getString(key));
            }
            return blockStateMap;
        } catch (NBTException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    public static Map<String, String> resolveBrigadierBlockStateToBlockStateMap(String brigadierBlockState) {
        Matcher matcher = BBS_PATTERN.matcher(brigadierBlockState);
        Map<String, String> blockStateMap = new HashMap<>();
        if (!matcher.matches()) {
            if (!brigadierBlockState.startsWith("minecraft:")) brigadierBlockState = "minecraft:" + brigadierBlockState;
            blockStateMap.put(null, brigadierBlockState);
            return blockStateMap;
        }
        String blockName = matcher.group("block");
        if (!blockName.startsWith("minecraft:")) blockName = "minecraft:" + blockName;
        blockStateMap.put(null, blockName);
        String[] properties = matcher.group("props").split(",");
        for (String property: properties) {
            Matcher propMatcher = PROP_PATTERN.matcher(property);
            if (propMatcher.matches()) {
                blockStateMap.put(propMatcher.group("key"), propMatcher.group("value"));
            }
        }
        return blockStateMap;
    }

    public static int deflattenBrigadierBlockStateToId0(String brigadierBlockState) {
        try {
            Map<String, String> resolvedMap = resolveBrigadierBlockStateToBlockStateMap(brigadierBlockState);
            if (DEFLATTENING_MAP.containsKey(resolvedMap)) {
//                System.out.println("Precise match with " + resolvedMap);
                return DEFLATTENING_MAP.get(resolvedMap); // precise match
            }
            // iteration start for the closest blockstate
            int minimalDistance = Integer.MAX_VALUE - 1;
            Map<String, String> closest = null;
            for (Map<String, String> candidate: DEFLATTENING_MAP.keySet()) {
                int distance = getBlockStateMapDistance(resolvedMap, candidate);
//                System.out.println("Distance of " + resolvedMap + " and " + candidate + " is " + distance);
                if (distance < minimalDistance) {
                    minimalDistance = distance;
                    closest = candidate;
                }
            }
            if (closest == null) return -1;
            else return DEFLATTENING_MAP.get(closest);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return -1;
        }
    }

    public static int deflattenBrigadierBlockStateToId(String brigadierBlockState) {
        if (CACHED_MAPPINGS.containsKey(brigadierBlockState)) return CACHED_MAPPINGS.get(brigadierBlockState);
        int result = deflattenBrigadierBlockStateToId0(brigadierBlockState);
        if (result != -1) {
            if (CACHED_MAPPINGS.size() < 512) {
                CACHED_MAPPINGS.put(brigadierBlockState, result);
            } else {
                CACHED_MAPPINGS.clear();
            }
        }
        return result;
    }

    public static IBlockState deflattenBrigadierBlockStateToState(String brigadierBlockState) {
        int id = deflattenBrigadierBlockStateToId(brigadierBlockState);
        if (id == -1) return null;
        int blockId = id / 16;
        int metadata = id & 15;
        return Block.getBlockById(blockId).getStateFromMeta(metadata);
    }

    static {
        BlockStateFlatteningMapInitializer.registerEntries();
    }
}
