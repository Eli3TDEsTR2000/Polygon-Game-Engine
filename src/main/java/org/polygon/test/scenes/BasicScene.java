package org.polygon.test.scenes;

import org.polygon.engine.core.Window;
import org.polygon.engine.core.scene.Scene;

public abstract class BasicScene {
    protected Scene scene;
    protected Window window;
    public BasicScene(Window window) {
        scene = window.createScene();
        this.window = window;
    }

    public Scene getScene() {
        return scene;
    }

    public abstract void init();

    public void reset() {
        scene.reset();
        init();
    }

    public abstract void input(Window window, long diffTimeMS);

    public abstract void update(Window window, long diffTimeMS);

    public abstract void cleanup();
}
