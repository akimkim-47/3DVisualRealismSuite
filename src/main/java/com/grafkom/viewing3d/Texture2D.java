package com.grafkom.viewing3d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/**
 * Praktikum 3: Kelas Texture2D - Java Texture Loader & UV Mapping (Indikator 4).
 *
 * Memuat berkas citra digital (PNG/JPG) menggunakan pustaka native STBImage ke dalam memori GPU,
 * menghasilkan hierarki Mipmap otomatis, serta mendukung peralihan mode Texture Filtering:
 * 1. Bilinear Mipmapped Filtering (GL_LINEAR_MIPMAP_LINEAR / GL_LINEAR) - Halus & bebas aliasing.
 * 2. Nearest Neighbor Filtering (GL_NEAREST) - Pikselasi blok (blocky texels) untuk perbandingan.
 *
 * @author Andrean Lukman Hakim (25050974059)
 */
public class Texture2D {

    private int textureID;
    private final String filePath;
    private boolean bilinearMipmap = true;

    public Texture2D(String filePath) {
        this.filePath = filePath;
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Texture Filtering & Wrap Parameters sesuai Praktikum 3
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        stbi_set_flip_vertically_on_load(true);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer data = stbi_load(filePath, width, height, channels, 4);
            if (data != null) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0),
                        0, GL_RGBA, GL_UNSIGNED_BYTE, data);
                glGenerateMipmap(GL_TEXTURE_2D);
                stbi_image_free(data);
            } else {
                System.err.println("Failed to load texture: " + filePath + " (" + stbi_failure_reason() + ")");
            }
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    /**
     * Mengatur mode penyaringan tekstur (Nearest Neighbor vs Bilinear Mipmapped) secara real-time.
     */
    public void setBilinearMipmap(boolean enable) {
        this.bilinearMipmap = enable;
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (enable) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public boolean isBilinearMipmap() {
        return bilinearMipmap;
    }

    public int getTextureID() {
        return textureID;
    }

    public String getFilePath() {
        return filePath;
    }

    public void cleanup() {
        glDeleteTextures(textureID);
    }

    /**
     * Rumus interpolasi UV perspektif-korektif sesuai Modul Bagian I (4.b):
     *   u_interp = (uA/wA * s + uB/wB * (1 - s)) / (1/wA * s + 1/wB * (1 - s))
     */
    public static float perspectiveCorrectInterpolateUV(float uA, float wA, float uB, float wB, float s) {
        float num = (uA / wA) * s + (uB / wB) * (1.0f - s);
        float den = (1.0f / wA) * s + (1.0f / wB) * (1.0f - s);
        return num / den;
    }

    /**
     * Menghasilkan berkas tekstur PNG beresolusi tinggi pada folder resources/textures
     * untuk pengujian STBImage & UV Mapping (Batu Bata, Checkerboard UV, & Lantai Marmer).
     */
    public static void ensureDefaultTexturesExist(File textureDir) {
        if (!textureDir.exists()) {
            textureDir.mkdirs();
        }
        File brickFile = new File(textureDir, "brick_wall.png");
        if (!brickFile.exists()) {
            generateBrickTexture(brickFile, 256, 256);
        }
        File checkerFile = new File(textureDir, "checkerboard_uv.png");
        if (!checkerFile.exists()) {
            generateCheckerboardTexture(checkerFile, 256, 256);
        }
        File marbleTileFile = new File(textureDir, "floor_tiles.png");
        if (!marbleTileFile.exists()) {
            generateFloorTileTexture(marbleTileFile, 256, 256);
        }
    }

    private static void generateBrickTexture(File file, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Warna semen nat bata
        g.setColor(new Color(210, 204, 194));
        g.fillRect(0, 0, w, h);

        int rows = 8;
        int cols = 4;
        int brickH = h / rows;
        int brickW = w / cols;
        int mortar = 4;

        for (int r = 0; r < rows; r++) {
            int offset = (r % 2 == 1) ? (brickW / 2) : 0;
            for (int c = -1; c <= cols; c++) {
                int x = c * brickW + offset;
                int y = r * brickH;
                int shade = ((r * 7 + c * 13) & 15) - 8;
                Color base = new Color(
                        clamp(186 + shade, 0, 255),
                        clamp(76 + shade / 2, 0, 255),
                        clamp(54 + shade / 2, 0, 255)
                );
                g.setColor(base);
                g.fillRect(x + mortar / 2, y + mortar / 2, brickW - mortar, brickH - mortar);
                g.setColor(new Color(215, 108, 82));
                g.drawLine(x + mortar / 2, y + mortar / 2, x + brickW - mortar, y + mortar / 2);
            }
        }
        g.dispose();
        writePng(img, file);
    }

    private static void generateCheckerboardTexture(File file, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        int cells = 8;
        int cellW = w / cells;
        int cellH = h / cells;

        for (int r = 0; r < cells; r++) {
            for (int c = 0; c < cells; c++) {
                boolean even = ((r + c) % 2 == 0);
                if (even) {
                    g.setColor(new Color(240, 244, 248));
                } else {
                    // Variasi warna kuadran UV agar orientasi pemetaan UV terlihat jelas
                    int cr = (c < cells / 2) ? 52 : 215;
                    int cg = (r < cells / 2) ? 142 : 85;
                    int cb = 205;
                    g.setColor(new Color(cr, cg, cb));
                }
                g.fillRect(c * cellW, r * cellH, cellW, cellH);
            }
        }
        g.dispose();
        writePng(img, file);
    }

    private static void generateFloorTileTexture(File file, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        int tiles = 4;
        int tileSize = w / tiles;

        for (int r = 0; r < tiles; r++) {
            for (int c = 0; c < tiles; c++) {
                boolean alt = ((r + c) % 2 == 0);
                g.setColor(alt ? new Color(188, 196, 208) : new Color(148, 158, 174));
                g.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                g.setColor(new Color(110, 120, 136));
                g.drawRect(c * tileSize, r * tileSize, tileSize - 1, tileSize - 1);
            }
        }
        g.dispose();
        writePng(img, file);
    }

    private static void writePng(BufferedImage img, File file) {
        try {
            ImageIO.write(img, "PNG", file);
        } catch (IOException ex) {
            System.err.println("Gagal membuat tekstur prosedural: " + ex.getMessage());
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
