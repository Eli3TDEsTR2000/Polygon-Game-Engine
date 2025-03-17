package org.polygon.test.scenes.lightTestScene;

import org.joml.AxisAngle4f;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.Camera;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.ModelLoader;
import org.polygon.engine.core.scene.SkyBox;
import org.polygon.engine.core.scene.lights.PointLight;
import org.polygon.engine.core.scene.lights.SceneLights;
import org.polygon.engine.core.scene.lights.SpotLight;
import org.polygon.test.scenes.BasicScene;

import static org.lwjgl.glfw.GLFW.*;

public class LightTestScene extends BasicScene {

    private final float MOUSE_SENSITIVITY = 0.1f;
    private final float MOVEMENT_SPEED = 0.005f;
    public LightTestScene(Window window) {
        super(window);
    }

    @Override
    public void init() {
        Model cube = ModelLoader.loadModel("Cube", "resources/models/cube/cube.obj"
                , scene.getTextureCache());
        scene.addModel(cube);

        Entity cubeEntity = new Entity("Cube-01", cube.getModelId());
        Entity cubeEntity2 = new Entity("Cube-02", cube.getModelId());
        cubeEntity.setPosition(0, 0, -2);
        cubeEntity2.setPosition(2, 0, -2);
        cubeEntity.updateModelMatrix();
        cubeEntity2.updateModelMatrix();
        scene.addEntity(cubeEntity);
        scene.addEntity(cubeEntity2);

        SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(0.2f);
        sceneLights.getPointLightList().add(new PointLight());
        sceneLights.getSpotLightList().add(new SpotLight());

        scene.setSceneLights(sceneLights);
        scene.setGuiInstance(new LightTestGUI(scene));

        SkyBox skyBox = new SkyBox("resources/models/skybox/skybox.obj"
                , scene.getTextureCache());
        scene.getTextureCache().createTexture("resources/models/skybox/skybox.jpeg");
        skyBox.getSkyBoxModel().getMaterialList().get(1).setTexturePath("resources/models/skybox/skybox.jpeg");
        skyBox.getSkyBoxEntity().setScale(40f);
        skyBox.getSkyBoxEntity().updateModelMatrix();
        scene.setSkyBox(skyBox);
    }

    @Override
    public void input(Window window, long diffTimeMS) {
        float incrementMovement = diffTimeMS * MOVEMENT_SPEED;
        Camera camera = window.getCurrentScene().getCamera();
        if(window.isKeyPressed(GLFW_KEY_W)) {
            camera.moveForward(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_S)) {
            camera.moveBackward(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_A)) {
            camera.moveLeft(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_D)) {
            camera.moveRight(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_SPACE)) {
            camera.moveUp(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_V)) {
            camera.moveDown(incrementMovement);
        }

        if(window.getMouseInputHandler().isRightButtonPressed()) {
            camera.addRotation(
                    (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().y * MOUSE_SENSITIVITY));
        }
    }

    @Override
    public void update(Window window, long diffTimeMS) {

    }
}
