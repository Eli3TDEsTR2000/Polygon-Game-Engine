package org.polygon.engine.core.scene;

import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.graph.TextureCache;

import java.util.HashMap;
import java.util.Map;

public class Scene {
    // Holds projection matrix and updating projection matrix logic
    Projection projection;
    // Holds all Models that are going to be rendered
    private Map<String, Model> modelMap;
    // Holds textures used in the scene
    private TextureCache textureCache;
    public Scene(int width, int height) {
        // Initialize the scene with empty Model map and a projection matrix
        modelMap = new HashMap<>();
        projection = new Projection(width, height);
        // Initialize the textureCache
        textureCache = new TextureCache();
    }

    public void cleanup() {
        // Removes VAO and VBO for each mesh
        modelMap.values().forEach(Model::cleanup);
    }

    public void resetScene() {
        // Removes VAO and VBO for each mesh and frees meshMap
        cleanup();
        modelMap = new HashMap<>();
    }

    public void addEntity(Entity entity) {
        String modelId = entity.getModelId();
        Model model = modelMap.get(modelId);
        if(model == null) {
            throw new RuntimeException("Couldn't find model [" + modelId +"] for entity ["
                    + entity.getEntityId() + "]");
        }
        model.getEntityList().add(entity);
    }

    public Map<String, Model> getModelMap() {
        return modelMap;
    }

    public void addModel(Model model) {
        modelMap.put(model.getModelId(), model);
    }

    public Projection getProjection() {
        return projection;
    }

    public TextureCache getTextureCache() {
        return textureCache;
    }

    public void resize(int width, int height) {
        // This method will be called each time the window is resized
        // updates projection matrix with the new window's with and height
        projection.updateProjectionMatrix(width, height);
    }
}
