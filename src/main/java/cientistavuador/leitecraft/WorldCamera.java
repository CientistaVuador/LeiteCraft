package cientistavuador.leitecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joml.Vector2i;
import static org.lwjgl.opengl.GL20.*;

/**
 *
 * @author Cien
 */
public class WorldCamera {

    private final World world;

    private boolean updateViewDistance = false;
    private int nextViewDistance = 5;

    private int viewDistance = 5;
    private int viewDistanceSize = (this.viewDistance * 2) + 1;

    private RenderableChunk[] chunks = new RenderableChunk[this.viewDistanceSize * this.viewDistanceSize];
    private Vector2i[] chunksPositions = new Vector2i[this.chunks.length];

    {
        updateChunkPositions();
    }
    
    private int cameraChunkX = 0;
    private int cameraChunkZ = 0;

    public WorldCamera(World world) {
        this.world = world;
    }

    private void updateChunkPositions() {
        for (int z = 0; z < this.viewDistanceSize; z++) {
            for (int x = 0; x < this.viewDistanceSize; x++) {
                int localX = x - this.viewDistance;
                int localZ = z - this.viewDistance;

                this.chunksPositions[x + (z * this.viewDistanceSize)] = new Vector2i(localX, localZ);
            }
        }

        Arrays.sort(this.chunksPositions, (o1, o2) -> {
            long disto1 = o1.lengthSquared();
            long disto2 = o2.lengthSquared();
            return Long.compare(disto1, disto2);
        });
    }

    public World getWorld() {
        return world;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public int getViewDistanceSize() {
        return viewDistanceSize;
    }

    public void scheduleViewDistanceUpdate(int viewDistance) {
        this.nextViewDistance = viewDistance;
        this.updateViewDistance = true;
    }

    public int amountOfRenderableChunks() {
        return this.chunks.length;
    }

    public RenderableChunk getRenderableChunk(int index) {
        return this.chunks[index];
    }

    public RenderableChunk getRenderableChunk(int localX, int localZ) {
        if (localX < -this.viewDistance || localX > this.viewDistance || localZ < -this.viewDistance || localZ > this.viewDistance) {
            return null;
        }
        localX += this.viewDistance;
        localZ += this.viewDistance;
        return this.chunks[localX + (localZ * this.viewDistanceSize)];
    }

    public void setRenderableChunk(int localX, int localZ, RenderableChunk chunk) {
        localX += this.viewDistance;
        localZ += this.viewDistance;
        this.chunks[localX + (localZ * this.viewDistanceSize)] = chunk;
    }

    public void signalRegenerateMesh(int chunkX, int chunkZ) {
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                int cx = (chunkX + xOffset) - this.cameraChunkX;
                int cz = (chunkZ + zOffset) - this.cameraChunkZ;

                RenderableChunk at = getRenderableChunk(cx, cz);
                if (at != null) {
                    at.signalRegenerateMesh();
                }
            }
        }
    }

    public Chunk getLocalChunk(int localX, int localZ) {
        RenderableChunk e = getRenderableChunk(localX, localZ);
        if (e == null) {
            return null;
        }
        return e.getChunk();
    }

    public void update(double camX, double camZ) {
        this.cameraChunkX = (int) (Math.floor(camX / Chunk.CHUNK_SIZE));
        this.cameraChunkZ = (int) (Math.floor(camZ / Chunk.CHUNK_SIZE));

        this.world.performCleanup(this.cameraChunkX, this.cameraChunkZ, (this.viewDistanceSize * this.viewDistanceSize) * 4);

        List<RenderableChunk> existingChunks = new ArrayList<>();
        for (RenderableChunk c : this.chunks) {
            if (c != null) {
                existingChunks.add(c);
            }
        }

        Arrays.fill(this.chunks, null);

        if (this.updateViewDistance) {
            this.viewDistance = this.nextViewDistance;
            this.viewDistanceSize = (this.viewDistance * 2) + 1;
            this.updateViewDistance = false;
            
            this.chunks = new RenderableChunk[this.viewDistanceSize * this.viewDistanceSize];
            this.chunksPositions = new Vector2i[this.chunks.length];
            
            updateChunkPositions();
        }

        for (Vector2i local : this.chunksPositions) {
            int localX = local.x();
            int localZ = local.y();
            int globalX = localX + this.cameraChunkX;
            int globalZ = localZ + this.cameraChunkZ;

            RenderableChunk existing = null;
            for (RenderableChunk c : existingChunks) {
                Chunk chunk = c.getChunk();
                if (chunk.getChunkX() == globalX && chunk.getChunkZ() == globalZ) {
                    existing = c;
                    break;
                }
            }

            if (existing != null) {
                existingChunks.remove(existing);
            } else {
                Chunk chunkAt = this.world.scheduleChunk(globalX, globalZ);
                if (chunkAt != null) {
                    existing = new RenderableChunk(this, chunkAt);
                    chunkAt.setRenderableChunk(existing);
                }
            }

            setRenderableChunk(localX, localZ, existing);
        }

        for (RenderableChunk toDelete : existingChunks) {
            if (toDelete.getVBO() != 0) {
                glDeleteBuffers(toDelete.getVBO());
            }
            if (toDelete.getEBO() != 0) {
                glDeleteBuffers(toDelete.getEBO());
            }
            if (toDelete.getQueryObject() != 0) {
                glDeleteQueries(toDelete.getQueryObject());
            }
            toDelete.getChunk().setRenderableChunk(null);
        }

        for (RenderableChunk r : this.chunks) {
            if (r != null) {
                r.setHighPriority(false);
            }
        }

        for (int priorityX = -1; priorityX <= 1; priorityX++) {
            for (int priorityZ = -1; priorityZ <= 1; priorityZ++) {
                RenderableChunk at = getRenderableChunk(priorityX, priorityZ);
                if (at != null) {
                    at.setHighPriority(true);
                }
            }
        }

        ChunkMatrix matrix = new ChunkMatrix();
        for (Vector2i local : this.chunksPositions) {
            int localX = local.x();
            int localZ = local.y();

            RenderableChunk renderable = getRenderableChunk(localX, localZ);

            if (renderable != null) {
                matrix.setChunk(0, 0, renderable.getChunk());

                matrix.setChunk(-1, 0, getLocalChunk(localX - 1, localZ));
                matrix.setChunk(1, 0, getLocalChunk(localX + 1, localZ));
                matrix.setChunk(0, -1, getLocalChunk(localX, localZ - 1));
                matrix.setChunk(0, 1, getLocalChunk(localX, localZ + 1));

                matrix.setChunk(1, 1, getLocalChunk(localX + 1, localZ + 1));
                matrix.setChunk(-1, -1, getLocalChunk(localX - 1, localZ - 1));
                matrix.setChunk(1, -1, getLocalChunk(localX + 1, localZ - 1));
                matrix.setChunk(-1, 1, getLocalChunk(localX - 1, localZ + 1));

                renderable.update(matrix);
            }
        }
    }

}
