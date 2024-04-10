package cientistavuador.leitecraft;

/**
 *
 * @author Cien
 */
public class BillboardBlock extends Block {
    
    public static final float AMBIENT_OCCLUSION = 0.75f;
    
    public BillboardBlock(int id, AtlasTexture texture) {
        super(id, texture);
    }

    private void writeQuad0(float lightLevel, float x, float y, float z, VerticesStream stream, AtlasTexture texture, boolean backFace) {
        int blendMode = texture.getBlendMode().getIndex();

        float lowX = texture.getLowerPosition().x();
        float lowY = texture.getLowerPosition().y();

        float higX = texture.getHigherPosition().x();
        float higY = texture.getHigherPosition().y();
        
        stream.offset();

        stream.vertex(
                x, y, z,
                0f, 1f, 0f,
                lowX, lowY,
                blendMode, 0f, 1, AMBIENT_OCCLUSION, lightLevel
        );

        stream.vertex(
                x + 1f, y, z + 1f,
                0f, 1f, 0f,
                higX, lowY,
                blendMode, 0f, 1, AMBIENT_OCCLUSION, lightLevel
        );

        stream.vertex(
                x, y + 1f, z,
                0f, 1f, 0f,
                lowX, higY,
                blendMode, 0f, 1, 1f, lightLevel
        );

        stream.vertex(
                x + 1f, y + 1f, z + 1f,
                0f, 1f, 0f,
                higX, higY,
                blendMode, 0f, 1, 1f, lightLevel
        );

        if (backFace) {
            stream.indices(
                    0, 2, 1,
                    1, 2, 3
            );
        } else {
            stream.indices(
                    0, 1, 2,
                    1, 3, 2
            );
        }
    }
    
    private void writeQuad1(float lightLevel, float x, float y, float z, VerticesStream stream, AtlasTexture texture, boolean backFace) {
        int blendMode = texture.getBlendMode().getIndex();

        float lowX = texture.getLowerPosition().x();
        float lowY = texture.getLowerPosition().y();

        float higX = texture.getHigherPosition().x();
        float higY = texture.getHigherPosition().y();
        
        stream.offset();

        stream.vertex(
                x + 1f, y, z,
                0f, 1f, 0f,
                lowX, lowY,
                blendMode, 0f, 1, AMBIENT_OCCLUSION, lightLevel
        );

        stream.vertex(
                x, y, z + 1f,
                0f, 1f, 0f,
                higX, lowY,
                blendMode, 0f, 1, AMBIENT_OCCLUSION, lightLevel
        );

        stream.vertex(
                x + 1f, y + 1f, z,
                0f, 1f, 0f,
                lowX, higY,
                blendMode, 0f, 1, 1f, lightLevel
        );

        stream.vertex(
                x, y + 1f, z + 1f,
                0f, 1f, 0f,
                higX, higY,
                blendMode, 0f, 1, 1f, lightLevel
        );

        if (backFace) {
            stream.indices(
                    0, 2, 1,
                    1, 2, 3
            );
        } else {
            stream.indices(
                    0, 1, 2,
                    1, 3, 2
            );
        }
    }
    
    @Override
    public void writeBlockVertices(int x, int y, int z,
            VerticesStream stream,
            ChunkMatrix matrix
    ) {
        float lightLevel = matrix.getChunk(0, 0).getLightLevelForRendering(x, y, z);
        
        writeQuad0(lightLevel, x, y, z, stream, getTexture(0), false);
        writeQuad0(lightLevel, x, y, z, stream, getTexture(0), true);
        writeQuad1(lightLevel, x, y, z, stream, getTexture(0), false);
        writeQuad1(lightLevel, x, y, z, stream, getTexture(0), true);
    }

}
