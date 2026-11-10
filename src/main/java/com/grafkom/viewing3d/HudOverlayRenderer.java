package com.grafkom.viewing3d;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Kelas HudOverlayRenderer berbasis Modern OpenGL 3.3 Core Profile (GLSL VAO/VBO)
 * untuk menampilkan panel telemetri 2D real-time 5 Indikator Visual Realism
 * serta jendela pratinjau langsung (Live Preview) Shadow Map Depth Buffer 2048x2048.
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
public class HudOverlayRenderer {

    private static final Map<Character, int[]> FONT_5X7 = new HashMap<>();

    static {
        putGlyph('A', 0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001);
        putGlyph('B', 0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110);
        putGlyph('C', 0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110);
        putGlyph('D', 0b11100, 0b10010, 0b10001, 0b10001, 0b10001, 0b10010, 0b11100);
        putGlyph('E', 0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111);
        putGlyph('F', 0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000);
        putGlyph('G', 0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01110);
        putGlyph('H', 0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001);
        putGlyph('I', 0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110);
        putGlyph('J', 0b00111, 0b00010, 0b00010, 0b00010, 0b10010, 0b10010, 0b01100);
        putGlyph('K', 0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001);
        putGlyph('L', 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111);
        putGlyph('M', 0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001);
        putGlyph('N', 0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001);
        putGlyph('O', 0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110);
        putGlyph('P', 0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000);
        putGlyph('Q', 0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101);
        putGlyph('R', 0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001);
        putGlyph('S', 0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110);
        putGlyph('T', 0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100);
        putGlyph('U', 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110);
        putGlyph('V', 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100);
        putGlyph('W', 0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b11011, 0b10001);
        putGlyph('X', 0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001);
        putGlyph('Y', 0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100);
        putGlyph('Z', 0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111);

        putGlyph('0', 0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110);
        putGlyph('1', 0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110);
        putGlyph('2', 0b01110, 0b10001, 0b00001, 0b00110, 0b01000, 0b10000, 0b11111);
        putGlyph('3', 0b11110, 0b00001, 0b00001, 0b01110, 0b00001, 0b00001, 0b11110);
        putGlyph('4', 0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010);
        putGlyph('5', 0b11111, 0b10000, 0b10000, 0b11110, 0b00001, 0b00001, 0b11110);
        putGlyph('6', 0b01110, 0b10000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110);
        putGlyph('7', 0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000);
        putGlyph('8', 0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110);
        putGlyph('9', 0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00001, 0b01110);

        putGlyph(' ', 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000);
        putGlyph(':', 0b00000, 0b00100, 0b00100, 0b00000, 0b00100, 0b00100, 0b00000);
        putGlyph(';', 0b00000, 0b00100, 0b00100, 0b00000, 0b00100, 0b00100, 0b01000);
        putGlyph('\'', 0b00100, 0b00100, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000);
        putGlyph('.', 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00100, 0b00100);
        putGlyph(',', 0b00000, 0b00000, 0b00000, 0b00000, 0b00100, 0b00100, 0b01000);
        putGlyph('-', 0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000);
        putGlyph('+', 0b00000, 0b00100, 0b00100, 0b11111, 0b00100, 0b00100, 0b00000);
        putGlyph('/', 0b00001, 0b00010, 0b00010, 0b00100, 0b01000, 0b01000, 0b10000);
        putGlyph('(', 0b00010, 0b00100, 0b01000, 0b01000, 0b01000, 0b00100, 0b00010);
        putGlyph(')', 0b01000, 0b00100, 0b00010, 0b00010, 0b00010, 0b00100, 0b01000);
        putGlyph('[', 0b01110, 0b01000, 0b01000, 0b01000, 0b01000, 0b01000, 0b01110);
        putGlyph(']', 0b01110, 0b00010, 0b00010, 0b00010, 0b00010, 0b00010, 0b01110);
        putGlyph('=', 0b00000, 0b00000, 0b11111, 0b00000, 0b11111, 0b00000, 0b00000);
        putGlyph('>', 0b10000, 0b01000, 0b00100, 0b00010, 0b00100, 0b01000, 0b10000);
        putGlyph('<', 0b00001, 0b00010, 0b00100, 0b01000, 0b00100, 0b00010, 0b00001);
        putGlyph('_', 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b11111);
        putGlyph('|', 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100);
        putGlyph('%', 0b11001, 0b11010, 0b00010, 0b00100, 0b01000, 0b01011, 0b10011);
        putGlyph('*', 0b00000, 0b00100, 0b10101, 0b01110, 0b10101, 0b00100, 0b00000);
        putGlyph('&', 0b01100, 0b10010, 0b10100, 0b01000, 0b10101, 0b10010, 0b01101);
        putGlyph('°', 0b00110, 0b01001, 0b01001, 0b00110, 0b00000, 0b00000, 0b00000);
    }

