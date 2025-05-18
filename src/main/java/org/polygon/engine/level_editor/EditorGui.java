package org.polygon.engine.level_editor;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import org.joml.Vector3f;
import org.polygon.engine.core.IGuiInstance;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.lights.*;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.Entity;
import imgui.type.ImString;
import imgui.type.ImInt;
import org.joml.*;
import org.polygon.engine.core.graph.TextureCache;
import org.polygon.engine.core.graph.Texture;
import org.polygon.engine.core.graph.Material;

import java.util.ArrayList;
import java.util.List;

// Import necessary classes (will add Model, Entity, ImString etc. later)

// Import static GL functions
import static org.lwjgl.opengl.GL11.glDeleteTextures;
// Explicitly import java.lang.Math to resolve ambiguity
import java.lang.Math;

public class EditorGui implements IGuiInstance {

    private final Scene scene;
    private final Editor editor;
    private boolean sceneLightsExist = false; // Track if SceneLights have been added
    private String title = "Editor Controls";
    private boolean isWindowHovered = false;
    private float[] ambientColor = {0.0f, 0.0f, 0.0f};
    private float[] ambientIntensity = {0.0f};
    private float[] dirColor = {0.0f, 0.0f, 0.0f};
    private float[] dirIntensity = {0.0f};
    private float[] dirDirection = {0.0f, 0.0f, 0.0f};
    private ImBoolean bypassLightingActive = new ImBoolean(false);

    // Flags for adding lights
    private boolean addPointLightFlag = false;
    private boolean addSpotLightFlag = false;

    // Lists to manage light removal
    private List<Integer> pointLightsToRemoveIndices = new ArrayList<>();
    private List<Integer> spotLightsToRemoveIndices = new ArrayList<>();

    // SkyBox State
    private boolean importSkyBoxFlag = false;
    private boolean removeSkyBoxFlag = false;
    private String currentSkyBoxPath = "None";
    private ImString skyboxPathInput = new ImString(256);

    // Model and Entity State
    private ImString modelIdInput = new ImString(128);
    private ImString modelPathInput = new ImString(256);
    private ImString entityIdInput = new ImString(128);
    private boolean importModelFlag = false;
    private boolean createEntityFlag = false;
    private ImInt selectedModelIndex = new ImInt(-1); // Use ImInt for ListBox state
    private String[] loadedModelIds = {}; // Array for ImGui list/combo
    private List<String> modelIdListCache = new ArrayList<>(); // Internal list
    private String selectedModelId = null; // Currently selected model ID

    private String[] sceneEntityIds = {}; // Array for ImGui list/combo
    private List<String> entityIdListCache = new ArrayList<>(); // Internal list

    private float[] selectedEntityPosition = {0f, 0f, 0f};
    private float[] selectedEntityRotationEuler = {0f, 0f, 0f}; // Degrees
    private float[] selectedEntityScale = {1f};
    private String lastSelectedEntityId = null; // Track changes
    private boolean transformChangedInGui = false;

    public EditorGui(Scene scene, Editor editor) {
        this.scene = scene;
        this.editor = editor;
        updateSceneLightsStatus(); // Check initial status
        if (sceneLightsExist) {
            loadStateFromSceneLights(); // Load initial state if lights already exist
        }
        updateSkyBoxStatus(); // Keep Skybox status update
        updateModelList(); // Initial model list update
        updateEntityList(); // Initial entity list update
    }

    private void updateSceneLightsStatus() {
        sceneLightsExist = (scene.getSceneLights() != null);
        bypassLightingActive.set(scene.isLightingDisabled());
    }

    // Method to update the displayed skybox path
    private void updateSkyBoxStatus() {
        // We need the SkyBox import here for this method
        org.polygon.engine.core.scene.SkyBox skyBox = scene.getSkyBox();
        if (skyBox != null && skyBox.getIBLData() != null) {
            currentSkyBoxPath = skyBox.getIBLData().getEnvironmentMapPath();
        } else {
            currentSkyBoxPath = "None";
        }
    }

