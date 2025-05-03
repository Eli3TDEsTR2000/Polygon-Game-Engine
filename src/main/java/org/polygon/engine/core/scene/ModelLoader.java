package org.polygon.engine.core.scene;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;
import org.polygon.engine.core.Utils;
import org.polygon.engine.core.graph.Material;
import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.graph.TextureCache;

import java.io.File;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.assimp.Assimp.*;

// Model Loading module using assimp to load various 3D formats.
public class ModelLoader {
    public static final int MAX_BONES = 250;
    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();

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
    public static Model loadModel(String modelId, String modelPath, TextureCache textureCache, boolean importAnimations) {
        return loadModel(modelId, modelPath, textureCache
                , aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices
                        | aiProcess_Triangulate | aiProcess_FixInfacingNormals | aiProcess_CalcTangentSpace
                        | aiProcess_LimitBoneWeights | aiProcess_GenBoundingBoxes
                        | (importAnimations ? 0 : aiProcess_PreTransformVertices), importAnimations);
    }

    public static Model loadAnimation(String modelId, String modelPath, TextureCache textureCache) {
        return loadAnimation(modelId, modelPath
                , aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices
                        | aiProcess_Triangulate | aiProcess_FixInfacingNormals | aiProcess_CalcTangentSpace
                        | aiProcess_LimitBoneWeights | aiProcess_GenBoundingBoxes);
    }

    private static Model loadModel(String modelId, String modelPath, TextureCache textureCache, int flags, boolean hasAnimation) {
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
        // Populate the boneList if the model contains bone data.
        List<Bone> boneList = new ArrayList<>();
        for (int i = 0; i < aiScene.mNumMeshes(); i++) {
            AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(i));
            Mesh mesh = processMesh(aiMesh, boneList);
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

        // Populate the model's animation list if the model contains animation data.
        List<Model.Animation> animationList = new ArrayList<>();
        int numAnimations = aiScene.mNumAnimations();
        if(numAnimations > 0) {
            // Build the bone hierarchy using Joint objects.
            Joint rootJoint = buildJointsTree(aiScene.mRootNode(), null);
            Matrix4f globalInverseTransformation = toMatrix(aiScene.mRootNode().mTransformation()).invert();
            // Create animation list where each animation contains animation frames
            // and each frame contains the bone transformation matrices sent to the vertex shader.
            animationList = processAnimations(aiScene, boneList, rootJoint, globalInverseTransformation);
        }

        aiReleaseImport(aiScene);

        return new Model(modelId, modelPath, materialList, animationList, hasAnimation);
    }

    private static Model loadAnimation(String modelId, String modelPath, int flags) {
        // check if the model path exists
        File file = new File(modelPath);
        if (!file.exists()) {
            throw new RuntimeException("Model path does not exists + [" + modelPath + "]");
        }

        // load the model with the selected flags
        AIScene aiScene = aiImportFile(modelPath, flags);
        if (aiScene == null) {
            throw new RuntimeException("Error loading model [modelPath: " + modelPath + "]");
        }

        List<Bone> boneList = new ArrayList<>();
        for (int i = 0; i < aiScene.mNumMeshes(); i++) {
            AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(i));
            processBones(aiMesh, boneList);
        }

        List<Model.Animation> animationList = new ArrayList<>();
        int numAnimations = aiScene.mNumAnimations();
        if(numAnimations > 0) {
            Joint rootJoint = buildJointsTree(aiScene.mRootNode(), null);
            Matrix4f globalInverseTransformation = toMatrix(aiScene.mRootNode().mTransformation()).invert();
            animationList = processAnimations(aiScene, boneList, rootJoint, globalInverseTransformation);
        }

        aiReleaseImport(aiScene);

