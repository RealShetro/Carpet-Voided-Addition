package carpet.helpers;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.*;

public class ClusterHelper {
    private static final Long2ObjectOpenHashMap<Long> ZN_AR_IS_CUTE = new Long2ObjectOpenHashMap<>();

    public static final List<ChunkPos>[] EXCLUDED_RELATIVE_POS_LISTS = new List[] {
            Arrays.asList(new ChunkPos(0, 0), new ChunkPos(0, 1), new ChunkPos(1, 0), new ChunkPos(1, 1)),
            Arrays.asList(new ChunkPos(0, 0), new ChunkPos(0, -1), new ChunkPos(1, 0), new ChunkPos(1, -1))
    };

    public ChunkPos searchStartPos = new ChunkPos(0, 0);
    public ChunkPos searchEndPos = new ChunkPos(0, 0);
    public int desiredClustering;
    public int hashSize = 8192;
    public int clusterWidth = 90;
    public int maximalClusterHeight = 200;
    public ChunkPos clusterCorner = new ChunkPos(0, 0);
    public EnumFacing clusterWidthDir = EnumFacing.EAST;
    public EnumFacing clusterHeightDir = EnumFacing.SOUTH;

    public int loadingGridSize;
    public final List<ChunkPos> clusterChunks = new ArrayList<>();
    public final List<ChunkPos> acceptedClusterChunks = new ArrayList<>();
    public final List<ChunkPos> optimalClusterChunks = new ArrayList<>();
    public final Set<ChunkPos> loadingGrid = new LinkedHashSet<>();
    public final Set<ChunkPos> acceptedLoadingGrid = new LinkedHashSet<>();
    public int acceptedHashStart;
    public final Set<ChunkPos> optimalLoadingGrid = new LinkedHashSet<>();
    public int optimalHashStart;
    public ChunkPos optimalTargetChunk = null;
    public List<ChunkPos> optimalExcludedRelative = null;

    // intermediate fields
    private Object2IntOpenHashMap<ChunkPos>[] hashChunkToHeight;
    private int[][] cumulativeHashHeightToChunkCount;
    private final int[] clusteredHashes = new int[21];

    private void testTargetChunk(ChunkPos targetPos, List<ChunkPos> excludedRelativePos) {
        int ptr = 0;
        final int[] clusteredHashes = this.clusteredHashes;
        final List<ChunkPos> clusterChunks = this.clusterChunks;
        final Set<ChunkPos> loadingGrid = this.loadingGrid;
        final Set<ChunkPos> acceptedLoadingGrid = this.acceptedLoadingGrid;
        final List<ChunkPos> acceptedClusterChunks = this.acceptedClusterChunks;
        for (int x = -2; x <= 2; x ++) {
            for (int z = -2; z <= 2; z ++) {
                if (!excludedRelativePos.contains(new ChunkPos(x, z)))
                    clusteredHashes[ptr ++] = hashChunkPos(targetPos.x + x, targetPos.z + z, hashSize);
            }
        }
        Arrays.sort(clusteredHashes);
        // The reasoning behind this non-brute iteration is that,
        // if you have the hash start between two clustered hashes, and you shift one to the right
        // then the same length will lead to non-decreasing clustering.
        acceptedLoadingGrid.clear();
        int minLoadingGridSize = Integer.MAX_VALUE;
        for (int idx = 0; idx < 21; idx ++) {
            int hashStart = clusteredHashes[idx];
            int hashLength = getHashLength(idx, clusteredHashes, hashStart, desiredClustering);
            if (hashLength < 0) continue;
            clusterChunks.clear();
            if (this.computeClusterChunks(hashStart, hashLength) < 0) continue;
            loadingGrid.clear();
            computeLoadingGrid(clusterChunks, loadingGrid);
            if (loadingGrid.size() < minLoadingGridSize) {
                minLoadingGridSize = loadingGrid.size();
                acceptedLoadingGrid.clear();
                acceptedLoadingGrid.addAll(loadingGrid);
                acceptedClusterChunks.clear();
                acceptedClusterChunks.addAll(clusterChunks);
                acceptedHashStart = hashStart;
            }
        }
        this.loadingGridSize = minLoadingGridSize;
    }