    private void updateModelList() {
        // Updates the list of loaded model IDs for the GUI
        List<String> currentIds = new ArrayList<>(scene.getModelMap().keySet());
        // Only update the array if the underlying list has changed
        if (!currentIds.equals(modelIdListCache)) {
            modelIdListCache = currentIds;
            loadedModelIds = modelIdListCache.toArray(new String[0]);
            // Reset selection if the list changes significantly or selection is invalid
            int currentSelection = selectedModelIndex.get();
            if (currentSelection >= loadedModelIds.length) {
                 selectedModelIndex.set(-1);
                 selectedModelId = null;
            } else if (currentSelection != -1) {
                // Re-affirm selection based on index
                selectedModelId = loadedModelIds[currentSelection];
            } else {
                selectedModelId = null;
            }
        }
    }

    private void updateEntityList() {
        // Updates the list of scene entity IDs for the GUI
        List<String> currentIds = new ArrayList<>(scene.getEntityMap().keySet());
         if (!currentIds.equals(entityIdListCache)) {
             entityIdListCache = currentIds;
             sceneEntityIds = entityIdListCache.toArray(new String[0]);
         }
    }

    // Method to load current light settings into GUI state variables
    private void loadStateFromSceneLights() {
        SceneLights lights = scene.getSceneLights();
        if (lights == null) return; // Should not happen if sceneLightsExist is true

        AmbientLight al = lights.getAmbientLight();
        Vector3f c = al.getColor();
        ambientColor = new float[]{c.x, c.y, c.z};
        ambientIntensity = new float[]{al.getIntensity()};

        DirectionalLight dl = lights.getDirectionalLight();
        c = dl.getColor();
        Vector3f d = dl.getDirection();
        dirColor = new float[]{c.x, c.y, c.z};
        dirIntensity = new float[]{dl.getIntensity()};
        dirDirection = new float[]{d.x, d.y, d.z};

        bypassLightingActive.set(scene.isLightingDisabled());

    }

    // Method to apply GUI state back to SceneLights
    private void applyStateToSceneLights() {
        SceneLights lights = scene.getSceneLights();
        if (lights == null) return;

        // Ambient
        lights.getAmbientLight().setColor(ambientColor[0], ambientColor[1], ambientColor[2]);
        lights.getAmbientLight().setIntensity(ambientIntensity[0]);

        // Directional
        lights.getDirectionalLight().setColor(dirColor[0], dirColor[1], dirColor[2]);
        lights.getDirectionalLight().setIntensity(dirIntensity[0]);
        lights.getDirectionalLight().setDirection(dirDirection[0], dirDirection[1], dirDirection[2]);

        // Apply removals first
        List<PointLight> pointLights = lights.getPointLightList();
        for (int i = pointLightsToRemoveIndices.size() - 1; i >= 0; i--) {
            int indexToRemove = pointLightsToRemoveIndices.get(i);
            if (indexToRemove >= 0 && indexToRemove < pointLights.size()) {
                pointLights.remove(indexToRemove);
            }
        }
        pointLightsToRemoveIndices.clear();

        List<SpotLight> spotLights = lights.getSpotLightList();
        for (int i = spotLightsToRemoveIndices.size() - 1; i >= 0; i--) {
            int indexToRemove = spotLightsToRemoveIndices.get(i);
            if (indexToRemove >= 0 && indexToRemove < spotLights.size()) {
                spotLights.remove(indexToRemove);
            }
        }
        spotLightsToRemoveIndices.clear();

        // Apply additions
        if (addPointLightFlag) {
            PointLight newPl = new PointLight();
            newPl.setColor(1.0f, 1.0f, 1.0f);
            newPl.setPosition(0.0f, 1.0f, 0.0f);
            pointLights.add(newPl);
            addPointLightFlag = false;
        }
        if (addSpotLightFlag) {
            SpotLight newSl = new SpotLight();
            newSl.setColor(1.0f, 1.0f, 1.0f);
            newSl.setPosition(0.0f, 1.0f, 0.0f);
            newSl.setConeDirection(0.0f, -1.0f, 0.0f);
            newSl.setCutOffAngle(15.0f);
            spotLights.add(newSl);
            addSpotLightFlag = false;
        }


        scene.setBypassLighting(bypassLightingActive.get());
    }