        return new Model(modelId, modelPath, null, animationList, true);
    }

    private static Material processMaterial(AIMaterial aiMaterial, String modelDir, TextureCache textureCache) {
        Material material = new Material();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIColor4D aiColor4D = AIColor4D.create();

            int result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0, aiColor4D);
            if(result == aiReturn_SUCCESS) {
                 material.setAmbientColor(new Vector4f(aiColor4D.r(), aiColor4D.g(), aiColor4D.b(), aiColor4D.a()));
            }

            result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, aiColor4D);
            if (result == aiReturn_SUCCESS) {
                material.setDiffuseColor(new Vector4f(aiColor4D.r(), aiColor4D.g(), aiColor4D.b(), aiColor4D.a()));
            }

            AIString aiTexturePath = AIString.calloc(stack);
            aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, aiTexturePath
                    , (IntBuffer) null, null, null, null, null, null);
            String texturePath = aiTexturePath.dataString();
            if (texturePath != null && texturePath.length() > 0) {
                material.setTexturePath(modelDir + File.separator + new File(texturePath).getName());
                textureCache.createTexture(material.getTexturePath());
                material.setDiffuseColor(Material.DEFAULT_COLOR);
            }

            AIString aiNormalMapPath = AIString.calloc(stack);
            aiGetMaterialTexture(aiMaterial, aiTextureType_NORMALS, 0, aiNormalMapPath
                    , (IntBuffer) null, null, null, null, null, null);
            String normalMapPath = aiNormalMapPath.dataString();
            if(normalMapPath != null && normalMapPath.length() > 0) {
                material.setNormalMapPath(modelDir + File.separator + new File(normalMapPath).getName());
                textureCache.createTexture(material.getNormalMapPath());
            }

            float[] metallicFactor = { 0.0f };
            int[] pMax = { 1 };
            result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_METALLIC_FACTOR, aiTextureType_NONE
                    , 0, metallicFactor, pMax);
            if (result == aiReturn_SUCCESS) {
                material.setMetallic(metallicFactor[0]);
            }

            AIString aiMetallicMapPath = AIString.calloc(stack);
            aiGetMaterialTexture(aiMaterial, aiTextureType_METALNESS, 0, aiMetallicMapPath
                    , (IntBuffer) null, null, null, null, null, null);
            String metallicMapPath = aiMetallicMapPath.dataString();
            if (metallicMapPath != null && metallicMapPath.length() > 0) {
                material.setMetallicMapPath(modelDir + File.separator + new File(metallicMapPath).getName());
                textureCache.createTexture(material.getMetallicMapPath());
            }

            float[] roughnessFactor = { 0.5f };
            pMax[0] = 1;
            result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_ROUGHNESS_FACTOR, aiTextureType_NONE
                    , 0, roughnessFactor, pMax);
            if (result == aiReturn_SUCCESS) {
                material.setRoughness(roughnessFactor[0]);
            }

            AIString aiRoughnessMapPath = AIString.calloc(stack);
            int resultRoughness = aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE_ROUGHNESS, 0
                    , aiRoughnessMapPath, (IntBuffer) null, null, null, null, null, null);
            if (resultRoughness != aiReturn_SUCCESS) {
                 aiGetMaterialTexture(aiMaterial, aiTextureType_UNKNOWN, 0, aiRoughnessMapPath
                         , (IntBuffer) null, null, null, null, null, null);
            }
            String roughnessMapPath = aiRoughnessMapPath.dataString();
            if (roughnessMapPath != null && roughnessMapPath.length() > 0 && !roughnessMapPath.equals(metallicMapPath)) {
                material.setRoughnessMapPath(modelDir + File.separator + new File(roughnessMapPath).getName());
                textureCache.createTexture(material.getRoughnessMapPath());
            } else if (metallicMapPath != null && metallicMapPath.length() > 0) {
                // If we found a metallic map path via UNKNOWN, assume it's a combined MetallicRoughness texture
                // Point roughness path to the same texture. Shader will need logic to sample correct channels.
                if (roughnessMapPath != null && roughnessMapPath.equals(metallicMapPath)) {
                   material.setRoughnessMapPath(material.getMetallicMapPath());
                }
            }

            AIString aiAoMapPath = AIString.calloc(stack);
            int resultAO = aiGetMaterialTexture(aiMaterial, aiTextureType_AMBIENT_OCCLUSION, 0
                    , aiAoMapPath, (IntBuffer) null, null, null, null, null, null);
            if (resultAO != aiReturn_SUCCESS) {
                resultAO = aiGetMaterialTexture(aiMaterial, aiTextureType_LIGHTMAP, 0
                        , aiAoMapPath, (IntBuffer) null, null, null, null, null, null);
            }
            if (resultAO != aiReturn_SUCCESS) {
                 aiGetMaterialTexture(aiMaterial, aiTextureType_AMBIENT, 0, aiAoMapPath
                         , (IntBuffer) null, null, null, null, null, null);
            }
            String aoMapPath = aiAoMapPath.dataString();
            if (aoMapPath != null && aoMapPath.length() > 0 && !aoMapPath.equals(texturePath)) {
                material.setAoMapPath(modelDir + File.separator + new File(aoMapPath).getName());
                textureCache.createTexture(material.getAoMapPath());
            }

            AIString aiEmissiveMapPath = AIString.calloc(stack);
            aiGetMaterialTexture(aiMaterial, aiTextureType_EMISSIVE, 0, aiEmissiveMapPath
                    , (IntBuffer) null, null, null, null, null, null);
            String emissiveMapPath = aiEmissiveMapPath.dataString();
            if(emissiveMapPath != null && emissiveMapPath.length() > 0) {
                material.setEmissiveMapPath(modelDir + File.separator + new File(emissiveMapPath).getName());
                textureCache.createTexture(material.getEmissiveMapPath());
            }

            return material;
        }
    }

    private static Mesh processMesh(AIMesh aiMesh, List<Bone> boneList) {
        float[] vertices = processVertices(aiMesh);
        float[] normals = processNormals(aiMesh);
        float[] tangents = processTangents(aiMesh, normals);
        float[] bitangents = processBitangents(aiMesh, normals);
        float[] textCoords = processTextCoords(aiMesh);
        int[] indexArray = processIndexArray(aiMesh);
        AnimMeshData animMeshData = processBones(aiMesh, boneList);

        // check if texture coordinates have been defined.
        //      If not, we just assign a set of texture coordinates to 0.0f to ensure consistency of the VAO.
        if(textCoords.length == 0) {
            int numOfElements = (vertices.length / 3) * 2;
            textCoords = new float[numOfElements];
        }

        // Calculate model's bounding box.
        AIAABB aabb = aiMesh.mAABB();
        Vector3f aabbMinCorner = new Vector3f(aabb.mMin().x(), aabb.mMin().y(), aabb.mMin().z());
        Vector3f aabbMaxCorner = new Vector3f(aabb.mMax().x(), aabb.mMax().y(), aabb.mMax().z());

        return new Mesh(vertices, normals, tangents, bitangents, textCoords, indexArray
                , animMeshData.boneIds(), animMeshData.weights(), aabbMinCorner, aabbMaxCorner);
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

    private static float[] processTangents(AIMesh aiMesh, float[] normals) {
        AIVector3D.Buffer buffer = aiMesh.mTangents();
        float[] tangents = new float[buffer.remaining() * 3];
        int position = 0;
        while(buffer.remaining() > 0) {
            AIVector3D aiTangent = buffer.get();
            tangents[position++] = aiTangent.x();
            tangents[position++] = aiTangent.y();
            tangents[position++] = aiTangent.z();
        }

        if(tangents.length == 0) {
            tangents = new float[normals.length];
        }

        return tangents;
    }

    private static float[] processBitangents(AIMesh aiMesh, float[] normals) {
        AIVector3D.Buffer buffer = aiMesh.mBitangents();
        float[] bitangents = new float[buffer.remaining() * 3];
        int position = 0;
        while(buffer.remaining() > 0) {
            AIVector3D aiBitangent = buffer.get();
            bitangents[position++] = aiBitangent.x();
            bitangents[position++] = aiBitangent.y();
            bitangents[position++] = aiBitangent.z();
        }

        if(bitangents.length == 0) {
            bitangents = new float[normals.length];
        }

        return bitangents;
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

    private static AnimMeshData processBones(AIMesh aiMesh, List<Bone> boneList) {
        List<Integer> boneIds = new ArrayList<>();
        List<Float> weights = new ArrayList<>();

        Map<Integer, List<VertexWeight>> weightSet = new HashMap<>();
        int numBones = aiMesh.mNumBones();
        PointerBuffer aiBones = aiMesh.mBones();
        for(int i = 0; i < numBones; i++) {
            AIBone aiBone = AIBone.create(aiBones.get(i));
            int id = boneList.size();
            Bone bone = new Bone(id, aiBone.mName().dataString(), toMatrix(aiBone.mOffsetMatrix()));
            boneList.add(bone);
            int numWeights = aiBone.mNumWeights();
            AIVertexWeight.Buffer aiWeights = aiBone.mWeights();
            for(int j = 0; j < numWeights; j++) {
                AIVertexWeight aiVertexWeight = aiWeights.get(j);
                VertexWeight vertexWeight = new VertexWeight(bone.boneId(), aiVertexWeight.mVertexId()
                        , aiVertexWeight.mWeight());
                List<VertexWeight> vertexWeightList = weightSet.get(vertexWeight.vertexId());
                if(vertexWeightList == null) {
                    vertexWeightList = new ArrayList<>();
                    weightSet.put(vertexWeight.vertexId(), vertexWeightList);
                }
                vertexWeightList.add(vertexWeight);
            }
        }

        int numVertices = aiMesh.mNumVertices();
        for(int i = 0; i < numVertices; i++) {
            List<VertexWeight> vertexWeightList = weightSet.get(i);
            int size = vertexWeightList != null ? vertexWeightList.size() : 0;
            for(int j = 0; j < Mesh.MAX_WEIGHTS; j++) {
                if(j < size) {
                    VertexWeight vertexWeight = vertexWeightList.get(j);
                    weights.add(vertexWeight.weight());
                    boneIds.add(vertexWeight.boneId());
                } else {
                    weights.add(0.0f);
                    boneIds.add(0);
                }
            }
        }

        return new AnimMeshData(Utils.listFloatToArray(weights), Utils.listIntToArray(boneIds));
    }

    private static Joint buildJointsTree(AINode aiNode, Joint parentNode) {
        String jointName = aiNode.mName().dataString();
        Joint joint = new Joint(jointName, parentNode, toMatrix(aiNode.mTransformation()));

        int numChildren = aiNode.mNumChildren();
        PointerBuffer aiChildren = aiNode.mChildren();
        for(int i = 0; i < numChildren; i++) {
            AINode aiChildNode = AINode.create(aiChildren.get(i));
            Joint childJoint = buildJointsTree(aiChildNode, joint);
            joint.addChild(childJoint);
        }

        return joint;
    }

    private static List<Model.Animation> processAnimations(AIScene aiScene, List<Bone> boneList, Joint rootNode
            , Matrix4f globalInverseTransformation) {
        List<Model.Animation> animationList = new ArrayList<>();

        int numAnimations = aiScene.mNumAnimations();
        PointerBuffer aiAnimations = aiScene.mAnimations();
        for(int i = 0; i < numAnimations; i++) {
            AIAnimation aiAnimation = AIAnimation.create(aiAnimations.get(i));
            int maxFrames = calcAnimationMaxFrames(aiAnimation);

            List<Model.AnimatedFrame> frames = new ArrayList<>();
            Model.Animation animation = new Model.Animation(aiAnimation.mName().dataString()
                    , aiAnimation.mDuration(), frames);
            animationList.add(animation);

            for(int j = 0; j < maxFrames; j++) {
                Matrix4f[] boneMatrices = new Matrix4f[MAX_BONES];
                Arrays.fill(boneMatrices, IDENTITY_MATRIX);
                Model.AnimatedFrame animatedFrame = new Model.AnimatedFrame(boneMatrices);
                buildFrameMatrices(aiAnimation, boneList, animatedFrame, j, rootNode
                        , rootNode.getTransform(), globalInverseTransformation);
                frames.add(animatedFrame);
            }
        }

        return animationList;
    }

    private static int calcAnimationMaxFrames(AIAnimation aiAnimation) {
        int maxFrames = 0;
        int numNodeAnims = aiAnimation.mNumChannels();
        PointerBuffer aiChannels = aiAnimation.mChannels();
        for(int i = 0; i < numNodeAnims; i++) {
            AINodeAnim aiNodeAnim = AINodeAnim.create(aiChannels.get(i));
            int numFrames = Math.max(Math.max(aiNodeAnim.mNumPositionKeys(), aiNodeAnim.mNumScalingKeys())
                    , aiNodeAnim.mNumRotationKeys());
            maxFrames = Math.max(maxFrames, numFrames);
        }

        return maxFrames;
    }

    private static void buildFrameMatrices(AIAnimation aiAnimation, List<Bone> boneList
            , Model.AnimatedFrame animatedFrame, int frame, Joint node, Matrix4f parentTransformation
            , Matrix4f globalInverseTransformation) {
        String nodeName = node.getName();
        AINodeAnim aiNodeAnim = findAINodeAnim(aiAnimation, nodeName);
        Matrix4f nodeTransform = node.getTransform();
        if(aiNodeAnim != null) {
            nodeTransform = buildNodeTransformationMatrix(aiNodeAnim, frame);
        }
        Matrix4f nodeGlobalTransform = new Matrix4f(parentTransformation).mul(nodeTransform);

        List<Bone> affectedBones = boneList.stream().filter(bone -> bone.boneName().equals(nodeName)).toList();
        for(Bone bone : affectedBones) {
            Matrix4f boneTransform = new Matrix4f(globalInverseTransformation).mul(nodeGlobalTransform)
                    .mul(bone.offsetMatrix());
            animatedFrame.boneMatrices()[bone.boneId()] = boneTransform;
        }

        for(Joint childNode : node.getChildren()) {
            buildFrameMatrices(aiAnimation, boneList, animatedFrame, frame, childNode
                    , nodeGlobalTransform, globalInverseTransformation);
        }
    }

    private static AINodeAnim findAINodeAnim(AIAnimation aiAnimation, String nodeName) {
        AINodeAnim result = null;
        int numAnimNodes = aiAnimation.mNumChannels();
        PointerBuffer aiChannels = aiAnimation.mChannels();
        for(int i = 0; i < numAnimNodes; i++) {
            AINodeAnim aiNodeAnim = AINodeAnim.create(aiChannels.get(i));
            if(nodeName.equals(aiNodeAnim.mNodeName().dataString())) {
                result = aiNodeAnim;
                break;
            }
        }

        return result;
    }

    private static Matrix4f buildNodeTransformationMatrix(AINodeAnim aiNodeAnim, int frame) {
        AIVectorKey.Buffer positionKeys = aiNodeAnim.mPositionKeys();
        AIVectorKey.Buffer scalingKeys = aiNodeAnim.mScalingKeys();
        AIQuatKey.Buffer rotationKeys = aiNodeAnim.mRotationKeys();

        AIVectorKey aiVectorKey;
        AIVector3D aiVector;

        Matrix4f nodeTransform = new Matrix4f();
        int numPositionKeys = aiNodeAnim.mNumPositionKeys();
        if(numPositionKeys > 0) {
            aiVectorKey = positionKeys.get(Math.min(numPositionKeys - 1, frame));
            aiVector = aiVectorKey.mValue();
            nodeTransform.translate(aiVector.x(), aiVector.y(), aiVector.z());
        }

        int numRotationKeys = aiNodeAnim.mNumRotationKeys();
        if(numRotationKeys > 0) {
            AIQuatKey quatKey = rotationKeys.get(Math.min(numRotationKeys - 1, frame));
            AIQuaternion aiQuaternion = quatKey.mValue();
            Quaternionf quaternion = new Quaternionf(aiQuaternion.x(), aiQuaternion.y()
                    , aiQuaternion.z(), aiQuaternion.w());
            nodeTransform.rotate(quaternion);
        }

        int numScalingKeys = aiNodeAnim.mNumScalingKeys();
        if(numScalingKeys > 0) {
            aiVectorKey = scalingKeys.get(Math.min(numScalingKeys - 1, frame));
            aiVector = aiVectorKey.mValue();
            nodeTransform.scale(aiVector.x(), aiVector.y(), aiVector.z());
        }

        return nodeTransform;
    }

    private static Matrix4f toMatrix(AIMatrix4x4 aiMatrix4x4) {
        Matrix4f result = new Matrix4f();
        result.m00(aiMatrix4x4.a1());
        result.m10(aiMatrix4x4.a2());
        result.m20(aiMatrix4x4.a3());
        result.m30(aiMatrix4x4.a4());
        result.m01(aiMatrix4x4.b1());
        result.m11(aiMatrix4x4.b2());
        result.m21(aiMatrix4x4.b3());
        result.m31(aiMatrix4x4.b4());
        result.m02(aiMatrix4x4.c1());
        result.m12(aiMatrix4x4.c2());
        result.m22(aiMatrix4x4.c3());
        result.m32(aiMatrix4x4.c4());
        result.m03(aiMatrix4x4.d1());
        result.m13(aiMatrix4x4.d2());
        result.m23(aiMatrix4x4.d3());
        result.m33(aiMatrix4x4.d4());

        return result;
    }

    public record AnimMeshData(float[] weights, int[] boneIds) {}
    private record Bone(int boneId, String boneName, Matrix4f offsetMatrix) {}
    private record VertexWeight(int boneId, int vertexId, float weight) {}
}

