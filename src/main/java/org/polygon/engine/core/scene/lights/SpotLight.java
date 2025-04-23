package org.polygon.engine.core.scene.lights;

import org.joml.Vector3f;

public class SpotLight extends PointLight implements ILightHasRadius {
    private Vector3f coneDirection;
    private float cutOffAngle;
    private float cutOff;

    public SpotLight(Vector3f color, float intensity, Vector3f position, Vector3f coneDirection, float cutOffAngle) {
        super(color, intensity, position);
        this.coneDirection = coneDirection;
        setCutOffAngle(cutOffAngle);
    }

    public SpotLight(PointLight pointLight, Vector3f coneDirection, float cutOffAngle) {
        this(pointLight.color, pointLight.intensity, pointLight.position, coneDirection, cutOffAngle);
    }

    public SpotLight() {
        coneDirection = new Vector3f(0, 0, 0);
        setCutOffAngle(15.0f);
    }

    public Vector3f getConeDirection() {
        return coneDirection;
    }

    public float getCutOffAngle() {
        return cutOffAngle;
    }

    public float getCutOff() {
        return cutOff;
    }

    public void setConeDirection(Vector3f coneDirection) {
        this.coneDirection = coneDirection;
    }

    public void setConeDirection(float x, float y, float z) {
        coneDirection.set(x, y, z);
    }

    public void setCutOffAngle(float cutOffAngle) {
        this.cutOffAngle = cutOffAngle;
        cutOff = (float) Math.cos(Math.toRadians(cutOffAngle));
    }
}
