package org.polygon.test.scenes.cubeScene;

import org.joml.Vector4f;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.Material;
import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.graph.Texture;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.Scene;
import org.polygon.test.scenes.BasicScene;
import org.polygon.test.scenes.BasicShape;
import org.polygon.test.scenes.cubeScene.meshes.BasicCube;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public class CubeScene extends BasicScene {
    private Entity cubeEntity;
    private Vector4f positionScaleVector;
    private float rotation;
    @Override
    public void initScene(Scene scene) {
        scene.resetScene();
        BasicShape cubeShape = new BasicCube();
        Material material = new Material();
        material.getMeshList().add(cubeShape.getMesh());
        scene.getTextureCache().createTexture("resources/models/cube/cube.png");
        material.setTexturePath("resources/models/cube/cube.png");
        Model cube = new Model("Cube", new ArrayList<>());
        cube.getMaterialList().add(material);
        scene.addModel(cube);

        cubeEntity = new Entity("Cube-01", cube.getModelId());
        cubeEntity.setPosition(0, 0, -2);
        cubeEntity.updateModelMatrix();
        scene.addEntity(cubeEntity);
        rotation = 0;
        positionScaleVector = new Vector4f();
    }

    @Override
    public void input(Window window, Scene scene, long diffTimeMS) {
        positionScaleVector.zero();
        if(window.isKeyPressed(GLFW_KEY_UP)) {
            positionScaleVector.y = 1;
        } else if(window.isKeyPressed(GLFW_KEY_DOWN)) {
            positionScaleVector.y = -1;

        } else if(window.isKeyPressed(GLFW_KEY_LEFT)) {
            positionScaleVector.x = -1;

        } else if(window.isKeyPressed(GLFW_KEY_RIGHT)) {
            positionScaleVector.x = 1;

        } else if(window.isKeyPressed(GLFW_KEY_Q)) {
            positionScaleVector.z = -1;
        } else if(window.isKeyPressed(GLFW_KEY_E)) {
            positionScaleVector.z = 1;

        } else if(window.isKeyPressed(GLFW_KEY_A)) {
            positionScaleVector.w = 1;

        } else if(window.isKeyPressed(GLFW_KEY_D)) {
            positionScaleVector.w = -1;
        }

        positionScaleVector.mul(diffTimeMS / 1000.0f);
        cubeEntity.setPosition(
                cubeEntity.getPosition().x + positionScaleVector.x,
                cubeEntity.getPosition().y + positionScaleVector.y,
                cubeEntity.getPosition().z + positionScaleVector.z
                );
        cubeEntity.setScale(cubeEntity.getScale() + positionScaleVector.w);
        cubeEntity.updateModelMatrix();
    }

    @Override
    public void update(Window window, Scene scene, long diffTimeMS) {
        rotation += 15 * diffTimeMS / 1000.0f;
        if (rotation > 360) {
            rotation = 0;
        }
        cubeEntity.setRotation(1, 1, 1, (float) Math.toRadians(rotation));
        cubeEntity.updateModelMatrix();
    }
}
