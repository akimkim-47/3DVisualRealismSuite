package com.grafkom.viewing3d;

import java.io.File;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pengujian Unit JUnit 5 untuk Verifikasi 5 Indikator Penilaian Minggu 11 (Visual Realism Suite):
 * 1. Indikator 1: Shading Models (Phong vs Blinn-Phong Reflection Model & GLSL Shaders)
 * 2. Indikator 2: Flat Shading (Per-Face Normal) vs Smooth Shading (Vertex Normal Averaging)
 * 3. Indikator 3: Hidden Surface Removal (Z-Buffer Depth Test & Back-Face Culling)
 * 4. Indikator 4: Texture Mapping to Faces (Perspective-Correct UV Interpolation & Texture Assets)
 * 5. Indikator 5: Shadows of Objects (Light Space Matrix & Slope-Scaled Shadow Bias)
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
@DisplayName("Verifikasi Modul 11: 3D Visual Realism Suite (5 Indikator Penilaian)")
public class VisualRealismTest {

    private static final float EPSILON = 1e-4f;

    @Test
    @DisplayName("Indikator 1: Verifikasi Model Pencahayaan Phong vs Blinn-Phong & Berkas GLSL 330 Core")
    public void testShadingModelsPhongAndBlinnPhong() {
        Vector3f normal = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f lightDir = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f viewDir = new Vector3f(0.0f, 1.0f, 0.0f);

        // Saat N, L, dan V berimpit tegak lurus (sudut 0 derajat), specular Phong & Blinn-Phong = 1.0
        assertEquals(1.0f, ShaderProgram.evaluatePhongSpecular(normal, lightDir, viewDir, 16.0f), EPSILON);
        assertEquals(1.0f, ShaderProgram.evaluateBlinnPhongSpecular(normal, lightDir, viewDir, 32.0f), EPSILON);

        // Saat cahaya berada di belakang permukaan (N . L <= 0), komponen specular harus 0.0
        Vector3f backLight = new Vector3f(0.0f, -1.0f, 0.0f);
        assertEquals(0.0f, ShaderProgram.evaluatePhongSpecular(normal, backLight, viewDir, 16.0f), EPSILON);
        assertEquals(0.0f, ShaderProgram.evaluateBlinnPhongSpecular(normal, backLight, viewDir, 32.0f), EPSILON);

        // Pastikan berkas shader GLSL #version 330 core tersedia dan valid
        String vertSrc = ShaderProgram.loadShaderSource("/shaders/phong.vert");
        String fragSrc = ShaderProgram.loadShaderSource("/shaders/phong.frag");
        assertTrue(vertSrc.contains("#version 330 core"));
        assertTrue(fragSrc.contains("useBlinnPhong"));
        assertTrue(fragSrc.contains("useFlatShading"));
    }

    @Test
    @DisplayName("Indikator 2: Verifikasi Perhitungan Face Normal (Flat) & Vertex Normal Averaging (Smooth)")
    public void testFlatAndSmoothShadingNormals() {
        Vector3f v0 = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f v1 = new Vector3f(2.0f, 0.0f, 0.0f);
        Vector3f v2 = new Vector3f(0.0f, 2.0f, 0.0f);

        Vector3f faceNormal = Mesh3D.calculateFaceNormal(v0, v1, v2);
        assertEquals(0.0f, faceNormal.x, EPSILON);
        assertEquals(0.0f, faceNormal.y, EPSILON);
        assertEquals(1.0f, faceNormal.z, EPSILON);

        // Rata-rata normal dari 3 bidang yang bertemu di sudut kubus (+X, +Y, +Z)
        Vector3f avgNormal = Mesh3D.calculateVertexNormalAverage(
                new Vector3f(1.0f, 0.0f, 0.0f),
                new Vector3f(0.0f, 1.0f, 0.0f),
                new Vector3f(0.0f, 0.0f, 1.0f)
        );
        float invSqrt3 = (float) (1.0 / Math.sqrt(3.0));
        assertEquals(invSqrt3, avgNormal.x, EPSILON);
        assertEquals(invSqrt3, avgNormal.y, EPSILON);
        assertEquals(invSqrt3, avgNormal.z, EPSILON);
        assertEquals(1.0f, avgNormal.length(), EPSILON);
    }

