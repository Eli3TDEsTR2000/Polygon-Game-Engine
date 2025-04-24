package org.polygon.engine.core.graph;

import org.polygon.engine.core.Window;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL40.*;

public class SceneFBO {

    private int fboId = -1;
    private int textureId = -1;
    private int depthRenderBufferId = -1;
    private int width;
    private int height;

    public SceneFBO(Window window) {
        this.width = window.getWidth();
        this.height = window.getHeight();
        createFramebuffer();

        window.addFrameBufferSizeCallback(this::handleResize);
    }

    private void createFramebuffer() {
        // Create FBO
        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        // Create Color Texture Attachment
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE
                , (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        // Create Depth Renderbuffer Attachment
        depthRenderBufferId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBufferId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderBufferId);

        // Set the list of draw buffers.
        glDrawBuffers(GL_COLOR_ATTACHMENT0);

        // Check FBO status
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("ERROR creating SceneFBO! Status: "
                    + glCheckFramebufferStatus(GL_FRAMEBUFFER));
        }

        // Unbind
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void handleResize(long windowHandle, int newWidth, int newHeight) {
        if (this.width == newWidth && this.height == newHeight) {
            return; // No change needed
        }
        this.width = newWidth;
        this.height = newHeight;

        // Resize Texture
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glBindTexture(GL_TEXTURE_2D, 0);

        // Resize Depth Renderbuffer
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBufferId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }

    public void cleanup() {
        if (fboId != -1) {
            glDeleteFramebuffers(fboId);
            fboId = -1;
        }
        if (textureId != -1) {
            glDeleteTextures(textureId);
            textureId = -1;
        }
        if (depthRenderBufferId != -1) {
            glDeleteRenderbuffers(depthRenderBufferId);
            depthRenderBufferId = -1;
        }
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTextureId() { return textureId; }
    public int getFboId() { return fboId; }
} 