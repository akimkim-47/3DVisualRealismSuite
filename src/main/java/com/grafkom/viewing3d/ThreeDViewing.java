package com.grafkom.viewing3d;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Main Class: ThreeDViewing - 3D Visual Realism Suite (Minggu 11).
 *
 * Mengintegrasikan 5 Pilar Utama Rendering Visual Realism (Java OOP & Modern OpenGL GLSL 330 Core):
 * 1. Indikator 1: Shading Models (Phong vs Blinn-Phong Reflection Model)        [Tombol 'B']
 * 2. Indikator 2: Flat Shading (Per-Face) vs Smooth Shading (Vertex Averaging)  [Tombol 'F']
 * 3. Indikator 3: Hidden Surface Removal (Z-Buffer Depth Test & Back-Face Cull) [Tombol 'Z' & 'C']
 * 4. Indikator 4: Texture Mapping to Faces (UV Mapping, Nearest vs Mipmap)      [Tombol 'T' & 'M']
 * 5. Indikator 5: Real-Time 2-Pass Shadow Mapping & 2048x2048 Depth FBO         [Tombol 'X' & '[' ']']
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
public class ThreeDViewing {

    private static final String STUDENT_NAME = "Andrean Lukman Hakim";
    private static final String STUDENT_NIM = "25050974059";

    private long window;
    private int windowWidth = 1280;
    private int windowHeight = 800;

    // Komponen OOP Minggu 10 & Minggu 11
    private Camera3D camera;
    private PerspectiveProjection perspectiveProjection;
    private ProjectionTaxonomyManager taxonomyManager;
    private HSRManager hsrManager;
    private ShadowMapFBO shadowMapFBO;
    private ShaderProgram phongShader;
    private ShaderProgram shadowDepthShader;
    private Mesh3D mesh3D;
    private HudOverlayRenderer hudRenderer;

    // Koleksi Tekstur 2D (Indikator 4)
    private Texture2D brickTexture;
    private Texture2D checkerTexture;
    private Texture2D floorTileTexture;
    private int activeTextureIndex = 0; // 0 = Brick Wall, 1 = Checkerboard UV

    // Parameter Visual Realism (Indikator 1 - 5)
    private boolean useBlinnPhong = true;
    private boolean useFlatShading = false;
    private boolean useTexture = true;
    private boolean bilinearMipmap = true;
    private float ambientStrength = 0.22f;
    private float specularStrength = 0.85f;
    private float shininess = 32.0f;

    // Posisi Sumber Cahaya & Animasi
    private final Vector3f lightPos = new Vector3f(4.2f, 5.2f, 4.4f);
    private final Vector3f lightColor = new Vector3f(1.0f, 0.98f, 0.94f);
    private float lightOrbitAngleDeg = 42.0f;
    private boolean autoOrbitLight = false;
    private boolean autoRotateObjects = false;
    private float objectAnimAngleDeg = 0.0f;

    // Interaksi Mouse & FPS Counter
    private boolean leftMouseDragging = false;
    private boolean rightMouseDragging = false;
    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;
    private double lastFrameTime = 0.0;
    private float currentFps = 60.0f;

    private String screenshotOutputDir = null;

    public ThreeDViewing() {
        float initialAspect = (float) windowWidth / (float) windowHeight;
        this.camera = new Camera3D(
                new Vector3f(4.4f, 3.1f, 5.8f),
                new Vector3f(0.0f, -0.15f, 0.0f),
                new Vector3f(0.0f, 1.0f, 0.0f)
        );
        this.perspectiveProjection = new PerspectiveProjection(45.0f, initialAspect, 0.15f, 50.0f);
        this.taxonomyManager = new ProjectionTaxonomyManager();
        this.hsrManager = new HSRManager();
        this.mesh3D = new Mesh3D();
        this.hudRenderer = new HudOverlayRenderer();
        this.hudRenderer.setStudentIdentity(STUDENT_NAME, STUDENT_NIM);

        this.camera.updateOrbitalPosition(6.8f, 32.0f, 24.0f);
        updateLightPositionFromAngle();
    }