    public int compute() {
        try {
            // Precompute all cluster chunk hashes
            int[][] hashHeightToChunkCount = new int[hashSize][maximalClusterHeight];
            int[][] cumulativeHashHeightToChunkCount = this.cumulativeHashHeightToChunkCount = new int[hashSize][maximalClusterHeight];
            Object2IntOpenHashMap<ChunkPos>[] hashChunkToHeight = this.hashChunkToHeight = new Object2IntOpenHashMap[hashSize];
            for (int dw = 0; dw < clusterWidth; dw++) {
                for (int dh = 0; dh < maximalClusterHeight; dh++) {
                    ChunkPos pos = new ChunkPos(
                            clusterCorner.x + dw * clusterWidthDir.getXOffset() + dh * clusterHeightDir.getXOffset(),
                            clusterCorner.z + dw * clusterWidthDir.getZOffset() + dh * clusterHeightDir.getZOffset());
                    int hash = hashChunkPos(pos, hashSize);
                    hashHeightToChunkCount[hash][dh]++;
                    if (hashChunkToHeight[hash] == null) hashChunkToHeight[hash] = new Object2IntOpenHashMap<>();
                    hashChunkToHeight[hash].put(pos, dh);
                }
            }
            for (int hash = 0; hash < hashSize; hash++) {
                int cumulative = 0;
                for (int dh = 0; dh < maximalClusterHeight; dh++) {
                    cumulativeHashHeightToChunkCount[hash][dh] = (cumulative += hashHeightToChunkCount[hash][dh]);
                }
            }
            // Run brute force search in search area
            int minSize = Integer.MAX_VALUE;
            final Set<ChunkPos> acceptedLoadingGrid = this.acceptedLoadingGrid;
            final Set<ChunkPos> optimalLoadingGrid = this.optimalLoadingGrid;
            final List<ChunkPos> acceptedClusterChunks = this.acceptedClusterChunks;
            final List<ChunkPos> optimalClusterChunks = this.optimalClusterChunks;
            for (int sx = searchStartPos.x; sx < searchEndPos.x; sx++) {
                for (int sz = searchStartPos.z; sz < searchEndPos.z; sz++) {
                    ChunkPos searchedChunk = new ChunkPos(sx, sz);
                    for (List<ChunkPos> excludedRelativePos : EXCLUDED_RELATIVE_POS_LISTS) {
                        testTargetChunk(searchedChunk, excludedRelativePos);
                        if (acceptedLoadingGrid.size() < minSize) {
                            minSize = acceptedLoadingGrid.size();
                            optimalLoadingGrid.clear();
                            optimalLoadingGrid.addAll(acceptedLoadingGrid);
                            optimalClusterChunks.clear();
                            optimalClusterChunks.addAll(acceptedClusterChunks);
                            optimalExcludedRelative = excludedRelativePos;
                            optimalTargetChunk = searchedChunk;
                            optimalHashStart = acceptedHashStart;
                        }
                    }
                }
            }
            return minSize;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void loadCluster(World world) {
        for (ChunkPos pos: optimalClusterChunks) {
            world.getChunk(pos.x, pos.z);
        }
    }

    public void loadGrid(World world) {
        for (ChunkPos pos: optimalLoadingGrid) {
            world.getChunk(pos.x, pos.z);
        }
    }

    @SuppressWarnings("all")
    public int checkClusterLoader() {
        if (this.optimalLoadingGrid.isEmpty() || this.optimalClusterChunks.isEmpty()) return -1;
        final Set<ChunkPos> remainingGridChunks = new HashSet<>(optimalLoadingGrid);
        final List<ChunkPos> addedGridChunks = new ArrayList<>();
        final ChunkPos firstClusterChunk = this.getUpperLeftClusterChunk();
        remainingGridChunks.remove(firstClusterChunk);
        addedGridChunks.add(firstClusterChunk);
        while (!remainingGridChunks.isEmpty() && !addedGridChunks.isEmpty()) {
            for (int i = addedGridChunks.size() - 1; i >= 0; i --) {
                ChunkPos pos = addedGridChunks.get(i);
                ChunkPos pos1 = new ChunkPos(pos.x + 1, pos.z);
                ChunkPos pos2 = new ChunkPos(pos.x - 1, pos.z);
                ChunkPos pos3 = new ChunkPos(pos.x, pos.z + 1);
                ChunkPos pos4 = new ChunkPos(pos.x, pos.z - 1);
                boolean flag = true;
                if (remainingGridChunks.remove(pos1)) {
                    flag = false;
                    addedGridChunks.add(pos1);
                }
                if (remainingGridChunks.remove(pos2)) {
                    flag = false;
                    addedGridChunks.add(pos2);
                }
                if (remainingGridChunks.remove(pos3)) {
                    flag = false;
                    addedGridChunks.add(pos3);
                }
                if (remainingGridChunks.remove(pos4)) {
                    flag = false;
                    addedGridChunks.add(pos4);
                }
                if (flag) addedGridChunks.remove(i);
            }
        }
//        System.err.println("A total of " + remainingGridChunks.size() + " chunks are left disconnected from the loader!!!");
        return remainingGridChunks.size();
    }

    @SuppressWarnings("all")
    public void buildClusterLoader(World world, int y, IBlockState buildingBlock) {
        if (this.optimalLoadingGrid.isEmpty() || this.optimalClusterChunks.isEmpty()) return;
        final Set<ChunkPos> remainingGridChunks = new HashSet<>(optimalLoadingGrid);
        final List<ChunkPos> addedGridChunks = new ArrayList<>();
        final ChunkPos firstClusterChunk = this.getUpperLeftClusterChunk();
        remainingGridChunks.remove(firstClusterChunk);
        addedGridChunks.add(firstClusterChunk);
        while (!remainingGridChunks.isEmpty() && !addedGridChunks.isEmpty()) {
            for (int i = addedGridChunks.size() - 1; i >= 0; i --) {
                ChunkPos pos = addedGridChunks.get(i);
                ChunkPos pos1 = new ChunkPos(pos.x + 1, pos.z);
                ChunkPos pos2 = new ChunkPos(pos.x - 1, pos.z);
                ChunkPos pos3 = new ChunkPos(pos.x, pos.z + 1);
                ChunkPos pos4 = new ChunkPos(pos.x, pos.z - 1);
                boolean flag = true;
                if (remainingGridChunks.remove(pos1)) {
                    flag = false;
                    buildBridge(world, y, buildingBlock, pos, EnumFacing.EAST);
                    addedGridChunks.add(pos1);
                }
                if (remainingGridChunks.remove(pos2)) {
                    flag = false;
                    buildBridge(world, y, buildingBlock, pos, EnumFacing.WEST);
                    addedGridChunks.add(pos2);
                }
                if (remainingGridChunks.remove(pos3)) {
                    flag = false;
                    buildBridge(world, y, buildingBlock, pos, EnumFacing.SOUTH);
                    addedGridChunks.add(pos3);
                }
                if (remainingGridChunks.remove(pos4)) {
                    flag = false;
                    buildBridge(world, y, buildingBlock, pos, EnumFacing.NORTH);
                    addedGridChunks.add(pos4);
                }
                if (flag) addedGridChunks.remove(i);
            }
        }
//        System.err.println("A total of " + remainingGridChunks + " chunks are left disconnected from the loader!!!");
        for (ChunkPos pos: optimalClusterChunks) {
            addHopper(world, y, buildingBlock, pos);
        }
    }

    public void buildBridge(World world, int y, IBlockState buildingBlock, ChunkPos chunk, EnumFacing dir) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos((chunk.x << 4) + 8, y, (chunk.z << 4) + 8);
        for (int i = 0; i < 16; i ++) {
            world.setBlockState(pos, buildingBlock, 2);
            pos.move(dir);
        }
        int offset = dir.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 7 : 8;
        pos.setPos((chunk.x << 4) + 8, y, (chunk.z << 4) + 8).move(dir, offset);
        world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);
        if (!(world.getTileEntity(pos) instanceof TileEntityChest))
            Messenger.print_server_message(CarpetServer.minecraft_server, "Missing chest tile entity!!");
        if (optimalClusterChunks.contains(chunk)) {
            pos.setPos((chunk.x << 4) + 8, y, (chunk.z << 4) + 8);
            world.setBlockState(pos, Blocks.HOPPER.getDefaultState().withProperty(BlockHopper.FACING, dir)
                    .withProperty(BlockHopper.ENABLED, true), 2);
            world.setBlockState(pos.up(), Blocks.TORCH.getDefaultState().withProperty(BlockTorch.FACING, EnumFacing.UP));
            TileEntity tileEntity = world.getTileEntity(pos);
            if (!(tileEntity instanceof TileEntityHopper))
                Messenger.print_server_message(CarpetServer.minecraft_server, "Missing hopper tile entity!!");
            else {
                TileEntityHopper hopper = (TileEntityHopper) tileEntity;
                for (int i = 0; i < 5; i ++) {
                    hopper.setInventorySlotContents(i, new ItemStack(Items.SHEARS, 1));
                }
            }
        }
    }

