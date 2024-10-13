#version 410

in vec2 outTextCoord;

out vec4 fragColor;

uniform sampler2D textSampler;

void main()
{
    fragColor = texture(textSampler, outTextCoord);
}