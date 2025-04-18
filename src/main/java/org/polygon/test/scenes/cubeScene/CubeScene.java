package org.polygon.test.scenes.cubeScene;

import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.Camera;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.ModelLoader;
import org.polygon.test.scenes.BasicScene;

import static org.lwjgl.glfw.GLFW.*;

public class CubeScene extends BasicScene {

    private final float MOUSE_SENSITIVITY = 0.1f;
    private final float MOVEMENT_SPEED = 0.005f;
    private Entity cubeEntity;
    private float rotation;

    public CubeScene(Window window) {
        super(window);
    }

    @Override
    public void init() {
        Model cube = ModelLoader.loadModel("Cube", "resources/models/cube/cube.obj"
                , scene.getTextureCache(), false);
        scene.addModel(cube);

        cubeEntity = new Entity("Cube-01", cube.getModelId());
        cubeEntity.setPosition(0, 0, -2);
        scene.addEntity(cubeEntity);
        rotation = 0;

        scene.setGuiInstance(new UpdateFpsTestGUI(window));
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
        rotation += 15 * diffTimeMS / 1000.0f;
        if (rotation > 360) {
            rotation = 0;
        }
        cubeEntity.setRotation(1, 1, 1, (float) Math.toRadians(rotation));
    }

    @Override
    public void cleanup() {
        scene.cleanup();
    }
}
