package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL;
import org.polygon.engine.core.IRenderPass;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.gui.GuiRender;
import org.polygon.engine.core.scene.Scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL40.*;

public class EngineRender {
    private ShadowRender shadowRender;
    private PointShadowRender pointShadowRender;
    private GBuffer gBuffer;
    private SceneRender sceneRender;
    private LightsRender lightsRender;
    private GuiRender guiRender;
    private SkyBoxRender skyBoxRender;
    private SceneFBO sceneFBO;
    private FXAARender fxaaRender;
    private SSAOBuffer ssaoBuffer;
    private SSAORender ssaoRender;
    private Frustum frustum;

    public enum RenderStage {
        POST_GEOMETRY,
        POST_LIGHTING,
    }

    private Map <RenderStage, List<IRenderPass>> renderPasses;

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
        pointShadowRender = new PointShadowRender();
        gBuffer = new GBuffer(window);
        sceneFBO = new SceneFBO(window);
        sceneRender = new SceneRender();
        lightsRender = new LightsRender();
        guiRender = new GuiRender(window);
        skyBoxRender = new SkyBoxRender();
        fxaaRender = new FXAARender();
        ssaoBuffer = new SSAOBuffer(window);
        ssaoRender = new SSAORender();
        renderPasses = new HashMap<>();
        frustum = new Frustum();
    }

    public void cleanup() {
        shadowRender.cleanup();
        pointShadowRender.cleanup();
        gBuffer.cleanup();
        sceneFBO.cleanup();
        sceneRender.cleanup();
        lightsRender.cleanup();
        guiRender.cleanup();
        skyBoxRender.cleanup();
        fxaaRender.cleanup();
        ssaoBuffer.cleanup();
        ssaoRender.cleanup();
    }

    public void render(Window window) {
        assertDefaultGL();

        Scene scene = window.getCurrentScene();

        // Shadow Pass
        shadowRender.render(scene);

        // Point lights shadow Pass
        pointShadowRender.render(scene);

        // Geometry Pass, draws to the G-Buffer FBO.
        sceneRender.render(scene, gBuffer, frustum);

        // POST_GEOMETRY Pass
        renderStage(RenderStage.POST_GEOMETRY, scene);

        // SSAO Pass
        if(window.getWindowOptions().ssaoEnabled) {
            ssaoRender.render(scene, gBuffer, ssaoBuffer);
        }

        bindIntermediateFBO();

        // Copy the real per-pixel scene depth from the GBuffer into the SceneFBO's depth buffer.
        blitGBufferDepth();

        // Base Lighting Pass, draws to the SceneFBO.
        int ssaoTextureId = window.getWindowOptions().ssaoEnabled ? ssaoBuffer.getBlurTextureId() : ssaoBuffer.getFallbackWhiteTextureId();
        lightsRender.render(scene, shadowRender, pointShadowRender
                , gBuffer, ssaoTextureId, sceneFBO.getWidth(), sceneFBO.getHeight());

        // POST_LIGHTING Pass
        renderStage(RenderStage.POST_LIGHTING, scene);

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
        glClear(GL_COLOR_BUFFER_BIT);
        glViewport(0, 0, sceneFBO.getWidth(), sceneFBO.getHeight());
    }

    private void blitGBufferDepth() {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, gBuffer.getGBufferId());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, sceneFBO.getFboId());
        glBlitFramebuffer(
                0, 0, gBuffer.getWidth(), gBuffer.getHeight(),
                0, 0, sceneFBO.getWidth(), sceneFBO.getHeight(),
                GL_DEPTH_BUFFER_BIT, GL_NEAREST);

        // Restore SceneFBO as both read and draw target for the passes that follow.
        glBindFramebuffer(GL_FRAMEBUFFER, sceneFBO.getFboId());
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
        glDisable(GL_FRAMEBUFFER_SRGB);
    }

    public void addRenderPass(RenderStage stage, IRenderPass renderPass) {
        renderPasses.computeIfAbsent(stage, renderPasses -> new ArrayList<>());
        renderPasses.get(stage).add(renderPass);
    }

    private void renderStage (RenderStage stage, Scene scene) {
        if(renderPasses.get(stage) != null) {
            for(IRenderPass renderPass : renderPasses.get(stage)) {
                renderPass.render(scene);
            }
        }
    }
}