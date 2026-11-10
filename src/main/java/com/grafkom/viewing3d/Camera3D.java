package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Praktikum 1: Kelas Encapsulated Camera3D & LookAt Matrix (Indikator 1).
 *
 * Mengimplementasikan Synthetic Camera Model dalam ruang 3D (World Coordinate System -> Viewing Coordinate System)
 * dengan perhitungan manual basis ortonormal (u, v, n) dan matriks transformasi LookAt (M_view),
 * serta mendukung dua pola navigasi kamera:
 * 1. Orbital Camera (Turntable): Kamera berputar mengelilingi titik pusat target (At).
 * 2. First-Person Camera (FPS): Posisi mata (Eye) bergerak bebas dengan orientasi sudut Euler (Pitch, Yaw, Roll).
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
public class Camera3D {

    public enum NavigationMode {
        ORBITAL("Orbital Camera (Turntable)"),
        FPS("First-Person Camera (FPS / Euler)");

        private final String label;

        NavigationMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // Parameter Utama Synthetic Camera Model (WCS)
    private Vector3f eye;
    private Vector3f target;
    private Vector3f up;

    // Vektor Basis Viewing Coordinate System (VCS: u = Right, v = True Up, n = View Plane Normal)
    private final Vector3f u = new Vector3f(1.0f, 0.0f, 0.0f);
    private final Vector3f v = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Vector3f n = new Vector3f(0.0f, 0.0f, 1.0f);

    // Parameter Koordinat Sferis untuk Kamera Orbital
    private float radius;
    private float azimuthDeg;
    private float elevationDeg;

    // Parameter Sudut Euler untuk Kamera First-Person (FPS)
    private float yawDeg;
    private float pitchDeg;
    private float rollDeg;

    // Mode Navigasi Aktif
    private NavigationMode navigationMode = NavigationMode.ORBITAL;

    public Camera3D(Vector3f eye, Vector3f target, Vector3f up) {
        this.eye = new Vector3f(eye);
        this.target = new Vector3f(target);
        this.up = new Vector3f(up);
        syncSphericalFromVectors();
        syncEulerFromVectors();
        calculateLookAtMatrix();
    }

    /**
     * Perhitungan manual Matriks LookAt (View Matrix) sesuai rumus Modul Bagian I (1.2 & 1.3) dan Praktikum 1:
     *   n = (E - At) / ||E - At||  (View Plane Normal, menunjuk berlawanan arah pandang)
     *   u = (Up x n) / ||Up x n||  (Right / Side Vector)
     *   v = n x u                  (True Up Vector)
     *
     * Matriks M_view mentranslasikan posisi kamera E ke (0,0,0) lalu merotasikan basis dunia ke basis (u, v, n).
     */
    public Matrix4f calculateLookAtMatrix() {
        // 1. Hitung Vektor Normal Bidang Pandang (View Plane Normal / n)
        eye.sub(target, n);
        if (n.lengthSquared() < 1e-8f) {
            n.set(0.0f, 0.0f, 1.0f);
        } else {
            n.normalize();
        }

        // Hitung vektor Up efektif apabila terdapat orientasi kemiringan (Roll)
        Vector3f effectiveUp = new Vector3f(up);
        if (Math.abs(rollDeg) > 1e-4f) {
            effectiveUp.rotateAxis((float) Math.toRadians(rollDeg), n.x, n.y, n.z);
        }

        // 2. Hitung Vektor Kanan Kamera (Side / Right Vector / u)
        effectiveUp.cross(n, u);
        if (u.lengthSquared() < 1e-8f) {
            // Penanganan singularitas saat arah pandang sejajar dengan sumbu Up
            Vector3f fallbackUp = new Vector3f(0.0f, 0.0f, (n.y >= 0.0f) ? -1.0f : 1.0f);
            fallbackUp.cross(n, u);
        }
        u.normalize();

        // 3. Hitung Vektor Atas Sebenarnya (True Up Vector / v)
        n.cross(u, v).normalize();

        // 4. Konstruksi manual Matriks LookAt (Column-Major untuk OpenGL / JOML)
        Matrix4f viewMatrix = new Matrix4f();
        viewMatrix.m00(u.x).m10(u.y).m20(u.z).m30(-u.dot(eye));
        viewMatrix.m01(v.x).m11(v.y).m21(v.z).m31(-v.dot(eye));
        viewMatrix.m02(n.x).m12(n.y).m22(n.z).m32(-n.dot(eye));
        viewMatrix.m03(0.0f).m13(0.0f).m23(0.0f).m33(1.0f);
        return viewMatrix;
    }

