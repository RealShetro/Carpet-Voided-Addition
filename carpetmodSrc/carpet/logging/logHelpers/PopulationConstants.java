package carpet.logging.logHelpers;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.gen.feature.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public abstract class PopulationConstants {
    private static class FeatureToNameMap extends HashMap<Class<? extends WorldGenerator>, String> {
        public FeatureToNameMap() {
            put(WorldGenBigMushroom.class, "big_mushroom");
            put(WorldGenBigTree.class, "big_oak");
            put(WorldGenBirchTree.class, "birch");
            put(WorldGenBlockBlob.class, "taiga_mossy_stone");
            put(WorldGenBush.class, "mushroom");
            put(WorldGenCactus.class, "cactus");
            put(WorldGenCanopyTree.class, "dark_oak");
            put(WorldGenClay.class, "clay");
            put(WorldGenDeadBush.class, "dead_bush");
            put(WorldGenDesertWells.class, "desert_wells");
            put(WorldGenDoublePlant.class, "tall_plant");
            put(WorldGenDungeons.class, "spawner");
            put(WorldGenEndGateway.class, "end_gateway");
            put(WorldGenEndIsland.class, "end_island");
            put(WorldGenEndPodium.class, "end_fountain");
            put(WorldGeneratorBonusChest.class, "bonus_chest");
            put(WorldGenFire.class, "fire");
            put(WorldGenFlowers.class, "dandelion");
            put(WorldGenFossils.class, "fossil");
            put(WorldGenGlowStone1.class, "glowstone1");
            put(WorldGenGlowStone2.class, "glowstone2");
            put(WorldGenHellLava.class, "nether_spring");
            put(WorldGenIcePath.class, "ice_path");
            put(WorldGenIceSpike.class, "ice_spike");
            put(WorldGenLakes.class, "lakes");
            put(WorldGenLiquids.class, "spring");
            put(WorldGenMegaJungle.class, "large_jungle");
            put(WorldGenMegaPineTree.class, "large_spruce");
            put(WorldGenMelon.class, "melon");
            put(WorldGenMinable.class, "overworld_ore");
            put(WorldGenPumpkin.class, "pumpkin");
            put(WorldGenReed.class, "sugarcanes");
            put(WorldGenSand.class, "sand");
            put(WorldGenSavannaTree.class, "acacia");
            put(WorldGenShrub.class, "jungle");
            put(WorldGenSpikes.class, "end_pillar");
            put(WorldGenSwamp.class, "swamp_tree");
            put(WorldGenTaiga1.class, "spruce");
            put(WorldGenTaiga2.class, "pine");
            put(WorldGenTallGrass.class, "tall_grass");
            put(WorldGenTrees.class, "oak_or_jungle");
            put(WorldGenVines.class, "vines");
            put(WorldGenWaterlily.class, "lilypad");
        }
    }

    public static final Map<Class<? extends WorldGenerator>, String> featureToName = new FeatureToNameMap();
    public static final Map<String, Class<? extends WorldGenerator>> nameToFeature = new HashMap<>();

    static {
        for (Class<? extends WorldGenerator> featureClass: featureToName.keySet()) {
            nameToFeature.put(featureToName.get(featureClass), featureClass);
        }
    }

    public static final int CACHED_OPTIONS_COUNT = 16;
    public static final LinkedHashMap<String, PopulationHelper.PopulationLoggerOptions> cachedOptions = new LinkedHashMap<>();
    public static final PopulationHelper.PopulationLoggerOptions EMPTY_OPTIONS = new PopulationHelper.PopulationLoggerOptions();

    /**
     * @param optionString an NBT string encoding things to log
     *                     Example: {world:[overworld],flags:[ITT,IF],suppression:1b,features:[spring]}
     */
    public static PopulationHelper.PopulationLoggerOptions resolveOptionsByString(String optionString) {
        if (cachedOptions.containsKey(optionString)) return cachedOptions.get(optionString);
        try {
            PopulationHelper.PopulationLoggerOptions options = resolveOptionsByString0(optionString);
            for (Iterator<?> iterator = cachedOptions.entrySet().iterator();
                 cachedOptions.size() >= CACHED_OPTIONS_COUNT && iterator.hasNext(); iterator.next()) {
                iterator.remove();
            }
            cachedOptions.put(optionString, options);
            return options;
        } catch (Exception exception) {
            Messenger.print_server_message(CarpetServer.minecraft_server, "An exception occurred while parsing options of /log population!!");
            Messenger.print_server_message(CarpetServer.minecraft_server, "Stack trace: ");
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);
            Messenger.print_server_message(CarpetServer.minecraft_server, stringWriter.toString());
            try {
                stringWriter.close();
                printWriter.close();
            } catch (IOException e) {
                Messenger.print_server_message(CarpetServer.minecraft_server, "114514 1919810");
            }
        }
        return EMPTY_OPTIONS;
    }

    private static PopulationHelper.PopulationLoggerOptions resolveOptionsByString0(String optionString) throws NBTException {
        System.out.println("Trying to resolve option " + optionString);
        PopulationHelper.PopulationLoggerOptions result = new PopulationHelper.PopulationLoggerOptions();
        NBTTagCompound optionNBT = JsonToNBT.getTagFromJson(optionString);
        for (String category: optionNBT.getKeySet()) {
            NBTTagList tagList = optionNBT.getTagList(category, 8 /* string 8 */);
            List<String> args = new ArrayList<>();
            for (int i = 0; i < tagList.tagCount(); i ++) args.add(tagList.getStringTagAt(i));
            switch (category) {
                case "world": {
                    result.worldsToLog.addAll(args);
                    break;
                }
                case "feature": {
                    Class<? extends WorldGenerator> featureClass = null;
                    for (String arg: args) {
                        if ((featureClass = nameToFeature.get(arg)) != null) result.featuresToLog.add(featureClass);
                        else {
                            // special case
                            if ("all".equals(arg)) {
                                result.featuresToLog.addAll(featureToName.keySet());
                            }
                        }
                    }
                    break;
                }
                case "flag": {
                    for (String arg: args) {
                        for (PopulationHelper.PopulationFlag flag: PopulationHelper.PopulationFlag.values()) {
                            if (flag.getAcronym().equals(arg)) result.flagsToLog.add(flag);
                        }
                    }
                    break;
                }
                case "suppression": {
                    result.shouldLogPopulationSuppression = true;
                    break;
                }
                case "async_population": {
                    result.shouldLogAsyncPopulation = true;
                    break;
                }
            }
        }
        return result;
    }


}
