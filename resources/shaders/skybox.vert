#version 410 core

layout (location=0) in vec3 position;
layout (location=1) in vec3 normal;
layout (location=4) in vec2 textCoord;

out vec2 outTextCoord;
out vec3 WorldPos;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform int hasIBLData;

void main()
{
    if (hasIBLData == 1) {
        // For HDR cubemap, vertex position is the direction vector
        WorldPos = position;
        // Remove translation from view matrix before calculating clip space pos
        mat4 viewNoTranslation = mat4(mat3(viewMatrix)); 
        vec4 pos = projectionMatrix * viewNoTranslation * vec4(WorldPos, 1.0);
        // Ensure skybox is always at the far plane (z = w)
        gl_Position = pos.xyww;
    } else {
        // We just render a 3D model of the skybox
        // and the skybox's color will be sampled from the 3D model's assigned textures.
        gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
        outTextCoord = textCoord;
    }
}