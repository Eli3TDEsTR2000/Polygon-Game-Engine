package org.polygon.test.scenes.testScenesForIBL;

import org.joml.*;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.Material;
import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.*;
import org.polygon.engine.core.scene.lights.PointLight;
import org.polygon.engine.core.scene.lights.SceneLights;
import org.polygon.engine.core.scene.lights.SpotLight;
import org.polygon.test.scenes.BasicScene;
import org.polygon.test.scenes.normalScene.LightTestGUI;

import java.io.IOException;
import java.lang.Math;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class IBLTestScene extends BasicScene {
    private final float MOUSE_SENSITIVITY = 0.1f;
    private final float MOVEMENT_SPEED = 0.009f;

    public IBLTestScene(Window window) {
        super(window);
    }

    @Override
    public void init() {
        Model damagedHelmet = ModelLoader.loadModel("damagedHelmet-model"
                , "resources/models/test/DamagedHelmet.gltf", scene.getTextureCache(), false);
        Material damagedHelmetMaterial = damagedHelmet.getMaterialList().get(0);
        damagedHelmetMaterial.setRoughnessMapPath("resources/models/test/Default_roughness.jpg");
        damagedHelmetMaterial.setMetallicMapPath("resources/models/test/Default_metallic.jpg");
        scene.getTextureCache().createTexture(damagedHelmetMaterial.getMetallicMapPath());
        scene.getTextureCache().createTexture(damagedHelmetMaterial.getRoughnessMapPath());
        scene.addModel(damagedHelmet);

        Entity damagedHelmetEntity = new Entity("damagedHelmet-01", damagedHelmet.getModelId());
        damagedHelmetEntity.setPosition(0, 0, -5);
        scene.addEntity(damagedHelmetEntity);


        float spacingY = 2.5f;
        float spacingX = 2.3f;
        float startY = -(5 - 1) * spacingY / 2.0f;
        float startX = 10.0f;
        float posZ = -20f;
        float scale = 1f;

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Model sphere = ModelLoader.loadModel("sphere-model" + row + "-" + col
                        , "resources/models/sphere/sphere.fbx"
                        , scene.getTextureCache(), false);
                scene.addModel(sphere);

                sphere.getMaterialList().get(0).setDiffuseColor(new Vector4f(0, 1, 1, 1));
                sphere.getMaterialList().get(0).setMetallic(row / 10.0f);
                sphere.getMaterialList().get(0).setRoughness(col / 10.0f);

                String entityId = "sphere-" + row + "-" + col;
                Entity sphereEntity = new Entity(entityId, sphere.getModelId());
                float posY = startY + col * spacingY;
                float posX = startX - row * spacingX;
                sphereEntity.setPosition(posX, posY, posZ);
                sphereEntity.setScale(scale);
                scene.addEntity(sphereEntity);
            }
        }

        SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(0.1f);
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

        SkyBox skyBox = new SkyBox("resources/models/skybox/newport_loft.hdr"
                , 1024, 32, 128);
        scene.setSkyBox(skyBox);

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

        if(window.isKeyPressed(GLFW_KEY_1)) {
            SkyBox skyBox = new SkyBox("resources/models/skybox/newport_loft.hdr"
                    , 1024, 32, 128);
            scene.setSkyBox(skyBox);
        }

        if(window.isKeyPressed(GLFW_KEY_2)) {
            SkyBox skyBox = new SkyBox("resources/models/skybox/kloppenheim_06_puresky_4k.hdr"
                    , 1024, 32, 128);
            scene.setSkyBox(skyBox);
        }

        if(window.isKeyPressed(GLFW_KEY_3)) {
            SkyBox skyBox = new SkyBox("resources/models/skybox/studio_small_08_4k.hdr"
                    , 1024, 32, 128);
            scene.setSkyBox(skyBox);
        }

        if(window.isKeyPressed(GLFW_KEY_4)) {
            SkyBox skyBox = new SkyBox("resources/models/skybox/metro_noord_4k.hdr"
                    , 1024, 32, 128);
            scene.setSkyBox(skyBox);
        }

        if(window.isKeyPressed(GLFW_KEY_5)) {
            SkyBox skyBox = new SkyBox("resources/models/skybox/rogland_clear_night_4k.hdr"
                    , 1024, 32, 128);
            scene.setSkyBox(skyBox);
        }

