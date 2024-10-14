package org.polygon.test.scenes;

import org.polygon.engine.core.graph.Mesh;

public abstract class BasicShape {
    protected float[] verticesPositions;
    protected float[] defaultTextCoords;
    protected int[] indexArray;
    protected Mesh mesh;

    protected BasicShape() {
        initShape();
        mesh = new Mesh(verticesPositions, defaultTextCoords, indexArray);
    }

    protected abstract void initShape();
    public final float[] getVerticesPositions() {
        return verticesPositions;
    }
    public final float[] getDefaultTextCoords() {
        return defaultTextCoords;
    }
    public final int[] getIndexArray() {
        return indexArray;
    }

    public final Mesh getMesh() {
        return mesh;
    }
}
