package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
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
    private final Vector3f aabbMinCorner;
    private final Vector3f aabbMaxCorner;

    public Model(String modelId, String modelPath, List<Material> materialList, List<Animation> animationList, boolean hasAnimation) {
        this.modelId = modelId;
        this.modelPath = modelPath;
        this.materialList = materialList;
        this.animationList = animationList;
        entityList = new ArrayList<>();
        this.hasAnimation = hasAnimation;
        aabbMinCorner = new Vector3f(Float.MAX_VALUE);
        aabbMaxCorner = new Vector3f(-Float.MAX_VALUE);
        computeBoundingBox();
    }

    private void computeBoundingBox() {
        boolean hasMesh = false;

        if(materialList != null) {
            for(Material material : materialList) {
                for(Mesh mesh : material.getMeshList()) {
                    Vector3f meshMin = mesh.getAabbMinCorner();
                    Vector3f meshMax = mesh.getAabbMaxCorner();

                    aabbMinCorner.x = Math.min(aabbMinCorner.x, meshMin.x);
                    aabbMinCorner.y = Math.min(aabbMinCorner.y, meshMin.y);
                    aabbMinCorner.z = Math.min(aabbMinCorner.z, meshMin.z);

                    aabbMaxCorner.x = Math.max(aabbMaxCorner.x, meshMax.x);
                    aabbMaxCorner.y = Math.max(aabbMaxCorner.y, meshMax.y);
                    aabbMaxCorner.z = Math.max(aabbMaxCorner.z, meshMax.z);

                    hasMesh = true;
                }
            }
        }

        if(!hasMesh) {
            aabbMinCorner.set(-0.5f);
            aabbMaxCorner.set(0.5f);
        }
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

    public Vector3f getAabbMinCorner() {
        return aabbMinCorner;
    }

    public Vector3f getAabbMaxCorner() {
        return aabbMaxCorner;
    }

    public boolean isAnimated() {
        return hasAnimation;
    }

    public record AnimatedFrame(Matrix4f[] boneMatrices) {}
    public record Animation(String name, double duration, List<AnimatedFrame> frames) {}
}
