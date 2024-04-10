package cientistavuador.leitecraft;

/**
 *
 * @author Cien
 */
public class BlockVertices {
    
    public static final int NEGATIVE_X = 0;
    public static final int POSITIVE_X = 1;
    
    public static final int NEGATIVE_Y = 2;
    public static final int POSITIVE_Y = 3;
    
    public static final int NEGATIVE_Z = 4;
    public static final int POSITIVE_Z = 5;
    
    public static final float MIN_AMBIENT_OCCLUSION = 0.25f;
    public static final float MAX_AMBIENT_OCCLUSION = 1f;
    
    public static float vertexAO(boolean side1, boolean side2, boolean corner) {
        int aoLevel;
        if (side1 && side2) {
            aoLevel = 3;
        } else {
            aoLevel = (side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0);
        }
        float lerp = 1f - (aoLevel / 3f);
        return MIN_AMBIENT_OCCLUSION + (lerp * (MAX_AMBIENT_OCCLUSION - MIN_AMBIENT_OCCLUSION));
    }
    
    public static void writeFace(ChunkMatrix matrix, int x, int y, int z, VerticesStream stream, AtlasTexture startFrame, AtlasTexture endFrame, int face) {
        switch (face) {
            case NEGATIVE_X -> {
                writeNegativeX(matrix, x, y, z, stream, startFrame, endFrame);
            }
            case POSITIVE_X -> {
                writePositiveX(matrix, x, y, z, stream, startFrame, endFrame);
            }
            case NEGATIVE_Y -> {
                writeNegativeY(matrix, x, y, z, stream, startFrame, endFrame);
            }
            case POSITIVE_Y -> {
                writePositiveY(matrix, x, y, z, stream, startFrame, endFrame);
            }
            case NEGATIVE_Z -> {
                writeNegativeZ(matrix, x, y, z, stream, startFrame, endFrame);
            }
            case POSITIVE_Z -> {
                writePositiveZ(matrix, x, y, z, stream, startFrame, endFrame);
            }
            default -> throw new RuntimeException("Unknown face "+face);
        }
    }
    
    private static boolean checkBlock(ChunkMatrix matrix, int x, int y, int z) {
        Block blockAt = matrix.getBlock(x, y, z);
        if (blockAt == Blocks.AIR) {
            return false;
        }
        return !blockAt.isTransparent();
    }
    
