package com.grafkom.viewing3d;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL20.*;

/**
 * Kelas ShaderProgram untuk mengelola kompilasi, linking, dan pengiriman uniform
 * pada Modern OpenGL GLSL (#version 330 core).
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
public class ShaderProgram {

    private final int programId;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public ShaderProgram(String vertexResourcePath, String fragmentResourcePath) {
        String vertexSource = loadShaderSource(vertexResourcePath);
        String fragmentSource = loadShaderSource(fragmentResourcePath);

        int vertexShaderId = compileShader(GL_VERTEX_SHADER, vertexSource, vertexResourcePath);
        int fragmentShaderId = compileShader(GL_FRAGMENT_SHADER, fragmentSource, fragmentResourcePath);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            throw new IllegalStateException("Gagal melakukan linking GLSL Shader Program: " + log);
        }

        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }

    private int compileShader(int type, String source, String name) {
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shaderId);
            throw new IllegalStateException("Gagal mengompilasi shader [" + name + "]: " + log);
        }
        return shaderId;
    }

    public static String loadShaderSource(String resourcePath) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }

        // Fallback baca dari direktori src/main/resources jika dijalankan langsung
        Path fallback = Path.of("src/main/resources" + resourcePath);
        try {
            if (Files.exists(fallback)) {
                return Files.readString(fallback, StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Gagal membaca berkas shader: " + resourcePath, ex);
        }
        throw new IllegalArgumentException("Berkas shader tidak ditemukan: " + resourcePath);
    }

    public void use() {
        glUseProgram(programId);
    }

    public void setMat4(String name, Matrix4f mat) {
        int loc = glGetUniformLocation(programId, name);
        if (loc >= 0) {
            mat.get(matrixBuffer);
            glUniformMatrix4fv(loc, false, matrixBuffer);
        }
    }

    public void setVec3(String name, Vector3f vec) {
        int loc = glGetUniformLocation(programId, name);
        if (loc >= 0) {
            glUniform3f(loc, vec.x, vec.y, vec.z);
        }
    }

    public void setVec3(String name, float x, float y, float z) {
        int loc = glGetUniformLocation(programId, name);
        if (loc >= 0) {
            glUniform3f(loc, x, y, z);
        }
    }

    public void setFloat(String name, float value) {
        int loc = glGetUniformLocation(programId, name);
        if (loc >= 0) {
            glUniform1f(loc, value);
        }
    }

    public void setInt(String name, int value) {
        int loc = glGetUniformLocation(programId, name);
        if (loc >= 0) {
            glUniform1i(loc, value);
        }
    }

    public void setBool(String name, boolean value) {
        int loc = glGetUniformLocation(programId, name);
        if (loc >= 0) {
            glUniform1i(loc, value ? 1 : 0);
        }
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }

    /**
     * Evaluasi matematis CPU untuk perbandingan intensitas Specular Phong vs Blinn-Phong
     * (digunakan untuk verifikasi unit test JUnit 5 & analisis laporan).
     */
    public static float evaluatePhongSpecular(Vector3f normal, Vector3f lightDir, Vector3f viewDir, float shininess) {
        Vector3f n = new Vector3f(normal).normalize();
        Vector3f l = new Vector3f(lightDir).normalize();
        Vector3f v = new Vector3f(viewDir).normalize();
        // R = 2(N . L)N - L
        float ndotl = Math.max(n.dot(l), 0.0f);
        if (ndotl <= 0.0f) {
            return 0.0f;
        }
        Vector3f r = new Vector3f(n).mul(2.0f * ndotl).sub(l).normalize();
        return (float) Math.pow(Math.max(r.dot(v), 0.0f), shininess);
    }

    public static float evaluateBlinnPhongSpecular(Vector3f normal, Vector3f lightDir, Vector3f viewDir, float shininess) {
        Vector3f n = new Vector3f(normal).normalize();
        Vector3f l = new Vector3f(lightDir).normalize();
        Vector3f v = new Vector3f(viewDir).normalize();
        if (n.dot(l) <= 0.0f) {
            return 0.0f;
        }
        // H = (L + V) / ||L + V||
        Vector3f h = new Vector3f(l).add(v).normalize();
        return (float) Math.pow(Math.max(n.dot(h), 0.0f), shininess);
    }
}
