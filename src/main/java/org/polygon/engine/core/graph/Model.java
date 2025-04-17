package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.Joint;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private final String modelId;
    private List<Entity> entityList;
    private List<Material> materialList;
    private List<Animation> animationList;

    public Model(String modelId, List<Material> materialList, List<Animation> animationList) {
        this.modelId = modelId;
        this.materialList = materialList;
        this.animationList = animationList;
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

    public List<Animation> getAnimationList() {
        return animationList;
    }

    public record AnimatedFrame(Matrix4f[] boneMatrices) {}
    public record Animation(String name, double duration, List<AnimatedFrame> frames) {}
}
