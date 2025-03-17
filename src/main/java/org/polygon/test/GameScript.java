package org.polygon.test;

import org.polygon.engine.core.IGameLogic;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.EngineRender;
import org.polygon.test.scenes.BasicScene;
import org.polygon.test.scenes.cubeScene.CubeScene;
import org.polygon.test.scenes.lightTestScene.LightTestScene;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public class GameScript implements IGameLogic {

    ArrayList<BasicScene> scenes = new ArrayList<>();

    int currentSceneIndex = 0;

    @Override
    public void init(Window window, EngineRender render) {
        scenes.add(new CubeScene(window));
        scenes.add(new LightTestScene(window));
        scenes.get(currentSceneIndex).init();
        window.setCurrentScene(scenes.get(currentSceneIndex).getScene());
        window.addKeyCallback((handle, key, scancode, action, mods) -> {
            if(key == GLFW_KEY_4 && action == GLFW_PRESS && currentSceneIndex < scenes.size() - 1) {
                scenes.get(currentSceneIndex).cleanup();
                currentSceneIndex++;
                window.setCurrentScene(scenes.get(currentSceneIndex).getScene());
                scenes.get(currentSceneIndex).reset();
                window.getCurrentScene().resize(window.getWidth(), window.getHeight());
            } else if(key == GLFW_KEY_2 && action == GLFW_PRESS && currentSceneIndex > 0) {
                scenes.get(currentSceneIndex).cleanup();
                currentSceneIndex --;
                window.setCurrentScene(scenes.get(currentSceneIndex).getScene());
                scenes.get(currentSceneIndex).reset();
                window.getCurrentScene().resize(window.getWidth(), window.getHeight());
            }
        });
    }

    @Override
    public void input(Window window, long diffTimeMS, boolean inputConsumed) {
        // Stop handling scene input if a GUI instance is on focus of the mouse or keyboard.
        if(inputConsumed && window.getCurrentScene().getGuiInstance() != null) {
            return;
        }
        scenes.get(currentSceneIndex).input(window, diffTimeMS);
    }

    @Override
    public void update(Window window, long diffTimeMS) {
        scenes.get(currentSceneIndex).update(window, diffTimeMS);
    }

    @Override
    public void cleanup() {

    }
}
