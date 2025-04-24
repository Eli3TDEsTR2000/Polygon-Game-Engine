package org.polygon.engine.core.graph;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class FXAARender {

    private ShaderProgram shaderProgram;
    private UniformMap uniformMap;
    private QuadMesh quadMesh;
    private Vector2f inverseScreenSize;

    public FXAARender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/fxaa.vert", GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/fxaa.frag", GL_FRAGMENT_SHADER));
        shaderProgram = new ShaderProgram(shaderModuleDataList);
        createUniforms();
        quadMesh = new QuadMesh();
        inverseScreenSize = new Vector2f();
    }

    public void cleanup() {
        shaderProgram.cleanup();
        if (quadMesh != null) {
            quadMesh.cleanup();
        }
    }

    private void createUniforms() {
        uniformMap = new UniformMap(shaderProgram.getProgramId());
        uniformMap.createUniform("sceneSampler");
        uniformMap.createUniform("inverseScreenSize");
    }

    public void render(int sceneTextureId, int width, int height) {
        shaderProgram.bind();

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);

        // Set uniforms
        uniformMap.setUniform("sceneSampler", 0); // Texture unit 0
        inverseScreenSize.set(1.0f / width, 1.0f / height);
        uniformMap.setUniform("inverseScreenSize", inverseScreenSize);

        // Bind input texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sceneTextureId);

        // Render the quad
        glBindVertexArray(quadMesh.getVaoId());
        glDrawElements(GL_TRIANGLES, quadMesh.getNumVertices(), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        shaderProgram.unbind();
    }
} 