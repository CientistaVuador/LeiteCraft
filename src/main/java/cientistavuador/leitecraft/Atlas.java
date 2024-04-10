package cientistavuador.leitecraft;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.joml.Vector2i;
import org.joml.Vector4f;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.opengl.GL20.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 *
 * @author Cien
 */
public class Atlas {

    public static final float MAX_TEXTURE_LOD = 4f;
    
    private static void getClampedPixel(ByteBuffer image, int x, int y, int width, int height, Vector4f receiver) {
        x = Math.min(Math.max(x, 0), width - 1);
        y = Math.min(Math.max(y, 0), height - 1);

        int index = (x + (y * width)) * 4;

        int r = image.get(index + 0) & 0xFF;
        int g = image.get(index + 1) & 0xFF;
        int b = image.get(index + 2) & 0xFF;
        int a = image.get(index + 3) & 0xFF;

        receiver.set(
                r / 255f,
                g / 255f,
                b / 255f,
                a / 255f
        );
    }

    public static ByteBuffer generateMipmap(ByteBuffer image, int width, int height, Vector2i outputWidthHeight) {
        int mipWidth = (int) Math.max(width / 2f, 1f);
        int mipHeight = (int) Math.max(height / 2f, 1f);
        
        outputWidthHeight.set(mipWidth, mipHeight);

        ByteBuffer downscaledImage = MemoryUtil.memAlloc(mipWidth * mipHeight * 4);
        
        Vector4f pixel = new Vector4f();
        Vector4f pixelAverage = new Vector4f();

        for (int i = 0; i < mipWidth * mipHeight; i++) {
            int x = i % mipWidth;
            int y = i / mipWidth;

            pixelAverage.zero();

            getClampedPixel(image, (x * 2), (y * 2), width, height, pixel);
            pixelAverage.add(pixel);

            getClampedPixel(image, (x * 2) + 1, (y * 2), width, height, pixel);
            pixelAverage.add(pixel);

            getClampedPixel(image, (x * 2), (y * 2) + 1, width, height, pixel);
            pixelAverage.add(pixel);

            getClampedPixel(image, (x * 2) + 1, (y * 2) + 1, width, height, pixel);
            pixelAverage.add(pixel);

            pixelAverage.div(4f);

            int r = Math.min(Math.max((int) (pixelAverage.x() * 255f), 0), 255);
            int g = Math.min(Math.max((int) (pixelAverage.y() * 255f), 0), 255);
            int b = Math.min(Math.max((int) (pixelAverage.z() * 255f), 0), 255);
            int a = Math.min(Math.max((int) (pixelAverage.w() * 255f), 0), 255);

            downscaledImage
                    .put((byte) r)
                    .put((byte) g)
                    .put((byte) b)
                    .put((byte) a);
        }

        downscaledImage.flip();

        return downscaledImage;
    }

    public static final float EPSILON = 0.001f;

    private static final Atlas INSTANCE;

