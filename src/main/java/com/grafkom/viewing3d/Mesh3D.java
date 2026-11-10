package com.grafkom.viewing3d;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Spesifikasi Tugas Mandiri Minggu 11 (1.a): Kelas Mesh3D Bermateri (VAO/VBO Modern OpenGL).
 *
 * Membangun geometri 3D kompleks yang dilengkapi:
 * - Koordinat Posisi 3D (x, y, z)
 * - Vektor Normal Permukaan:
 *     1. Smooth Vertex Normal Averaging: N_vertex = (sum N_face_i) / ||sum N_face_i||
 *     2. Flat Facet Normal            : N_face   = normalize((v1 - v0) x (v2 - v0))
 * - Koordinat Tekstur UV ternormalisasi (u, v)
 * - Warna Materi Dasar (r, g, b)
 *
 * Mencakup 4 objek utama dalam scene:
 * 1. UV Sphere 3D (Bola ber-tesselasi untuk uji Phong vs Blinn-Phong & Flat vs Smooth Shading)
 * 2. Torus 3D (Cincin lengkung 3D untuk uji kurvatur, Self-Shadowing, & Cast Shadow)
 * 3. Rumah 3D Bermateri (Bangunan arsitektural bertekstur UV untuk uji HSR & Texture Filtering)
 * 4. Ground Plane (Bidang lantai luas penerima bayangan Real-Time 2-Pass Shadow Mapping)
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
public class Mesh3D {

    public enum ScenePreset {
        COMPLETE_REALISM_SUITE("SCENE 1: VISUAL REALISM SUITE"),
        SHADING_SPHERE_TORUS_LAB("SCENE 2: SHADING & NORMAL LAB"),
        ARCHITECTURE_SHADOW_LAB("SCENE 3: HOUSE & SHADOW LAB");

        private final String label;

