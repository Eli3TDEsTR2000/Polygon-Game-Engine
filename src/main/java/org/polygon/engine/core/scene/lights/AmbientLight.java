package org.polygon.engine.core.scene.lights;

import org.joml.Vector3f;

public class AmbientLight extends Light{
    public AmbientLight(Vector3f color, float intensity) {
        super(color, intensity);
    }

    public AmbientLight() {

    }
}
