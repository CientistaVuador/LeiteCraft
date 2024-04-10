package cientistavuador.leitecraft;

import org.joml.Matrix4f;
import org.joml.Vector3fc;

/**
 *
 * @author Cien
 */
public class Outline {

    private final World world;
    private final Camera camera;

    private int blockX;
    private int blockY;
    private int blockZ;
    private int sideBlockX;
    private int sideBlockY;
    private int sideBlockZ;
    private boolean canPlaceBlockAtSide;
    private Block block;

    private final Matrix4f model = new Matrix4f();

    public Outline(World world, Camera camera) {
        this.world = world;
        this.camera = camera;
    }

    public World getWorld() {
        return world;
    }

    public Camera getCamera() {
        return camera;
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public int getSideBlockX() {
        return sideBlockX;
    }

    public int getSideBlockY() {
        return sideBlockY;
    }

    public int getSideBlockZ() {
        return sideBlockZ;
    }

    public boolean canPlaceBlockAtSide() {
        return canPlaceBlockAtSide;
    }
    
    public Block getBlock() {
        return block;
    }

    public void update() {
        Vector3fc position = this.camera.getPosition();
        Vector3fc direction = this.camera.getFront();

        Utils.PlayerLookingAtBlockResult result = Utils.playerLookingAtBlock(
                this.world,
                position.x(), position.y(), position.z(),
                direction.x(), direction.y(), direction.z()
        );

        int x = Integer.MAX_VALUE;
        int y = Integer.MAX_VALUE;
        int z = Integer.MAX_VALUE;
        int sideX = Integer.MAX_VALUE;
        int sideY = Integer.MAX_VALUE;
        int sideZ = Integer.MAX_VALUE;
        boolean placeSide = false;
        Block b = Blocks.AIR;

        if (result != null) {
            x = result.getBlockX();
            y = result.getBlockY();
            z = result.getBlockZ();
            sideX = result.getSideBlockX();
            sideY = result.getSideBlockY();
            sideZ = result.getSideBlockZ();
            placeSide = result.canPlaceBlockAtSide();
            b = result.getBlock();
        }

        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
        this.sideBlockX = sideX;
        this.sideBlockY = sideY;
        this.sideBlockZ = sideZ;
        this.canPlaceBlockAtSide = placeSide;
        this.block = b;
    }

    public void render() {
        if (this.block == Blocks.AIR) {
            return;
        }

        Vector3fc cam = this.camera.getPosition();

        final float scale = 1.01f;
        final float invscale = 1f / scale;

        this.model
                .identity()
                .translate(
                        (this.blockX - cam.x()) * invscale,
                        (this.blockY - cam.y()) * invscale,
                        (this.blockZ - cam.z()) * invscale
                )
                .translate(
                        0.5f * invscale,
                        0.5f * invscale,
                        0.5f * invscale
                )
                .scale(scale)
                .translate(
                        -0.5f,
                        -0.5f,
                        -0.5f
                );

        LineRenderer.lineStrips(
                this.camera.getProjection(),
                this.camera.getView(),
                this.model,
                0.15f, 0.15f, 0.15f, 0.75f,
                new float[][]{
                    {
                        0f, 0f, 0f,
                        1f, 0f, 0f,
                        1f, 0f, 1f,
                        0f, 0f, 1f,
                        0f, 0f, 0f
                    },
                    {
                        0f, 1f, 0f,
                        1f, 1f, 0f,
                        1f, 1f, 1f,
                        0f, 1f, 1f,
                        0f, 1f, 0f
                    },
                    {
                        0f, 0f, 0f,
                        0f, 1f, 0f
                    },
                    {
                        1f, 0f, 0f,
                        1f, 1f, 0f
                    },
                    {
                        1f, 0f, 1f,
                        1f, 1f, 1f
                    },
                    {
                        0f, 0f, 1f,
                        0f, 1f, 1f
                    }
                }
        );
    }

}
