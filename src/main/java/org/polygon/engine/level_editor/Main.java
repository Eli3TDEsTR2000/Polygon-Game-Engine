package org.polygon.engine.level_editor;

import org.polygon.engine.core.Engine;
import org.polygon.engine.core.Window;

public class Main {
    public static void main(String[] args) {
        Window.WindowOptions options = new Window.WindowOptions();
        options.width = 1270;
        options.height = 720;
        options.ups = 50;
        Engine engine = new Engine("Polygon Engine - Level editor", options, new Editor());
        engine.start();
    }
}
