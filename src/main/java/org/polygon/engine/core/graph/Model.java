package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.polygon.engine.core.scene.Entity;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private final String modelId;
    private final String modelPath;
    private List<Entity> entityList;
    private List<Material> materialList;
    private List<Animation> animationList;
    private boolean hasAnimation;

    public Model(String modelId, String modelPath, List<Material> materialList, List<Animation> animationList, boolean hasAnimation) {
        this.modelId = modelId;
        this.modelPath = modelPath;
        this.materialList = materialList;
        this.animationList = animationList;
        entityList = new ArrayList<>();
        this.hasAnimation = hasAnimation;
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

    public String getModelPath() {
        return modelPath;
    }

    public List<Animation> getAnimationList() {
        return animationList;
    }

    public boolean isAnimated() {
        return hasAnimation;
    }

    public record AnimatedFrame(Matrix4f[] boneMatrices) {}
    public record Animation(String name, double duration, List<AnimatedFrame> frames) {}
}
