package org.polygon.test.scenes.quadScene.meshes;

import org.polygon.test.scenes.BasicShape;

public class BasicQuad extends BasicShape {

    public BasicQuad(float scale) {
        verticesPositions = new float[] {
                -0.5f, 0.5f, -1.0f,
                -0.5f, -0.5f, -1.0f,
                0.5f, -0.5f, -1.0f,
                0.5f, 0.5f, -1.0f,
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

        setScale(scale);
    }
}
