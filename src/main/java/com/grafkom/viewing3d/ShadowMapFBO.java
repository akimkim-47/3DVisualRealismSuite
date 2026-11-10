package com.grafkom.viewing3d;

import java.nio.ByteBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Praktikum 4: Kelas ShadowMapFBO - Shadow Mapping Depth FBO Manager (Indikator 5).
 *
 * Mengelola Framebuffer Object (FBO) khusus beresolusi 2048x2048 untuk menyimpan
 * tekstur kedalaman (Depth Map / Shadow Map) pada Render Pass 1 dari sudut pandang sumber cahaya
 * (Light Space), serta menghitung matriks transformasi Light Space (P_light * V_light)
 * dan Slope-Scaled Shadow Bias untuk mencegah artefak Shadow Acne maupun Peter Panning.
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
public class ShadowMapFBO {

    private int fboID;
    private int depthTextureID;
    private final int SHADOW_WIDTH = 2048, SHADOW_HEIGHT = 2048;

    private float shadowBiasMin = 0.0025f;
    private float shadowBiasMax = 0.0150f;
    private boolean shadowsEnabled = true;

    public ShadowMapFBO() {
        fboID = glGenFramebuffers();
        depthTextureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, SHADOW_WIDTH, SHADOW_HEIGHT,
                0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureID, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bindForWriting() {
        glViewport(0, 0, SHADOW_WIDTH, SHADOW_HEIGHT);
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    public void bindDepthTexture(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
    }

    public int getDepthTextureID() {
        return depthTextureID;
    }

    public int getShadowWidth() {
        return SHADOW_WIDTH;
    }

    public int getShadowHeight() {
        return SHADOW_HEIGHT;
    }

    /**
     * Menghitung matriks transformasi Light Space (M_light = P_ortho_light * V_lookAt_light)
     * untuk memetakan koordinat dunia 3D ke dalam ruang kamera sumber cahaya pada Pass 1 & Pass 2.
     */
    public static Matrix4f calculateLightSpaceMatrix(Vector3f lightPos, Vector3f sceneCenter) {
        float orthoSize = 7.5f;
        Matrix4f lightProjection = new Matrix4f().ortho(
                -orthoSize, orthoSize,
                -orthoSize, orthoSize,
                1.0f, 24.0f
        );
        Matrix4f lightView = new Matrix4f().lookAt(
                lightPos,
                sceneCenter,
                new Vector3f(0.0f, 1.0f, 0.0f)
        );
        return lightProjection.mul(lightView);
    }

    /**
     * Menghitung Slope-Scaled Shadow Bias sesuai Modul Bagian I (5.b):
     *   bias = max(biasMax * (1.0 - max(N . L, 0.0)), biasMin)
     */
    public static float calculateDynamicShadowBias(
            Vector3f surfaceNormal,
            Vector3f lightDir,
            float biasMin,
            float biasMax) {
        Vector3f n = new Vector3f(surfaceNormal).normalize();
        Vector3f l = new Vector3f(lightDir).normalize();
        float ndotl = Math.max(n.dot(l), 0.0f);
        return Math.max(biasMax * (1.0f - ndotl), biasMin);
    }

    public float getShadowBiasMin() {
        return shadowBiasMin;
    }

    public float getShadowBiasMax() {
        return shadowBiasMax;
    }

    public void adjustShadowBias(float delta) {
        this.shadowBiasMin = Math.max(0.0f, Math.min(0.08f, this.shadowBiasMin + delta * 0.25f));
        this.shadowBiasMax = Math.max(0.0f, Math.min(0.15f, this.shadowBiasMax + delta));
    }

    public void resetShadowBias() {
        this.shadowBiasMin = 0.0025f;
        this.shadowBiasMax = 0.0150f;
    }

    public boolean isShadowsEnabled() {
        return shadowsEnabled;
    }

    public void toggleShadowsEnabled() {
        this.shadowsEnabled = !this.shadowsEnabled;
    }

    public void setShadowsEnabled(boolean shadowsEnabled) {
        this.shadowsEnabled = shadowsEnabled;
    }

    public void cleanup() {
        glDeleteFramebuffers(fboID);
        glDeleteTextures(depthTextureID);
    }
}
