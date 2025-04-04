package org.polygon.engine.core;

import org.polygon.engine.core.graph.EngineRender;

public class Engine {
    public static final int TARGET_UPS = 30;
    private final IGameLogic gameLogic;
    private final Window window;
    private EngineRender render;
    private boolean running;

    public Engine(String windowTitle, Window.WindowOptions opts, IGameLogic gameLogic) {
        // Creating engine's window and passing the resize function as reference
        window = new Window(windowTitle, opts);

        // Passing game logic to engine
        // creating the renderer scene entities
        // initializing game;
        this.gameLogic = gameLogic;
        render = new EngineRender(window);
        // Fail-safe to ensure the engine won't crash if the current scene is empty
        window.setCurrentScene(window.createScene());
        gameLogic.init(window, render);

        // Initialize key call back functions if set
        if(window.isKeyCallBacksSet()) {
            window.initKeyCallbacks();
        }

        // Initialize the frame buffer size callback functions
        window.initFrameBufferSizeCallbacks();

        running = true;
    }

    private void cleanup() {
        // Destroying game and freeing memory
        gameLogic.cleanup();
        // Destroying Renderer entity
        render.cleanup();
        // Destroying Scene entity
        window.getCurrentScene().cleanup();
        // Finally, destroying window entity
        window.cleanup();
    }

    // Main game loop
    private void run() {
        // Track user options
        Window.WindowOptions opts = window.getWindowOptions();
        // This will hold before loop time in MS to calculate lag
        long rbeforeMS = System.currentTimeMillis();
        long ubeforeMS = rbeforeMS;

        float targetUpdateMS;
        float targetRenderMS;
        float beforeTargetUpdateMs = 1000.0f / opts.ups;
        float beforeTargetRenderMS = opts.fps > 0 ? 1000.0f / opts.fps : 0;

        // Will determine if we should update or render based on targetFps and targetUps
        float deltaUpdate = 0;
        float deltaRender = 0;

        while(running && !window.windowShouldClose()) {
            // Finds the target update time in MS to manage fixed game updates
            targetUpdateMS = 1000.0f / opts.ups;
            // Finds the target render time in MS to manage capped frame rate
            // If there is no specified frame rate cap, we will depend on
            // GLFW v-sync to control render calls rate instead
            targetRenderMS = opts.fps > 0 ? 1000.0f / opts.fps : 0;

            // Check if the user changed target FPS
            // This reset is needed for instant target FPS change.
            if(beforeTargetRenderMS != targetRenderMS) {
                deltaRender = 0;
            }
            // Check if the user changed target UPS
            // This reset is needed for instant target UPS change.
            if(beforeTargetUpdateMs != targetUpdateMS) {
                deltaUpdate = 0;
            }

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

            // Process the game window's mouse inputs
            window.getMouseInputHandler().input();

            // Handle GUI inputs and decide if the GUI is in the focus of the mouse or keyboard
            boolean inputConsumed = window.getCurrentScene().getGuiInstance() != null
                    && window.getCurrentScene().getGuiInstance().handleGuiInput(window);
            // Process game inputs and passing delta time taken between frames
            gameLogic.input(window, nowMS - rbeforeMS, inputConsumed);

            // Caps updating according to targetUps
            if(deltaUpdate >= 1) {
                // Updating game according to physics or game inputs
                // and passing delta time taken between updates
                gameLogic.update(window, nowMS - ubeforeMS);
                ubeforeMS = nowMS;
                // Reset deltaUpdate to re-determine if a time matching targetUps in MS
                // had passed or not for future update calls
                deltaUpdate--;
            }

            if(opts.fps <= 0 || deltaRender >= 1) {
                // Clears the screen and initiate draw calls then redraw frame buffer
                render.render(window);
                // Reset deltaRender to redetermine if a time matching targetFps in MS
                // had passed or not for future draw calls
                deltaRender--;
                // Swap frame buffer
                window.update();
            }

            rbeforeMS = nowMS;

            // Used to track if user changed target FPS
            beforeTargetRenderMS = targetRenderMS;
            // Used to track if user changed target UPS
            beforeTargetUpdateMs = targetUpdateMS;
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
