package org.polygon.test.scenes.normalScene;

import org.joml.Vector3f;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.*;
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
        Model damagedHelmet = ModelLoader.loadModel("damagedHelmet-model"
                , "resources/models/test/DamagedHelmet.gltf"
                , scene.getTextureCache(), false);
        scene.addModel(damagedHelmet);

        damagedHelmet.getMaterialList().get(0).setMetallicMapPath("resources/models/test/Default_metallic.jpg");
        scene.getTextureCache().createTexture(damagedHelmet.getMaterialList().get(0).getMetallicMapPath());

        damagedHelmet.getMaterialList().get(0).setRoughnessMapPath("resources/models/test/Default_roughness.jpg");
        scene.getTextureCache().createTexture(damagedHelmet.getMaterialList().get(0).getRoughnessMapPath());


        float spacingX = 2.5f;
        float spacingZ = 2.0f;
        float startX = -(5 - 1) * spacingX / 2.0f;
        float startZ = -2.0f;
        float posY = 0.71f;
        float scale = 1f;

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 5; col++) {
                String entityId = "damagedHelmet-" + row + "-" + col;
                Entity damagedHelmetEntity = new Entity(entityId, damagedHelmet.getModelId());
                float posX = startX + col * spacingX;
                float posZ = startZ - row * spacingZ;
                damagedHelmetEntity.setPosition(posX, posY, posZ);
                damagedHelmetEntity.setScale(scale);
//                damagedHelmetEntity.setRotation(1, 1, 1, (float)Math.toRadians(180.0));
                scene.addEntity(damagedHelmetEntity);
            }
        }

        SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(0.0f);
        sceneLights.getPointLightList().add(new PointLight());
        PointLight pointLight = sceneLights.getPointLightList().get(0);
        pointLight.setIntensity(1);
        sceneLights.getSpotLightList().add(new SpotLight());
        sceneLights.getSpotLightList().get(0).setIntensity(0);

        scene.setSceneLights(sceneLights);
        scene.setGuiInstance(new LightTestGUI(scene, "Light Test Scene - light controls"));

//        SkyBox skyBox = new SkyBox("resources/models/skybox/Sphere.fbx", scene.getTextureCache());
//        scene.getTextureCache().createTexture("resources/models/skybox/sky_water_landscape.jpg");
//        skyBox.getSkyBoxModel().getMaterialList().get(0).setTexturePath("resources/models/skybox/sky_water_landscape.jpg");
//        scene.setSkyBox(skyBox);

//        SkyBox skyBox = new SkyBox("resources/models/skybox/kloppenheim_06_puresky_4k.hdr"
//                , 1024, 32, 128);
//        scene.setSkyBox(skyBox);

//        scene.setFog(new Fog(true, new Vector3f(0.5f, 0.5f, 0.5f), 0.02f));
    }

    @Override
    public void input(Window window, long diffTimeMS) {
        int factor = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT) ? 3 : 1;
        float incrementMovement = diffTimeMS * MOVEMENT_SPEED * factor;
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