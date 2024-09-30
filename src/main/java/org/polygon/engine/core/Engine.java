package org.polygon.engine.core;

import org.polygon.engine.core.graph.EngineRender;
import org.polygon.engine.core.scene.Scene;

public class Engine {
    public static final int TARGET_UPS = 30;
    private final IGameLogic gameLogic;
    private final Window window;
    private EngineRender render;
    private Scene scene;
    private int targetFps;
    private int targetUps;
    private boolean running;

    public Engine(String windowTitle, Window.WindowOptions opts, IGameLogic gameLogic) {
        // Assign game engine's target fps and ups from options
        targetFps = opts.fps;
        targetUps = opts.ups;

        // Creating engine's window and passing the resize function as reference
        window = new Window(windowTitle, opts, () -> {
            resize();
            return null;
        });

        // Passing game logic to engine
        // creating the renderer scene entities
        // initializing game;
        this.gameLogic = gameLogic;
        render = new EngineRender();
        scene = new Scene();
        gameLogic.init(window, scene, render);
        running = true;
    }

    private void cleanup() {
        // Destroying game and freeing memory
        gameLogic.cleanup();
        // Destroying Renderer entity
        render.cleanup();
        // Destroying Scene entity
        scene.cleanup();
        // Finally, destroying
        window.cleanup();
    }

    private void resize() {
        // Implemented later
    }

    // Main game loop
    private void run() {
        // This will hold before loop time in MS to calculate lag
        long rbeforeMS = System.currentTimeMillis();
        long ubeforeMS = rbeforeMS;

        // Finds the target update time in MS to manage fixed game updates
        float targetUpdateMS = 1000.0f / targetUps;

        // Finds the target render time in MS to manage capped frame rate
        // If there is no specified frame rate cap, we will depend on
        // GLFW v-sync to control render calls rate instead
        float targetRenderMS = targetFps > 0 ? 1000.0f / targetFps : 0;

        // Will determine if we should update or render based on targetFps and targetUps
        float deltaUpdate = 0;
        float deltaRender = 0;

        while(running && !window.windowShouldClose()) {
            // Poll window events and key callbacks will only invoke during this call
            window.pollEvents();

            // Get current iteration time in MS and
            // increment both deltaUpdate and deltaRender based on lag
            // if lag was equal to or bigger than the target values for updating and rendering
            // then the division result will be equal to 1 or more and that is a flag to
            // render/update. If otherwise we need to sleep for more iterations until it's time to render/update
            long nowMS = System.currentTimeMillis();
            deltaUpdate += (nowMS - rbeforeMS) / targetUpdateMS;
            deltaRender += (nowMS - rbeforeMS) / targetRenderMS;

            // Process game inputs and passing delta time taken between frames
            gameLogic.input(window, scene, nowMS - rbeforeMS);

            // Caps updating according to targetUps
            if(deltaUpdate >= 1) {
                // Updating game according to physics or game inputs
                // and passing delta time taken between updates
                gameLogic.update(window, scene, nowMS - ubeforeMS);
                ubeforeMS = nowMS;
                // Reset deltaUpdate to redetermine if a time matching targetUps in MS
                // had passed or not for future update calls
                deltaUpdate--;
            }

            if(targetFps <= 0 || deltaRender >= 1) {
                // Clears the screen and initiate draw calls then redraw frame buffer
                render.render(window, scene);
                // Reset deltaRender to redetermine if a time matching targetFps in MS
                // had passed or not for future draw calls
                deltaRender--;
                // Swap frame buffer
                window.update();
            }

            rbeforeMS = nowMS;
        }
        // Cleans up engine components when window closes (game loop shutdown)
        cleanup();
    }

    public void start() {
        running = true;
        run();
    }

    public void stop() {
        running = false;
    }
}
