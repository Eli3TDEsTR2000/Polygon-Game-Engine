package org.polygon.test.scenes.skeletalMeshScene;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import org.polygon.engine.core.IGuiInstance;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.AnimationData;

public class AnimationTestGUI implements IGuiInstance {
    private final AnimationData animationData;
    private final float[] speedBuffer = new float[1];
    private final ImBoolean interpolateBuffer = new ImBoolean();
    private final float[] timeBuffer = new float[1];
    private boolean isDraggingTimeline = false;
    private boolean isWindowHovered;

    public AnimationTestGUI(AnimationData animationData) {
        this.animationData = animationData;
        this.speedBuffer[0] = animationData.getAnimationSpeed();
        this.interpolateBuffer.set(animationData.isInterpolate());
    }

    @Override
    public void drawGui() {
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(400, 400, ImGuiCond.Once);

        ImGui.begin("AnimationTest - Animation Controls");

        isWindowHovered = ImGui.isWindowFocused();

        Model.Animation currentAnimation = animationData.getCurrentAnimation();

        // Animation speed control
        if (ImGui.sliderFloat("Animation Speed", speedBuffer, 0.0f, 60.0f)) {
            animationData.setAnimationSpeed(speedBuffer[0]);
        }

        // Interpolation toggle
        if (ImGui.checkbox("Interpolate Frames", interpolateBuffer)) {
            animationData.setInterpolate(interpolateBuffer.get());
        }

        // Animation info
        ImGui.text(String.format("Total Frames: %d", currentAnimation.frames().size()));
        ImGui.text(String.format("Current Frame: %d", animationData.getCurrentFrameIndex()));
        ImGui.text(String.format("Duration: %.2f seconds", currentAnimation.duration()));
        ImGui.text(String.format("Current Time: %.2f seconds", animationData.getCurrentTime()));

        // Timeline section
        ImGui.separator();
        ImGui.text("Timeline");

        // Calculate timeline dimensions
        float totalWidth = ImGui.getWindowContentRegionMax().x - ImGui.getWindowContentRegionMin().x;
        float timelineHeight = 40;
        float startY = ImGui.getCursorPosY();
        float startX = ImGui.getWindowPos().x + ImGui.getWindowContentRegionMin().x;

        // Draw timeline background
        ImGui.getWindowDrawList().addRectFilled(
                startX,
                startY,
                startX + totalWidth,
                startY + timelineHeight,
                ImGui.getColorU32(0.2f, 0.2f, 0.2f, 1.0f)
        );

        // Draw frame markers
        float markerSpacing = totalWidth / (currentAnimation.frames().size() - 1);
        for (int i = 0; i < currentAnimation.frames().size(); i++) {
            float x = startX + (i * markerSpacing);
            boolean isCurrentFrame = i == animationData.getCurrentFrameIndex();
            float markerHeight = isCurrentFrame ? timelineHeight * 0.75f : timelineHeight * 0.5f;
            float markerWidth = isCurrentFrame ? 2.0f : 1.0f;
            int markerColor = isCurrentFrame ?
                    ImGui.getColorU32(1, 0, 0, 1) :
                    ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1);

            ImGui.getWindowDrawList().addLine(
                    x,
                    startY + timelineHeight - markerHeight,
                    x,
                    startY + timelineHeight,
                    markerColor,
                    markerWidth
            );
        }

        // Draw playhead
        float playheadX = startX + (totalWidth * (animationData.getCurrentTime() / (float)currentAnimation.duration()));
        ImGui.getWindowDrawList().addTriangleFilled(
                playheadX, startY,
                playheadX - 5, startY - 8,
                playheadX + 5, startY - 8,
                ImGui.getColorU32(1, 1, 1, 1)
        );

        // Handle timeline interaction
        ImGui.invisibleButton("timeline", totalWidth, timelineHeight);
        if (ImGui.isItemHovered() || isDraggingTimeline) {
            if (ImGui.isMouseDown(0)) {
                isDraggingTimeline = true;
                float mouseX = ImGui.getMousePos().x;
                float relativeX = mouseX - startX;
                float newTime = (relativeX / totalWidth) * (float)currentAnimation.duration();
                animationData.setCurrentTime(newTime);
            } else {
                isDraggingTimeline = false;
            }
        }

        ImGui.dummy(0, timelineHeight + 10); // Add some spacing after the timeline

        ImGui.end();
    }

    @Override
    public boolean handleGuiInput(Window window) {
        ImGuiIO imGuiIO = ImGui.getIO();

        return (imGuiIO.getWantCaptureKeyboard() || imGuiIO.getWantCaptureMouse()) && isWindowHovered;
    }
}
