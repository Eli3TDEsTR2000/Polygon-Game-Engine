package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.scene.Scene;

import static org.lwjgl.opengl.GL11.*;

public class EngineRender {
    public EngineRender() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
    }

    public void cleanup() {

    }

    public void render(Window window, Scene scene) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
    }
}
