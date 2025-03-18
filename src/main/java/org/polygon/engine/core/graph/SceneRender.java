package org.polygon.engine.core.graph;

import org.joml.*;
import org.polygon.engine.core.scene.Entity;
import org.polygon.engine.core.scene.Scene;
import org.polygon.engine.core.scene.lights.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class SceneRender {
    private ShaderProgram shaderProgram;
    private UniformMap uniformMap;
    private static final int MAX_POINT_LIGHTS = 5;
    private static final int MAX_SPOT_LIGHTS = 5;
    public SceneRender() {
        // This will hold shader modules
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(
                "resources/shaders/scene.frag", GL_FRAGMENT_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(
                "resources/shaders/scene.vert", GL_VERTEX_SHADER));
        // Initialize A shader program
        shaderProgram = new ShaderProgram(shaderModuleDataList);

        // Initialize a uniformMap
        createUniforms();
    }

    private void createUniforms() {
        uniformMap = new UniformMap(shaderProgram.getProgramId());
        uniformMap.createUniform("projectionMatrix");
        uniformMap.createUniform("modelMatrix");
        uniformMap.createUniform("textSampler");
        uniformMap.createUniform("viewMatrix");
        uniformMap.createUniform("material.ambient");
        uniformMap.createUniform("material.diffuse");
        uniformMap.createUniform("material.specular");
        uniformMap.createUniform("material.reflectance");

        uniformMap.createUniform("ambientLight.color");
        uniformMap.createUniform("ambientLight.intensity");

        uniformMap.createUniform("directionalLight.color");
        uniformMap.createUniform("directionalLight.intensity");
        uniformMap.createUniform("directionalLight.direction");

        uniformMap.createUniform("fog.activeFog");
        uniformMap.createUniform("fog.color");
        uniformMap.createUniform("fog.density");

        uniformMap.createUniform("bypassLighting");

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
    }

    // TODO - point light and spot light needs to be populated in the shader according to proximity with the camera
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

    public void cleanup() {
        // Destroy programId reference from shader program
        shaderProgram.cleanup();
    }

    public void render(Scene scene) {
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        shaderProgram.bind();

        if(scene.getSceneLights() == null) {
            scene.setBypassLighting(true);
        }

        // Set the projectionMatrix uniform with the projection matrix stored in scene.
        uniformMap.setUniform("projectionMatrix", scene.getProjection().getMatrix());
        // set the viewMatrix uniform with the scene's camera viewMatrix.
        uniformMap.setUniform("viewMatrix", scene.getCamera().getViewMatrix());
        // Set the textSampler uniform with 0 (one texture unit)
        uniformMap.setUniform("textSampler", 0);
        uniformMap.setUniform("fog.activeFog", scene.getFog().isActive() ? 1 : 0);
        uniformMap.setUniform("fog.color", scene.getFog().getColor());
        uniformMap.setUniform("fog.density", scene.getFog().getDensity());
        uniformMap.setUniform("bypassLighting", scene.isLightingDisabled());

        if(!scene.isLightingDisabled()) {
            updateLights(scene);
        }

        // Draw calls initiated here

        Collection<Model> models = scene.getModelMap().values();
        TextureCache textureCache = scene.getTextureCache();
        for(Model model : models) {
            List<Entity> entityList = model.getEntityList();

            for(Material material : model.getMaterialList()) {
                uniformMap.setUniform("material.ambient", material.getAmbientColor());
                uniformMap.setUniform("material.diffuse", material.getDiffuseColor());
                uniformMap.setUniform("material.specular", material.getSpecularColor());
                uniformMap.setUniform("material.reflectance", material.getReflectance());
                glActiveTexture(GL_TEXTURE0);
                textureCache.getTexture(material.getTexturePath()).bind();

                for(Mesh mesh : material.getMeshList()) {
                    glBindVertexArray(mesh.getVaoId());
                    for(Entity entity : entityList) {
                        uniformMap.setUniform("modelMatrix", entity.getModelMatrix());
                        glDrawElements(GL_TRIANGLES, mesh.getNumVertices(), GL_UNSIGNED_INT, 0);
                    }
                }
            }
        }

        glBindVertexArray(0);
        shaderProgram.unbind();
        glDisable(GL_BLEND);
    }
}
