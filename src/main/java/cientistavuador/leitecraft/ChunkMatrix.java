package cientistavuador.leitecraft;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 *
 * @author Cien
 */
public class ChunkMatrix {
    
    private final Chunk[] matrix;
    
    public ChunkMatrix() {
        this.matrix = new Chunk[3*3];
    }
    
    public ChunkMatrix(ChunkMatrix copy) {
        this.matrix = copy.matrix.clone();
    }
    
    public int length() {
        return this.matrix.length;
    }
    
    public Chunk getChunk(int index) {
        return this.matrix[index];
    }
    
    public void setChunk(int index, Chunk chunk) {
        this.matrix[index] = chunk;
    }
    
    public Chunk getChunk(int x, int z) {
        return this.matrix[(x + 1) + ((z + 1) * 3)];
    }
    
    public void setChunk(int x, int z, Chunk chunk) {
        this.matrix[(x + 1) + ((z + 1) * 3)] = chunk;
    }
    
    public Chunk chunkFromBlock(int x, int z) {
        int chunkX = (int) Math.floor(((float)x) / Chunk.CHUNK_SIZE);
        int chunkZ = (int) Math.floor(((float)z) / Chunk.CHUNK_SIZE);
        if (chunkX < -1 || chunkX > 1) {
            return null;
        }
        if (chunkZ < -1 || chunkZ > 1) {
            return null;
        }
        
        return getChunk(chunkX, chunkZ);
    }
    
    public Block getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.CHUNK_HEIGHT) {
            return Blocks.AIR;
        }
        int chunkX = (int) Math.floor(((float)x) / Chunk.CHUNK_SIZE);
        int chunkZ = (int) Math.floor(((float)z) / Chunk.CHUNK_SIZE);
        if (chunkX < -1 || chunkX > 1) {
            return Blocks.AIR;
        }
        if (chunkZ < -1 || chunkZ > 1) {
            return Blocks.AIR;
        }
        
        Chunk chunk = getChunk(chunkX, chunkZ);
        
        if (chunk == null) {
            return Blocks.AIR;
        }
        
        x = x - (Chunk.CHUNK_SIZE * chunkX);
        z = z - (Chunk.CHUNK_SIZE * chunkZ);
        
        return chunk.getBlock(x, y, z);
    }
    
    public int getLightLevel(int x, int y, int z) {
        if (y < 0 || y >= Chunk.CHUNK_HEIGHT) {
            return Chunk.MAX_LIGHT_LEVEL;
        }
        int chunkX = (int) Math.floor(((float)x) / Chunk.CHUNK_SIZE);
        int chunkZ = (int) Math.floor(((float)z) / Chunk.CHUNK_SIZE);
        if (chunkX < -1 || chunkX > 1) {
            return 0;
        }
        if (chunkZ < -1 || chunkZ > 1) {
            return 0;
        }
        
        Chunk chunk = getChunk(chunkX, chunkZ);
        
        if (chunk == null) {
            return 0;
        }
        
        x -= (Chunk.CHUNK_SIZE * chunkX);
        z -= (Chunk.CHUNK_SIZE * chunkZ);
        
        return chunk.getLightLevel(x, y, z);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Arrays.deepHashCode(this.matrix);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChunkMatrix other = (ChunkMatrix) obj;
        return Arrays.deepEquals(this.matrix, other.matrix);
    }
    
    
    
}
