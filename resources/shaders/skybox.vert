#version 410 core

layout (location=0) in vec3 position;

out vec3 WorldPos;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

void main()
{
    // For HDR cubemap, vertex position is the direction vector
    WorldPos = position;
    // Remove translation from view matrix before calculating clip space pos
    mat4 viewNoTranslation = mat4(mat3(viewMatrix));
    vec4 pos = projectionMatrix * viewNoTranslation * vec4(WorldPos, 1.0);
    // Ensure skybox is always at the far plane (z = w)
    gl_Position = pos.xyww;
}