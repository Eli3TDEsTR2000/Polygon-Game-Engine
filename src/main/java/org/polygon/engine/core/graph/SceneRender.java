package org.polygon.engine.core.graph;

import org.polygon.engine.core.scene.Scene;

import java.util.ArrayList;
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
        uniformMap.createUniform("modelMatrix");
        uniformMap.createUniform("textSampler");
        uniformMap.createUniform("viewMatrix");
    }

    public void cleanup() {
        // Destroy programId reference from shader program
        shaderProgram.cleanup();
    }

    public void render(Scene scene) {
        shaderProgram.bind();

        // Set the projectionMatrix uniform with the projection matrix stored in scene.
        uniformMap.setUniform("projectionMatrix", scene.getProjection().getMatrix());
        // set the viewMatrix uniform with the scene's camera viewMatrix.
        uniformMap.setUniform("viewMatrix", scene.getCamera().getViewMatrix());
        // Set the textSampler uniform with 0 (one texture unit)
        uniformMap.setUniform("textSampler", 0);

        // Draw calls initiated here

        scene.getModelMap().values().forEach((model) -> {
            model.getMaterialList().forEach((material) -> {
                glActiveTexture(GL_TEXTURE0);
                scene.getTextureCache().getTexture(material.getTexturePath()).bind();
                material.getMeshList().forEach((mesh) -> {
                    glBindVertexArray(mesh.getVaoId());
                    model.getEntityList().forEach((entity) -> {
                        uniformMap.setUniform("modelMatrix", entity.getModelMatrix());
                        glDrawElements(GL_TRIANGLES, mesh.getNumVertices(), GL_UNSIGNED_INT, 0);
                    });
                });
            });
        });

        glBindVertexArray(0);

        shaderProgram.unbind();
    }
}
