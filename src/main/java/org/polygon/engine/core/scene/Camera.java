package org.polygon.engine.core.scene;

import org.joml.*;

public class Camera {
    private Vector3f position;
    private Vector3f direction;
    private Vector3f right;
    private Vector3f up;
    private Vector2f rotation;
    private Matrix4f viewMatrix;
    private Matrix4f invViewMatrix;

    // Initialize the position vector. The camera's world coordinates.
    // Initialize the direction vector. the direction the camera is looking at which is a vector intersecting the
    //      line of view of the camera, will apply calculation on the direction vector
    //      to represent the rotation around the z-axis (yaw).
    // Initialize the right vector. the camera's local x-coordinate.
    // Initialize the up vector. the camera's local y-coordinate.
    // Initialize the rotation vector. which is a 2D vector with the angular value of how the camera rotates on
    //      the local x-axis (roll) and the local y-axis (pitch).
    // Initialize the viewMatrix. which is a world transformation matrix that will be applied
    //      to every object in the world.
    public Camera() {
        position = new Vector3f();
        direction = new Vector3f();
        right = new Vector3f();
        up = new Vector3f();
        rotation = new Vector2f();
        viewMatrix = new Matrix4f();
        invViewMatrix = new Matrix4f();
    }

    public Vector3f getPosition() {
        return position;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }
    public Matrix4f getInvViewMatrix() {
        return invViewMatrix;
    }

    public Vector2f getRotation() {
        return rotation;
    }

    // Recalculate the view matrix with new rotation and position values
    private void applyViewCalculation() {
        viewMatrix.identity()
                .rotateX(rotation.x)
                .rotateY(rotation.y)
                .translate(-position.x, -position.y, -position.z);
        invViewMatrix.set(viewMatrix).invert();
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        applyViewCalculation();
    }

    // Add the angular values of rotating around the local x-axis and local y-axis to the rotation vector
    //      and recalculate the viewMatrix.
    public void addRotation(float angX, float angY) {
        rotation.add(angX, angY);
        applyViewCalculation();
    }

    public void setRotation(float angX, float angY) {
        rotation.set(angX, angY);
        applyViewCalculation();
    }

    // Move the camera backward relative to the direction the camera is pointing at.
    public void moveBackward(float inc) {
        viewMatrix.positiveZ(direction).negate().mul(inc);
        position.sub(direction);
        applyViewCalculation();
    }

    // Move the camera forward relative to the direction the camera is pointing at.
    public void moveForward(float inc) {
        viewMatrix.positiveZ(direction).negate().mul(inc);
        position.add(direction);
        applyViewCalculation();
    }

    // Move the camera with increment opposite to the up vector representing the local y-axis.
    public void moveDown(float inc) {
        viewMatrix.positiveY(up).mul(inc);
        position.sub(up);
        applyViewCalculation();
    }
    // Move the camera with increment to the up vector representing the local y-axis.
    public void moveUp(float inc) {
        viewMatrix.positiveY(up).mul(inc);
        position.add(up);
        applyViewCalculation();
    }

    // Move the camera with increment opposite to the right vector representing the local x-axis.
    public void moveLeft(float inc) {
        viewMatrix.positiveX(right).mul(inc);
        position.sub(right);
        applyViewCalculation();
    }

    // Move the camera with increment to the right vector representing the local x-axis.
    public void moveRight(float inc) {
        viewMatrix.positiveX(right).mul(inc);
        position.add(right);
        applyViewCalculation();
    }
}
