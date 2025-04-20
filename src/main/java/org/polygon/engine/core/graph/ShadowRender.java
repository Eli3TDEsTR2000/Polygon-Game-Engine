package org.polygon.engine.core.graph;

import org.polygon.engine.core.scene.AnimationData;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.Scene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class ShadowRender {
    private List<CascadeShadow> cascadeShadowList;
    private ShaderProgram shaderProgram;
    private ShadowBuffer shadowBuffer;
    private UniformMap uniformMap;

    public ShadowRender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(
                new ShaderProgram.ShaderModuleData("resources/shaders/shadow.vert", GL_VERTEX_SHADER));
        shaderProgram = new ShaderProgram(shaderModuleDataList);

        shadowBuffer = new ShadowBuffer();

        cascadeShadowList = new ArrayList<>();
        for(int i = 0; i < CascadeShadow.SHADOW_MAP_CASCADE_COUNT; i++) {
            CascadeShadow cascadeShadow = new CascadeShadow();
            cascadeShadowList.add(cascadeShadow);
        }

        createUniforms();
    }

    public void cleanup() {
        shaderProgram.cleanup();
        shadowBuffer.cleanup();
    }

    private void createUniforms() {
        uniformMap = new UniformMap(shaderProgram.getProgramId());
        uniformMap.createUniform("modelMatrix");
        uniformMap.createUniform("projViewMatrix");
        uniformMap.createUniform("bonesMatrices");
    }

    public List<CascadeShadow> getCascadeShadowList() {
        return cascadeShadowList;
    }

    public ShadowBuffer getShadowBuffer() {
        return shadowBuffer;
    }

    public void render(Scene scene) {
        CascadeShadow.updateCascadeShadows(cascadeShadowList, scene);

        glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer.getDepthMapFBO());
        glViewport(0, 0, ShadowBuffer.SHADOW_MAP_WIDTH, ShadowBuffer.SHADOW_MAP_HEIGHT);

        shaderProgram.bind();

        Collection<Model> models = scene.getModelMap().values();
        for(int i = 0; i < CascadeShadow.SHADOW_MAP_CASCADE_COUNT; i++) {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowBuffer.getDepthMapTexture().getIds()[i], 0);
            glClear(GL_DEPTH_BUFFER_BIT);

            CascadeShadow cascadeShadow = cascadeShadowList.get(i);
            uniformMap.setUniform("projViewMatrix", cascadeShadow.getProjViewMatrix());

            for(Model model : models) {
                List<Entity> entityList = model.getEntityList();
                for(Material material : model.getMaterialList()) {
                    for(Mesh mesh : material.getMeshList()) {
                        glBindVertexArray(mesh.getVaoId());
                        for(Entity entity : entityList) {
                            uniformMap.setUniform("modelMatrix", entity.getModelMatrix());
                            AnimationData animationData = entity.getAnimationData();
                            if(animationData == null) {
                                uniformMap.setUniform("bonesMatrices", AnimationData.DEFAULT_BONES_MATRICES);
                            } else {
                                uniformMap.setUniform("bonesMatrices"
                                        , animationData.getCurrentFrame().boneMatrices());
                            }
                            glDrawElements(GL_TRIANGLES, mesh.getNumVertices(), GL_UNSIGNED_INT, 0);
                        }
                    }
                }
            }
        }

        shaderProgram.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