    private static void writeNegativeX(ChunkMatrix matrix, int x, int y, int z, VerticesStream stream, AtlasTexture startFrame, AtlasTexture endFrame) {
        float lowX = startFrame.getLowerPosition().x();
        float lowY = startFrame.getLowerPosition().y();
        
        float higX = startFrame.getHigherPosition().x();
        float higY = startFrame.getHigherPosition().y();
        
        int blend = startFrame.getBlendMode().getIndex();
        
        int amountOfFrames = 1;
        float frameOffset = 1f;
        if (endFrame != null) {
            int startX = (int) (startFrame.getLowerPosition().x() * Atlas.getInstance().getWidth());
            int endX = (int) (endFrame.getHigherPosition().x() * Atlas.getInstance().getWidth());
            
            amountOfFrames = ((endX - startX) + 1) / startFrame.getWidth();
            frameOffset = ((float)startFrame.getWidth()) / Atlas.getInstance().getWidth();
        }
        
        float lightLevel = matrix.getChunk(0, 0).getLightLevelForRendering(x - 1, y, z);
        
        stream.offset();
        
        stream.vertex(
                x + 0f, y + 0f, z + 0f,
                -1f, 0f, 0f,
                lowX, lowY,
                blend,
                frameOffset, amountOfFrames,
                vertexAO(
                        checkBlock(matrix, x - 1, y, z - 1),
                        checkBlock(matrix, x - 1, y - 1, z),
                        checkBlock(matrix, x - 1, y - 1, z - 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 0f, y + 0f, z + 1f,
                -1f, 0f, 0f,
                higX, lowY,
                blend,
                frameOffset, amountOfFrames,
                vertexAO(
                        checkBlock(matrix, x - 1, y, z + 1),
                        checkBlock(matrix, x - 1, y - 1, z),
                        checkBlock(matrix, x - 1, y - 1, z + 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 0f, y + 1f, z + 0f,
                -1f, 0f, 0f,
                lowX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x - 1, y, z - 1),
                        checkBlock(matrix, x - 1, y + 1, z),
                        checkBlock(matrix, x - 1, y + 1, z - 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 0f, y + 1f, z + 1f,
                -1f, 0f, 0f,
                higX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x - 1, y, z + 1),
                        checkBlock(matrix, x - 1, y + 1, z),
                        checkBlock(matrix, x - 1, y + 1, z + 1)
                ),
                lightLevel
        );
        
        stream.indices(0, 1, 2, 1, 3, 2);
    }
    
    private static void writePositiveX(ChunkMatrix matrix, int x, int y, int z, VerticesStream stream, AtlasTexture startFrame, AtlasTexture endFrame) {
        float lowX = startFrame.getLowerPosition().x();
        float lowY = startFrame.getLowerPosition().y();
        
        float higX = startFrame.getHigherPosition().x();
        float higY = startFrame.getHigherPosition().y();
        
        int blend = startFrame.getBlendMode().getIndex();
        
        int amountOfFrames = 1;
        float frameOffset = 1f;
        if (endFrame != null) {
            int startX = (int) (startFrame.getLowerPosition().x() * Atlas.getInstance().getWidth());
            int endX = (int) (endFrame.getHigherPosition().x() * Atlas.getInstance().getWidth());
            
            amountOfFrames = ((endX - startX) + 1) / startFrame.getWidth();
            frameOffset = ((float)startFrame.getWidth()) / Atlas.getInstance().getWidth();
        }
        
        float lightLevel = matrix.getChunk(0, 0).getLightLevelForRendering(x + 1, y, z);
        
        stream.offset();
        
        stream.vertex(
                x + 1f, y + 0f, z + 1f,
                1f, 0f, 0f,
                lowX, lowY,
                blend,
                frameOffset, amountOfFrames,
                vertexAO(
                        checkBlock(matrix, x + 1, y, z + 1),
                        checkBlock(matrix, x + 1, y - 1, z),
                        checkBlock(matrix, x + 1, y - 1, z + 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 1f, y + 0f, z + 0f,
                1f, 0f, 0f,
                higX, lowY,
                blend,
                frameOffset, amountOfFrames,
                vertexAO(
                        checkBlock(matrix, x + 1, y, z - 1),
                        checkBlock(matrix, x + 1, y - 1, z),
                        checkBlock(matrix, x + 1, y - 1, z - 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 1f, y + 1f, z + 1f,
                1f, 0f, 0f,
                lowX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x + 1, y, z + 1),
                        checkBlock(matrix, x + 1, y + 1, z),
                        checkBlock(matrix, x + 1, y + 1, z + 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 1f, y + 1f, z + 0f,
                1f, 0f, 0f,
                higX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x + 1, y, z - 1),
                        checkBlock(matrix, x + 1, y + 1, z),
                        checkBlock(matrix, x + 1, y + 1, z - 1)
                ),
                lightLevel
        );
        
        stream.indices(0, 1, 2, 1, 3, 2);
    }
    
    private static void writeNegativeY(ChunkMatrix matrix, int x, int y, int z, VerticesStream stream, AtlasTexture startFrame, AtlasTexture endFrame) {
        float lowX = startFrame.getLowerPosition().x();
        float lowY = startFrame.getLowerPosition().y();
        
        float higX = startFrame.getHigherPosition().x();
        float higY = startFrame.getHigherPosition().y();
        
        int blend = startFrame.getBlendMode().getIndex();
        
        int amountOfFrames = 1;
        float frameOffset = 1f;
        if (endFrame != null) {
            int startX = (int) (startFrame.getLowerPosition().x() * Atlas.getInstance().getWidth());
            int endX = (int) (endFrame.getHigherPosition().x() * Atlas.getInstance().getWidth());
            
            amountOfFrames = ((endX - startX) + 1) / startFrame.getWidth();
            frameOffset = ((float)startFrame.getWidth()) / Atlas.getInstance().getWidth();
        }
        
        float lightLevel = matrix.getChunk(0, 0).getLightLevelForRendering(x, y - 1, z);
        
        stream.offset();
        
        stream.vertex(
                x + 0f, y + 0f, z + 0f,
                0f, -1f, 0f,
                lowX, lowY,
                blend,
                frameOffset, amountOfFrames,
                vertexAO(
                        checkBlock(matrix, x - 1, y - 1, z),
                        checkBlock(matrix, x, y - 1, z - 1),
                        checkBlock(matrix, x - 1, y - 1, z - 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 1f, y + 0f, z + 0f,
                0f, -1f, 0f,
                higX, lowY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x + 1, y - 1, z),
                        checkBlock(matrix, x, y - 1, z - 1),
                        checkBlock(matrix, x + 1, y - 1, z - 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 0f, y + 0f, z + 1f,
                0f, -1f, 0f,
                lowX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x - 1, y - 1, z),
                        checkBlock(matrix, x, y - 1, z + 1),
                        checkBlock(matrix, x - 1, y - 1, z + 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 1f, y + 0f, z + 1f,
                0f, -1f, 0f,
                higX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x + 1, y - 1, z),
                        checkBlock(matrix, x, y - 1, z + 1),
                        checkBlock(matrix, x + 1, y - 1, z + 1)
                ),
                lightLevel
        );
        
        stream.indices(0, 1, 2, 1, 3, 2);
    }
    
    private static void writePositiveY(ChunkMatrix matrix, int x, int y, int z, VerticesStream stream, AtlasTexture startFrame, AtlasTexture endFrame) {
        float lowX = startFrame.getLowerPosition().x();
        float lowY = startFrame.getLowerPosition().y();
        
        float higX = startFrame.getHigherPosition().x();
        float higY = startFrame.getHigherPosition().y();
        
        int blend = startFrame.getBlendMode().getIndex();
        
        int amountOfFrames = 1;
        float frameOffset = 1f;
        if (endFrame != null) {
            int startX = (int) (startFrame.getLowerPosition().x() * Atlas.getInstance().getWidth());
            int endX = (int) (endFrame.getHigherPosition().x() * Atlas.getInstance().getWidth());
            
            amountOfFrames = ((endX - startX) + 1) / startFrame.getWidth();
            frameOffset = ((float)startFrame.getWidth()) / Atlas.getInstance().getWidth();
        }
        
        float lightLevel = matrix.getChunk(0, 0).getLightLevelForRendering(x, y + 1, z);
        
        stream.offset();
        
        stream.vertex(
                x + 0f, y + 1f, z + 1f,
                0f, 1f, 0f,
                lowX, lowY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x, y + 1, z + 1),
                        checkBlock(matrix, x - 1, y + 1, z),
                        checkBlock(matrix, x - 1, y + 1, z + 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 1f, y + 1f, z + 1f,
                0f, 1f, 0f,
                higX, lowY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x + 1, y + 1, z),
                        checkBlock(matrix, x, y + 1, z + 1),
                        checkBlock(matrix, x + 1, y + 1, z + 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 0f, y + 1f, z + 0f,
                0f, 1f, 0f,
                lowX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x - 1, y + 1, z),
                        checkBlock(matrix, x, y + 1, z - 1),
                        checkBlock(matrix, x - 1, y + 1, z - 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 1f, y + 1f, z + 0f,
                0f, 1f, 0f,
                higX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x + 1, y + 1, z),
                        checkBlock(matrix, x, y + 1, z - 1),
                        checkBlock(matrix, x + 1, y + 1, z - 1)
                ),
                lightLevel
        );
        
        stream.indices(0, 1, 2, 1, 3, 2);
    }
    
    private static void writeNegativeZ(ChunkMatrix matrix, int x, int y, int z, VerticesStream stream, AtlasTexture startFrame, AtlasTexture endFrame) {
        float lowX = startFrame.getLowerPosition().x();
        float lowY = startFrame.getLowerPosition().y();
        
        float higX = startFrame.getHigherPosition().x();
        float higY = startFrame.getHigherPosition().y();
        
        int blend = startFrame.getBlendMode().getIndex();
        
        int amountOfFrames = 1;
        float frameOffset = 1f;
        if (endFrame != null) {
            int startX = (int) (startFrame.getLowerPosition().x() * Atlas.getInstance().getWidth());
            int endX = (int) (endFrame.getHigherPosition().x() * Atlas.getInstance().getWidth());
            
            amountOfFrames = ((endX - startX) + 1) / startFrame.getWidth();
            frameOffset = ((float)startFrame.getWidth()) / Atlas.getInstance().getWidth();
        }
        
        float lightLevel = matrix.getChunk(0, 0).getLightLevelForRendering(x, y, z - 1);
        
        stream.offset();
        
        stream.vertex(
                x + 1f, y + 0f, z + 0f,
                0f, 0f, -1f,
                lowX, lowY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x + 1, y, z - 1),
                        checkBlock(matrix, x, y - 1, z - 1),
                        checkBlock(matrix, x + 1, y - 1, z - 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 0f, y + 0f, z + 0f,
                0f, 0f, -1f,
                higX, lowY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x - 1, y, z - 1),
                        checkBlock(matrix, x, y - 1, z - 1),
                        checkBlock(matrix, x - 1, y - 1, z - 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 1f, y + 1f, z + 0f,
                0f, 0f, -1f,
                lowX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x + 1, y, z - 1),
                        checkBlock(matrix, x, y + 1, z - 1),
                        checkBlock(matrix, x + 1, y + 1, z - 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 0f, y + 1f, z + 0f,
                0f, 0f, -1f,
                higX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x - 1, y, z - 1),
                        checkBlock(matrix, x, y + 1, z - 1),
                        checkBlock(matrix, x - 1, y + 1, z - 1)
                ),
                lightLevel
        );
        
        stream.indices(0, 1, 2, 1, 3, 2);
    }
    
