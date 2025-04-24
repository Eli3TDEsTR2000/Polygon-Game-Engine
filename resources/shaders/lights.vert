#version 410 core

layout (location=0) in vec3 inPos;
layout (location=1) in vec2 inTextCoord;

out vec2 outTextCoord;

void main()
{
    outTextCoord = inTextCoord;
    gl_Position = vec4(inPos, 1.0f);
}