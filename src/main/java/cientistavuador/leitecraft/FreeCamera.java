package cientistavuador.leitecraft;

import static org.lwjgl.glfw.GLFW.*;

/**
 *
 * @author Cien
 */
public class FreeCamera extends Camera {

    public static final float SENSITIVITY = 0.12f;
    public static final float RUN_SPEED = 13f;
    public static final float SPEED = 4.5f;

    private boolean capturingMouse = false;
    private double lastX = 0f;
    private double lastY = 0f;
    
    private boolean movementEnabled = true;

    public FreeCamera() {

    }

    public boolean isMovementEnabled() {
        return movementEnabled;
    }

    public void setMovementEnabled(boolean movementEnabled) {
        this.movementEnabled = movementEnabled;
    }
    
    public boolean isCapturingMouse() {
        return capturingMouse;
    }
    
    public void updateMovement() {
        if (!isMovementEnabled()) {
            return;
        }
        
        int directionX = 0;
        int directionZ = 0;
        
        if (glfwGetKey(Main.WINDOW_POINTER, GLFW_KEY_W) == GLFW_PRESS) {
            directionZ += 1;
        }
        if (glfwGetKey(Main.WINDOW_POINTER, GLFW_KEY_S) == GLFW_PRESS) {
            directionZ += -1;
        }
        if (glfwGetKey(Main.WINDOW_POINTER, GLFW_KEY_A) == GLFW_PRESS) {
            directionX += -1;
        }
        if (glfwGetKey(Main.WINDOW_POINTER, GLFW_KEY_D) == GLFW_PRESS) {
            directionX += 1;
        }
        
        float diagonal = (Math.abs(directionX) == 1 && Math.abs(directionZ) == 1) ? 0.707106781186f : 1f;
        float currentSpeed = glfwGetKey(Main.WINDOW_POINTER, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS ? RUN_SPEED : SPEED;
        if (glfwGetKey(Main.WINDOW_POINTER, GLFW_KEY_LEFT_ALT) == GLFW_PRESS) {
            currentSpeed /= 4f;
        }
        
        //acceleration in X and Z axis
        float xa = currentSpeed * diagonal * directionX;
        float za = currentSpeed * diagonal * directionZ;
        
        float tpf = (float) Main.TPF;
        super.setPosition(
            super.getPosition().x() + ((super.getRight().x() * xa + super.getFront().x() * za) * tpf),
            super.getPosition().y() + ((super.getRight().y() * xa + super.getFront().y() * za) * tpf),
            super.getPosition().z() + ((super.getRight().z() * xa + super.getFront().z() * za) * tpf)
        );
    }
    
    public void cursorPosCallback(long window, double posx, double posy) {
        if (this.capturingMouse) {
            double x = this.lastX - posx;
            double y = this.lastY - posy;

            super.setRotation(
                    super.getRotation().x() + (float) (y * SENSITIVITY),
                    super.getRotation().y() + (float) (x * -SENSITIVITY),
                    0
            );

            if (super.getRotation().x() >= 90) {
                super.setRotation(89.9f, super.getRotation().y(), 0);
            }
            if (super.getRotation().x() <= -90) {
                super.setRotation(-89.9f, super.getRotation().y(), 0);
            }
        }
        this.lastX = posx;
        this.lastY = posy;
    }

    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            this.capturingMouse = !this.capturingMouse;
            glfwSetInputMode(Main.WINDOW_POINTER, GLFW_CURSOR,
                    this.capturingMouse ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL
            );
        }
    }
    
}
