package org.polygon.engine.level_editor;

import org.polygon.engine.core.IGameLogic;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.EngineRender;
import org.polygon.engine.core.scene.*;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.ModelLoader;

// Imports needed for raycasting
import org.joml.*;

import java.io.IOException;
import java.lang.Math;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

// Import Material and explicit Mesh
import org.polygon.engine.core.graph.Material;
import org.polygon.engine.core.graph.Mesh;

public class Editor implements IGameLogic {
    private final float MOUSE_SENSITIVITY = 0.1f;
    private final float MOVEMENT_SPEED = 0.005f;
    private Scene editorScene;
    private EditorGui editorGui;
    private boolean leftMouseButtonPressedLastFrame = false; // For click detection


    @Override
    public void init(Window window, EngineRender render) {
        editorScene = window.createScene();
        editorScene.getCamera().moveUp(1);
        window.setCurrentScene(editorScene);
        render.getPreRenders().add(new EndlessGridRender());
        editorGui = new EditorGui(editorScene, this);
        editorScene.setGuiInstance(editorGui);
    }

    @Override
    public void input(Window window, long diffTimeMS, boolean inputConsumed) {
        int factor = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT) ? 3 : 1;
        float incrementMovement = diffTimeMS * MOVEMENT_SPEED * factor;
        Camera camera = window.getCurrentScene().getCamera();
        if(window.isKeyPressed(GLFW_KEY_W)) {
            camera.moveForward(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_S)) {
            camera.moveBackward(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_A)) {
            camera.moveLeft(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_D)) {
            camera.moveRight(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_SPACE)) {
            camera.moveUp(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_V)) {
            camera.moveDown(incrementMovement);
        }


        // Saving and loading
        if(window.isKeyPressed(GLFW_KEY_S) && window.isKeyPressed(GLFW_KEY_LEFT_CONTROL)) {
            try {
                SceneSerialization.saveToFile("resources/levels/test.json", editorScene);
            } catch(IOException e) {
                System.err.println(e.getMessage());
            }
        }

        if(window.isKeyPressed(GLFW_KEY_L) && window.isKeyPressed(GLFW_KEY_LEFT_CONTROL)) {
            try {
                editorScene.cleanup();
                editorScene = SceneSerialization.loadFromFile("resources/levels/test.json", window.getWidth(), window.getHeight());
                window.setCurrentScene(editorScene);
            } catch(IOException e) {
                System.err.println(e.getMessage());
            }
            editorGui = new EditorGui(editorScene, this);
            editorScene.setGuiInstance(editorGui);
        }

        boolean guiConsumedInput = (editorGui != null && editorGui.handleGuiInput(window));

