package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.polygon.engine.core.scene.AnimationData;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.lights.PointLight;
import org.polygon.engine.core.scene.lights.SceneLights;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class PointShadowRender {
    private static final float NEAR_PLANE = 0.1f;

    private static final Vector3f[] FACE_DIRECTIONS = {
            new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0),
            new Vector3f(0, 1, 0), new Vector3f(0, -1, 0),
            new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)
    };
    private static final Vector3f[] FACE_UPS = {
            new Vector3f(0, -1, 0), new Vector3f(0, -1, 0),
            new Vector3f(0, 0, 1), new Vector3f(0, 0, -1),
            new Vector3f(0, -1, 0), new Vector3f(0, -1, 0)
    };

    private ShaderProgram shaderProgram;
    private UniformMap uniformMap;
    private PointShadowBuffer pointShadowBuffer;
    private final List<PointLight> activeShadowLights;

    public PointShadowRender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(
                        "resources/shaders/point_shadow.vert", GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(
                        "resources/shaders/point_shadows.frag", GL_FRAGMENT_SHADER));
        shaderProgram = new ShaderProgram(shaderModuleDataList);

        pointShadowBuffer = new PointShadowBuffer();
        activeShadowLights = new ArrayList<>();

        createUniforms();
    }

    private void createUniforms() {
        uniformMap = new UniformMap(shaderProgram.getProgramId());
        uniformMap.createUniform("modelMatrix");
        uniformMap.createUniform("projViewMatrix");
        uniformMap.createUniform("bonesMatrices");
        uniformMap.createUniform("lightPos");
        uniformMap.createUniform("farPlane");
    }

    public void cleanup() {
        shaderProgram.cleanup();
        pointShadowBuffer.cleanup();
    }

    public PointShadowBuffer getPointShadowBuffer() {
        return pointShadowBuffer;
    }

    public List<PointLight> getActiveShadowLights() {
        return activeShadowLights;
    }

    private void selectShadowCastingLights(Scene scene) {
        activeShadowLights.clear();

        SceneLights sceneLights = scene.getSceneLights();
        if(sceneLights == null) {
            return;
        }

        Vector3f cameraPos = scene.getCamera().getPosition();

        List<PointLight> castingShadowPointLights = new ArrayList<>();
        for(PointLight pointLight : sceneLights.getPointLightList()) {
            if(pointLight.isShadowCasting()) {
                castingShadowPointLights.add(pointLight);
            }
        }

        castingShadowPointLights.sort(
                Comparator.comparingDouble(light -> light.getPosition().distanceSquared(cameraPos)));

        int count = Math.min(castingShadowPointLights.size(), PointShadowBuffer.MAX_POINT_LIGHT_SHADOWS);
        for(int i = 0; i < count; i++) {
            activeShadowLights.add(castingShadowPointLights.get(i));
        }
    }

    public void render(Scene scene) {
        selectShadowCastingLights(scene);
        if(activeShadowLights.isEmpty()) {
            return;
        }

        glBindFramebuffer(GL_FRAMEBUFFER, pointShadowBuffer.getDepthMapFBO());
        glViewport(0, 0
                , PointShadowBuffer.SHADOW_CUBEMAP_RESOLUTION, PointShadowBuffer.SHADOW_CUBEMAP_RESOLUTION);
        shaderProgram.bind();

        Collection<Model> models = scene.getModelMap().values();
        int[] cubemapIds = pointShadowBuffer.getDepthCubemaps().getIds();

        for(int lightSlot = 0; lightSlot < activeShadowLights.size(); lightSlot++) {
            PointLight pointLight = activeShadowLights.get(lightSlot);
            Vector3f lightPos = pointLight.getPosition();
            float farPlane = pointLight.getRadius();

            Matrix4f projection = new Matrix4f().perspective(
                    (float) Math.toRadians(90.0f), 1.0f, NEAR_PLANE, farPlane);

            uniformMap.setUniform("lightPos", lightPos);
            uniformMap.setUniform("farPlane", farPlane);

            for(int face = 0; face < 6; face++) {
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT
                        , GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, cubemapIds[lightSlot], 0);
                glClear(GL_DEPTH_BUFFER_BIT);

                Vector3f center = new Vector3f(lightPos).add(FACE_DIRECTIONS[face]);
                Matrix4f view = new Matrix4f().lookAt(lightPos, center, FACE_UPS[face]);
                Matrix4f projViewMatrix = new Matrix4f(projection).mul(view);
                uniformMap.setUniform("projViewMatrix", projViewMatrix);

                for(Model model : models) {
                    List<Entity> entityList = model.getEntityList();
                    for(Material material : model.getMaterialList()) {
                        for(Mesh mesh : material.getMeshList()) {
                            glBindVertexArray(mesh.getVaoId());
                            for(Entity entity : entityList) {
                                uniformMap.setUniform("modelMatrix", entity.getModelMatrix());
                                AnimationData animationData = entity.getAnimationData();
                                if(animationData == null) {
                                    uniformMap.setUniform("bonesMatrices", AnimationData.DEFAULT_BONES_MATRICES);
                                } else {
                                    uniformMap.setUniform("bonesMatrices"
                                            , animationData.getCurrentFrame().boneMatrices());
                                }
                                glDrawElements(GL_TRIANGLES, mesh.getNumVertices(), GL_UNSIGNED_INT, 0);
                            }
                        }
                    }
                }
            }
        }
        shaderProgram.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