    public static void addHopper(World world, int y, IBlockState buildingBlock, ChunkPos chunk) {
        BlockPos pos = chunk.getBlock(8, y, 8);
        if (!(world.getBlockState(pos).getBlock() == Blocks.HOPPER)) {
            EnumFacing dir = EnumFacing.DOWN;
            for (EnumFacing facing: EnumFacing.HORIZONTALS) {
                if (world.getBlockState(pos.offset(facing)).getBlock() == buildingBlock.getBlock()) {
                    dir = facing;
                }
            }
            world.setBlockState(pos, Blocks.HOPPER.getDefaultState().withProperty(BlockHopper.FACING, dir)
                    .withProperty(BlockHopper.ENABLED, true), 2);
            world.setBlockState(pos.up(), Blocks.TORCH.getDefaultState().withProperty(BlockTorch.FACING, EnumFacing.UP));
            TileEntity tileEntity = world.getTileEntity(pos);
            if (!(tileEntity instanceof TileEntityHopper))
                Messenger.print_server_message(CarpetServer.minecraft_server, "Missing hopper tile entity!!");
            else {
                TileEntityHopper hopper = (TileEntityHopper) tileEntity;
                for (int i = 0; i < 5; i ++) {
                    hopper.setInventorySlotContents(i, new ItemStack(Items.SHEARS, 1));
                }
            }
        }
    }

