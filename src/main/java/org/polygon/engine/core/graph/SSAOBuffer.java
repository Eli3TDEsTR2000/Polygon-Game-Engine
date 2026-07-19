package org.polygon.engine.core.graph;

import org.lwjgl.system.MemoryStack;
import org.polygon.engine.core.Window;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL40.*;

public class SSAOBuffer {
    private int ssaoFboId = -1;
    private int ssaoTextureId = -1;

    private int blurFboId = -1;
    private int blurTextureId = -1;

    private int fallbackWhiteTextureId = -1;

    private int width;
    private int height;

    public SSAOBuffer(Window window) {
        this.width = window.getWidth();
        this.height = window.getHeight();
        createBuffers();

        window.addFrameBufferSizeCallback(this::handleResize);

        fallbackWhiteTextureId = createFallbackWhiteTexture();
    }

    private int createTargetTexture(int width, int height){
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, width, height, 0, GL_RED, GL_UNSIGNED_BYTE
                , (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        return textureId;
    }

    private void createBuffers() {
        ssaoFboId = glGenFramebuffers();
        ssaoTextureId = createTargetTexture(width, height);
        glBindFramebuffer(GL_FRAMEBUFFER, ssaoFboId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ssaoTextureId, 0);

        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("ERROR: SSAO Framebuffer is not complete!");
        }

        blurFboId = glGenFramebuffers();
        blurTextureId = createTargetTexture(width, height);
        glBindFramebuffer(GL_FRAMEBUFFER, blurFboId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, blurTextureId, 0);

        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("ERROR: SSAO Blur Framebuffer is not complete!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void handleResize(long windowHandle, int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;

        glBindTexture(GL_TEXTURE_2D, ssaoTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, newWidth, newHeight, 0, GL_RED, GL_UNSIGNED_BYTE
                , (ByteBuffer) null);

        glBindTexture(GL_TEXTURE_2D, blurTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, newWidth, newHeight, 0, GL_RED, GL_UNSIGNED_BYTE
                , (ByteBuffer) null);
    }

    private int createFallbackWhiteTexture() {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        try(MemoryStack stack  = MemoryStack.stackPush()) {
            ByteBuffer whitePixel = stack.malloc(1);
            whitePixel.put((byte) 255).flip();
            glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, 1, 1, 0, GL_RED, GL_UNSIGNED_BYTE, whitePixel);
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        return textureId;
    }

    public void cleanup() {
        if(ssaoFboId != -1) {
            glDeleteFramebuffers(ssaoFboId);
            ssaoFboId = -1;
        }
        if(blurFboId != -1) {
            glDeleteFramebuffers(blurFboId);
            blurFboId = -1;
        }
        if(ssaoTextureId != -1) {
            glDeleteTextures(ssaoTextureId);
            ssaoTextureId = -1;
        }
        if(blurTextureId != -1) {
            glDeleteTextures(blurTextureId);
            blurTextureId = -1;
        }
        if(fallbackWhiteTextureId != -1) {
            glDeleteTextures(fallbackWhiteTextureId);
            fallbackWhiteTextureId = -1;
        }
    }

    public int getSSAOFramebufferId() {
        return ssaoFboId;
    }

    public int getSSAOTextureId() {
        return ssaoTextureId;
    }

    public int getBlurFramebufferId() {
        return blurFboId;
    }

    public int getBlurTextureId() {
        return blurTextureId;
    }

    public int getFallbackWhiteTextureId() {
        return fallbackWhiteTextureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
