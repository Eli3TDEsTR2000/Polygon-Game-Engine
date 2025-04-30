package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector2f;
import org.polygon.engine.core.scene.Fog;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.lights.*;
import org.polygon.engine.core.utils.ShapeGenerator;
import org.polygon.engine.core.scene.SkyBox;
import org.polygon.engine.core.scene.IBLData;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class LightsRender {
    private final ShaderProgram baseLightShaderProgram;
    private UniformMap baseLightUniformMap;
    private QuadMesh quadMesh;

    private ShaderProgram lightVolumeShaderProgram;
    private UniformMap lightVolumeUniformMap;
    private Mesh sphereMesh;
    private final Matrix4f modelMatrix;
    // Temp vec3 and vec4 used to send light data to the shader.
    private final Vector4f auxVec4;
    private final Vector3f auxVec3;
    private final Vector2f screenSizeVec;

    private static final int IRRADIANCE_MAP_TEXTURE_UNIT = 8;

    public LightsRender() {
        List<ShaderProgram.ShaderModuleData> baseShaderModules = new ArrayList<>();
        baseShaderModules.add(new ShaderProgram.ShaderModuleData("resources/shaders/lights.vert"
                , GL_VERTEX_SHADER));
        baseShaderModules.add(new ShaderProgram.ShaderModuleData("resources/shaders/lights.frag"
                , GL_FRAGMENT_SHADER));
        baseLightShaderProgram = new ShaderProgram(baseShaderModules);
        quadMesh = new QuadMesh();
        createBaseLightUniforms();

        List<ShaderProgram.ShaderModuleData> volumeShaderModules = new ArrayList<>();
        volumeShaderModules.add(new ShaderProgram.ShaderModuleData("resources/shaders/light_volume.vert"
                , GL_VERTEX_SHADER));
        volumeShaderModules.add(new ShaderProgram.ShaderModuleData("resources/shaders/light_volume.frag"
                , GL_FRAGMENT_SHADER));
        lightVolumeShaderProgram = new ShaderProgram(volumeShaderModules);
        createLightVolumeUniforms();

        sphereMesh = ShapeGenerator.generateSphere(1.0f, 16, 16);

        modelMatrix = new Matrix4f();
        auxVec4 = new Vector4f();
        auxVec3 = new Vector3f();
        screenSizeVec = new Vector2f();
    }

    public void cleanup() {
        quadMesh.cleanup();
        baseLightShaderProgram.cleanup();
        if (sphereMesh != null) {
            sphereMesh.cleanup();
        }
        lightVolumeShaderProgram.cleanup();
    }

    private void createBaseLightUniforms() {
        baseLightUniformMap = new UniformMap(baseLightShaderProgram.getProgramId());
        baseLightUniformMap.createUniform("bypassLighting");
        baseLightUniformMap.createUniform("albedoSampler");
        baseLightUniformMap.createUniform("normalSampler");
        baseLightUniformMap.createUniform("materialSampler");
        baseLightUniformMap.createUniform("emissiveSampler");
        baseLightUniformMap.createUniform("depthSampler");
        baseLightUniformMap.createUniform("invProjectionMatrix");
        baseLightUniformMap.createUniform("invViewMatrix");
        baseLightUniformMap.createUniform("ambientLight.color");
        baseLightUniformMap.createUniform("ambientLight.intensity");
        baseLightUniformMap.createUniform("directionalLight.color");
        baseLightUniformMap.createUniform("directionalLight.intensity");
        baseLightUniformMap.createUniform("directionalLight.direction");
        baseLightUniformMap.createUniform("fog.activeFog");
        baseLightUniformMap.createUniform("fog.color");
        baseLightUniformMap.createUniform("fog.density");
        baseLightUniformMap.createUniform("brdfLUT");
        baseLightUniformMap.createUniform("irradianceMap");
        baseLightUniformMap.createUniform("prefilterMap");
        baseLightUniformMap.createUniform("hasIBL");
        for (int i = 0; i < CascadeShadow.SHADOW_MAP_CASCADE_COUNT; i++) {
            baseLightUniformMap.createUniform("shadowMap[" + i + "]");
            baseLightUniformMap.createUniform("cascadeshadows[" + i + "]" + ".projViewMatrix");
            baseLightUniformMap.createUniform("cascadeshadows[" + i + "]" + ".splitDistance");
        }
    }

    private void createLightVolumeUniforms() {
        lightVolumeUniformMap = new UniformMap(lightVolumeShaderProgram.getProgramId());
        lightVolumeUniformMap.createUniform("projectionMatrix");
        lightVolumeUniformMap.createUniform("viewMatrix");
        lightVolumeUniformMap.createUniform("modelMatrix");
        lightVolumeUniformMap.createUniform("albedoSampler");
        lightVolumeUniformMap.createUniform("normalSampler");
        lightVolumeUniformMap.createUniform("materialSampler");
        lightVolumeUniformMap.createUniform("depthSampler");
        lightVolumeUniformMap.createUniform("screenSize");
        lightVolumeUniformMap.createUniform("invProjectionMatrix");
        lightVolumeUniformMap.createUniform("lightType");
        lightVolumeUniformMap.createUniform("pointLight.color");
        lightVolumeUniformMap.createUniform("pointLight.intensity");
        lightVolumeUniformMap.createUniform("pointLight.position_view");
        lightVolumeUniformMap.createUniform("pointLight.attenuation.constant");
        lightVolumeUniformMap.createUniform("pointLight.attenuation.linear");
        lightVolumeUniformMap.createUniform("pointLight.attenuation.exponent");
        lightVolumeUniformMap.createUniform("spotLight.color");
        lightVolumeUniformMap.createUniform("spotLight.intensity");
        lightVolumeUniformMap.createUniform("spotLight.position_view");
        lightVolumeUniformMap.createUniform("spotLight.attenuation.constant");
        lightVolumeUniformMap.createUniform("spotLight.attenuation.linear");
        lightVolumeUniformMap.createUniform("spotLight.attenuation.exponent");
        lightVolumeUniformMap.createUniform("spotLight.coneDirection_view");
        lightVolumeUniformMap.createUniform("spotLight.cutOff");
    }

    public void render(Scene scene, ShadowRender shadowRender, GBuffer gBuffer, int windowWidth, int windowHeight) {
        if(scene.getSceneLights() == null) {
            scene.setBypassLighting(true);
        }

        renderBaseLighting(scene, shadowRender, gBuffer);

        if (!scene.isLightingDisabled()) {
            renderLightVolumes(scene, gBuffer, windowWidth, windowHeight);
        }
    }

    private void renderBaseLighting(Scene scene, ShadowRender shadowRender, GBuffer gBuffer) {
        glDisable(GL_BLEND);

        baseLightShaderProgram.bind();

        int[] textureIds = gBuffer.getTextureIds();
        int numTextures = textureIds != null ? textureIds.length : 0;
        for (int i = 0; i < numTextures; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, textureIds[i]);
        }

        baseLightUniformMap.setUniform("albedoSampler", 0);
        baseLightUniformMap.setUniform("normalSampler", 1);
        baseLightUniformMap.setUniform("materialSampler", 2);
        baseLightUniformMap.setUniform("emissiveSampler", 3);
        baseLightUniformMap.setUniform("depthSampler", 4);

        baseLightUniformMap.setUniform("invProjectionMatrix", scene.getProjection().getInvProjMatrix());
        baseLightUniformMap.setUniform("invViewMatrix", scene.getCamera().getInvViewMatrix());

        baseLightUniformMap.setUniform("bypassLighting", scene.isLightingDisabled());

        if (scene.isLightingDisabled()) {
            glBindVertexArray(quadMesh.getVaoId());
            glDrawElements(GL_TRIANGLES, quadMesh.getNumVertices(), GL_UNSIGNED_INT, 0);
            baseLightShaderProgram.unbind();
            return;
        }

        updateBaseLights(scene);

        Fog fog = scene.getFog();
        baseLightUniformMap.setUniform("fog.activeFog", fog.isActive() ? 1 : 0);
        baseLightUniformMap.setUniform("fog.color", fog.getColor());
        baseLightUniformMap.setUniform("fog.density", fog.getDensity());

        SkyBox skyBox = scene.getSkyBox();
        IBLData iblData = (skyBox != null) ? skyBox.getIBLData() : null;

        if (iblData != null && iblData.getIrradianceMapTextureId() != -1 && iblData.getPrefilterMapTextureId() != -1) {
            baseLightUniformMap.setUniform("hasIBL", true);
            glActiveTexture(GL_TEXTURE20);
            Texture.BRDF_LUT.bind();
            baseLightUniformMap.setUniform("brdfLUT", 20);

            glActiveTexture(GL_TEXTURE0 + IRRADIANCE_MAP_TEXTURE_UNIT);
            glBindTexture(GL_TEXTURE_CUBE_MAP, iblData.getIrradianceMapTextureId());
            baseLightUniformMap.setUniform("irradianceMap", IRRADIANCE_MAP_TEXTURE_UNIT);

            glActiveTexture(GL_TEXTURE25);
            glBindTexture(GL_TEXTURE_CUBE_MAP, iblData.getPrefilterMapTextureId());
            baseLightUniformMap.setUniform("prefilterMap", 25);
        } else {
            baseLightUniformMap.setUniform("hasIBL", false);
        }

        int shadowMapStartUnit = 5;
        List<CascadeShadow> cascadeShadows = shadowRender.getCascadeShadowList();
        for (int i = 0; i < CascadeShadow.SHADOW_MAP_CASCADE_COUNT; i++) {
            baseLightUniformMap.setUniform("shadowMap[" + i + "]", shadowMapStartUnit + i);
            CascadeShadow cascadeShadow = cascadeShadows.get(i);
            baseLightUniformMap.setUniform("cascadeshadows[" + i + "]" + ".projViewMatrix"
                    , cascadeShadow.getProjViewMatrix());
            baseLightUniformMap.setUniform("cascadeshadows[" + i + "]" + ".splitDistance"
                    , cascadeShadow.getSplitDistance());
        }
        shadowRender.getShadowBuffer().bindTextures(GL_TEXTURE0 + shadowMapStartUnit);

        glBindVertexArray(quadMesh.getVaoId());
        glDrawElements(GL_TRIANGLES, quadMesh.getNumVertices(), GL_UNSIGNED_INT, 0);

        baseLightShaderProgram.unbind();
    }

    private void renderLightVolumes(Scene scene, GBuffer gBuffer, int windowWidth, int windowHeight) {
        if (sphereMesh == null) {
            throw new IllegalStateException("Error: Sphere mesh not initialized for light volume rendering.");
        }

        lightVolumeShaderProgram.bind();

        lightVolumeUniformMap.setUniform("projectionMatrix", scene.getProjection().getProjMatrix());
        lightVolumeUniformMap.setUniform("viewMatrix", scene.getCamera().getViewMatrix());
        lightVolumeUniformMap.setUniform("invProjectionMatrix", scene.getProjection().getInvProjMatrix());
        screenSizeVec.set(windowWidth, windowHeight);
        lightVolumeUniformMap.setUniform("screenSize", screenSizeVec);

        int[] textureIds = gBuffer.getTextureIds();
        bindGBufferTextures(textureIds);
        lightVolumeUniformMap.setUniform("albedoSampler", 0);
        lightVolumeUniformMap.setUniform("normalSampler", 1);
        lightVolumeUniformMap.setUniform("materialSampler", 2);
        lightVolumeUniformMap.setUniform("depthSampler", 4);

        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_ONE, GL_ONE);

        glDepthMask(false);
        glDepthFunc(GL_GEQUAL);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);

        Matrix4f viewMatrix = scene.getCamera().getViewMatrix();
        SceneLights sceneLights = scene.getSceneLights();

        lightVolumeUniformMap.setUniform("lightType", 0);
        glBindVertexArray(sphereMesh.getVaoId());
        for (PointLight pointLight : sceneLights.getPointLightList()) {
            if (pointLight.getIntensity() <= 0 || pointLight.getRadius() <= 0) {
                continue;
            }

            updateLightVolumePointLightUniforms(pointLight, viewMatrix);

            modelMatrix.identity()
                       .translate(pointLight.getPosition())
                       .scale(pointLight.getRadius());
            lightVolumeUniformMap.setUniform("modelMatrix", modelMatrix);

            glDrawElements(GL_TRIANGLES, sphereMesh.getNumVertices(), GL_UNSIGNED_INT, 0);
        }

        lightVolumeUniformMap.setUniform("lightType", 1);
        for (SpotLight spotLight : sceneLights.getSpotLightList()) {
            if (spotLight.getIntensity() <= 0 || spotLight.getRadius() <= 0) {
                continue;
            }

            updateLightVolumeSpotLightUniforms(spotLight, viewMatrix);

            modelMatrix.identity()
                       .translate(spotLight.getPosition())
                       .scale(spotLight.getRadius());
            lightVolumeUniformMap.setUniform("modelMatrix", modelMatrix);

            glDrawElements(GL_TRIANGLES, sphereMesh.getNumVertices(), GL_UNSIGNED_INT, 0);
        }
        glBindVertexArray(0);

        glDisable(GL_BLEND);
        glDepthMask(true);
        glDepthFunc(GL_LESS);
        glDisable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        lightVolumeShaderProgram.unbind();
    }

    private void bindGBufferTextures(int[] textureIds) {
        int numTextures = textureIds != null ? textureIds.length : 0;
        for (int i = 0; i < numTextures; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, textureIds[i]);
        }
        if (numTextures == 0) {
             glActiveTexture(GL_TEXTURE0);
        }
    }

    private void updateBaseLights(Scene scene) {
        Matrix4f viewMatrix = scene.getCamera().getViewMatrix();
        SceneLights sceneLights = scene.getSceneLights();

        AmbientLight ambientLight = sceneLights.getAmbientLight();
        baseLightUniformMap.setUniform("ambientLight.color", ambientLight.getColor());
        baseLightUniformMap.setUniform("ambientLight.intensity", ambientLight.getIntensity());

        DirectionalLight directionalLight = sceneLights.getDirectionalLight();
        auxVec4.set(directionalLight.getDirection(), 0).mul(viewMatrix);
        auxVec3.set(auxVec4.x, auxVec4.y, auxVec4.z);
        baseLightUniformMap.setUniform("directionalLight.color", directionalLight.getColor());
        baseLightUniformMap.setUniform("directionalLight.intensity", directionalLight.getIntensity());
        baseLightUniformMap.setUniform("directionalLight.direction", auxVec3);

        Fog fog = scene.getFog();
        baseLightUniformMap.setUniform("fog.activeFog", fog.isActive() ? 1 : 0);
        baseLightUniformMap.setUniform("fog.color", fog.getColor());
        baseLightUniformMap.setUniform("fog.density", fog.getDensity());
    }

    private void updateLightVolumePointLightUniforms(PointLight pointLight, Matrix4f viewMatrix) {
        auxVec4.set(pointLight.getPosition(), 1).mul(viewMatrix);
        auxVec3.set(auxVec4.x, auxVec4.y, auxVec4.z);

        lightVolumeUniformMap.setUniform("pointLight.position_view", auxVec3);

        lightVolumeUniformMap.setUniform("pointLight.color", pointLight.getColor());
        lightVolumeUniformMap.setUniform("pointLight.intensity", pointLight.getIntensity());

        PointLight.Attenuation attenuation = pointLight.getAttenuation();
        lightVolumeUniformMap.setUniform("pointLight.attenuation.constant", attenuation.getConstant());
        lightVolumeUniformMap.setUniform("pointLight.attenuation.linear", attenuation.getLinear());
        lightVolumeUniformMap.setUniform("pointLight.attenuation.exponent", attenuation.getExponent());
    }

    private void updateLightVolumeSpotLightUniforms(SpotLight spotLight, Matrix4f viewMatrix) {
        auxVec4.set(spotLight.getPosition(), 1).mul(viewMatrix);
        auxVec3.set(auxVec4.x, auxVec4.y, auxVec4.z);

        lightVolumeUniformMap.setUniform("spotLight.position_view", auxVec3);

        auxVec4.set(spotLight.getConeDirection(), 0).mul(viewMatrix);
        Vector3f coneDirectionView = auxVec3.set(auxVec4.x, auxVec4.y, auxVec4.z).normalize();
        lightVolumeUniformMap.setUniform("spotLight.coneDirection_view", coneDirectionView);

        lightVolumeUniformMap.setUniform("spotLight.color", spotLight.getColor());
        lightVolumeUniformMap.setUniform("spotLight.intensity", spotLight.getIntensity());
        lightVolumeUniformMap.setUniform("spotLight.cutOff", spotLight.getCutOff());

        PointLight.Attenuation attenuation = spotLight.getAttenuation();
        lightVolumeUniformMap.setUniform("spotLight.attenuation.constant", attenuation.getConstant());
        lightVolumeUniformMap.setUniform("spotLight.attenuation.linear", attenuation.getLinear());
        lightVolumeUniformMap.setUniform("spotLight.attenuation.exponent", attenuation.getExponent());
    }
}