    private ChunkPos getUpperLeftClusterChunk() {
        int minHeight = Integer.MAX_VALUE;
        int minOffset = Integer.MAX_VALUE;
        ChunkPos bestPos = null;
        for (ChunkPos pos: optimalClusterChunks) {
            final int height = pos.x * clusterHeightDir.getXOffset() + pos.z * clusterHeightDir.getZOffset();
            if (height < minHeight) {
                minHeight = height;
                bestPos = pos;
            }
        }
        for (ChunkPos pos: optimalClusterChunks) {
            final int height = pos.x * clusterHeightDir.getXOffset() + pos.z * clusterHeightDir.getZOffset();
            final int offset = pos.x * clusterWidthDir.getXOffset() + pos.z * clusterWidthDir.getZOffset();
            if (height == minHeight && offset < minOffset) {
                minOffset = offset;
                bestPos = pos;
            }
        }
        return bestPos;
    }

    // INSTANCE UTILITIES

    public int computeClusterChunks(int hashStart, int length) {
        final int maximalHeight = this.maximalClusterHeight;
        final int mask = this.hashSize - 1;
        final List<ChunkPos> clusterChunks = this.clusterChunks;
        Object2IntOpenHashMap<ChunkPos>[] hashChunkToHeight = this.hashChunkToHeight;
        int[][] cumulativeHashHeightToChunkCount = this.cumulativeHashHeightToChunkCount;
        int requiredHeight = 0;
        int currentLength = 0;
        int currentHash = hashStart;
        while (currentLength < length) {
            int remainingLength = ((currentHash - hashStart) & mask) - currentLength;
            if (cumulativeHashHeightToChunkCount[currentHash][requiredHeight] < remainingLength) {
                requiredHeight ++;
                if (requiredHeight >= maximalHeight) return -1;
                currentLength = 0;
                currentHash = hashStart;
                continue;
            }
            currentLength += cumulativeHashHeightToChunkCount[currentHash][requiredHeight];
            currentHash = (currentHash + 1) & mask;
        }
        Object2IntOpenHashMap<ChunkPos> curMap;
        for (int hash = hashStart; hash != currentHash; hash = (hash + 1) & mask) {
            if ((curMap = hashChunkToHeight[hash]) != null) {
                int finalRequiredHeight = requiredHeight;
                curMap.forEach((cp, h) -> {
                    if (h <= finalRequiredHeight) clusterChunks.add(cp);
                });
            }
        }
        return requiredHeight;
    }

