package org.polygon.test.scenes.selectionScene;

import org.joml.*;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.Material;
import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.Camera;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.ModelLoader;
import org.polygon.test.scenes.BasicScene;
import org.polygon.test.scenes.cubeScene.UpdateFpsTestGUI;

import java.lang.Math;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_V;

public class EntitySelectionTestScene extends BasicScene {
    private final float MOUSE_SENSITIVITY = 0.1f;
    private final float MOVEMENT_SPEED = 0.005f;
    private Entity cubeEntity;
    private Entity cubeEntity2;
    private float rotation;

    public EntitySelectionTestScene(Window window) {
        super(window);
    }

    @Override
    public void init() {
        Model cube = ModelLoader.loadModel("Cube", "resources/models/cube/cube.obj"
                , scene.getTextureCache(), false);
        scene.addModel(cube);

        cubeEntity = new Entity("Cube-01", cube.getModelId());
        cubeEntity.setPosition(-1, 0, -2);
        scene.addEntity(cubeEntity);

        cubeEntity2 = new Entity("Cube-02", cube.getModelId());
        cubeEntity2.setPosition(1, 0, -2);
        scene.addEntity(cubeEntity2);

        rotation = 0;

        scene.setGuiInstance(new EntitySelectionGUI(scene));
    }

    @Override
    public void input(Window window, long diffTimeMS) {
        float incrementMovement = diffTimeMS * MOVEMENT_SPEED;
        Camera camera = window.getCurrentScene().getCamera();
        if(window.isKeyPressed(GLFW_KEY_W)) {
            camera.moveForward(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_S)) {
            camera.moveBackward(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_A)) {
            camera.moveLeft(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_D)) {
            camera.moveRight(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_SPACE)) {
            camera.moveUp(incrementMovement);
        }
        if(window.isKeyPressed(GLFW_KEY_V)) {
            camera.moveDown(incrementMovement);
        }

        if(window.getMouseInputHandler().isRightButtonPressed()) {
            camera.addRotation(
                    (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().y * MOUSE_SENSITIVITY));
        }

        if(window.getMouseInputHandler().isLeftButtonPressed()) {
            selectEntity(window);
        }
    }

    public void selectEntity(Window window) {
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();

        Vector2f mousePos = window.getMouseInputHandler().getCurrentPosition();

        float x = (2 * mousePos.x) / windowWidth - 1.0f;
        float y = 1.0f - (2 * mousePos.y) / windowHeight;
        float z = -1.0f;

        Matrix4f invProjMatrix = window.getCurrentScene().getProjection().getInvProjMatrix();
        Vector4f mouseDir = new Vector4f(x, y, z, 1.0f);
        mouseDir.mul(invProjMatrix);
        mouseDir.z = -1.0f;
        mouseDir.w = 0.0f;

        Matrix4f invViewMatrix = window.getCurrentScene().getCamera().getInvViewMatrix();;
        mouseDir.mul(invViewMatrix);

        Vector4f min = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        Vector4f max = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        Vector2f nearFar = new Vector2f();

        Entity selectedEntity = null;
        float closestDistance = Float.POSITIVE_INFINITY;
        Vector3f center = scene.getCamera().getPosition();

        Collection<Model> models = scene.getModelMap().values();
        Matrix4f modelMatrix = new Matrix4f();
        for (Model model : models) {
            List<Entity> entities = model.getEntityList();
            for (Entity entity : entities) {
                modelMatrix.translate(entity.getPosition()).scale(entity.getScale());
                for (Material material : model.getMaterialList()) {
                    for (Mesh mesh : material.getMeshList()) {
                        Vector3f aabbMin = mesh.getAabbMinCorner();
                        min.set(aabbMin.x, aabbMin.y, aabbMin.z, 1.0f);
                        min.mul(modelMatrix);
                        Vector3f aabMax = mesh.getAabbMaxCorner();
                        max.set(aabMax.x, aabMax.y, aabMax.z, 1.0f);
                        max.mul(modelMatrix);
                        if (Intersectionf.intersectRayAab(center.x, center.y, center.z, mouseDir.x, mouseDir.y, mouseDir.z,
                                min.x, min.y, min.z, max.x, max.y, max.z, nearFar) && nearFar.x < closestDistance) {
                            closestDistance = nearFar.x;
                            selectedEntity = entity;
                        }
                    }
                }
                modelMatrix.identity();
            }
        }

        window.getCurrentScene().setSelectedEntity(selectedEntity);
    }

    @Override
    public void update(Window window, long diffTimeMS) {
        rotation += 15 * diffTimeMS / 1000.0f;
        if (rotation > 360) {
            rotation = 0;
        }
        cubeEntity.setRotation(1, 1, 1, (float) Math.toRadians(rotation));
        cubeEntity2.setRotation(1, 1, 1, (float) Math.toRadians(rotation));
    }

    @Override
    public void cleanup() {
        scene.cleanup();
    }
}
