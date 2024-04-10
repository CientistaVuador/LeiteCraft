package cientistavuador.leitecraft;

import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import static org.lwjgl.glfw.GLFW.*;

/**
 *
 * @author Cien
 */
public class PlayerController {

    public static final float LENGTH = 0.65f;
    public static final float HEIGHT = 1.65f;
    public static final float CROUCH_HEIGHT = 0.85f;

    public static final float PLAYER_SPEED = 4f;
    public static final float PLAYER_CROUCH_SPEED = 1f;
    
    public static final float JUMP_SPEED = 8.5f;
    public static final float CROUCH_JUMP_SPEED = 6.5f;

    public static final float EYE_HEIGHT = -0.15f;

    private final PlayerPhysics playerPhysics;

    private final Vector3d eyePosition = new Vector3d();

    public PlayerController(World world) {
        this.playerPhysics = new PlayerPhysics(world, LENGTH, HEIGHT, CROUCH_HEIGHT);
    }

    public PlayerPhysics getPlayerPhysics() {
        return playerPhysics;
    }

    public Vector3dc getEyePosition() {
        return eyePosition;
    }
    
    public void update() {
        this.playerPhysics.update();
    }

    public void updateMovement(Camera camera) {
        Vector3d pos = this.playerPhysics.getPosition();
        
        double targetY = pos.y() + this.playerPhysics.getCurrentHeight() + EYE_HEIGHT;
        if (this.playerPhysics.onGround()) {
            double currentY = this.eyePosition.y();
            double direction = targetY - currentY;
            double step = direction * Main.TPF * 10f;
            if (Math.abs(step) > Math.abs(direction)) {
                step = direction;
            }
            currentY += step;
            
            this.eyePosition.set(pos.x(), currentY, pos.z());
        } else {
            this.eyePosition.set(pos.x(), targetY, pos.z());
        }

        camera.setPosition(
                ((float) this.eyePosition.x()),
                ((float) this.eyePosition.y()),
                ((float) this.eyePosition.z())
        );

        Vector2f front = new Vector2f(camera.getFront().x(), camera.getFront().z()).normalize();
        Vector2f right = new Vector2f(camera.getRight().x(), camera.getRight().z()).normalize();

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
        
        float speed = PLAYER_SPEED;
        if (this.playerPhysics.isCrouched() && !this.playerPhysics.onAir()) {
            speed = PLAYER_CROUCH_SPEED;
        }
        
        float xa = diagonal * directionX * speed;
        float za = diagonal * directionZ * speed;
        
        this.playerPhysics.setWalkDirection(
                (front.x() * za) + (right.x() * xa),
                (front.y() * za) + (right.y() * xa)
        );

        if (glfwGetKey(Main.WINDOW_POINTER, GLFW_KEY_SPACE) == GLFW_PRESS) {
            this.playerPhysics.setJumpSpeed(JUMP_SPEED, CROUCH_JUMP_SPEED);
        } else {
            this.playerPhysics.setJumpSpeed(0f, 0f);
        }
    }

    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_LEFT_CONTROL) {
            if (action == GLFW_PRESS) {
                this.playerPhysics.setCrouched(true);
            } else if (action == GLFW_RELEASE) {
                this.playerPhysics.setCrouched(false);
            }
        }
    }

}
