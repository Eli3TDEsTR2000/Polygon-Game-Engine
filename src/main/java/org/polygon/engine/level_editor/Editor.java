package org.polygon.engine.level_editor;

import org.polygon.engine.core.IGameLogic;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.EngineRender;
import org.polygon.engine.core.scene.*;

import static org.lwjgl.glfw.GLFW.*;

public class Editor implements IGameLogic {
    private final float MOUSE_SENSITIVITY = 0.1f;
    private final float MOVEMENT_SPEED = 0.005f;
    private Scene editorScene;


    @Override
    public void init(Window window, EngineRender render) {
        editorScene = window.createScene();
        editorScene.getCamera().moveUp(1);
        window.setCurrentScene(editorScene);
        render.getPreRenders().add(new EndlessGridRender());
    }

    @Override
    public void input(Window window, long diffTimeMS, boolean inputConsumed) {
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
    }
}
