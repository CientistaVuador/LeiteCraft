package cientistavuador.leitecraft;

import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import static org.lwjgl.opengl.GL20.*;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author Cien
 */
public class UIRenderer {
    
    private static final int QUAD_VBO;
    private static final int QUAD_EBO;
    private static final int QUAD_INDICES;
    
    static {
        float[] vertices = {
            0.0f, 0.0f, 0f,
            1.0f, 0.0f, 1f,
            0.0f, 1.0f, 2f,
            1.0f, 1.0f, 3f
        };
        
        int[] indices = {
            0, 1, 2,
            1, 3, 2
        };
        
        QUAD_INDICES = indices.length;
        
        QUAD_VBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, QUAD_VBO);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        QUAD_EBO = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, QUAD_EBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    private static final int SHADER_PROGRAM = ProgramCompiler.compile(
            """
            #version 110
            
            uniform vec2 uvs[4];
            uniform mat4 model;
            
            attribute vec2 vertexPosition;
            attribute float vertexId;
            
            varying vec2 uv;
            
            void main() {
                uv = uvs[int(vertexId)];
                gl_Position = model * vec4(vertexPosition, 0.0, 1.0);
            }
            """
            ,
            """
            #version 110
            
            uniform sampler2D atlas;
            
            varying vec2 uv;
            
            void main() {
                gl_FragColor = texture2D(atlas, uv);
            }
            """
    );
    
    private static final int UVS_0 = glGetUniformLocation(SHADER_PROGRAM, "uvs[0]");
    private static final int UVS_1 = glGetUniformLocation(SHADER_PROGRAM, "uvs[1]");
    private static final int UVS_2 = glGetUniformLocation(SHADER_PROGRAM, "uvs[2]");
    private static final int UVS_3 = glGetUniformLocation(SHADER_PROGRAM, "uvs[3]");
    private static final int MODEL = glGetUniformLocation(SHADER_PROGRAM, "model");
    private static final int ATLAS = glGetUniformLocation(SHADER_PROGRAM, "atlas");
    
    private static final int VERTEX_ATTRIBUTE = glGetAttribLocation(SHADER_PROGRAM, "vertexPosition");
    private static final int VERTEX_ID_ATTRIBUTE = glGetAttribLocation(SHADER_PROGRAM, "vertexId");
    
    private static final Matrix4f globalModel = new Matrix4f();
    private static final ConcurrentLinkedQueue<Runnable> renderTasks = new ConcurrentLinkedQueue<>();
    
    public static void init() {
        
    }
    
    public static void render() {
        glDisable(GL_DEPTH_TEST);
        
        glUseProgram(SHADER_PROGRAM);
        
        glBindBuffer(GL_ARRAY_BUFFER, QUAD_VBO);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, QUAD_EBO);
        
        glEnableVertexAttribArray(VERTEX_ATTRIBUTE);
        glVertexAttribPointer(VERTEX_ATTRIBUTE, 2, GL_FLOAT, false, 3 * Float.BYTES, 0);
        
        glEnableVertexAttribArray(VERTEX_ID_ATTRIBUTE);
        glVertexAttribPointer(VERTEX_ID_ATTRIBUTE, 1, GL_FLOAT, false, 3 * Float.BYTES, 2 * Float.BYTES);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Atlas.getInstance().getTexture());
        glUniform1i(ATLAS, 0);
        
        Runnable r;
        while ((r = renderTasks.poll()) != null) {
            r.run();
        }
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        
        glUseProgram(0);
        
        glEnable(GL_DEPTH_TEST);
    }
    
    private static void writeUvs(float lowerX, float lowerY, float higherX, float higherY) {
        glUniform2f(UVS_0, lowerX, lowerY);
        glUniform2f(UVS_1, higherX, lowerY);
        glUniform2f(UVS_2, lowerX, higherY);
        glUniform2f(UVS_3, higherX, higherY);
    }
    
    private static void writeModel(Matrix4fc model) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matrixData = stack.mallocFloat(4*4);
            model.get(matrixData);
            glUniformMatrix4fv(MODEL, false, matrixData);
        }
    }
    
    private static void writeTranslateAndScaleModel(float x, float y, float scaleX, float scaleY) {
        Matrix4f matrix = globalModel.identity();
        
        matrix
                .translate((x * 2f) - 1f, (y * 2f) - 1f, 0f)
                .scale(scaleX * 2f, scaleY * 2f, 0f)
                ;
        
        writeModel(matrix);
    }
    
    public static float ratioX() {
        if (Main.WIDTH > Main.HEIGHT) {
            return ((float)Main.HEIGHT) / Main.WIDTH;
        }
        return 1f;
    }
    
    public static float ratioY() {
        if (Main.HEIGHT > Main.WIDTH) {
            return ((float)Main.WIDTH) / Main.HEIGHT;
        }
        return 1f;
    }
    
    public static void drawImage(AtlasTexture texture, float x, float y, float scaleX, float scaleY) {
        float lowerX = texture.getLowerPosition().x();
        float lowerY = texture.getLowerPosition().y();
        float higherX = texture.getHigherPosition().x();
        float higherY = texture.getHigherPosition().y();
        
        renderTasks.add(() -> {
            writeUvs(lowerX, lowerY, higherX, higherY);
            writeTranslateAndScaleModel(x, y, scaleX, scaleY);
            
            glDrawElements(GL_TRIANGLES, QUAD_INDICES, GL_UNSIGNED_INT, 0);
        });
    }
    
    public static void drawText(String text, float x, float y, float size) {
        AtlasTexture fallback = Atlas.getInstance().getTexture("char_"+((int)'?'));
        
        float textWidth = size * ratioX();
        float textHeight = size * ratioY();
        
        float returnX = x;
        float textX = x;
        float textY = y;
        
        String textToRender = text.toUpperCase();
        
        int unicodePoints = textToRender.length();
        for (int i = 0; i < unicodePoints; i++) {
            int unicode = textToRender.codePointAt(i);
            
            AtlasTexture toRender = null;
            
            switch (unicode) {
                case ' ' -> {
                    textX += textWidth;
                }
                case '\t' -> {
                    textX += (textWidth * 4f);
                }
                case '\n' -> {
                    textX = returnX;
                    textY -= textHeight;
                }
                case '\r' -> {
                    
                }
                default -> {
                    toRender = Atlas.getInstance().getTexture("char_"+unicode);
                    if (toRender == null) {
                        toRender = fallback;
                    }
                }
            }
            
            if (toRender == null) {
                continue;
            }
            
            drawImage(toRender, textX, textY, textWidth, textHeight);
            textX += textWidth;
        }
    }
    
    private UIRenderer() {
        
    }
    
}
