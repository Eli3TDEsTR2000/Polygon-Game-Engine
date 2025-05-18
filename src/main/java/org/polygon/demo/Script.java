package org.polygon.demo;

import org.polygon.engine.core.IGameLogic;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.EngineRender;
import org.polygon.engine.core.scene.SceneSerialization;

import java.io.IOException;

public class Script implements IGameLogic {
    @Override
    public void init(Window window, EngineRender render) {
        try {
            window.setCurrentScene(SceneSerialization.loadFromFile("resources/levels/test.json", window.getWidth(), window.getHeight()));
        } catch(IOException e) {
            System.err.println("Didn't find the level");
        }
    }

    @Override
    public void input(Window window, long diffTimeMS, boolean inputConsumed) {

    }

    @Override
    public void update(Window window, long diffTimeMS) {

    }

    @Override
    public void cleanup() {

    }
}
