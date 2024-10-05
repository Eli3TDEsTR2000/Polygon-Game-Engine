package org.polygon.game.scenes.quadScene;

import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.scene.Scene;
import org.polygon.game.scenes.BasicScene;
import org.polygon.game.scenes.quadScene.meshes.BasicQuad;

public class QuadScene extends BasicScene {

    @Override
    public void initScene(Scene scene) {
        scene.initNewScene();
        float[] colors = new float[]{
                0.5f, 0.0f, 0.0f,
                0.0f, 0.5f, 0.0f,
                0.0f, 0.0f, 0.5f,
                0.0f, 0.5f, 0.5f,
        };
        int[] indexArray = new int[] {
                0, 1, 3, 3, 1, 2,
        };
        BasicQuad quad = new BasicQuad("Quad-01", 1.0f);
        Mesh quadMesh = new Mesh(quad.getVerticesPositions(), colors, indexArray);
        scene.addMesh(quad.getShapeId(), quadMesh);
    }
}
