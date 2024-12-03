package carpet.logging.logHelpers;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.*;
import java.util.function.BiConsumer;

import static carpet.logging.logHelpers.PopulationConstants.*;

public class PopulationHelper {
    private PopulationHelper() {}

    private static final PopulationHelper INSTANCE = new PopulationHelper();

    public static PopulationHelper getInstance() {
        return INSTANCE;
    }

    /*
     * Events to log during population:
     * 1. Placement of features, such as liquid pockets
     * 2. Placement of structures
     * 3. Population start and end
     * 4. Instant tile ticks and instant falling
     * 5. Population suppression and invisible chunks
     * 6. Async population
     */

    // Part 0: Utilities
    public static String getOptionStringForPlayer(EntityPlayer player) {
        return getPopulationLogger().getSubscribedPlayers().get(player.getName());
    }

    public static Logger getPopulationLogger() {
        return LoggerRegistry.getLogger("population");
    }
    public static String blockPosToString(BlockPos pos) {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ')';
    }
    public static class PopulationLoggerOptions {
        public Set<String> worldsToLog = new HashSet<>();
        public Set<Class<? extends WorldGenerator>> featuresToLog = new HashSet<>();
        public Set<PopulationFlag> flagsToLog = EnumSet.noneOf(PopulationFlag.class);
        public boolean shouldLogPopulationSuppression = false;
        public boolean shouldLogAsyncPopulation = false;

        public boolean shouldLogWorld(World world) {
            return worldsToLog.contains(world.provider.getDimensionType().getName());
        }

        public boolean shouldLogFeature(WorldGenerator feature) {
            return !feature.isDoBlockNotify() && featuresToLog.contains(feature.getClass());
        }

        public boolean shouldLogFlag(World world, PopulationFlag flag) {
            if (flag == PopulationFlag.RPF) return false;
            if (flag.isWorldDependent()) {
                return shouldLogWorld(world) && flagsToLog.contains(flag);
            } else {
                return flagsToLog.contains(flag);
            }
        }
    }

    public void log(BiConsumer<PopulationLoggerOptions, List<ITextComponent>> message) {
        if (!LoggerRegistry.__population) return;
        if (isOnBeaconThread()) return; // avoid trouble with multithreading
        Logger logger = getPopulationLogger();
        logger.log((yeet, player) -> {
            List<ITextComponent> parts = new ArrayList<>();
            PopulationLoggerOptions options = resolveOptionsByString(getOptionStringForPlayer(player));
            message.accept(options, parts);
            return parts.toArray(new ITextComponent[0]);
        });
    }

    // Part 1: Features

    public static enum PopulationFlag {
        ITT("instant tile ticks", "ITT", true),
        IF("instant fall", "IF", false),
        RPF("redstone power", "RPF", false);
        private String name;
        private String acronym;
        private boolean worldDependent;
        PopulationFlag(String name, String acronym, boolean worldDependent) {
            this.name = name;
            this.acronym = acronym;
            this.worldDependent = worldDependent;
        }
        public String getName() {
            return name;
        }

        public String getAcronym() {
            return acronym;
        }

        public boolean isWorldDependent() {
            return worldDependent;
        }
        public boolean getValue(World world) {
            switch (this) {
                case ITT:
                    return world.scheduledUpdatesAreImmediate;
                case IF:
                    return BlockFalling.fallInstantly;
                case RPF:
                    if (Blocks.REDSTONE_WIRE == null) return true;
                    return Blocks.REDSTONE_WIRE.canProvidePower;
            }
            throw new IllegalStateException();
        }
    }

    public void logFeature(WorldGenerator feature, World world, BlockPos pos) {
        log((options, components) -> {
            if (options.shouldLogWorld(world) && options.shouldLogFeature(feature)) {
                components.add(Messenger.s(null,
                        String.format("Feature %s placed in world %s at position %s"
                                , featureToName.get(feature.getClass())
                                , world.provider.getDimensionType().getName()
                                , blockPosToString(pos))));
            }
        });
    }

    // Part 2: Structures
    // Currently unsupported

