package org.polygon.game.scenes.triangleScene.meshs;

public class BasicTriangle {
    private float[] verticesPositions = new float[] {
            0.0f, 0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
    };
    private String Id;
    public BasicTriangle(String Id, float scale) {
        this.Id = Id;

        // Set triangle scale
        for(int i = 0; i < verticesPositions.length; i++) {
            verticesPositions[i] *= scale;
        }
    }

    public String getId() {
        return Id;
    }

    public float[] getPositions() {
        return verticesPositions;
    }
}
