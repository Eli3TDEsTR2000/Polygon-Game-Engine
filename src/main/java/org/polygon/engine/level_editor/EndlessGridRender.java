package org.polygon.engine.level_editor;

import org.joml.Matrix4f;
import org.polygon.engine.core.IRender;
import org.polygon.engine.core.graph.ShaderProgram;
import org.polygon.engine.core.graph.UniformMap;
import org.polygon.engine.core.scene.Scene;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class EndlessGridRender implements IRender {
    private ShaderProgram shaderProgram;
    private UniformMap uniformMap;
    private int vaoId;

    public EndlessGridRender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/endless_grid.vert"
                , GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/endless_grid.frag"
                , GL_FRAGMENT_SHADER));
        shaderProgram = new ShaderProgram(shaderModuleDataList);

        vaoId = glGenVertexArrays();

        createUniforms();
    }

    private void createUniforms() {
        uniformMap = new UniformMap(shaderProgram.getProgramId());
        uniformMap.createUniform("gVP");
        uniformMap.createUniform("gCameraWorldPos");
    }

    @Override
    public void render(Scene scene) {
        glDisable(GL_CULL_FACE);
        shaderProgram.bind();
        Matrix4f gVP = new Matrix4f();
        scene.getProjection().getProjMatrix().mul(scene.getCamera().getViewMatrix(), gVP);

        uniformMap.setUniform("gVP", gVP);
        uniformMap.setUniform("gCameraWorldPos", scene.getCamera().getPosition());

        glBindVertexArray(vaoId);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        glBindVertexArray(0);
        shaderProgram.unbind();
        glEnable(GL_CULL_FACE);
    }
}
