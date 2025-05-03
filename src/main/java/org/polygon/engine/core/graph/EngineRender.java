package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL;
import org.polygon.engine.core.IRender;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.gui.GuiRender;
import org.polygon.engine.core.scene.Scene;

import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class EngineRender {
    private ShadowRender shadowRender;
    private GBuffer gBuffer;
    private SceneRender sceneRender;
    private LightsRender lightsRender;
    private GuiRender guiRender;
    private SkyBoxRender skyBoxRender;
    private SceneFBO sceneFBO;
    private FXAARender fxaaRender;

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

        shadowRender = new ShadowRender();
        gBuffer = new GBuffer(window);
        sceneFBO = new SceneFBO(window);
        sceneRender = new SceneRender();
        lightsRender = new LightsRender();
        guiRender = new GuiRender(window);
        skyBoxRender = new SkyBoxRender();
        fxaaRender = new FXAARender();
    }

    public void cleanup() {
        shadowRender.cleanup();
        gBuffer.cleanup();
        sceneFBO.cleanup();
        sceneRender.cleanup();
        lightsRender.cleanup();
        guiRender.cleanup();
        skyBoxRender.cleanup();
        fxaaRender.cleanup();
    }

    public void render(Window window) {
        assertDefaultGL();

        Scene scene = window.getCurrentScene();

        // Shadow Pass
        shadowRender.render(scene);

        // Geometry Pass, draws to the G-Buffer FBO.
        sceneRender.render(scene, gBuffer);

        bindIntermediateFBO();

        // Base Lighting Pass, draws to the SceneFBO.
        lightsRender.render(scene, shadowRender, gBuffer, sceneFBO.getWidth(), sceneFBO.getHeight());

        // Skybox Pass
        skyBoxRender.render(scene);

        unbindIntermediateFBO(window);
        // Post Processing: FXAA Pass, draws to the screen.
        fxaaRender.render(sceneFBO.getTextureId(), window);

        // GUI Pass draws to the screen.
        guiRender.render(window);
    }

    // Binds an intermediateFBO to draw into. used for final Image post-processing.
    private void bindIntermediateFBO() {
        // Intermediate FBO used for final image post-processing.
        sceneFBO.bind();

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, sceneFBO.getWidth(), sceneFBO.getHeight());
    }

    private void unbindIntermediateFBO(Window window) {
        sceneFBO.unbind();
        glViewport(0, 0, window.getWidth(), window.getHeight());
    }

    private void assertDefaultGL() {
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glDepthFunc(GL_LESS);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
    }

    public List<IRender> getPreRenders() {
        return sceneRender.getPreRenders();
    }
}
