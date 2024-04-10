package cientistavuador.leitecraft;

import static cientistavuador.leitecraft.Chunk.CHUNK_HEIGHT;
import static cientistavuador.leitecraft.Chunk.CHUNK_SIZE;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import static org.lwjgl.opengl.GL20.*;

/**
 *
 * @author Cien
 */
public class RenderableChunk {

    private static class FutureUpdate {

        private Chunk lighting = null;
        private float[] vertices = null;
        private int[] indices = null;
        private int solidCount = 0;
        private int solidOffset = 0;
        private int alphaCount = 0;
        private int alphaOffset = 0;
        private final Vector3f halfExtents = new Vector3f();
        private final Vector3f center = new Vector3f();
        private final Vector2f textureHalfExtents = new Vector2f();
        private final Vector2f textureCenter = new Vector2f();
    }

    private final WorldCamera worldCamera;
    private final Chunk chunk;

    private ChunkMatrix chunkMatrix;

    private Future<FutureUpdate> futureUpdate = null;
    private ChunkMatrix futureMatrix = null;
    private boolean readyForUpdate = false;
    private boolean shouldUpdate = false;

    private int vbo = 0;
    private int ebo = 0;
    private int queryObject = 0;

    private float[] vertices = null;
    private int[] indices = null;

    private int solidCount = 0;
    private int solidOffset = 0;
    private int alphaCount = 0;
    private int alphaOffset = 0;

    private final Vector3f halfExtents = new Vector3f();
    private final Vector3f center = new Vector3f();

    private final Vector2f textureHalfExtents = new Vector2f();
    private final Vector2f textureCenter = new Vector2f();

    private final Vector3f relativeCameraLocation = new Vector3f();

    private final Matrix4f modelMatrix = new Matrix4f();
    private final Matrix4f occlusionModelMatrix = new Matrix4f();

    private boolean regenerateMesh = true;
    private boolean highPriority = false;

    public RenderableChunk(WorldCamera worldCamera, Chunk chunk) {
        this.worldCamera = worldCamera;
        this.chunk = chunk;
    }

