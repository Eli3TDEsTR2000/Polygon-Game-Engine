package org.polygon.engine.core;

import org.polygon.engine.core.graph.EngineRender;
import org.polygon.engine.core.scene.Scene;

public interface IGameLogic {
    void init(Window window, Scene scene, EngineRender render);
    void input(Window window, Scene scene, long diffTimeMS);
    void update(Window window, Scene scene, long diffTimeMS);
    void cleanup();
}
