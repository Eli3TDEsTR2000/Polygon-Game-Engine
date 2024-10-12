package org.polygon.test.scenes.quadScene;

import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.Scene;
import org.polygon.test.scenes.BasicScene;
import org.polygon.test.scenes.quadScene.meshes.BasicQuad;

import java.util.ArrayList;
import java.util.List;

public class QuadScene extends BasicScene {

    @Override
    public void initScene(Scene scene) {
        scene.resetScene();
        BasicQuad quad = new BasicQuad("Quad", 1.0f);
        Mesh quadMesh = new Mesh(quad.getVerticesPositions(), quad.getDefaultColor(), quad.getIndexArray());
        List<Mesh> meshList = new ArrayList<>();
        meshList.add(quadMesh);
        Model basicQuad = new Model("BasicQuad", meshList);
        scene.addModel(basicQuad);

        Entity quadEntity = new Entity("Quad-01", basicQuad.getModelId());
        quadEntity.setPosition(0, 0, 0);
        quadEntity.updateModelMatrix();
        scene.addEntity(quadEntity);
    }
}