    public WorldCamera getWorldCamera() {
        return worldCamera;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public int getVBO() {
        return vbo;
    }

    public int getEBO() {
        return ebo;
    }
    
    public int getQueryObject() {
        return queryObject;
    }

    public void setQueryObject(int queryObject) {
        this.queryObject = queryObject;
    }

    public float[] vertices() {
        return this.vertices;
    }

    public int[] indices() {
        return this.indices;
    }

    public int getSolidCount() {
        return solidCount;
    }

    public int getSolidOffset() {
        return solidOffset;
    }

    public int getAlphaCount() {
        return alphaCount;
    }

    public int getAlphaOffset() {
        return alphaOffset;
    }

    public Vector3fc getHalfExtents() {
        return halfExtents;
    }

    public Vector3fc getCenter() {
        return center;
    }

    public Vector2fc getTextureHalfExtents() {
        return textureHalfExtents;
    }

    public Vector2fc getTextureCenter() {
        return textureCenter;
    }

    public Vector3fc getRelativeCameraLocation() {
        return relativeCameraLocation;
    }

    public void calculateRelativeLocationAndMatrices(Camera camera) {
        double blockX = this.chunk.getChunkX() * Chunk.CHUNK_SIZE;
        double blockZ = this.chunk.getChunkZ() * Chunk.CHUNK_SIZE;

        float relativeX = (float) (blockX - camera.getPosition().x());
        float relativeZ = (float) (blockZ - camera.getPosition().z());

        this.relativeCameraLocation.set(relativeX, -camera.getPosition().y(), relativeZ);

        this.modelMatrix
                .identity()
                .translate(
                        this.relativeCameraLocation
                );

        this.occlusionModelMatrix
                .set(this.modelMatrix)
                .translate(this.center)
                .scale(this.halfExtents);
    }

    public Matrix4fc getModelMatrix() {
        return modelMatrix;
    }

    public Matrix4fc getOcclusionModelMatrix() {
        return occlusionModelMatrix;
    }

    public void signalRegenerateMesh() {
        this.regenerateMesh = true;
    }

    public boolean isReadyForUpdate() {
        return readyForUpdate;
    }

    public boolean isHighPriority() {
        return highPriority;
    }

    public void setHighPriority(boolean highPriority) {
        this.highPriority = highPriority;
    }

    private void checkFutureUpdate() {
        if (this.futureUpdate != null && this.futureUpdate.isDone()) {
            this.readyForUpdate = true;

            if (!this.shouldUpdate) {
                return;
            }

            if (Game.CAN_UPDATE_CHUNK || this.highPriority) {
                try {
                    FutureUpdate update = this.futureUpdate.get();

                    if (this.vbo == 0) {
                        this.vbo = glGenBuffers();
                    }
                    if (this.ebo == 0) {
                        this.ebo = glGenBuffers();
                    }

                    this.chunk.copyLightingFrom(update.lighting);
                    
                    this.vertices = update.vertices;
                    this.indices = update.indices;

                    this.solidOffset = update.solidOffset;
                    this.solidCount = update.solidCount;

                    this.alphaOffset = update.alphaOffset;
                    this.alphaCount = update.alphaCount;

                    this.halfExtents.set(update.halfExtents);
                    this.center.set(update.center);

                    this.textureHalfExtents.set(update.textureHalfExtents);
                    this.textureCenter.set(update.textureCenter);

                    glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
                    glBufferData(GL_ARRAY_BUFFER, this.vertices, GL_STATIC_DRAW);
                    glBindBuffer(GL_ARRAY_BUFFER, 0);

                    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.ebo);
                    glBufferData(GL_ELEMENT_ARRAY_BUFFER, this.indices, GL_STATIC_DRAW);
                    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

                    this.futureUpdate = null;
                    Game.CAN_UPDATE_CHUNK = false;
                } catch (InterruptedException | ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private void scheduleFutureUpdate(ChunkMatrix matrix) {
        final ChunkMatrix cloneMatrix = new ChunkMatrix(matrix);
        for (int i = 0; i < cloneMatrix.length(); i++) {
            Chunk toCopy = cloneMatrix.getChunk(i);
            if (toCopy != null) {
                cloneMatrix.setChunk(i, new Chunk(toCopy));
            } else {
                cloneMatrix.setChunk(i, null);
            }
        }

        this.futureUpdate = Main.THREADS.submit(() -> {
            FutureUpdate update = new FutureUpdate();

            Chunk centerChunk = cloneMatrix.getChunk(0, 0);
            update.lighting = centerChunk;

            ChunkLighting.computeLighting(cloneMatrix);

            VerticesStream solidStream = new VerticesStream();
            VerticesStream alphaStream = new VerticesStream();

            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    for (int x = 0; x < CHUNK_SIZE; x++) {
                        Block b = centerChunk.getBlock(x, y, z);
                        if (b != Blocks.AIR) {
                            VerticesStream stream = solidStream;
                            if (b.isAlphaEnabled()) {
                                stream = alphaStream;
                            }
                            b.writeBlockVertices(x, y, z,
                                    stream,
                                    cloneMatrix
                            );
                        }
                    }
                }
            }

            float[] solidVertices = solidStream.copyVertices();
            int[] solidIndices = solidStream.copyIndices();

            float[] alphaVertices = alphaStream.copyVertices();
            int[] alphaIndices = alphaStream.copyIndices();

            update.vertices = new float[solidVertices.length + alphaVertices.length];
            update.indices = new int[solidIndices.length + alphaIndices.length];

            System.arraycopy(solidVertices, 0, update.vertices, 0, solidVertices.length);
            System.arraycopy(alphaVertices, 0, update.vertices, solidVertices.length, alphaVertices.length);

            System.arraycopy(solidIndices, 0, update.indices, 0, solidIndices.length);
            System.arraycopy(alphaIndices, 0, update.indices, solidIndices.length, alphaIndices.length);

            Vector3f min = new Vector3f();
            Vector3f max = new Vector3f();

            Utils.aabb(
                    update.vertices,
                    VerticesStream.XYZ_OFFSET, 3,
                    VerticesStream.VERTEX_SIZE,
                    min, max
            );

            update.halfExtents.set(max).sub(min).mul(0.5f);
            update.center.set(max).add(min).mul(0.5f);

            int offset = solidStream.amountOfVertices();

            for (int i = solidIndices.length; i < solidIndices.length + alphaIndices.length; i++) {
                update.indices[i] = update.indices[i] + offset;
            }

            update.solidOffset = 0;
            update.solidCount = solidIndices.length;

            update.alphaOffset = update.solidCount * Integer.BYTES;
            update.alphaCount = alphaIndices.length;

            return update;
        });
    }

    public void update(ChunkMatrix matrix) {
        checkFutureUpdate();

        this.chunkMatrix = new ChunkMatrix(matrix);

        if (this.futureUpdate == null && (!this.chunkMatrix.equals(this.futureMatrix) || this.regenerateMesh)) {
            scheduleFutureUpdate(this.chunkMatrix);

            this.futureMatrix = new ChunkMatrix(this.chunkMatrix);
            this.regenerateMesh = false;
            this.readyForUpdate = false;
        }

        this.shouldUpdate = true;
        for (int i = 0; i < matrix.length(); i++) {
            Chunk e = matrix.getChunk(i);
            if (e == null) {
                continue;
            }
            RenderableChunk r = e.getRenderableChunk();
            if (r != null && !r.readyForUpdate) {
                this.shouldUpdate = false;
                break;
            }
        }
    }

}
