package org.polygon.test;

import org.polygon.engine.core.IGameLogic;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.EngineRender;
import org.polygon.test.scenes.BasicScene;
import org.polygon.test.scenes.cubeScene.CubeScene;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public class GameScript implements IGameLogic {

    ArrayList<BasicScene> scenes = new ArrayList<>();

    int currentSceneIndex = 0;

    @Override
    public void init(Window window, EngineRender render) {
        scenes.add(new CubeScene(window));
        window.setCurrentScene(scenes.get(currentSceneIndex).getScene());
        window.getCurrentScene().setGuiInstance(new ImGuiDemo());
        window.addKeyCallback((handle, key, scancode, action, mods) -> {
            if(key == GLFW_KEY_4 && action == GLFW_PRESS && currentSceneIndex < scenes.size() - 1) {
                currentSceneIndex++;
                window.setCurrentScene(scenes.get(currentSceneIndex).getScene());
                window.getCurrentScene().resize(window.getWidth(), window.getHeight());
            } else if(key == GLFW_KEY_2 && action == GLFW_PRESS && currentSceneIndex > 0) {
                currentSceneIndex --;
                window.setCurrentScene(scenes.get(currentSceneIndex).getScene());
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
