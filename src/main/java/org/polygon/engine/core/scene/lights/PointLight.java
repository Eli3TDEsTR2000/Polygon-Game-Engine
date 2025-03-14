package org.polygon.engine.core.scene;

import org.joml.*;

public class PointLight {
    private Attenuation attenuation;
    private Vector3f color;
    private Vector3f position;
    private float intensity;

    public PointLight(Vector3f color, Vector3f position, float intensity) {
        attenuation = new Attenuation(0, 0, 1);
        this.color = color;
        this.position = position;
        this.intensity = intensity;
    }

    public Attenuation getAttenuation() {
        return attenuation;
    }

    public Vector3f getColor() {
        return color;
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setAttenuation(Attenuation attenuation) {
        this.attenuation = attenuation;
    }

    public void setColor(float r, float g, float b) {
        color.set(r, g, b);
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public static class Attenuation {
        private float constant;
        private float linear;
        private float exponent;
        public Attenuation(float constant, float linear, float exponent) {
            this.constant = constant;
            this.linear = linear;
            this.exponent = exponent;
        }

        public float getConstant() {
            return constant;
        }

        public float getLinear() {
            return linear;
        }

        public float getExponent() {
            return exponent;
        }

        public void setConstant(float constant) {
            this.constant = constant;
        }

        public void setLinear(float linear) {
            this.linear = linear;
        }

        public void setExponent(float exponent) {
            this.exponent = exponent;
        }
    }
}
