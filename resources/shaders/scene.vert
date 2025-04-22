#version 410

const int MAX_WEIGHTS = 4;
const int MAX_BONES = 250;

layout (location=0) in vec3 position;
layout (location=1) in vec3 normal;
layout (location=2) in vec3 tangent;
layout (location=3) in vec3 bitangent;
layout (location=4) in vec2 textCoord;
layout (location=5) in vec4 boneWeights;
layout (location=6) in ivec4 boneIndices;

out vec4 outViewPosition;
out vec4 outWorldPosition;
out vec3 outNormal;
out vec3 outTangent;
out vec3 outBitangent;
out vec2 outTextCoord;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform mat4 bonesMatrices[MAX_BONES];
void main()
{
    vec4 totalPos = vec4(0, 0, 0, 0);
    vec4 totalNormal = vec4(0, 0, 0, 0);
    vec4 totalTangent = vec4(0, 0, 0, 0);
    vec4 totalBitangent = vec4(0, 0, 0, 0);

    int count = 0;
    for(int i = 0; i < MAX_WEIGHTS; i++) {
        float weight = boneWeights[i];
        if(weight > 0) {
            count++;
            int boneIndex = boneIndices[i];
            vec4 tmpPos = bonesMatrices[boneIndex] * vec4(position, 1.0);
            totalPos += weight * tmpPos;

            vec4 tmpNormal = bonesMatrices[boneIndex] * vec4(normal, 0.0);
            totalNormal += weight * tmpNormal;

            vec4 tmpTangent = bonesMatrices[boneIndex] * vec4(tangent, 0.0);
            totalTangent += weight * tmpTangent;

            vec4 tmpBitangent = bonesMatrices[boneIndex] * vec4(bitangent, 0.0);
            totalBitangent += weight * tmpBitangent;
        }
    }
    if(count == 0) {
        totalPos = vec4(position, 1.0);
        totalNormal = vec4(normal, 0.0);
        totalTangent = vec4(tangent, 0.0);
        totalBitangent = vec4(bitangent, 0.0);
    }

    mat4 modelViewMatrix = viewMatrix * modelMatrix;
    outWorldPosition = modelMatrix * totalPos;
    outViewPosition = viewMatrix * outWorldPosition;
    gl_Position = projectionMatrix * outViewPosition;
    outNormal = normalize(modelViewMatrix * totalNormal).xyz;
    outTangent = normalize(modelViewMatrix * totalTangent).xyz;
    outBitangent = normalize(modelViewMatrix * totalBitangent).xyz;
    outTextCoord = textCoord;
}