package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.scene.Scene;

import static org.lwjgl.opengl.GL11.*;

public class EngineRender {
    private SceneRender sceneRender;
    public EngineRender() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
        sceneRender = new SceneRender();
    }

    public void cleanup() {
        sceneRender.cleanup();
    }

    public void render(Window window, Scene scene) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
        // Setup a viewport
        glViewport(0, 0, window.getWidth(), window.getHeight());
        // Render scene objects using shaders
        sceneRender.render(scene);
    }
}
