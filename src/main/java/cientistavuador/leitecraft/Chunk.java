package cientistavuador.leitecraft;

import java.util.Arrays;

/**
 *
 * @author Cien
 */
public class Chunk {
    
    public static final int MAX_LIGHT_LEVEL = 16;

    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 128;
    
    private final byte[] blocks = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT];
    private final byte[] blocksData = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT];
    private final byte[] lighting = new byte[(CHUNK_SIZE + 2) * (CHUNK_SIZE + 2) * (CHUNK_HEIGHT + 2)];
    
    private final int chunkX;
    private final int chunkZ;
    
    private RenderableChunk renderableChunk = null;
    
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        Arrays.fill(this.lighting, (byte) 32);
    }
    
    public Chunk(Chunk toCopy) {
        this.chunkX = toCopy.chunkX;
        this.chunkZ = toCopy.chunkZ;
        System.arraycopy(toCopy.blocks, 0, this.blocks, 0, this.blocks.length);
        System.arraycopy(toCopy.blocksData, 0, this.blocksData, 0, this.blocksData.length);
        System.arraycopy(toCopy.lighting, 0, this.lighting, 0, this.lighting.length);
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public Block getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.CHUNK_HEIGHT) {
            return Blocks.AIR;
        }
        return Blocks.getBlock(
                ((int) this.blocks[x + (y * CHUNK_SIZE * CHUNK_SIZE) + (z * CHUNK_SIZE)]) & 0xFF
        );
    }
    
    public void setBlock(int x, int y, int z, Block b) {
        int id = 0;
        if (b != Blocks.AIR) {
            id = b.getId();
        }
        this.blocks[x + (y * CHUNK_SIZE * CHUNK_SIZE) + (z * CHUNK_SIZE)] = (byte) id;
        int data = 0;
        if (b != Blocks.AIR) {
            data = b.getDefaultData();
        }
        setData(x, y, z, data);
        if (this.renderableChunk != null) {
            this.renderableChunk.getWorldCamera().signalRegenerateMesh(this.chunkX, this.chunkZ);
        }
    }
    
    public int getData(int x, int y, int z) {
        return this.blocksData[x + (y * CHUNK_SIZE * CHUNK_SIZE) + (z * CHUNK_SIZE)] & 0xFF;
    }
    
    public void setData(int x, int y, int z, int data) {
        this.blocksData[x + (y * CHUNK_SIZE * CHUNK_SIZE) + (z * CHUNK_SIZE)] = (byte) data;
    }
    
    public int getLightLevel(int x, int y, int z) {
        x += 1;
        y += 1;
        z += 1;
        return this.lighting[x + (y * (CHUNK_SIZE + 2) * (CHUNK_SIZE + 2)) + (z * (CHUNK_SIZE + 2))];
    }

    public void setLightLevel(int x, int y, int z, int lightLevel) {
        x += 1;
        y += 1;
        z += 1;
        this.lighting[x + (y * (CHUNK_SIZE + 2) * (CHUNK_SIZE + 2)) + (z * (CHUNK_SIZE + 2))] = (byte) lightLevel;
    }

    public float getLightLevelForRendering(int x, int y, int z) {
        float norm = ((float) getLightLevel(x, y, z)) / Chunk.MAX_LIGHT_LEVEL;
        return norm * norm;
    }
    
    public void copyLightingFrom(Chunk other) {
        System.arraycopy(other.lighting, 0, this.lighting, 0, other.lighting.length);
    }

    public RenderableChunk getRenderableChunk() {
        return renderableChunk;
    }

    public void setRenderableChunk(RenderableChunk renderableChunk) {
        this.renderableChunk = renderableChunk;
    }
    
}