     // Method to handle SkyBox changes (Keep the text input version)
    private void applySkyBoxChanges() {
        if (importSkyBoxFlag) {
            String inputPath = skyboxPathInput.get();
            if (inputPath != null && !inputPath.trim().isEmpty()) {
                System.out.println("Attempting to import SkyBox HDR: " + inputPath);
                try {
                    // Need SkyBox import here too
                    org.polygon.engine.core.scene.SkyBox.setupIBLResources();
                    org.polygon.engine.core.scene.SkyBox newSkyBox = new org.polygon.engine.core.scene.SkyBox(inputPath);
                    if (newSkyBox.getIBLData() != null && newSkyBox.getIBLData().getIrradianceMapTextureId() != -1) {
                        scene.setSkyBox(newSkyBox);
                        updateSkyBoxStatus();
                        System.out.println("SkyBox created and set successfully from path: " + inputPath);
                        skyboxPathInput.set(""); // Clear input on success
                    } else {
                        System.err.println("Failed to create valid IBL data from HDR: " + inputPath);
                        // Need glDeleteTextures import here
                        if (newSkyBox.getEnvironmentMapTextureId() != -1) {
                            org.lwjgl.opengl.GL11.glDeleteTextures(newSkyBox.getEnvironmentMapTextureId());
                        }
                         System.err.println("ERROR: Failed to load HDR or create IBL maps from path: "+ inputPath);
                    }
                } catch (Exception e) {
                    System.err.println("Error creating SkyBox from path [" + inputPath + "]: " + e.getMessage());
                    System.err.println("ERROR: Failed creating SkyBox from path: " + inputPath + " - Check path and file.");
                }
            } else {
                 System.err.println("SkyBox import failed: Input path is empty.");
            }
            importSkyBoxFlag = false; // Reset flag
        }

        if (removeSkyBoxFlag) {
            if (scene.getSkyBox() != null) {
                scene.setSkyBox(null);
                updateSkyBoxStatus();
                System.out.println("SkyBox removed from scene.");
            }
            removeSkyBoxFlag = false;
        }
    }

    private void applyModelChanges() {
        if (importModelFlag) {
            System.out.println("[DEBUG] applyModelChanges entered, importModelFlag=true");
            String modelId = modelIdInput.get().trim();
            String modelPath = modelPathInput.get().trim();

            System.out.println("[DEBUG] Got modelId: '" + modelId + "', modelPath: '" + modelPath + "'");

            if (modelId.isEmpty()) {
                System.err.println("Model import failed: Model ID cannot be empty.");
            } else if (modelPath.isEmpty()) {
                System.err.println("Model import failed: Model Path cannot be empty.");
            } else if (scene.getModelMap().containsKey(modelId)) {
                 System.err.println("Model import failed: Model ID '" + modelId + "' already exists.");
            } else {
                 System.out.println("Attempting to load model: ID='" + modelId + "', Path='" + modelPath + "'");
                 System.out.println("[DEBUG] Calling editor.loadModelAndAddToScene...");
                 try {
                     // Call the actual loading method on the editor instance
                     boolean success = editor.loadModelAndAddToScene(modelId, modelPath);

                     System.out.println("[DEBUG] editor.loadModelAndAddToScene returned: " + success);

                     if (success) {
                         System.out.println("Model '" + modelId + "' loaded successfully.");
                         modelIdInput.set("");
                         modelPathInput.set("");
                         updateModelList(); // Refresh the list in the GUI
                     } else {
                         System.err.println("Model loading failed for ID '" + modelId + "'. Check logs.");
                     }
                 } catch (Exception e) {
                      System.err.println("Exception during model load for ID '" + modelId + "': " + e.getMessage());
                 }
            }
            importModelFlag = false; // Reset flag
            System.out.println("[DEBUG] applyModelChanges finished, reset importModelFlag");
        }
    }

    private void applyEntityChanges() {
        if (createEntityFlag) {
            System.out.println("[DEBUG] applyEntityChanges entered, createEntityFlag=true");
            String entityId = entityIdInput.get().trim();

            System.out.println("[DEBUG] Got entityId: '" + entityId + "', selectedModelId: '" + selectedModelId + "'");

            if (entityId.isEmpty()) {
                System.err.println("Entity creation failed: Entity ID cannot be empty.");
            } else if (selectedModelId == null) {
                 System.err.println("Entity creation failed: No model selected.");
            } else if (scene.getEntityMap().containsKey(entityId)) {
                 System.err.println("Entity creation failed: Entity ID '" + entityId + "' already exists.");
            } else {
                 System.out.println("Creating entity '" + entityId + "' from model '" + selectedModelId + "'");
                 System.out.println("[DEBUG] Attempting scene.addEntity...");
                 try {
                     Entity newEntity = new Entity(entityId, selectedModelId);
                     scene.addEntity(newEntity);
                     System.out.println("Entity '" + entityId + "' created successfully.");
                     entityIdInput.set(""); // Clear input
                     System.out.println("[DEBUG] Calling updateEntityList...");
                     updateEntityList(); // Refresh GUI list
                 } catch (Exception e) {
                     System.err.println("Exception during entity creation for ID '" + entityId + "': " + e.getMessage());
                 }
            }
            createEntityFlag = false; // Reset flag
        }
    }

