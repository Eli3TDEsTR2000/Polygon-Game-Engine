#version 410 core

in vec3 FragPos_world;

uniform vec3 lightPos;
uniform float farPlane;

void main() {
    float lightDistance = length(FragPos_world - lightPos);
    lightDistance = lightDistance / farPlane;

    gl_FragDepth = lightDistance;
}