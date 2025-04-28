package org.polygon.test.scenes.cubeScene;

import imgui.*;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import org.polygon.engine.core.IGuiInstance;
import org.polygon.engine.core.Window;

import static org.lwjgl.glfw.GLFW.glfwSwapInterval;

public class UpdateFpsTestGUI implements IGuiInstance {
    private ImInt ups;
    private ImInt fps;
    private ImBoolean vsyncEnabled;
    private ImBoolean postProcessingEnabled;
    private float[] gamma;
    private float[] exposure;
    private Window window;
    private boolean isWindowHovered;

    public UpdateFpsTestGUI(Window window) {
        ups = new ImInt();
        fps = new ImInt();
        vsyncEnabled = new ImBoolean();
        ups.set(window.getWindowOptions().ups);
        if(window.getWindowOptions().fps == 0) {
            vsyncEnabled.set(true);
            fps.set(0);
        } else {
            vsyncEnabled.set(false);
            fps.set(window.getWindowOptions().fps);
        }

        gamma = new float[]{window.getWindowOptions().gamma};
        exposure = new float[]{window.getWindowOptions().exposure};
        postProcessingEnabled = new ImBoolean();
        postProcessingEnabled.set(window.getWindowOptions().enableToneGamma);

        this.window = window;
    }
    @Override
    public void drawGui() {
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(300, 300, ImGuiCond.Once);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, new ImVec2(20, 20));
        ImGui.begin("Engine's Fixed-updates / V-Sync Test");

        isWindowHovered = ImGui.isWindowFocused();

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, new ImVec2(10, 18));
        if(ImGui.checkbox("V-Sync", vsyncEnabled)) {
            if(vsyncEnabled.get()) {
                glfwSwapInterval(1);
                window.getWindowOptions().fps = 0;
            } else {
                glfwSwapInterval(0);
                window.getWindowOptions().fps = 10000;
                fps.set(10000);
            }
        }

        if(!vsyncEnabled.get()) {
            ImGui.labelText("##FramesPerSecond", "FPS cap:");
            ImGui.sameLine(100);
            ImGui.pushItemWidth(80);
            if(ImGui.inputScalar("##InputScalarFPS", fps)) {
                if(fps.intValue() <= 5) {
                    fps.set(5);
                }
                window.getWindowOptions().fps = fps.intValue();
            }
            ImGui.popItemWidth();
        }
        ImGui.popStyleVar();


        ImGui.labelText("##UpdatesPerSecondLabel", "Updates per second:");
        ImGui.sameLine(170);
        ImGui.pushItemWidth(80);
        if(ImGui.inputScalar("##InputScalarUPS", ups)) {
            if(ups.intValue() < 0) {
                ups.set(0);
            }
            window.getWindowOptions().ups = ups.intValue();
        }
        ImGui.popItemWidth();

        ImGui.dummy(0, 20);

        if(ImGui.checkbox("Post-Processing", postProcessingEnabled)) {
            if(postProcessingEnabled.get()) {
                window.getWindowOptions().enableToneGamma = true;
            } else {
                window.getWindowOptions().enableToneGamma = false;
            }
        }

        ImGui.dummy(0, 10);

        if(ImGui.collapsingHeader("Tone mapping / Gamma correction", ImGuiTreeNodeFlags.DefaultOpen)) {
            ImGui.labelText("##GammaCorrectionSlider", "Gamma: ");
            ImGui.sameLine(100);
            if(ImGui.sliderFloat("##Gamma", gamma, 0.0f, 10.0f, "%.2f")) {
                window.getWindowOptions().gamma = this.gamma[0];
            }

            ImGui.labelText("##ExposureSlider", "Exposure: ");
            ImGui.sameLine(100);
            if(ImGui.sliderFloat("##Exposure", exposure, 0.0f, 10.0f, "%.2f")) {
                window.getWindowOptions().exposure = this.exposure[0];
            }
        }

        ImGui.end();
        ImGui.popStyleVar();
    }



    @Override
    public boolean handleGuiInput(Window window) {
        ImGuiIO imGuiIO = ImGui.getIO();
        boolean consumed = (imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard()) && isWindowHovered;

        return consumed;
    }
}
