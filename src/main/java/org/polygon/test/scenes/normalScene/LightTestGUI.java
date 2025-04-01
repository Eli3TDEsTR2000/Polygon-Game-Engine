package org.polygon.test.scenes.lightTestScene;

import imgui.*;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiMouseButton;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.polygon.engine.core.IGuiInstance;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.lights.*;

public class LightTestGUI implements IGuiInstance {
    private float[] ambientColor;
    private float[] ambientIntensity;

    private float[] directionalLightColor;
    private float[] directionalLightIntensity;
    private float[] directionalLightX;
    private float[] directionalLightY;
    private float[] directionalLightZ;

    private float[] spotLightColor;
    private float[] spotLightIntensity;
    private float[] spotLightCutOff;
    private float[] coneDirectionX;
    private float[] coneDirectionY;
    private float[] coneDirectionZ;
    private float[] spotLightX;
    private float[] spotLightY;
    private float[] spotLightZ;
    private float[] pointLightColor;
    private float[] pointLightIntensity;
    private float[] pointLightX;
    private float[] pointLightY;
    private float[] pointLightZ;

    private ImBoolean active;


    public LightTestGUI(Scene scene) {
        SceneLights sceneLights = scene.getSceneLights();
        AmbientLight ambientLight= sceneLights.getAmbientLight();
        Vector3f color = ambientLight.getColor();

        ambientColor = new float[]{color.x, color.y, color.z};
        ambientIntensity = new float[]{ambientLight.getIntensity()};

        PointLight pointLight = sceneLights.getPointLightList().get(0);
        color = pointLight.getColor();
        Vector3f position = pointLight.getPosition();
        pointLightColor = new float[]{color.x, color.y, color.z};
        pointLightIntensity = new float[]{pointLight.getIntensity()};
        pointLightX = new float[]{position.x};
        pointLightY = new float[]{position.y};
        pointLightZ = new float[]{position.z};

        SpotLight spotLight = sceneLights.getSpotLightList().get(0);
        color = spotLight.getColor();
        position = spotLight.getPosition();
        Vector3f coneDirection = spotLight.getConeDirection();
        spotLightColor = new float[]{color.x, color.y, color.z};
        spotLightIntensity = new float[]{spotLight.getIntensity()};
        spotLightX = new float[]{position.x};
        spotLightY = new float[]{position.y};
        spotLightZ = new float[]{position.z};
        coneDirectionX = new float[]{coneDirection.x};
        coneDirectionY = new float[]{coneDirection.y};
        coneDirectionZ = new float[]{coneDirection.z};
        spotLightCutOff = new float[]{spotLight.getCutOff()};

        DirectionalLight directionalLight = sceneLights.getDirectionalLight();
        color = directionalLight.getColor();
        position = directionalLight.getDirection();
        directionalLightColor = new float[]{color.x, color.y, color.z};
        directionalLightIntensity =new float[]{directionalLight.getIntensity()};
        directionalLightX = new float[]{position.x};
        directionalLightY = new float[]{position.y};
        directionalLightZ = new float[]{position.z};

        active = new ImBoolean(false);
    }

    @Override
    public void drawGui() {
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);

