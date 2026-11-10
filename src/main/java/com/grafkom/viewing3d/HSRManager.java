package com.grafkom.viewing3d;

import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.*;

/**
 * Praktikum 2: Kelas HSRManager - Hidden Surface Removal Setup (Indikator 3).
 *
 * Mengelola dua mekanisme utama Visible Surface Determination / Hidden Surface Removal (HSR):
 * 1. Algoritma Z-Buffer (Depth-Buffer Test) pada tingkat fragment/pixel.
 * 2. Back-Face Culling berdasarkan orientasi putaran vertex (CCW Winding Order) dan N . V.
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
public class HSRManager {

    private boolean depthTestEnabled = true;
    private boolean backFaceCullingEnabled = true;

    public static void setupDepthTest(boolean enable) {
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS); // Pixel lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    public static void setupBackFaceCulling(boolean enable) {
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);  // Buang polygon belakang
            glFrontFace(GL_CCW);  // Counter-Clockwise adalah tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }

    public void applyCurrentState() {
        setupDepthTest(depthTestEnabled);
        setupBackFaceCulling(backFaceCullingEnabled);
    }

    public void toggleDepthTest() {
        this.depthTestEnabled = !this.depthTestEnabled;
        setupDepthTest(this.depthTestEnabled);
    }

    public void toggleBackFaceCulling() {
        this.backFaceCullingEnabled = !this.backFaceCullingEnabled;
        setupBackFaceCulling(this.backFaceCullingEnabled);
    }

    /**
     * Evaluasi matematis Back-Face Culling:
     * Polygon di-cull (dibuang) apabila dot product antara normal permukaan (N)
     * dan vektor arah pandang dari kamera ke permukaan (V_ray = P_frag - E) bernilai >= 0.
     */
    public static boolean isBackFaceCulled(Vector3f faceNormal, Vector3f cameraToSurfaceRay) {
        Vector3f n = new Vector3f(faceNormal).normalize();
        Vector3f v = new Vector3f(cameraToSurfaceRay).normalize();
        return n.dot(v) >= 0.0f;
    }

    /**
     * Evaluasi matematis Algoritma Z-Buffer (GL_LESS):
     * Fragment baru dengan kedalaman zNew lolos uji apabila zNew < zBufferCurrent.
     */
    public static boolean passesZBufferTest(float zNew, float zBufferCurrent) {
        return zNew < zBufferCurrent;
    }

    public boolean isDepthTestEnabled() {
        return depthTestEnabled;
    }

    public void setDepthTestEnabled(boolean depthTestEnabled) {
        this.depthTestEnabled = depthTestEnabled;
        setupDepthTest(depthTestEnabled);
    }

    public boolean isBackFaceCullingEnabled() {
        return backFaceCullingEnabled;
    }

    public void setBackFaceCullingEnabled(boolean backFaceCullingEnabled) {
        this.backFaceCullingEnabled = backFaceCullingEnabled;
        setupBackFaceCulling(backFaceCullingEnabled);
    }
}
