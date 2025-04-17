package org.polygon.engine.core.scene;

import org.joml.Matrix4f;
import org.polygon.engine.core.graph.Model;

import java.util.Arrays;

// Entity can store AnimationData to animate Model.
// AnimationData returns animation frames based on the currentTime in the animation timeline.
public class AnimationData {
    // Initialize a default bone transformation matrices array with zero matrices.
    // used for static meshes in the vertex shader.
    public static final Matrix4f[] DEFAULT_BONES_MATRICES = new Matrix4f[ModelLoader.MAX_BONES];
    static {
        Matrix4f zeroMatrix = new Matrix4f().zero();
        Arrays.fill(DEFAULT_BONES_MATRICES, zeroMatrix);
    }

    // Current animation loaded in the animationData objects.
    private Model.Animation currentAnimation;

    // Used to retrieve the animation frame data based on the animation timeline.
    private float currentTime;

    // Default 30 FPS as an animationSpeed.
    private float animationSpeed = 30.0f;

    // Used to decide interpolation between animation frames.
    private boolean interpolate = true;

    public AnimationData(Model.Animation currentAnimation) {
        this.currentAnimation = currentAnimation;
        this.currentTime = 0.0f;
    }

    public Model.Animation getCurrentAnimation() {
        return currentAnimation;
    }

    // Returns current frame or interpolated frame if interpolation is set to true.
    public Model.AnimatedFrame getCurrentFrame() {
        Model.AnimatedFrame currentFrame = currentAnimation.frames().get(getCurrentFrameIndex());
        if(interpolate) {
            // Get current and next frame matrices.
            Matrix4f[] currentFrameMatrices = currentFrame.boneMatrices();
            Matrix4f[] nextFrameMatrices = currentAnimation.frames().get(getNextFrameIndex()).boneMatrices();
            float interpolationFactor = getInterpolationFactor();

            // Create interpolated matrices.
            Matrix4f[] interpolatedMatrices = new Matrix4f[ModelLoader.MAX_BONES];
            for (int i = 0; i < ModelLoader.MAX_BONES; i++) {
                interpolatedMatrices[i] = new Matrix4f();
                // Linear interpolation between current and next frame.
                currentFrameMatrices[i].lerp(nextFrameMatrices[i], interpolationFactor, interpolatedMatrices[i]);
            }

            return new Model.AnimatedFrame(interpolatedMatrices);
        }
        return currentFrame;
    }

    public int getCurrentFrameIndex() {
        int frameIndex = (int) Math.floor(currentTime * currentAnimation.frames().size() / currentAnimation.duration());
        // Ensure the frame index never exceeds the last valid frame
        return Math.min(frameIndex, currentAnimation.frames().size() - 1);
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public int getNextFrameIndex() {
        int currentIndex = getCurrentFrameIndex();
        int nextIndex = currentIndex + 1;
        if (nextIndex >= currentAnimation.frames().size()) {
            nextIndex = 0;
        }
        return nextIndex;
    }

    public float getInterpolationFactor() {
        float frameTime = (float)currentAnimation.duration() / currentAnimation.frames().size();
        float currentFrameTime = currentTime % frameTime;
        return currentFrameTime / frameTime;
    }

    public void nextFrame(float deltaTime) {
        // Update the current time based on delta time and animation speed
        currentTime += deltaTime * animationSpeed;
        
        // Loop the animation when we reach the end
        if (currentTime >= currentAnimation.duration()) {
            currentTime = 0.0f;
        }
    }

    public void setCurrentAnimation(Model.Animation currentAnimation) {
        this.currentAnimation = currentAnimation;
        this.currentTime = 0.0f;
    }

    public void setAnimationSpeed(float speed) {
        this.animationSpeed = speed;
    }

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    public boolean isInterpolate() {
        return interpolate;
    }

    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    public void setCurrentTime(float time) {
        if (time < 0) {
            time = 0;
        } else if (time > currentAnimation.duration()) {
            time = (float)currentAnimation.duration();
        }
        this.currentTime = time;
    }
}
