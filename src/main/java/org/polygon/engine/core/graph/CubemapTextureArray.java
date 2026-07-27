package org.polygon.engine.core.graph;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL40.*;

public class CubemapTextureArray {
    private final int[] ids;

    public CubemapTextureArray(int numCubemaps, int resolution) {
        ids = new int[numCubemaps];
        glGenTextures(ids);

        int previousActiveTextureUnit = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);

        for(int i = 0; i < numCubemaps; i++) {
            glBindTexture(GL_TEXTURE_CUBE_MAP, ids[i]);
            for(int face = 0; face < 6; face++) {
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, 0, GL_DEPTH_COMPONENT32F
                        , resolution, resolution, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
            }
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_COMPARE_MODE, GL_NONE);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        }

        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
        glActiveTexture(previousActiveTextureUnit);
    }

    public void cleanup() {
        for(int id : ids) {
            glDeleteTextures(id);
        }
    }

    public int[] getIds() {
        return ids;
    }
}
