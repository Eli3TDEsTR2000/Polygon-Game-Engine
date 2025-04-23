package org.polygon.test.scenes.normalScene;

import imgui.*;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import org.joml.Vector3f;
import org.polygon.engine.core.IGuiInstance;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.lights.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LightTestGUI implements IGuiInstance {

    private static class LightGuiState {
        float[] color = {1.0f, 1.0f, 1.0f};
        float[] intensity = {1.0f};
        float[] position = {0.0f, 0.0f, 0.0f};
        float[] coneDirection = {0.0f, -1.0f, 0.0f};
        float[] cutOffAngle = {15.0f};
        PointLight lightRef;

        LightGuiState(PointLight light) {
            this.lightRef = light;
            Vector3f c = light.getColor();
            this.color[0] = c.x; this.color[1] = c.y; this.color[2] = c.z;
            this.intensity[0] = light.getIntensity();
            Vector3f p = light.getPosition();
            this.position[0] = p.x; this.position[1] = p.y; this.position[2] = p.z;

            if (light instanceof SpotLight spot) {
                Vector3f cd = spot.getConeDirection();
                this.coneDirection[0] = cd.x; this.coneDirection[1] = cd.y; this.coneDirection[2] = cd.z;
                this.cutOffAngle[0] = spot.getCutOffAngle();
            }
        }
    }

    private Random random = new Random();

    private float[] ambientColor;
    private float[] ambientIntensity;
    private float[] directionalLightColor;
    private float[] directionalLightIntensity;
    private float[] directionalLightX;
    private float[] directionalLightY;
    private float[] directionalLightZ;

    private List<LightGuiState> pointLightStates;
    private List<LightGuiState> spotLightStates;
    private List<Integer> pointLightsToRemoveIndices;
    private List<Integer> spotLightsToRemoveIndices;
    private boolean addPointLightFlag = false;
    private boolean addSpotLightFlag = false;

    private ImBoolean bypassLightingActive;
    private boolean isWindowHovered;
    String title;

    public LightTestGUI(Scene scene, String title) {
        this.title = title;
        SceneLights sceneLights = scene.getSceneLights();

        // Ambient Light
        AmbientLight ambientLight = sceneLights.getAmbientLight();
        Vector3f color = ambientLight.getColor();
        ambientColor = new float[]{color.x, color.y, color.z};
        ambientIntensity = new float[]{ambientLight.getIntensity()};

        // Directional Light
        DirectionalLight directionalLight = sceneLights.getDirectionalLight();
        color = directionalLight.getColor();
        Vector3f position = directionalLight.getDirection(); // Direction stored as position here
        directionalLightColor = new float[]{color.x, color.y, color.z};
        directionalLightIntensity = new float[]{directionalLight.getIntensity()};
        directionalLightX = new float[]{position.x};
        directionalLightY = new float[]{position.y};
        directionalLightZ = new float[]{position.z};

        // Point Lights
        pointLightStates = new ArrayList<>();
        for (PointLight pl : sceneLights.getPointLightList()) {
            pointLightStates.add(new LightGuiState(pl));
        }

        // Spot Lights
        spotLightStates = new ArrayList<>();
        for (SpotLight sl : sceneLights.getSpotLightList()) {
            spotLightStates.add(new LightGuiState(sl));
        }

        pointLightsToRemoveIndices = new ArrayList<>();
        spotLightsToRemoveIndices = new ArrayList<>();
        bypassLightingActive = new ImBoolean(false);
    }

    @Override
    public void drawGui() {
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSizeConstraints(400, 200, Float.MAX_VALUE, Float.MAX_VALUE);
        ImGui.setNextWindowSize(450, 400, ImGuiCond.Once);

        ImGui.begin(title);
        isWindowHovered = ImGui.isWindowFocused();

        ImGui.checkbox("Bypass Lighting", bypassLightingActive);
        ImGui.separator();

        // Ambient Light
        if (ImGui.collapsingHeader("Ambient Light")) {
            ImGui.colorEdit3("##ambColor", ambientColor);
            ImGui.sameLine(); ImGui.text("Color");
            ImGui.sliderFloat("##ambIntensity", ambientIntensity, 0.0f, 1.0f, "%.2f Intensity");
        }

        // Directional Light
        if (ImGui.collapsingHeader("Directional Light")) {
            ImGui.colorEdit3("##dirColor", directionalLightColor);
            ImGui.sameLine(); ImGui.text("Color");
            ImGui.sliderFloat("##dirIntensity", directionalLightIntensity, 0.0f, 10.0f, "%.2f Intensity");
            ImGui.text("Direction:");
            ImGui.sliderFloat("##dirX", directionalLightX, -1.0f, 1.0f, "X: %.2f");
            ImGui.sliderFloat("##dirY", directionalLightY, -1.0f, 1.0f, "Y: %.2f");
            ImGui.sliderFloat("##dirZ", directionalLightZ, -1.0f, 1.0f, "Z: %.2f");
        }

        // Point Lights
        if (ImGui.collapsingHeader("Point Lights")) {
            if (ImGui.button("Add Point Light")) {
                addPointLightFlag = true;
            }
            ImGui.separator();
            pointLightsToRemoveIndices.clear();
            for (int i = 0; i < pointLightStates.size(); i++) {
                ImGui.pushID("pointLight_" + i);
                LightGuiState state = pointLightStates.get(i);
                ImGui.text("Point Light " + i);
                ImGui.sameLine();
                if (ImGui.button("Remove##point" + i)) {
                    pointLightsToRemoveIndices.add(i);
                }
                ImGui.colorEdit3("##plColor", state.color);
                ImGui.sameLine(); ImGui.text("Color");
                ImGui.sliderFloat("##plIntensity", state.intensity, 0.0f, 10.0f, "%.2f Intensity");
                ImGui.text("Position:");
                ImGui.sliderFloat3("##plPos", state.position, -10.0f, 10.0f, "%.2f");
                ImGui.separator();
                ImGui.popID();
            }
        }

        // --- Spot Lights --- 
        if (ImGui.collapsingHeader("Spot Lights")) {
            if (ImGui.button("Add Spot Light")) {
                addSpotLightFlag = true;
            }
            ImGui.separator();
            spotLightsToRemoveIndices.clear();
            for (int i = 0; i < spotLightStates.size(); i++) {
                ImGui.pushID("spotLight_" + i);
                LightGuiState state = spotLightStates.get(i);
                ImGui.text("Spot Light " + i);
                ImGui.sameLine();
                if (ImGui.button("Remove##spot" + i)) {
                    spotLightsToRemoveIndices.add(i);
                }
                ImGui.colorEdit3("##slColor", state.color);
                ImGui.sameLine(); ImGui.text("Color");
                ImGui.sliderFloat("##slIntensity", state.intensity, 0.0f, 10.0f, "%.2f Intensity");
                ImGui.text("Position:");
                ImGui.sliderFloat3("##slPos", state.position, -10.0f, 10.0f, "%.2f");
                ImGui.text("Cone Dir:");
                ImGui.sliderFloat3("##slDir", state.coneDirection, -1.0f, 1.0f, "%.2f");
                ImGui.sliderFloat("##slCutoff", state.cutOffAngle, 0.0f, 90.0f, "Cutoff Angle: %.1f");
                ImGui.separator();
                ImGui.popID();
            }
        }

        ImGui.end();
    }

    @Override
    public boolean handleGuiInput(Window window) {
        ImGuiIO imGuiIO = ImGui.getIO();
        boolean consumed = (imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard()) && isWindowHovered;

        if (consumed) {
            Scene currentScene = window.getCurrentScene();
            SceneLights sceneLights = currentScene.getSceneLights();
            if (sceneLights == null)  {
                return true;
            }

            // Handle Additions
            if (addPointLightFlag) {
                PointLight newPl = new PointLight();

                // Generate random color (0.0 to 1.0)
                float r = random.nextFloat();
                float g = random.nextFloat();
                float b = random.nextFloat();
                newPl.setColor(r, g, b);

                // Generate random position covering the backpack grid area
                float x = random.nextFloat(-6.0f, 6.0f);
                float y = random.nextFloat(0.5f, 4.0f);
                float z = random.nextFloat(-21.0f, -1.0f);
                newPl.setPosition(x, y, z);


                sceneLights.getPointLightList().add(newPl);
                pointLightStates.add(new LightGuiState(newPl));
                addPointLightFlag = false;
            }
            if (addSpotLightFlag) {
                SpotLight newSl = new SpotLight();
                sceneLights.getSpotLightList().add(newSl);
                spotLightStates.add(new LightGuiState(newSl));
                addSpotLightFlag = false;
            }

            for (int i = pointLightsToRemoveIndices.size() - 1; i >= 0; i--) {
                int indexToRemove = pointLightsToRemoveIndices.get(i);
                if (indexToRemove < sceneLights.getPointLightList().size()) {
                    sceneLights.getPointLightList().remove(indexToRemove);
                }
                if (indexToRemove < pointLightStates.size()) {
                    pointLightStates.remove(indexToRemove);
                }
            }
            pointLightsToRemoveIndices.clear();

            for (int i = spotLightsToRemoveIndices.size() - 1; i >= 0; i--) {
                int indexToRemove = spotLightsToRemoveIndices.get(i);
                if (indexToRemove < sceneLights.getSpotLightList().size()) {
                    sceneLights.getSpotLightList().remove(indexToRemove);
                }
                if (indexToRemove < spotLightStates.size()) {
                    spotLightStates.remove(indexToRemove);
                }
            }
            spotLightsToRemoveIndices.clear();


            // Ambient
            AmbientLight ambientLight = sceneLights.getAmbientLight();
            ambientLight.setColor(ambientColor[0], ambientColor[1], ambientColor[2]);
            ambientLight.setIntensity(ambientIntensity[0]);

            // Directional
            DirectionalLight directionalLight = sceneLights.getDirectionalLight();
            directionalLight.setColor(directionalLightColor[0], directionalLightColor[1], directionalLightColor[2]);
            directionalLight.setIntensity(directionalLightIntensity[0]);
            directionalLight.setDirection(directionalLightX[0], directionalLightY[0], directionalLightZ[0]);

            // Point Lights
            for (int i = 0; i < pointLightStates.size(); i++) {
                LightGuiState state = pointLightStates.get(i);
                if (i < sceneLights.getPointLightList().size()) {
                    PointLight pl = sceneLights.getPointLightList().get(i);
                    state.lightRef = pl;
                    pl.setColor(state.color[0], state.color[1], state.color[2]);
                    pl.setIntensity(state.intensity[0]);
                    pl.setPosition(state.position[0], state.position[1], state.position[2]);
                } else {
                    state.lightRef = null;
                }
            }

            // Spot Lights
            for (int i = 0; i < spotLightStates.size(); i++) {
                LightGuiState state = spotLightStates.get(i);
                if (i < sceneLights.getSpotLightList().size()) {
                    SpotLight sl = sceneLights.getSpotLightList().get(i);
                    state.lightRef = sl;
                    sl.setColor(state.color[0], state.color[1], state.color[2]);
                    sl.setIntensity(state.intensity[0]);
                    sl.setPosition(state.position[0], state.position[1], state.position[2]);
                    sl.setConeDirection(state.coneDirection[0], state.coneDirection[1], state.coneDirection[2]);
                    sl.setCutOffAngle(state.cutOffAngle[0]);
                } else {
                    state.lightRef = null;
                }
            }

            currentScene.setBypassLighting(bypassLightingActive.get());
        }

        return consumed;
    }
}