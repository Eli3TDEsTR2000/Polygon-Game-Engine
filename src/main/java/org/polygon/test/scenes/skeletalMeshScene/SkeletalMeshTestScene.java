package org.polygon.test.scenes.skeletalMeshScene;

import org.joml.Vector3f;
import org.lwjgl.openal.AL11;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.*;
import org.polygon.engine.core.scene.lights.*;
import org.polygon.engine.core.sound.SoundBuffer;
import org.polygon.engine.core.sound.SoundListener;
import org.polygon.engine.core.sound.SoundManager;
import org.polygon.engine.core.sound.SoundSource;
import org.polygon.test.scenes.BasicScene;

import static org.lwjgl.glfw.GLFW.*;

public class SkeletalMeshTestScene extends BasicScene {
    private final float MOUSE_SENSITIVITY = 0.1f;
    private final float MOVEMENT_SPEED = 0.005f;
    private SoundSource playerSoundSource;
    private SoundManager soundManager;

    private AnimationData animationData;

    public SkeletalMeshTestScene(Window window) {
        super(window);
    }
    @Override
    public void init() {
        String bobModelId = "bobModel";
        Model bobModel = ModelLoader.loadModel(bobModelId, "resources/models/bob/boblamp.md5mesh",
                scene.getTextureCache(), true);
        scene.addModel(bobModel);
        Entity bobEntity = new Entity("bobEntity", bobModelId);
        bobEntity.setPosition(0, -1, -2);
        bobEntity.setScale(0.03f);
        animationData = new AnimationData(bobModel.getAnimationList().get(0));
        bobEntity.setAnimationData(animationData);
        scene.addEntity(bobEntity);

        SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(0.3f);
        sceneLights.getPointLightList().add(new PointLight());
        PointLight pointLight = sceneLights.getPointLightList().get(0);
        pointLight.setIntensity(5);
        sceneLights.getSpotLightList().add(new SpotLight());
        sceneLights.getSpotLightList().get(0).setIntensity(0);

        scene.setSceneLights(sceneLights);
        scene.setGuiInstance(new AnimationTestGUI(animationData));

        initSound(bobEntity.getPosition(), scene.getCamera());
    }

    public void initSound(Vector3f position,Camera camera) {
        soundManager = new SoundManager();
        soundManager.setAttenuationModel(AL11.AL_EXPONENT_DISTANCE);
        soundManager.setSoundListener(new SoundListener(camera.getPosition()));

        SoundBuffer creakBuffer = new SoundBuffer("resources/sounds/creak1.ogg");
        soundManager.addSoundBuffer(creakBuffer);
        playerSoundSource = new SoundSource(false, false);
        playerSoundSource.setPosition(position);
        playerSoundSource.setBuffer(creakBuffer.getBufferId());
        soundManager.addSoundSource("CREAK", playerSoundSource);

        SoundBuffer backgroundSoundBuffer = new SoundBuffer("resources/sounds/woo_scary.ogg");
        soundManager.addSoundBuffer(backgroundSoundBuffer);
        SoundSource backgroundSoundSource = new SoundSource(true, true);
        backgroundSoundSource.setBuffer(backgroundSoundBuffer.getBufferId());
        soundManager.addSoundSource("MUSIC", backgroundSoundSource);
        backgroundSoundSource.play();
    }

    @Override
    public void input(Window window, long diffTimeMS) {
        float incrementMovement = diffTimeMS * MOVEMENT_SPEED;
        Camera camera = window.getCurrentScene().getCamera();
        if(window.isKeyPressed(GLFW_KEY_W)) {
            camera.moveForward(incrementMovement);
            soundManager.updateListenerPosition(window.getCurrentScene().getCamera());
        }
        if(window.isKeyPressed(GLFW_KEY_S)) {
            camera.moveBackward(incrementMovement);
            soundManager.updateListenerPosition(window.getCurrentScene().getCamera());
        }
        if(window.isKeyPressed(GLFW_KEY_A)) {
            camera.moveLeft(incrementMovement);
            soundManager.updateListenerPosition(window.getCurrentScene().getCamera());
        }
        if(window.isKeyPressed(GLFW_KEY_D)) {
            camera.moveRight(incrementMovement);
            soundManager.updateListenerPosition(window.getCurrentScene().getCamera());
        }
        if(window.isKeyPressed(GLFW_KEY_SPACE)) {
            camera.moveUp(incrementMovement);
            soundManager.updateListenerPosition(window.getCurrentScene().getCamera());
        }
        if(window.isKeyPressed(GLFW_KEY_V)) {
            camera.moveDown(incrementMovement);
            soundManager.updateListenerPosition(window.getCurrentScene().getCamera());
        }

        if(window.getMouseInputHandler().isRightButtonPressed()) {
            camera.addRotation(
                    (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().y * MOUSE_SENSITIVITY));
            soundManager.updateListenerPosition(window.getCurrentScene().getCamera());
        }
    }

    @Override
    public void update(Window window, long diffTimeMS) {
        animationData.nextFrame(diffTimeMS / 1000.0f);
        if (animationData.getCurrentFrameIndex() == 45) {
            playerSoundSource.play();
        }
    }

    @Override
    public void cleanup() {
        scene.cleanup();
        soundManager.cleanup();
    }
}
