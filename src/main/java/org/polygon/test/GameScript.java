package org.polygon.test;

import org.polygon.engine.core.IGameLogic;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.EngineRender;
import org.polygon.engine.core.scene.Scene;
import org.polygon.test.scenes.BasicScene;
import org.polygon.test.scenes.cubeScene.CubeScene;
import org.polygon.test.scenes.quadScene.QuadScene;
import org.polygon.test.scenes.triangleScene.TriangleScene;

import static org.lwjgl.glfw.GLFW.*;

public class GameScript implements IGameLogic {
    private BasicScene[] scenes = new BasicScene[] {
            new TriangleScene(),
            new QuadScene(),
            new CubeScene(),
    };

    int currentSceneIndex = 0;

    @Override
    public void init(Window window, Scene scene, EngineRender render) {
        scenes[currentSceneIndex].initScene(scene);
        glfwSetKeyCallback(window.getWindowHandle(), (windowRef, key, scancode, action, mods) -> {
            if(key == GLFW_KEY_4 && action == GLFW_PRESS) {
                if (currentSceneIndex < scenes.length - 1) {
                    currentSceneIndex++;
                    scenes[currentSceneIndex].initScene(scene);
                }
            } else if(key == GLFW_KEY_2 && action == GLFW_PRESS) {
                if(currentSceneIndex > 0) {
                    currentSceneIndex --;
                    scenes[currentSceneIndex].initScene(scene);
                }
            }
        });
    }

    @Override
    public void input(Window window, Scene scene, long diffTimeMS) {
        scenes[currentSceneIndex].input(window, scene, diffTimeMS);
    }

    @Override
    public void update(Window window, Scene scene, long diffTimeMS) {
        scenes[currentSceneIndex].update(window, scene, diffTimeMS);
    }

    @Override
    public void cleanup() {

    }
}
