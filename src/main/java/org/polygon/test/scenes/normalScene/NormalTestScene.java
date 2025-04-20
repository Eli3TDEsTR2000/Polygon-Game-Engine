package org.polygon.test.scenes.normalScene;

import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.Camera;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.ModelLoader;
import org.polygon.engine.core.scene.lights.PointLight;
import org.polygon.engine.core.scene.lights.SceneLights;
import org.polygon.engine.core.scene.lights.SpotLight;
import org.polygon.test.scenes.BasicScene;

import static org.lwjgl.glfw.GLFW.*;

public class NormalTestScene extends BasicScene {
    private final float MOUSE_SENSITIVITY = 0.1f;
    private final float MOVEMENT_SPEED = 0.005f;

    public NormalTestScene(Window window) {
        super(window);
    }

    @Override
    public void init() {
        String terrainModelId = "terrain";
        Model terrainModel = ModelLoader.loadModel(terrainModelId, "resources/models/terrain/terrain.obj",
                scene.getTextureCache(), false);
        scene.addModel(terrainModel);
        Entity terrainEntity = new Entity("terrainEntity", terrainModelId);
        terrainEntity.setScale(100.0f);
        terrainEntity.setPosition(0, -0.50f, 0);
        scene.addEntity(terrainEntity);

        Model backpack = ModelLoader.loadModel("backpack-model"
                , "resources/models/backpack/Survival_BackPack_2.fbx"
                , scene.getTextureCache(), false);
        scene.addModel(backpack);

        scene.getTextureCache().createTexture("resources/models/backpack/textures/1001_albedo.jpg");
        backpack.getMaterialList().get(0).setTexturePath("resources/models/backpack/textures/1001_albedo.jpg");

        scene.getTextureCache().createTexture("resources/models/backpack/textures/1001_normal.png");
        backpack.getMaterialList().get(0).setNormalMapPath("resources/models/backpack/textures/1001_normal.png");

        Entity backpackEntity = new Entity("backpack-01", backpack.getModelId());
        backpackEntity.setPosition(0, 0.71f, -2);
        backpackEntity.setScale(0.005f);
        scene.addEntity(backpackEntity);

        SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(0.3f);
        sceneLights.getPointLightList().add(new PointLight());
        PointLight pointLight = sceneLights.getPointLightList().get(0);
        pointLight.setIntensity(1);
        sceneLights.getSpotLightList().add(new SpotLight());
        sceneLights.getSpotLightList().get(0).setIntensity(0);

        scene.setSceneLights(sceneLights);
        scene.setGuiInstance(new LightTestGUI(scene, "Normal mapping test - light controls"));
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

    @Override
    public void cleanup() {
        scene.cleanup();
    }
}