    // Load selected entity state into GUI variables
    private void loadSelectedEntityState() {
        Entity selectedEntity = scene.getSelectedEntity();
        if (selectedEntity == null) {
            lastSelectedEntityId = null; // Reset tracking
            return;
        }

        // Check if the selected entity changed since last time
        if (!selectedEntity.getEntityId().equals(lastSelectedEntityId)) {
             // Update GUI state from the newly selected entity
            Vector3f pos = selectedEntity.getPosition();
            selectedEntityPosition[0] = pos.x;
            selectedEntityPosition[1] = pos.y;
            selectedEntityPosition[2] = pos.z;

            // Convert quaternion to Euler angles (degrees) for display
            Vector3f eulerDeg = new Vector3f();
            selectedEntity.getRotation().getEulerAnglesXYZ(eulerDeg);
            selectedEntityRotationEuler[0] = (float) Math.toDegrees(eulerDeg.x);
            selectedEntityRotationEuler[1] = (float) Math.toDegrees(eulerDeg.y);
            selectedEntityRotationEuler[2] = (float) Math.toDegrees(eulerDeg.z);

            selectedEntityScale[0] = selectedEntity.getScale();

            lastSelectedEntityId = selectedEntity.getEntityId();
            transformChangedInGui = false; // Reset change flag
        }
    }

    // Apply GUI state back to the selected entity
    private void applySelectedEntityState() {
        if (!transformChangedInGui) return; // Only apply if GUI actually changed it

        Entity selectedEntity = scene.getSelectedEntity();
        if (selectedEntity == null || !selectedEntity.getEntityId().equals(lastSelectedEntityId)) {
            // Safety check: Should not happen if logic is correct, but good practice
            transformChangedInGui = false;
            return;
        }

        // Apply position
        selectedEntity.setPosition(selectedEntityPosition[0], selectedEntityPosition[1], selectedEntityPosition[2]);

        // Apply rotation (convert Euler degrees back to Quaternion)
        Quaternionf newRotation = new Quaternionf();
        newRotation.rotationXYZ(
            (float) Math.toRadians(selectedEntityRotationEuler[0]),
            (float) Math.toRadians(selectedEntityRotationEuler[1]),
            (float) Math.toRadians(selectedEntityRotationEuler[2])
        );
        selectedEntity.setRotation(newRotation);

        // Apply scale
        selectedEntity.setScale(selectedEntityScale[0]);

        transformChangedInGui = false; // Reset flag after applying
         System.out.println("[DEBUG] Applied transform changes to entity: " + selectedEntity.getEntityId());
    }

