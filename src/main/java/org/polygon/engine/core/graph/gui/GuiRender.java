package org.polygon.engine.core.graph.gui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import org.lwjgl.glfw.GLFW;
import org.polygon.engine.core.IGuiInstance;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.gui.backend.gl3.ImGuiImplGl3;
import org.polygon.engine.core.graph.gui.backend.glfw.ImGuiImplGlfw;

public class GuiRender {
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private String glslVersion;

    public GuiRender(Window window) {
        glslVersion = "#version 410";
        initImGui();
        imGuiGlfw.init(window.getWindowHandle(), true);
        imGuiGl3.init(glslVersion);
    }

    public void cleanup() {
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();

    }

    private void initImGui() {
        ImGui.createContext();

        final ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);                                // We don't want to save .ini file
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);  // Enable Keyboard Controls
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);      // Enable Docking

        // Currently disabled, tanks performance
//        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);    // Enable Multi-Viewport / Platform Windows
        io.setConfigViewportsNoTaskBarIcon(true);
    }

    public void render(Window window) {
        IGuiInstance sceneGuiInstance = window.getCurrentScene().getGuiInstance();
        IGuiInstance windowGuiInstance = window.getGuiInstance();

        if(windowGuiInstance == null && sceneGuiInstance == null) {
            return;
        }

        imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        if(windowGuiInstance != null) {
            windowGuiInstance.drawGui();
        }
        if(sceneGuiInstance != null) {
            sceneGuiInstance.drawGui();
        }
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        // Update and Render additional Platform Windows
        // (Platform functions may change the current OpenGL context, so we save/restore it to make it easier to paste this code elsewhere.
        //  For this specific demo app we could also call glfwMakeContextCurrent(window) directly)
        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupCurrentContext = GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backupCurrentContext);
        }
    }
}
