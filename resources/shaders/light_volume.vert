#version 410 core

layout (location=0) in vec3 position;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;

out vec3 FragPos_view;

void main()
{
    vec4 worldPos = modelMatrix * vec4(position, 1.0);
    vec4 viewPos = viewMatrix * worldPos;

    FragPos_view = viewPos.xyz;

    gl_Position = projectionMatrix * viewPos;
} 