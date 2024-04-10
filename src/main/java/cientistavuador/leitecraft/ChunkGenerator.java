package cientistavuador.leitecraft;

import java.util.Random;

/**
 *
 * @author Cien
 */
public class ChunkGenerator {

    public static final long WORLD_SEED = new Random().nextLong();

    public static final int MAX_TERRAIN_HEIGHT = 96;
    public static final int MIN_TERRAIN_HEIGHT = 24;

    public static final double TERRAIN_SCALE = 1f / 100f;
    public static final double MOUNTAINS_SCALE = 1f / 100f;

    public static final double TREES_SCALE = 1f / 500f;
    public static final float TREE_CHANCE = 0.40f;
    public static final int TREE_HEIGHT = 5;

    public static final int WATER_START_HEIGHT = MIN_TERRAIN_HEIGHT + 4;
    public static final int WATER_HEIGHT = WATER_START_HEIGHT - 1;

    public static final float CAVE_SCALE = 1f / 25f;
    public static final float CAVE_MIN_THRESHOLD = 0.1f;
    public static final float CAVE_MAX_THRESHOLD = 0.4f;
    
    public static final float COAL_ORE_CHANCE = 0.05f;
    public static final float IRON_ORE_CHANCE = 0.01f;
    
    public static final int LAVA_HEIGHT = 3;
    
    public static void generateChunk(Chunk chunk) {
        new ChunkGenerator(chunk).generate();
    }

    private final Chunk chunk;
    private final long chunkPos;
    private final Random seeds;

    private final long mountainRandom;
    private final Random bedrockRandom;

    private final long treeSeed;
    private final Random treeRandom;

    private final long caveSeed;
    
    private final Random oreRandom;
    private final Random grassRandom;

    private ChunkGenerator(Chunk chunk) {
        this.chunk = chunk;
        this.chunkPos = (Integer.toUnsignedLong(chunk.getChunkX()) << 32) | Integer.toUnsignedLong(chunk.getChunkZ());
        this.seeds = new Random(WORLD_SEED);
        
        this.mountainRandom = this.seeds.nextLong();
        this.bedrockRandom = new Random(this.seeds.nextLong() ^ this.chunkPos);
        this.treeSeed = this.seeds.nextLong();
        this.treeRandom = new Random(this.treeSeed ^ this.chunkPos);

        this.caveSeed = this.seeds.nextLong();
        
        this.oreRandom = new Random(this.seeds.nextLong() ^ this.chunkPos);
        this.grassRandom = new Random(this.seeds.nextLong() ^ this.chunkPos);
    }

