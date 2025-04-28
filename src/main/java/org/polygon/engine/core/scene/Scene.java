package org.polygon.engine.core.scene;

import org.polygon.engine.core.IGuiInstance;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.graph.TextureCache;
import org.polygon.engine.core.scene.lights.SceneLights;

import java.util.HashMap;
import java.util.Map;

public class Scene {
    // Holds projection matrix and updating projection matrix logic
    Projection projection;
    // Holds all Models that are going to be rendered
    private Map<String, Model> modelMap;
    // Scene objects can hold a reference to a selectedEntity used for game interactions.
    private Entity selectedEntity;
    // Holds textures used in the scene
    private TextureCache textureCache;
    // Holds the Scene's camera
    // Every Scene instance will hold a primary camera with control methods to navigate through the scene.
    // The primary camera holds the view matrix sent to the vertex shader controlling the viewport layout.
    // A more complex implementation of a camera system would need to extend this class, use the setCamera method.
    private Camera camera;
    // Holds the current GUI instance.
    private IGuiInstance guiInstance;
    // Holds the SceneLights that contains all the scene's light casters.
    private SceneLights sceneLights;
    // Holds the scene's skybox.
    private SkyBox skyBox;
    // Holds the scene's fog instance.
    private Fog fog;

    // Flag to bypass lighting in the shader.
    private boolean bypassLighting;

    public Scene(int width, int height) {
        // Initialize the scene with empty Model map and a projection matrix
        modelMap = new HashMap<>();
        projection = new Projection(width, height);
        // Initialize the textureCache
        textureCache = new TextureCache();
        // Initialize Scene's camera
        camera = new Camera();
        // Initialize the scene's fog.
        fog = new Fog();

        bypassLighting = false;
    }

    public void cleanup() {
        // Removes VAO and VBO for each mesh
        modelMap.values().forEach(Model::cleanup);
        textureCache.cleanup();
        sceneLights = null;
        if(skyBox != null) {
            skyBox.cleanup();
        }
    }

    public void reset() {
        // Removes VAO and VBO for each mesh and reset modelMap and textureCache.
        // Ideal for level resets.
        cleanup();
        modelMap = new HashMap<>();
        textureCache = new TextureCache();
        camera = new Camera();
        bypassLighting = false;
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

    public Entity getSelectedEntity() {
        return selectedEntity;
    }

    public Projection getProjection() {
        return projection;
    }

    public TextureCache getTextureCache() {
        return textureCache;
    }

    public Camera getCamera() {
        return camera;
    }

    public IGuiInstance getGuiInstance() {
        return guiInstance;
    }

    public SceneLights getSceneLights() {
        return sceneLights;
    }
    public SkyBox getSkyBox() {
        return skyBox;
    }
    public Fog getFog() {
        return fog;
    }

    public boolean isLightingDisabled() {
        return bypassLighting;
    }

    public void setSelectedEntity(Entity selectedEntity) {
        this.selectedEntity = selectedEntity;
    }

    public void setGuiInstance(IGuiInstance guiInstance) {
        this.guiInstance = guiInstance;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }
    public void setSkyBox(SkyBox skyBox) {
        this.skyBox = skyBox;
    }

    public void setSceneLights(SceneLights sceneLights) {
        this.sceneLights = sceneLights;
    }
    public void setFog(Fog fog) {
        this.fog = fog;
    }

    public void setBypassLighting(boolean bool) {
        bypassLighting = bool;
    }

    public void addModel(Model model) {
        modelMap.put(model.getModelId(), model);
    }

    public void resize(int width, int height) {
        // This method will be called each time the window is resized
        // updates projection matrix with the new window's with and height
        projection.updateProjectionMatrix(width, height);
    }
}
