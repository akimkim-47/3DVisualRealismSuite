package com.grafkom.viewing3d;

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;

/**
 * Praktikum 4: Kelas ProjectionTaxonomyManager (Indikator 4).
 *
 * Mengimplementasikan taksonomi proyeksi grafika komputer secara komprehensif (> 5 jenis proyeksi)
 * menggunakan Java Enum Switcher interaktif:
 * 1. ORTHOGRAPHIC       : Proyeksi Paralel Ortografik (Multiview Tampak Depan/Atas/Samping)
 * 2. ISOMETRIC          : Proyeksi Paralel Aksonometrik Isometrik (Elevasi 35.264°, Azimuth 45°)
 * 3. OBLIQUE_CAVALIER   : Proyeksi Paralel Miring / Oblique Cavalier (Shearing sumbu Z 45°, L = 1.0 / Cabinet L = 0.5)
 * 4. PERSPECTIVE_1POINT : Proyeksi Perspektif Linear 1 Titik Hilang (Sumbu Z konvergen ke pusat)
 * 5. PERSPECTIVE_2POINT : Proyeksi Perspektif Linear 2 Titik Hilang (Sumbu X & Z konvergen ke horizon)
 * 6. PERSPECTIVE_3POINT : Proyeksi Perspektif Linear 3 Titik Hilang (Sumbu X, Y, & Z konvergen ke 3 titik hilang)
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
public class ProjectionTaxonomyManager {

    public enum ProjectionType {
        ORTHOGRAPHIC(
                1,
                "1. Parallel Orthographic (Multiview)",
                "Paralel -> Ortografik (Proyektor Tegak Lurus Bidang Pandang)",
                0,
                0.0f,
                0.0f
        ),
        ISOMETRIC(
                2,
                "2. Parallel Axonometric: Isometric",
                "Paralel -> Aksonometrik -> Isometrik (RotX 35.264°, RotY 45°)",
                0,
                45.0f,
                35.26439f
        ),
        OBLIQUE_CAVALIER(
                3,
                "3. Parallel Oblique: Cavalier / Cabinet",
                "Paralel -> Oblique (Matriks Shearing Z Miring 45°)",
                0,
                0.0f,
                0.0f
        ),
        PERSPECTIVE_1POINT(
                4,
                "4. Perspective: 1-Point (1 Titik Hilang)",
                "Perspektif Linear -> 1 Titik Hilang (Sumbu Z Konvergen)",
                1,
                0.0f,
                0.0f
        ),
        PERSPECTIVE_2POINT(
                5,
                "5. Perspective: 2-Point (2 Titik Hilang)",
                "Perspektif Linear -> 2 Titik Hilang (Sumbu X & Z Konvergen)",
                2,
                45.0f,
                0.0f
        ),
        PERSPECTIVE_3POINT(
                6,
                "6. Perspective: 3-Point (3 Titik Hilang)",
                "Perspektif Linear -> 3 Titik Hilang (Sumbu X, Y, & Z Konvergen)",
                3,
                38.0f,
                28.0f
        );

        private final int keyNumber;
        private final String title;
        private final String taxonomyDescription;
        private final int vanishingPointCount;
        private final float canonicalAzimuthDeg;
        private final float canonicalElevationDeg;

        ProjectionType(
                int keyNumber,
                String title,
                String taxonomyDescription,
                int vanishingPointCount,
                float canonicalAzimuthDeg,
                float canonicalElevationDeg) {
            this.keyNumber = keyNumber;
            this.title = title;
            this.taxonomyDescription = taxonomyDescription;
            this.vanishingPointCount = vanishingPointCount;
            this.canonicalAzimuthDeg = canonicalAzimuthDeg;
            this.canonicalElevationDeg = canonicalElevationDeg;
        }

        public int getKeyNumber() {
            return keyNumber;
        }

        public String getTitle() {
            return title;
        }

        public String getTaxonomyDescription() {
            return taxonomyDescription;
        }

        public int getVanishingPointCount() {
            return vanishingPointCount;
        }

        public float getCanonicalAzimuthDeg() {
            return canonicalAzimuthDeg;
        }

        public float getCanonicalElevationDeg() {
            return canonicalElevationDeg;
        }

        public boolean isPerspective() {
            return vanishingPointCount > 0;
        }
    }

    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    // Parameter konfigurasi tambahan untuk proyeksi paralel & oblique
    private ProjectionType currentType = ProjectionType.PERSPECTIVE_3POINT;
    private float orthoHalfHeight = 3.2f;
    private float obliqueAngleDeg = 45.0f;
    private boolean obliqueCabinetMode = false; // false = Cavalier (L=1.0), true = Cabinet (L=0.5)

    /**
     * Metode statis standar sesuai Praktikum 4 untuk menerapkan matriks proyeksi berdasarkan ProjectionType.
     */
    public static void applyProjection(ProjectionType type, float aspect) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        switch (type) {
            case ORTHOGRAPHIC:
                glOrtho(-3.0 * aspect, 3.0 * aspect, -3.0, 3.0, -20.0, 20.0);
                break;
            case ISOMETRIC:
                glOrtho(-3.0 * aspect, 3.0 * aspect, -3.0, 3.0, -20.0, 20.0);
                glRotatef(35.264f, 1.0f, 0.0f, 0.0f);
                glRotatef(45.0f, 0.0f, 1.0f, 0.0f);
                break;
            case OBLIQUE_CAVALIER:
                glOrtho(-3.0 * aspect, 3.0 * aspect, -3.0, 3.0, -20.0, 20.0);
                // Terapkan matriks shearing Z miring 45 derajat (L = 1.0 untuk Cavalier)
                Matrix4f shear = calculateObliqueShearMatrix(45.0f, 1.0f, 0.0f);
                shear.get(MATRIX_BUFFER);
                glMultMatrixf(MATRIX_BUFFER);
                break;
            case PERSPECTIVE_1POINT:
            case PERSPECTIVE_2POINT:
            case PERSPECTIVE_3POINT:
                gluPerspective(45.0f, aspect, 0.1f, 50.0f);
                break;
        }
    }

    /**
     * Menerapkan proyeksi taksonomi secara terintegrasi dengan objek PerspectiveProjection dan Camera3D
     * sehingga mendukung tuning FOV, Frustum Clipping, Zoom Ortografik, serta Oblique Cavalier/Cabinet.
     */
    public void applyIntegratedProjection(
            PerspectiveProjection perspectiveProjection,
            Camera3D camera) {

        float aspect = perspectiveProjection.getAspectRatio();
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        switch (currentType) {
            case ORTHOGRAPHIC:
            case ISOMETRIC: {
                double halfH = orthoHalfHeight;
                double halfW = halfH * aspect;
                glOrtho(-halfW, halfW, -halfH, halfH, -30.0, 30.0);
                break;
            }
            case OBLIQUE_CAVALIER: {
                double halfH = orthoHalfHeight;
                double halfW = halfH * aspect;
                glOrtho(-halfW, halfW, -halfH, halfH, -30.0, 30.0);

                // Skala kedalaman L = 1.0 untuk Oblique Cavalier, L = 0.5 untuk Oblique Cabinet
                float depthScale = obliqueCabinetMode ? 0.5f : 1.0f;
                float pivotDepth = (camera != null) ? camera.getRadius() : 0.0f;
                Matrix4f shearMatrix = calculateObliqueShearMatrix(obliqueAngleDeg, depthScale, pivotDepth);
                shearMatrix.get(MATRIX_BUFFER);
                glMultMatrixf(MATRIX_BUFFER);
                break;
            }
            case PERSPECTIVE_1POINT:
            case PERSPECTIVE_2POINT:
            case PERSPECTIVE_3POINT: {
                Matrix4f projMatrix = perspectiveProjection.calculateProjectionMatrix();
                projMatrix.get(MATRIX_BUFFER);
                glLoadMatrixf(MATRIX_BUFFER);
                break;
            }
        }
    }

    /**
     * Menghitung matriks Shearing 4x4 untuk Proyeksi Paralel Oblique (Cavalier & Cabinet).
     *
     * Pada proyeksi Oblique, bidang proyeksi sejajar dengan bidang depan objek (XY),
     * sedangkan proyektor miring membentuk sudut terhadap bidang proyeksi sehingga sumbu Z mengalami pergeseran:
     *   x' = x + L * z * cos(alpha)
     *   y' = y + L * z * sin(alpha)
     * Dimana:
     *   - Oblique Cavalier: L = 1.0 (panjang sumbu Z tetap 100% skala penuh, alpha = 45°)
     *   - Oblique Cabinet : L = 0.5 (panjang sumbu Z direduksi 50% agar proporsi visual lebih alami)
     */
    public static Matrix4f calculateObliqueShearMatrix(float angleDeg, float depthScale, float pivotDistance) {
        float rad = (float) Math.toRadians(angleDeg);
        float shX = -depthScale * (float) Math.cos(rad);
        float shY = -depthScale * (float) Math.sin(rad);

        Matrix4f shear = new Matrix4f().identity();
        // Kolom ke-2 (sumbu Z) dimodifikasi untuk menggeser X dan Y proporsional terhadap Z
        shear.m20(shX);
        shear.m21(shY);
        // Kompensasi titik pusat objek yang berada pada jarak -pivotDistance di Eye Space
        shear.m30(shX * pivotDistance);
        shear.m31(shY * pivotDistance);
        return shear;
    }

    /**
     * Menghitung matriks proyeksi untuk pengujian unit (JUnit) atau inspeksi konsol.
     */
    public Matrix4f calculateActiveProjectionMatrix(PerspectiveProjection perspectiveProjection, float pivotDistance) {
        float aspect = perspectiveProjection.getAspectRatio();
        switch (currentType) {
            case ORTHOGRAPHIC:
                return new Matrix4f().ortho(
                        -orthoHalfHeight * aspect, orthoHalfHeight * aspect,
                        -orthoHalfHeight, orthoHalfHeight,
                        -30.0f, 30.0f
                );
            case ISOMETRIC:
                return new Matrix4f()
                        .ortho(-orthoHalfHeight * aspect, orthoHalfHeight * aspect,
                                -orthoHalfHeight, orthoHalfHeight, -30.0f, 30.0f)
                        .rotateX((float) Math.toRadians(35.26439f))
                        .rotateY((float) Math.toRadians(45.0f));
            case OBLIQUE_CAVALIER: {
                float depthScale = obliqueCabinetMode ? 0.5f : 1.0f;
                Matrix4f ortho = new Matrix4f().ortho(
                        -orthoHalfHeight * aspect, orthoHalfHeight * aspect,
                        -orthoHalfHeight, orthoHalfHeight,
                        -30.0f, 30.0f
                );
                Matrix4f shear = calculateObliqueShearMatrix(obliqueAngleDeg, depthScale, pivotDistance);
                return ortho.mul(shear);
            }
            case PERSPECTIVE_1POINT:
            case PERSPECTIVE_2POINT:
            case PERSPECTIVE_3POINT:
            default:
                return perspectiveProjection.calculateProjectionMatrix();
        }
    }

    /**
     * Implementasi kompatibilitas gluPerspective untuk LWJGL 3 menggunakan rumus matriks frustum perspektif.
     */
    public static void gluPerspective(float fovyDeg, float aspect, float zNear, float zFar) {
        PerspectiveProjection temp = new PerspectiveProjection(fovyDeg, aspect, zNear, zFar);
        Matrix4f mat = temp.calculateProjectionMatrix();
        mat.get(MATRIX_BUFFER);
        glMultMatrixf(MATRIX_BUFFER);
    }

    /**
     * Mengatur jenis proyeksi aktif dan menyelaraskan orientasi kamera ke sudut kanonik taksonomi tersebut.
     */
    public void selectProjectionType(ProjectionType type, Camera3D camera) {
        this.currentType = type;
        if (camera != null) {
            camera.setTarget(new org.joml.Vector3f(0.0f, 0.0f, 0.0f));
            camera.setRollDeg(0.0f);
            camera.setNavigationMode(Camera3D.NavigationMode.ORBITAL);
            camera.updateOrbitalPosition(
                    camera.getRadius(),
                    type.getCanonicalAzimuthDeg(),
                    type.getCanonicalElevationDeg()
            );
        }
    }

    public ProjectionType getCurrentType() {
        return currentType;
    }

    public float getOrthoHalfHeight() {
        return orthoHalfHeight;
    }

    public void setOrthoHalfHeight(float orthoHalfHeight) {
        this.orthoHalfHeight = Math.max(1.0f, Math.min(15.0f, orthoHalfHeight));
    }

    public void adjustOrthoZoom(float delta) {
        setOrthoHalfHeight(this.orthoHalfHeight + delta);
    }

    public float getObliqueAngleDeg() {
        return obliqueAngleDeg;
    }

    public void setObliqueAngleDeg(float obliqueAngleDeg) {
        this.obliqueAngleDeg = obliqueAngleDeg;
    }

    public void adjustObliqueAngle(float deltaDeg) {
        this.obliqueAngleDeg = ((this.obliqueAngleDeg + deltaDeg) % 360.0f + 360.0f) % 360.0f;
    }

    public boolean isObliqueCabinetMode() {
        return obliqueCabinetMode;
    }

    public void toggleObliqueCabinetMode() {
        this.obliqueCabinetMode = !this.obliqueCabinetMode;
    }
}