    /**
     * Update koordinat sferis untuk kamera orbital sesuai Praktikum 1.
     */
    public void updateOrbitalPosition(float radius, float azimuthDeg, float elevationDeg) {
        this.radius = Math.max(1.2f, Math.min(40.0f, radius));
        this.azimuthDeg = ((azimuthDeg % 360.0f) + 360.0f) % 360.0f;
        this.elevationDeg = Math.max(-89.0f, Math.min(89.0f, elevationDeg));

        float azRad = (float) Math.toRadians(this.azimuthDeg);
        float elRad = (float) Math.toRadians(this.elevationDeg);

        this.eye.x = this.target.x + this.radius * (float) (Math.cos(elRad) * Math.sin(azRad));
        this.eye.y = this.target.y + this.radius * (float) (Math.sin(elRad));
        this.eye.z = this.target.z + this.radius * (float) (Math.cos(elRad) * Math.cos(azRad));

        syncEulerFromVectors();
    }

    /**
     * Rotasi interaktif kamera orbital berdasarkan perubahan sudut Azimuth dan Elevation.
     */
    public void orbit(float deltaAzimuthDeg, float deltaElevationDeg) {
        updateOrbitalPosition(this.radius, this.azimuthDeg + deltaAzimuthDeg, this.elevationDeg + deltaElevationDeg);
    }

    /**
     * Mengubah jarak radius kamera orbital terhadap titik target (Zoom in / out).
     */
    public void zoomOrbitalRadius(float deltaRadius) {
        updateOrbitalPosition(this.radius + deltaRadius, this.azimuthDeg, this.elevationDeg);
    }

    /**
     * Menggeser posisi kamera dan titik target secara sejajar bidang pandang (Camera Panning).
     */
    public void pan(float deltaRight, float deltaUp) {
        calculateLookAtMatrix();
        Vector3f offset = new Vector3f();
        offset.fma(deltaRight, u);
        offset.fma(deltaUp, v);
        this.eye.add(offset);
        this.target.add(offset);
    }

    /**
     * Rotasi kamera First-Person (FPS) menggunakan sudut Euler (Yaw, Pitch).
     */
    public void rotateFPS(float deltaYawDeg, float deltaPitchDeg) {
        this.yawDeg = (this.yawDeg + deltaYawDeg) % 360.0f;
        this.pitchDeg = Math.max(-89.0f, Math.min(89.0f, this.pitchDeg + deltaPitchDeg));
        updateTargetFromEuler();
    }

    /**
     * Pergerakan bebas kamera First-Person (FPS) maju/mundur, kiri/kanan, atas/bawah.
     */
    public void moveFPS(float forwardDelta, float rightDelta, float upDelta) {
        calculateLookAtMatrix();
        Vector3f forward = new Vector3f(n).negate(); // Kamera melihat ke arah -n
        Vector3f displacement = new Vector3f();
        displacement.fma(forwardDelta, forward);
        displacement.fma(rightDelta, u);
        displacement.fma(upDelta, up);

        this.eye.add(displacement);
        this.target.add(displacement);
        syncSphericalFromVectors();
    }

    /**
     * Mengatur kemiringan kamera (Roll) dalam derajat.
     */
    public void adjustRoll(float deltaRollDeg) {
        this.rollDeg = Math.max(-90.0f, Math.min(90.0f, this.rollDeg + deltaRollDeg));
    }

