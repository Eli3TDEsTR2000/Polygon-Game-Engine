package org.polygon.test.scenes.quadScene;

import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.scene.Scene;
import org.polygon.test.scenes.BasicScene;
import org.polygon.test.scenes.quadScene.meshes.BasicQuad;

public class QuadScene extends BasicScene {

    @Override
    public void initScene(Scene scene) {
        scene.initNewScene();
        BasicQuad quad = new BasicQuad("Quad-01", 1.0f);
        Mesh quadMesh = new Mesh(quad.getVerticesPositions(), quad.getDefaultColor(), quad.getIndexArray());
        scene.addMesh(quad.getShapeId(), quadMesh);
    }
}