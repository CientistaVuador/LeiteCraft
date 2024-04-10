package cientistavuador.leitecraft;

import java.nio.FloatBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL20.*;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author Cien
 */
public class GPUOcclusion {

    private static final int VBO;
    private static final int EBO;
    private static final int COUNT;
    
    public static final float MARGIN = 0.02f;
    
    public static final float SMALL_CUBE_SCALE = 1f - MARGIN;
    public static final float LARGE_CUBE_SCALE = 1f + MARGIN;
    
    static {
        final float sa = SMALL_CUBE_SCALE;
        final float sb = LARGE_CUBE_SCALE;
        
        float[] vertices = {
            -1f * sa, -1f * sa, -1f * sa,
            1f * sa, -1f * sa, -1f * sa,
            -1f * sa, -1f * sa, 1f * sa,
            1f * sa, -1f * sa, 1f * sa,
            -1f * sa, 1f * sa, -1f * sa,
            1f * sa, 1f * sa, -1f * sa,
            -1f * sa, 1f * sa, 1f * sa,
            1f * sa, 1f * sa, 1f * sa,
            
            -1f, -1f, -1f,
            1f, -1f, -1f,
            -1f, -1f, 1f,
            1f, -1f, 1f,
            -1f, 1f, -1f,
            1f, 1f, -1f,
            -1f, 1f, 1f,
            1f, 1f, 1f,
            
            -1f * sb, -1f * sb, -1f * sb,
            1f * sb, -1f * sb, -1f * sb,
            -1f * sb, -1f * sb, 1f * sb,
            1f * sb, -1f * sb, 1f * sb,
            -1f * sb, 1f * sb, -1f * sb,
            1f * sb, 1f * sb, -1f * sb,
            -1f * sb, 1f * sb, 1f * sb,
            1f * sb, 1f * sb, 1f * sb,
        };
        int[] indices = {
            0, 5, 1, 0, 4, 5,
            2, 3, 7, 2, 7, 6,
            0, 2, 6, 0, 6, 4,
            1, 7, 3, 1, 5, 7,
            4, 6, 5, 5, 6, 7,
            0, 3, 2, 0, 1, 3,
            8, 13, 9, 8, 12, 13,
            10, 11, 15, 10, 15, 14,
            8, 10, 14, 8, 14, 12,
            9, 15, 11, 9, 13, 15,
            12, 14, 13, 13, 14, 15,
            8, 11, 10, 8, 9, 11,
            16, 21, 17, 16, 20, 21,
            18, 19, 23, 18, 23, 22,
            16, 18, 22, 16, 22, 20,
            17, 23, 19, 17, 21, 23,
            20, 22, 21, 21, 22, 23,
            16, 19, 18, 16, 17, 19
        };

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        VBO = vbo;
        EBO = ebo;
        COUNT = indices.length;
    }

    private static final int SHADER_PROGRAM = ProgramCompiler.compile(
            """
            #version 110
            
            uniform mat4 projectionViewModel;
            
            attribute vec3 vertexPosition;
            
            void main() {
                gl_Position = projectionViewModel * vec4(vertexPosition, 1.0);
            }
            """,
            """
            #version 110
            
            void main() {
                gl_FragColor = vec4(1.0);
            }
            """
    );

    private static final int UNIFORM_PROJECTION_VIEW_MODEL = glGetUniformLocation(SHADER_PROGRAM, "projectionViewModel");
    private static final int ATTRIBUTE_POSITION = glGetAttribLocation(SHADER_PROGRAM, "vertexPosition");

    public static void init() {

    }

    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();
    
    public static boolean testAabPoint(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float px, float py, float pz) {
        return !(px < minX || px > maxX || py < minY || py > maxY || pz < minZ || pz > maxZ);
    }
    
    public static boolean testCamera(float x, float y, float z, float nearPlaneMargin, Matrix4fc model) {
        float minX = -1f * LARGE_CUBE_SCALE;
        float minY = -1f * LARGE_CUBE_SCALE;
        float minZ = -1f * LARGE_CUBE_SCALE;
        float maxX = 1f * LARGE_CUBE_SCALE;
        float maxY = 1f * LARGE_CUBE_SCALE;
        float maxZ = 1f * LARGE_CUBE_SCALE;
        
        Vector3f transformed = new Vector3f();
        
        model.transformProject(transformed.set(minX, minY, minZ));
        
        minX = transformed.x();
        minY = transformed.y();
        minZ = transformed.z();
        
        model.transformProject(transformed.set(maxX, maxY, maxZ));
        
        maxX = transformed.x();
        maxY = transformed.y();
        maxZ = transformed.z();
        
        float newMinX = Math.min(minX, maxX) - nearPlaneMargin;
        float newMinY = Math.min(minY, maxY) - nearPlaneMargin;
        float newMinZ = Math.min(minZ, maxZ) - nearPlaneMargin;
        float newMaxX = Math.max(minX, maxX) + nearPlaneMargin;
        float newMaxY = Math.max(minY, maxY) + nearPlaneMargin;
        float newMaxZ = Math.max(minZ, maxZ) + nearPlaneMargin;
        
        return testAabPoint(
                newMinX, newMinY, newMinZ,
                newMaxX, newMaxY, newMaxZ,
                x, y, z
        );
    }
    
    public static void occlusionQuery(
            Matrix4fc projection,
            Matrix4fc view,
            Matrix4fc model,
            final int queryObject
    ) {
        final Matrix4f projectionViewModel = new Matrix4f()
                .set(projection).mul(view).mul(model);
        TASKS.add(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer matrixData = stack.mallocFloat(4 * 4);
                projectionViewModel.get(matrixData);
                glUniformMatrix4fv(UNIFORM_PROJECTION_VIEW_MODEL, false, matrixData);
            }

            glBeginQuery(GL_SAMPLES_PASSED, queryObject);
            glDrawElements(GL_TRIANGLES, COUNT, GL_UNSIGNED_INT, 0);
            glEndQuery(GL_SAMPLES_PASSED);
        });
    }

    public static void executeQueries() {
        glUseProgram(SHADER_PROGRAM);

        glColorMask(false, false, false, false);
        glDepthMask(false);

        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);

        glEnableVertexAttribArray(ATTRIBUTE_POSITION);
        glVertexAttribPointer(ATTRIBUTE_POSITION, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);

        Runnable r;
        while ((r = TASKS.poll()) != null) {
            r.run();
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        glColorMask(true, true, true, true);
        glDepthMask(true);

        glUseProgram(0);
    }

    private GPUOcclusion() {

    }
}
