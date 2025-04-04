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
    public static final Vector4f DEFAULT_COLOR = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
    private Vector4f diffuseColor;
    private Vector4f ambientColor;
    private Vector4f specularColor;
    private float reflectance;

    public Material() {
        // Initialize the meshList that will hold meshes assigned to the Material object.
        meshList = new ArrayList<>();
        ambientColor = DEFAULT_COLOR;
        diffuseColor = DEFAULT_COLOR;
        specularColor = DEFAULT_COLOR;
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

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
        diffuseColor = DEFAULT_COLOR;
    }
    public void setNormalMapPath(String normalMapPath) {
        this.normalMapPath = normalMapPath;
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
}
