package org.polygon.test.scenes;

import org.polygon.engine.core.Window;
import org.polygon.engine.core.scene.Scene;

public abstract class BasicScene {
    public BasicScene() {
    }

    public abstract void initScene(Scene scene);

    public void input(Window window, Scene scene, long diffTimeMS) {

    }

    public void update(Window window, Scene scene, long diffTimeMS) {

    }
}
