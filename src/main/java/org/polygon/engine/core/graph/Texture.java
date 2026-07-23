package org.polygon.engine.core.graph;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {
    private int textureId;
    private String texturePath;

    public static final Texture BRDF_LUT = new Texture("resources/textures/brdf.png");

    public Texture(int width, int height, ByteBuffer bfr) {
        texturePath = "";
        generateTexture(width, height, bfr);
    }

    public Texture(String texturePath) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            this.texturePath = texturePath;
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
            generateTexture(width, height, bfr);

            // We then need to free the byte buffer
            stbi_image_free(bfr);
        }
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void cleanup() {
        glDeleteTextures(textureId);
    }

    private void generateTexture(int width, int height, ByteBuffer bfr) {
        // Generate a texture in the GPU
        textureId = glGenTextures();
        // Bind that texture
        glBindTexture(GL_TEXTURE_2D, textureId);
        // Since the channels for the image is one byte each, the param is 1 (RGBA).
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        // TODO - HARDCODED MAX_ANISOTROPY for ANISOTROPIC FILTERING, rework when implementing graphics settings
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY, 16.0f);
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
    }
}