    private static void putGlyph(char c, int... rows) {
        FONT_5X7.put(c, rows);
    }

    private ShaderProgram hudShader;
    private int vaoId;
    private int vboId;
    private final List<Float> vertexBatch = new ArrayList<>(32768);
    private boolean visible = true;

    private String studentName = "ANDREAN LUKMAN HAKIM";
    private String studentNim = "25050974059";

    public void init() {
        hudShader = new ShaderProgram("/shaders/hud.vert", "/shaders/hud.frag");
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        int stride = 8 * Float.BYTES;
        // loc 0: vec2 pos
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        // loc 1: vec4 color
        glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);
        // loc 2: vec2 texCoord
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void setStudentIdentity(String name, String nim) {
        this.studentName = name.toUpperCase(Locale.ROOT);
        this.studentNim = nim;
    }

    public void renderHud(
            int width,
            int height,
            boolean useBlinnPhong,
            boolean useFlatShading,
            float shininess,
            HSRManager hsr,
            boolean useTexture,
            String textureName,
            boolean bilinearMipmap,
            ShadowMapFBO shadowFbo,
            Camera3D camera,
            Vector3f lightPos,
            Mesh3D mesh,
            float fps) {

        if (!visible) {
            return;
        }

        boolean prevDepth = glIsEnabled(GL_DEPTH_TEST);
        boolean prevCull = glIsEnabled(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Matrix4f ortho = new Matrix4f().ortho(0.0f, width, height, 0.0f, -1.0f, 1.0f);
        hudShader.use();
        hudShader.setMat4("orthoProjection", ortho);
        hudShader.setBool("useDepthTexture", false);

        vertexBatch.clear();

        float scale = Math.max(1.12f, Math.min(1.42f, (width - 220.0f) / 600.0f));
        float lineStep = 10.6f * scale;

        String line1 = "3D VISUAL REALISM SUITE - MINGGU 11 (JAVA OOP & GLSL 330 CORE)";
        String line2 = String.format(Locale.US, "MAHASISWA : %s | NIM : %s | FPS : %.0f",
                studentName, studentNim, fps);
        String line3 = String.format(Locale.US,
                "[IND 1] SHADING MODEL [B] : %s | SHININESS(N)=%.0f",
                useBlinnPhong ? "BLINN-PHONG (HALFWAY H = L+V)" : "STANDARD PHONG (REFLECT R)",
                shininess);
        String line4 = "[IND 2] INTERPOLATION [F] : " + (useFlatShading
                ? "FLAT SHADING (PER-FACE NORMAL / MACH BAND)"
                : "SMOOTH SHADING (VERTEX NORMAL AVERAGING)");
        String line5 = String.format(Locale.US,
                "[IND 3] HSR PIPELINE [Z/C]: Z-BUFFER DEPTH=%s | BACK-FACE CULLING=%s",
                hsr.isDepthTestEnabled() ? "ON (GL_LESS)" : "OFF (OVERLAP!)",
                hsr.isBackFaceCullingEnabled() ? "ON (CCW)" : "OFF");
        String line6 = String.format(Locale.US,
                "[IND 4] TEXTURE MAP [T/M] : %s | FILTER=%s",
                useTexture ? "ON (" + textureName + ")" : "OFF (BASE MATERIAL COLOR)",
                bilinearMipmap ? "BILINEAR MIPMAP (GL_LINEAR_MIPMAP_LINEAR)" : "NEAREST (GL_NEAREST BLOCKY)");
        String line7 = String.format(Locale.US,
                "[IND 5] SHADOW MAP 2-PASS : %s (2048X2048 FBO) | BIAS=[%.4f, %.4f]",
                shadowFbo.isShadowsEnabled() ? "ON (3X3 PCF + SLOPE BIAS)" : "OFF",
                shadowFbo.getShadowBiasMin(), shadowFbo.getShadowBiasMax());
        Vector3f eye = camera.getEye();
        String line8 = String.format(Locale.US,
                "SCENE & LIGHT [V / L]     : EYE(%.1f,%.1f,%.1f) | LIGHT(%.1f,%.1f,%.1f) | %s",
                eye.x, eye.y, eye.z, lightPos.x, lightPos.y, lightPos.z,
                mesh.getActivePreset().getLabel());

        String[] lines = {line1, line2, line3, line4, line5, line6, line7, line8};
        int maxChars = 0;
        for (String s : lines) {
            if (s.length() > maxChars) {
                maxChars = s.length();
            }
        }

        float panelX = 12.0f;
        float panelY = 12.0f;
        float panelW = Math.min(width - 190.0f, maxChars * 6.0f * scale + 24.0f);
        float panelH = lines.length * lineStep + 18.0f;

        addPanel(panelX, panelY, panelW, panelH,
                0.05f, 0.08f, 0.13f, 0.86f,
                0.28f, 0.65f, 0.96f, 0.94f);

        float tx = panelX + 12.0f;
        float ty = panelY + 10.0f;
        addText(line1, tx, ty, scale, 0.40f, 0.86f, 1.0f); ty += lineStep;
        addText(line2, tx, ty, scale, 0.98f, 0.86f, 0.35f); ty += lineStep;
        addText(line3, tx, ty, scale, 0.46f, 0.96f, 0.60f); ty += lineStep;
        addText(line4, tx, ty, scale, 0.96f, 0.78f, 0.42f); ty += lineStep;
        addText(line5, tx, ty, scale,
                hsr.isDepthTestEnabled() ? 0.46f : 1.0f,
                hsr.isDepthTestEnabled() ? 0.96f : 0.45f,
                0.60f);
        ty += lineStep;
        addText(line6, tx, ty, scale, 0.84f, 0.92f, 0.98f); ty += lineStep;
        addText(line7, tx, ty, scale, 0.46f, 0.96f, 0.60f); ty += lineStep;
        addText(line8, tx, ty, scale, 0.78f, 0.86f, 0.94f);

        // Panel Kontrol Bawah
        String bLine1 = "SHADING & HSR  [B] PHONG / BLINN-PHONG   [F] FLAT / SMOOTH SHADING   [Z] Z-BUFFER DEPTH TEST   [C] BACK-FACE CULLING   [+/-] SHININESS";
        String bLine2 = "TEKSTUR & FBO  [T] TOGGLE/CYCLE TEXTURE  [M] NEAREST / BILINEAR MIPMAP FILTER   [X] SHADOW MAPPING ON/OFF   [[ / ]] TUNING SHADOW BIAS";
        String bLine3 = "KAMERA & SCENE [MOUSE DRAG/SCROLL] ORBIT/ZOOM   [L] ORBIT CAHAYA   [V] GANTI SCENE 3D   [1-6] TAKSONOMI PROYEKSI   [W] WIREFRAME   [SPACE] RESET";

        int maxBottomChars = Math.max(bLine1.length(), Math.max(bLine2.length(), bLine3.length()));
        float barW = width - 24.0f;
        float barScale = Math.max(0.95f, Math.min(1.36f, (barW - 24.0f) / (maxBottomChars * 6.0f)));
        float barLineStep = 10.6f * barScale;
        float barH = 3.0f * barLineStep + 14.0f;
        float barY = height - barH - 10.0f;

        addPanel(12.0f, barY, barW, barH,
                0.05f, 0.08f, 0.13f, 0.86f,
                0.28f, 0.65f, 0.96f, 0.94f);

        float bx = 22.0f;
        float by = barY + 8.0f;
        addText(bLine1, bx, by, barScale, 0.98f, 0.88f, 0.42f); by += barLineStep;
        addText(bLine2, bx, by, barScale, 0.84f, 0.92f, 0.98f); by += barLineStep;
        addText(bLine3, bx, by, barScale, 0.72f, 0.95f, 0.78f);

        // Frame border untuk Live Preview Shadow Depth Map di Kanan Atas
        float mapSize = Math.min(148.0f, width * 0.16f);
        float mapX = width - mapSize - 14.0f;
        float mapY = 12.0f;
        addPanel(mapX - 4.0f, mapY - 4.0f, mapSize + 8.0f, mapSize + 24.0f,
                0.05f, 0.08f, 0.13f, 0.88f,
                0.28f, 0.65f, 0.96f, 0.94f);
        addText("PASS 1 DEPTH FBO", mapX + 4.0f, mapY + mapSize + 4.0f, 1.15f, 0.98f, 0.88f, 0.42f);

        flushBatch();

        // Gambar tekstur Shadow Depth FBO di dalam kotak kanan atas
        vertexBatch.clear();
        addTexturedQuad(mapX, mapY, mapSize, mapSize);
        hudShader.setBool("useDepthTexture", true);
        hudShader.setInt("depthTex", 1);
        shadowFbo.bindDepthTexture(1);
        flushBatch();
        hudShader.setBool("useDepthTexture", false);

        if (prevDepth) glEnable(GL_DEPTH_TEST);
        if (prevCull) glEnable(GL_CULL_FACE);
    }

    private void addPanel(
            float x, float y, float w, float h,
            float bgR, float bgG, float bgB, float bgA,
            float brR, float brG, float brB, float brA) {
        addQuad(x, y, w, h, bgR, bgG, bgB, bgA);
        float t = 1.6f;
        addQuad(x, y, w, t, brR, brG, brB, brA);
        addQuad(x, y + h - t, w, t, brR, brG, brB, brA);
        addQuad(x, y, t, h, brR, brG, brB, brA);
        addQuad(x + w - t, y, t, h, brR, brG, brB, brA);
    }

    private void addText(String text, float x, float y, float pixelScale, float r, float g, float b) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String upper = text.toUpperCase(Locale.ROOT);
        float cursorX = x;
        float charAdvance = 6.0f * pixelScale;

        for (int i = 0; i < upper.length(); i++) {
            int[] glyph = FONT_5X7.get(upper.charAt(i));
            if (glyph != null) {
                for (int row = 0; row < 7; row++) {
                    int rowBits = glyph[row];
                    for (int col = 0; col < 5; col++) {
                        if (((rowBits >> (4 - col)) & 1) != 0) {
                            addQuad(cursorX + col * pixelScale,
                                    y + row * pixelScale,
                                    pixelScale,
                                    pixelScale,
                                    r, g, b, 1.0f);
                        }
                    }
                }
            }
            cursorX += charAdvance;
        }
    }

