package org.polygon.test;

import org.polygon.engine.core.Engine;
import org.polygon.engine.core.Window;

public class Main {
    public static void main(String[] args) {
        GameScript script = new GameScript();
        Window.WindowOptions options = new Window.WindowOptions();
        options.width = 1920;
        options.height = 1080;
        Engine engine = new Engine("Polygon Game Engine", options, script);
        engine.start();
    }
}