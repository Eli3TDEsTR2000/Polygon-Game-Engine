package org.polygon.engine.core.scene;

import org.polygon.engine.core.graph.Mesh;

import java.util.HashMap;
import java.util.Map;

public class Scene {
    private Map<String, Mesh> meshMap;
    public Scene() {
        meshMap = new HashMap<>();
    }

    public void cleanup() {
        meshMap.values().forEach(Mesh::cleanup);
    }

    public void initNewScene() {
        cleanup();
        meshMap = new HashMap<>();
    }

    public Map<String, Mesh> getMeshMap() {
        return meshMap;
    }

    public void addMesh(String meshId, Mesh mesh) {
        meshMap.put(meshId, mesh);
    }
}
