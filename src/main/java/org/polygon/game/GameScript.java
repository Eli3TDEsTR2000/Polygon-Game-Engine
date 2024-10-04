package org.polygon.game;

import org.polygon.engine.core.IGameLogic;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.EngineRender;
import org.polygon.engine.core.scene.Scene;
import org.polygon.game.scenes.triangleScene.MainScene;

public class GameScript implements IGameLogic {

    @Override
    public void init(Window window, Scene scene, EngineRender render) {
        scene = MainScene.get().getScene();
    }

    @Override
    public void input(Window window, Scene scene, long diffTimeMS) {

    }

    @Override
    public void update(Window window, Scene scene, long diffTimeMS) {

    }

    @Override
    public void cleanup() {

    }
}
