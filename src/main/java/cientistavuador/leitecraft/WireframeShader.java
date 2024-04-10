package cientistavuador.leitecraft;

import static org.lwjgl.opengl.GL20.*;

/**
 *
 * @author Cien
 */
public class WireframeShader {
    
    public static final int SHADER_PROGRAM = ProgramCompiler.compile(
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
            
            uniform vec4 color;
            
            void main() {
                gl_FragColor = color;
            }
            """
    );
    
    public static final int UNIFORM_PROJECTION_VIEW_MODEL = glGetUniformLocation(SHADER_PROGRAM, "projectionViewModel");
    public static final int UNIFORM_COLOR = glGetUniformLocation(SHADER_PROGRAM, "color");
    
    public static final int ATTRIBUTE_VERTEX_POSITION = glGetAttribLocation(SHADER_PROGRAM, "vertexPosition");
    
    private WireframeShader() {
        
    }
    
}
