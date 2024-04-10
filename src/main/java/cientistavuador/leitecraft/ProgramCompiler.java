package cientistavuador.leitecraft;

import static org.lwjgl.opengl.GL20.*;

/**
 *
 * @author Cien
 */
public class ProgramCompiler {
    
    public static int compile(String vertexShaderSource, String fragmentShaderSource) {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) != GL_TRUE) {
            String error = glGetShaderInfoLog(vertexShader);
            throw new RuntimeException("Vertex Shader Failed to Compile: "+error);
        }
        
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);
        
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) != GL_TRUE) {
            String error = glGetShaderInfoLog(fragmentShader);
            throw new RuntimeException("Fragment Shader Failed to Compile: "+error);
        }
        
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        
        glLinkProgram(program);
        
        if (glGetProgrami(program, GL_LINK_STATUS) != GL_TRUE) {
            String error = glGetProgramInfoLog(program);
            throw new RuntimeException("Shader Program Failed to Link: "+error);
        }
        
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        
        return program;
    }
    
    private ProgramCompiler() {
        
    }
    
}
