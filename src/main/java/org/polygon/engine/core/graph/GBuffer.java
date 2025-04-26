package org.polygon.engine.core.graph;

import org.lwjgl.system.MemoryStack;
import org.polygon.engine.core.Window;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL40.*;

public class GBuffer {
    public static final int GBUFFER_IDX_ALBEDO = 0;
    public static final int GBUFFER_IDX_NORMAL = 1;
    public static final int GBUFFER_IDX_MATERIAL = 2;
    public static final int GBUFFER_IDX_EMISSIVE = 3;
    public static final int GBUFFER_IDX_DEPTH = 4;
    private final static int TOTAL_TEXTURES = 5;

    private int gBufferId = -1;
    private int[] textureIds = null;
    private int width;
    private int height;

    public GBuffer(Window window) {
        this.width = window.getWidth();
        this.height = window.getHeight();
        createBuffers();

        // Add the resize callback
        window.addFrameBufferSizeCallback(this::handleResize);
    }

    private void createBuffers() {
        gBufferId = glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, gBufferId);

        textureIds = new int[TOTAL_TEXTURES];
        glGenTextures(textureIds);

        // Initialize textures
        setupTextures(this.width, this.height);

        // Attach textures and setup draw buffers
        attachAndConfigure();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void setupTextures(int newWidth, int newHeight) {
         for(int i = 0; i < TOTAL_TEXTURES; i++) {
            glBindTexture(GL_TEXTURE_2D, textureIds[i]);

            switch (i) {
                case GBUFFER_IDX_ALBEDO:
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, newWidth, newHeight, 0
                            , GL_RGBA, GL_FLOAT, (ByteBuffer) null);
                    break;
                case GBUFFER_IDX_NORMAL:
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, newWidth, newHeight, 0
                            , GL_RGBA, GL_FLOAT, (ByteBuffer) null);
                    break;
                case GBUFFER_IDX_MATERIAL:
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, newWidth, newHeight
                            , 0, GL_RGB, GL_FLOAT, (ByteBuffer) null);
                    break;
                case GBUFFER_IDX_EMISSIVE:
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, newWidth, newHeight
                            , 0, GL_RGB, GL_FLOAT, (ByteBuffer) null);
                    break;
                case GBUFFER_IDX_DEPTH:
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, newWidth, newHeight
                            , 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
                    break;
            }

             glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
             glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
             glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
             glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
    }

    private void attachAndConfigure() {
         for(int i = 0; i < TOTAL_TEXTURES; i++) {
            int attachmentType;
            if (i == GBUFFER_IDX_DEPTH) {
                attachmentType = GL_DEPTH_ATTACHMENT;
            } else {
                attachmentType = GL_COLOR_ATTACHMENT0 + i;
            }
            glFramebufferTexture2D(GL_FRAMEBUFFER, attachmentType, GL_TEXTURE_2D, textureIds[i], 0);
         }

         try(MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer drawBuffers = stack.mallocInt(TOTAL_TEXTURES - 1);
            for(int i = 0; i < TOTAL_TEXTURES - 1; i++) {
                drawBuffers.put(i, GL_COLOR_ATTACHMENT0 + i);
            }
            glDrawBuffers(drawBuffers);
         }

         if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
             throw new IllegalStateException("ERROR: GBuffer Framebuffer is not complete after attach/configure!");
         }
    }

    private void handleResize(long windowHandle, int newWidth, int newHeight) {
        this.width = newWidth;
        this.height = newHeight;

        setupTextures(newWidth, newHeight);
    }


    public void cleanup() {
        if (gBufferId != -1) {
            glDeleteFramebuffers(gBufferId);
            gBufferId = -1;
        }
        if (textureIds != null) {
            glDeleteTextures(textureIds);
            textureIds = null;
        }
    }
    
    public int getGBufferId() { return gBufferId; }
    public int[] getTextureIds() { return textureIds; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