    public void run(String[] args) {
        parseArguments(args);
        printStartupVerificationReport();

        initWindowAndOpenGL();
        initVisualRealismResources();

        if (screenshotOutputDir != null) {
            runAutomatedScreenshotSequence(screenshotOutputDir);
        } else {
            loop();
        }
        cleanup();
    }

    private void parseArguments(String[] args) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            if ("--capture-screenshots".equals(args[i]) && i + 1 < args.length) {
                screenshotOutputDir = args[i + 1];
            }
        }
    }

    public void printStartupVerificationReport() {
        System.out.println("================================================================================");
        System.out.println("  MODUL 11 GRAFIKA KOMPUTER: 3D VISUAL REALISM SUITE (JAVA OOP & GLSL 330 CORE)");
        System.out.println("  Mahasiswa : " + STUDENT_NAME + " | NIM : " + STUDENT_NIM);
        System.out.println("  Dosen     : Drs. Bambang Sujatmiko, M.T.");
        System.out.println("================================================================================");

        Vector3f sampleN = new Vector3f(0.0f, 0.85f, 0.52f).normalize();
        Vector3f sampleL = new Vector3f(0.45f, 0.75f, 0.48f).normalize();
        Vector3f sampleV = new Vector3f(0.35f, 0.65f, 0.67f).normalize();
        float specPhong = ShaderProgram.evaluatePhongSpecular(sampleN, sampleL, sampleV, 16.0f);
        float specBlinn = ShaderProgram.evaluateBlinnPhongSpecular(sampleN, sampleL, sampleV, 32.0f);

        System.out.println("\n[INDIKATOR 1] VERIFIKASI SHADING MODELS (PHONG VS BLINN-PHONG):");
        System.out.printf(Locale.US, "  Standard Phong Specular (R . V)^16  = %.5f%n", specPhong);
        System.out.printf(Locale.US, "  Blinn-Phong Specular    (N . H)^32  = %.5f%n", specBlinn);

        Vector3f v0 = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f v1 = new Vector3f(1.0f, 0.0f, 0.0f);
        Vector3f v2 = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f fn = Mesh3D.calculateFaceNormal(v0, v1, v2);
        System.out.println("\n[INDIKATOR 2] VERIFIKASI FLAT SHADING VS SMOOTH SHADING:");
        System.out.printf(Locale.US, "  Face Normal (Flat Shading)          = (%.3f, %.3f, %.3f)%n", fn.x, fn.y, fn.z);

        System.out.println("\n[INDIKATOR 3] VERIFIKASI HIDDEN SURFACE REMOVAL (HSR):");
        System.out.printf(Locale.US, "  Z-Buffer Depth Test = %s (GL_LESS) | Back-Face Culling = %s (GL_CCW)%n",
                hsrManager.isDepthTestEnabled() ? "ENABLED" : "DISABLED",
                hsrManager.isBackFaceCullingEnabled() ? "ENABLED" : "DISABLED");

        System.out.println("\n[INDIKATOR 4] VERIFIKASI TEXTURE MAPPING & PERSPECTIVE-CORRECT UV:");
        float uLinear = 0.5f;
        float uPersp = Texture2D.perspectiveCorrectInterpolateUV(0.0f, 2.0f, 1.0f, 6.0f, 0.5f);
        System.out.printf(Locale.US, "  Midpoint UV (Linear Screen = %.4f vs Perspective-Correct = %.4f)%n", uLinear, uPersp);

        System.out.println("\n[INDIKATOR 5] VERIFIKASI SHADOW MAPPING 2-PASS & SLOPE-SCALED BIAS:");
        float dynamicBias = ShadowMapFBO.calculateDynamicShadowBias(sampleN, sampleL, 0.0025f, 0.015f);
        System.out.printf(Locale.US, "  Shadow Map Resolution = 2048x2048 FBO | Slope-Scaled Bias = %.5f%n", dynamicBias);
        System.out.println("================================================================================\n");
    }

    private void initWindowAndOpenGL() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW!");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);
        if (screenshotOutputDir != null) {
            glfwWindowHint(GLFW_FLOATING, GLFW_TRUE);
        }

        window = glfwCreateWindow(windowWidth, windowHeight, buildWindowTitle(), NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Gagal membuat jendela OpenGL 3.3 Core Profile!");
        }

        setupInputCallbacks();

        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            glfwSetWindowPos(
                    window,
                    (vidMode.width() - windowWidth) / 2,
                    (vidMode.height() - windowHeight) / 2
            );
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();
        glClearColor(0.07f, 0.09f, 0.14f, 1.0f);

        int[] fbW = new int[1];
        int[] fbH = new int[1];
        glfwGetFramebufferSize(window, fbW, fbH);
        if (fbW[0] > 0 && fbH[0] > 0) {
            windowWidth = fbW[0];
            windowHeight = fbH[0];
            glViewport(0, 0, windowWidth, windowHeight);
            perspectiveProjection.setAspectRatio((float) windowWidth / (float) windowHeight);
        }
    }

    private void initVisualRealismResources() {
        // 1. Inisialisasi GLSL Shaders (#version 330 core)
        phongShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        shadowDepthShader = new ShaderProgram("/shaders/shadow_depth.vert", "/shaders/shadow_depth.frag");

        // 2. Inisialisasi HSR (Z-Buffer & Back-Face Culling)
        hsrManager.applyCurrentState();

        // 3. Buat & Muat Tekstur PNG menggunakan STBImage (Praktikum 3)
        File texDir = new File("src/main/resources/textures");
        Texture2D.ensureDefaultTexturesExist(texDir);
        brickTexture = new Texture2D(new File(texDir, "brick_wall.png").getPath());
        checkerTexture = new Texture2D(new File(texDir, "checkerboard_uv.png").getPath());
        floorTileTexture = new Texture2D(new File(texDir, "floor_tiles.png").getPath());

        // 4. Inisialisasi Shadow Map 2048x2048 Depth FBO (Praktikum 4)
        shadowMapFBO = new ShadowMapFBO();

        // 5. Bangun VAO/VBO Mesh 3D Bermateri & HUD 2D
        mesh3D.initGpuMeshes();
        hudRenderer.init();
    }

    private void setupInputCallbacks() {
        glfwSetFramebufferSizeCallback(window, (win, width, height) -> {
            if (width > 0 && height > 0) {
                windowWidth = width;
                windowHeight = height;
                glViewport(0, 0, width, height);
                perspectiveProjection.setAspectRatio((float) width / (float) height);
            }
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS && action != GLFW_REPEAT) {
                return;
            }

            switch (key) {
                case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(win, true);

                // Indikator 1: Key 'B' - Switch Phong vs Blinn-Phong
                case GLFW_KEY_B -> {
                    useBlinnPhong = !useBlinnPhong;
                    logStatus("Shading Model [Indikator 1]: "
                            + (useBlinnPhong ? "Blinn-Phong (Halfway Vector H)" : "Standard Phong (Reflection Vector R)"));
                }

                // Indikator 2: Key 'F' - Toggle Flat Shading vs Smooth Shading
                case GLFW_KEY_F -> {
                    useFlatShading = !useFlatShading;
                    logStatus("Surface Normal Interpolation [Indikator 2]: "
                            + (useFlatShading ? "Flat Shading (Per-Face Normal)" : "Smooth Shading (Vertex Normal Averaging)"));
                }

                // Indikator 3: Key 'Z' & 'C' - Z-Buffer Depth Test & Back-Face Culling
                case GLFW_KEY_Z -> {
                    hsrManager.toggleDepthTest();
                    logStatus("Z-Buffer Depth Test [Indikator 3]: "
                            + (hsrManager.isDepthTestEnabled() ? "ON (GL_LESS)" : "OFF"));
                }
                case GLFW_KEY_C -> {
                    hsrManager.toggleBackFaceCulling();
                    logStatus("Back-Face Culling [Indikator 3]: "
                            + (hsrManager.isBackFaceCullingEnabled() ? "ON (GL_BACK, GL_CCW)" : "OFF"));
                }

                // Indikator 4: Key 'T' & 'M' - Texture Mapping & Filtering Mode
                case GLFW_KEY_T -> {
                    if (!useTexture) {
                        useTexture = true;
                        activeTextureIndex = 0;
                    } else if (activeTextureIndex == 0) {
                        activeTextureIndex = 1;
                    } else {
                        useTexture = false;
                    }
                    logStatus("Texture Mapping [Indikator 4]: "
                            + (useTexture ? "ON (" + getActiveTextureName() + ")" : "OFF"));
                }
                case GLFW_KEY_M -> {
                    bilinearMipmap = !bilinearMipmap;
                    brickTexture.setBilinearMipmap(bilinearMipmap);
                    checkerTexture.setBilinearMipmap(bilinearMipmap);
                    floorTileTexture.setBilinearMipmap(bilinearMipmap);
                    logStatus("Texture Filtering [Indikator 4]: "
                            + (bilinearMipmap ? "Bilinear Mipmapped (GL_LINEAR_MIPMAP_LINEAR)" : "Nearest Neighbor (GL_NEAREST)"));
                }

                // Indikator 5: Key 'X' & '[' ']' - Real-Time Shadow Mapping & Shadow Bias Tuning
                case GLFW_KEY_X -> {
                    shadowMapFBO.toggleShadowsEnabled();
                    logStatus("Shadow Mapping 2-Pass [Indikator 5]: "
                            + (shadowMapFBO.isShadowsEnabled() ? "ON (2048x2048 FBO)" : "OFF"));
                }
                case GLFW_KEY_LEFT_BRACKET -> {
                    shadowMapFBO.adjustShadowBias(-0.002f);
                    logStatus(String.format(Locale.US, "Shadow Bias: min=%.4f, max=%.4f",
                            shadowMapFBO.getShadowBiasMin(), shadowMapFBO.getShadowBiasMax()));
                }
                case GLFW_KEY_RIGHT_BRACKET -> {
                    shadowMapFBO.adjustShadowBias(0.002f);
                    logStatus(String.format(Locale.US, "Shadow Bias: min=%.4f, max=%.4f",
                            shadowMapFBO.getShadowBiasMin(), shadowMapFBO.getShadowBiasMax()));
                }

                // Pengaturan Shininess Specular (+/-)
                case GLFW_KEY_EQUAL, GLFW_KEY_KP_ADD -> {
                    shininess = Math.min(128.0f, shininess + 8.0f);
                    logStatus("Specular Shininess (n) = " + shininess);
                }
                case GLFW_KEY_MINUS, GLFW_KEY_KP_SUBTRACT -> {
                    shininess = Math.max(4.0f, shininess - 8.0f);
                    logStatus("Specular Shininess (n) = " + shininess);
                }

                // Kontrol Posisi Cahaya & Scene
                case GLFW_KEY_L -> autoOrbitLight = !autoOrbitLight;
                case GLFW_KEY_R -> autoRotateObjects = !autoRotateObjects;
                case GLFW_KEY_V -> {
                    mesh3D.cycleScenePreset();
                    logStatus("Scene Preset: " + mesh3D.getActivePreset().getLabel());
                }
                case GLFW_KEY_W -> mesh3D.toggleWireframeMode();
                case GLFW_KEY_H -> hudRenderer.toggleVisible();

                // Navigasi Kamera via Keyboard
                case GLFW_KEY_LEFT, GLFW_KEY_A -> camera.orbit(-4.0f, 0.0f);
                case GLFW_KEY_RIGHT, GLFW_KEY_D -> camera.orbit(4.0f, 0.0f);
                case GLFW_KEY_UP -> camera.orbit(0.0f, 3.5f);
                case GLFW_KEY_DOWN -> camera.orbit(0.0f, -3.5f);

                // Tombol 1-6 Taksonomi Proyeksi
                case GLFW_KEY_1 -> taxonomyManager.selectProjectionType(ProjectionTaxonomyManager.ProjectionType.ORTHOGRAPHIC, camera);
                case GLFW_KEY_2 -> taxonomyManager.selectProjectionType(ProjectionTaxonomyManager.ProjectionType.ISOMETRIC, camera);
                case GLFW_KEY_3 -> taxonomyManager.selectProjectionType(ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CAVALIER, camera);
                case GLFW_KEY_4 -> taxonomyManager.selectProjectionType(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_1POINT, camera);
                case GLFW_KEY_5 -> taxonomyManager.selectProjectionType(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_2POINT, camera);
                case GLFW_KEY_6 -> taxonomyManager.selectProjectionType(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3POINT, camera);

                case GLFW_KEY_SPACE -> resetAll();
            }
            glfwSetWindowTitle(window, buildWindowTitle());
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                leftMouseDragging = (action == GLFW_PRESS);
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                rightMouseDragging = (action == GLFW_PRESS);
            }
            double[] mx = new double[1];
            double[] my = new double[1];
            glfwGetCursorPos(win, mx, my);
            lastMouseX = mx[0];
            lastMouseY = my[0];
        });

        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            double dx = xpos - lastMouseX;
            double dy = ypos - lastMouseY;
            lastMouseX = xpos;
            lastMouseY = ypos;

            if (leftMouseDragging) {
                camera.orbit((float) (-dx * 0.35), (float) (dy * 0.35));
            } else if (rightMouseDragging) {
                camera.pan((float) (-dx * 0.012), (float) (dy * 0.012));
            }
        });

        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            camera.zoomOrbitalRadius((float) (-yoffset * 0.45));
        });
    }

    private void updateLightPositionFromAngle() {
        float rad = (float) Math.toRadians(lightOrbitAngleDeg);
        float radius = 5.8f;
        lightPos.set(
                radius * (float) Math.cos(rad),
                5.1f,
                radius * (float) Math.sin(rad)
        );
    }

    private Texture2D getActiveObjectTexture() {
        return (activeTextureIndex == 0) ? brickTexture : checkerTexture;
    }

    private String getActiveTextureName() {
        return (activeTextureIndex == 0) ? "BRICK_WALL.PNG" : "CHECKERBOARD_UV.PNG";
    }

    private void resetAll() {
        useBlinnPhong = true;
        useFlatShading = false;
        useTexture = true;
        activeTextureIndex = 0;
        bilinearMipmap = true;
        brickTexture.setBilinearMipmap(true);
        checkerTexture.setBilinearMipmap(true);
        floorTileTexture.setBilinearMipmap(true);
        hsrManager.setDepthTestEnabled(true);
        hsrManager.setBackFaceCullingEnabled(true);
        shadowMapFBO.setShadowsEnabled(true);
        shadowMapFBO.resetShadowBias();
        shininess = 32.0f;
        lightOrbitAngleDeg = 42.0f;
        updateLightPositionFromAngle();
        autoOrbitLight = false;
        autoRotateObjects = false;
        objectAnimAngleDeg = 0.0f;
        camera.reset(6.8f, 32.0f, 24.0f);
        logStatus("Seluruh konfigurasi Visual Realism telah di-reset ke kondisi awal.");
    }

    private void logStatus(String msg) {
        System.out.println("[VISUAL REALISM] " + msg);
    }

    private String buildWindowTitle() {
        return String.format(Locale.US,
                "3D Visual Realism Suite - %s %s | [%s] [%s] [Shadow: %s]",
                STUDENT_NIM,
                STUDENT_NAME,
                useBlinnPhong ? "Blinn-Phong" : "Phong",
                useFlatShading ? "Flat" : "Smooth",
                shadowMapFBO != null && shadowMapFBO.isShadowsEnabled() ? "ON" : "OFF"
        );
    }

    private void loop() {
        lastFrameTime = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) Math.max(0.0001, now - lastFrameTime);
            lastFrameTime = now;
            currentFps = 0.9f * currentFps + 0.1f * (1.0f / dt);

            if (autoOrbitLight) {
                lightOrbitAngleDeg = (lightOrbitAngleDeg + dt * 28.0f) % 360.0f;
                updateLightPositionFromAngle();
            }
            if (autoRotateObjects) {
                objectAnimAngleDeg = (objectAnimAngleDeg + dt * 24.0f) % 360.0f;
            }

            renderFrame();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderFrame() {
        Matrix4f lightSpaceMatrix = ShadowMapFBO.calculateLightSpaceMatrix(
                lightPos,
                new Vector3f(0.0f, -0.5f, 0.0f)
        );

        // =========================================================================
        // RENDER PASS 1: Shadow Mapping Depth Pass (dari Sudut Pandang Cahaya)
        // =========================================================================
        HSRManager.setupDepthTest(true);
        HSRManager.setupBackFaceCulling(false);
        shadowMapFBO.bindForWriting();

        shadowDepthShader.use();
        shadowDepthShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        mesh3D.renderSceneObjects(
                shadowDepthShader,
                useFlatShading,
                false,
                null,
                null,
                objectAnimAngleDeg
        );

        shadowMapFBO.unbind(windowWidth, windowHeight);

        // =========================================================================
        // RENDER PASS 2: Lighting, Texture, & Shadow Pass (dari Sudut Pandang Kamera)
        // =========================================================================
        hsrManager.applyCurrentState();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix4f viewMatrix = camera.calculateLookAtMatrix();
        Matrix4f projMatrix = taxonomyManager.calculateActiveProjectionMatrix(
                perspectiveProjection,
                camera.getRadius()
        );

        phongShader.use();
        phongShader.setMat4("view", viewMatrix);
        phongShader.setMat4("projection", projMatrix);
        phongShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);

        phongShader.setVec3("lightPos", lightPos);
        phongShader.setVec3("viewPos", camera.getEye());
        phongShader.setVec3("lightColor", lightColor);

        phongShader.setBool("useBlinnPhong", useBlinnPhong);
        phongShader.setBool("useFlatShading", useFlatShading);
        phongShader.setBool("useShadows", shadowMapFBO.isShadowsEnabled());
        phongShader.setBool("unlitMode", false);

        phongShader.setFloat("ambientStrength", ambientStrength);
        phongShader.setFloat("specularStrength", specularStrength);
        phongShader.setFloat("shininess", shininess);
        phongShader.setFloat("shadowBiasMin", shadowMapFBO.getShadowBiasMin());
        phongShader.setFloat("shadowBiasMax", shadowMapFBO.getShadowBiasMax());

        phongShader.setInt("textureSampler", 0);
        phongShader.setInt("shadowMap", 1);
        shadowMapFBO.bindDepthTexture(1);

        mesh3D.renderSceneObjects(
                phongShader,
                useFlatShading,
                useTexture,
                getActiveObjectTexture(),
                floorTileTexture,
                objectAnimAngleDeg
        );

        // Gambar penanda posisi sumber cahaya 3D
        mesh3D.renderLightSourceMarker(phongShader, lightPos);

        // Render Panel Informasi HUD 2D & Preview Shadow Depth Map FBO
        hudRenderer.renderHud(
                windowWidth,
                windowHeight,
                useBlinnPhong,
                useFlatShading,
                shininess,
                hsrManager,
                useTexture,
                getActiveTextureName(),
                bilinearMipmap,
                shadowMapFBO,
                camera,
                lightPos,
                mesh3D,
                currentFps
        );
    }

    /**
     * Sekuens otomatis tangkapan layar (screenshot) untuk kelima indikator penilaian pada Laporan Praktikum:
     * (a) Phong vs Blinn-Phong Shading
     * (b) Flat vs Smooth Shading
     * (c) HSR (Z-Buffer & Back-Face Culling)
     * (d) Texture Mapping & Filtering (Checkerboard UV & Brick Mipmap)
     * (e) Real-Time 2-Pass Shadow Mapping dengan Depth FBO
     */
    private void runAutomatedScreenshotSequence(String outDir) {
        File dir = new File(outDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // (a) Indikator 1: Blinn-Phong vs Phong Specular Highlight pada Sphere & Torus Lab
        resetAll();
        mesh3D.setActivePreset(Mesh3D.ScenePreset.SHADING_SPHERE_TORUS_LAB);
        useTexture = false;
        useBlinnPhong = true;
        useFlatShading = false;
        shininess = 48.0f;
        camera.updateOrbitalPosition(5.6f, 24.0f, 20.0f);
        renderAndWarmup(4);
        captureFramebufferToFile(new File(dir, "Screenshot_1_Indikator1_BlinnPhong_Shading.png"));

        useBlinnPhong = false;
        renderAndWarmup(4);
        captureFramebufferToFile(new File(dir, "Screenshot_2_Indikator1_StandardPhong_Shading.png"));

        // (b) Indikator 2: Flat Shading (Per-Face Facet Normal / Mach Band) vs Smooth Shading
        useBlinnPhong = true;
        useFlatShading = true;
        renderAndWarmup(4);
        captureFramebufferToFile(new File(dir, "Screenshot_3_Indikator2_Flat_Shading_Faceted.png"));

        // (c) Indikator 3: HSR (Z-Buffer OFF overlap artifact vs Z-Buffer & Back-Face Culling ON)
        resetAll();
        mesh3D.setActivePreset(Mesh3D.ScenePreset.COMPLETE_REALISM_SUITE);
        hsrManager.setDepthTestEnabled(false);
        renderAndWarmup(4);
        captureFramebufferToFile(new File(dir, "Screenshot_4_Indikator3_HSR_ZBuffer_Off_Overlap.png"));

        hsrManager.setDepthTestEnabled(true);
        hsrManager.setBackFaceCullingEnabled(true);

        // (d) Indikator 4: Texture Mapping UV (Checkerboard UV & Brick Wall Mipmapped)
        activeTextureIndex = 1; // Checkerboard UV
        useTexture = true;
        bilinearMipmap = true;
        renderAndWarmup(4);
        captureFramebufferToFile(new File(dir, "Screenshot_5_Indikator4_Texture_UV_Mapping.png"));

        // (e) Indikator 5: Real-Time 2-Pass Shadow Mapping & Complete Visual Realism Suite
        activeTextureIndex = 0; // Brick Wall
        useTexture = true;
        shadowMapFBO.setShadowsEnabled(true);
        camera.updateOrbitalPosition(6.8f, 32.0f, 24.0f);
        renderAndWarmup(4);
        captureFramebufferToFile(new File(dir, "Screenshot_6_Indikator5_Shadow_Mapping_Complete_Suite.png"));
    }

    private void renderAndWarmup(int frames) {
        for (int i = 0; i < frames; i++) {
            renderFrame();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void captureFramebufferToFile(File outputFile) {
        glReadBuffer(GL_BACK);
        ByteBuffer pixels = BufferUtils.createByteBuffer(windowWidth * windowHeight * 4);
        glReadPixels(0, 0, windowWidth, windowHeight, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        BufferedImage image = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < windowHeight; y++) {
            for (int x = 0; x < windowWidth; x++) {
                int idx = ((windowHeight - 1 - y) * windowWidth + x) * 4;
                int r = pixels.get(idx) & 0xFF;
                int g = pixels.get(idx + 1) & 0xFF;
                int b = pixels.get(idx + 2) & 0xFF;
                image.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        try {
            ImageIO.write(image, "PNG", outputFile);
            System.out.println("[SCREENSHOT SAVED] " + outputFile.getAbsolutePath());
        } catch (IOException ex) {
            System.err.println("Gagal menyimpan screenshot: " + ex.getMessage());
        }
    }

    private void cleanup() {
        if (mesh3D != null) mesh3D.cleanup();
        if (hudRenderer != null) hudRenderer.cleanup();
        if (phongShader != null) phongShader.cleanup();
        if (shadowDepthShader != null) shadowDepthShader.cleanup();
        if (shadowMapFBO != null) shadowMapFBO.cleanup();
        if (brickTexture != null) brickTexture.cleanup();
        if (checkerTexture != null) checkerTexture.cleanup();
        if (floorTileTexture != null) floorTileTexture.cleanup();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) {
            cb.free();
        }
    }

    public static void main(String[] args) {
        new ThreeDViewing().run(args);
    }
}
