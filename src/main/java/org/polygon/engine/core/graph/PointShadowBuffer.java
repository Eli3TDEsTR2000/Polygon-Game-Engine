package org.polygon.engine.core.graph;

import static org.lwjgl.opengl.GL40.*;

public class PointShadowBuffer {
    public static final int MAX_POINT_LIGHT_SHADOWS = 4;
    public static final int SHADOW_CUBEMAP_RESOLUTION = 1024;

    private final CubemapTextureArray depthCubemaps;
    private final int depthMapFBO;

    public PointShadowBuffer() {
        depthMapFBO = glGenFramebuffers();
        depthCubemaps = new CubemapTextureArray(MAX_POINT_LIGHT_SHADOWS, SHADOW_CUBEMAP_RESOLUTION);

        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT
                , GL_TEXTURE_CUBE_MAP_POSITIVE_X, depthCubemaps.getIds()[0], 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Could not create PointShadowBuffer's framebuffer");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void cleanup() {
        glDeleteFramebuffers(depthMapFBO);
        depthCubemaps.cleanup();
    }

    public int getDepthMapFBO() {
        return depthMapFBO;
    }

    public CubemapTextureArray getDepthCubemaps() {
        return depthCubemaps;
    }

    public void bindTextures(int start) {
        int[]ids = depthCubemaps.getIds();
        for(int i = 0; i < MAX_POINT_LIGHT_SHADOWS; i++) {
            glActiveTexture(start + i);
            glBindTexture(GL_TEXTURE_CUBE_MAP, ids[i]);
        }
    }
}
