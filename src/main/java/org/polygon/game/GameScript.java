package org.polygon.game;

import org.polygon.engine.core.IGameLogic;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.EngineRender;
import org.polygon.engine.core.scene.Scene;
import org.polygon.game.scenes.quadScene.QuadScene;
import org.polygon.game.scenes.triangleScene.TriangleScene;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_Q;

public class GameScript implements IGameLogic {

    @Override
    public void init(Window window, Scene scene, EngineRender render) {
        TriangleScene.initScene(scene);
    }

    @Override
    public void input(Window window, Scene scene, long diffTimeMS) {
        if(window.isKeyPressed(GLFW_KEY_Q)) {
            QuadScene.initScene(scene);
        }
    }

    @Override
    public void update(Window window, Scene scene, long diffTimeMS) {

    }

    @Override
    public void cleanup() {

    }
}
