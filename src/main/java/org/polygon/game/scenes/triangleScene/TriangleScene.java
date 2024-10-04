package org.polygon.game.scenes.triangleScene;

import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.scene.Scene;
import org.polygon.game.scenes.triangleScene.meshs.BasicTriangle;

public class TriangleScene {
    private static TriangleScene triangleScene = null;

    private TriangleScene() {

    }

    public static void initScene(Scene scene) {
        scene.initNewScene();
        BasicTriangle triangle = new BasicTriangle("Triangle-01", 1.0f);
        Mesh triangleMesh = new Mesh(triangle.getPositions(), 3);
        scene.addMesh(triangle.getId(), triangleMesh);
    }
}
