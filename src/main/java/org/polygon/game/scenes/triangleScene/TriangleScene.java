package org.polygon.game.scenes.triangleScene;

import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.scene.Scene;
import org.polygon.game.scenes.BasicScene;
import org.polygon.game.scenes.triangleScene.meshes.BasicTriangle;

public class TriangleScene extends BasicScene {
    @Override
    public void initScene(Scene scene) {
        scene.initNewScene();
        BasicTriangle triangle = new BasicTriangle("Triangle-01", 1.0f);
        Mesh triangleMesh = new Mesh(triangle.getVerticesPositions(),
                triangle.getDefaultColor(), triangle.getIndexArray());
        scene.addMesh(triangle.getShapeId(), triangleMesh);
    }
}
