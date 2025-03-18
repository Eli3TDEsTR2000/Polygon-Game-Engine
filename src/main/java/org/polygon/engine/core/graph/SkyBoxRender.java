package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.SkyBox;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;
public class SkyBoxRender {
    private ShaderProgram shaderProgram;
    private UniformMap uniformMap;
    // Holds the viewMatrix to modify it and disable transformation before sending the data to the shader.
    private Matrix4f viewMatrix;

    public SkyBoxRender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/skybox.vert"
                , GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/skybox.frag"
                , GL_FRAGMENT_SHADER));

        shaderProgram = new ShaderProgram(shaderModuleDataList);
        viewMatrix = new Matrix4f();
        createUniforms();
    }

    public void cleanup() {
        shaderProgram.cleanup();
    }

    private void createUniforms() {
        uniformMap = new UniformMap(shaderProgram.getProgramId());
        uniformMap.createUniform("projectionMatrix");
        uniformMap.createUniform("viewMatrix");
        uniformMap.createUniform("modelMatrix");
        uniformMap.createUniform("textSampler");
        uniformMap.createUniform("diffuse");
        uniformMap.createUniform("hasTexture");
    }

    public void render(Scene scene) {
        SkyBox skyBox = scene.getSkyBox();

        if(skyBox == null) {
            return;
        }

        glDisable(GL_CULL_FACE);
        shaderProgram.bind();

        // disable transformation for the skybox to stay in the center and
        // only rotates the skybox based on the viewMatrix.
        viewMatrix.set(scene.getCamera().getViewMatrix());
        viewMatrix.m30(0);
        viewMatrix.m31(0);
        viewMatrix.m32(0);

        uniformMap.setUniform("projectionMatrix", scene.getProjection().getMatrix());
        uniformMap.setUniform("viewMatrix", viewMatrix);
        uniformMap.setUniform("textSampler", 0);

        TextureCache textureCache = scene.getTextureCache();

        // draw skybox
        for(Material material : skyBox.getSkyBoxModel().getMaterialList()) {
            Texture texture = textureCache.getTexture(material.getTexturePath());
            glActiveTexture(GL_TEXTURE0);
            texture.bind();

            uniformMap.setUniform("diffuse", material.getDiffuseColor());
            if(texture.getTexturePath().equals(TextureCache.DEFAULT_TEXTURE)) {
                uniformMap.setUniform("hasTexture", 0);
            } else {
                uniformMap.setUniform("hasTexture", 1);
            }

            for(Mesh mesh : material.getMeshList()) {
                glBindVertexArray(mesh.getVaoId());

                uniformMap.setUniform("modelMatrix", skyBox.getSkyBoxEntity().getModelMatrix());
                glDrawElements(GL_TRIANGLES, mesh.getNumVertices(), GL_UNSIGNED_INT, 0);
            }
        }

        glBindVertexArray(0);
        shaderProgram.unbind();
        glEnable(GL_CULL_FACE);
    }
}
