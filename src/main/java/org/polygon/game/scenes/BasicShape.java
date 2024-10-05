package org.polygon.game.scenes;

public abstract class BasicShape {
    protected float[] verticesPositions;
    protected String shapeId;

    public abstract String getShapeId();
    public abstract float[] getVerticesPositions();
}