        ImGui.begin("Light test");
        ImGui.checkbox("Bypass Lighting", active);
        ImGui.separator();
        if(ImGui.collapsingHeader("Ambient Light")) {
            ImGui.colorEdit3("Ambient Light Color", ambientColor);
            ImGui.sliderFloat("Ambient Intensity", ambientIntensity, 0.0f, 1.0f, "%.2f");
        }
        if(ImGui.collapsingHeader("Directional Light")) {
            ImGui.colorEdit3("Directional Light Color", directionalLightColor);
            ImGui.sliderFloat("Directional Light Intensity", directionalLightIntensity
                    , 0.0f, 10.0f, "%.2f");
            ImGui.sliderFloat("direction-x", directionalLightX, -1.0f, 1.0f, "%.2f");
            ImGui.sliderFloat("direction-y", directionalLightY, -1.0f, 1.0f, "%.2f");
            ImGui.sliderFloat("direction-z", directionalLightZ, -1.0f, 1.0f, "%.2f");
        }
        if(ImGui.collapsingHeader("Point Light")) {
            ImGui.colorEdit3("Point Light Color", pointLightColor);
            ImGui.sliderFloat("Point Light Intensity", pointLightIntensity, 0.0f, 10.0f, "%.2f");
            ImGui.sliderFloat("Point Light - x", pointLightX, -10.0f,10.0f, "%.2f");
            ImGui.sliderFloat("Point Light - y", pointLightY, -10.0f,10.0f, "%.2f");
            ImGui.sliderFloat("Point Light - z", pointLightZ, -10.0f,10.0f, "%.2f");
        }
        if(ImGui.collapsingHeader("Spot Light")) {
            ImGui.colorEdit3("Spot Light Color", spotLightColor);
            ImGui.sliderFloat("Spot Light Intensity", spotLightIntensity, 0.0f, 1.0f, "%.2f");
            ImGui.sliderFloat("Spot Light - x", spotLightX, -10.0f,10.0f, "%.2f");
            ImGui.sliderFloat("Spot Light - y", spotLightY, -10.0f,10.0f, "%.2f");
            ImGui.sliderFloat("Spot Light - z", spotLightZ, -10.0f,10.0f, "%.2f");
            ImGui.separator();
            ImGui.sliderFloat("Spot Light Cutoff", spotLightCutOff, 0.0f, 360.0f, "%2.f");
            ImGui.sliderFloat("cone direction - x", coneDirectionX, -1.0f, 1.0f, "%.2f");
            ImGui.sliderFloat("cone direction - y", coneDirectionY, -1.0f, 1.0f, "%.2f");
            ImGui.sliderFloat("cone direction - z", coneDirectionZ, -1.0f, 1.0f, "%.2f");
        }
        ImGui.end();
        ImGui.endFrame();
        ImGui.render();
    }

    @Override
    public boolean handleGuiInput(Window window) {
        ImGuiIO imGuiIO = ImGui.getIO();
        Vector2f mousePosition = window.getMouseInputHandler().getCurrentPosition();
        imGuiIO.addMousePosEvent(mousePosition.x, mousePosition.y);
        imGuiIO.addMouseButtonEvent(ImGuiMouseButton.Left, window.getMouseInputHandler().isLeftButtonPressed());
        imGuiIO.addMouseButtonEvent(ImGuiMouseButton.Right, window.getMouseInputHandler().isRightButtonPressed());

        boolean consumed = imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard();

        if(consumed) {
            SceneLights sceneLights = window.getCurrentScene().getSceneLights();

            AmbientLight ambientLight = sceneLights.getAmbientLight();
            ambientLight.setColor(ambientColor[0], ambientColor[1], ambientColor[2]);
            ambientLight.setIntensity(ambientIntensity[0]);

            DirectionalLight directionalLight = sceneLights.getDirectionalLight();
            directionalLight.setColor(directionalLightColor[0], directionalLightColor[1], directionalLightColor[2]);
            directionalLight.setIntensity(directionalLightIntensity[0]);
            directionalLight.setDirection(directionalLightX[0], directionalLightY[0], directionalLightZ[0]);

            PointLight pointLight = sceneLights.getPointLightList().get(0);
            pointLight.setColor(pointLightColor[0], pointLightColor[1], pointLightColor[2]);
            pointLight.setIntensity(pointLightIntensity[0]);
            pointLight.setPosition(pointLightX[0], pointLightY[0], pointLightZ[0]);

            SpotLight spotLight = sceneLights.getSpotLightList().get(0);
            spotLight.setColor(spotLightColor[0], spotLightColor[1], spotLightColor[2]);
            spotLight.setIntensity(spotLightIntensity[0]);
            spotLight.setPosition(spotLightX[0], spotLightY[0], spotLightZ[0]);
            spotLight.setConeDirection(coneDirectionX[0], coneDirectionY[0], coneDirectionZ[0]);
            spotLight.setCutOffAngle(spotLightCutOff[0]);

            window.getCurrentScene().setBypassLighting(active.get());
        }

        return consumed;
    }
}