        ScenePreset(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static class GpuMesh {
        int smoothVaoId;
        int smoothVboId;
        int flatVaoId;
        int flatVboId;
        int vertexCount;

        void draw(boolean flatShading) {
            glBindVertexArray(flatShading ? flatVaoId : smoothVaoId);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
            glBindVertexArray(0);
        }

        void cleanup() {
            glDeleteBuffers(smoothVboId);
            glDeleteVertexArrays(smoothVaoId);
            glDeleteBuffers(flatVboId);
            glDeleteVertexArrays(flatVaoId);
        }
    }

    private GpuMesh sphereMesh;
    private GpuMesh torusMesh;
    private GpuMesh houseWallsMesh;
    private GpuMesh houseRoofMesh;
    private GpuMesh cubeMesh;
    private GpuMesh groundPlaneMesh;

    private ScenePreset activePreset = ScenePreset.COMPLETE_REALISM_SUITE;
    private boolean wireframeMode = false;

    // Palet warna atap rumah (dapat dikustomisasi)
    private Vector3f roofColor = new Vector3f(0.24f, 0.66f, 0.42f);
    private Vector3f sphereColor = new Vector3f(0.56f, 0.36f, 0.94f);
    private Vector3f torusColor = new Vector3f(0.96f, 0.48f, 0.24f);

    public void initGpuMeshes() {
        sphereMesh = buildSphereMesh(1.05f, 28, 18);
        torusMesh = buildTorusMesh(0.78f, 0.30f, 28, 18);
        houseWallsMesh = buildHouseWallsMesh();
        houseRoofMesh = buildHouseRoofMesh();
        cubeMesh = buildUnitCubeMesh();
        groundPlaneMesh = buildGroundPlaneMesh(8.5f, 6.0f);
    }

    /**
     * Merender seluruh objek 3D pada Pass 1 (Shadow Map Depth Pass) atau Pass 2 (Lighting Pass).
     */
    public void renderSceneObjects(
            ShaderProgram shader,
            boolean flatShading,
            boolean useTexture,
            Texture2D mainObjectTexture,
            Texture2D floorTexture,
            float animAngleDeg) {

        if (wireframeMode) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }

        // 1. Render Bidang Lantai (Ground Plane penerima bayangan)
        if (floorTexture != null && useTexture) {
            floorTexture.bind(0);
            shader.setBool("useTexture", true);
        } else {
            shader.setBool("useTexture", false);
        }
        shader.setBool("useVertexColor", false);
        shader.setVec3("objectColor", 0.78f, 0.82f, 0.88f);
        Matrix4f floorModel = new Matrix4f().identity();
        shader.setMat4("model", floorModel);
        groundPlaneMesh.draw(false);

        // Konfigurasi tekstur untuk objek utama
        if (mainObjectTexture != null && useTexture) {
            mainObjectTexture.bind(0);
        }

        switch (activePreset) {
            case SHADING_SPHERE_TORUS_LAB -> renderShadingLabObjects(shader, flatShading, useTexture, animAngleDeg);
            case ARCHITECTURE_SHADOW_LAB -> renderArchitectureLabObjects(shader, flatShading, useTexture, animAngleDeg);
            case COMPLETE_REALISM_SUITE -> renderCompleteSuiteObjects(shader, flatShading, useTexture, animAngleDeg);
        }

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    private void renderCompleteSuiteObjects(
            ShaderProgram shader,
            boolean flatShading,
            boolean useTexture,
            float animAngleDeg) {

        // A. UV Sphere 3D di Kiri-Depan (-1.85, -0.12, 0.85) -> Dirender lebih dulu agar saat Z-Buffer OFF terlihat efek overlap oleh objek belakang
        Matrix4f sphereModel = new Matrix4f()
                .translate(-1.85f, -0.12f, 0.85f)
                .rotateY((float) Math.toRadians(animAngleDeg));
        shader.setMat4("model", sphereModel);
        shader.setBool("useTexture", useTexture);
        shader.setVec3("objectColor", useTexture ? new Vector3f(0.95f, 0.95f, 0.95f) : sphereColor);
        sphereMesh.draw(flatShading);

        // B. Torus 3D di Kanan-Depan (1.85, -0.05, 0.80) -> Fokus uji Bayangan Lengkung & Specular
        Matrix4f torusModel = new Matrix4f()
                .translate(1.85f, -0.05f, 0.80f)
                .rotateX((float) Math.toRadians(32.0f))
                .rotateY((float) Math.toRadians(25.0f + animAngleDeg * 0.8f));
        shader.setMat4("model", torusModel);
        shader.setBool("useTexture", useTexture);
        shader.setVec3("objectColor", useTexture ? new Vector3f(0.98f, 0.92f, 0.78f) : torusColor);
        torusMesh.draw(flatShading);

        // C. Rumah 3D Bermateri di Tengah-Belakang (0.0, -1.20, -0.95)
        Matrix4f houseModel = new Matrix4f()
                .translate(0.0f, -1.20f, -0.95f)
                .rotateY((float) Math.toRadians(animAngleDeg * 0.25f));
        shader.setMat4("model", houseModel);
        shader.setBool("useTexture", useTexture);
        shader.setVec3("objectColor", 0.95f, 0.91f, 0.84f);
        houseWallsMesh.draw(flatShading);

        shader.setBool("useTexture", false);
        shader.setVec3("objectColor", roofColor);
        houseRoofMesh.draw(flatShading);
    }

    private void renderShadingLabObjects(
            ShaderProgram shader,
            boolean flatShading,
            boolean useTexture,
            float animAngleDeg) {

        // Dua Sphere & Satu Torus besar untuk pengamatan jarak dekat Specular & Normal Interpolation
        Matrix4f leftSphere = new Matrix4f()
                .translate(-1.65f, 0.05f, 0.25f)
                .scale(1.22f);
        shader.setMat4("model", leftSphere);
        shader.setBool("useTexture", useTexture);
        shader.setVec3("objectColor", sphereColor);
        sphereMesh.draw(flatShading);

        Matrix4f rightTorus = new Matrix4f()
                .translate(1.65f, 0.10f, 0.25f)
                .rotateX((float) Math.toRadians(35.0f))
                .rotateY((float) Math.toRadians(28.0f + animAngleDeg))
                .scale(1.25f);
        shader.setMat4("model", rightTorus);
        shader.setBool("useTexture", useTexture);
        shader.setVec3("objectColor", torusColor);
        torusMesh.draw(flatShading);
    }

    private void renderArchitectureLabObjects(
            ShaderProgram shader,
            boolean flatShading,
            boolean useTexture,
            float animAngleDeg) {

        Matrix4f houseModel = new Matrix4f()
                .translate(0.0f, -1.20f, 0.0f)
                .scale(1.18f);
        shader.setMat4("model", houseModel);
        shader.setBool("useTexture", useTexture);
        shader.setVec3("objectColor", 0.96f, 0.92f, 0.86f);
        houseWallsMesh.draw(flatShading);

        shader.setBool("useTexture", false);
        shader.setVec3("objectColor", roofColor);
        houseRoofMesh.draw(flatShading);

        // Dua pilar kubus di kiri & kanan depan untuk uji bayangan bertingkat
        for (float px : new float[]{-2.45f, 2.45f}) {
            Matrix4f pillar = new Matrix4f()
                    .translate(px, -0.30f, 1.35f)
                    .scale(0.45f, 0.90f, 0.45f);
            shader.setMat4("model", pillar);
            shader.setBool("useTexture", useTexture);
            shader.setVec3("objectColor", 0.88f, 0.84f, 0.78f);
            cubeMesh.draw(flatShading);
        }
    }

    /**
     * Merender kubus penanda posisi sumber cahaya (Light Source Indicator).
     */
    public void renderLightSourceMarker(ShaderProgram shader, Vector3f lightPos) {
        Matrix4f lightModel = new Matrix4f()
                .translate(lightPos)
                .scale(0.18f);
        shader.setMat4("model", lightModel);
        shader.setBool("unlitMode", true);
        shader.setVec3("objectColor", 1.0f, 0.96f, 0.72f);
        cubeMesh.draw(true);
        shader.setBool("unlitMode", false);
    }

    /**
     * Rumus matematis Per-Face Normal untuk Flat Shading:
     *   N_face = normalize((v1 - v0) x (v2 - v0))
     */
    public static Vector3f calculateFaceNormal(Vector3f v0, Vector3f v1, Vector3f v2) {
        Vector3f edge1 = new Vector3f(v1).sub(v0);
        Vector3f edge2 = new Vector3f(v2).sub(v0);
        Vector3f normal = new Vector3f();
        edge1.cross(edge2, normal);
        if (normal.lengthSquared() > 1e-10f) {
            normal.normalize();
        } else {
            normal.set(0.0f, 1.0f, 0.0f);
        }
        return normal;
    }

    /**
     * Rumus matematis Vertex Normal Averaging untuk Smooth Shading sesuai Modul Bagian I (2.b):
     *   N_vertex = (sum N_face_i) / ||sum N_face_i||
     */
    public static Vector3f calculateVertexNormalAverage(Vector3f... adjacentFaceNormals) {
        Vector3f sum = new Vector3f();
        for (Vector3f fn : adjacentFaceNormals) {
            sum.add(fn);
        }
        if (sum.lengthSquared() > 1e-10f) {
            sum.normalize();
        } else {
            sum.set(0.0f, 1.0f, 0.0f);
        }
        return sum;
    }

    private GpuMesh buildSphereMesh(float radius, int sectors, int stacks) {
        List<Float> smoothData = new ArrayList<>();
        List<Float> flatData = new ArrayList<>();

        for (int i = 0; i < stacks; i++) {
            float phi1 = (float) (Math.PI * ((double) i / stacks - 0.5));
            float phi2 = (float) (Math.PI * ((double) (i + 1) / stacks - 0.5));
            float v1 = (float) i / stacks;
            float v2 = (float) (i + 1) / stacks;

            for (int j = 0; j < sectors; j++) {
                float theta1 = (float) (2.0 * Math.PI * j / sectors);
                float theta2 = (float) (2.0 * Math.PI * (j + 1) / sectors);
                float u1 = (float) j / sectors;
                float u2 = (float) (j + 1) / sectors;

                Vector3f p00 = spherePoint(radius, phi1, theta1);
                Vector3f p10 = spherePoint(radius, phi1, theta2);
                Vector3f p11 = spherePoint(radius, phi2, theta2);
                Vector3f p01 = spherePoint(radius, phi2, theta1);

                Vector3f n00 = new Vector3f(p00).normalize();
                Vector3f n10 = new Vector3f(p10).normalize();
                Vector3f n11 = new Vector3f(p11).normalize();
                Vector3f n01 = new Vector3f(p01).normalize();

                // Segitiga 1: p00 -> p10 -> p11
                Vector3f fn1 = calculateFaceNormal(p00, p10, p11);
                addTriangle(smoothData, p00, n00, u1, v1, p10, n10, u2, v1, p11, n11, u2, v2);
                addTriangle(flatData, p00, fn1, u1, v1, p10, fn1, u2, v1, p11, fn1, u2, v2);

                // Segitiga 2: p00 -> p11 -> p01
                Vector3f fn2 = calculateFaceNormal(p00, p11, p01);
                addTriangle(smoothData, p00, n00, u1, v1, p11, n11, u2, v2, p01, n01, u1, v2);
                addTriangle(flatData, p00, fn2, u1, v1, p11, fn2, u2, v2, p01, fn2, u1, v2);
            }
        }
        return uploadDualMesh(smoothData, flatData);
    }

    private Vector3f spherePoint(float r, float phi, float theta) {
        float x = r * (float) (Math.cos(phi) * Math.sin(theta));
        float y = r * (float) Math.sin(phi);
        float z = r * (float) (Math.cos(phi) * Math.cos(theta));
        return new Vector3f(x, y, z);
    }

    private GpuMesh buildTorusMesh(float majorR, float minorR, int majorSegs, int minorSegs) {
        List<Float> smoothData = new ArrayList<>();
        List<Float> flatData = new ArrayList<>();

        for (int i = 0; i < majorSegs; i++) {
            float u1 = (float) i / majorSegs;
            float u2 = (float) (i + 1) / majorSegs;
            float a1 = (float) (2.0 * Math.PI * u1);
            float a2 = (float) (2.0 * Math.PI * u2);

            for (int j = 0; j < minorSegs; j++) {
                float v1 = (float) j / minorSegs;
                float v2 = (float) (j + 1) / minorSegs;
                float b1 = (float) (2.0 * Math.PI * v1);
                float b2 = (float) (2.0 * Math.PI * v2);

                Vector3f p00 = torusPoint(majorR, minorR, a1, b1);
                Vector3f p10 = torusPoint(majorR, minorR, a2, b1);
                Vector3f p11 = torusPoint(majorR, minorR, a2, b2);
                Vector3f p01 = torusPoint(majorR, minorR, a1, b2);

                Vector3f n00 = torusNormal(a1, b1);
                Vector3f n10 = torusNormal(a2, b1);
                Vector3f n11 = torusNormal(a2, b2);
                Vector3f n01 = torusNormal(a1, b2);

                Vector3f fn1 = calculateFaceNormal(p00, p10, p11);
                addTriangle(smoothData, p00, n00, u1, v1, p10, n10, u2, v1, p11, n11, u2, v2);
                addTriangle(flatData, p00, fn1, u1, v1, p10, fn1, u2, v1, p11, fn1, u2, v2);

                Vector3f fn2 = calculateFaceNormal(p00, p11, p01);
                addTriangle(smoothData, p00, n00, u1, v1, p11, n11, u2, v2, p01, n01, u1, v2);
                addTriangle(flatData, p00, fn2, u1, v1, p11, fn2, u2, v2, p01, fn2, u1, v2);
            }
        }
        return uploadDualMesh(smoothData, flatData);
    }

    private Vector3f torusPoint(float majorR, float minorR, float u, float v) {
        float r = majorR + minorR * (float) Math.cos(v);
        return new Vector3f(
                r * (float) Math.cos(u),
                minorR * (float) Math.sin(v),
                -r * (float) Math.sin(u)
        );
    }

    private Vector3f torusNormal(float u, float v) {
        return new Vector3f(
                (float) (Math.cos(v) * Math.cos(u)),
                (float) Math.sin(v),
                (float) (-Math.cos(v) * Math.sin(u))
        ).normalize();
    }

    private GpuMesh buildHouseWallsMesh() {
        List<Float> smoothData = new ArrayList<>();
        List<Float> flatData = new ArrayList<>();

        float xL = -1.25f, xR = 1.25f;
        float yB = 0.0f, yT = 1.45f, yPeak = 2.30f;
        float zB = -1.05f, zF = 1.05f;

        // 4 Dinding Persegi Panjang (CCW Winding Order untuk Back-Face Culling)
        addQuad(smoothData, flatData,
                new Vector3f(xL, yB, zF), new Vector3f(xR, yB, zF),
                new Vector3f(xR, yT, zF), new Vector3f(xL, yT, zF), 2.0f, 1.5f);
        addQuad(smoothData, flatData,
                new Vector3f(xR, yB, zB), new Vector3f(xL, yB, zB),
                new Vector3f(xL, yT, zB), new Vector3f(xR, yT, zB), 2.0f, 1.5f);
        addQuad(smoothData, flatData,
                new Vector3f(xL, yB, zB), new Vector3f(xL, yB, zF),
                new Vector3f(xL, yT, zF), new Vector3f(xL, yT, zB), 2.0f, 1.5f);
        addQuad(smoothData, flatData,
                new Vector3f(xR, yB, zF), new Vector3f(xR, yB, zB),
                new Vector3f(xR, yT, zB), new Vector3f(xR, yT, zF), 2.0f, 1.5f);

        // Segitiga Pedimen Depan & Belakang (CCW)
        Vector3f nf = new Vector3f(0.0f, 0.0f, 1.0f);
        addTriangle(smoothData,
                new Vector3f(xL, yT, zF), nf, 0.0f, 0.0f,
                new Vector3f(xR, yT, zF), nf, 2.0f, 0.0f,
                new Vector3f(0.0f, yPeak, zF), nf, 1.0f, 1.0f);
        addTriangle(flatData,
                new Vector3f(xL, yT, zF), nf, 0.0f, 0.0f,
                new Vector3f(xR, yT, zF), nf, 2.0f, 0.0f,
                new Vector3f(0.0f, yPeak, zF), nf, 1.0f, 1.0f);

        Vector3f nb = new Vector3f(0.0f, 0.0f, -1.0f);
        addTriangle(smoothData,
                new Vector3f(xR, yT, zB), nb, 0.0f, 0.0f,
                new Vector3f(xL, yT, zB), nb, 2.0f, 0.0f,
                new Vector3f(0.0f, yPeak, zB), nb, 1.0f, 1.0f);
        addTriangle(flatData,
                new Vector3f(xR, yT, zB), nb, 0.0f, 0.0f,
                new Vector3f(xL, yT, zB), nb, 2.0f, 0.0f,
                new Vector3f(0.0f, yPeak, zB), nb, 1.0f, 1.0f);

        return uploadDualMesh(smoothData, flatData);
    }

    private GpuMesh buildHouseRoofMesh() {
        List<Float> smoothData = new ArrayList<>();
        List<Float> flatData = new ArrayList<>();

        float rx = 1.42f, rz = 1.22f;
        float yEave = 1.38f, yPeak = 2.35f;

        // Lereng Atap Kiri (Atas & Bawah agar tampak solid dari segala sudut)
        addQuad(smoothData, flatData,
                new Vector3f(-rx, yEave, rz), new Vector3f(0.0f, yPeak, rz),
                new Vector3f(0.0f, yPeak, -rz), new Vector3f(-rx, yEave, -rz), 2.0f, 2.0f);
        addQuad(smoothData, flatData,
                new Vector3f(-rx, yEave, -rz), new Vector3f(0.0f, yPeak, -rz),
                new Vector3f(0.0f, yPeak, rz), new Vector3f(-rx, yEave, rz), 2.0f, 2.0f);

        // Lereng Atap Kanan
        addQuad(smoothData, flatData,
                new Vector3f(0.0f, yPeak, rz), new Vector3f(rx, yEave, rz),
                new Vector3f(rx, yEave, -rz), new Vector3f(0.0f, yPeak, -rz), 2.0f, 2.0f);
        addQuad(smoothData, flatData,
                new Vector3f(0.0f, yPeak, -rz), new Vector3f(rx, yEave, -rz),
                new Vector3f(rx, yEave, rz), new Vector3f(0.0f, yPeak, rz), 2.0f, 2.0f);

        return uploadDualMesh(smoothData, flatData);
    }

    private GpuMesh buildUnitCubeMesh() {
        List<Float> smoothData = new ArrayList<>();
        List<Float> flatData = new ArrayList<>();
        float s = 1.0f;

        // 6 Sisi Kubus Berorientasi CCW
        addQuad(smoothData, flatData,
                new Vector3f(-s, -s, s), new Vector3f(s, -s, s),
                new Vector3f(s, s, s), new Vector3f(-s, s, s), 1.0f, 1.0f);
        addQuad(smoothData, flatData,
                new Vector3f(s, -s, -s), new Vector3f(-s, -s, -s),
                new Vector3f(-s, s, -s), new Vector3f(s, s, -s), 1.0f, 1.0f);
        addQuad(smoothData, flatData,
                new Vector3f(-s, -s, -s), new Vector3f(-s, -s, s),
                new Vector3f(-s, s, s), new Vector3f(-s, s, -s), 1.0f, 1.0f);
        addQuad(smoothData, flatData,
                new Vector3f(s, -s, s), new Vector3f(s, -s, -s),
                new Vector3f(s, s, -s), new Vector3f(s, s, s), 1.0f, 1.0f);
        addQuad(smoothData, flatData,
                new Vector3f(-s, s, s), new Vector3f(s, s, s),
                new Vector3f(s, s, -s), new Vector3f(-s, s, -s), 1.0f, 1.0f);
        addQuad(smoothData, flatData,
                new Vector3f(-s, -s, -s), new Vector3f(s, -s, -s),
                new Vector3f(s, -s, s), new Vector3f(-s, -s, s), 1.0f, 1.0f);

        return uploadDualMesh(smoothData, flatData);
    }

    private GpuMesh buildGroundPlaneMesh(float halfExtent, float uvRepeat) {
        List<Float> smoothData = new ArrayList<>();
        List<Float> flatData = new ArrayList<>();
        float y = -1.20f;

        addQuad(smoothData, flatData,
                new Vector3f(-halfExtent, y, halfExtent),
                new Vector3f(halfExtent, y, halfExtent),
                new Vector3f(halfExtent, y, -halfExtent),
                new Vector3f(-halfExtent, y, -halfExtent),
                uvRepeat, uvRepeat);
        return uploadDualMesh(smoothData, flatData);
    }

    private void addQuad(
            List<Float> smoothOut,
            List<Float> flatOut,
            Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3,
            float uMax, float vMax) {

        Vector3f fn = calculateFaceNormal(p0, p1, p2);
        // Segitiga 1: p0 -> p1 -> p2
        addTriangle(smoothOut, p0, fn, 0.0f, 0.0f, p1, fn, uMax, 0.0f, p2, fn, uMax, vMax);
        addTriangle(flatOut, p0, fn, 0.0f, 0.0f, p1, fn, uMax, 0.0f, p2, fn, uMax, vMax);
        // Segitiga 2: p0 -> p2 -> p3
        addTriangle(smoothOut, p0, fn, 0.0f, 0.0f, p2, fn, uMax, vMax, p3, fn, 0.0f, vMax);
        addTriangle(flatOut, p0, fn, 0.0f, 0.0f, p2, fn, uMax, vMax, p3, fn, 0.0f, vMax);
    }

    private void addTriangle(
            List<Float> out,
            Vector3f p0, Vector3f n0, float u0, float v0,
            Vector3f p1, Vector3f n1, float u1, float v1,
            Vector3f p2, Vector3f n2, float u2, float v2) {
        pushVertex(out, p0, n0, u0, v0);
        pushVertex(out, p1, n1, u1, v1);
        pushVertex(out, p2, n2, u2, v2);
    }

    private void pushVertex(List<Float> out, Vector3f p, Vector3f n, float u, float v) {
        // Position (3)
        out.add(p.x);
        out.add(p.y);
        out.add(p.z);
        // Normal (3)
        out.add(n.x);
        out.add(n.y);
        out.add(n.z);
        // UV (2)
        out.add(u);
        out.add(v);
        // Default Color (3)
        out.add(1.0f);
        out.add(1.0f);
        out.add(1.0f);
    }

    private GpuMesh uploadDualMesh(List<Float> smoothFloats, List<Float> flatFloats) {
        GpuMesh mesh = new GpuMesh();
        mesh.vertexCount = smoothFloats.size() / 11;

        int[] smoothIds = uploadBufferToVao(smoothFloats);
        mesh.smoothVaoId = smoothIds[0];
        mesh.smoothVboId = smoothIds[1];

        int[] flatIds = uploadBufferToVao(flatFloats);
        mesh.flatVaoId = flatIds[0];
        mesh.flatVboId = flatIds[1];
        return mesh;
    }

    private int[] uploadBufferToVao(List<Float> data) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.size());
        for (Float f : data) {
            buffer.put(f);
        }
        buffer.flip();

        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        int stride = 11 * Float.BYTES;
        // location 0: aPos (vec3)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        // location 1: aNormal (vec3)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        // location 2: aTexCoords (vec2)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        // location 3: aColor (vec3)
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8L * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        return new int[]{vao, vbo};
    }

    public ScenePreset getActivePreset() {
        return activePreset;
    }

    public void setActivePreset(ScenePreset activePreset) {
        this.activePreset = activePreset;
    }

    public void cycleScenePreset() {
        ScenePreset[] values = ScenePreset.values();
        this.activePreset = values[(this.activePreset.ordinal() + 1) % values.length];
    }

    public boolean isWireframeMode() {
        return wireframeMode;
    }

    public void toggleWireframeMode() {
        this.wireframeMode = !this.wireframeMode;
    }

    public void setRoofColor(float r, float g, float b) {
        this.roofColor.set(r, g, b);
    }

    public void setSphereColor(float r, float g, float b) {
        this.sphereColor.set(r, g, b);
    }

    public void cleanup() {
        if (sphereMesh != null) sphereMesh.cleanup();
        if (torusMesh != null) torusMesh.cleanup();
        if (houseWallsMesh != null) houseWallsMesh.cleanup();
        if (houseRoofMesh != null) houseRoofMesh.cleanup();
        if (cubeMesh != null) cubeMesh.cleanup();
        if (groundPlaneMesh != null) groundPlaneMesh.cleanup();
    }
}
