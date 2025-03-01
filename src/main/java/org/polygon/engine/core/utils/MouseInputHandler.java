package org.polygon.engine.core.utils;

import org.joml.Vector2f;
import static org.lwjgl.glfw.GLFW.*;

public class MouseInputHandler {
    private Vector2f currentPosition;
    private Vector2f displacement;
    private Vector2f previousPosition;
    private boolean inWindow;
    private boolean leftButtonPressed;
    private boolean rightButtonPressed;

    // Initialize the cursor previous position and current position to later calculate the cursor displacement.
    // Initialize mouse state values TODO - add more boolean states to represent all the mouse buttons.
    // Setup callback functions to update the current cursor positions and check if the cursor is inside the window.
    // Setup a callback function to update if the left or right button is pressed when a mouse button event happens.
    public MouseInputHandler(long windowHandle) {
        previousPosition = new Vector2f(-1, -1);
        currentPosition = new Vector2f();
        displacement = new Vector2f();
        leftButtonPressed = false;
        rightButtonPressed = false;
        inWindow = false;

        glfwSetCursorPosCallback(windowHandle, (handle, xPos, yPos) -> {
            currentPosition.x = (float) xPos;
            currentPosition.y = (float) yPos;
        });
        glfwSetCursorEnterCallback(windowHandle, (handle, entered) -> inWindow = entered);
        glfwSetMouseButtonCallback(windowHandle, (handle, button, action, mode) -> {
            leftButtonPressed = button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS;
            rightButtonPressed = button == GLFW_MOUSE_BUTTON_2 && action == GLFW_PRESS;
        });
    }

    public Vector2f getCurrentPosition() {
        return currentPosition;
    }

    public Vector2f getDisplacement() {
        return displacement;
    }

    public boolean isLeftButtonPressed() {
        return leftButtonPressed;
    }

    public boolean isRightButtonPressed() {
        return rightButtonPressed;
    }

    // Updates the displacement vector when the cursor is inside the window.
    public void input() {
        displacement.x = 0;
        displacement.y = 0;

        if(previousPosition.x > 0 && previousPosition.y > 0 && inWindow) {
            double dx = currentPosition.x - previousPosition.x;
            double dy = currentPosition.y - previousPosition.y;
            boolean displacedX = dx != 0;
            boolean displacedY = dy != 0;
            if(displacedX) {
                displacement.y = (float) dx;
            }
            if(displacedY) {
                displacement.x = (float) dy;
            }
        }
        previousPosition.x = currentPosition.x;
        previousPosition.y = currentPosition.y;
    }
}
