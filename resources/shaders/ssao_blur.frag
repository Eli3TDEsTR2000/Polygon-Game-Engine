#version 410 core

in vec2 outTextCoord;

out float fragColor;

uniform sampler2D ssaoSampler;

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(ssaoSampler, 0));
    float result = 0.0;

    for(int x = -2; x < 2; ++x) {
        for(int y = -2; y < 2; ++y) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            result += texture(ssaoSampler, outTextCoord + offset).r;
        }
    }

    fragColor = result / 16.0; // 4x4 = 16; matched to the noise tile size
}