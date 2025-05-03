package org.polygon.engine.core.scene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.polygon.engine.core.graph.Material;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.graph.TextureCache;
import org.polygon.engine.core.scene.lights.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SceneSerialization {
    private SceneSerialization() {}
    private static Scene parseScene(SceneSaveData data, int width, int height) {
        Scene scene = new Scene(width, height);

        Camera camera = scene.getCamera();
        TextureCache textureCache = scene.getTextureCache();
        Map<String, Model> modelMap = scene.getModelMap();
        Map<String, Entity> entityMap = scene.getEntityMap();
        // Apply loaded camera settings
        camera.setPosition(data.cameraData.posX, data.cameraData.posY, data.cameraData.posZ);
        camera.setRotation(data.cameraData.rotX, data.cameraData.rotY);

        // Recreate textures from saved paths
        if (data.texturePaths != null) {
            for (String path : data.texturePaths) {
                try {
                    textureCache.createTexture(path);
                } catch (Exception e) {
                    System.err.println("Failed to load texture during scene load: " + path);
                    e.printStackTrace();
                }
            }
        }

        // Reconstruct models from saved data
        if (data.modelDataList != null) {
            for (SceneSaveData.ModelData modelData : data.modelDataList) {
                try {
                    Model loadedModel = ModelLoader.loadModel(modelData.modelId, modelData.modelPath,
                            textureCache, modelData.hasAnimation);

                    // Set material texture paths
                    if (modelData.materials != null && loadedModel.getMaterialList() != null &&
                            modelData.materials.size() == loadedModel.getMaterialList().size()) {

                        for (int i = 0; i < modelData.materials.size(); i++) {
                            SceneSaveData.MaterialData matData = modelData.materials.get(i);
                            // Use fully qualified name for Material from graph package
                            Material material = loadedModel.getMaterialList().get(i);

                            material.setTexturePath(matData.texturePath);
                            material.setNormalMapPath(matData.normalMapPath);
                            material.setMetallicMapPath(matData.metallicMapPath);
                            material.setRoughnessMapPath(matData.roughnessMapPath);
                            material.setAoMapPath(matData.aoMapPath);
                            material.setEmissiveMapPath(matData.emissiveMapPath);
                            material.setMetallic(matData.metallic);
                            material.setRoughness(matData.roughness);
                            material.setAoStrength(matData.aoStrength);
                            if (matData.diffuseColor != null && matData.diffuseColor.length == 4) {
                                material.setDiffuseColor(new Vector4f(matData.diffuseColor[0],
                                        matData.diffuseColor[1],
                                        matData.diffuseColor[2],
                                        matData.diffuseColor[3]));
                            }
                        }
                    }

                    modelMap.put(modelData.modelId, loadedModel);
                } catch (Exception e) {
                    System.err.println("Failed to load model during scene load: " + modelData.modelPath
                            + " (ID: " + modelData.modelId + ")");
                    e.printStackTrace();
                }
            }
        }

        // Reconstruct entities from saved data
        if (data.entityDataList != null) {
            for (SceneSaveData.EntityData entityData : data.entityDataList) {
                // Ensure model for this entity exists before creating entity
                Model parentModel = modelMap.get(entityData.modelId);
                if (parentModel != null) {
                    try {
                        Entity newEntity = new Entity(entityData.entityId, entityData.modelId);

                        // Set position
                        if (entityData.position != null && entityData.position.length == 3) {
                            newEntity.setPosition(entityData.position[0], entityData.position[1], entityData.position[2]);
                        }

                        // Set rotation (using quaternion components)
                        if (entityData.rotation != null && entityData.rotation.length == 4) {
                            newEntity.setRotation(new Quaternionf(entityData.rotation[0],
                                    entityData.rotation[1],
                                    entityData.rotation[2],
                                    entityData.rotation[3]));
                        }

                        // Set scale
                        newEntity.setScale(entityData.scale);

                        // Add entity to scene map AND model's list
                        entityMap.put(newEntity.getEntityId(), newEntity);
                        parentModel.getEntityList().add(newEntity);

                    } catch (Exception e) {
                        System.err.println("Failed to load entity during scene load: " + entityData.entityId
                                + " (Model ID: " + entityData.modelId + ")");
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Warning: Could not load entity [" + entityData.entityId +
                            "] because its model [" + entityData.modelId + "] failed to load or was not saved.");
                }
            }
        }

        // Reconstruct SceneLights from saved data
        if (data.sceneLightsData != null) {
            SceneLights newSceneLights = new SceneLights();

            // Ambient
            if (data.sceneLightsData.ambientLightData != null) {
                SceneSaveData.AmbientLightData ambientData = data.sceneLightsData.ambientLightData;
                if (ambientData.color != null && ambientData.color.length == 3) {
                    newSceneLights.getAmbientLight().setColor(new Vector3f(ambientData.color[0]
                            , ambientData.color[1], ambientData.color[2]));
                }
                newSceneLights.getAmbientLight().setIntensity(ambientData.intensity);
            }

            // Directional
            if (data.sceneLightsData.directionalLightData != null) {
                SceneSaveData.DirectionalLightData directionalData = data.sceneLightsData.directionalLightData;
                if (directionalData.color != null && directionalData.color.length == 3) {
                    newSceneLights.getDirectionalLight().setColor(new Vector3f(directionalData.color[0]
                            , directionalData.color[1], directionalData.color[2]));
                }
                newSceneLights.getDirectionalLight().setIntensity(directionalData.intensity);
                if (directionalData.direction != null && directionalData.direction.length == 3) {
                    newSceneLights.getDirectionalLight().setDirection(new Vector3f(directionalData.direction[0]
                            , directionalData.direction[1], directionalData.direction[2]));
                }
            }

            // Point Lights
            if (data.sceneLightsData.pointLightDataList != null) {
                for (SceneSaveData.PointLightData pointData : data.sceneLightsData.pointLightDataList) {
                    PointLight newPointLight = new PointLight();
                    if (pointData.color != null && pointData.color.length == 3) {
                        newPointLight.setColor(new Vector3f(pointData.color[0], pointData.color[1], pointData.color[2]));
                    }
                    newPointLight.setIntensity(pointData.intensity);
                    if (pointData.position != null && pointData.position.length == 3) {
                        newPointLight.setPosition(pointData.position[0], pointData.position[1], pointData.position[2]);
                    }
                    if (pointData.attenuation != null) {
                        PointLight.Attenuation att = new PointLight.Attenuation(newPointLight,
                                pointData.attenuation.constant,
                                pointData.attenuation.linear,
                                pointData.attenuation.exponent);
                        newPointLight.setAttenuation(att);
                    }
                    newPointLight.setRadius(pointData.radius);
                    newSceneLights.getPointLightList().add(newPointLight);
                }
            }

            // Spot Lights
            if (data.sceneLightsData.spotLightDataList != null) {
                for (SceneSaveData.SpotLightData spotData : data.sceneLightsData.spotLightDataList) {
                    SpotLight newSpotLight = new SpotLight();
                    if (spotData.color != null && spotData.color.length == 3) {
                        newSpotLight.setColor(new Vector3f(spotData.color[0], spotData.color[1], spotData.color[2]));
                    }
                    newSpotLight.setIntensity(spotData.intensity);
                    if (spotData.position != null && spotData.position.length == 3) {
                        newSpotLight.setPosition(spotData.position[0], spotData.position[1], spotData.position[2]);
                    }
                    if (spotData.attenuation != null) {
                        PointLight.Attenuation att = new PointLight.Attenuation(newSpotLight,
                                spotData.attenuation.constant,
                                spotData.attenuation.linear,
                                spotData.attenuation.exponent);
                        newSpotLight.setAttenuation(att);
                    }
                    if (spotData.coneDirection != null && spotData.coneDirection.length == 3) {
                        newSpotLight.setConeDirection(new Vector3f(spotData.coneDirection[0], spotData.coneDirection[1]
                                , spotData.coneDirection[2]));
                    }
                    // Convert cutOff (cosine) back to angle for the setter
                    float cutOffAngle = (float) Math.toDegrees(Math.acos(spotData.cutOff));
                    newSpotLight.setCutOffAngle(cutOffAngle);
                    newSpotLight.setRadius(spotData.radius);

                    newSceneLights.getSpotLightList().add(newSpotLight);
                }
            }
            scene.setSceneLights(newSceneLights);
        }

        // Reconstruct Fog from saved data
        if (data.fogData != null) {
            SceneSaveData.FogData fogData = data.fogData;
            Vector3f fogColor = new Vector3f();
            if (fogData.color != null && fogData.color.length == 3) {
                fogColor.set(fogData.color[0], fogData.color[1], fogData.color[2]);
            }
            Fog newFog = new Fog(fogData.active, fogColor, fogData.density);
            scene.setFog(newFog);
        }

        // Reconstruct SkyBox from saved data
        if (data.skyBoxEnvironmentMapPath != null && !data.skyBoxEnvironmentMapPath.isEmpty()) {
            try {
                SkyBox newSkyBox = new SkyBox(data.skyBoxEnvironmentMapPath);
                // Check if the SkyBox actually generated its textures successfully
                if (newSkyBox.getEnvironmentMapTextureId() != -1) {
                    scene.setSkyBox(newSkyBox);
                } else {
                    System.err.println("Warning: Failed to create valid SkyBox from path during load: "
                            + data.skyBoxEnvironmentMapPath);
                }
            } catch (Exception e) {
                System.err.println("Failed to load SkyBox during scene load from path: "
                        + data.skyBoxEnvironmentMapPath);
                e.printStackTrace();
            }
        }

        return scene;
    }


    public static void saveToFile(String filePath, Scene scene) throws IOException {
        SceneSaveData data = new SceneSaveData();
        data.cameraData = new SceneSaveData.CameraData();
        Vector3f camPos = scene.getCamera().getPosition();
        Vector2f camRot = scene.getCamera().getRotation();
        data.cameraData.posX = camPos.x;
        data.cameraData.posY = camPos.y;
        data.cameraData.posZ = camPos.z;
        data.cameraData.rotX = camRot.x;
        data.cameraData.rotY = camRot.y;

        // Save texture paths
        data.texturePaths = new ArrayList<>(scene.getTextureCache().getTexturePaths());

        // Save model data
        data.modelDataList = new ArrayList<>();
        for (Map.Entry<String, Model> entry : scene.getModelMap().entrySet()) {
            Model model = entry.getValue();
            SceneSaveData.ModelData modelData = new SceneSaveData.ModelData();
            modelData.modelId = entry.getKey();
            modelData.modelPath = model.getModelPath();
            modelData.hasAnimation = model.isAnimated();
            modelData.materials = new ArrayList<>();

            if (model.getMaterialList() != null) {
                for (org.polygon.engine.core.graph.Material material : model.getMaterialList()) {
                    SceneSaveData.MaterialData matData = new SceneSaveData.MaterialData();
                    matData.texturePath = material.getTexturePath();
                    matData.normalMapPath = material.getNormalMapPath();
                    matData.metallicMapPath = material.getMetallicMapPath();
                    matData.roughnessMapPath = material.getRoughnessMapPath();
                    matData.aoMapPath = material.getAoMapPath();
                    matData.emissiveMapPath = material.getEmissiveMapPath();
                    // Save other material properties
                    matData.metallic = material.getMetallic();
                    matData.roughness = material.getRoughness();
                    matData.aoStrength = material.getAoStrength();
                    Vector4f diffuse = material.getDiffuseColor();
                    matData.diffuseColor = new float[]{diffuse.x, diffuse.y, diffuse.z, diffuse.w};

                    modelData.materials.add(matData);
                }
            }
            data.modelDataList.add(modelData);
        }

        // Save entity data
        data.entityDataList = new ArrayList<>();
        for (Entity entity : scene.getEntityMap().values()) { // Iterate over map values
            SceneSaveData.EntityData entityData = new SceneSaveData.EntityData();
            entityData.entityId = entity.getEntityId();
            entityData.modelId = entity.getModelId();
            entityData.scale = entity.getScale();

            Vector3f pos = entity.getPosition();
            entityData.position = new float[]{pos.x, pos.y, pos.z};

            Quaternionf rot = entity.getRotation();
            entityData.rotation = new float[]{rot.x, rot.y, rot.z, rot.w};

            data.entityDataList.add(entityData);
        }

        // Save SceneLights data
        if (scene.getSceneLights() != null) {
            data.sceneLightsData = new SceneSaveData.SceneLightsData();

            // Ambient light
            AmbientLight ambient = scene.getSceneLights().getAmbientLight();
            SceneSaveData.AmbientLightData ambientData = new SceneSaveData.AmbientLightData();
            ambientData.intensity = ambient.getIntensity();
            Vector3f ambientColor = ambient.getColor();
            ambientData.color = new float[]{ambientColor.x, ambientColor.y, ambientColor.z};
            data.sceneLightsData.ambientLightData = ambientData;

            // Directional light
            DirectionalLight directional = scene.getSceneLights().getDirectionalLight();
            SceneSaveData.DirectionalLightData directionalData = new SceneSaveData.DirectionalLightData();
            directionalData.intensity = directional.getIntensity();
            Vector3f dirColor = directional.getColor();
            directionalData.color = new float[]{dirColor.x, dirColor.y, dirColor.z};
            Vector3f dirDirection = directional.getDirection();
            directionalData.direction = new float[]{dirDirection.x, dirDirection.y, dirDirection.z};
            data.sceneLightsData.directionalLightData = directionalData;

            // Point Lights
            data.sceneLightsData.pointLightDataList = new ArrayList<>();
            for (PointLight pointLight : scene.getSceneLights().getPointLightList()) {
                SceneSaveData.PointLightData pointData = new SceneSaveData.PointLightData();
                pointData.intensity = pointLight.getIntensity();
                Vector3f pointColor = pointLight.getColor();
                pointData.color = new float[]{pointColor.x, pointColor.y, pointColor.z};
                Vector3f pointPos = pointLight.getPosition();
                pointData.position = new float[]{pointPos.x, pointPos.y, pointPos.z};
                PointLight.Attenuation att = pointLight.getAttenuation();
                if (att != null) {
                    SceneSaveData.AttenuationData attData = new SceneSaveData.AttenuationData();
                    attData.constant = att.getConstant();
                    attData.linear = att.getLinear();
                    attData.exponent = att.getExponent();
                    pointData.attenuation = attData;
                }
                pointData.radius = pointLight.getRadius();
                data.sceneLightsData.pointLightDataList.add(pointData);
            }

            // Spot Lights
            data.sceneLightsData.spotLightDataList = new ArrayList<>();
            for (SpotLight spotLight : scene.getSceneLights().getSpotLightList()) {
                SceneSaveData.SpotLightData spotData = new SceneSaveData.SpotLightData();
                spotData.intensity = spotLight.getIntensity();
                Vector3f spotColor = spotLight.getColor();
                spotData.color = new float[]{spotColor.x, spotColor.y, spotColor.z};
                Vector3f spotPos = spotLight.getPosition();
                spotData.position = new float[]{spotPos.x, spotPos.y, spotPos.z};
                PointLight.Attenuation spotAtt = spotLight.getAttenuation();
                if (spotAtt != null) {
                    SceneSaveData.AttenuationData attData = new SceneSaveData.AttenuationData();
                    attData.constant = spotAtt.getConstant();
                    attData.linear = spotAtt.getLinear();
                    attData.exponent = spotAtt.getExponent();
                    spotData.attenuation = attData;
                }
                Vector3f coneDir = spotLight.getConeDirection();
                spotData.coneDirection = new float[]{coneDir.x, coneDir.y, coneDir.z};
                spotData.cutOff = spotLight.getCutOff();
                spotData.radius = spotLight.getRadius();
                data.sceneLightsData.spotLightDataList.add(spotData);
            }
        }

        // Save Fog data
        if (scene.getFog() != null) {
            data.fogData = new SceneSaveData.FogData();
            data.fogData.active = scene.getFog().isActive();
            data.fogData.density = scene.getFog().getDensity();
            Vector3f fogColor = scene.getFog().getColor();
            data.fogData.color = new float[]{fogColor.x, fogColor.y, fogColor.z};
        }

        // Save SkyBox data (environment map path)
        if (scene.getSkyBox() != null && scene.getSkyBox().getIBLData() != null) {
            data.skyBoxEnvironmentMapPath = scene.getSkyBox().getIBLData().getEnvironmentMapPath();
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(data, writer);
        }
    }

    public static Scene loadFromFile(String filePath, int width, int height) throws IOException {
        Gson gson = new Gson();
        SceneSaveData data;
        try (FileReader reader = new FileReader(filePath)) {
            data = gson.fromJson(reader, SceneSaveData.class);
        }

        if (data == null || data.cameraData == null) {
            throw new IOException("Failed to load or parse scene file: " + filePath);
        }

        // Create a new scene using a private constructor that takes SceneSaveData and dimensions
        Scene loadedScene = parseScene(data, width, height);


        return loadedScene;
    }

    private static class SceneSaveData {
        CameraData cameraData;
        List<String> texturePaths;
        List<ModelData> modelDataList;
        List<EntityData> entityDataList;
        SceneLightsData sceneLightsData;
        FogData fogData;
        String skyBoxEnvironmentMapPath;

        private static class CameraData {
            float posX, posY, posZ;
            float rotX, rotY;
        }

        private static class ModelData {
            String modelId;
            String modelPath;
            boolean hasAnimation;
            List<MaterialData> materials;
        }

        private static class MaterialData {
            String texturePath;
            String normalMapPath;
            String metallicMapPath;
            String roughnessMapPath;
            String aoMapPath;
            String emissiveMapPath;

            float metallic;
            float roughness;
            float aoStrength;
            float[] diffuseColor;
        }

        private static class EntityData {
            String entityId;
            String modelId;
            float[] position;
            float[] rotation;
            float scale;
        }

        private static class SceneLightsData {
            AmbientLightData ambientLightData;
            DirectionalLightData directionalLightData;
            List<PointLightData> pointLightDataList;
            List<SpotLightData> spotLightDataList;
        }

        private static class AmbientLightData {
            float[] color;
            float intensity;
        }

        private static class DirectionalLightData {
            float[] color;
            float intensity;
            float[] direction;
        }

        private static class AttenuationData {
            float constant;
            float linear;
            float exponent;
        }

        private static class PointLightData {
            float[] color;
            float intensity;
            float[] position;
            AttenuationData attenuation;
            float radius;
        }

        private static class SpotLightData {
            float[] color;
            float intensity;
            float[] position;
            AttenuationData attenuation;
            float[] coneDirection;
            float cutOff;
            float radius;
        }

        private static class FogData {
            boolean active;
            float[] color;
            float density;
        }
    }
}
