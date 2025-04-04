package org.polygon.engine.core.scene.lights;

import org.joml.Vector3f;

public class DirectionalLight extends Light{
    private Vector3f direction;

    public DirectionalLight(Vector3f color, float intensity, Vector3f direction) {
        super(color, intensity);
        this.direction = direction;
    }

    public DirectionalLight(Vector3f direction) {
        this.direction = direction;
    }

    public DirectionalLight() {
        this.direction = new Vector3f(0, 0, 0);
    }

    public Vector3f getDirection() {
        return direction;
    }

    public void setDirection(Vector3f direction) {
        this.direction = direction;
    }

    public void setDirection(float x, float y, float z) {
        direction.set(x, y, z);
    }
}
