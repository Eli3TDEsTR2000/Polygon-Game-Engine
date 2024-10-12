package org.polygon.test.scenes.triangleScene;

import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.Scene;
import org.polygon.test.scenes.BasicScene;
import org.polygon.test.scenes.triangleScene.meshes.BasicTriangle;

import java.util.ArrayList;
import java.util.List;

public class TriangleScene extends BasicScene {
    @Override
    public void initScene(Scene scene) {
        scene.resetScene();
        BasicTriangle triangle = new BasicTriangle("Triangle", 1.0f);
        Mesh triangleMesh = new Mesh(triangle.getVerticesPositions(),
                triangle.getDefaultColor(), triangle.getIndexArray());
        List<Mesh> meshList = new ArrayList<>();
        meshList.add(triangleMesh);
        Model basicTriangle = new Model("BasicTriangle", meshList);
        scene.addModel(basicTriangle);

        Entity triangleEntity = new Entity("Triangle-01", basicTriangle.getModelId());
        triangleEntity.setPosition(0, 0, 0);
        triangleEntity.updateModelMatrix();
        scene.addEntity(triangleEntity);
    }
}
