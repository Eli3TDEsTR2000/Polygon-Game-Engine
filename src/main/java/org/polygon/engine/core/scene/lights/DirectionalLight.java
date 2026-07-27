package org.polygon.engine.core.scene.lights;

import org.joml.Vector3f;

public class DirectionalLight extends Light{
    private Vector3f direction;

    public static final Vector3f DEFAULT_DIRECTION = new Vector3f(0.0001f, 1.0f, 0.0001f);

    public DirectionalLight(Vector3f color, float intensity, Vector3f direction) {
        super(color, intensity);
        this.direction = new Vector3f(DEFAULT_DIRECTION);
        setDirection(direction);
    }

    public DirectionalLight(Vector3f direction) {
        this.direction = new Vector3f(DEFAULT_DIRECTION);
        setDirection(direction);
    }

    public DirectionalLight() {
        this.direction = new Vector3f(DEFAULT_DIRECTION);
    }

    public Vector3f getDirection() {
        return direction;
    }

    public void setDirection(Vector3f direction) {
        setDirection(direction.x, direction.y, direction.z);
    }

    public void setDirection(float x, float y, float z) {
        if(x == 0.0f && z == 0.0f) {
            x = 0.0001f;
            z = 0.0001f;
        }
        direction.set(x, y, z);
    }
}
