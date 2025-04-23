package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.gui.GuiRender;
import org.polygon.engine.core.scene.Scene;

import static org.lwjgl.opengl.GL40.*;

public class EngineRender {
    private ShadowRender shadowRender;
    private GBuffer gBuffer;
    private SceneRender sceneRender;
    private LightsRender lightsRender;
    private GuiRender guiRender;
    private SkyBoxRender skyBoxRender;
    public EngineRender(Window window) {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        shadowRender = new ShadowRender();
        gBuffer = new GBuffer(window);
        sceneRender = new SceneRender();
        lightsRender = new LightsRender();
        guiRender = new GuiRender(window);
        skyBoxRender = new SkyBoxRender();
    }

    public void cleanup() {
        shadowRender.cleanup();
        gBuffer.cleanup();
        sceneRender.cleanup();
        lightsRender.cleanup();
        guiRender.cleanup();
        skyBoxRender.cleanup();
    }

    private void lightRenderFinish() {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    private void lightRenderStart(Window window) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, window.getWidth(), window.getHeight());

        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_ONE, GL_ONE);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, gBuffer.getGBufferId());
    }

    public void render(Window window) {
        Scene scene = window.getCurrentScene();

        shadowRender.render(scene);
        glEnable(GL_CULL_FACE);
        sceneRender.render(scene, gBuffer);
        lightRenderStart(window);
        lightsRender.render(scene, shadowRender, gBuffer, window.getWidth(), window.getHeight());
        skyBoxRender.render(scene);
        lightRenderFinish();
        guiRender.render(window);
    }
}
