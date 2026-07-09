package org.polygon.engine.level_editor;

import org.joml.Matrix4f;
import org.polygon.engine.core.IRenderPass;
import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.graph.ShaderProgram;
import org.polygon.engine.core.graph.UniformMap;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.lights.PointLight;
import org.polygon.engine.core.scene.lights.SceneLights;
import org.polygon.engine.core.utils.ShapeGenerator;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class PointLightSphereRender implements IRenderPass {
    private static final float SPHERE_RADIUS = 0.15f;

    private ShaderProgram shaderProgram;
    private UniformMap uniformMap;
    private Mesh sphereMesh;
    private Matrix4f modelMatrix;

    public PointLightSphereRender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/light_sphere.vert"
                , GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/light_sphere.frag"
                , GL_FRAGMENT_SHADER));
        shaderProgram = new ShaderProgram(shaderModuleDataList);

        sphereMesh = ShapeGenerator.generateSphere(SPHERE_RADIUS, 12, 12);

        modelMatrix = new Matrix4f();

        createUniforms();
    }

    private void createUniforms() {
        uniformMap = new UniformMap(shaderProgram.getProgramId());
        uniformMap.createUniform("projectionMatrix");
        uniformMap.createUniform("viewMatrix");
        uniformMap.createUniform("modelMatrix");
        uniformMap.createUniform("lightColor");
    }

    public void cleanup() {
        if (sphereMesh != null) {
            sphereMesh.cleanup();
        }
        shaderProgram.cleanup();
    }

    @Override
    public void render(Scene scene) {
        SceneLights sceneLights = scene.getSceneLights();
        if (sceneLights == null || sceneLights.getPointLightList().isEmpty()) {
            return;
        }

        shaderProgram.bind();

        uniformMap.setUniform("projectionMatrix", scene.getProjection().getProjMatrix());
        uniformMap.setUniform("viewMatrix", scene.getCamera().getViewMatrix());

        glBindVertexArray(sphereMesh.getVaoId());

        for (PointLight pointLight : sceneLights.getPointLightList()) {
            modelMatrix.identity().translate(pointLight.getPosition());
            uniformMap.setUniform("modelMatrix", modelMatrix);
            uniformMap.setUniform("lightColor", pointLight.getColor());

            glDrawElements(GL_TRIANGLES, sphereMesh.getNumVertices(), GL_UNSIGNED_INT, 0);
        }

        glBindVertexArray(0);
        shaderProgram.unbind();
    }
}