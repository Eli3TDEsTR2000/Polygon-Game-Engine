package org.polygon.engine.core.scene.lights;

import org.joml.Vector3f;

public class Light {
    protected Vector3f color;
    protected float intensity;

    protected Light(Vector3f color, float intensity) {
        this.color = color;
        this.intensity = intensity;
    }

    protected Light() {
        this(new Vector3f(1.0f, 1.0f, 1.0f), 1.0f);
    }

    public Vector3f getColor() {
        return color;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public void setColor(float r, float g, float b) {
        color.set(r, g, b);
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }
}
