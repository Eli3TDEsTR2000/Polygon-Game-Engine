#version 410 core

out vec4 FragColor;

in vec3 WorldPos;
in vec2 outTextCoord;

uniform samplerCube environmentMapSampler;
uniform sampler2D textSampler;

uniform vec4 diffuse;
uniform int hasTexture;
uniform int hasIBLData;

void main()
{             
    if (hasIBLData == 1) {
        vec3 samplingVec = vec3(WorldPos.x, -WorldPos.y, WorldPos.z); 
        vec3 envColor = texture(environmentMapSampler, samplingVec).rgb;
        FragColor = vec4(envColor, 1.0);
    } else {
        // Render 3D model skybox
        if(hasTexture == 1) {
            FragColor = texture(textSampler, outTextCoord);
        } else {
            FragColor = diffuse;
        }
    }
}