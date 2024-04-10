package cientistavuador.leitecraft;

import static org.lwjgl.opengl.GL33C.*;

/**
 *
 * @author Cien
 */
public class SmokePass {

    private static final int VAO;
    
    static {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, new float[]{
            -1f, -1f,
            1f, -1f,
            1f, 1f,
            -1f, 1f
        }, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);

        glBindVertexArray(0);

        VAO = vao;
    }

    private static final int SHADER_PROGRAM = ProgramCompiler.compile(
            """
            #version 330
            
            layout (location = 0) in vec2 vertexPosition;
            
            void main() {
                gl_Position = vec4(vertexPosition, 0.0, 1.0);
            }
            """,
            """
            #version 330
            
            const vec2 sampleOffsets[] = vec2[](
                vec2(0.0, 0.0),
                
                vec2(1.5, -1.5),
                vec2(1.5, 1.5),
                vec2(-1.5, -1.5),
                vec2(-1.5, 1.5),
                
                vec2(3.0, 0.0),
                vec2(-3.0, 0.0),
                vec2(0.0, 3.0),
                vec2(0.0, -3.0),
                
                vec2(4.5, -4.5),
                vec2(4.5, 4.5),
                vec2(-4.5, -4.5),
                vec2(-4.5, 4.5)
            );
            
            uniform sampler2D colorBuffer;
            uniform sampler2D depthBuffer;
            uniform vec2 viewport;
            
            layout (location = 0) out vec4 fragColor;
            
            void main() {
                vec2 uv = gl_FragCoord.xy / viewport;
                vec2 invSize = vec2(1.0) / vec2(textureSize(colorBuffer, 0));
                
                vec4 finalColor = vec4(0.0);
                for (int i = 0; i < sampleOffsets.length(); i++) {
                    vec2 offset = sampleOffsets[i] * invSize;
                    
                    vec4 texColor = texture(colorBuffer, uv + offset);
                    finalColor += texColor;
                }
                
                finalColor /= float(sampleOffsets.length());
                fragColor = finalColor;
                
                gl_FragDepth = texelFetch(depthBuffer, ivec2(floor(uv / invSize)), 0).r;
            }
            """
    );
    
    private static final int UNIFORM_COLOR_BUFFER = glGetUniformLocation(SHADER_PROGRAM, "colorBuffer");
    private static final int UNIFORM_DEPTH_BUFFER = glGetUniformLocation(SHADER_PROGRAM, "depthBuffer");
    private static final int UNIFORM_VIEWPORT = glGetUniformLocation(SHADER_PROGRAM, "viewport");

    public static void init() {

    }

    private static final int AMOUNT_OF_RESOLUTIONS = 4;

    private static final int[] width = new int[AMOUNT_OF_RESOLUTIONS];
    private static final int[] height = new int[AMOUNT_OF_RESOLUTIONS];

    private static final int[] fbos = new int[AMOUNT_OF_RESOLUTIONS];
    private static final int[] colorBuffers = new int[AMOUNT_OF_RESOLUTIONS];
    private static final int[] depthBuffers = new int[AMOUNT_OF_RESOLUTIONS];

    private static void updateResolutions() {
        for (int i = 0; i < AMOUNT_OF_RESOLUTIONS; i++) {
            int w;
            int h;
            if (i != 0) {
                w = (int) Math.ceil(width[i - 1] / 2f);
                h = (int) Math.ceil(height[i - 1] / 2f);
            } else {
                w = Main.WIDTH;
                h = Main.HEIGHT;
            }
            width[i] = w;
            height[i] = h;
        }

        glActiveTexture(GL_TEXTURE0);

        for (int i = 0; i < AMOUNT_OF_RESOLUTIONS; i++) {
            glDeleteFramebuffers(fbos[i]);
            glDeleteTextures(colorBuffers[i]);
            glDeleteTextures(depthBuffers[i]);
        }

        for (int i = 0; i < AMOUNT_OF_RESOLUTIONS; i++) {
            if (i == 0) {
                continue;
            }

            int w = width[i];
            int h = height[i];

            int fbo = glGenFramebuffers();
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);

            int colorBuffer = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, colorBuffer);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            int depthBuffer = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, depthBuffer);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, w, h, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            glFramebufferTexture(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, colorBuffer, 0);
            glFramebufferTexture(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depthBuffer, 0);

            glDrawBuffer(GL_COLOR_ATTACHMENT0);

            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

            fbos[i] = fbo;
            colorBuffers[i] = colorBuffer;
            depthBuffers[i] = depthBuffer;
        }
    }

    public static void render() {
        if (Main.WIDTH == 0 || Main.HEIGHT == 0) {
            return;
        }
        
        if (SmokeRenderer.queuedForRendering() <= 0) {
            return;
        }

        int currentWidth = width[0];
        int currentHeight = height[0];

        if (currentWidth != Main.WIDTH || currentHeight != Main.HEIGHT) {
            updateResolutions();
        }

        int lastBufferIndex = AMOUNT_OF_RESOLUTIONS - 1;

        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbos[lastBufferIndex]);

        glViewport(0, 0, width[lastBufferIndex], height[lastBufferIndex]);
        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

        SmokeRenderer.render();

        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

        glUseProgram(SHADER_PROGRAM);
        glBindVertexArray(VAO);

        for (int i = lastBufferIndex - 1; i >= 0; i--) {
            int colorBuffer = colorBuffers[i + 1];
            int depthBuffer = depthBuffers[i + 1];

            int fbo = fbos[i];
            int w = width[i];
            int h = height[i];

            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);

            glViewport(0, 0, w, h);
            
            if (i != 0) {
                glClearColor(0f, 0f, 0f, 0f);
                glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
            }
            
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, colorBuffer);
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, depthBuffer);

            glUniform1i(UNIFORM_COLOR_BUFFER, 0);
            glUniform1i(UNIFORM_DEPTH_BUFFER, 1);
            glUniform2f(UNIFORM_VIEWPORT, w, h);

            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        }

        glBindVertexArray(0);
        glUseProgram(0);
    }

    private SmokePass() {

    }

}