    // --- Helper to draw Material Editor --- 
    private void drawMaterialEditorWindow() {
        ImGui.begin("Material Editor");

        if (selectedModelId == null || selectedModelId.isEmpty()) {
            ImGui.text("Select a model from the 'Models' list in 'Editor Controls' window.");
        } else {
            Model model = scene.getModelMap().get(selectedModelId);
            if (model == null) {
                ImGui.text("Error: Selected model ID not found in scene map.");
            } else {
                ImGui.text("Editing Model: " + selectedModelId);
                ImGui.separator();

                TextureCache textureCache = scene.getTextureCache();
                List<Material> materials = model.getMaterialList();

                for (int i = 0; i < materials.size(); i++) {
                    Material material = materials.get(i);
                    ImGui.pushID("material_" + i);

                    if (ImGui.treeNode("Material " + i)) {
                        // --- Diffuse Color & Texture ---
                        float[] diffuseColor = {material.getDiffuseColor().x, material.getDiffuseColor().y, material.getDiffuseColor().z, material.getDiffuseColor().w};
                        if (ImGui.colorEdit4("Diffuse Color##mat" + i, diffuseColor)) {
                            material.setDiffuseColor(new Vector4f(diffuseColor[0], diffuseColor[1], diffuseColor[2], diffuseColor[3]));
                        }
                        drawTextureInput("Diffuse Map", i, material.getTexturePath(), textureCache, material::setTexturePath);
                        
                        // --- Normal Map --- 
                        drawTextureInput("Normal Map", i, material.getNormalMapPath(), textureCache, material::setNormalMapPath);

                        // --- Metallic & Map ---
                        float[] metallic = {material.getMetallic()};
                        if(ImGui.dragFloat("Metallic##mat"+i, metallic, 0.01f, 0.0f, 1.0f)) {
                             material.setMetallic(metallic[0]);
                        }
                        drawTextureInput("Metallic Map", i, material.getMetallicMapPath(), textureCache, material::setMetallicMapPath);

                        // --- Roughness & Map ---
                        float[] roughness = {material.getRoughness()};
                        if(ImGui.dragFloat("Roughness##mat"+i, roughness, 0.01f, 0.0f, 1.0f)) {
                             material.setRoughness(roughness[0]);
                        }
                        // Note: Roughness map might share path with Metallic for combined textures
                        drawTextureInput("Roughness Map", i, material.getRoughnessMapPath(), textureCache, material::setRoughnessMapPath);

                        // --- AO & Map ---
                        float[] aoStrength = {material.getAoStrength()};
                        if(ImGui.dragFloat("AO Strength##mat"+i, aoStrength, 0.01f, 0.0f, 1.0f)) {
                            material.setAoStrength(aoStrength[0]);
                        }
                        drawTextureInput("AO Map", i, material.getAoMapPath(), textureCache, material::setAoMapPath);
                       
                        // --- Emissive Map --- 
                        drawTextureInput("Emissive Map", i, material.getEmissiveMapPath(), textureCache, material::setEmissiveMapPath);


                        ImGui.treePop();
                    }
                    ImGui.popID();
                }
            }
        }

        ImGui.end();
    }

    // Helper function to draw texture input field and icon
    private void drawTextureInput(String label, int materialIndex, String currentPath, TextureCache cache, java.util.function.Consumer<String> pathSetter) {
        ImString pathInput = new ImString(currentPath != null ? currentPath : "", 256);
        ImGui.text(label + ":");
        ImGui.sameLine();

        int textureId = -1;
        if (currentPath != null && !currentPath.isEmpty()) {
            Texture texture = cache.getTexture(currentPath); // This might load if not present
            // Now we can get the texture ID
            if (texture != null) {
                textureId = texture.getTextureId();
            }
        }
        
        // Display Icon
        if (textureId != -1) {
            // Use the texture ID for the image widget
            ImGui.image(textureId, 64, 64, 0, 1, 1, 0); // UV coords flipped if necessary (0,1 to 1,0 for OpenGL)
        } else {
            ImGui.dummy(64, 64); // Placeholder space
            ImGui.sameLine();
            ImGui.textUnformatted("[No Preview]");
        }
        ImGui.sameLine();

        // Input Text - Use unique ID
        ImGui.pushID(label + "_input_" + materialIndex);
        if (ImGui.inputText("##path", pathInput)) {
            String newPath = pathInput.get();
            String updatedPath = newPath.isEmpty() ? null : newPath;
            pathSetter.accept(newPath.isEmpty() ? null : newPath);
            System.out.println("Set " + label + " path to: " + newPath + " for Material " + materialIndex);

            // Attempt to load the new texture into the cache immediately
            if (updatedPath != null) {
                try {
                    // This will load the texture if not already cached,
                    // or just return the existing one if it is.
                    cache.createTexture(updatedPath);
                    System.out.println("Ensured texture is loaded/cached: " + updatedPath);
                } catch (Exception e) {
                    System.err.println("Failed to load texture from new path [" + updatedPath + "]: " + e.getMessage());
                    // Optionally revert pathSetter or show error message
                }
            }
            // Note: This doesn't explicitly *unload* the old texture if it's no longer
            // referenced anywhere else. TextureCache cleanup might handle that later.
        }
        ImGui.popID();
        // Button placeholder - Add file dialog later if needed
        // ImGui.sameLine();
        // if (ImGui.button("Browse##" + label + materialIndex)) { /* Open file dialog */ }
    }

