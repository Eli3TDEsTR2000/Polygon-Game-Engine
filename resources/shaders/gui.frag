#version 410

in vec2 outTexCoords;
in vec4 outColor;

out vec4 color;

uniform sampler2D textSampler;

void main() {
    color = outColor * texture(textSampler, outTexCoords);
}