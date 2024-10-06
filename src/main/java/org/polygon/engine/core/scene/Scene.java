package org.polygon.engine.core.scene;

import org.polygon.engine.core.graph.Mesh;

import java.util.HashMap;
import java.util.Map;

public class Scene {
    // Holds projection matrix and updating projection matrix logic
    Projection projection;
    // Holds all Meshes that are going to be rendered
    private Map<String, Mesh> meshMap;
    public Scene(int width, int height) {
        // Initialize the Mesh map and projection matrix
        meshMap = new HashMap<>();
        projection = new Projection(width, height);
    }

    public void cleanup() {
        // Removes VAO and VBO for each mesh
        meshMap.values().forEach(Mesh::cleanup);
    }

    public void initNewScene() {
        // Removes VAO and VBO for each mesh and frees meshMap
        cleanup();
        meshMap = new HashMap<>();
    }

    public Map<String, Mesh> getMeshMap() {
        return meshMap;
    }

    public void addMesh(String meshId, Mesh mesh) {
        meshMap.put(meshId, mesh);
    }

    public Projection getProjection() {
        return projection;
    }

    public void resize(int width, int height) {
        // This method will be called each time the window is resized
        // updates projection matrix with the new window's with and height
        projection.updateProjectionMatrix(width, height);
    }
}
