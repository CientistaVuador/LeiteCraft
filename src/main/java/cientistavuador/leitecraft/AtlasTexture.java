package cientistavuador.leitecraft;

import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 *
 * @author Cien
 */
public class AtlasTexture {
    
    public static enum BlendMode {
        OPAQUE(0), TESTED(1), BLENDED(2);
        
        private final int index;
        
        private BlendMode(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }
    
    private final String name;
    private final int width;
    private final int height;
    private final Vector2f lowerPosition = new Vector2f();
    private final Vector2f higherPosition = new Vector2f();
    private final BlendMode blendMode;
    
    public AtlasTexture(String name, int width, int height, float x0, float y0, float x1, float y1, BlendMode mode) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.lowerPosition.set(x0, y0);
        this.higherPosition.set(x1, y1);
        if (mode == null) {
            this.blendMode = BlendMode.OPAQUE;
        } else {
            this.blendMode = mode;
        }
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    
    public Vector2fc getLowerPosition() {
        return lowerPosition;
    }

    public Vector2fc getHigherPosition() {
        return higherPosition;
    }
    
    public BlendMode getBlendMode() {
        return blendMode;
    }
    
}
