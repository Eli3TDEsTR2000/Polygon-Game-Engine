package org.polygon.test.scenes.selectionScene;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import org.polygon.engine.core.IGuiInstance;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.Scene;

public class EntitySelectionGUI implements IGuiInstance {
    String selectedEntity;
    Scene scene;

    public EntitySelectionGUI(Scene scene) {
        selectedEntity = "None";
        this.scene = scene;
    }

    @Override
    public void drawGui() {
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(400, 250, ImGuiCond.Once);

        if(ImGui.begin("Entity selection test")) {
            ImGui.dummy(0, 30);
            Entity selected = scene.getSelectedEntity();
            if(selected == null) {
                selectedEntity = "None";
            } else {
                selectedEntity = scene.getSelectedEntity().getEntityId();
            }
            ImGui.labelText("##selectedEntity", "Current Selected Entity is : " + selectedEntity);
        }
        ImGui.end();
    }

    @Override
    public boolean handleGuiInput(Window window) {
        ImGuiIO imGuiIO = ImGui.getIO();

        return imGuiIO.getWantCaptureKeyboard() || imGuiIO.getWantCaptureMouse();
    }
}
