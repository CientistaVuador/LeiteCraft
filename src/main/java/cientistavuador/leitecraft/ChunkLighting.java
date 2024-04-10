package cientistavuador.leitecraft;

/**
 *
 * @author Cien
 */
public class ChunkLighting {

    public static void computeLighting(ChunkMatrix matrix) {
        new ChunkLighting(matrix).compute();
    }

    public static final int SIZE = Chunk.CHUNK_SIZE * 3;
    public static final int HEIGHT = Chunk.CHUNK_HEIGHT;

    private final ChunkMatrix matrix;
    
    private final boolean[] noWorkDoneCache = new boolean[HEIGHT];
    private final boolean[] solidCache = new boolean[SIZE * SIZE * HEIGHT];
    
    private byte[] lightingRead = new byte[SIZE * SIZE * HEIGHT];
    private byte[] lightingWrite = new byte[SIZE * SIZE * HEIGHT];
    
    private ChunkLighting(ChunkMatrix matrix) {
        this.matrix = matrix;
    }

    private void flipBuffers() {
        byte[] a = this.lightingWrite;
        byte[] b = this.lightingRead;
        this.lightingWrite = b;
        this.lightingRead = a;
    }

    private int getLightLevel(int x, int y, int z) {
        if (y < 0 || y >= HEIGHT) {
            return Chunk.MAX_LIGHT_LEVEL;
        }
        x += Chunk.CHUNK_SIZE;
        z += Chunk.CHUNK_SIZE;
        if (x < 0 || x >= SIZE) {
            return 0;
        }
        if (z < 0 || z >= SIZE) {
            return 0;
        }
        return this.lightingRead[x + (y * SIZE * SIZE) + (z * SIZE)];
    }

    private void setLightLevel(int x, int y, int z, int lightLevel) {
        x += Chunk.CHUNK_SIZE;
        z += Chunk.CHUNK_SIZE;
        this.lightingWrite[x + (y * SIZE * SIZE) + (z * SIZE)] = (byte) lightLevel;
    }

    private boolean isSolid(int x, int y, int z) {
        x += Chunk.CHUNK_SIZE;
        z += Chunk.CHUNK_SIZE;
        return this.solidCache[x + (y * SIZE * SIZE) + (z * SIZE)];
    }

    private void setSolid(int x, int y, int z, boolean solid) {
        x += Chunk.CHUNK_SIZE;
        z += Chunk.CHUNK_SIZE;
        this.solidCache[x + (y * SIZE * SIZE) + (z * SIZE)] = solid;
    }

    private void prepareLightingAndSolidCache() {
        for (int absZ = 0; absZ < SIZE; absZ++) {
            for (int absX = 0; absX < SIZE; absX++) {
                boolean foundGround = false;
                for (int absY = HEIGHT - 1; absY >= 0; absY--) {
                    int x = absX - Chunk.CHUNK_SIZE;
                    int y = absY;
                    int z = absZ - Chunk.CHUNK_SIZE;

                    Block blockAt = this.matrix.getBlock(x, y, z);

                    boolean solid = blockAt != Blocks.AIR && !blockAt.isTransparent();
                    setSolid(x, y, z, solid);

                    if (solid) {
                        foundGround = true;
                    }

                    if (!foundGround && this.matrix.chunkFromBlock(x, z) != null) {
                        setLightLevel(x, y, z, Chunk.MAX_LIGHT_LEVEL);
                    } else {
                        if (blockAt != Blocks.AIR) {
                            setLightLevel(x, y, z, blockAt.getLightEmission());
                        } else {
                            setLightLevel(x, y, z, 0);
                        }
                    }
                }
            }
        }

    }
    
    private void updateLighting() {
        System.arraycopy(this.lightingRead, 0, this.lightingWrite, 0, this.lightingWrite.length);
        
        for (int absY = 0; absY < HEIGHT; absY++) {
            if (this.noWorkDoneCache[absY]) {
                continue;
            }
            boolean noWorkDone = true;
            for (int absZ = 0; absZ < SIZE; absZ++) {
                for (int absX = 0; absX < SIZE; absX++) {
                    int x = absX - Chunk.CHUNK_SIZE;
                    int y = absY;
                    int z = absZ - Chunk.CHUNK_SIZE;
                    
                    int currentLightLevel = getLightLevel(x, y, z);
                    
                    if (currentLightLevel >= (Chunk.MAX_LIGHT_LEVEL - 1)) {
                        continue;
                    }
                    
                    boolean solid = isSolid(x, y, z);
                    if (solid) {
                        continue;
                    }
                    
                    noWorkDone = false;
                    
                    int negativeX = getLightLevel(x - 1, y, z) - 1;
                    int positiveX = getLightLevel(x + 1, y, z) - 1;
                    int negativeY = getLightLevel(x, y - 1, z) - 1;
                    int positiveY = getLightLevel(x, y + 1, z) - 1;
                    int negativeZ = getLightLevel(x, y, z - 1) - 1;
                    int positiveZ = getLightLevel(x, y, z + 1) - 1;
                    
                    setLightLevel(x, y, z, 
                            Math.max(
                                    Math.max(
                                            currentLightLevel,
                                            Math.max(negativeX, positiveX)
                                    ),
                                    Math.max(
                                            Math.max(negativeY, positiveY),
                                            Math.max(negativeZ, positiveZ)
                                    )
                            )
                    );
                }
            }
            this.noWorkDoneCache[absY] = noWorkDone;
        }
    }
    
    private void output() {
        Chunk center = this.matrix.getChunk(0, 0);
        for (int y = -1; y < HEIGHT + 1; y++) {
            for (int z = -1; z < Chunk.CHUNK_SIZE + 1; z++) {
                for (int x = -1; x < Chunk.CHUNK_SIZE + 1; x++) {
                    center.setLightLevel(x, y, z, getLightLevel(x, y, z));
                }
            }
        }
    }

    private void compute() {
        prepareLightingAndSolidCache();
        flipBuffers();
        
        for (int i = 0; i < Chunk.MAX_LIGHT_LEVEL; i++) {
            updateLighting();
            flipBuffers();
        }
        
        output();
    }

}