//        if(window.isKeyPressed(GLFW_KEY_L)) {
//            try {
//                SceneSerialization.saveToFile("resources/levels/test.json", scene);
//            } catch(IOException e) {
//                System.err.println(e.getMessage());
//            }
//        }
//
//        if(window.isKeyPressed(GLFW_KEY_P)) {
//            try {
//                scene.cleanup();
//                scene = SceneSerialization.loadFromFile("resources/levels/test.json", window.getWidth(), window.getHeight());
//                window.setCurrentScene(scene);
//            } catch(IOException e) {
//                System.err.println(e.getMessage());
//            }
//        }

        if(window.getMouseInputHandler().isRightButtonPressed()) {
            camera.addRotation(
                    (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().y * MOUSE_SENSITIVITY));
        }
        if(window.getMouseInputHandler().isLeftButtonPressed()) {
            selectEntity(window);
            if(scene.getSelectedEntity() != null) {
                String modelId = scene.getSelectedEntity().getModelId();
                System.out.println("Metallic: "
                        + scene.getModelMap().get(modelId).getMaterialList().get(0).getMetallic());
                System.out.println("Roughness: "
                        + scene.getModelMap().get(modelId).getMaterialList().get(0).getRoughness());
            }
        }
    }

    public void selectEntity(Window window) {
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();

        Vector2f mousePos = window.getMouseInputHandler().getCurrentPosition();

        float x = (2 * mousePos.x) / windowWidth - 1.0f;
        float y = 1.0f - (2 * mousePos.y) / windowHeight;
        float z = -1.0f;

        Matrix4f invProjMatrix = window.getCurrentScene().getProjection().getInvProjMatrix();
        Vector4f mouseDir = new Vector4f(x, y, z, 1.0f);
        mouseDir.mul(invProjMatrix);
        mouseDir.z = -1.0f;
        mouseDir.w = 0.0f;

        Matrix4f invViewMatrix = window.getCurrentScene().getCamera().getInvViewMatrix();;
        mouseDir.mul(invViewMatrix);

        Vector4f min = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        Vector4f max = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        Vector2f nearFar = new Vector2f();

        Entity selectedEntity = null;
        float closestDistance = Float.POSITIVE_INFINITY;
        Vector3f center = scene.getCamera().getPosition();

        Collection<Model> models = scene.getModelMap().values();
        Matrix4f modelMatrix = new Matrix4f();
        for (Model model : models) {
            List<Entity> entities = model.getEntityList();
            for (Entity entity : entities) {
                modelMatrix.translate(entity.getPosition()).scale(entity.getScale());
                for (Material material : model.getMaterialList()) {
                    for (Mesh mesh : material.getMeshList()) {
                        Vector3f aabbMin = mesh.getAabbMinCorner();
                        min.set(aabbMin.x, aabbMin.y, aabbMin.z, 1.0f);
                        min.mul(modelMatrix);
                        Vector3f aabMax = mesh.getAabbMaxCorner();
                        max.set(aabMax.x, aabMax.y, aabMax.z, 1.0f);
                        max.mul(modelMatrix);
                        if (Intersectionf.intersectRayAab(center.x, center.y, center.z, mouseDir.x, mouseDir.y, mouseDir.z,
                                min.x, min.y, min.z, max.x, max.y, max.z, nearFar) && nearFar.x < closestDistance) {
                            closestDistance = nearFar.x;
                            selectedEntity = entity;
                        }
                    }
                }
                modelMatrix.identity();
            }
        }

        window.getCurrentScene().setSelectedEntity(selectedEntity);
    }

    @Override
    public void update(Window window, long diffTimeMS) {

    }

    @Override
    public void cleanup() {
        scene.cleanup();
    }
}
