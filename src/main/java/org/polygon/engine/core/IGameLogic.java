package org.polygon.engine.core;

import org.polygon.engine.core.graph.EngineRender;
import org.polygon.engine.core.scene.Scene;

public interface IGameLogic {
    void init(Window window, EngineRender render);
    void input(Window window, long diffTimeMS);
    void update(Window window, long diffTimeMS);
    void cleanup();
}