    public void readFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("startX")) searchStartPos = new ChunkPos(compound.getInteger("startX"), searchStartPos.z);
        if (compound.hasKey("startZ")) searchStartPos = new ChunkPos(searchStartPos.x, compound.getInteger("startZ"));
        if (compound.hasKey("endX")) searchEndPos = new ChunkPos(compound.getInteger("endX"), searchEndPos.z);
        if (compound.hasKey("endZ")) searchEndPos = new ChunkPos(searchEndPos.x, compound.getInteger("endZ"));
        this.validateSearchCorners();
        if (compound.hasKey("desiredClustering")) desiredClustering = Math.max(0, compound.getInteger("desiredClustering"));
        if (compound.hasKey("hashSize")) hashSize = nextPowerOfTwo(Math.max(1, compound.getInteger("hashSize") - 1));
        if (compound.hasKey("clusterWidth")) clusterWidth = Math.max(0, compound.getInteger("clusterWidth"));
        if (compound.hasKey("maxClusterHeight")) maximalClusterHeight = Math.max(0, compound.getInteger("maxClusterHeight"));
        if (compound.hasKey("clusterCornerX")) clusterCorner = new ChunkPos(compound.getInteger("clusterCornerX"), clusterCorner.z);
        if (compound.hasKey("clusterCornerZ")) clusterCorner = new ChunkPos(clusterCorner.x, compound.getInteger("clusterCornerZ"));
        EnumFacing facing;
        if (compound.hasKey("widthDir") && ((facing = EnumFacing.NAME_LOOKUP.get(compound.getString("widthDir"))) != null)
            && facing.getYOffset() == 0) clusterWidthDir = facing;
        if (compound.hasKey("heightDir") && ((facing = EnumFacing.NAME_LOOKUP.get(compound.getString("heightDir"))) != null)
                && facing.getYOffset() == 0 && facing.getAxis() != clusterWidthDir.getAxis()) clusterHeightDir = facing;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("startX", searchStartPos.x);
        compound.setInteger("startZ", searchStartPos.z);
        compound.setInteger("endX", searchEndPos.x);
        compound.setInteger("endZ", searchEndPos.z);
        compound.setInteger("desiredClustering", desiredClustering);
        compound.setInteger("hashSize", hashSize);
        compound.setInteger("clusterWidth", clusterWidth);
        compound.setInteger("maxClusterHeight", maximalClusterHeight);
        compound.setInteger("clusterCornerX", clusterCorner.x);
        compound.setInteger("clusterCornerZ", clusterCorner.z);
        compound.setString("widthDir", clusterWidthDir.getName2());
        compound.setString("heightDir", clusterHeightDir.getName2());
        return compound;
    }

    private void validateSearchCorners() {
        int sx = searchStartPos.x, ex = searchEndPos.x, sz = searchStartPos.z, ez = searchEndPos.z;
        searchStartPos = new ChunkPos(Math.min(sx, ex), Math.min(sz, ez));
        searchEndPos = new ChunkPos(Math.max(sx, ex), Math.max(sz, ez));
    }

    // STATIC UTILITIES
    public static int nextPowerOfTwo(int v) {
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        return v + 1;
    }

    public static int hashChunkPos(int x, int z, int hashSize) {
        return (int) (HashCommon.mix(ChunkPos.asLong(x, z)) & (hashSize - 1));
    }

    public static int hashChunkPos(ChunkPos pos, int hashSize) {
        return (int) (HashCommon.mix(ChunkPos.asLong(pos.x, pos.z)) & (hashSize - 1));
    }

    public static int getHashLength(int idx, int[] clusteredHashes, int hashStart, int desiredClustering) {
        int ptr;
        int cumulativeClustering = 0;
        int length;
        int step = 0;
        for (ptr = idx; ptr < 21; ptr ++) {
            cumulativeClustering += (length = clusteredHashes[ptr] - hashStart);
            if (cumulativeClustering >= desiredClustering) {
                return length - (cumulativeClustering - desiredClustering) / step;
            }
            step ++;
        }
        for (ptr = 0; ptr < idx; ptr ++) {
            cumulativeClustering += (length = clusteredHashes[ptr] - hashStart);
            if (cumulativeClustering >= desiredClustering) {
                return length - (cumulativeClustering - desiredClustering) / step;
            }
            step ++;
        }
        return -1;
    }

    public static void computeLoadingGrid(List<ChunkPos> cluster, Set<ChunkPos> loadingGrid) {
        // Searches for a tree using a greedy search similarly to Prim's algorithm
        final Map<ChunkPos, ChunkPath> pathTracker = new HashMap<>();
        final ChunkPos firstChunk = cluster.get(0);
        cluster.forEach(pos -> {
            if (pos == firstChunk) return;
            pathTracker.put(pos, new ChunkPath());
        });
        loadingGrid.add(firstChunk);
        ChunkPos closestChunk = updateXSegment(pathTracker, firstChunk, 0);
        while (!pathTracker.isEmpty()) {
//            if (pathTracker.size() == 1) System.out.println("Last added chunk is " + closestChunk);
            closestChunk = addClosestChunk(closestChunk, loadingGrid, pathTracker);
        }
    }

    public static void computeLoadingGridWithBreakpoints(List<ChunkPos> cluster, Set<ChunkPos> loadingGrid) {
        // Searches for a tree using a greedy search similarly to Prim's algorithm
        final Map<ChunkPos, ChunkPath> pathTracker = new HashMap<>();
        final ChunkPos firstChunk = cluster.get(0);
        cluster.forEach(pos -> {
            if (pos == firstChunk) return;
            pathTracker.put(pos, new ChunkPath());
        });
        loadingGrid.add(firstChunk);
        ChunkPos closestChunk = updateXSegment(pathTracker, firstChunk, 0);
        while (!pathTracker.isEmpty()) {
            if (closestChunk.x == -496 && closestChunk.z == -440) {
                "114514".startsWith("");
            }
//            if (pathTracker.size() == 1) System.out.println("Last added chunk is " + closestChunk);
            closestChunk = addClosestChunk(closestChunk, loadingGrid, pathTracker);
        }
    }

    private static ChunkPos addClosestChunk(ChunkPos chunk, Set<ChunkPos> loadingGrid, Map<ChunkPos, ChunkPath> pathTracker) {
        ChunkPath path = pathTracker.remove(chunk);
        path.drawSegments(loadingGrid);
        final int dx = path.dx, dz = path.dz;
        final ChunkPos start = path.start;
        updateXSegment(pathTracker, dx >= 0 ? start : new ChunkPos(start.x + dx, start.z), Math.abs(dx));
        return updateZSegment(pathTracker, dz >= 0 ? new ChunkPos(start.x + dx, start.z) : new ChunkPos(start.x + dx, start.z + dz), Math.abs(dz));
    }

    private static ChunkPos updateXSegment(
            Map<ChunkPos, ChunkPath> pathTracker, ChunkPos segmentStart, int length) {
        int minDistance = Integer.MAX_VALUE;
        ChunkPos minChunkPos = null;
        final int sx = segmentStart.x, sz = segmentStart.z;
        for (ChunkPos pos: pathTracker.keySet()) {
            final int px = pos.x, pz = pos.z;
            int newDistance;
            if (px < sx) newDistance = Math.abs(pz - sz) + sx - px;
            else if (px > (sx + length)) newDistance = Math.abs(pz - sz) + (px - sx - length);
            else newDistance = Math.abs(pz - sz);
            ChunkPath path = pathTracker.computeIfAbsent(pos, __ -> new ChunkPath());
            if (newDistance < path.length) {
                path.length = newDistance;
                path.start = pos;
                path.dz = sz - pz;
                if (px < sx) path.dx = sx - px;
                else if (px > (sx + length)) path.dx = sx + length - px;
                else path.dx = 0;
            }
            if (newDistance < minDistance) {
                minDistance = newDistance;
                minChunkPos = pos;
            }
        }
        return minChunkPos;
    }

    private static ChunkPos updateZSegment(
            Map<ChunkPos, ChunkPath> pathTracker, ChunkPos segmentStart
            , int length) {
        int minDistance = Integer.MAX_VALUE;
        ChunkPos minChunkPos = null;
        final int sz = segmentStart.z, sx = segmentStart.x;
        for (ChunkPos pos: pathTracker.keySet()) {
            final int px = pos.x, pz = pos.z;
            int newDistance;
            if (pz < sz) newDistance = Math.abs(px - sx) + sz - pz;
            else if (pz > (sz + length)) newDistance = Math.abs(px - sx) + (pz - sz - length);
            else newDistance = Math.abs(px - sx);
            ChunkPath path = pathTracker.computeIfAbsent(pos, __ -> new ChunkPath());
            if (newDistance < path.length) {
                path.length = newDistance;
                path.start = pos;
                path.dx = sx - px;
                if (pz < sz) path.dz = sz - pz;
                else if (pz > (sz + length)) path.dz = sz + length - pz;
                else path.dz = 0;
            }
            if (newDistance < minDistance) {
                minDistance = newDistance;
                minChunkPos = pos;
            }
        }
        return minChunkPos;
    }

    public static class ChunkPath {
        public ChunkPos start;
        public int dx;
        public int dz;
        public int length = Integer.MAX_VALUE;
        public void drawSegments(Collection<ChunkPos> loadingGrid) {
            int x = start.x, z = start.z;
            if (dx >= 0) {
                for (int i = 0; i <= dx; i ++) loadingGrid.add(new ChunkPos(x + i, z));
            } else {
                for (int i = dx; i <= 0; i ++) loadingGrid.add(new ChunkPos(x + i, z));
            }
            x += dx;
            if (dz >= 0) {
                for (int i = 1; i <= dz; i ++) loadingGrid.add(new ChunkPos(x, z + i));
            } else {
                for (int i = dz; i <= -1; i ++) loadingGrid.add(new ChunkPos(x, z + i));
            }
        }
    }
}
