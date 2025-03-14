package org.polygon.engine.core.scene.lights;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SceneLights {
    private AmbientLight ambientLight;
    private DirectionalLight directionalLight;
    private List<PointLight> pointLightList;
    private List<SpotLight> spotLightList;

    public SceneLights() {
        ambientLight = new AmbientLight();
        directionalLight = new DirectionalLight(new Vector3f(0, 1, 0));
        pointLightList = new ArrayList<>();
        spotLightList = new ArrayList<>();
    }

    public AmbientLight getAmbientLight() {
        return ambientLight;
    }

    public DirectionalLight getDirectionalLight() {
        return directionalLight;
    }

    public List<PointLight> getPointLightList() {
        return pointLightList;
    }

    public List<SpotLight> getSpotLightList() {
        return spotLightList;
    }

    public void setPointLightList(List<PointLight> pointLightList) {
        this.pointLightList = pointLightList;
    }

    public void setSpotLightList(List<SpotLight> spotLightList) {
        this.spotLightList = spotLightList;
    }
}