    @Override
    public void drawGui() {
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSizeConstraints(400, 200, Float.MAX_VALUE, Float.MAX_VALUE);
        // Start with a reasonable default size, can be adjusted
        ImGui.setNextWindowSize(450, 400, ImGuiCond.Once);

        ImGui.begin(title);
        isWindowHovered = ImGui.isWindowHovered() || ImGui.isWindowFocused();

        updateSceneLightsStatus(); // Refresh status
        updateSkyBoxStatus(); // Refresh Skybox status

        // --- Scene Lights Section ---
        if (!sceneLightsExist) {
            if (ImGui.button("Add Scene Lights")) {
                scene.setSceneLights(new SceneLights());
                sceneLightsExist = true;
                loadStateFromSceneLights(); // Load defaults
            }
        } else {
            // --- Display Light Controls --- 
            SceneLights lights = scene.getSceneLights(); // Should not be null here
            if (lights == null) {
                 ImGui.text("Error: SceneLights reported missing unexpectedly.");
                 // Skip light rendering if null
            } else {
                // Collapsible header for all lighting controls
                if (ImGui.collapsingHeader("Lighting Controls")) {
                    ImGui.checkbox("Bypass Lighting", bypassLightingActive);
                    ImGui.separator();

                    // Ambient Light Section
                    if (ImGui.treeNode("Ambient Light")) {
                        ImGui.colorEdit3("##ambColor", ambientColor);
                        ImGui.sameLine(); ImGui.text("Color");
                        ImGui.sliderFloat("##ambIntensity", ambientIntensity, 0.0f, 1.0f, "%.2f Intensity");
                        ImGui.treePop();
                    }

                    // Directional Light Section
                    if (ImGui.treeNode("Directional Light")) {
                        ImGui.colorEdit3("##dirColor", dirColor);
                        ImGui.sameLine(); ImGui.text("Color");
                        ImGui.sliderFloat("##dirIntensity", dirIntensity, 0.0f, 10.0f, "%.2f Intensity");
                        ImGui.text("Direction:");
                        ImGui.dragFloat3("##dirDirection", dirDirection, 0.01f);
                        ImGui.treePop();
                    }

                    // Point Lights Section
                    if (ImGui.treeNode("Point Lights")) {
                        if (ImGui.button("Add Point Light")) {
                            addPointLightFlag = true; // Set flag for handleGuiInput
                        }
                        ImGui.separator();
                        List<PointLight> pointLights = lights.getPointLightList();
                        for (int i = 0; i < pointLights.size(); i++) {
                            PointLight pl = pointLights.get(i);
                            ImGui.pushID("pointLight_" + i);
                            if (ImGui.treeNode("Point Light " + i)) {
                                if (ImGui.button("Remove##point" + i)) {
                                    pointLightsToRemoveIndices.add(i);
                                }
                                // Directly edit light properties
                                float[] color = {pl.getColor().x, pl.getColor().y, pl.getColor().z};
                                if (ImGui.colorEdit3("Color##pl", color)) {
                                    pl.setColor(color[0], color[1], color[2]);
                                }
                                float[] intensity = {pl.getIntensity()};
                                if (ImGui.sliderFloat("Intensity##pl", intensity, 0.0f, 1000.0f, "%.2f")) {
                                    pl.setIntensity(intensity[0]);
                                }
                                float[] radius = {pl.getRadius()};
                                if (ImGui.sliderFloat("Radius##pl", radius, 0.0f, 50.0f, "%.2f")) {
                                   pl.setRadius(radius[0]); // Directly set radius
                                }
                                float[] position = {pl.getPosition().x, pl.getPosition().y, pl.getPosition().z};
                                if (ImGui.dragFloat3("Position##pl", position, 0.1f)) {
                                    pl.setPosition(position[0], position[1], position[2]);
                                }
                                ImGui.treePop();
                            } else {
                                // Add remove button even when collapsed
                                ImGui.sameLine(ImGui.getContentRegionAvailX() - ImGui.calcTextSize("Remove").x - ImGui.getStyle().getItemSpacing().x * 2);
                                if (ImGui.smallButton("Remove##point" + i)) {
                                    pointLightsToRemoveIndices.add(i);
                                }
                            }
                            ImGui.popID();
                        }
                        ImGui.treePop();
                    }

                    // Spot Lights Section
                     if (ImGui.treeNode("Spot Lights")) {
                        if (ImGui.button("Add Spot Light")) {
                            addSpotLightFlag = true; // Set flag
                        }
                        ImGui.separator();
                        List<SpotLight> spotLights = lights.getSpotLightList();
                        for (int i = 0; i < spotLights.size(); i++) {
                            SpotLight sl = spotLights.get(i);
                            ImGui.pushID("spotLight_" + i);
                            if (ImGui.treeNode("Spot Light " + i)) {
                                 if (ImGui.button("Remove##spot" + i)) {
                                    spotLightsToRemoveIndices.add(i);
                                }
                                float[] color = {sl.getColor().x, sl.getColor().y, sl.getColor().z};
                                if (ImGui.colorEdit3("Color##sl", color)) {
                                    sl.setColor(color[0], color[1], color[2]);
                                }
                                float[] intensity = {sl.getIntensity()};
                                if (ImGui.sliderFloat("Intensity##sl", intensity, 0.0f, 1000.0f, "%.2f")) {
                                     sl.setIntensity(intensity[0]);
                                }
                                float[] radius = {sl.getRadius()};
                                 if (ImGui.sliderFloat("Radius##sl", radius, 0.0f, 50.0f, "%.2f")) {
                                    sl.setRadius(radius[0]);
                                }
                                float[] position = {sl.getPosition().x, sl.getPosition().y, sl.getPosition().z};
                                if (ImGui.dragFloat3("Position##sl", position, 0.1f)) {
                                    sl.setPosition(position[0], position[1], position[2]);
                                }
                                float[] coneDir = {sl.getConeDirection().x, sl.getConeDirection().y, sl.getConeDirection().z};
                                if (ImGui.dragFloat3("Cone Dir##sl", coneDir, 0.01f)) {
                                     sl.setConeDirection(coneDir[0], coneDir[1], coneDir[2]);
                                }
                                float[] cutOffAngle = {sl.getCutOffAngle()};
                                 if (ImGui.sliderFloat("Cutoff Angle##sl", cutOffAngle, 0.0f, 90.0f, "%.1f")) {
                                    sl.setCutOffAngle(cutOffAngle[0]);
                                }
                                ImGui.treePop();
                            } else {
                                 // Add remove button even when collapsed
                                ImGui.sameLine(ImGui.getContentRegionAvailX() - ImGui.calcTextSize("Remove").x - ImGui.getStyle().getItemSpacing().x * 2);
                                 if (ImGui.smallButton("Remove##spot" + i)) {
                                     spotLightsToRemoveIndices.add(i);
                                 }
                            }
                            ImGui.popID();
                        }
                        ImGui.treePop();
                    }
                } // End Lighting Controls Header
            }
        } // End Scene Lights Section

        // --- SkyBox Section (Keep text input version) ---
        if (ImGui.collapsingHeader("SkyBox")) {
            ImGui.text("Current HDR: " + currentSkyBoxPath);
            ImGui.inputText("HDR Path##skyboxpath", skyboxPathInput);
            ImGui.sameLine();
            if (ImGui.button("Import##skybox")) {
                importSkyBoxFlag = true;
            }
            if (!currentSkyBoxPath.equals("None")) {
                ImGui.sameLine();
                if (ImGui.button("Remove##skybox")) {
                    removeSkyBoxFlag = true;
                }
            }
        }
        // --- End SkyBox Section ---

        // --- Model Section ---
        if (ImGui.collapsingHeader("Models")) {
            ImGui.inputText("Model ID##modelid", modelIdInput);
            ImGui.inputText("Model Path##modelpath", modelPathInput);
            if (ImGui.button("Import Model")) {
                importModelFlag = true;
            }
            ImGui.separator();
            ImGui.text("Loaded Models:");
            // Use a ListBox for selection
            // Store the index before the call
            int previousSelectionIndex = selectedModelIndex.get();

            // Call the listBox (which returns void in this version)
            ImGui.listBox("##loadedmodels", selectedModelIndex, loadedModelIds);

            // Check if the index changed after the call
            int currentSelection = selectedModelIndex.get();
            if (currentSelection != previousSelectionIndex) {
                 if (currentSelection >= 0 && currentSelection < loadedModelIds.length) {
                     selectedModelId = loadedModelIds[currentSelection];
                     System.out.println("Model selection changed to: " + selectedModelId);
                 } else {
                      selectedModelId = null;
                      // Handle potential de-selection or invalid index if needed
                      if (currentSelection != -1) { // Only reset if not explicitly deselected
                           selectedModelIndex.set(-1); // Ensure index is reset if invalid
                      }
                      System.out.println("Model deselected or selection invalid.");
                 }
             }
        }

        // --- Entity Section ---
        if (ImGui.collapsingHeader("Entities")) {
            if (selectedModelId != null) {
                ImGui.text("Selected Model: " + selectedModelId);
                ImGui.inputText("Entity ID##entityid", entityIdInput);
                 if (ImGui.button("Create Entity")) {
                     System.out.println("[DEBUG] Create Entity button clicked.");
                     createEntityFlag = true;
                 }
            } else {
                 ImGui.text("Select a model from the list above to create an entity.");
            }
            ImGui.separator();
            ImGui.text("Scene Entities:");
            // Use ListBox to display entity IDs (no selection for now)
            ImGui.listBox("##sceneentities", new ImInt(-1), sceneEntityIds); // Pass dummy ImInt if no selection needed
        }

        // --- Selected Entity Section ---
        if (ImGui.collapsingHeader("Selected Entity Properties")) {
            Entity selectedEntity = scene.getSelectedEntity();
            if (selectedEntity != null) {
                // Load state if selection changed or first time
                loadSelectedEntityState();

                ImGui.text("ID: " + selectedEntity.getEntityId());
                ImGui.separator();

                // Position Control
                if (ImGui.dragFloat3("Position##selPos", selectedEntityPosition, 0.1f)) {
                     transformChangedInGui = true;
                }
                // Rotation Control (Euler Degrees)
                if (ImGui.dragFloat3("Rotation (XYZ Deg)##selRot", selectedEntityRotationEuler, 1.0f)) {
                    transformChangedInGui = true;
                    // Optional: Wrap angles here if desired (e.g., 0-360)
                }
                // Scale Control
                if (ImGui.dragFloat("Scale##selScale", selectedEntityScale, 0.05f, 0.01f, 100.0f)) {
                     transformChangedInGui = true;
                }

            } else {
                ImGui.text("Click on an entity in the scene to select it.");
            }
        }

        ImGui.end();

        // --- Material Editor Window ---
        drawMaterialEditorWindow();
    }

