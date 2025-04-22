package org.polygon.engine.core.graph;

import org.joml.*;
import org.polygon.engine.core.scene.AnimationData;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.lights.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class SceneRender {
    private ShaderProgram shaderProgram;
    private UniformMap uniformMap;
    public SceneRender() {
        // This will hold shader modules
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(
                "resources/shaders/scene.frag", GL_FRAGMENT_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(
                "resources/shaders/scene.vert", GL_VERTEX_SHADER));
        // Initialize A shader program
        shaderProgram = new ShaderProgram(shaderModuleDataList);

        // Initialize a uniformMap
        createUniforms();
    }

    private void createUniforms() {
        uniformMap = new UniformMap(shaderProgram.getProgramId());
        uniformMap.createUniform("projectionMatrix");
        uniformMap.createUniform("viewMatrix");
        uniformMap.createUniform("modelMatrix");

        uniformMap.createUniform("textSampler");
        uniformMap.createUniform("normalSampler");

        uniformMap.createUniform("material.diffuse");
        uniformMap.createUniform("material.specular");
        uniformMap.createUniform("material.reflectance");
        uniformMap.createUniform("material.hasNormalMap");

        uniformMap.createUniform("bonesMatrices");
    }

    public void cleanup() {
        // Destroy programId reference from shader program
        shaderProgram.cleanup();
    }

    public void render(Scene scene, GBuffer gBuffer) {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, gBuffer.getGBufferId());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, gBuffer.getWidth(), gBuffer.getHeight());
        glDisable(GL_BLEND);

        shaderProgram.bind();

        // Set the projectionMatrix uniform with the projection matrix stored in scene.
        uniformMap.setUniform("projectionMatrix", scene.getProjection().getProjMatrix());
        // set the viewMatrix uniform with the scene's camera viewMatrix.
        uniformMap.setUniform("viewMatrix", scene.getCamera().getViewMatrix());
        // Set the textSampler uniform with 0 (one texture unit)
        uniformMap.setUniform("textSampler", 0);
        uniformMap.setUniform("normalSampler", 1);


        // Draw calls initiated here

        Collection<Model> models = scene.getModelMap().values();
        TextureCache textureCache = scene.getTextureCache();
        for(Model model : models) {
            List<Entity> entityList = model.getEntityList();

            for(Material material : model.getMaterialList()) {
                boolean hasNormalMap = material.getNormalMapPath() != null;

                uniformMap.setUniform("material.diffuse", material.getDiffuseColor());
                uniformMap.setUniform("material.specular", material.getSpecularColor());
                uniformMap.setUniform("material.reflectance", material.getReflectance());
                uniformMap.setUniform("material.hasNormalMap", hasNormalMap ? 1 : 0);
                glActiveTexture(GL_TEXTURE0);
                textureCache.getTexture(material.getTexturePath()).bind();

                if(hasNormalMap) {
                    glActiveTexture(GL_TEXTURE1);
                    textureCache.getTexture(material.getNormalMapPath()).bind();
                }

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

        glBindVertexArray(0);
        glEnable(GL_BLEND);
        shaderProgram.unbind();
    }
}
