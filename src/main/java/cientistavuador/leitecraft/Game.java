package cientistavuador.leitecraft;

import java.util.Locale;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.glfw.GLFW.*;
import static cientistavuador.leitecraft.ConvexPolygonRenderer.*;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author Cien
 */
public class Game {

    public static boolean CAN_UPDATE_CHUNK = true;

    private static final Game INSTANCE = new Game();

    public static Game getInstance() {
        return INSTANCE;
    }

    private final World world = new World();
    private final WorldCamera worldCamera = new WorldCamera(this.world);
    private final PlayerController controller = new PlayerController(world);
    private final FreeCamera camera = new FreeCamera();
    private final Outline outline = new Outline(world, camera);

    private final Vector3f sunAmbient = new Vector3f(0.4f, 0.4f, 0.4f);
    private final Vector3f sunDiffuse = new Vector3f(0.6f, 0.6f, 0.6f);
    private final Vector3f sunDirection = new Vector3f(-1f, -1f, -1f).normalize();

    private int currentFrame = 0;
    private float currentFrameCounter = 0f;

    private boolean wireframeEnabled = false;
    private boolean playerWireframeEnabled = false;

    private final List<SmokeSpawner> smokeSpawner = new ArrayList<>();

    private Block currentBlock = Blocks.LANTERN;

    private boolean hoveringIncreaseViewDistance = false;
    private boolean hoveringDecreaseViewDistance = false;
    private boolean hoveringGPUOcclusionCulling = false;

    private boolean gpuOcclusionCulling = true;

    public Game() {
        this.camera.setPosition(8f, ChunkGenerator.MAX_TERRAIN_HEIGHT + 1, 4f);

        this.camera.setMovementEnabled(false);

        this.controller.getPlayerPhysics().teleportToTheTop();
    }

    private void render(RenderableChunk chunk, boolean alpha) {
        int count = chunk.getSolidCount();
        int offset = chunk.getSolidOffset();
        if (alpha) {
            count = chunk.getAlphaCount();
            offset = chunk.getAlphaOffset();
        }

        if (count == 0) {
            return;
        }
        
        ChunkShader.uniformModel(chunk.getModelMatrix());

        int verticesVBO = chunk.getVBO();
        int verticesEBO = chunk.getEBO();
        
        glBindBuffer(GL_ARRAY_BUFFER, verticesVBO);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, verticesEBO);
        
        ChunkShader.attribute(ChunkShader.ATTRIBUTE_POSITION, 3,
                VerticesStream.VERTEX_SIZE, VerticesStream.XYZ_OFFSET, true);

        ChunkShader.attribute(ChunkShader.ATTRIBUTE_NORMAL, 3,
                VerticesStream.VERTEX_SIZE, VerticesStream.NORMAL_XYZ_OFFSET, true);

        ChunkShader.attribute(ChunkShader.ATTRIBUTE_UV, 2,
                VerticesStream.VERTEX_SIZE, VerticesStream.UV_OFFSET, true);

        ChunkShader.attribute(ChunkShader.ATTRIBUTE_BLEND_MODE, 1,
                VerticesStream.VERTEX_SIZE, VerticesStream.BLEND_MODE_OFFSET, false);

        ChunkShader.attribute(ChunkShader.ATTRIBUTE_ANIMATED_OFFSET, 1,
                VerticesStream.VERTEX_SIZE, VerticesStream.ANIMATED_OFFSET, true);

        ChunkShader.attribute(ChunkShader.ATTRIBUTE_ANIMATED_FRAMES, 1,
                VerticesStream.VERTEX_SIZE, VerticesStream.ANIMATED_FRAMES_OFFSET, false);

        ChunkShader.attribute(ChunkShader.ATTRIBUTE_AO, 1,
                VerticesStream.VERTEX_SIZE, VerticesStream.AO_OFFSET, true);
        
        ChunkShader.attribute(ChunkShader.ATTRIBUTE_SHADOW, 1,
                VerticesStream.VERTEX_SIZE, VerticesStream.SHADOW_OFFSET, true);
        
