#version 410 core

in vec2 outTextCoord;

out vec4 fragColor;

uniform sampler2D textSampler;
uniform vec4 diffuse;
uniform int hasTexture;

void main() {
    if(hasTexture == 1) {
        fragColor = texture(textSampler, outTextCoord);
    } else {
        fragColor = diffuse;
    }
}