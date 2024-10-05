package org.polygon.game.scenes.quadScene;

import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.scene.Scene;
import org.polygon.game.scenes.quadScene.meshes.BasicQuad;

public abstract class QuadScene {
    private QuadScene() {

    }

    public static void initScene(Scene scene) {
        scene.initNewScene();
        BasicQuad quad = new BasicQuad("Quad-01", 1.0f);
        Mesh quadMesh = new Mesh(quad.getVerticesPositions(), 6);
        scene.addMesh(quad.getShapeId(), quadMesh);
    }
}
