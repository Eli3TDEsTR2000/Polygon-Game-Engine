package org.polygon.game.scenes.triangleScene.meshes;

import org.polygon.game.scenes.BasicShape;

public class BasicTriangle extends BasicShape {
    public BasicTriangle(String shapeId, float scale) {
        verticesPositions = new float[] {
                0.0f, 0.5f, 0.0f,
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
        };
        defaultColor = new float[] {
                1.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 1.0f,
        };
        indexArray = new int[] {
                0, 1, 2
        };
        this.shapeId = shapeId;

        // Set triangle scale
        for(int i = 0; i < verticesPositions.length; i++) {
            verticesPositions[i] *= scale;
        }
    }
}
