package org.polygon.engine.core.graph;

import org.polygon.engine.core.scene.Entity;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private final String modelId;
    private List<Entity> entityList;
    private List<Mesh> meshList;

    public Model(String modelId, List<Mesh> meshList) {
        this.modelId = modelId;
        this.meshList = meshList;
        entityList = new ArrayList<>();
    }

    public void cleanup() {
        meshList.forEach(Mesh::cleanup);
    }

    public List<Entity> getEntityList() {
        return entityList;
    }

    public List<Mesh> getMeshList() {
        return meshList;
    }

    public String getModelId() {
        return modelId;
    }
}
