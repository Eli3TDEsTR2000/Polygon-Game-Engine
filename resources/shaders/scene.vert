#version 410

layout (location=0) in vec3 inPosition;
layout (location=1) in vec3 color;

out vec3 outColor;

uniform mat4 projectionMatrix;

void main()
{
    gl_Position = projectionMatrix * vec4(inPosition, 1.0);
    outColor = color;
}