package cientistavuador.leitecraft;

/**
 *
 * @author Cien
 */
public class Block {

    private final int id;
    private final AtlasTexture[] textures;
    
    private boolean alphaEnabled = false;
    private boolean transparent = false;
    
    private int lightEmission = 0;
    private boolean collision = true;
    
    private boolean liquid = false;
    private AtlasTexture overlayTexture = null;
    
    public Block(int id,
            AtlasTexture startNegativeX, AtlasTexture startPositiveX,
            AtlasTexture startNegativeY, AtlasTexture startPositiveY,
            AtlasTexture startNegativeZ, AtlasTexture startPositiveZ,
            AtlasTexture endNegativeX, AtlasTexture endPositiveX,
            AtlasTexture endNegativeY, AtlasTexture endPositiveY,
            AtlasTexture endNegativeZ, AtlasTexture endPositiveZ
    ) {
        if (id <= 0) {
            throw new RuntimeException("Invalid ID");
        }
        this.id = id;
        this.textures = new AtlasTexture[]{
            startNegativeX, startPositiveX,
            startNegativeY, startPositiveY,
            startNegativeZ, startPositiveZ,
            endNegativeX, endPositiveX,
            endNegativeY, endPositiveY,
            endNegativeZ, endPositiveZ
        };
    }

    public Block(int id,
            AtlasTexture negativeX, AtlasTexture positiveX,
            AtlasTexture negativeY, AtlasTexture positiveY,
            AtlasTexture negativeZ, AtlasTexture positiveZ
    ) {
        this(id,
                negativeX, positiveX,
                negativeY, positiveY,
                negativeZ, positiveZ,
                null, null,
                null, null,
                null, null
        );
    }

    public Block(int id, AtlasTexture top, AtlasTexture side, AtlasTexture bottom) {
        this(id, side, side, bottom, top, side, side);
    }

    public Block(int id, AtlasTexture topBottom, AtlasTexture side) {
        this(id, topBottom, side, topBottom);
    }

    public Block(int id, AtlasTexture texture) {
        this(id, texture, texture);
    }

    public int getId() {
        return id;
    }

    public AtlasTexture getTexture(int side) {
        return this.textures[side];
    }

    public boolean isAlphaEnabled() {
        return alphaEnabled;
    }
    
    public void setAlphaEnabled(boolean alphaEnabled) {
        this.alphaEnabled = alphaEnabled;
    }
    
    public boolean isTransparent() {
        return transparent;
    }
    
    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    public int getLightEmission() {
        return lightEmission;
    }

    public void setLightEmission(int lightEmission) {
        this.lightEmission = lightEmission;
    }
    
    public int getDefaultData() {
        return 0;
    }

    public void setCollision(boolean collision) {
        this.collision = collision;
    }
    
    public boolean hasCollision() {
        return this.collision;
    }

    public boolean isLiquid() {
        return liquid;
    }

    public void setLiquid(boolean liquid) {
        this.liquid = liquid;
    }

    public AtlasTexture getOverlayTexture() {
        return overlayTexture;
    }

    public void setOverlayTexture(AtlasTexture overlayTexture) {
        this.overlayTexture = overlayTexture;
    }
    
    private void writeBlockFaceVertices(int x, int y, int z, VerticesStream stream, ChunkMatrix matrix, int face) {
        BlockVertices.writeFace(matrix, x, y, z, stream, this.textures[face], this.textures[face + 6], face);
    }

    public void writeBlockVertices(
            int x, int y, int z,
            VerticesStream stream,
            ChunkMatrix matrix
            ) {
        
        Block bNegativeX = matrix.getBlock(x - 1, y, z);
        Block bPositiveX = matrix.getBlock(x + 1, y, z);
        Block bNegativeY = matrix.getBlock(x, y - 1, z);
        Block bPositiveY = matrix.getBlock(x, y + 1, z);
        Block bNegativeZ = matrix.getBlock(x, y, z - 1);
        Block bPositiveZ = matrix.getBlock(x, y, z + 1);
        
        if (bNegativeX == Blocks.AIR || (bNegativeX.isTransparent() && bNegativeX != this)) {
            writeBlockFaceVertices(x, y, z, stream, matrix, BlockVertices.NEGATIVE_X);
        }
        if (bPositiveX == Blocks.AIR || (bPositiveX.isTransparent() && bPositiveX != this)) {
            writeBlockFaceVertices(x, y, z, stream, matrix, BlockVertices.POSITIVE_X);
        }
        if (bNegativeY == Blocks.AIR || (bNegativeY.isTransparent() && bNegativeY != this)) {
            writeBlockFaceVertices(x, y, z, stream, matrix, BlockVertices.NEGATIVE_Y);
        }
        if (bPositiveY == Blocks.AIR || (bPositiveY.isTransparent() && bPositiveY != this)) {
            writeBlockFaceVertices(x, y, z, stream, matrix, BlockVertices.POSITIVE_Y);
        }
        if (bNegativeZ == Blocks.AIR || (bNegativeZ.isTransparent() && bNegativeZ != this)) {
            writeBlockFaceVertices(x, y, z, stream, matrix, BlockVertices.NEGATIVE_Z);
        }
        if (bPositiveZ == Blocks.AIR || (bPositiveZ.isTransparent() && bPositiveZ != this)) {
            writeBlockFaceVertices(x, y, z, stream, matrix, BlockVertices.POSITIVE_Z);
        }
    }

}
