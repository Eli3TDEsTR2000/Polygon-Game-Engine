package org.polygon.engine.core.graph;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class Material {
    // Material objects will hold the meshes that have this material assigned to them.
    private List<Mesh> meshList;
    // The Material object's won't hold the texture loaded to OpenGL; instead, the TextureCache will hold the texture
    //      and the Material object will hold a String reference to it called the texturePath.
    private String texturePath;
    private String normalMapPath;
    private String metallicMapPath;
    private String roughnessMapPath;
    private String aoMapPath;
    private String emissiveMapPath;
    public static final Vector4f DEFAULT_COLOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private Vector4f diffuseColor;
    private Vector4f ambientColor;
    private Vector4f specularColor;
    private float reflectance;
    private float metallic;
    private float roughness;
    private float aoStrength;

    public Material() {
        // Initialize the meshList that will hold meshes assigned to the Material object.
        meshList = new ArrayList<>();
        ambientColor = DEFAULT_COLOR;
        diffuseColor = DEFAULT_COLOR;
        specularColor = DEFAULT_COLOR;
        reflectance = 0.0f;

        metallic = 0.0f;
        roughness = 0.5f;
        aoStrength = 1.0f;
    }

    public Material(Vector4f albedoColor, float metallic, float roughness, float aoStrength) {
        this.diffuseColor = albedoColor;
        this.specularColor = DEFAULT_COLOR;
        this.reflectance = 0.0f;
        this.metallic = metallic;
        this.roughness = roughness;
        this.aoStrength = aoStrength;
        meshList = new ArrayList<>();
    }

    public void cleanup() {
        meshList.forEach(Mesh::cleanup);
    }

    public List<Mesh> getMeshList() {
        return meshList;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public String getNormalMapPath() {
        return normalMapPath;
    }

    public String getMetallicMapPath() {
        return metallicMapPath;
    }

    public String getRoughnessMapPath() {
        return roughnessMapPath;
    }

    public String getAoMapPath() {
        return aoMapPath;
    }

    public String getEmissiveMapPath() {
        return emissiveMapPath;
    }

    public Vector4f getDiffuseColor() {
        return diffuseColor;
    }

    public Vector4f getAmbientColor() {
        return ambientColor;
    }

    public Vector4f getSpecularColor() {
        return specularColor;
    }

    public float getReflectance() {
        return reflectance;
    }

    public float getMetallic() {
        return metallic;
    }

    public float getRoughness() {
        return roughness;
    }

    public float getAoStrength() {
        return aoStrength;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
        diffuseColor = DEFAULT_COLOR;
    }

    public void setNormalMapPath(String normalMapPath) {
        this.normalMapPath = normalMapPath;
    }

    public void setMetallicMapPath(String metallicMapPath) {
        this.metallicMapPath = metallicMapPath;
    }

    public void setRoughnessMapPath(String roughnessMapPath) {
        this.roughnessMapPath = roughnessMapPath;
    }

    public void setAoMapPath(String aoMapPath) {
        this.aoMapPath = aoMapPath;
    }

    public void setEmissiveMapPath(String emissiveMapPath) {
        this.emissiveMapPath = emissiveMapPath;
    }

    public void setDiffuseColor(Vector4f diffuseColor) {
        this.diffuseColor = diffuseColor;
    }

    public void setAmbientColor(Vector4f ambientColor) {
        this.ambientColor = ambientColor;
    }

    public void setSpecularColor(Vector4f specularColor) {
        this.specularColor = specularColor;
    }

    public void setReflectance(float reflectance) {
        this.reflectance = reflectance;
    }

    public void setMetallic(float metallic) {
        this.metallic = metallic;
    }

    public void setRoughness(float roughness) {
        this.roughness = roughness;
    }

    public void setAoStrength(float aoStrength) {
        this.aoStrength = aoStrength;
    }
}
