#version 410 core

out vec4 FragColor;

in vec3 WorldPos;

uniform samplerCube environmentMapSampler;

uniform int hasIBLData;

void main()
{
    vec3 samplingVec = vec3(WorldPos.x, -WorldPos.y, WorldPos.z);
    vec3 envColor = texture(environmentMapSampler, samplingVec).rgb;
    FragColor = vec4(envColor, 1.0);
}