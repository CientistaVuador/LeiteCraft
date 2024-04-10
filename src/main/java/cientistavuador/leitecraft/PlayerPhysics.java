package cientistavuador.leitecraft;

import java.util.HashSet;
import java.util.Set;
import org.joml.Intersectiond;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 *
 * @author Cien
 */
public class PlayerPhysics {

    public static final float TICK_RATE = 1f / 300f;

    public static final float EPSILON = 0.001f;
    public static final float MOVEMENT_ROUGHNESS = 10f;

    public static final float GRAVITY = -9.8f * 3f;
    public static final float LIQUID_FRICTION = 12f;
    public static final float SWIM_FORCE = 6f;

    private final World world;

    private final float length;
    private final float height;
    private final float crouchHeight;

    private final Vector3f min = new Vector3f();
    private final Vector3f max = new Vector3f();

    private final Vector3d position = new Vector3d();
    private final Vector2f walkDirection = new Vector2f();

    private final Vector3f velocity = new Vector3f();

    private double updateCounter = 0f;

    private float jumpSpeed = 0f;
    private float crouchJumpSpeed = 0f;

    private float nextJumpImpulse = 0f;
    
    private boolean onGround = false;
    private boolean crouched = false;
    private boolean airCrouched = false;
    private boolean crouchStateChanged = false;
    private final Set<Block> inCollisionBlocks = new HashSet<>();
    private boolean onLiquid = false;
    private boolean lastOnLiquid = false;

    public PlayerPhysics(World world, float length, float height, float crouchHeight) {
        this.world = world;
        this.length = length;
        this.height = height;
        this.crouchHeight = crouchHeight;
        this.min.set(-length * 0.5f, 0.0f, -length * 0.5f);
        this.max.set(length * 0.5f, height, length * 0.5f);
    }

    public World getWorld() {
        return world;
    }

    public float getLength() {
        return length;
    }

    public float getHeight() {
        return height;
    }

    public float getCrouchHeight() {
        return crouchHeight;
    }

    public float getCurrentHeight() {
        if (isCrouched()) {
            return getCrouchHeight();
        }
        return getHeight();
    }

    public Vector3fc getMin() {
        return min;
    }

    public Vector3fc getMax() {
        return max;
    }

    public Vector3d getPosition() {
        return position;
    }

    public void setWalkDirection(float x, float z) {
        this.walkDirection.set(x, z);
    }

    public Vector2fc getWalkDirection() {
        return walkDirection;
    }

    public boolean onAir() {
        return !onGround();
    }

    public boolean onGround() {
        return this.onGround;
    }

    public void setJumpSpeed(float jumpSpeed, float crouchJumpSpeed) {
        this.jumpSpeed = jumpSpeed;
        this.crouchJumpSpeed = crouchJumpSpeed;
    }

    public float getJumpSpeed() {
        return jumpSpeed;
    }

    public float getCrouchJumpSpeed() {
        return crouchJumpSpeed;
    }

    private void jump(float speed) {
        this.nextJumpImpulse += speed;
    }

    private boolean willJump() {
        return this.nextJumpImpulse != 0f;
    }

    private void checkedJump(float speed, float crouchSpeed) {
        if ((!onGround() && !onLiquid()) || willJump() || (Math.abs(this.velocity.y()) > 0.01 && !onLiquid())) {
            return;
        }
        if (isCrouched()) {
            jump(crouchSpeed);
        } else {
            jump(speed);
        }
    }

    public boolean isCrouched() {
        return crouched;
    }

    public void setCrouched(boolean crouched) {
        this.crouchStateChanged = this.crouched != crouched;
    }

    public boolean onLiquid() {
        return onLiquid;
    }

    public void teleportToTheTop() {
        int playerBlockX = ((int) Math.floor(this.position.x()));
        int playerBlockZ = ((int) Math.floor(this.position.z()));
        int playerBlockY = Chunk.CHUNK_HEIGHT - 1;
        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
            Block blockAt = this.world.getBlock(playerBlockX, y, playerBlockZ);
            if (blockAt != Blocks.AIR && blockAt.hasCollision()) {
                playerBlockY = y;
                break;
            }
        }

