package cientistavuador.leitecraft;

import org.joml.Vector3f;

/**
 *
 * @author Cien
 */
public class Utils {

    public static class PlayerLookingAtBlockResult {

        private final int blockX;
        private final int blockY;
        private final int blockZ;
        private final int sideBlockX;
        private final int sideBlockY;
        private final int sideBlockZ;
        private final boolean canPlaceBlockAtSide;
        private final Block block;

        public PlayerLookingAtBlockResult(int blockX, int blockY, int blockZ, int sideBlockX, int sideBlockY, int sideBlockZ, boolean canPlaceBlockAtSide, Block block) {
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            this.sideBlockX = sideBlockX;
            this.sideBlockY = sideBlockY;
            this.sideBlockZ = sideBlockZ;
            this.canPlaceBlockAtSide = canPlaceBlockAtSide;
            this.block = block;
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

    }

    private static int signum(int i) {
        if (i < 0) {
            return -1;
        }
        if (i > 0) {
            return 1;
        }
        return 0;
    }

    public static PlayerLookingAtBlockResult playerLookingAtBlock(World world, double px, double py, double pz, float dx, float dy, float dz) {
        final float step = 0.05f;
        final float maxLength = 4f;

        boolean lastBlockSet = false;
        int lastBlockX = 0;
        int lastBlockY = 0;
        int lastBlockZ = 0;
        
        float currentLength = 0f;
        while (currentLength <= maxLength) {
            double x = px + (dx * currentLength);
            double y = py + (dy * currentLength);
            double z = pz + (dz * currentLength);

            int blockX = (int) Math.floor(x);
            int blockY = (int) Math.floor(y);
            int blockZ = (int) Math.floor(z);

            if (lastBlockSet && (blockX == lastBlockX && blockY == lastBlockY && blockZ == lastBlockZ)) {
                currentLength += step;
                continue;
            }

            Block blockAt = world.getBlockOrNull(blockX, blockY, blockZ);
            if (blockAt != Blocks.AIR && !blockAt.isLiquid()) {
                int offsetX = 0;
                int offsetY = 0;
                int offsetZ = 0;
                boolean canPlaceBlockAtSide = false;
                if (lastBlockSet) {
                    offsetX = signum(lastBlockX - blockX);
                    offsetY = signum(lastBlockY - blockY);
                    offsetZ = signum(lastBlockZ - blockZ);
                    
                    if ((Math.abs(offsetX) + Math.abs(offsetY) + Math.abs(offsetZ)) != 1) {
                        findBestAxis: {
                            if (Math.abs(offsetX) != 0) {
                                Block b = world.getBlockOrNull(blockX + offsetX, blockY, blockZ);
                                if (b == Blocks.AIR || b.isLiquid()) {
                                    offsetY = 0;
                                    offsetZ = 0;
                                    canPlaceBlockAtSide = true;
                                    break findBestAxis;
                                }
                            }
                            if (Math.abs(offsetY) != 0) {
                                Block b = world.getBlockOrNull(blockX, blockY + offsetY, blockZ);
                                if (b == Blocks.AIR || b.isLiquid()) {
                                    offsetX = 0;
                                    offsetZ = 0;
                                    canPlaceBlockAtSide = true;
                                    break findBestAxis;
                                }
                            }
                            if (Math.abs(offsetZ) != 0) {
                                Block b = world.getBlockOrNull(blockX, blockY, blockZ + offsetZ);
                                if (b == Blocks.AIR || b.isLiquid()) {
                                    offsetX = 0;
                                    offsetY = 0;
                                    canPlaceBlockAtSide = true;
                                    break findBestAxis;
                                }
                            }
                            offsetX = 0;
                            offsetY = 0;
                            offsetZ = 0;
                        }
                    }
                }
                
                if (!canPlaceBlockAtSide) {
                    Block b = world.getBlock(
                            blockX + offsetX,
                            blockY + offsetY,
                            blockZ + offsetZ
                    );
                    if (b == Blocks.AIR || b.isLiquid()) {
                        canPlaceBlockAtSide = true;
                    }
                }

                return new PlayerLookingAtBlockResult(
                        blockX,
                        blockY,
                        blockZ,
                        blockX + offsetX,
                        blockY + offsetY,
                        blockZ + offsetZ,
                        canPlaceBlockAtSide,
                        blockAt
                );
            }

            lastBlockX = blockX;
            lastBlockY = blockY;
            lastBlockZ = blockZ;
            lastBlockSet = true;

            currentLength += step;
        }

        return null;
    }

    public static boolean testAabPoint(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float px, float py, float pz) {
        return !(px < minX || px > maxX || py < minY || py > maxY || pz < minZ || pz > maxZ);
    }

    public static void aabb(float[] vertices, int offset, int components, int vertexSize, Vector3f min, Vector3f max) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;

        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < vertices.length; i += vertexSize) {
            float x = 0f;
            float y = 0f;
            float z = 0f;

            if (components >= 1) {
                x = vertices[i + offset + 0];
            }
            if (components >= 2) {
                y = vertices[i + offset + 1];
            }
            if (components >= 3) {
                z = vertices[i + offset + 2];
            }

            minX = Math.min(x, minX);
            minY = Math.min(y, minY);
            minZ = Math.min(z, minZ);

            maxX = Math.max(x, maxX);
            maxY = Math.max(y, maxY);
            maxZ = Math.max(z, maxZ);
        }

        min.set(minX, minY, minZ);
        max.set(maxX, maxY, maxZ);
    }

    private Utils() {

    }
}
