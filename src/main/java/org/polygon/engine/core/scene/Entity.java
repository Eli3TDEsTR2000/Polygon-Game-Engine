package org.polygon.engine.core.scene;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Entity {
    private final String entityId;
    private final String modelId;
    private Matrix4f modelMatrix;
    private Vector3f position;
    private Quaternionf rotation;
    private float scale;
    private AnimationData animationData;

    public Entity(String entityId, String modelId) {
        // entity object stores its ID and the referenced model ID in ordered to be rendered
        // TODO LATER - Implement onCreate() and onUpdate() methods to script classes inheriting Entity.
        this.entityId = entityId;
        this.modelId = modelId;
        modelMatrix = new Matrix4f();
        position = new Vector3f();
        rotation = new Quaternionf();
        scale = 1;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getModelId() {
        return modelId;
    }

    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public float getScale() {
        return scale;
    }
    public AnimationData getAnimationData() {
        return animationData;
    }

    public void setPosition(float x, float y, float z) {
        position.x = x;
        position.y = y;
        position.z = z;

        updateModelMatrix();
    }

    public void setRotation(float x, float y, float z, float angle) {
        rotation.fromAxisAngleRad(x, y, z, angle);

        updateModelMatrix();
    }

    public void setScale(float scale) {
        this.scale = scale;

        updateModelMatrix();
    }
    public void setAnimationData(AnimationData animationData) {
        this.animationData = animationData;
    }

    // We need to call this every time we edit the attributes of the entity
    private void updateModelMatrix() {
        modelMatrix.translationRotateScale(position, rotation, scale);
    }
}