        this.position.set(
                playerBlockX + 0.5,
                playerBlockY + 1.0 + EPSILON,
                playerBlockZ + 0.5
        );
    }

    private boolean checkCollision(Set<Block> collidedBlocks) {
        int heightCheck = (int) Math.ceil(this.height);
        int lengthCheck = (int) Math.ceil(this.length);

        int playerBlockX = ((int) Math.floor(this.position.x()));
        int playerBlockY = ((int) Math.floor(this.position.y()));
        int playerBlockZ = ((int) Math.floor(this.position.z()));

        double minX = this.min.x() + this.position.x();
        double minY = this.min.y() + this.position.y();
        double minZ = this.min.z() + this.position.z();
        double maxX = this.max.x() + this.position.x();
        double maxY = this.max.y() + this.position.y();
        double maxZ = this.max.z() + this.position.z();

        boolean collisionResult = false;
        for (int y = -heightCheck; y <= heightCheck; y++) {
            for (int z = -lengthCheck; z <= lengthCheck; z++) {
                for (int x = -lengthCheck; x <= lengthCheck; x++) {
                    int blockX = x + playerBlockX;
                    int blockY = y + playerBlockY;
                    int blockZ = z + playerBlockZ;

                    Block blockAt = this.world.getBlock(
                            blockX,
                            blockY,
                            blockZ
                    );

                    if (blockAt == Blocks.AIR) {
                        continue;
                    }

                    if (blockAt.hasCollision() || collidedBlocks != null) {
                        boolean collided = Intersectiond.testAabAab(
                                minX, minY, minZ, maxX, maxY, maxZ,
                                blockX, blockY, blockZ,
                                blockX + 1.0, blockY + 1.0, blockZ + 1.0
                        );
                        if (collided) {
                            if (collidedBlocks != null) {
                                if (!collidedBlocks.contains(blockAt)) {
                                    collidedBlocks.add(blockAt);
                                }
                                collisionResult = true;
                            } else {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return collisionResult;
    }

    private boolean tryMove(float x, float y, float z) {
        double lastX = this.position.x();
        double lastY = this.position.y();
        double lastZ = this.position.z();

        this.position.add(x, y, z);
        boolean success = !checkCollision(null);

        if (!success) {
            this.position.set(lastX, lastY, lastZ);
        }
        return success;
    }
    
    private void checkGround() {
        double lastX = this.position.x();
        double lastY = this.position.y();
        double lastZ = this.position.z();
        
        this.position.add(0.0, -0.04, 0.0);
        this.onGround = checkCollision(null);
        
        this.position.set(lastX, lastY, lastZ);
    }
    
    private void checkStuck() {
        boolean stuck = checkCollision(null);
        if (stuck) {
            teleportToTheTop();
        }
    }

    private void checkCrouch() {
        if (!this.crouchStateChanged) {
            return;
        }

        if (this.crouched) {
            this.max.set(this.length * 0.5f, this.height, this.length * 0.5f);

            boolean success = false;

            if (this.airCrouched) {
                this.position.sub(0f, this.height - this.crouchHeight, 0f);
                if (checkCollision(null)) {
                    this.position.add(0f, this.height - this.crouchHeight, 0f);
                    if (!checkCollision(null)) {
                        success = true;
                    }
                } else {
                    success = true;
                }
            } else {
                if (!checkCollision(null)) {
                    success = true;
                }
            }

            if (success) {
                this.airCrouched = false;
                this.crouched = false;
                this.crouchStateChanged = false;
            } else {
                this.max.set(this.length * 0.5f, this.crouchHeight, this.length * 0.5f);
            }
        } else {
            this.max.set(this.length * 0.5f, this.crouchHeight, this.length * 0.5f);
            if (onAir()) {
                this.position.add(0f, this.height - this.crouchHeight, 0f);
                this.airCrouched = true;
            } else {
                this.airCrouched = false;
            }
            this.crouched = true;
            this.crouchStateChanged = false;
        }
    }

    private void checkLiquid() {
        this.lastOnLiquid = this.onLiquid;
        this.onLiquid = false;

        this.inCollisionBlocks.clear();
        checkCollision(this.inCollisionBlocks);

        if (this.inCollisionBlocks.isEmpty()) {
            return;
        }

        for (Block b : this.inCollisionBlocks) {
            if (b.isLiquid()) {
                this.onLiquid = true;
                return;
            }
        }
    }

    private void updateMovement() {
        float movementRoughness = MOVEMENT_ROUGHNESS;

        float targetX = this.walkDirection.x();
        float targetZ = this.walkDirection.y();
        
        float dX = targetX - this.velocity.x();
        float dZ = targetZ - this.velocity.z();

        float dXStep = dX * TICK_RATE * movementRoughness;
        float dZStep = dZ * TICK_RATE * movementRoughness;

        if (Math.abs(dXStep) > Math.abs(dX) || Math.abs(dX) < EPSILON) {
            dXStep = dX;
        }
        if (Math.abs(dZStep) > Math.abs(dZ) || Math.abs(dZ) < EPSILON) {
            dZStep = dZ;
        }

        this.velocity.add(dXStep, 0f, dZStep);
    }

    private void updateGravity() {
        this.velocity.add(0f, GRAVITY * TICK_RATE, 0f);
    }

    private void updateJump() {
        float impulse = this.nextJumpImpulse;
        if (onLiquid()) {
            impulse *= TICK_RATE * SWIM_FORCE;
        }
        if (!onLiquid() && this.lastOnLiquid) {
            impulse -= this.velocity.y();
            impulse = Math.max(impulse, 0f);
        }
        this.velocity.add(0f, impulse, 0f);
        this.nextJumpImpulse = 0f;
    }

    private void applyLiquidFriction() {
        if (!this.onLiquid) {
            return;
        }

        float dX = -this.velocity.x();
        float dY = -this.velocity.y();
        float dZ = -this.velocity.z();

        float dXStep = dX * TICK_RATE * LIQUID_FRICTION;
        float dYStep = dY * TICK_RATE * LIQUID_FRICTION;
        float dZStep = dZ * TICK_RATE * LIQUID_FRICTION;

        if (Math.abs(dXStep) > Math.abs(dX) || Math.abs(dX) < EPSILON) {
            dXStep = dX;
        }
        if (Math.abs(dYStep) > Math.abs(dY) || Math.abs(dY) < EPSILON) {
            dYStep = dY;
        }
        if (Math.abs(dZStep) > Math.abs(dZ) || Math.abs(dZ) < EPSILON) {
            dZStep = dZ;
        }

        this.velocity.add(dXStep, dYStep, dZStep);
    }

    private void move() {
        float stepX = (this.velocity.x() * TICK_RATE);
        float stepY = (this.velocity.y() * TICK_RATE);
        float stepZ = (this.velocity.z() * TICK_RATE);

        if (!tryMove(stepX, stepY, stepZ)) {
            if (!tryMove(0f, stepY, 0f)) {
                this.velocity.setComponent(1, 0f);
            }
            if (!tryMove(stepX, 0f, stepZ)) {
                if (!tryMove(stepX, 0f, 0f)) {
                    this.velocity.setComponent(0, 0f);
                    if (!tryMove(0f, 0f, stepZ)) {
                        this.velocity.setComponent(2, 0f);
                    }
                }
            }
        }
    }

    public void update() {
        this.updateCounter += Main.TPF;
        while (this.updateCounter >= TICK_RATE) {
            this.updateCounter -= TICK_RATE;
            
            checkGround();
            
            checkedJump(getJumpSpeed(), getCrouchJumpSpeed());

            checkStuck();
            checkCrouch();
            checkLiquid();

            updateMovement();
            updateGravity();
            updateJump();

            applyLiquidFriction();

            move();
        }
    }

    public void debugUpdate(Camera camera) {
        Vector3fc cameraPosition = camera.getPosition();

        Matrix4fc projection = camera.getProjection();
        Matrix4fc view = camera.getView();
        Matrix4fc model = new Matrix4f()
                .translate(
                        (float) (this.position.x() - cameraPosition.x()),
                        (float) (this.position.y() - cameraPosition.y()),
                        (float) (this.position.z() - cameraPosition.z())
                );

        float r = 1f;
        float g = 0f;
        float b = 0f;
        float a = 0.75f;

        float minX = this.min.x();
        float minY = this.min.y();
        float minZ = this.min.z();
        float maxX = this.max.x();
        float maxY = this.max.y();
        float maxZ = this.max.z();

        LineRenderer.lineStrips(
                projection, view, model,
                r, g, b, a,
                new float[][]{
                    {
                        minX, minY, minZ,
                        minX, minY, maxZ,
                        maxX, minY, maxZ,
                        maxX, minY, minZ,
                        minX, minY, minZ,},
                    {
                        minX, maxY, minZ,
                        minX, maxY, maxZ,
                        maxX, maxY, maxZ,
                        maxX, maxY, minZ,
                        minX, maxY, minZ,},
                    {
                        minX, minY, minZ,
                        minX, maxY, minZ,},
                    {
                        minX, minY, maxZ,
                        minX, maxY, maxZ,},
                    {
                        maxX, minY, maxZ,
                        maxX, maxY, maxZ,},
                    {
                        maxX, minY, minZ,
                        maxX, maxY, minZ,}
                }
        );
    }

}
