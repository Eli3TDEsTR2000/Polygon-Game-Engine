package org.polygon.test;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import org.polygon.engine.core.IGuiInstance;
import org.polygon.engine.core.Window;

public class performanceGUI implements IGuiInstance {
    boolean showFPS;

    public performanceGUI(boolean showFPS) {
        this.showFPS = showFPS;
    }

    public static void renderFPS() {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        ImGui.setNextWindowPos(ImGui.getIO().getDisplaySizeX() - 130, 10, ImGuiCond.Always);
        ImGui.setNextWindowSize(new ImVec2(200, 80), ImGuiCond.Always);
        ImGui.setNextWindowBgAlpha(0);

        if (ImGui.begin("##Performance",
                ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse
                        | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoTitleBar)) {

            ImGui.text(String.format("FPS: %.1f", ImGui.getIO().getFramerate()));
        }
        ImGui.end();
        ImGui.popStyleVar();
    }

    @Override
    public void drawGui() {
        if(showFPS) renderFPS();
    }

    @Override
    public boolean handleGuiInput(Window window) {
        return false;
    }
}