    static {
        byte[] imageData;
        {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                try (InputStream imageStream = Atlas.class.getResourceAsStream("atlas.png")) {
                    byte[] buffer = new byte[8192];
                    int r;
                    while ((r = imageStream.read(buffer)) != -1) {
                        stream.write(buffer, 0, r);
                    }
                }
                imageData = stream.toByteArray();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        ByteBuffer nativeImageData = MemoryUtil.memAlloc(imageData.length);

        ByteBuffer outputImage;
        int width;
        int height;

        try {
            nativeImageData.put(imageData).flip();

            stbi_set_flip_vertically_on_load_thread(1);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer imageWidth = stack.mallocInt(1);
                IntBuffer imageHeight = stack.mallocInt(1);
                IntBuffer imageChannels = stack.mallocInt(1);

                outputImage = stbi_load_from_memory(nativeImageData, imageWidth, imageHeight, imageChannels, 4);
                
                if (outputImage == null) {
                    throw new RuntimeException("Failed to load atlas: " + stbi_failure_reason());
                }

                width = imageWidth.get();
                height = imageHeight.get();
            }
        } finally {
            MemoryUtil.memFree(nativeImageData);
        }
        
        for (int i = 0; i < width * height; i++) {
            outputImage.mark();
            
            float r = (outputImage.get() & 0xFF) / 255f;
            float g = (outputImage.get() & 0xFF) / 255f;
            float b = (outputImage.get() & 0xFF) / 255f;
            float a = (outputImage.get() & 0xFF) / 255f;
            
            r *= a;
            g *= a;
            b *= a;
            
            byte br = (byte) (r * 255f);
            byte bg = (byte) (g * 255f);
            byte bb = (byte) (b * 255f);
            byte ba = (byte) (a * 255f);
            
            outputImage.reset();
            
            outputImage.put(br);
            outputImage.put(bg);
            outputImage.put(bb);
            outputImage.put(ba);
        }
        
        outputImage.flip();

        int mipLevelsLength = (int) Math.abs(
                Math.log(Math.max(width, height)) / Math.log(2.0)
        ) + 1;

        ByteBuffer[] mipLevels = new ByteBuffer[mipLevelsLength];
        int[] mipWidth = new int[mipLevelsLength];
        int[] mipHeight = new int[mipLevelsLength];
        {
            Vector2i outputWidthHeight = new Vector2i(width, height);
            ByteBuffer currentData = outputImage;
            for (int i = 0; i < mipLevels.length; i++) {
                mipLevels[i] = currentData;
                mipWidth[i] = outputWidthHeight.x();
                mipHeight[i] = outputWidthHeight.y();
                
                if (i == (mipLevels.length - 1)) {
                    break;
                }
                
                currentData = generateMipmap(
                        currentData, outputWidthHeight.x(), outputWidthHeight.y(),
                        outputWidthHeight
                );
            }
        }

        int texture = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        
        for (int i = 0; i < mipLevels.length; i++) {
            ByteBuffer mipLevel = mipLevels[i];
            glTexImage2D(GL_TEXTURE_2D, i,
                    GL_RGBA8, mipWidth[i], mipHeight[i],
                    0,
                    GL_RGBA, GL_UNSIGNED_BYTE, mipLevel
            );
            if (i == 0) {
                stbi_image_free(mipLevel);
            } else {
                MemoryUtil.memFree(mipLevel);
            }
        }
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, MAX_TEXTURE_LOD);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);

        List<AtlasTexture> textures = new ArrayList<>();

        try {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            Atlas.class.getResourceAsStream("atlas.csv"),
                            StandardCharsets.UTF_8)
            )) {
                String r;
                while ((r = reader.readLine()) != null) {
                    String[] split = r.split(Pattern.quote(","));

                    String name = split[0];
                    float lowerX = Float.parseFloat(split[1]);
                    float lowerY = Float.parseFloat(split[2]) + 1f;
                    float higherX = Float.parseFloat(split[3]) + 1f;
                    float higherY = Float.parseFloat(split[4]);
                    
                    int atlasTextureWidth = Math.abs(Math.round(higherX - lowerX));
                    int atlasTextureHeight = Math.abs(Math.round(higherY - lowerY));
                    
                    higherY += EPSILON;
                    lowerY -= EPSILON;

                    lowerX += EPSILON;
                    higherX -= EPSILON;

                    lowerY = height - lowerY;
                    higherY = height - higherY;

                    lowerX /= width;
                    higherX /= width;

                    lowerY /= height;
                    higherY /= height;

                    AtlasTexture.BlendMode mode = AtlasTexture.BlendMode.valueOf(split[5].toUpperCase());

                    textures.add(new AtlasTexture(name,
                            atlasTextureWidth, atlasTextureHeight,
                            lowerX, lowerY,
                            higherX, higherY,
                            mode
                    ));
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        System.out.println("Finished loading atlas with size " + width + "x" + height + " and with " + textures.size() + " textures!");

        INSTANCE = new Atlas(texture, textures, width, height);
    }

    public static Atlas getInstance() {
        return INSTANCE;
    }

    private final int texture;
    private final AtlasTexture[] textures;
    private final Map<String, AtlasTexture> map = new HashMap<>();
    private final int width;
    private final int height;

    private Atlas(int texture, List<AtlasTexture> textures, int width, int height) {
        this.texture = texture;
        this.textures = textures.toArray(AtlasTexture[]::new);
        for (AtlasTexture e : this.textures) {
            this.map.put(e.getName(), e);
        }
        this.width = width;
        this.height = height;
    }

    public int getTexture() {
        return texture;
    }

    public AtlasTexture getTexture(int index) {
        return this.textures[index];
    }

    public int getTexturesLength() {
        return this.textures.length;
    }

    public AtlasTexture getTexture(String name) {
        return this.map.get(name);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}