    private void generateTerrain() {
        final int noiseIterations = 5;

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                double globalBlockX = x + (this.chunk.getChunkX() * Chunk.CHUNK_SIZE);
                double globalBlockZ = z + (this.chunk.getChunkZ() * Chunk.CHUNK_SIZE);

                double perlinX = globalBlockX * TERRAIN_SCALE;
                double perlinY = globalBlockZ * TERRAIN_SCALE;

                float amplitude = 0.5f;
                float frequency = 1f;

                float perlinHeight = 0f;
                for (int i = 0; i < noiseIterations; i++) {
                    float noiseValue = OpenSimplex2S.noise2(WORLD_SEED,
                            perlinX * frequency,
                            perlinY * frequency
                    );
                    noiseValue = ((noiseValue + 1f) * 0.5f) * amplitude;

                    amplitude /= 2f;
                    frequency *= 2f;

                    perlinHeight += noiseValue;
                }

                float mountains = (OpenSimplex2S.noise2(mountainRandom,
                        globalBlockX * MOUNTAINS_SCALE,
                        globalBlockZ * MOUNTAINS_SCALE
                ) + 1f) * 0.5f;
                perlinHeight *= mountains;
                perlinHeight = Math.min(Math.max(perlinHeight, 0f), 1f);

                int height = (int) Math.floor(MIN_TERRAIN_HEIGHT + (perlinHeight * (MAX_TERRAIN_HEIGHT - MIN_TERRAIN_HEIGHT)));

                if (height > WATER_START_HEIGHT) {
                    this.chunk.setBlock(x, height, z, Blocks.GRASS);

                    for (int i = height - 2; i < height; i++) {
                        this.chunk.setBlock(x, i, z, Blocks.DIRT);
                    }
                } else {
                    for (int i = WATER_HEIGHT; i >= height; i--) {
                        this.chunk.setBlock(x, i, z, Blocks.WATER);
                    }
                    for (int i = height - 2; i <= height; i++) {
                        this.chunk.setBlock(x, i, z, Blocks.SAND);
                    }
                }

                for (int i = 1; i < height - 2; i++) {
                    int ore = this.oreRandom.nextInt(2);
                    float oreValue = this.oreRandom.nextFloat();
                    if (ore == 0 && oreValue <= COAL_ORE_CHANCE) {
                        this.chunk.setBlock(x, i, z, Blocks.COAL_ORE);
                    } else if (ore == 1 && oreValue <= IRON_ORE_CHANCE) {
                        this.chunk.setBlock(x, i, z, Blocks.IRON_ORE);
                    } else {
                        this.chunk.setBlock(x, i, z, Blocks.STONE);
                    }
                }

                if (this.bedrockRandom.nextBoolean()) {
                    this.chunk.setBlock(x, 1, z, Blocks.BEDROCK);
                }
                this.chunk.setBlock(x, 0, z, Blocks.BEDROCK);
            }
        }
    }

    private void generateCaves() {
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int terrainHeight = 0;
                for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
                    Block blockAt = this.chunk.getBlock(x, y, z);
                    if (blockAt == Blocks.AIR || blockAt == Blocks.WATER) {
                        continue;
                    }
                    terrainHeight = y;
                    break;
                }
                
                if (terrainHeight == 0) {
                    continue;
                }
                
                for (int y = 0; y <= terrainHeight; y++) {
                    double globalBlockX = x + (this.chunk.getChunkX() * Chunk.CHUNK_SIZE);
                    double globalBlockY = y;
                    double globalBlockZ = z + (this.chunk.getChunkZ() * Chunk.CHUNK_SIZE);

                    float caveValue = OpenSimplex2S.noise3_ImproveXZ(this.caveSeed,
                            globalBlockX * CAVE_SCALE, globalBlockY * CAVE_SCALE, globalBlockZ * CAVE_SCALE
                    );
                    caveValue = (caveValue + 1f) * 0.5f;
                    
                    float thresholdLerp = ((float)(terrainHeight - y)) / terrainHeight;
                    float threshold = (CAVE_MAX_THRESHOLD * thresholdLerp) + (CAVE_MIN_THRESHOLD * (1f - thresholdLerp));
                    
                    if (caveValue > threshold) {
                        continue;
                    }
                    
                    Block blockAt = this.chunk.getBlock(x, y, z);
                    if (blockAt == Blocks.AIR || blockAt == Blocks.WATER || blockAt == Blocks.BEDROCK) {
                        continue;
                    }
                    
                    if (y <= LAVA_HEIGHT) {
                        this.chunk.setBlock(x, y, z, Blocks.LAVA);
                    } else {
                        this.chunk.setBlock(x, y, z, Blocks.AIR);
                    }
                }
            }
        }
    }
    
    private void exposedDirtToGrass() {
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
                    Block blockAt = this.chunk.getBlock(x, y, z);
                    if (blockAt == Blocks.AIR) {
                        continue;
                    }
                    if (blockAt != Blocks.DIRT) {
                        break;
                    }
                    this.chunk.setBlock(x, y, z, Blocks.GRASS);
                    break;
                }
            }
        }
    }

    private void generateTrees() {
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                double globalBlockX = x + (this.chunk.getChunkX() * Chunk.CHUNK_SIZE);
                double globalBlockZ = z + (this.chunk.getChunkZ() * Chunk.CHUNK_SIZE);

                int validGrassY = -1;
                for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
                    Block blockAt = this.chunk.getBlock(x, y, z);
                    if (blockAt == Blocks.AIR) {
                        continue;
                    }
                    if (blockAt != Blocks.GRASS) {
                        break;
                    }
                    validGrassY = y;
                }
                
                if (validGrassY == -1) {
                    continue;
                }
                
                float treeNoise = OpenSimplex2S.noise2(this.treeSeed, globalBlockX * TREES_SCALE, globalBlockZ * TREES_SCALE);
                treeNoise = (treeNoise + 1f) * 0.5f;
                
                if (x < 2 || z < 2 || x >= (Chunk.CHUNK_SIZE - 2) || z >= (Chunk.CHUNK_SIZE - 2) || treeNoise > TREE_CHANCE || this.treeRandom.nextBoolean() || this.treeRandom.nextBoolean()) {
                    if (validGrassY != (Chunk.CHUNK_HEIGHT - 1) && this.grassRandom.nextBoolean() && this.grassRandom.nextBoolean()) {
                        this.chunk.setBlock(x, validGrassY + 1, z, Blocks.GRASS_LEAVES);
                    }
                    continue;
                }
                
                generateTree(x, validGrassY, z);
            }
        }
    }

    private boolean isEmpty(int startX, int startY, int startZ, int endX, int endY, int endZ) {
        if (startY < 0 || endY >= Chunk.CHUNK_HEIGHT) {
            return false;
        }
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = startY; y <= endY; y++) {
                    if (this.chunk.getBlock(x, y, z) != Blocks.AIR) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void fill(int startX, int startY, int startZ, int endX, int endY, int endZ, Block block) {
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = startY; y <= endY; y++) {
                    this.chunk.setBlock(x, y, z, block);
                }
            }
        }
    }

    private void generateTree(int x, int y, int z) {
        int trunkStartY = y + 1;
        int trunkEndY = y + 1 + TREE_HEIGHT;

        if (!isEmpty(x, trunkStartY, z, x, trunkEndY, z)) {
            return;
        }

        int leavesStartX = x - 2;
        int leavesStartY = trunkEndY - 2;
        int leavesStartZ = z - 2;

        int leavesEndX = x + 2;
        int leavesEndY = trunkEndY;
        int leavesEndZ = z + 2;

        if (!isEmpty(leavesStartX, leavesStartY, leavesStartZ, leavesEndX, leavesEndY, leavesEndZ)) {
            return;
        }

        int leaves1StartX = x - 1;
        int leaves1StartY = trunkEndY + 1;
        int leaves1StartZ = z;

        int leaves1EndX = x + 1;
        int leaves1EndY = trunkEndY + 1;
        int leaves1EndZ = z;

        if (!isEmpty(leaves1StartX, leaves1StartY, leaves1StartZ, leaves1EndX, leaves1EndY, leaves1EndZ)) {
            return;
        }

        int leaves2StartX = x;
        int leaves2StartY = trunkEndY + 1;
        int leaves2StartZ = z - 1;

        int leaves2EndX = x;
        int leaves2EndY = trunkEndY + 1;
        int leaves2EndZ = z + 1;

        if (!isEmpty(leaves2StartX, leaves2StartY, leaves2StartZ, leaves2EndX, leaves2EndY, leaves2EndZ)) {
            return;
        }

        int leaves3StartX = x;
        int leaves3StartY = trunkEndY + 2;
        int leaves3StartZ = z;

        int leaves3EndX = x;
        int leaves3EndY = trunkEndY + 3;
        int leaves3EndZ = z;

        if (!isEmpty(leaves3StartX, leaves3StartY, leaves3StartZ, leaves3EndX, leaves3EndY, leaves3EndZ)) {
            return;
        }

        fill(leavesStartX, leavesStartY, leavesStartZ, leavesEndX, leavesEndY, leavesEndZ, Blocks.FOLIAGE);
        fill(leaves1StartX, leaves1StartY, leaves1StartZ, leaves1EndX, leaves1EndY, leaves1EndZ, Blocks.FOLIAGE);
        fill(leaves2StartX, leaves2StartY, leaves2StartZ, leaves2EndX, leaves2EndY, leaves2EndZ, Blocks.FOLIAGE);
        fill(leaves3StartX, leaves3StartY, leaves3StartZ, leaves3EndX, leaves3EndY, leaves3EndZ, Blocks.FOLIAGE);
        fill(x, trunkStartY, z, x, trunkEndY, z, Blocks.WOOD);
        fill(x, y, z, x, y, z, Blocks.DIRT);
    }

    private void generate() {
        generateTerrain();
        generateCaves();
        exposedDirtToGrass();
        generateTrees();
    }

}
