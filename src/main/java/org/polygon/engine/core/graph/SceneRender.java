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

        uniformMap.createUniform("baseColorSampler");
        uniformMap.createUniform("normalSampler");
        uniformMap.createUniform("metallicSampler");
        uniformMap.createUniform("roughnessSampler");
        uniformMap.createUniform("aoSampler");
        uniformMap.createUniform("emissiveSampler");

        uniformMap.createUniform("material.diffuseColor");
        uniformMap.createUniform("material.metallic");
        uniformMap.createUniform("material.roughness");
        uniformMap.createUniform("material.aoStrength");
        uniformMap.createUniform("material.hasTexture");
        uniformMap.createUniform("material.hasNormalMap");
        uniformMap.createUniform("material.hasMetallicMap");
        uniformMap.createUniform("material.hasRoughnessMap");
        uniformMap.createUniform("material.hasAoMap");
        uniformMap.createUniform("material.hasEmissiveMap");

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

        // Ensure correct state for GBuffer pass
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE); // Standard back-face culling
        glCullFace(GL_BACK);

        shaderProgram.bind();

        // Set the projectionMatrix uniform with the projection matrix stored in scene.
        uniformMap.setUniform("projectionMatrix", scene.getProjection().getProjMatrix());
        // set the viewMatrix uniform with the scene's camera viewMatrix.
        uniformMap.setUniform("viewMatrix", scene.getCamera().getViewMatrix());
        // Set the texture sampler uniforms (texture units)
        uniformMap.setUniform("baseColorSampler", 0);
        uniformMap.setUniform("normalSampler", 1);
        uniformMap.setUniform("metallicSampler", 2);
        uniformMap.setUniform("roughnessSampler", 3);
        uniformMap.setUniform("aoSampler", 4);
        uniformMap.setUniform("emissiveSampler", 5);


        // Draw calls initiated here

        Collection<Model> models = scene.getModelMap().values();
        TextureCache textureCache = scene.getTextureCache();
        for(Model model : models) {
            List<Entity> entityList = model.getEntityList();

            for(Material material : model.getMaterialList()) {
                boolean hasTexture = material.getTexturePath() != null;
                boolean hasNormalMap = material.getNormalMapPath() != null;
                boolean hasMetallicMap = material.getMetallicMapPath() != null;
                boolean hasRoughnessMap = material.getRoughnessMapPath() != null;
                boolean hasAoMap = material.getAoMapPath() != null;
                boolean hasEmissiveMap = material.getEmissiveMapPath() != null;

                uniformMap.setUniform("material.diffuseColor", material.getDiffuseColor());
                uniformMap.setUniform("material.metallic", material.getMetallic());
                uniformMap.setUniform("material.roughness", material.getRoughness());
                uniformMap.setUniform("material.aoStrength", material.getAoStrength());
                uniformMap.setUniform("material.hasTexture", hasTexture ? 1 : 0);
                uniformMap.setUniform("material.hasNormalMap", hasNormalMap ? 1 : 0);
                uniformMap.setUniform("material.hasMetallicMap", hasMetallicMap ? 1 : 0);
                uniformMap.setUniform("material.hasRoughnessMap", hasRoughnessMap ? 1 : 0);
                uniformMap.setUniform("material.hasAoMap", hasAoMap ? 1 : 0);
                uniformMap.setUniform("material.hasEmissiveMap", hasEmissiveMap ? 1 : 0);

                // Bind textures
                int texUnit = 0;
                if (hasTexture) {
                    glActiveTexture(GL_TEXTURE0 + texUnit);
                    textureCache.getTexture(material.getTexturePath()).bind();
                }
                texUnit++;
                if(hasNormalMap) {
                    glActiveTexture(GL_TEXTURE0 + texUnit);
                    textureCache.getTexture(material.getNormalMapPath()).bind();
                }
                texUnit++;
                if(hasMetallicMap) {
                    glActiveTexture(GL_TEXTURE0 + texUnit);
                    textureCache.getTexture(material.getMetallicMapPath()).bind();
                }
                texUnit++;
                if(hasRoughnessMap) {
                    glActiveTexture(GL_TEXTURE0 + texUnit);
                    textureCache.getTexture(material.getRoughnessMapPath()).bind();
                }
                texUnit++;
                if(hasAoMap) {
                    glActiveTexture(GL_TEXTURE0 + texUnit);
                    textureCache.getTexture(material.getAoMapPath()).bind();
                }
                texUnit++;
                if(hasEmissiveMap) {
                    glActiveTexture(GL_TEXTURE0 + texUnit);
                    textureCache.getTexture(material.getEmissiveMapPath()).bind();
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
