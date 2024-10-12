package org.polygon.test.scenes;

public abstract class BasicShape {
    protected float[] verticesPositions;
    protected float[] defaultColor;
    protected int[] indexArray;
    public final float[] getVerticesPositions() {
        return verticesPositions;
    }
    public final float[] getDefaultColor() {
        return defaultColor;
    }
    public final int[] getIndexArray() {
        return indexArray;
    }

    protected void setScale(float scale) {
        for(int i = 0; i < verticesPositions.length; i++) {
            verticesPositions[i] *= scale;
        }
        // Set the default z coordinate for each vertex
        for(int i = 2; i < verticesPositions.length; i += 3) {
            verticesPositions[i] /= scale;
        }
    }
}
