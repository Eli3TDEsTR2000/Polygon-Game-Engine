package org.polygon.test.physicsWIP;
/*
 Copyright (c) 2020-2025 Stephen Gold and Yanis Boudiaf

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. Neither the name of the copyright holder nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Plane;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import electrostatic4j.snaploader.LibraryInfo;
import electrostatic4j.snaploader.LoadingCriterion;
import electrostatic4j.snaploader.NativeBinaryLoader;
import electrostatic4j.snaploader.filesystem.DirectoryPath;
import electrostatic4j.snaploader.platform.NativeDynamicLibrary;
import electrostatic4j.snaploader.platform.util.PlatformPredicate;
import org.joml.Vector4f;
import org.polygon.engine.core.Engine;
import org.polygon.engine.core.IGameLogic;
import org.polygon.engine.core.Window;
import org.polygon.engine.core.graph.EngineRender;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.scene.Camera;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.ModelLoader;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.lights.SceneLights;
import org.polygon.test.PerformanceGUI;
import org.polygon.test.scenes.normalScene.LightTestGUI;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Drop a dynamic sphere onto a horizontal surface (non-graphical illustrative
 * example).
 *
 * @author Stephen Gold sgold@sonic.net
 */
final class HelloLibbulletjme implements IGameLogic {
    private final float MOUSE_SENSITIVITY = 0.1f;
    private final float MOVEMENT_SPEED = 0.005f;
    private final float timeStep = 0.02f;
    private Entity sphereEntity;
    // *************************************************************************
    // fields

    private static PhysicsRigidBody ball;
    private static PhysicsSpace physicsSpace;
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private HelloLibbulletjme() {
        // do nothing
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloLibbulletjme application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        Window.WindowOptions options = new Window.WindowOptions();
        options.width = 1280;
        options.height = 720;
        options.ups = 60;
        Engine engine = new Engine("HelloLibbulletjme", options, new HelloLibbulletjme());
        engine.start();
    }
    // *************************************************************************
    // private methods

    /**
     * Create the PhysicsSpace. Invoked once during initialization.
     *
     * @return a new object
     */
    private static PhysicsSpace createSpace() {
        PhysicsSpace result
                = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);
        return result;
    }

    /**
     * Populate the PhysicsSpace. Invoked once during initialization.
     */
    private static void populateSpace(Scene scene) {
        // Add a static horizontal plane at y=-1:
        float groundY = -10f;
        Plane plane = new Plane(Vector3f.UNIT_Y, groundY);
        CollisionShape planeShape = new PlaneCollisionShape(plane);
        float mass = PhysicsBody.massForStatic;
        PhysicsRigidBody floor = new PhysicsRigidBody(planeShape, mass);
        physicsSpace.addCollisionObject(floor);

        Model floorModel = ModelLoader.loadModel("floor-model", "resources/models/terrain/terrain.obj"
                , scene.getTextureCache(), false);
        floorModel.getMaterialList().get(1).setTexturePath(null);
        floorModel.getMaterialList().get(1).setRoughness(10f);
        scene.addModel(floorModel);

        Entity floorEntity = new Entity("floor-entity", floorModel.getModelId());
        floorEntity.setPosition(0, -10.7f, 0);
        floorEntity.setScale(1000.0f);
        scene.addEntity(floorEntity);

        // Add a sphere-shaped, dynamic, rigid body at the origin:
        float radius = 0.3f;
        CollisionShape ballShape = new SphereCollisionShape(radius);
        mass = 1f;
        ball = new PhysicsRigidBody(ballShape, mass);
        physicsSpace.addCollisionObject(ball);
    }

    /**
     * Advance the physics simulation by the specified amount.
     *
     * @param intervalSeconds the amount of time to simulate (in seconds, &ge;0)
     */
    private static void updatePhysics(float intervalSeconds) {
        int maxSteps = 0; // for a single step of the specified duration
        physicsSpace.update(intervalSeconds, maxSteps);
    }

    @Override
    public void init(Window window, EngineRender render) {
        LibraryInfo info
                = new LibraryInfo(null, "bulletjme", DirectoryPath.USER_DIR);
        NativeBinaryLoader loader = new NativeBinaryLoader(info);

        NativeDynamicLibrary[] libraries = {
                new NativeDynamicLibrary(
                        "native/linux/arm64", PlatformPredicate.LINUX_ARM_64),
                new NativeDynamicLibrary(
                        "native/linux/arm32", PlatformPredicate.LINUX_ARM_32),
                new NativeDynamicLibrary(
                        "native/linux/x86_64", PlatformPredicate.LINUX_X86_64),
                new NativeDynamicLibrary(
                        "native/osx/arm64", PlatformPredicate.MACOS_ARM_64),
                new NativeDynamicLibrary(
                        "native/osx/x86_64", PlatformPredicate.MACOS_X86_64),
                new NativeDynamicLibrary(
                        "native/windows/x86_64", PlatformPredicate.WIN_X86_64)
        };
        loader.registerNativeLibraries(libraries)
                .initPlatformLibrary()
                .setLoggingEnabled(true);
        loader.setRetryWithCleanExtraction(true);

        // Load the Libbulletjme native library for this platform.
        try {
            loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load the Libbulletjme library!");
        }

        physicsSpace = createSpace();
        populateSpace(window.getCurrentScene());

        Model sphere = ModelLoader.loadModel("sphere-model"
                , "resources/models/sphere/sphere.fbx"
                , window.getCurrentScene().getTextureCache(), false);
        window.getCurrentScene().addModel(sphere);

        sphere.getMaterialList().get(0).setDiffuseColor(new Vector4f(0, 1, 1, 1));
        sphere.getMaterialList().get(0).setMetallic(1.0f);
        sphere.getMaterialList().get(0).setRoughness(0.547f);

        sphereEntity = new Entity("sphere-entity", sphere.getModelId());
        Vector3f vector3f = new Vector3f();
        ball.getPhysicsLocation(vector3f);
        sphereEntity.setPosition(vector3f.x, vector3f.y, vector3f.z);
        sphereEntity.setScale(1f);
        window.getCurrentScene().addEntity(sphereEntity);
        window.getCurrentScene().getCamera().moveBackward(20);

        SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(0.0f);
        sceneLights.getDirectionalLight().setIntensity(1.0f);
        sceneLights.getDirectionalLight().setDirection(0.54f, 1.0f, 0.22f);
        window.getCurrentScene().setSceneLights(sceneLights);

        window.addGuiInstance(new PerformanceGUI(true));
    }

    @Override
    public void input(Window window, long diffTimeMS, boolean inputConsumed) {
        int factor = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT) ? 3 : 1;
        float incrementMovement = diffTimeMS * MOVEMENT_SPEED * factor;
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
        if(window.isKeyPressed(GLFW_KEY_R)) {
            ball.applyForce(new Vector3f(1.0f, 0.0f, 1.0f), new Vector3f(5.0f, 0.0f, 5.0f));
        }

        if(window.getMouseInputHandler().isRightButtonPressed()) {
            camera.addRotation(
                    (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(window.getMouseInputHandler().getDisplacement().y * MOUSE_SENSITIVITY));
        }
    }

    @Override
    public void update(Window window, long diffTimeMS) {
        Vector3f location = new Vector3f();
        Quaternion rotation = new Quaternion();

        updatePhysics(timeStep);

        ball.getPhysicsLocation(location);
        ball.getPhysicsRotation(rotation);
        sphereEntity.setPosition(location.x, location.y, location.z);
        System.out.println(location);
    }

    @Override
    public void cleanup() {

    }
}