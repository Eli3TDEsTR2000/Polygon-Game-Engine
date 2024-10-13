package org.polygon.engine.core.graph;

import org.polygon.engine.core.scene.Entity;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private final String modelId;
    private List<Entity> entityList;
    private List<Material> materialList;

    public Model(String modelId, List<Material> materialList) {
        this.modelId = modelId;
        this.materialList = materialList;
        entityList = new ArrayList<>();
    }

    public void cleanup() {
        materialList.forEach(Material::cleanup);
    }

    public List<Entity> getEntityList() {
        return entityList;
    }

    public List<Material> getMaterialList() {
        return materialList;
    }

    public String getModelId() {
        return modelId;
    }
}
