package org.polygon.demo;

import org.polygon.engine.core.Engine;
import org.polygon.engine.core.Window;

public class Main {
    public static void main(String[] args) {
        Window.WindowOptions options = new Window.WindowOptions();
        options.ups = 50;
        Engine engine = new Engine("Demo", options, new Script());
        engine.start();
    }
}