    private void addQuad(float x, float y, float w, float h, float r, float g, float b, float a) {
        pushHudVertex(x, y, r, g, b, a, 0.0f, 0.0f);
        pushHudVertex(x + w, y, r, g, b, a, 1.0f, 0.0f);
        pushHudVertex(x + w, y + h, r, g, b, a, 1.0f, 1.0f);

        pushHudVertex(x, y, r, g, b, a, 0.0f, 0.0f);
        pushHudVertex(x + w, y + h, r, g, b, a, 1.0f, 1.0f);
        pushHudVertex(x, y + h, r, g, b, a, 0.0f, 1.0f);
    }

    private void addTexturedQuad(float x, float y, float w, float h) {
        pushHudVertex(x, y, 1, 1, 1, 1, 0.0f, 1.0f);
        pushHudVertex(x + w, y, 1, 1, 1, 1, 1.0f, 1.0f);
        pushHudVertex(x + w, y + h, 1, 1, 1, 1, 1.0f, 0.0f);

        pushHudVertex(x, y, 1, 1, 1, 1, 0.0f, 1.0f);
        pushHudVertex(x + w, y + h, 1, 1, 1, 1, 1.0f, 0.0f);
        pushHudVertex(x, y + h, 1, 1, 1, 1, 0.0f, 0.0f);
    }

    private void pushHudVertex(float x, float y, float r, float g, float b, float a, float u, float v) {
        vertexBatch.add(x);
        vertexBatch.add(y);
        vertexBatch.add(r);
        vertexBatch.add(g);
        vertexBatch.add(b);
        vertexBatch.add(a);
        vertexBatch.add(u);
        vertexBatch.add(v);
    }

    private void flushBatch() {
        if (vertexBatch.isEmpty()) {
            return;
        }
        FloatBuffer buf = BufferUtils.createFloatBuffer(vertexBatch.size());
        for (Float f : vertexBatch) {
            buf.put(f);
        }
        buf.flip();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STREAM_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, vertexBatch.size() / 8);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void toggleVisible() {
        this.visible = !this.visible;
    }

    public void cleanup() {
        if (hudShader != null) {
            hudShader.cleanup();
        }
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }
}
