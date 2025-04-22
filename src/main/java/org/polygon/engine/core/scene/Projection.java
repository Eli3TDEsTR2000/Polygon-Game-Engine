package org.polygon.engine.core.scene;

import org.joml.Matrix4f;

public class Projection {
    private static final float DEFAULT_FOV = (float) Math.toRadians(60.0f);
    private static final float DEFAULT_Z_FAR = 1000.0f;
    private static final float DEFAULT_Z_NEAR = 0.01f;

    private Matrix4f projMatrix;
    private Matrix4f invProjMatrix;

    public Projection(int width, int height) {
        // Create and initialize a projection Matrix
        projMatrix = new Matrix4f();
        invProjMatrix = new Matrix4f();
        updateProjectionMatrix(width, height);
    }

    public Matrix4f getProjMatrix() {
        return projMatrix;
    }

    public Matrix4f getInvProjMatrix() {
        return invProjMatrix;
    }

    public void updateProjectionMatrix(int width, int height) {
        projMatrix.setPerspective(DEFAULT_FOV, (float) width / height, DEFAULT_Z_NEAR, DEFAULT_Z_FAR);
        invProjMatrix.set(projMatrix).invert();
    }
}
