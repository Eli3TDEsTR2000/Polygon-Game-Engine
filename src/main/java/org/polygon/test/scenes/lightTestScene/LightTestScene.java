package org.polygon.test.scenes.lightTestScene;

import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.*;
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
        Model cube = ModelLoader.loadModel("Cube-model", "resources/models/cube/cube.obj"
                , scene.getTextureCache());
        scene.addModel(cube);

        Model backpack = ModelLoader.loadModel("backpack-model"
                , "resources/models/backpack/Survival_BackPack_2.fbx"
                , scene.getTextureCache());
        scene.addModel(backpack);

        scene.getTextureCache().createTexture("resources/models/backpack/textures/1001_albedo.jpg");
        backpack.getMaterialList().get(0).setTexturePath("resources/models/backpack/textures/1001_albedo.jpg");

        Model terrain = ModelLoader.loadModel("terrain-model", "resources/models/terrain/terrain.obj"
                , scene.getTextureCache());
        scene.addModel(terrain);

        Entity cubeEntity = new Entity("Cube-01", cube.getModelId());
        Entity backpackEntity = new Entity("backpack-01", backpack.getModelId());
        cubeEntity.setPosition(0, 0, -2);
        backpackEntity.setPosition(2, 0.71f, -2);
        backpackEntity.setScale(0.005f);
        cubeEntity.updateModelMatrix();
        backpackEntity.updateModelMatrix();
        scene.addEntity(cubeEntity);
        scene.addEntity(backpackEntity);

        Entity terrainEntity = new Entity("terrain-entity", terrain.getModelId());
        terrainEntity.setPosition(0, -0.5f, 0);
        terrainEntity.setScale(100f);
        terrainEntity.updateModelMatrix();
        scene.addEntity(terrainEntity);

        SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(0.3f);
        sceneLights.getPointLightList().add(new PointLight());
        PointLight pointLight = sceneLights.getPointLightList().get(0);
        pointLight.setIntensity(1);
        sceneLights.getSpotLightList().add(new SpotLight());
        sceneLights.getSpotLightList().get(0).setIntensity(0);

        scene.setSceneLights(sceneLights);
        scene.setGuiInstance(new LightTestGUI(scene));

        SkyBox skyBox = new SkyBox("resources/models/skybox/skybox.obj"
                , scene.getTextureCache());
        scene.getTextureCache().createTexture("resources/models/skybox/skybox.jpeg");
        skyBox.getSkyBoxModel().getMaterialList().get(1).setTexturePath("resources/models/skybox/skybox.jpeg");
        skyBox.getSkyBoxEntity().setPosition(0, -70, -120);
        skyBox.getSkyBoxEntity().setScale(40f);
        skyBox.getSkyBoxEntity().updateModelMatrix();
        scene.setSkyBox(skyBox);

        scene.setFog(new Fog(true, new Vector3f(0.5f, 0.5f, 0.5f), 0.02f));
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
