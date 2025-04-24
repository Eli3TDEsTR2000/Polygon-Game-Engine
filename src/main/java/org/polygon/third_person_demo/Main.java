package org.polygon.third_person_demo;

import org.polygon.engine.core.Engine;
import org.polygon.engine.core.Window;

public class Main {
    public static void main(String[] args) {
        Window.WindowOptions options = new Window.WindowOptions();
        options.ups = 50;
        Engine engine = new Engine("Third person game", options, new Script());
        engine.start();
    }
}
