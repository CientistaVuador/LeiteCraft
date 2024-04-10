package cientistavuador.leitecraft;

import java.util.Arrays;

/**
 *
 * @author Cien
 */
public class VerticesStream {

    public static final int VERTEX_SIZE = 3 + 3 + 2 + 1 + 1 + 1 + 1 + 1;

    public static final int XYZ_OFFSET = 0;
    public static final int NORMAL_XYZ_OFFSET = XYZ_OFFSET + 3;
    public static final int UV_OFFSET = NORMAL_XYZ_OFFSET + 3;
    public static final int BLEND_MODE_OFFSET = UV_OFFSET + 2;
    public static final int ANIMATED_OFFSET = BLEND_MODE_OFFSET + 1;
    public static final int ANIMATED_FRAMES_OFFSET = ANIMATED_OFFSET + 1;
    public static final int AO_OFFSET = ANIMATED_FRAMES_OFFSET + 1;
    public static final int SHADOW_OFFSET = AO_OFFSET + 1;
    
    private float[] vertices = new float[VERTEX_SIZE * 64];
    private int[] indices = new int[64];

    private int verticesIndex = 0;
    private int indicesIndex = 0;

    private int indicesOffset = 0;

    public VerticesStream() {

    }

    public int currentIndex() {
        return this.indicesIndex;
    }

    public int currentOffset() {
        return this.indicesOffset;
    }

    public void offset() {
        this.indicesOffset = this.verticesIndex / VERTEX_SIZE;
    }

    public void zeroOffset() {
        this.indicesOffset = 0;
    }

    public int amountOfVertices() {
        return this.verticesIndex / VERTEX_SIZE;
    }

    public void vertex(
            float x, float y, float z,
            float normalX, float normalY, float normalZ,
            float u, float v,
            int blendMode,
            float animatedOffset,
            int animatedFrames,
            float vertexAO,
            float vertexShadow
    ) {
        if ((this.verticesIndex + VERTEX_SIZE) > this.vertices.length) {
            this.vertices = Arrays.copyOf(this.vertices, this.vertices.length * 2);
        }
        this.vertices[this.verticesIndex + XYZ_OFFSET + 0] = x;
        this.vertices[this.verticesIndex + XYZ_OFFSET + 1] = y;
        this.vertices[this.verticesIndex + XYZ_OFFSET + 2] = z;

        this.vertices[this.verticesIndex + NORMAL_XYZ_OFFSET + 0] = normalX;
        this.vertices[this.verticesIndex + NORMAL_XYZ_OFFSET + 1] = normalY;
        this.vertices[this.verticesIndex + NORMAL_XYZ_OFFSET + 2] = normalZ;

        this.vertices[this.verticesIndex + UV_OFFSET + 0] = u;
        this.vertices[this.verticesIndex + UV_OFFSET + 1] = v;

        this.vertices[this.verticesIndex + BLEND_MODE_OFFSET + 0] = blendMode;
        this.vertices[this.verticesIndex + ANIMATED_OFFSET + 0] = animatedOffset;
        this.vertices[this.verticesIndex + ANIMATED_FRAMES_OFFSET + 0] = animatedFrames;
        this.vertices[this.verticesIndex + AO_OFFSET + 0] = vertexAO;
        this.vertices[this.verticesIndex + SHADOW_OFFSET + 0] = vertexShadow;

        this.verticesIndex += VERTEX_SIZE;
    }

    public void indices(int... inds) {
        if ((this.indicesIndex + inds.length) > this.indices.length) {
            this.indices = Arrays.copyOf(this.indices, inds.length + this.indices.length * 2);
        }
        for (int i = 0; i < inds.length; i++) {
            this.indices[this.indicesIndex + i] = inds[i] + this.indicesOffset;
        }
        this.indicesIndex += inds.length;
    }
    
    public float[] copyVertices() {
        return Arrays.copyOf(this.vertices, this.verticesIndex);
    }

    public int[] copyIndices() {
        return Arrays.copyOf(this.indices, this.indicesIndex);
    }

}