    public void toggleNavigationMode() {
        if (this.navigationMode == NavigationMode.ORBITAL) {
            syncEulerFromVectors();
            this.navigationMode = NavigationMode.FPS;
        } else {
            syncSphericalFromVectors();
            this.navigationMode = NavigationMode.ORBITAL;
        }
    }

    public void reset(float defaultRadius, float defaultAzimuthDeg, float defaultElevationDeg) {
        this.target.set(0.0f, 0.0f, 0.0f);
        this.up.set(0.0f, 1.0f, 0.0f);
        this.rollDeg = 0.0f;
        this.navigationMode = NavigationMode.ORBITAL;
        updateOrbitalPosition(defaultRadius, defaultAzimuthDeg, defaultElevationDeg);
    }

    private void syncSphericalFromVectors() {
        Vector3f rel = new Vector3f();
        this.eye.sub(this.target, rel);
        this.radius = Math.max(1.2f, rel.length());
        this.elevationDeg = (float) Math.toDegrees(Math.asin(Math.max(-1.0f, Math.min(1.0f, rel.y / this.radius))));
        this.azimuthDeg = (float) Math.toDegrees(Math.atan2(rel.x, rel.z));
        if (this.azimuthDeg < 0.0f) {
            this.azimuthDeg += 360.0f;
        }
    }

    private void syncEulerFromVectors() {
        Vector3f dir = new Vector3f();
        this.target.sub(this.eye, dir);
        if (dir.lengthSquared() < 1e-8f) {
            dir.set(0.0f, 0.0f, -1.0f);
        } else {
            dir.normalize();
        }
        this.pitchDeg = (float) Math.toDegrees(Math.asin(Math.max(-1.0f, Math.min(1.0f, dir.y))));
        this.yawDeg = (float) Math.toDegrees(Math.atan2(dir.x, -dir.z));
    }

    private void updateTargetFromEuler() {
        float pitchRad = (float) Math.toRadians(this.pitchDeg);
        float yawRad = (float) Math.toRadians(this.yawDeg);

        Vector3f forward = new Vector3f(
                (float) (Math.cos(pitchRad) * Math.sin(yawRad)),
                (float) Math.sin(pitchRad),
                (float) (-Math.cos(pitchRad) * Math.cos(yawRad))
        ).normalize();

        float dist = Math.max(1.2f, this.radius);
        this.eye.fma(dist, forward, this.target);
        syncSphericalFromVectors();
    }

    // Getters & Setters Terenkapsulasi
    public Vector3f getEye() {
        return new Vector3f(eye);
    }

    public void setEye(Vector3f eye) {
        this.eye.set(eye);
        syncSphericalFromVectors();
        syncEulerFromVectors();
    }

    public Vector3f getTarget() {
        return new Vector3f(target);
    }

    public void setTarget(Vector3f target) {
        this.target.set(target);
        syncSphericalFromVectors();
        syncEulerFromVectors();
    }

    public Vector3f getUp() {
        return new Vector3f(up);
    }

    public void setUp(Vector3f up) {
        this.up.set(up).normalize();
    }

    public Vector3f getRightVectorU() {
        return new Vector3f(u);
    }

    public Vector3f getTrueUpVectorV() {
        return new Vector3f(v);
    }

    public Vector3f getViewPlaneNormalN() {
        return new Vector3f(n);
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        updateOrbitalPosition(radius, this.azimuthDeg, this.elevationDeg);
    }

    public float getAzimuthDeg() {
        return azimuthDeg;
    }

    public float getElevationDeg() {
        return elevationDeg;
    }

    public float getYawDeg() {
        return yawDeg;
    }

    public float getPitchDeg() {
        return pitchDeg;
    }

    public float getRollDeg() {
        return rollDeg;
    }

    public void setRollDeg(float rollDeg) {
        this.rollDeg = rollDeg;
    }

    public NavigationMode getNavigationMode() {
        return navigationMode;
    }

    public void setNavigationMode(NavigationMode navigationMode) {
        this.navigationMode = navigationMode;
    }
}
