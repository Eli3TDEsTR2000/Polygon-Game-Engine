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
        defaultColor = new float[]{
                0.5f, 0.0f, 0.0f,
                0.0f, 0.5f, 0.0f,
                0.0f, 0.0f, 0.5f,
                0.0f, 0.5f, 0.5f,
        };
        indexArray = new int[] {
                0, 1, 3, 3, 1, 2
        };
        this.shapeId = shapeId;

        // Scale quad
        for (int i = 0; i < verticesPositions.length; i++) {
            verticesPositions[i] *= scale;
        }

        // Set the default z coordinate for each vertex
        for(int i = 2; i < verticesPositions.length; i += 3) {
            verticesPositions[i] = -1.0f;
        }
    }
}
