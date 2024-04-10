package cientistavuador.leitecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author Cien
 */
public class World {

    private static class GeneratingChunk {

        private final Chunk chunk;
        private final Future<?> task;

        public GeneratingChunk(Chunk chunk, Future<?> task) {
            this.chunk = chunk;
            this.task = task;
        }
    }

    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final Map<Long, GeneratingChunk> chunksGenerating = new HashMap<>();

    public World() {

    }
    
    private long chunkId(int x, int z) {
        return (Integer.toUnsignedLong(x) << 32) | Integer.toUnsignedLong(z);
    }
    
    public Chunk getChunk(int x, int z, boolean forced) {
        long id = chunkId(x, z);
        
        Chunk chunkAt = this.chunks.get(id);
        if (chunkAt == null) {
            GeneratingChunk generating = this.chunksGenerating.get(id);
            if (generating != null && (generating.task.isDone() || forced)) {
                try {
                    generating.task.get();
                } catch (InterruptedException | ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
                chunkAt = generating.chunk;
                this.chunksGenerating.remove(id);
                this.chunks.put(id, chunkAt);
            } else if (generating == null && forced) {
                chunkAt = new Chunk(x, z);
                ChunkGenerator.generateChunk(chunkAt);
                this.chunks.put(id, chunkAt);
            }
        }

        return chunkAt;
    }

    public Chunk getChunk(int x, int z) {
        return getChunk(x, z, true);
    }

    public Chunk getChunkOrNull(int x, int z) {
        return getChunk(x, z, false);
    }
    
    public Block getBlock(int x, int y, int z, boolean force) {
        int chunkX = (int) Math.floor(((double) x) / Chunk.CHUNK_SIZE);
        int chunkZ = (int) Math.floor(((double) z) / Chunk.CHUNK_SIZE);

        Chunk chunk = getChunk(chunkX, chunkZ, force);
        if (chunk == null) {
            return Blocks.AIR;
        }
        
        int localX = x - (chunkX * Chunk.CHUNK_SIZE);
        int localZ = z - (chunkZ * Chunk.CHUNK_SIZE);

        return chunk.getBlock(localX, y, localZ);
    }
    
    public Block getBlock(int x, int y, int z) {
        return getBlock(x, y, z, true);
    }
    
    public Block getBlockOrNull(int x, int y, int z) {
        return getBlock(x, y, z, false);
    }
    
    public boolean setBlock(Block block, int x, int y, int z, boolean force) {
        if (y < 0 || y >= Chunk.CHUNK_HEIGHT) {
            return false;
        }
        
        int chunkX = (int) Math.floor(((double) x) / Chunk.CHUNK_SIZE);
        int chunkZ = (int) Math.floor(((double) z) / Chunk.CHUNK_SIZE);
        
        Chunk chunk = getChunk(chunkX, chunkZ, force);
        if (chunk == null) {
            return false;
        }
        
        int localX = x - (chunkX * Chunk.CHUNK_SIZE);
        int localZ = z - (chunkZ * Chunk.CHUNK_SIZE);
        
        chunk.setBlock(localX, y, localZ, block);
        
        return true;
    }
    
    public boolean setBlock(Block block, int x, int y, int z) {
        return setBlock(block, x, y, z, true);
    }
    
    public boolean setBlockOrNull(Block block, int x, int y, int z) {
        return setBlock(block, x, y, z, false);
    }
    
    public boolean isGenerating(int x, int z) {
        return this.chunksGenerating.get(chunkId(x, z)) != null;
    }

    public Chunk scheduleChunk(int x, int z) {
        Chunk chunkAt = getChunkOrNull(x, z);
        if (chunkAt != null) {
            return chunkAt;
        }

        if (!isGenerating(x, z)) {
            Chunk toGenerate = new Chunk(x, z);
            GeneratingChunk generating = new GeneratingChunk(toGenerate, Main.THREADS.submit(() -> {
                ChunkGenerator.generateChunk(toGenerate);
            }));
            this.chunksGenerating.put(chunkId(x, z), generating);
        }
        
        return null;
    }
    
    public void performCleanup(int chunkCenterX, int chunkCenterZ, int maxChunks) {
        List<Chunk> generated = new ArrayList<>();
        for (Map.Entry<Long, Chunk> entry:this.chunks.entrySet()) {
            generated.add(entry.getValue());
        }
        
        this.chunks.clear();
        
        generated.sort((o1, o2) -> {
            int o1x = o1.getChunkX() - chunkCenterX;
            int o1z = o1.getChunkZ() - chunkCenterZ;
            int o2x = o2.getChunkX() - chunkCenterX;
            int o2z = o2.getChunkZ() - chunkCenterZ;
            
            return Integer.compare(
                    (o1x * o1x) + (o1z * o1z),
                    (o2x * o2x) + (o2z * o2z)
            );
        });
        
        for (Chunk c:generated) {
            this.chunks.put(chunkId(c.getChunkX(), c.getChunkZ()), c);
            maxChunks--;
            if (maxChunks <= 0) {
                break;
            }
        }
    }

}
