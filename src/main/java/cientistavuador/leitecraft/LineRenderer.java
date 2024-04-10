package cientistavuador.leitecraft;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import static org.lwjgl.opengl.GL20.*;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author Cien
 */
public class LineRenderer {

    private static class LineStrip {

        final Matrix4fc projectionViewModel;
        final float[] vertices;
        final int[] first;
        final int[] count;
        final float r;
        final float g;
        final float b;
        final float a;

        public LineStrip(Matrix4fc projectionViewModel, float[] vertices, int[] first, int[] count, float r, float g, float b, float a) {
            this.projectionViewModel = projectionViewModel;
            this.vertices = vertices;
            this.first = first;
            this.count = count;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

    }

    private static final Queue<LineStrip> LINE_STRIPS = new ConcurrentLinkedQueue<>();
    private static final int LINE_BUFFER;

    static {
        LINE_BUFFER = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, LINE_BUFFER);
        glBufferData(GL_ARRAY_BUFFER, 0, GL_STREAM_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public static void init() {

    }

    public static void lineStrips(
            Matrix4fc projection,
            Matrix4fc view,
            Matrix4fc model,
            float r, float g, float b, float a,
            float[][] lineStrips
    ) {
        final Matrix4fc projectionViewModel = new Matrix4f(projection).mul(view).mul(model);

        int verticesCount = 0;
        for (float[] e : lineStrips) {
            if (e.length % 3 != 0) {
                throw new RuntimeException("Invalid line strip size.");
            }
            verticesCount += e.length / 3;
        }

        final float[] totalVertices = new float[verticesCount * 3];
        int totalVerticesIndex = 0;
        final int[] firstArray = new int[lineStrips.length];
        final int[] countArray = new int[lineStrips.length];

        int first = 0;
        for (int i = 0; i < lineStrips.length; i++) {
            float[] vertices = lineStrips[i];
            int count = vertices.length / 3;

            firstArray[i] = first;
            countArray[i] = count;

            for (int j = 0; j < count * 3; j++) {
                totalVertices[totalVerticesIndex] = vertices[j];
                totalVerticesIndex++;
            }

            first += count;
        }

        LINE_STRIPS.add(
                new LineStrip(
                        projectionViewModel,
                        totalVertices,
                        firstArray,
                        countArray,
                        r, g, b, a
                ));
    }

    public static void renderLineStrips() {
        List<LineStrip> strips = new ArrayList<>();
        {
            LineStrip s;
            while ((s = LINE_STRIPS.poll()) != null) {
                strips.add(s);
            }
        }

        if (strips.isEmpty()) {
            return;
        }

        int totalVertices = 0;
        for (LineStrip s : strips) {
            totalVertices += s.vertices.length;
        }

        float[] vertices = new float[totalVertices];

        int offset = 0;
        for (int i = 0; i < strips.size(); i++) {
            LineStrip s = strips.get(i);

            System.arraycopy(s.vertices, 0, vertices, offset * 3, s.vertices.length);

            for (int j = 0; j < s.first.length; j++) {
                s.first[j] = s.first[j] + offset;
            }

            offset += s.vertices.length / 3;
        }

        glUseProgram(WireframeShader.SHADER_PROGRAM);

        int attribute = WireframeShader.ATTRIBUTE_VERTEX_POSITION;

        glBindBuffer(GL_ARRAY_BUFFER, LINE_BUFFER);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STREAM_DRAW);
        glEnableVertexAttribArray(attribute);
        glVertexAttribPointer(attribute, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);

        for (int i = 0; i < strips.size(); i++) {
            LineStrip s = strips.get(i);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(4 * 4);
                s.projectionViewModel.get(buffer);
                glUniformMatrix4fv(WireframeShader.UNIFORM_PROJECTION_VIEW_MODEL, false, buffer);
            }

            glUniform4f(WireframeShader.UNIFORM_COLOR, s.r * s.a, s.g * s.a, s.b * s.a, s.a);
            
            glMultiDrawArrays(GL_LINE_STRIP, s.first, s.count);
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glUseProgram(0);
    }

    private LineRenderer() {

    }

}
