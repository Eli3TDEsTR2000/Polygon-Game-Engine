package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.SkyBox;
import org.polygon.engine.core.utils.ShapeGenerator;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;

public class SkyBoxRender {
    private ShaderProgram shaderProgram;
    private UniformMap uniformMap;
    // Holds the viewMatrix to modify it and disable transformation before sending the data to the shader.
    private Matrix4f viewMatrix;
    private Mesh cubeMesh;

    public SkyBoxRender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/skybox.vert"
                , GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/skybox.frag"
                , GL_FRAGMENT_SHADER));

        shaderProgram = new ShaderProgram(shaderModuleDataList);
        viewMatrix = new Matrix4f();
        cubeMesh = ShapeGenerator.generateCube();
        createUniforms();
    }

    public void cleanup() {
        shaderProgram.cleanup();
        if (cubeMesh != null) {
            cubeMesh.cleanup();
        }
    }

    private void createUniforms() {
        uniformMap = new UniformMap(shaderProgram.getProgramId());
        uniformMap.createUniform("projectionMatrix");
        uniformMap.createUniform("viewMatrix");
        uniformMap.createUniform("environmentMapSampler");
    }

    public void render(Scene scene) {
        SkyBox skyBox = scene.getSkyBox();

        if(skyBox == null) {
            return;
        }

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(false);
        glDisable(GL_CULL_FACE);

        shaderProgram.bind();

        // disable transformation for the skybox to stay in the center and
        // only rotates the skybox based on the viewMatrix.
        viewMatrix.set(scene.getCamera().getViewMatrix());
        viewMatrix.m30(0);
        viewMatrix.m31(0);
        viewMatrix.m32(0);

        uniformMap.setUniform("projectionMatrix", scene.getProjection().getProjMatrix());
        uniformMap.setUniform("viewMatrix", viewMatrix);

        final int SKYBOX_TEXTURE_UNIT = 5;

        if (skyBox.getIBLData() != null && skyBox.getEnvironmentMapTextureId() != -1) {
            uniformMap.setUniform("environmentMapSampler", SKYBOX_TEXTURE_UNIT);

            int textureId = skyBox.getEnvironmentMapTextureId();
            if (textureId <= 0) {
                 System.err.println("Error: Invalid cubemap texture ID!");
            }

            glActiveTexture(GL_TEXTURE0 + SKYBOX_TEXTURE_UNIT);
            glBindTexture(GL_TEXTURE_CUBE_MAP, textureId);

            glBindVertexArray(cubeMesh.getVaoId());
            glDrawElements(GL_TRIANGLES, cubeMesh.getNumVertices(), GL_UNSIGNED_INT, 0);
        }

        glBindVertexArray(0);
        shaderProgram.unbind();
        glEnable(GL_CULL_FACE);
        glDepthFunc(GL_LESS);
        glDepthMask(true);
    }
}
