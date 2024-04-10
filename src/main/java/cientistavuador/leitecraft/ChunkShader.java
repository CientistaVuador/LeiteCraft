package cientistavuador.leitecraft;

import java.nio.FloatBuffer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import static org.lwjgl.opengl.GL20.*;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author Cien
 */
public class ChunkShader {

    public static void init() {

    }

    public static final int SHADER_PROGRAM = ProgramCompiler.compile(
            """
            #version 110
            
            uniform mat4 projection;
            uniform mat4 view;
            uniform mat4 model;
            uniform mat3 normalModel;
            
            uniform int currentFrame;
            
            attribute vec3 vertexPosition;
            attribute vec3 vertexNormal;
            attribute vec2 vertexUV;
            attribute float blendMode;
            attribute float animatedOffset;
            attribute float animatedFrames;
            attribute float vertexAO;
            attribute float vertexShadow;
            
            varying vec3 outPosition;
            varying vec3 outNormal;
            varying vec2 outUV;
            varying float outBlendMode;
            varying float outAO;
            varying float outShadow;
            
            void main() {
                vec4 pos = model * vec4(vertexPosition, 1.0);
                
                outPosition = pos.xyz;
                outNormal = vertexNormal * normalModel;
                outUV = vertexUV + vec2(animatedOffset * mod(float(currentFrame), animatedFrames), 0.0);
                outBlendMode = blendMode;
                outAO = vertexAO;
                outShadow = vertexShadow;
                
                gl_Position = projection * view * pos;
            }
            """,
            """
            #version 110
            
            #define OPAQUE 0
            #define TESTED 1
            #define BLENDED 2
            
            uniform vec3 sunDiffuse;
            uniform vec3 sunAmbient;
            uniform vec3 sunDirection;
            
            uniform sampler2D atlasTexture;
            
            varying vec3 outPosition;
            varying vec3 outNormal;
            varying vec2 outUV;
            varying float outBlendMode;
            varying float outAO;
            varying float outShadow;
            
            const float gamma = 2.2;
            
            void main() {
                vec4 texColor = texture2D(atlasTexture, outUV);
                
                texColor.rgb = pow(texColor.rgb, vec3(gamma));
                
                int blendMode = int(outBlendMode + 0.001);
                if (blendMode == OPAQUE) {
                    texColor.a = 1.0;
                } else if (blendMode == TESTED) {
                    if (texColor.a < 0.5) {
                        discard;
                    }
                    texColor.a = 1.0;
                }
                
                vec3 ambient = texColor.rgb * sunAmbient * outAO * outShadow;
                vec3 diffuse = texColor.rgb * sunDiffuse * max(dot(normalize(outNormal), normalize(-sunDirection)), 0.0) * outAO * outShadow;
                
                gl_FragColor = vec4(pow(ambient + diffuse, vec3(1.0/gamma)), texColor.a);
            }
            """
    );

    public static final int UNIFORM_PROJECTION = glGetUniformLocation(SHADER_PROGRAM, "projection");
    public static final int UNIFORM_VIEW = glGetUniformLocation(SHADER_PROGRAM, "view");
    public static final int UNIFORM_MODEL = glGetUniformLocation(SHADER_PROGRAM, "model");
    public static final int UNIFORM_NORMAL_MODEL = glGetUniformLocation(SHADER_PROGRAM, "normalModel");
    public static final int UNIFORM_CURRENT_FRAME = glGetUniformLocation(SHADER_PROGRAM, "currentFrame");
    public static final int UNIFORM_SUN_DIFFUSE = glGetUniformLocation(SHADER_PROGRAM, "sunDiffuse");
    public static final int UNIFORM_SUN_AMBIENT = glGetUniformLocation(SHADER_PROGRAM, "sunAmbient");
    public static final int UNIFORM_SUN_DIRECTION = glGetUniformLocation(SHADER_PROGRAM, "sunDirection");
    public static final int UNIFORM_ATLAS_TEXTURE = glGetUniformLocation(SHADER_PROGRAM, "atlasTexture");

    public static final int ATTRIBUTE_POSITION = glGetAttribLocation(SHADER_PROGRAM, "vertexPosition");
    public static final int ATTRIBUTE_NORMAL = glGetAttribLocation(SHADER_PROGRAM, "vertexNormal");
    public static final int ATTRIBUTE_UV = glGetAttribLocation(SHADER_PROGRAM, "vertexUV");
    public static final int ATTRIBUTE_BLEND_MODE = glGetAttribLocation(SHADER_PROGRAM, "blendMode");
    public static final int ATTRIBUTE_ANIMATED_OFFSET = glGetAttribLocation(SHADER_PROGRAM, "animatedOffset");
    public static final int ATTRIBUTE_ANIMATED_FRAMES = glGetAttribLocation(SHADER_PROGRAM, "animatedFrames");
    public static final int ATTRIBUTE_AO = glGetAttribLocation(SHADER_PROGRAM, "vertexAO");
    public static final int ATTRIBUTE_SHADOW = glGetAttribLocation(SHADER_PROGRAM, "vertexShadow");

    public static void use() {
        glUseProgram(SHADER_PROGRAM);
    }

    public static void attribute(int attribute, int size, int stride, int offset, boolean normalized) {
        glEnableVertexAttribArray(attribute);
        glVertexAttribPointer(attribute, size, GL_FLOAT, normalized, stride * Float.BYTES, offset * Float.BYTES);
    }

    public static void uniformCameraMatrices(Matrix4fc projection, Matrix4fc view) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer projectionBuffer = stack.mallocFloat(4 * 4);
            FloatBuffer viewBuffer = stack.mallocFloat(4 * 4);

            projection.get(projectionBuffer);
            view.get(viewBuffer);

            glUniformMatrix4fv(UNIFORM_PROJECTION, false, projectionBuffer);
            glUniformMatrix4fv(UNIFORM_VIEW, false, viewBuffer);
        }
    }

    public static void uniformModel(Matrix4fc model) {
        Matrix3f normalModel = model
                .invert(new Matrix4f())
                .transpose3x3(new Matrix3f());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer modelBuffer = stack.mallocFloat(4 * 4);
            FloatBuffer normalModelBuffer = stack.mallocFloat(3 * 3);

            model.get(modelBuffer);
            normalModel.get(normalModelBuffer);

            glUniformMatrix4fv(UNIFORM_MODEL, false, modelBuffer);
            glUniformMatrix3fv(UNIFORM_NORMAL_MODEL, false, normalModelBuffer);
        }
    }
    
    public static void uniformCurrentFrame(int frame) {
        glUniform1i(UNIFORM_CURRENT_FRAME, frame);
    }

    public static void uniformSunDiffuse(Vector3fc sunDiffuse) {
        glUniform3f(UNIFORM_SUN_DIFFUSE, sunDiffuse.x(), sunDiffuse.y(), sunDiffuse.z());
    }

    public static void uniformSunAmbient(Vector3fc sunAmbient) {
        glUniform3f(UNIFORM_SUN_AMBIENT, sunAmbient.x(), sunAmbient.y(), sunAmbient.z());
    }

    public static void uniformSunDirection(Vector3fc sunDirection) {
        glUniform3f(UNIFORM_SUN_DIRECTION, sunDirection.x(), sunDirection.y(), sunDirection.z());
    }

    public static void uniformAtlasTexture(int unit) {
        glUniform1i(UNIFORM_ATLAS_TEXTURE, unit);
    }

    public static void unuse() {
        glUseProgram(0);
    }

    private ChunkShader() {

    }

}