    @Override
    public boolean handleGuiInput(Window window) {
        boolean consumed = isWindowHovered;
        
        // Apply SkyBox changes first if flags are set
        if (importSkyBoxFlag || removeSkyBoxFlag) {
            applySkyBoxChanges();
            // Setting consumed might be good if interaction happened
            consumed = true; 
        }
        
        // Apply light changes if GUI is interacted with OR if light add/remove flags were set
        if ((consumed && sceneLightsExist) || addPointLightFlag || addSpotLightFlag || !pointLightsToRemoveIndices.isEmpty() || !spotLightsToRemoveIndices.isEmpty()) {
           if (sceneLightsExist) {
                applyStateToSceneLights();
           } else {
                // Clear flags if lights aren't active
                addPointLightFlag = false;
                addSpotLightFlag = false;
                pointLightsToRemoveIndices.clear();
                spotLightsToRemoveIndices.clear();
           }
        }

        // --- Reset One-Shot Flags --- 
        // Skybox flags reset in applySkyBoxChanges
        // Light add flags reset in applyStateToSceneLights
        // Light remove lists cleared in applyStateToSceneLights
        
        // Apply Model/Entity changes if flags are set
        if (importModelFlag) { // <--- THIS is the check that seems to be failing
            System.out.println("[DEBUG] handleGuiInput detected importModelFlag=true, calling applyModelChanges...");
            applyModelChanges();
            consumed = true; // Assume button click consumed input
        }

        if (createEntityFlag) {
            System.out.println("[DEBUG] handleGuiInput detected createEntityFlag=true, calling applyEntityChanges...");
            applyEntityChanges();
            consumed = true; // Assume button click consumed input
        }

        // Apply selected entity transform changes if GUI was interacted with
        if (transformChangedInGui) {
            applySelectedEntityState();
            // consumed = true; // Dragging floats might already set ImGui capture flags
        }

        // Determine final consumption based on ImGui state (after processing our inputs)
        // This ensures mouse clicks on buttons are captured correctly
        return consumed || ImGui.getIO().getWantCaptureMouse() || ImGui.getIO().getWantCaptureKeyboard();
    }
}