    // Part 3: Population start and end
    public void logPopulationStart(World world, ChunkPos pos) {
        if (isOnBeaconThread()) {
            world.getMinecraftServer().addScheduledTask(() -> {
                log((options, components) -> {
                    if (options.shouldLogWorld(world) && options.shouldLogAsyncPopulation) {
                        components.add(Messenger.s(null, String.format(
                                "Async population of chunk (%d, %d) started! Async population will not be logged for technical purposes. ", pos.x, pos.z)));
                    }
                });
            });
            return;
        }
        log((options, components) -> {
            if (options.shouldLogWorld(world)) {
                components.add(Messenger.s(null, String.format(
                        "Population of chunk (%d, %d) started! ", pos.x, pos.z)));
            }
        });
        logFlagToggle(null, PopulationFlag.IF, true);
    }

    public void logPopulationEnd(World world, ChunkPos pos) {
        logFlagToggle(null, PopulationFlag.IF, false);
        log((options, components) -> {
            if (options.shouldLogWorld(world)) {
                components.add(Messenger.s(null, String.format(
                        "Population of chunk (%d, %d) returned! ", pos.x, pos.z)));
            }
        });
    }

    // Part 4: ITT & IF
    public void logFlagToggle(World world, PopulationFlag flag, boolean value) {
        log((options, components) -> {
            if (options.shouldLogFlag(world, flag)) {
                String msg = "";
                if (flag.isWorldDependent()) {
                    msg = String.format("Toggled the %s flag to %s in world %s",
                            flag.getName(), value, world.provider.getDimensionType().getName());
                } else {
                    msg = String.format("Toggled the %s flag to %s",
                            flag.getName(), value);
                }
                components.add(Messenger.s(null, msg));
            }
        });
    }

    // Part 5: Population suppression
    public boolean isChunkInvisible(Chunk target, Chunk neighbor1, Chunk neighbor2, Chunk neighbor3) {
        return target != null && !target.isTerrainPopulated() && neighbor1 != null && neighbor2 != null && neighbor3 != null;
    }
    public ITextComponent getInvisChunkLog(int x, int z) {
        return Messenger.s(null, String.format("Chunk (%d, %d) is now an invisible chunk. ", x, z));
    }

    public void logPopulationSuppressed(World world, ChunkPos suppressedPos) {
        log((options, components) -> {
           if (options.shouldLogWorld(world) && options.shouldLogPopulationSuppression) {
               components.add(Messenger.s(null, String.format(
                       "Population of chunk (%d, %d) is suppressed",
                       suppressedPos.x, suppressedPos.z)));
               StringBuilder flagLog = new StringBuilder("Current flags: ");
               for (PopulationFlag flag: PopulationFlag.values()) {
                   flagLog.append(flag.getName()).append(" = ").append(flag.getValue(world)).append("; ");
               }
               components.add(Messenger.s(null, flagLog.toString()));
               int x = suppressedPos.x;
               int z = suppressedPos.z;
               Chunk c00 = world.getChunkProvider().getLoadedChunk(x, z);
               Chunk c0p = world.getChunkProvider().getLoadedChunk(x, z + 1);
               Chunk c0m = world.getChunkProvider().getLoadedChunk(x, z - 1);
               Chunk cp0 = world.getChunkProvider().getLoadedChunk(x + 1, z);
               Chunk cpm = world.getChunkProvider().getLoadedChunk(x + 1, z - 1);
               Chunk cm0 = world.getChunkProvider().getLoadedChunk(x - 1, z);
               Chunk cmp = world.getChunkProvider().getLoadedChunk(x - 1, z + 1);
               Chunk cmm = world.getChunkProvider().getLoadedChunk(x - 1, z - 1);
               if (isChunkInvisible(c0m, cpm, c00, cp0)) components.add(getInvisChunkLog(x, z - 1));
               if (isChunkInvisible(cm0, c00, cmp, c0p)) components.add(getInvisChunkLog(x - 1, z));
               if (isChunkInvisible(cmm, c0m, cm0, c00)) components.add(getInvisChunkLog(x - 1, z - 1));
           }
        });
    }

    // Part 6: Async population
    public static boolean isOnBeaconThread() {
        return Thread.currentThread().getName().startsWith("Downloader");
    }

}
