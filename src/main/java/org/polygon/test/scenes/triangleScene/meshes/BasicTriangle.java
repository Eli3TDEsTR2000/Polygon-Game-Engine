package org.polygon.test.scenes.triangleScene.meshes;

import org.polygon.test.scenes.BasicShape;

public class BasicTriangle extends BasicShape {
    public BasicTriangle(float scale) {
        verticesPositions = new float[] {
                0.0f, 0.5f, -1.0f,
                -0.5f, -0.5f, -1.0f,
                0.5f, -0.5f, -1.0f,
        };
        defaultColor = new float[] {
                1.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 1.0f,
        };
        indexArray = new int[] {
                0, 1, 2
        };

        setScale(scale);
    }
}