        glDrawElements(
                GL_TRIANGLES,
                count,
                GL_UNSIGNED_INT,
                offset
        );
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void update() {
        this.currentFrameCounter += Main.TPF;
        if (this.currentFrameCounter >= 0.35f) {
            this.currentFrameCounter = 0f;
            this.currentFrame++;
        }
        if (this.currentFrame >= 256) {
            this.currentFrame = 0;
        }

        if (!this.camera.isMovementEnabled()) {
            this.controller.update();
        }
        
        if (this.playerWireframeEnabled) {
            this.controller.getPlayerPhysics().debugUpdate(this.camera);
        }

        if (!this.camera.isMovementEnabled()) {
            this.controller.updateMovement(this.camera);
        } else {
            this.camera.updateMovement();
        }

        this.worldCamera.update(this.camera.getPosition().x(), this.camera.getPosition().z());

        this.outline.update();

        if (Main.isSupported(3, 3)) {
            for (SmokeSpawner s : this.smokeSpawner) {
                s.update(this.camera);
            }
        }
    }

    public void loop() {
        Game.CAN_UPDATE_CHUNK = true;

        update();

        {
            final float textSize = 0.032f;
            UIRenderer.drawText(new StringBuilder()
                    .append("X: ").append(String.format(Locale.US, "%.2f", this.camera.getPosition().x())).append('\n')
                    .append("Y: ").append(String.format(Locale.US, "%.2f", this.camera.getPosition().y())).append('\n')
                    .append("Z: ").append(String.format(Locale.US, "%.2f", this.camera.getPosition().z())).append('\n')
                    .toString(),
                    0f, 1f - (textSize * UIRenderer.ratioY()), textSize
            );
        }
        
        List<RenderableChunk> toRender = new ArrayList<>();

        for (int i = 0; i < this.worldCamera.amountOfRenderableChunks(); i++) {
            RenderableChunk c = this.worldCamera.getRenderableChunk(i);
            if (c == null) {
                continue;
            }
            if (c.getEBO() == 0 || c.getVBO() == 0) {
                continue;
            }
            c.calculateRelativeLocationAndMatrices(this.camera);
            toRender.add(c);
        }

        Matrix4f frustum = new Matrix4f(this.camera.getProjection())
                .mul(this.camera.getView());

        List<RenderableChunk> toRemove = new ArrayList<>();
        List<RenderableChunk> toQuery = new ArrayList<>();
        for (RenderableChunk chunk : toRender) {
            Vector3fc halfExtents = chunk.getHalfExtents();

            float width = halfExtents.x() * 2f;
            float height = halfExtents.y() * 2f;
            float depth = halfExtents.z() * 2f;

            float minX = chunk.getRelativeCameraLocation().x();
            float minY = chunk.getRelativeCameraLocation().y();
            float minZ = chunk.getRelativeCameraLocation().z();

            float maxX = minX + width;
            float maxY = minY + height;
            float maxZ = minZ + depth;

            if (!frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ)) {
                toRemove.add(chunk);
                continue;
            }

            if (GPUOcclusion.testCamera(
                    0f, 0f, 0f,
                    Camera.NEAR_PLANE * 2f,
                    chunk.getOcclusionModelMatrix()
            )) {
                continue;
            }

            toQuery.add(chunk);

            if (this.gpuOcclusionCulling) {
                int queryObject = chunk.getQueryObject();
                if (queryObject != 0) {
                    int samplesPassed = glGetQueryObjecti(queryObject, GL_QUERY_RESULT);
                    if (samplesPassed <= 8) {
                        toRemove.add(chunk);
                    }
                }
            }
        }
        toRender.removeAll(toRemove);

        toRender.sort((o1, o2) -> {
            float x1 = o1.getRelativeCameraLocation().x() + (Chunk.CHUNK_SIZE * 0.5f);
            float z1 = o1.getRelativeCameraLocation().z() + (Chunk.CHUNK_SIZE * 0.5f);

            float x2 = o2.getRelativeCameraLocation().x() + (Chunk.CHUNK_SIZE * 0.5f);
            float z2 = o2.getRelativeCameraLocation().z() + (Chunk.CHUNK_SIZE * 0.5f);

            float dist1 = (x1 * x1) + (z1 * z1);
            float dist2 = (x2 * x2) + (z2 * z2);

            return Float.compare(dist1, dist2);
        });

