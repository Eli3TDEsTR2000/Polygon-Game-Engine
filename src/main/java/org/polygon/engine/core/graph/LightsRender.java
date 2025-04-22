package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.polygon.engine.core.scene.Fog;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.lights.*;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class LightsRender {
    private static final int MAX_POINT_LIGHTS = 5;
    private static final int MAX_SPOT_LIGHTS = 5;

    private final ShaderProgram shaderProgram;
    private QuadMesh quadMesh;
    private UniformMap uniformMap;

    public LightsRender() {
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/lights.vert"
                , GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("resources/shaders/lights.frag"
                , GL_FRAGMENT_SHADER));
        shaderProgram = new ShaderProgram(shaderModuleDataList);
        quadMesh = new QuadMesh();
        createUniforms();
    }

    public void cleanup() {
        quadMesh.cleanup();
        shaderProgram.cleanup();
    }

    public void createUniforms() {
        uniformMap = new UniformMap(shaderProgram.getProgramId());

        uniformMap.createUniform("bypassLighting");

        uniformMap.createUniform("albedoSampler");
        uniformMap.createUniform("normalSampler");
        uniformMap.createUniform("specularSampler");
        uniformMap.createUniform("depthSampler");
        uniformMap.createUniform("invProjectionMatrix");
        uniformMap.createUniform("invViewMatrix");

        uniformMap.createUniform("ambientLight.color");
        uniformMap.createUniform("ambientLight.intensity");

        uniformMap.createUniform("directionalLight.color");
        uniformMap.createUniform("directionalLight.intensity");
        uniformMap.createUniform("directionalLight.direction");

        for(int i = 0; i < MAX_POINT_LIGHTS; i++)  {
            String name = "pointLights[" + i + "]";
            uniformMap.createUniform(name + ".color");
            uniformMap.createUniform(name + ".intensity");
            uniformMap.createUniform(name + ".position");
            uniformMap.createUniform(name + ".attenuation.constant");
            uniformMap.createUniform(name + ".attenuation.linear");
            uniformMap.createUniform(name + ".attenuation.exponent");
        }

        for(int i = 0; i < MAX_SPOT_LIGHTS; i++) {
            String name = "spotLights[" + i + "]";
            uniformMap.createUniform(name + ".color");
            uniformMap.createUniform(name + ".intensity");
            uniformMap.createUniform(name + ".position");
            uniformMap.createUniform(name + ".attenuation.constant");
            uniformMap.createUniform(name + ".attenuation.linear");
            uniformMap.createUniform(name + ".attenuation.exponent");
            uniformMap.createUniform(name + ".coneDirection");
            uniformMap.createUniform(name + ".cutOff");
        }

        uniformMap.createUniform("fog.activeFog");
        uniformMap.createUniform("fog.color");
        uniformMap.createUniform("fog.density");

        for (int i = 0; i < CascadeShadow.SHADOW_MAP_CASCADE_COUNT; i++) {
            uniformMap.createUniform("shadowMap[" + i + "]");
            uniformMap.createUniform("cascadeshadows[" + i + "]" + ".projViewMatrix");
            uniformMap.createUniform("cascadeshadows[" + i + "]" + ".splitDistance");
        }
    }

    public void render(Scene scene, ShadowRender shadowRender, GBuffer gBuffer) {

        if(scene.getSceneLights() == null) {
            scene.setBypassLighting(true);
        }

        shaderProgram.bind();


        // Bind the G-Buffer textures
        int[] textureIds = gBuffer.getTextureIds();
        int numTextures = textureIds != null ? textureIds.length : 0;
        for (int i = 0; i < numTextures; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, textureIds[i]);
        }

        uniformMap.setUniform("albedoSampler", 0);
        uniformMap.setUniform("normalSampler", 1);
        uniformMap.setUniform("specularSampler", 2);
        uniformMap.setUniform("depthSampler", 3);

        uniformMap.setUniform("invProjectionMatrix", scene.getProjection().getInvProjMatrix());
        uniformMap.setUniform("invViewMatrix", scene.getCamera().getInvViewMatrix());

        uniformMap.setUniform("bypassLighting", scene.isLightingDisabled());

        if(scene.isLightingDisabled()) {
            glBindVertexArray(quadMesh.getVaoId());
            glDrawElements(GL_TRIANGLES, quadMesh.getNumVertices(), GL_UNSIGNED_INT, 0);

            return;
        }

        updateLights(scene);

        Fog fog = scene.getFog();
        uniformMap.setUniform("fog.activeFog", fog.isActive() ? 1 : 0);
        uniformMap.setUniform("fog.color", fog.getColor());
        uniformMap.setUniform("fog.density", fog.getDensity());

        int start = 4;
        List<CascadeShadow> cascadeShadows = shadowRender.getCascadeShadowList();
        for (int i = 0; i < CascadeShadow.SHADOW_MAP_CASCADE_COUNT; i++) {
            uniformMap.setUniform("shadowMap[" + i + "]", start + i);
            CascadeShadow cascadeShadow = cascadeShadows.get(i);
            uniformMap.setUniform("cascadeshadows[" + i + "]" + ".projViewMatrix"
                    , cascadeShadow.getProjViewMatrix());
            uniformMap.setUniform("cascadeshadows[" + i + "]" + ".splitDistance"
                    , cascadeShadow.getSplitDistance());
        }
        shadowRender.getShadowBuffer().bindTextures(GL_TEXTURE0 + start);


        glBindVertexArray(quadMesh.getVaoId());
        glDrawElements(GL_TRIANGLES, quadMesh.getNumVertices(), GL_UNSIGNED_INT, 0);

        shaderProgram.unbind();
    }

    private void updateLights(Scene scene) {
        Matrix4f viewMatrix = scene.getCamera().getViewMatrix();

        SceneLights sceneLights = scene.getSceneLights();

        AmbientLight ambientLight = sceneLights.getAmbientLight();
        uniformMap.setUniform("ambientLight.color", ambientLight.getColor());
        uniformMap.setUniform("ambientLight.intensity", ambientLight.getIntensity());

        DirectionalLight directionalLight = sceneLights.getDirectionalLight();
        Vector4f auxDirection = new Vector4f(directionalLight.getDirection(), 0);
        auxDirection.mul(viewMatrix);
        Vector3f direction = new Vector3f(auxDirection.x, auxDirection.y, auxDirection.z);
        uniformMap.setUniform("directionalLight.color", directionalLight.getColor());
        uniformMap.setUniform("directionalLight.intensity", directionalLight.getIntensity());
        uniformMap.setUniform("directionalLight.direction", direction);

        List<PointLight> pointLightList = sceneLights.getPointLightList();
        int numOfPointLights = pointLightList.size();
        PointLight pointLight;
        for(int i = 0; i < MAX_POINT_LIGHTS; i++) {
            if(i < numOfPointLights) {
                pointLight = pointLightList.get(i);
            } else {
                pointLight = null;
            }
            String name = "pointLights[" + i + "]";
            updatePointLight(pointLight, name, viewMatrix);
        }

        List<SpotLight> spotLightList = sceneLights.getSpotLightList();
        int numOfSpotLights = spotLightList.size();
        SpotLight spotLight;
        for(int i = 0; i < MAX_SPOT_LIGHTS; i++) {
            if(i < numOfSpotLights) {
                spotLight = spotLightList.get(i);
            } else {
                spotLight = null;
            }
            String name = "spotLights[" + i + "]";
            updateSpotLight(spotLight, name, viewMatrix);
        }
    }

    private void updatePointLight(PointLight pointLight, String name, Matrix4f viewMatrix) {
        Vector4f auxPosition = new Vector4f();
        Vector3f color = new Vector3f();
        Vector3f position = new Vector3f();
        float intensity = 0.0f;
        float constant = 0.0f;
        float linear = 0.0f;
        float exponent = 0.0f;

        if(pointLight != null) {
            auxPosition.set(pointLight.getPosition(), 1);
            auxPosition.mul(viewMatrix);
            position.set(auxPosition.x, auxPosition.y, auxPosition.z);
            color.set(pointLight.getColor());
            intensity = pointLight.getIntensity();
            PointLight.Attenuation attenuation = pointLight.getAttenuation();
            constant = attenuation.getConstant();
            linear = attenuation.getLinear();
            exponent = attenuation.getExponent();
        }

        uniformMap.setUniform(name + ".color", color);
        uniformMap.setUniform(name + ".intensity", intensity);
        uniformMap.setUniform(name + ".position", position);
        uniformMap.setUniform(name + ".attenuation.constant", constant);
        uniformMap.setUniform(name + ".attenuation.linear", linear);
        uniformMap.setUniform(name + ".attenuation.exponent", exponent);
    }

    private void updateSpotLight(SpotLight spotLight, String name, Matrix4f viewMatrix) {
        Vector4f auxPosition = new Vector4f();
        Vector3f color = new Vector3f();
        Vector3f position = new Vector3f();
        Vector3f coneDirection = new Vector3f();
        float intensity = 0.0f;
        float constant = 0.0f;
        float linear = 0.0f;
        float exponent = 0.0f;
        float cutOff = 0.0f;

        if(spotLight != null) {
            auxPosition.set(spotLight.getPosition(), 1);
            auxPosition.mul(viewMatrix);
            position.set(auxPosition.x, auxPosition.y, auxPosition.z);
            color.set(spotLight.getColor());
            intensity = spotLight.getIntensity();
            PointLight.Attenuation attenuation = spotLight.getAttenuation();
            constant = attenuation.getConstant();
            linear = attenuation.getLinear();
            exponent = attenuation.getExponent();
            coneDirection = spotLight.getConeDirection();
            cutOff = spotLight.getCutOff();
        }

        uniformMap.setUniform(name + ".color", color);
        uniformMap.setUniform(name + ".intensity", intensity);
        uniformMap.setUniform(name + ".position", position);
        uniformMap.setUniform(name + ".attenuation.constant", constant);
        uniformMap.setUniform(name + ".attenuation.linear", linear);
        uniformMap.setUniform(name + ".attenuation.exponent", exponent);
        uniformMap.setUniform(name + ".coneDirection", coneDirection);
        uniformMap.setUniform(name + ".cutOff", cutOff);
    }
}
