package org.polygon.engine.core.graph;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {
    private final int textureId;
    private final String texturePath;

    public static final Texture BRDF_LUT = new Texture("resources/textures/brdf.png");

    public Texture(int width, int height, ByteBuffer bfr) {
        texturePath = "";
        textureId = generateTexture(width, height, bfr);
    }

    public Texture(String texturePath) {
        this.texturePath = texturePath;
        this.textureId = generateTexture(texturePath);
    }

    public String getTexturePath() {
        return texturePath;
    }

    public int getTextureId() {
        return textureId;
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void cleanup() {
        glDeleteTextures(textureId);
    }

    private int generateTexture(int width, int height, ByteBuffer bfr) {
        // Generate a texture in the GPU
        int textureId = glGenTextures();
        // Bind that texture
        glBindTexture(GL_TEXTURE_2D, textureId);
        // Since the channels for the image is one byte each, the param is 1 (RGBA).
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        // If there is no 1:1 association to a texture coords, just use the nearest point in the texture coords
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        // Load the texture data
        // Loads the texture with a target 2D texture (GL_TEXTURE_2D)
        // Level of details is set to 0, base image level of detail
        // The internal format is set to RGBA
        // border value must be 0
        // We then send the image data stored in bfr to generate the texture
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, bfr);
        // Generate a mipmap for our HD image when mapped object is scaled
        glGenerateMipmap(GL_TEXTURE_2D);
        return textureId;
    }

    private int generateTexture(String texturePath) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            // Allocate off-heap memory for the width, height and image channels
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load a picture into a buffer using stb
            ByteBuffer bfr = stbi_load(texturePath, w, h, channels, 4);

            if(bfr == null) {
                throw new RuntimeException("Couldn't load image file [" + texturePath + "] "
                        + stbi_failure_reason());
            }

            int width = w.get();
            int height = h.get();

            // This will generate a texture in the gpu based on the Image buffer
            return generateTexture(width, height, bfr);
        }
    }
}
