package org.polygon.game.scenes.triangleScene;

import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.scene.Scene;
import org.polygon.game.scenes.triangleScene.meshs.BasicTriangle;

public class MainScene {
    private Scene scene;
    private static MainScene mainScene = null;

    private MainScene() {
        BasicTriangle triangle = new BasicTriangle("Triangle-01", 1.0f);
        Mesh triangleMesh = new Mesh(triangle.getPositions(), 3);
        scene = new Scene();
        scene.addMesh(triangle.getId(), triangleMesh);
    }

    public static MainScene get() {
        if(mainScene == null) {
            mainScene = new MainScene();
        }

        return mainScene;
    }

    public Scene getScene() {
        return scene;
    }
}
