package com.grafkom.viewing3d;

import org.joml.Matrix4f;

/**
 * Praktikum 2: Kelas PerspectiveProjection & Frustum Tuning (Indikator 2).
 *
 * Mengimplementasikan perhitungan eksplisit Viewing Frustum dan Matriks Proyeksi Perspektif:
 *   f = 1.0 / tan(FOV_y / 2)
 *   P[0,0] = f / Aspect
 *   P[1,1] = f
 *   P[2,2] = -(far + near) / (far - near)
 *   P[2,3] = -(2 * far * near) / (far - near)
 *   P[3,2] = -1
 *   P[3,3] = 0
 *
 * Dilengkapi fitur Frustum Clipping Tuning (zNear, zFar), koreksi Aspect Ratio dinamis,
 * serta eksperimen interaktif FOV / Dolly Zoom (Vertigo Effect).
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
public class PerspectiveProjection {

    private float fovDegree;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    // Parameter pengujian interaktif Dolly Zoom (Vertigo Effect)
    private boolean dollyZoomEnabled = false;
    private float referenceFovDegree = 45.0f;
    private float referenceDistance = 7.0f;

    public PerspectiveProjection(float fovDegree, float aspectRatio, float zNear, float zFar) {
        this.fovDegree = clamp(fovDegree, 10.0f, 120.0f);
        this.aspectRatio = Math.max(0.1f, aspectRatio);
        this.zNear = Math.max(0.05f, zNear);
        this.zFar = Math.max(this.zNear + 0.5f, zFar);
        this.referenceFovDegree = this.fovDegree;
    }

    /**
     * Perhitungan eksplisit Matriks Proyeksi Perspektif (Column-Major OpenGL) sesuai Praktikum 2.
     */
    public Matrix4f calculateProjectionMatrix() {
        float fovRad = (float) Math.toRadians(fovDegree);
        float f = 1.0f / (float) Math.tan(fovRad / 2.0f);

        Matrix4f proj = new Matrix4f();
        proj.zero();
        proj.m00(f / aspectRatio);
        proj.m11(f);
        proj.m22(-(zFar + zNear) / (zFar - zNear));
        proj.m32(-(2.0f * zFar * zNear) / (zFar - zNear));
        proj.m23(-1.0f);
        proj.m33(0.0f);
        return proj;
    }

    /**
     * Menghitung jarak kamera (radius) secara proporsional terhadap perubahan FOV
     * agar ukuran objek pada bidang fokus tetap konstan (Efek Dolly Zoom / Vertigo).
     * Rumus: d_baru = d_acuan * tan(FOV_acuan / 2) / tan(FOV_baru / 2)
     */
    public float calculateDollyZoomRadius() {
        float refHalfRad = (float) Math.toRadians(referenceFovDegree / 2.0f);
        float curHalfRad = (float) Math.toRadians(fovDegree / 2.0f);
        return referenceDistance * ((float) Math.tan(refHalfRad) / (float) Math.tan(curHalfRad));
    }

    /**
     * Menghitung dimensi bidang potong dekat (Near Clipping Plane: [left, right, bottom, top]).
     */
    public float[] getNearPlaneBounds() {
        float fovRad = (float) Math.toRadians(fovDegree);
        float top = zNear * (float) Math.tan(fovRad / 2.0f);
        float bottom = -top;
        float right = top * aspectRatio;
        float left = -right;
        return new float[]{left, right, bottom, top};
    }

    // Getter/Setter untuk pengujian interaktif FOV (Dolly Zoom) & Frustum Clipping
    public void setFovDegree(float fovDegree) {
        this.fovDegree = clamp(fovDegree, 10.0f, 120.0f);
    }

    public void adjustFovDegree(float deltaDegree) {
        setFovDegree(this.fovDegree + deltaDegree);
    }

    public float getFovDegree() {
        return fovDegree;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        if (aspectRatio > 0.05f) {
            this.aspectRatio = aspectRatio;
        }
    }

    public float getZNear() {
        return zNear;
    }

    public void setZNear(float zNear) {
        this.zNear = clamp(zNear, 0.05f, this.zFar - 0.5f);
    }

    public void adjustZNear(float delta) {
        setZNear(this.zNear + delta);
    }

    public float getZFar() {
        return zFar;
    }

    public void setZFar(float zFar) {
        this.zFar = clamp(zFar, this.zNear + 0.5f, 200.0f);
    }

    public void adjustZFar(float delta) {
        setZFar(this.zFar + delta);
    }

    public boolean isDollyZoomEnabled() {
        return dollyZoomEnabled;
    }

    public void setDollyZoomEnabled(boolean enabled, float currentCameraRadius) {
        this.dollyZoomEnabled = enabled;
        if (enabled) {
            this.referenceFovDegree = this.fovDegree;
            this.referenceDistance = currentCameraRadius;
        }
    }

    public void reset(float aspectRatio) {
        this.fovDegree = 45.0f;
        this.aspectRatio = Math.max(0.1f, aspectRatio);
        this.zNear = 0.1f;
        this.zFar = 50.0f;
        this.dollyZoomEnabled = false;
        this.referenceFovDegree = 45.0f;
        this.referenceDistance = 7.0f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
