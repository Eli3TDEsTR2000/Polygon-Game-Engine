package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.gui.GuiRender;
import org.polygon.engine.core.scene.Scene;

import static org.lwjgl.opengl.GL11.*;

public class EngineRender {
    private SceneRender sceneRender;
    private GuiRender guiRender;
    private SkyBoxRender skyBoxRender;
    public EngineRender(Window window) {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        sceneRender = new SceneRender();
        guiRender = new GuiRender(window);
        skyBoxRender = new SkyBoxRender();
    }

    public void cleanup() {
        sceneRender.cleanup();
        guiRender.cleanup();
        skyBoxRender.cleanup();
    }

    public void render(Window window) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
        // Setup a viewport.
        glViewport(0, 0, window.getWidth(), window.getHeight());

        Scene scene = window.getCurrentScene();

        // Render skybox first, any objects that have transparent materials will be blended to the skybox.
        skyBoxRender.render(scene);
        // Render scene's objects using shaders.
        sceneRender.render(scene);
        // Render scene's GUI instance and window's GUI instance.
        guiRender.render(window);
    }
}
