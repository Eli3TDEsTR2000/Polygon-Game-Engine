package org.polygon.game.scenes.quadScene.meshes;

import org.polygon.game.scenes.BasicShape;

public class BasicQuad extends BasicShape {

    public BasicQuad(String shapeId, float scale) {
        verticesPositions = new float[] {
                -0.5f, 0.5f, 0.0f,
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                0.5f, 0.5f, 0.0f,
        };
        this.shapeId = shapeId;

        // Scale quad
        for (int i = 0; i < verticesPositions.length; i++) {
            verticesPositions[i] *= scale;
        }
    }

    @Override
    public String getShapeId() {
        return shapeId;
    }

    @Override
    public float[] getVerticesPositions() {
        return verticesPositions;
    }
}
