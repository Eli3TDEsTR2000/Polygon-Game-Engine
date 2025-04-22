package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;
import org.polygon.engine.core.Window;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL40.*;

public class GBuffer {
    private final static int TOTAL_TEXTURES = 4;

    private int gBufferId;
    private int[] textureIds;
    private int width;
    private int height;

    public GBuffer(Window window) {
        gBufferId = glGenFramebuffers();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, gBufferId);

        textureIds = new int[TOTAL_TEXTURES];
        glGenTextures(textureIds);

        this.width = window.getWidth();
        this.height = window.getHeight();

        for(int i = 0; i < TOTAL_TEXTURES; i++) {
            glBindTexture(GL_TEXTURE_2D, textureIds[i]);
            int attachmentType;
            if(i == TOTAL_TEXTURES - 1) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, width, height
                        , 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
                attachmentType = GL_DEPTH_ATTACHMENT;
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height
                        , 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
                attachmentType = GL_COLOR_ATTACHMENT0 + i;
            }
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glFramebufferTexture2D(GL_FRAMEBUFFER, attachmentType, GL_TEXTURE_2D, textureIds[i], 0);
        }

        try(MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer intBuffer = stack.mallocInt(TOTAL_TEXTURES);
            for(int i = 0; i < TOTAL_TEXTURES; i++) {
                intBuffer.put(i, GL_COLOR_ATTACHMENT0 + i);
            }
            glDrawBuffers(intBuffer);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void cleanup() {
        glDeleteFramebuffers(gBufferId);
        Arrays.stream(textureIds).forEach(GL40::glDeleteBuffers);
    }

    public int getGBufferId() {
        return gBufferId;
    }

    public int[] getTextureIds() {
        return textureIds;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