        if (this.gpuOcclusionCulling) {
            for (RenderableChunk chunk : toQuery) {
                int queryObject = chunk.getQueryObject();
                if (queryObject == 0) {
                    queryObject = glGenQueries();
                    chunk.setQueryObject(queryObject);
                }

                GPUOcclusion.occlusionQuery(
                        this.camera.getProjection(),
                        this.camera.getView(),
                        chunk.getOcclusionModelMatrix(),
                        queryObject
                );
            }
        }
        
        ChunkShader.use();

        ChunkShader.uniformCameraMatrices(this.camera.getProjection(), this.camera.getView());
        ChunkShader.uniformSunAmbient(this.sunAmbient);
        ChunkShader.uniformSunDiffuse(this.sunDiffuse);
        ChunkShader.uniformSunDirection(this.sunDirection);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Atlas.getInstance().getTexture());
        ChunkShader.uniformAtlasTexture(0);

        ChunkShader.uniformCurrentFrame(this.currentFrame);
        
        for (int i = 0; i < toRender.size(); i++) {
            render(toRender.get(i), false);
        }
        
        ChunkShader.unuse();

        GPUOcclusion.executeQueries();
        this.outline.render();
        LineRenderer.renderLineStrips();

        ChunkShader.use();
        
        for (int i = toRender.size() - 1; i >= 0; i--) {
            render(toRender.get(i), true);
        }

        ChunkShader.unuse();

        LineRenderer.renderLineStrips();

        if (Main.isSupported(3, 3)) {
            SmokePass.render();
        }

        if (this.wireframeEnabled) {
            glClear(GL_DEPTH_BUFFER_BIT);

            glUseProgram(WireframeShader.SHADER_PROGRAM);

            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

            for (RenderableChunk chunk : toRender) {
                int vbo = chunk.getVBO();
                int ebo = chunk.getEBO();
                int count = chunk.getSolidCount() + chunk.getAlphaCount();
                if (count == 0 || vbo == 0 || ebo == 0) {
                    continue;
                }

                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

                ChunkShader.attribute(
                        WireframeShader.ATTRIBUTE_VERTEX_POSITION,
                        3,
                        VerticesStream.VERTEX_SIZE,
                        VerticesStream.XYZ_OFFSET,
                        true
                );

                Matrix4fc projection = this.camera.getProjection();
                Matrix4fc view = this.camera.getView();
                Matrix4fc model = chunk.getModelMatrix();

                Matrix4f projectionViewModel = new Matrix4f(projection).mul(view).mul(model);

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    FloatBuffer buffer = stack.mallocFloat(4 * 4);
                    projectionViewModel.get(buffer);

                    glUniformMatrix4fv(WireframeShader.UNIFORM_PROJECTION_VIEW_MODEL, false, buffer);
                }

                Random r = new Random(vbo);
                int v = r.nextInt(32) + 1;
                for (int i = 0; i < v; i++) {
                    r.nextLong();
                }
                float hue = r.nextFloat();
                int argb = Color.HSBtoRGB(hue, 1f, 1f);

                float red = ((argb >> 16) & 0xFF) / 255f;
                float green = ((argb >> 8) & 0xFF) / 255f;
                float blue = ((argb >> 0) & 0xFF) / 255f;

                glUniform4f(WireframeShader.UNIFORM_COLOR, red, green, blue, 1f);

                glDrawElements(GL_TRIANGLES, count, GL_UNSIGNED_INT, 0);

                glBindBuffer(GL_ARRAY_BUFFER, 0);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            }

            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

            glUseProgram(0);
        }

        int camBlockX = (int) Math.floor(this.camera.getPosition().x());
        int camBlockY = (int) Math.floor(this.camera.getPosition().y());
        int camBlockZ = (int) Math.floor(this.camera.getPosition().z());

        Block camBlock = this.world.getBlockOrNull(camBlockX, camBlockY, camBlockZ);

        if (camBlock != Blocks.AIR) {
            AtlasTexture overlay = camBlock.getOverlayTexture();
            if (overlay != null) {
                polyBegin();

                polyConvex(
                        -1f, -1f,
                        1f, -1f,
                        1f, 1f,
                        -1f, 1f
                );

                polyTexture(Atlas.getInstance().getTexture());
                polyTextureTranslate(
                        overlay.getLowerPosition().x(),
                        overlay.getLowerPosition().y()
                );
                polyTextureScale(
                        overlay.getHigherPosition().x() - overlay.getLowerPosition().x(),
                        overlay.getHigherPosition().y() - overlay.getLowerPosition().y()
                );

                polyFinish();

                polyDraw();
            }
        }

        glEnable(GL_MULTISAMPLE);

        //crosshair
        {
            polyBegin();

            polyConvexTranslate(-0.5f, 0.5f);
            polyConvexScale(1f / 128f, -1f / 128f);

            polyConvex(
                    0, 108,
                    19, 127,
                    64, 82,
                    45, 63
            );
            polyConvex(
                    0, 19,
                    108, 127,
                    127, 108,
                    19, 0
            );
            polyConvex(
                    63, 45,
                    82, 64,
                    127, 19,
                    108, 0
            );

            polyColor(0.05f, 0.05f, 0.05f, 1f);

            polyScale(0.04f, 0.04f);
            polyFinish();

            polyIdentity();

            polyColor(0.95f, 0.95f, 0.95f, 1f);

            polyScale(0.02f, 0.02f);
            polyFinish();
        }

        //icon
        {
            polyBegin();

            polyConvexTranslate(0.0f, 1.0f);
            polyConvexScale(1f / 64f, -1f / 64f);
            polyConvexColor(0.5f, 0.5f, 0.5f, 1.0f);
            polyConvex(
                    13, 63,
                    50, 63,
                    51, 59,
                    12, 59
            );
            polyConvex(
                    44, 59,
                    51, 59,
                    63, 0,
                    56, 0
            );
            polyConvex(
                    12, 59,
                    19, 59,
                    7, 0,
                    0, 0
            );
            polyConvexColor(0.9f, 0.9f, 0.9f, 1.0f);
            polyConvex(
                    21, 57,
                    42, 57,
                    51, 14,
                    12, 14
            );

            polyTranslate(-0.98f, -0.98f);
            polyScale(0.2f, 0.2f);

            polyFinish();
        }

        //current block
        {
            polyBegin();

            polyConvex(
                    -1, 0,
                    0, 0,
                    0, 1,
                    -1, 1
            );

            AtlasTexture texture = this.currentBlock.getTexture(0);

            polyTexture(Atlas.getInstance().getTexture());
            polyTextureTranslate(
                    texture.getLowerPosition().x(),
                    texture.getLowerPosition().y()
            );
            polyTextureScale(
                    texture.getHigherPosition().x() - texture.getLowerPosition().x(),
                    texture.getHigherPosition().y() - texture.getLowerPosition().y()
            );

            polyTranslate(0.95f, -0.95f);
            polyScale(0.2f, 0.2f);

            polyFinish();
        }

        if (!this.camera.isCapturingMouse()) {
            polyBegin();

            polyConvex(
                    -1, -1,
                    1, -1,
                    0, 1
            );

            polyTranslate(0.923f, 0.12f);
            polyScale(0.03f, 0.03f);

            this.hoveringIncreaseViewDistance = polyTestAabbPoint(Main.MOUSE_X, Main.MOUSE_Y);

            if (this.hoveringIncreaseViewDistance) {
                polyColor(0.0f, 0.5f, 0.0f, 1.0f);
            } else {
                polyColor(0.5f, 0.5f, 0.5f, 1.0f);
            }

            polyFinish();

            float offset = 0f;
            if (this.worldCamera.getViewDistance() > 9) {
                offset = -0.012f;
            }
            UIRenderer.drawText(
                    Integer.toString(this.worldCamera.getViewDistance()),
                    0.95f + offset, 0.50f,
                    0.032f
            );

            polyBegin();

            polyConvex(
                    1, 1,
                    -1, 1,
                    0, -1
            );

            polyTranslate(0.923f, -0.05f);
            polyScale(0.03f, 0.03f);

            this.hoveringDecreaseViewDistance = polyTestAabbPoint(Main.MOUSE_X, Main.MOUSE_Y);

            if (this.hoveringDecreaseViewDistance) {
                polyColor(0.0f, 0.5f, 0.0f, 1.0f);
            } else {
                polyColor(0.5f, 0.5f, 0.5f, 1.0f);
            }

            polyFinish();

            polyBegin();

            polyConvexTranslate(-0.5f, 0.5f);
            polyConvexScale(1f / 32f, -1f / 32f);

            polyConvexColor(0.65f, 0.65f, 0.65f, 1f);
            polyConvex(
                    0, 31,
                    31, 31,
                    29, 29,
                    2, 29
            );
            polyConvex(
                    31, 31,
                    31, 0,
                    29, 2,
                    29, 29
            );
            polyConvex(
                    2, 2,
                    29, 2,
                    31, 0,
                    0, 0
            );
            polyConvex(
                    0, 31,
                    2, 29,
                    2, 2,
                    0, 0
            );

            if (this.gpuOcclusionCulling) {
                polyConvexColor(0f, 0.65f, 0f, 1f);
                polyConvex(
                        12, 24,
                        27, 10,
                        24, 7,
                        12, 19
                );
                polyConvex(
                        4, 18,
                        12, 24,
                        12, 19,
                        7, 15
                );
            }

            polyTranslate(-0.95f, 0.72f);
            polyScale(0.1f, 0.1f);

            this.hoveringGPUOcclusionCulling = polyTestAabbPoint(Main.MOUSE_X, Main.MOUSE_Y);

            polyFinish();

            UIRenderer.drawText("GPU Occlusion Culling", 0.05f, 0.845f, 0.032f);
        }

        polyDraw();

        glDisable(GL_MULTISAMPLE);

        UIRenderer.render();
    }

    public void windowSizeUpdated(int width, int height) {
        this.camera.updateAspectRatio(width, height);
    }

    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        this.camera.keyCallback(window, key, scancode, action, mods);
        if (!this.camera.isMovementEnabled()) {
            this.controller.keyCallback(window, key, scancode, action, mods);
        }
        if (key == GLFW_KEY_F1 && action == GLFW_PRESS) {
            try {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("chunks.obj"), StandardCharsets.UTF_8))) {
                    int indexOffset = 0;
                    for (int i = 0; i < this.worldCamera.amountOfRenderableChunks(); i++) {
                        RenderableChunk chunk = this.worldCamera.getRenderableChunk(i);

                        if (chunk == null) {
                            continue;
                        }

                        double chunkX = chunk.getChunk().getChunkX() * Chunk.CHUNK_SIZE;
                        double chunkZ = chunk.getChunk().getChunkZ() * Chunk.CHUNK_SIZE;

                        float relativeX = (float) (chunkX - this.camera.getPosition().x());
                        float relativeZ = (float) (chunkZ - this.camera.getPosition().z());

                        float[] vertices = chunk.vertices();
                        int[] indices = chunk.indices();

                        if (vertices == null || indices == null) {
                            continue;
                        }

                        for (int j = 0; j < vertices.length; j += VerticesStream.VERTEX_SIZE) {
                            float x = vertices[j + 0];
                            float y = vertices[j + 1];
                            float z = vertices[j + 2];
                            float nx = vertices[j + 3];
                            float ny = vertices[j + 4];
                            float nz = vertices[j + 5];
                            float u = vertices[j + 6];
                            float v = vertices[j + 7];

                            x += relativeX;
                            y += -this.camera.getPosition().y();
                            z += relativeZ;

                            writer.write("v " + x + " " + y + " " + z);
                            writer.newLine();

                            writer.write("vt " + u + " " + v);
                            writer.newLine();

                            writer.write("vn " + nx + " " + ny + " " + nz);
                            writer.newLine();
                        }

                        for (int j = 0; j < indices.length; j += 3) {
                            int i0 = indices[j + 0] + indexOffset + 1;
                            int i1 = indices[j + 1] + indexOffset + 1;
                            int i2 = indices[j + 2] + indexOffset + 1;

                            writer.write("f " + i0 + "/" + i0 + "/" + i0 + " " + i1 + "/" + i1 + "/" + i1 + " " + i2 + "/" + i2 + "/" + i2);
                            writer.newLine();
                        }

                        indexOffset += vertices.length / VerticesStream.VERTEX_SIZE;
                    }
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        if (key == GLFW_KEY_F2 && action == GLFW_PRESS) {
            this.wireframeEnabled = !this.wireframeEnabled;
        }
        if (key == GLFW_KEY_F3 && action == GLFW_PRESS) {
            this.camera.setMovementEnabled(!this.camera.isMovementEnabled());
            if (!this.camera.isMovementEnabled()) {
                float x = this.camera.getPosition().x();
                float y = this.camera.getPosition().y() - (this.controller.getPlayerPhysics().getCurrentHeight() + PlayerController.EYE_HEIGHT);
                float z = this.camera.getPosition().z();
                this.controller.getPlayerPhysics().getPosition().set(
                        x,
                        y,
                        z
                );
            }
        }
        if (key == GLFW_KEY_F4 && action == GLFW_PRESS) {
            this.playerWireframeEnabled = !this.playerWireframeEnabled;
        }
        if (key == GLFW_KEY_F5 && action == GLFW_PRESS && Main.isSupported(3, 3)) {
            Vector3fc camPos = this.camera.getPosition();
            SmokeSpawner s = new SmokeSpawner(camPos.x(), camPos.y(), camPos.z(), 5f);
            this.smokeSpawner.add(s);
        }
        if (key == GLFW_KEY_F6 && action == GLFW_PRESS && Main.isSupported(3, 3)) {
            this.smokeSpawner.clear();
        }
        if (key == GLFW_KEY_R && action == GLFW_PRESS) {
            this.currentBlock = Blocks.getBlock(this.currentBlock.getId() + 1);
            if (this.currentBlock == Blocks.AIR) {
                this.currentBlock = Blocks.getBlock(1);
            }
        }
    }

    public void cursorPosCallback(long window, double posx, double posy) {
        this.camera.cursorPosCallback(window, posx, posy);
    }

    public void mouseButtonCallback(long window, int button, int action, int mods) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS && this.camera.isCapturingMouse()) {
            if (this.outline.getBlock() != Blocks.AIR && this.outline.getBlockY() > 0) {
                this.world.setBlockOrNull(
                        Blocks.AIR,
                        this.outline.getBlockX(),
                        this.outline.getBlockY(),
                        this.outline.getBlockZ()
                );
            }
        }
        if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS && this.camera.isCapturingMouse()) {
            if (this.outline.getBlock() != Blocks.AIR && this.outline.canPlaceBlockAtSide()) {
                this.world.setBlockOrNull(
                        this.currentBlock,
                        this.outline.getSideBlockX(),
                        this.outline.getSideBlockY(),
                        this.outline.getSideBlockZ()
                );
            }
        }
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS && this.hoveringIncreaseViewDistance && !this.camera.isCapturingMouse()) {
            int viewDistance = this.worldCamera.getViewDistance() + 1;
            if (viewDistance > 10) {
                viewDistance = 10;
            }
            this.worldCamera.scheduleViewDistanceUpdate(viewDistance);
        }
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS && this.hoveringDecreaseViewDistance && !this.camera.isCapturingMouse()) {
            int viewDistance = this.worldCamera.getViewDistance() - 1;
            if (viewDistance < 0) {
                viewDistance = 0;
            }
            this.worldCamera.scheduleViewDistanceUpdate(viewDistance);
        }
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS && this.hoveringGPUOcclusionCulling && !this.camera.isCapturingMouse()) {
            this.gpuOcclusionCulling = !this.gpuOcclusionCulling;
        }
    }

}
