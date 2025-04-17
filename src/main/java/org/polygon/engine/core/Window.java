package org.polygon.engine.core;

import org.lwjgl.glfw.*;
import org.lwjgl.system.*;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.utils.MouseInputHandler;

import java.nio.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Window {

    // The window handle
    private long windowHandle;
    private int width;
    private int height;
    private WindowOptions opts;
    private Scene currentScene;
    private IGuiInstance guiInstance;
    private MouseInputHandler mouseInputHandler;
    private List<KeyCallback> keyCallbacks;
    private GLFWKeyCallback prevKeyCallback;
    private List<FrameBufferSizeCallback> frameBufferSizeCallbacks;
    private GLFWFramebufferSizeCallback prevFramebufferSizeCallback;

    public Window(String title, WindowOptions opts) {
        this.opts = opts;
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() ) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // TODO - This antiAliasing implementation is temporary until FBO is introduced to the engine
        // Using GLFW MSAA
        checkAntiAliasing();

        // Sets glfw OpenGL context version to 4.x
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);

        // Checks if compatible profile is enabled, (default: false)
        if(this.opts.compatibleProfile) {
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        } else {
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        }

        // Get the resolution of the primary monitor
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        if(this.opts.width > 0 && this.opts.height > 0) {
            this.width = this.opts.width;
            this.height = this.opts.height;
        } else {
            glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
            this.width = vidmode.width();
            this.height = vidmode.height();
        }

        // Create the window
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if ( windowHandle == NULL ) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a window resize callback. It will be called every time the window is resized
        frameBufferSizeCallbacks = new ArrayList<>();
        addFrameBufferSizeCallback((handle, w, h) -> resized(w, h));

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        // Default engine key callback function setup.
        // the Window.addKeyCallback will override this default callback function if set.
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            keyCallBack(key, action);
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(windowHandle, pWidth, pHeight);

            this.width = pWidth.get(0);
            this.height =pHeight.get(0);

            if(!(glfwGetWindowAttrib(windowHandle, GLFW_MAXIMIZED) == GLFW_TRUE)) {
                // Center the window
                glfwSetWindowPos(
                        windowHandle,
                        (vidmode.width() - pWidth.get(0)) / 2,
                        (vidmode.height() - pHeight.get(0)) / 2
                );
            }
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle);

        // If a custom fps is set, it will disable v-sync
        if(this.opts.fps > 0) {
            // disable v-sync
            glfwSwapInterval(0);
        } else {
            // enable v-sync
            glfwSwapInterval(1);
        }

        // Make the window visible
        glfwShowWindow(windowHandle);

        mouseInputHandler = new MouseInputHandler(windowHandle);
        keyCallbacks = new ArrayList<>();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getWindowHandle() {
        return windowHandle;
    }
    public WindowOptions getWindowOptions() {
        return opts;
    }
    public IGuiInstance getGuiInstance() {
        return guiInstance;
    }

    public MouseInputHandler getMouseInputHandler() {
        return mouseInputHandler;
    }

    public Scene getCurrentScene() {
        if(currentScene == null) {
            throw new RuntimeException("currentScene doesn't hold a scene yet!");
        }
        return currentScene;
    }

    public boolean isKeyCallBacksSet() {
        return keyCallbacks.size() > 0;
    }
    public void setCurrentScene(Scene scene) {
        currentScene = scene;
    }
    public void setGuiInstance(IGuiInstance guiInstance) {
        this.guiInstance = guiInstance;
    }

    public Scene createScene() {
        return new Scene(getWidth(), getHeight());
    }

    public boolean isKeyPressed(int keyCode) {
        return glfwGetKey(windowHandle, keyCode) == GLFW_PRESS;
    }

    public boolean isKeyReleased(int keyCode) {
        return glfwGetKey(windowHandle, keyCode) == GLFW_RELEASE;
    }

    public void pollEvents() {
        // Poll for window events. The key callback above will only be
        // invoked during this call.
        glfwPollEvents();
    }

    public void update() {
        glfwSwapBuffers(windowHandle); // swap the color buffers
    }

    public boolean windowShouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public void cleanup() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    // The default key callback function that will be called if there are no callback functions set
    public void keyCallBack(int key, int action) {
        // If Escape key is pressed, the window will close
        if(key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            glfwSetWindowShouldClose(windowHandle, true);
        }
    }

    // This method is used to chain key callbacks.
    public KeyCallback addKeyCallback(KeyCallback callBack) {
        keyCallbacks.add(callBack);
        return callBack;
    }

    // Adds the support for multiple key callback functions.
    public void initKeyCallbacks() {
        prevKeyCallback = glfwSetKeyCallback(windowHandle, (handle, key, scancode, action, mods) -> {
            if(prevKeyCallback != null) {
                prevKeyCallback.invoke(handle, key, scancode, action, mods);
            }
            for(KeyCallback keyCallback : keyCallbacks) {
                keyCallback.invoke(handle, key, scancode, action, mods);
            }
        });
    }

    // This method is used to chain frame buffer size callbacks.
    public FrameBufferSizeCallback addFrameBufferSizeCallback(FrameBufferSizeCallback callback) {
        frameBufferSizeCallbacks.add(callback);
        return callback;
    }

    // Adds the support for multiple frame buffer size callbacks.
    public void initFrameBufferSizeCallbacks() {
        prevFramebufferSizeCallback = glfwSetFramebufferSizeCallback(windowHandle, (handle, w, h) -> {
            if(prevFramebufferSizeCallback != null) {
                prevFramebufferSizeCallback.invoke(handle, w, h);
            }
            for(FrameBufferSizeCallback frameBufferSizeCallback : frameBufferSizeCallbacks) {
                frameBufferSizeCallback.invoke(handle, w, h);
            }
        });
    }

    private void resized(int width, int height) {
        this.width = width;
        this.height = height;

        currentScene.resize(this.width, this.height);
    }

    public void checkAntiAliasing() {
        int sampleSize;
        switch(this.opts.antiAliasing) {
            case 1:
                sampleSize = 2;
                break;
            case 2:
                sampleSize = 4;
                break;
            case 3:
                sampleSize = 8;
                break;

            default :
                sampleSize = 0;
        }

        // TODO - This antiAliasing implementation is temporary until FBO is introduced to the engine
        if(windowHandle == NULL) {
            glfwWindowHint(GLFW_SAMPLES, sampleSize);
        }
    }

    public static class WindowOptions {
        public static final int MSAA_2X = 1;
        public static final int MSAA_4X = 2;
        public static final int MSAA_16X = 3;
        public boolean compatibleProfile;
        public int fps;
        public int ups = Engine.TARGET_UPS;
        public int width;
        public int height;
        public int antiAliasing;

    }

    public interface KeyCallback {
        void invoke(long windowHandle, int key, int scancode, int action, int mods);
    }

    public interface FrameBufferSizeCallback {
        void invoke(long windowHandle, int width, int height);
    }
}