        if (!guiConsumedInput) {
            // Process camera rotation if right mouse button is pressed
            if(window.getMouseInputHandler().isRightButtonPressed()) {
                camera.addRotation(
                        (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().x * MOUSE_SENSITIVITY),
                        (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().y * MOUSE_SENSITIVITY));
            }

            // Process entity selection if left mouse button is clicked (use isLeftButtonReleased for click detection)
            boolean leftPressed = window.getMouseInputHandler().isLeftButtonPressed();
            if (leftPressed && !leftMouseButtonPressedLastFrame) { // Check for press event
                selectEntityByRaycast(window);
            }
            leftMouseButtonPressedLastFrame = leftPressed; // Store current state for next frame
        }
    }

    @Override
    public void update(Window window, long diffTimeMS) {

    }

    @Override
    public void cleanup() {
    }

    public boolean loadModelAndAddToScene(String modelId, String modelPath) {
        if (editorScene == null) {
            System.err.println("Cannot load model, editorScene is null.");
            return false;
        }
        try {
            System.out.println("Editor: Loading model '" + modelId + "' from: " + modelPath);
            Model loadedModel = ModelLoader.loadModel(modelId, modelPath, editorScene.getTextureCache(), false);

            if (loadedModel != null) {
                editorScene.addModel(loadedModel);
                System.out.println("Editor: Model '" + modelId + "' added to scene.");
                return true;
            } else {
                System.err.println("Editor: ModelLoader failed to load model: " + modelPath);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Editor: Exception while loading model '" + modelId + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Raycasting logic adapted from EntitySelectionTestScene
    public void selectEntityByRaycast(Window window) {
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        Scene currentScene = window.getCurrentScene();
        Camera camera = currentScene.getCamera();
        Vector2f mousePos = window.getMouseInputHandler().getCurrentPosition();

        // Don't raycast if mouse is outside window bounds (or handle edge cases if needed)
        if (mousePos.x < 0 || mousePos.x > windowWidth || mousePos.y < 0 || mousePos.y > windowHeight) {
            return;
        }

        // Normalized Device Coordinates
        float x = (2.0f * mousePos.x) / windowWidth - 1.0f;
        float y = 1.0f - (2.0f * mousePos.y) / windowHeight;
        float z = -1.0f; // Point into the screen

        // View Space
        Matrix4f invProjMatrix = currentScene.getProjection().getInvProjMatrix();
        Vector4f rayClip = new Vector4f(x, y, z, 1.0f);
        Vector4f rayEye = invProjMatrix.transform(rayClip);
        rayEye.z = -1.0f; // Point forward in view space
        rayEye.w = 0.0f; // Vector

        // World Space
        Matrix4f invViewMatrix = camera.getInvViewMatrix();
        Vector4f rayWorld = invViewMatrix.transform(rayEye);
        Vector3f mouseDir = new Vector3f(rayWorld.x, rayWorld.y, rayWorld.z).normalize();

        Vector4f min = new Vector4f();
        Vector4f max = new Vector4f();
        Vector2f nearFar = new Vector2f();

        Entity selectedEntity = null;
        float closestDistance = Float.POSITIVE_INFINITY;
        Vector3f cameraPos = camera.getPosition();

        Collection<Model> models = currentScene.getModelMap().values();
        Matrix4f modelMatrix = new Matrix4f(); // Reusable matrix
        for (Model model : models) {
            List<Entity> entities = model.getEntityList();
            for (Entity entity : entities) {
                // Get entity's world transform
                modelMatrix.set(entity.getModelMatrix()); // Use entity's pre-calculated matrix

                // Iterate through meshes to check bounding boxes
                for (Material material : model.getMaterialList()) {
                    for (Mesh mesh : material.getMeshList()) { // Use imported Mesh
                        Vector3f aabbMin = mesh.getAabbMinCorner();
                        Vector3f aabbMax = mesh.getAabbMaxCorner();

                        // Transform AABB corners to world space
                        // Note: Transforming AABB corners like this isn't perfectly accurate for rotated objects.
                        // A more robust method involves transforming the ray into object space or using OBB intersection.
                        // However, for simplicity matching the test scene, we use this.
                        Vector4f worldMin = modelMatrix.transform(new Vector4f(aabbMin, 1.0f));
                        Vector4f worldMax = modelMatrix.transform(new Vector4f(aabbMax, 1.0f));
                        
                        // Recalculate world AABB min/max after transformation (crude method)
                        float minX = Math.min(worldMin.x, worldMax.x);
                        float minY = Math.min(worldMin.y, worldMax.y);
                        float minZ = Math.min(worldMin.z, worldMax.z);
                        float maxX = Math.max(worldMin.x, worldMax.x);
                        float maxY = Math.max(worldMin.y, worldMax.y);
                        float maxZ = Math.max(worldMin.z, worldMax.z);

                        if (Intersectionf.intersectRayAab(cameraPos.x, cameraPos.y, cameraPos.z, mouseDir.x, mouseDir.y, mouseDir.z,
                                minX, minY, minZ, maxX, maxY, maxZ, nearFar) && nearFar.x < closestDistance) {
                            closestDistance = nearFar.x;
                            selectedEntity = entity;
                        }
                    }
                }
            }
        }

        currentScene.setSelectedEntity(selectedEntity);
        if(selectedEntity != null) {
             System.out.println("Selected Entity: " + selectedEntity.getEntityId());
        } else {
             System.out.println("Selected Nothing.");
        }
    }
}
