package cientistavuador.leitecraft;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11C.GL_ONE;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.opengl.GL20.*;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author Cien
 */
public class Main {

    public static final String APPLICATION_NAME = "LeiteCraft";

    static {
        try {
            Path tempFolder = Path.of(System.getProperty("java.io.tmpdir"), APPLICATION_NAME + "-natives-dcda843a-96f2-4889-adf8-c7a57477d196");
            if (!Files.exists(tempFolder)) {
                Files.createDirectory(tempFolder);
            }

            try (ZipInputStream zipFile = new ZipInputStream(Main.class.getResourceAsStream("natives.zip"), StandardCharsets.UTF_8)) {
                ZipEntry e;
                while ((e = zipFile.getNextEntry()) != null) {
                    String fileName = e.getName();
                    byte[] fileData = zipFile.readAllBytes();

                    boolean shouldWrite = true;
                    Path currentFile = tempFolder.resolve(fileName);
                    if (Files.exists(currentFile)) {
                        byte[] currentFileData = Files.readAllBytes(currentFile);
                        if (Arrays.equals(fileData, currentFileData)) {
                            shouldWrite = false;
                        }
                    }

                    if (shouldWrite) {
                        Files.write(currentFile, fileData);
                    }
                }
            }

            org.lwjgl.system.Configuration.LIBRARY_PATH.set(tempFolder.toAbsolutePath().toString());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static long WINDOW_POINTER = NULL;
    public static int MAJOR_VERSION = 1;
    public static int MINOR_VERSION = 1;
    public static int WIDTH = 800;
    public static int HEIGHT = 600;
    public static double TPF = 1f / 60f;
    public static int FPS = 60;
    public static float MOUSE_X;
    public static float MOUSE_Y;
    public static final Queue<Runnable> MAIN_TASKS = new ConcurrentLinkedQueue<>();
    public static final Thread MAIN_THREAD = Thread.currentThread();
    public static final ExecutorService THREADS = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1, (r) -> {
        Thread t = new Thread(r, "game-auxiliar-thread");
        t.setDaemon(true);
        return t;
    });

    public static void checkError() {
        int error = glGetError();
        if (error != 0) {
            System.out.println("OpenGL Error " + error);
        }
    }

    public static boolean isSupported(int major, int minor) {
        if (MAJOR_VERSION > major) {
            return true;
        }
        if (MAJOR_VERSION < major) {
            return false;
        }
        return MINOR_VERSION >= minor;
    }

    public static <T> Future<T> execute(final Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable r = () -> {
            try {
                T value = supplier.get();
                future.complete(value);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        };
        if (Thread.currentThread() == MAIN_THREAD) {
            r.run();
        } else {
            MAIN_TASKS.add(r);
        }
        return future;
    }

    public static void main(String[] args) {
        if (!glfwInit()) {
            throw new RuntimeException("Could not initialize GLFW!");
        }

        glfwWindowHint(GLFW_SAMPLES, 8);

        WINDOW_POINTER = glfwCreateWindow(WIDTH, HEIGHT, APPLICATION_NAME, NULL, NULL);
        if (WINDOW_POINTER == NULL) {
            throw new RuntimeException("Could not create a OpenGL Window");
        }

        glfwMakeContextCurrent(WINDOW_POINTER);
        GL.createCapabilities();

        glfwSwapInterval(0);

        APIUtil.APIVersion version = APIUtil.apiParseVersion(glGetString(GL_VERSION));
        MAJOR_VERSION = version.major;
        MINOR_VERSION = version.minor;

        System.out.println("Running on OpenGL " + MAJOR_VERSION + "." + MINOR_VERSION);
        System.out.println(glGetString(GL_VERSION));
        System.out.println(glGetString(GL_VENDOR));

        glClearColor(0.2f, 0.4f, 0.6f, 1f);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        glEnable(GL_BLEND);
        glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
        glBlendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        
        glDisable(GL_MULTISAMPLE);

        glLineWidth(2f);

        glEnable(GL_DEPTH_TEST);

        Atlas.getInstance();
        Blocks.init();
        UIRenderer.init();
        ChunkShader.init();
        ConvexPolygonRenderer.polyStaticInit();
        GPUOcclusion.init();
        LineRenderer.init();

        if (Main.isSupported(3, 3)) {
            SmokeRenderer.init();
            SmokePass.init();
        }

        Game.getInstance();

        glfwSetFramebufferSizeCallback(WINDOW_POINTER, (window, width, height) -> {
            glViewport(0, 0, width, height);
            Main.WIDTH = width;
            Main.HEIGHT = height;
            Game.getInstance().windowSizeUpdated(width, height);
        });

        glfwSetKeyCallback(WINDOW_POINTER, (window, key, scancode, action, mods) -> {
            Game.getInstance().keyCallback(window, key, scancode, action, mods);
        });

        glfwSetCursorPosCallback(WINDOW_POINTER, (window, xpos, ypos) -> {
            Game.getInstance().cursorPosCallback(window, xpos, ypos);
        });

        glfwSetMouseButtonCallback(WINDOW_POINTER, (window, button, action, mods) -> {
            Game.getInstance().mouseButtonCallback(window, button, action, mods);
        });

        checkError();

        long nextFpsUpdate = System.currentTimeMillis() + 1000;
        int frames = 0;
        long last = System.nanoTime();
        while (!glfwWindowShouldClose(WINDOW_POINTER)) {
            long frameTime = System.nanoTime() - last;
            last = System.nanoTime();
            Main.TPF = frameTime / 1E9d;

            glClearColor(0.2f, 0.4f, 0.6f, 1f);
            glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                DoubleBuffer mouseX = stack.mallocDouble(1);
                DoubleBuffer mouseY = stack.mallocDouble(1);
                glfwGetCursorPos(Main.WINDOW_POINTER, mouseX, mouseY);
                double mX = mouseX.get();
                double mY = mouseY.get();
                mY = Main.HEIGHT - mY;
                mX /= Main.WIDTH;
                mY /= Main.HEIGHT;
                mX = (mX * 2.0) - 1.0;
                mY = (mY * 2.0) - 1.0;

                Main.MOUSE_X = (float) mX;
                Main.MOUSE_Y = (float) mY;
            }

            glfwPollEvents();

            Game.getInstance().loop();

            Runnable r;
            while ((r = MAIN_TASKS.poll()) != null) {
                r.run();
            }

            checkError();

            glfwSwapBuffers(WINDOW_POINTER);
            frames++;
            if (System.currentTimeMillis() >= nextFpsUpdate) {
                nextFpsUpdate = System.currentTimeMillis() + 1000;
                Main.FPS = frames;
                frames = 0;

                glfwSetWindowTitle(WINDOW_POINTER, APPLICATION_NAME + " - FPS: " + Main.FPS);
            }
        }
        glfwTerminate();
        System.exit(0);
    }
}
