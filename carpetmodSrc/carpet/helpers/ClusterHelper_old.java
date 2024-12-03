package carpet.helpers;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterHelper_old {
    private static final Long2ObjectOpenHashMap<Long> THIS_IS_HERE_SO_THAT_I_CAN_JUMP_TO_THAT_HASHMAP_CLASS_MORE_EASILY = new Long2ObjectOpenHashMap<>();
    private int hashSize = -1;
    private int clusterSize = -1;
    private ChunkPos targetStart = null;
    private ChunkPos targetEnd = null;
    private ChunkPos clusterSearchStart = null;
    private int clusterWidth = -1;
    private int desiredHashStart = -1;
    private OptimalClusteringReport lastOptimalClusteringReport = null;
    private ClusterChunksReport lastClusterChunksReport = null;
    public static final List<ChunkPos>[] CORNER_CHUNKS = new List[] {
        Arrays.asList(new ChunkPos(0, 0), new ChunkPos(0, 1), new ChunkPos(1, 0), new ChunkPos(1, 1)),
        Arrays.asList(new ChunkPos(0, 0), new ChunkPos(0, -1), new ChunkPos(1, 0), new ChunkPos(1, -1)),
        Arrays.asList(new ChunkPos(0, 0), new ChunkPos(0, 1), new ChunkPos(-1, 0), new ChunkPos(-1, 1)),
        Arrays.asList(new ChunkPos(0, 0), new ChunkPos(0, -1), new ChunkPos(-1, 0), new ChunkPos(-1, -1))
    };

    public boolean isInitialized() {
        return hashSize > 0 && clusterSize > 0 && targetStart != null && targetEnd != null && clusterSearchStart != null && clusterWidth != 0;
    }

    public static class OptimalClusteringReport implements Cloneable {
        public ChunkPos target;
        public int hashStart;
        public Map<ChunkPos, Integer> detailedClustering;
        public List<ChunkPos> preloadedChunks;
        public int totalClustering;

        @Override
        public OptimalClusteringReport clone() {
            try {
                OptimalClusteringReport clone = (OptimalClusteringReport) super.clone();
                clone.hashStart = hashStart;
                clone.target = target;
                clone.detailedClustering = new HashMap<>(detailedClustering);
                clone.preloadedChunks = preloadedChunks;
                clone.totalClustering = totalClustering;
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public static class ClusterChunksReport {
        public Map<Integer, Integer> hashStartToMaxZ = new HashMap<>();
        public Map<Integer, List<ChunkPos>> hashStartToClusterChunks = new HashMap<>();
    }

    private static void computeHashesToCluster(ChunkPos center, List<ChunkPos> toAvoid, int[] hashesToCluster, int mask) {
        int ptr = 0;
        for (int dx = -2; dx <= 2; dx ++) {
            for (int dz = -2; dz <= 2; dz ++) {
                // ik constructing these new objects are wasteful alright
                // it's just 25 objects what's the big deal
                if (toAvoid.contains(new ChunkPos(dx, dz))) continue;
                hashesToCluster[ptr] = (int) (mask & HashCommon.mix(ChunkPos.asLong(center.x + dx, center.z + dz)));
                ptr ++;
            }
        }
    }

    private static int getTotalClustering(int[] hashesToCluster, int hashStart, int clusterSize, int mask) {
        final int hashEnd = (hashStart + clusterSize) & mask;
        int totalClustering = 0;
        if (hashStart < hashEnd) {
            for (int toCluster: hashesToCluster) {
                if (hashStart <= toCluster && toCluster < hashEnd) totalClustering += hashEnd - toCluster;
            }
        } else {
            for (int toCluster: hashesToCluster) {
                if (toCluster >= hashStart) totalClustering += clusterSize - (toCluster - hashStart);
                else if (toCluster < hashEnd) totalClustering += hashEnd - toCluster;
            }
        }
        return totalClustering;
    }

    public static int getTotalClustering(ChunkPos target, List<ChunkPos> preloadedChunks, int hashStart, int clusterSize, int mask) {
        int[] hashesToCluster = new int[21];
        computeHashesToCluster(target, preloadedChunks, hashesToCluster, mask);
        return getTotalClustering(hashesToCluster, hashStart, clusterSize, mask);
    }

    private static Map<ChunkPos, Integer> getDetailedClustering(ChunkPos target, List<ChunkPos> toAvoid, int hashStart, int clusterSize, int mask) {
        final int hashEnd = (hashStart + clusterSize) & mask;
        final Map<ChunkPos, Integer> result = new HashMap<>();
        for (int dx = -2; dx <= 2; dx ++) {
            for (int dz = -2; dz <= 2; dz ++) {
                // ik constructing these new objects are wasteful alright
                // it's just 25 objects what's the big deal
                if (toAvoid.contains(new ChunkPos(dx, dz))) continue;
                int clustering = 0;
                int toCluster = (int) (HashCommon.mix(ChunkPos.asLong(target.x + dx, target.z + dz)) & mask);
                if (hashStart < hashEnd) {
                    if (hashStart <= toCluster && toCluster < hashEnd) clustering = hashEnd - toCluster;
                } else {
                    if (toCluster >= hashStart) clustering = clusterSize - (toCluster - hashStart);
                    else if (toCluster < hashEnd) clustering = hashEnd - toCluster;
                }
                result.put(new ChunkPos(target.x + dx, target.z + dz), clustering);
            }
        }
        return result;
    }

    private void computeOptimalClustering0(ChunkPos target, OptimalClusteringReport report) {
        final int[] hashesToCluster = new int[21];
        final int hashSize = this.hashSize;
        final int mask = hashSize - 1;
        final int clusterSize = this.clusterSize;
        int netMaxClustering = 0;
        List<ChunkPos> maxClusteringPreloadedChunks = null;
        int netMaxClusteringHashStart = 0;
        for (List<ChunkPos> preloadedChunks: CORNER_CHUNKS) {
            computeHashesToCluster(target, preloadedChunks, hashesToCluster, mask);
            int maxClustering = 0;
            int maxClusteringHashStart = 0;
            for (int hashStart = 0; hashStart <= hashSize; hashStart ++) {
                int totalClustering = getTotalClustering(hashesToCluster, hashStart, clusterSize, mask);
                if (totalClustering >= maxClustering) {
                    maxClustering = totalClustering;
                    maxClusteringHashStart = hashStart;
                }
            }
            if (maxClustering >= netMaxClustering) {
                netMaxClustering = maxClustering;
                maxClusteringPreloadedChunks = preloadedChunks;
                netMaxClusteringHashStart = maxClusteringHashStart;
            }
        }
        report.preloadedChunks = maxClusteringPreloadedChunks;
        report.totalClustering = netMaxClustering;
        report.hashStart = netMaxClusteringHashStart;
        report.target = target;
        report.detailedClustering = getDetailedClustering(target, maxClusteringPreloadedChunks, netMaxClusteringHashStart, clusterSize, mask);
    }

    public OptimalClusteringReport computeOptimalClustering(ChunkPos target) {
        if (!isInitialized()) return null;
        OptimalClusteringReport report = new OptimalClusteringReport();
        computeOptimalClustering0(target, report);
        lastOptimalClusteringReport = report;
        return report;
    }

    public OptimalClusteringReport computeOptimalClustering() {
        if (!isInitialized()) return null;
        int startX, endX, startZ, endZ;
        startX = Math.min(targetStart.x, targetEnd.x);
        endX = Math.max(targetStart.x, targetEnd.x);
        startZ = Math.min(targetStart.z, targetEnd.z);
        endZ = Math.max(targetStart.z, targetEnd.z);
        OptimalClusteringReport report = new OptimalClusteringReport();
        OptimalClusteringReport optimalReport = null;
        for (int x = startX; x <= endX; x ++) {
            for (int z = startZ; z <= endZ; z ++) {
                computeOptimalClustering0(new ChunkPos(x, z), report);
                if (optimalReport == null || optimalReport.totalClustering <= report.totalClustering)
                    optimalReport = report.clone();
            }
        }
        lastOptimalClusteringReport = optimalReport;
        return optimalReport;
    }

    private final List<ChunkPos> clusterChunksExtractCache = new ArrayList<>();
    private List<ChunkPos> tryExtractClusterChunks(int hashStart, List<ChunkPos>[] chunkToHashSlots) {
        final int clusterSize = this.clusterSize;
        final int hashSize = this.hashSize;
        final int mask = hashSize - 1;
        final int hashEnd = (hashStart + clusterSize) & mask;
        int hash = hashStart;
        int iterations = 0;
        int accumulatedSize = 0;
        clusterChunksExtractCache.clear();
        List<ChunkPos> currList;
        while (hash != hashEnd && accumulatedSize < clusterSize) {
            if ((currList = chunkToHashSlots[hash]) != null && !currList.isEmpty()) {
                accumulatedSize += currList.size();
                clusterChunksExtractCache.addAll(currList);
            }
            if (accumulatedSize <= iterations) {
                return null;
            }
            iterations ++;
            hash = (hash + 1) & mask;
        }
        if (accumulatedSize < clusterSize) return null;
        return new ArrayList<>(clusterChunksExtractCache);
    }

    public List<ChunkPos> getClusterChunks(int hashStart, AtomicInteger zEndCallback) {
        // I initially intended to copy Fallen's implementation from Carpet TIS Addition,
        // but I felt too lazy to read that, so I will just write one myself
        if (!isInitialized()) return null;
        final int hashSize = this.hashSize;
        final int mask = hashSize - 1;
        final int xStart = clusterSearchStart.x;
        final int xEnd = xStart + clusterWidth;
        List<ChunkPos>[] chunkToHashSlots = new List[hashSize];
        List<ChunkPos> result;
        for (int z = clusterSearchStart.z; /* loop terminates by a break statement later */ ; z ++) {
            for (int x = xStart; x < xEnd; x ++) {
                int hash = (int) (mask & HashCommon.mix(ChunkPos.asLong(x, z)));
                if (chunkToHashSlots[hash] == null) chunkToHashSlots[hash] = new ArrayList<>();
                chunkToHashSlots[hash].add(new ChunkPos(x, z));
            }
            if ((result = tryExtractClusterChunks(hashStart, chunkToHashSlots)) != null) {
                zEndCallback.set(z);
                return result;
            }
        }
    }

    public ClusterChunksReport getClusterChunks(OptimalClusteringReport report, int suboptimalRange) {
        ClusterChunksReport clusterReport = new ClusterChunksReport();
        AtomicInteger zMaxCallback = new AtomicInteger();
        final int mask = hashSize - 1;
        int optimalStart = report.hashStart;
        for (int hashStart0 = optimalStart - suboptimalRange; hashStart0 <= optimalStart + suboptimalRange; hashStart0 ++) {
            int hashStart = hashStart0 & mask;
            clusterReport.hashStartToClusterChunks.put(hashStart, getClusterChunks(hashStart, zMaxCallback));
            clusterReport.hashStartToMaxZ.put(hashStart, zMaxCallback.get());
        }
        lastClusterChunksReport = clusterReport;
        return clusterReport;
    }

    public int getHashSize() {
        return hashSize;
    }

    public void setHashSize(int hashSize) {
        this.hashSize = HashCommon.nextPowerOfTwo(hashSize);
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(int clusterSize) {
        this.clusterSize = clusterSize;
    }

    public ChunkPos getTargetStart() {
        return targetStart;
    }

    public void setTargetStart(ChunkPos targetStart) {
        this.targetStart = targetStart;
    }

    public ChunkPos getTargetEnd() {
        return targetEnd;
    }

    public void setTargetEnd(ChunkPos targetEnd) {
        this.targetEnd = targetEnd;
    }

    public ChunkPos getClusterSearchStart() {
        return clusterSearchStart;
    }

    public void setClusterSearchStart(ChunkPos clusterSearchStart) {
        this.clusterSearchStart = clusterSearchStart;
    }

    public int getClusterWidth() {
        return clusterWidth;
    }

    public void setClusterWidth(int clusterWidth) {
        this.clusterWidth = clusterWidth;
    }

    public int getDesiredHashStart() {
        return desiredHashStart;
    }

    public void setDesiredHashStart(int desiredHashStart) {
        this.desiredHashStart = desiredHashStart;
    }

    public OptimalClusteringReport getLastOptimalClusteringReport() {
        return lastOptimalClusteringReport;
    }

    public ClusterChunksReport getLastClusterChunksReport() {
        return lastClusterChunksReport;
    }
}
