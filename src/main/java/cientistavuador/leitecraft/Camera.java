package cientistavuador.leitecraft;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 *
 * @author Cien
 */
public class Camera {

    public static final Vector3fc WORLD_UP = new Vector3f(0f, 1f, 0f);

    public static final float FOV = 90f;
    public static final float PITCH = 0f;
    public static final float YAW = -90f;

    public static final float NEAR_PLANE = 0.01f;
    public static final float FAR_PLANE = 1000f;

    private final Vector3f position = new Vector3f(0f, 0f, 0f);
    private final Vector3f rotation = new Vector3f(PITCH, YAW, 0f);

    private final Vector3f front = new Vector3f();
    private final Vector3f right = new Vector3f();
    private final Vector3f up = new Vector3f();

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    
    private float aspectRatio = 800f / 600f;

    public Camera() {
        recalculateCamera();
    }

    public Vector3fc getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        recalculateCamera();
    }

    public void setPosition(Vector3fc position) {
        setPosition(position.x(), position.y(), position.z());
    }

    public Vector3fc getRotation() {
        return rotation;
    }

    public void setRotation(float pitch, float yaw, float roll) {
        this.rotation.set(pitch, yaw, roll);
        recalculateCamera();
    }

    public void setRotation(Vector3fc rotation) {
        setRotation(rotation.x(), rotation.y(), rotation.z());
    }

    public Vector3fc getFront() {
        return front;
    }

    public Vector3fc getRight() {
        return right;
    }

    public Vector3fc getUp() {
        return up;
    }

    public Matrix4fc getProjection() {
        return projection;
    }

    public Matrix4fc getView() {
        return view;
    }

    private void calculateVectors() {
        float pitchRadians = (float) Math.toRadians(this.rotation.x());
        float yawRadians = (float) Math.toRadians(this.rotation.y());

        this.front.set(
                Math.cos(pitchRadians) * Math.cos(yawRadians),
                Math.sin(pitchRadians),
                Math.cos(pitchRadians) * Math.sin(yawRadians)
        ).normalize();

        this.right.set(this.front).cross(WORLD_UP).normalize();
        this.up.set(this.right).cross(this.front).normalize();
    }

    private void calculateProjection() {
        this.projection.setPerspective(
                (float) Math.toRadians(FOV),
                this.aspectRatio,
                NEAR_PLANE,
                FAR_PLANE
        );
    }

    private void calculateView() {
        this.view.setLookAt(
                0f, 0f, 0f,
                0f + this.front.x(),
                0f + this.front.y(),
                0f + this.front.z(),
                this.up.x(), this.up.y(), this.up.z()
        );
    }

    private void recalculateCamera() {
        calculateVectors();
        calculateProjection();
        calculateView();
    }

    public void updateAspectRatio(int width, int height) {
        this.aspectRatio = width / ((float) height);
        recalculateCamera();
    }

}