    private static void writePositiveZ(ChunkMatrix matrix, int x, int y, int z, VerticesStream stream, AtlasTexture startFrame, AtlasTexture endFrame) {
        float lowX = startFrame.getLowerPosition().x();
        float lowY = startFrame.getLowerPosition().y();
        
        float higX = startFrame.getHigherPosition().x();
        float higY = startFrame.getHigherPosition().y();
        
        int blend = startFrame.getBlendMode().getIndex();
        
        int amountOfFrames = 1;
        float frameOffset = 1f;
        if (endFrame != null) {
            int startX = (int) (startFrame.getLowerPosition().x() * Atlas.getInstance().getWidth());
            int endX = (int) (endFrame.getHigherPosition().x() * Atlas.getInstance().getWidth());
            
            amountOfFrames = ((endX - startX) + 1) / startFrame.getWidth();
            frameOffset = ((float)startFrame.getWidth()) / Atlas.getInstance().getWidth();
        }
        
        float lightLevel = matrix.getChunk(0, 0).getLightLevelForRendering(x, y, z + 1);
        
        stream.offset();
        
        stream.vertex(
                x + 0f, y + 0f, z + 1f,
                0f, 0f, 1f,
                lowX, lowY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x - 1, y, z + 1),
                        checkBlock(matrix, x, y - 1, z + 1),
                        checkBlock(matrix, x - 1, y - 1, z + 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 1f, y + 0f, z + 1f,
                0f, 0f, 1f,
                higX, lowY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x + 1, y, z + 1),
                        checkBlock(matrix, x, y - 1, z + 1),
                        checkBlock(matrix, x + 1, y - 1, z + 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 0f, y + 1f, z + 1f,
                0f, 0f, 1f,
                lowX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x - 1, y, z + 1),
                        checkBlock(matrix, x, y + 1, z + 1),
                        checkBlock(matrix, x - 1, y + 1, z + 1)
                ),
                lightLevel
        );
        stream.vertex(
                x + 1f, y + 1f, z + 1f,
                0f, 0f, 1f,
                higX, higY,
                blend,
                frameOffset, amountOfFrames, 
                vertexAO(
                        checkBlock(matrix, x + 1, y, z + 1),
                        checkBlock(matrix, x, y + 1, z + 1),
                        checkBlock(matrix, x + 1, y + 1, z + 1)
                ),
                lightLevel
        );
        
        stream.indices(0, 1, 2, 1, 3, 2);
    }
    
    private BlockVertices() {
        
    }
    
}
