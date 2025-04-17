package org.polygon.engine.core.scene;

import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.graph.TextureCache;

public class SkyBox {
    // The class loads the skybox model and hold an entity reference of the model to be rendered.
    private Model skyBoxModel;
    private Entity skyBoxEntity;

    public SkyBox(String skyBoxModelPath, TextureCache textureCache) {
        skyBoxModel = ModelLoader.loadModel("skybox-model", skyBoxModelPath, textureCache, false);
        skyBoxEntity = new Entity("skybox-entity", skyBoxModel.getModelId());
    }

    public Model getSkyBoxModel() {
        return skyBoxModel;
    }

    public Entity getSkyBoxEntity() {
        return skyBoxEntity;
    }
}
