#version 410

in vec2 outTextCoord;

out vec4 fragColor;

struct Material {
    vec4 diffuse;
};

uniform sampler2D textSampler;
uniform Material material;

void main()
{
    fragColor = texture(textSampler, outTextCoord) + material.diffuse;
}