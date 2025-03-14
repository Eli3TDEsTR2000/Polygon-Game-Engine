package org.polygon.engine.core.scene;

import org.joml.Vector4f;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;
import org.polygon.engine.core.graph.Material;
import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.graph.TextureCache;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class ModelLoader {
    // This is a Model Loading module using assimp to load various 3D formats.
    private ModelLoader() {

    }

    // Calls the second loadModel method with assimp flags.
    // aiProcess_JoinIdenticalVertices, reduces the number of vertices by
    //      identifying those that can be reused between two faces.
    // aiProcess_Triangulate, Triangulate quads.
    // aiProcess_FixInfacingNormals, fix normals that are inward.
    // aiProcess_CalcTangentSpace, calculate normal tangents that will be used in lighting.
    // aiProcess_LimitBoneWeights, limits weights affected by a single vertex.
    // aiProcess_PreTransformVertices, transforms loaded model to the OpenGL origin.
    //      DO NOT THIS FLAG USE WITH ANIMATED MODELS.
    public static Model loadModel(String modelId, String modelPath, TextureCache textureCache) {
        return loadModel(modelId, modelPath, textureCache
                , aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices
                        | aiProcess_Triangulate | aiProcess_FixInfacingNormals | aiProcess_CalcTangentSpace
                        | aiProcess_LimitBoneWeights | aiProcess_PreTransformVertices);
    }

    public static Model loadModel(String modelId, String modelPath, TextureCache textureCache, int flags) {
        // check if the model path exists
        File file = new File(modelPath);
        if (!file.exists()) {
            throw new RuntimeException("Model path does not exists + [" + modelPath + "]");
        }
        // hold the parent directory of the model file
        String modelDir = file.getParent();

        // load the model with the selected flags
        AIScene aiScene = aiImportFile(modelPath, flags);
        if (aiScene == null) {
            throw new RuntimeException("Error loading model [modelPath: " + modelPath + "]");
        }

        // load materials to the textureCache
        List<Material> materialList = new ArrayList<>();
        for (int i = 0; i < aiScene.mNumMaterials(); i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiScene.mMaterials().get(i));
            materialList.add(processMaterial(aiMaterial, modelDir, textureCache));
        }

        // Load the model's 3D meshes.
        // Assign meshes for each material.
        // If the mesh doesn't have a material assigned to it then assign the default material to it instead.
        // If the defaultMaterial has meshes assigned to it then add the defaultMaterial to the materialList.
        // create model with model id and the meshList and return it.
        Material defaultMaterial = new Material();
        for (int i = 0; i < aiScene.mNumMeshes(); i++) {
            AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(i));
            Mesh mesh = processMesh(aiMesh);
            int materialIndex = aiMesh.mMaterialIndex();
            Material material;
            if (materialIndex >= 0 && materialIndex < materialList.size()) {
                material = materialList.get(materialIndex);
            } else {
                // Load the default material.
                material = defaultMaterial;
            }
            material.getMeshList().add(mesh);
        }

        // Check if the model contains a mesh with unassigned material.
        if (!defaultMaterial.getMeshList().isEmpty()) {
            materialList.add(defaultMaterial);
        }

        return new Model(modelId, materialList);
    }

    private static Material processMaterial(AIMaterial aiMaterial, String modelDir, TextureCache textureCache) {
        Material material = new Material();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIColor4D aiColor4D = AIColor4D.create();

            int result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_AMBIENT
                    , aiTextureType_NONE, 0, aiColor4D);
            if(result == aiReturn_SUCCESS) {
                material.setAmbientColor(new Vector4f(aiColor4D.r(), aiColor4D.g(), aiColor4D.b(), aiColor4D.a()));
            }

            // Get the diffuse color of the texture and assign it to the material by default
            result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_DIFFUSE
                    , aiTextureType_NONE, 0, aiColor4D);
            if (result == aiReturn_SUCCESS) {
                material.setDiffuseColor(new Vector4f(aiColor4D.r(), aiColor4D.g(), aiColor4D.b(), aiColor4D.a()));
            }

            result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE, 0, aiColor4D);
            if(result == aiReturn_SUCCESS) {
                material.setSpecularColor(new Vector4f(aiColor4D.r(), aiColor4D.g(), aiColor4D.b(), aiColor4D.a()));
            }

            float reflectance = 0.0f;
            float[] shininessFactor = new float[] {0.0f};
            int[] pMax = new int[] {1};
            result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_SHININESS_STRENGTH, aiTextureType_NONE
                    , 0, shininessFactor, pMax);
            if(result != aiReturn_SUCCESS) {
                reflectance = shininessFactor[0];
            }
            material.setReflectance(reflectance);

            // If there is a texture path defined, assign the texture to the material and
            //      set the diffuse color to the default color.
            AIString aiTexturePath = AIString.calloc(stack);
            aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, aiTexturePath, (IntBuffer) null
                    , null, null, null, null, null);
            String texturePath = aiTexturePath.dataString();
            if (texturePath != null && texturePath.length() > 0) {
                material.setTexturePath(modelDir + File.separator + new File(texturePath).getName());
                textureCache.createTexture(material.getTexturePath());
                material.setDiffuseColor(Material.DEFAULT_COLOR);
            }

            return material;
        }
    }

    private static Mesh processMesh(AIMesh aiMesh) {
        float[] vertices = processVertices(aiMesh);
        float[] normals = processNormals(aiMesh);
        float[] textCoords = processTextCoords(aiMesh);
        int[] indexArray = processIndexArray(aiMesh);

        // check if texture coordinates have been defined.
        //      If not, we just assign a set of texture coordinates to 0.0f to ensure consistency of the VAO.
        if(textCoords.length == 0) {
            int numOfElements = (vertices.length / 3) * 2;
            textCoords = new float[numOfElements];
        }

        return new Mesh(vertices, normals, textCoords, indexArray);
    }

    private static float[] processVertices(AIMesh aiMesh) {
        AIVector3D.Buffer buffer = aiMesh.mVertices();
        float[] vertices = new float[buffer.remaining() * 3];
        int position = 0;
        while(buffer.remaining() > 0) {
            AIVector3D textCoords = buffer.get();
            vertices[position++] = textCoords.x();
            vertices[position++] = textCoords.y();
            vertices[position++] = textCoords.z();
        }

        return vertices;
    }

    private static float[] processNormals(AIMesh aiMesh) {
        AIVector3D.Buffer buffer = aiMesh.mNormals();
        float[] normals = new float[buffer.remaining() * 3];
        int position = 0;
        while(buffer.remaining() > 0) {
            AIVector3D normal = buffer.get();
            normals[position++] = normal.x();
            normals[position++] = normal.y();
            normals[position++] = normal.z();
        }
        return normals;
    }

    private static float[] processTextCoords(AIMesh aiMesh) {
        AIVector3D.Buffer buffer = aiMesh.mTextureCoords(0);
        if(buffer == null) {
            return new float[]{};
        }
        float[] textCoords = new float[buffer.remaining() * 2];
        int position = 0;
        while(buffer.remaining() > 0) {
            AIVector3D aiTextCoords = buffer.get();
            textCoords[position++] = aiTextCoords.x();
            textCoords[position++] = 1 - aiTextCoords.y();
        }
        return textCoords;
    }

    private static int[] processIndexArray(AIMesh aiMesh) {
        List<Integer> indexArray = new ArrayList<>();
        AIFace.Buffer aiFaces = aiMesh.mFaces();
        for(int i = 0; i < aiMesh.mNumFaces(); i++) {
            AIFace aiFace = aiFaces.get(i);
            IntBuffer buffer = aiFace.mIndices();
            while(buffer.remaining() > 0) {
                indexArray.add(buffer.get());
            }
        }

        return indexArray.stream().mapToInt(Integer::intValue).toArray();
    }
}