    @Test
    @DisplayName("Indikator 3: Verifikasi Algoritma Z-Buffer Depth Test & Back-Face Culling")
    public void testHiddenSurfaceRemovalZBufferAndCulling() {
        // Uji Z-Buffer (GL_LESS): fragment lebih dekat (z = 0.35) harus mengalahkan z_buffer (0.75)
        assertTrue(HSRManager.passesZBufferTest(0.35f, 0.75f));
        assertFalse(HSRManager.passesZBufferTest(0.85f, 0.75f));

        // Uji Back-Face Culling: permukaan menghadap kamera (N . V_ray < 0) TIDAK di-cull
        Vector3f frontFaceNormal = new Vector3f(0.0f, 0.0f, 1.0f);
        Vector3f cameraToFragRay = new Vector3f(0.0f, 0.0f, -1.0f);
        assertFalse(HSRManager.isBackFaceCulled(frontFaceNormal, cameraToFragRay));

        // Permukaan membelakangi kamera (N . V_ray >= 0) HARUS di-cull
        Vector3f backFaceNormal = new Vector3f(0.0f, 0.0f, -1.0f);
        assertTrue(HSRManager.isBackFaceCulled(backFaceNormal, cameraToFragRay));
    }

    @Test
    @DisplayName("Indikator 4: Verifikasi Perspective-Correct UV Interpolation & Pembuatan Tekstur PNG")
    public void testPerspectiveCorrectUVAndTextures() {
        // Titik A dekat kamera (wA = 2.0, uA = 0.0) dan Titik B jauh dari kamera (wB = 6.0, uB = 1.0)
        // Pada titik tengah layar (s = 0.5), u_persp = (0 + 0.5/6) / (0.5/2 + 0.5/6) = (1/12) / (4/12) = 0.25
        float uPersp = Texture2D.perspectiveCorrectInterpolateUV(0.0f, 2.0f, 1.0f, 6.0f, 0.5f);
        assertEquals(0.25f, uPersp, EPSILON, "Interpolasi UV perspektif-korektif harus tepat 0.25");

        File tempTexDir = new File("src/main/resources/textures");
        Texture2D.ensureDefaultTexturesExist(tempTexDir);
        assertTrue(new File(tempTexDir, "brick_wall.png").exists());
        assertTrue(new File(tempTexDir, "checkerboard_uv.png").exists());
        assertTrue(new File(tempTexDir, "floor_tiles.png").exists());
    }

    @Test
    @DisplayName("Indikator 5: Verifikasi Matriks Light Space & Slope-Scaled Shadow Bias pada ShadowMapFBO")
    public void testShadowMappingLightMatrixAndBias() {
        Vector3f lightPos = new Vector3f(4.0f, 5.0f, 4.0f);
        Vector3f center = new Vector3f(0.0f, 0.0f, 0.0f);

        Matrix4f lightSpaceMatrix = ShadowMapFBO.calculateLightSpaceMatrix(lightPos, center);
        // Titik pusat scene (0, 0, 0) harus terproyeksi ke pusat xy (0, 0) dalam Light Clip Space
        Vector4f projectedCenter = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f).mul(lightSpaceMatrix);
        assertEquals(0.0f, projectedCenter.x / projectedCenter.w, EPSILON);
        assertEquals(0.0f, projectedCenter.y / projectedCenter.w, EPSILON);

        // Uji Slope-Scaled Bias: permukaan tegak lurus cahaya (N . L = 1) menggunakan biasMin,
        // sedangkan permukaan miring tajam (N . L = 0) menggunakan biasMax
        Vector3f normal = new Vector3f(0.0f, 1.0f, 0.0f);
        float minBias = ShadowMapFBO.calculateDynamicShadowBias(normal, new Vector3f(0.0f, 1.0f, 0.0f), 0.002f, 0.02f);
        float maxBias = ShadowMapFBO.calculateDynamicShadowBias(normal, new Vector3f(1.0f, 0.0f, 0.0f), 0.002f, 0.02f);
        assertEquals(0.002f, minBias, EPSILON);
        assertEquals(0.02f, maxBias, EPSILON);
    }
}
