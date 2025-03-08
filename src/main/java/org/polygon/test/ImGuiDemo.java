package org.polygon.test;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiMouseButton;
import org.joml.Vector2f;
import org.polygon.engine.core.IGuiInstance;
import org.polygon.engine.core.Window;

public class ImGuiDemo implements IGuiInstance {

    // Setup the ImGui demo GUI.
    @Override
    public void drawGui() {
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.showDemoWindow();
        ImGui.endFrame();
        ImGui.render();
    }

    // Handle mouse and keyboard inputs and return true if the ImGui context is on focus of the mouse or keyboard.
    @Override
    public boolean handleGuiInput(Window window) {
        ImGuiIO imGuiIO = ImGui.getIO();
        Vector2f mousePosition = window.getMouseInputHandler().getCurrentPosition();
        imGuiIO.addMousePosEvent(mousePosition.x, mousePosition.y);
        imGuiIO.addMouseButtonEvent(ImGuiMouseButton.Left, window.getMouseInputHandler().isLeftButtonPressed());
        imGuiIO.addMouseButtonEvent(ImGuiMouseButton.Right, window.getMouseInputHandler().isRightButtonPressed());

        return imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard();
    }
}
