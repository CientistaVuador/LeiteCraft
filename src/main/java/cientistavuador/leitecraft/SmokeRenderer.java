package cientistavuador.leitecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author Cien
 */
public class SmokeRenderer {

    private static final int MASK_TEXTURE;

    static {
        byte[] data;
        try (InputStream stream = SmokeRenderer.class.getResourceAsStream("smoke_mask.png")) {
            data = stream.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        
        stbi_set_flip_vertically_on_load_thread(1);
        
        ByteBuffer imageData = null;
        int width = 0;
        int height = 0;

        ByteBuffer memory = memAlloc(data.length).put(data).flip();
        try {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer xBuffer = stack.mallocInt(1);
                IntBuffer yBuffer = stack.mallocInt(1);
                IntBuffer channelBuffer = stack.mallocInt(1);
                
                imageData = stbi_load_from_memory(memory, xBuffer, yBuffer, channelBuffer, 1);
                
                if (imageData == null) {
                    throw new RuntimeException("Failed to load mask: "+stbi_failure_reason());
                }
                
                width = xBuffer.get();
                height = yBuffer.get();
            }
        } finally {
            memFree(memory);
        }

        glActiveTexture(GL_TEXTURE0);

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, imageData);
        glGenerateMipmap(GL_TEXTURE_2D);
        
        stbi_image_free(imageData);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        
        glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, new int[] {
            GL_ONE, GL_ONE, GL_ONE, GL_RED
        });
        
        glBindTexture(GL_TEXTURE_2D, 0);

        MASK_TEXTURE = texture;
    }

    private static final int VAO;

    static {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, new float[]{
            -1f, -1f, 0f, 0f, 0f,
            1f, -1f, 0f, 1f, 0f,
            1f, 1f, 0f, 1f, 1f,
            -1f, 1f, 0f, 0f, 1f
        }, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);

        glBindVertexArray(0);

        VAO = vao;
    }

    private static final int SHADER_PROGRAM = ProgramCompiler.compile(
            """
            #version 330
            
            uniform mat4 projectionViewModel;
            
            layout (location = 0) in vec3 vertexPosition;
            layout (location = 1) in vec2 vertexUv;
            
            out vec2 fragUv;
            
            void main() {
                fragUv = vertexUv;
                gl_Position = projectionViewModel * vec4(vertexPosition, 1.0);
            }
            """,
            """
            #version 330
            
            uniform vec4 color;
            uniform sampler2D mask;
            
            in vec2 fragUv;
            
            layout (location = 0) out vec4 fragColor;
            
            void main() {
                vec4 finalColor = texture(mask, fragUv) * color;
                finalColor.rgb *= finalColor.a;
                fragColor = finalColor;
            }
            """
    );
    
    private static final int UNIFORM_PROJECTION_VIEW_MODEL = glGetUniformLocation(SHADER_PROGRAM, "projectionViewModel");
    private static final int UNIFORM_COLOR = glGetUniformLocation(SHADER_PROGRAM, "color");
    private static final int UNIFORM_MASK = glGetUniformLocation(SHADER_PROGRAM, "mask");
    
    public static void init() {

    }

    private static class Smoke {

        public final float distanceSquared;
        public final Matrix4fc projectionViewModel;
        public final float r;
        public final float g;
        public final float b;
        public final float a;

        public Smoke(float distance, Matrix4fc model, float r, float g, float b, float a) {
            this.distanceSquared = distance;
            this.projectionViewModel = model;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }

    private static final Queue<Smoke> TO_RENDER = new ConcurrentLinkedQueue<>();

    public static void smoke(Camera camera, double x, double y, double z, float r, float g, float b, float a) {
        Vector3fc camPos = camera.getPosition();

        float relativeX = (float) (x - camPos.x());
        float relativeY = (float) (y - camPos.y());
        float relativeZ = (float) (z - camPos.z());

        float distanceSquared = (relativeX * relativeX) + (relativeY * relativeY) + (relativeZ * relativeZ);

        Matrix4f projectionViewModel
                = new Matrix4f(camera.getProjection())
                        .mul(camera.getView())
                        .translate(relativeX, relativeY, relativeZ)
                        .mul(camera.getView().invert(new Matrix4f()));

        TO_RENDER.add(new Smoke(distanceSquared, projectionViewModel, r, g, b, a));
    }

    public static void render() {
        List<Smoke> smokes = new ArrayList<>();

        {
            Smoke s;
            while ((s = TO_RENDER.poll()) != null) {
                smokes.add(s);
            }
        }

        smokes.sort((o1, o2) -> Float.compare(o1.distanceSquared, o2.distanceSquared));

        glUseProgram(SHADER_PROGRAM);
        glBindVertexArray(VAO);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, MASK_TEXTURE);
        
        for (int i = (smokes.size() - 1); i >= 0; i--) {
            Smoke s = smokes.get(i);
            
            glUniform4f(UNIFORM_COLOR, s.r, s.g, s.b, s.a);
            glUniform1i(UNIFORM_MASK, 0);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(4 * 4);
                s.projectionViewModel.get(buffer);
                
                glUniformMatrix4fv(UNIFORM_PROJECTION_VIEW_MODEL, false, buffer);
            }

            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        }

        glBindVertexArray(0);
        glUseProgram(0);
    }
    
    public static int queuedForRendering() {
        return TO_RENDER.size();
    }

}
