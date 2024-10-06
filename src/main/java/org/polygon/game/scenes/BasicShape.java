package org.polygon.game.scenes;

public abstract class BasicShape {
    protected float[] verticesPositions;
    protected float[] defaultColor;
    protected int[] indexArray;
    protected String shapeId;

    public final String getShapeId() {
        return shapeId;
    }
    public final float[] getVerticesPositions() {
        return verticesPositions;
    }
    public final float[] getDefaultColor() {
        return defaultColor;
    }
    public final int[] getIndexArray() {
        return indexArray;
    }
}
