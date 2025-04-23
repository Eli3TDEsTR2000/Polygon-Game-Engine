package org.polygon.engine.core.scene.lights;

import org.joml.Vector3f;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;

public class PointLight extends Light implements ILightHasRadius {
    protected Attenuation attenuation;
    protected Vector3f position;
    protected float radius;

    public PointLight(Vector3f color, float intensity, Vector3f position) {
        super(color, intensity);
        attenuation = new Attenuation(this, 0, 0, 1);
        this.position = position;
        calculateRadius();
    }

    public PointLight() {
        attenuation = new Attenuation(this, 0, 0, 1);
        this.position = new Vector3f(0, 0, 0);
        calculateRadius();
    }

    public Attenuation getAttenuation() {
        return attenuation;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setAttenuation(Attenuation attenuation) {
        this.attenuation = attenuation;
        if (this.attenuation != null) {
            this.attenuation.setOwner(this);
        }
        calculateRadius();
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public void calculateRadius() {
        float lightMax = max(max(color.x, color.y), color.z);

        Attenuation att = getAttenuation();

        if (att == null) {
             this.radius = 0.0f;
             return;
        }

        float constant = att.getConstant();
        float linear = att.getLinear();
        float quadratic = att.getExponent();

        if (quadratic <= 0) {
            this.radius = Float.MAX_VALUE;
            return;
        }

        float delta = linear * linear - 4.0f * quadratic * (constant - lightMax * (256.0f / 5.0f));

        if (delta < 0.0f) {
            this.radius = 0.0f;
        } else {
            this.radius = (float)(-linear + sqrt(delta)) / (2.0f * quadratic);
        }
    }

    @Override
    public void setColor(Vector3f color) {
        super.setColor(color);
        calculateRadius();
    }

    @Override
    public void setColor(float r, float g, float b) {
        super.setColor(r, g, b);
        calculateRadius();
    }

    public static class Attenuation {
        private float constant;
        private float linear;
        private float exponent;
        private ILightHasRadius owner;

        public Attenuation(ILightHasRadius owner, float constant, float linear, float exponent) {
            this.owner = owner;
            this.constant = constant;
            this.linear = linear;
            this.exponent = exponent;
        }

        public void setOwner(ILightHasRadius owner) {
            this.owner = owner;
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
            if (owner != null) owner.calculateRadius();
        }

        public void setLinear(float linear) {
            this.linear = linear;
            if (owner != null) owner.calculateRadius();
        }

        public void setExponent(float exponent) {
            this.exponent = exponent;
            if (owner != null) owner.calculateRadius();
        }
    }
}
