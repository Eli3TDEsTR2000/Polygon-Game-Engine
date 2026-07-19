#version 410 core

in vec2 outTextCoord;

out float fragColor;

const int KERNEL_SIZE = 32;

uniform sampler2D depthSampler;
uniform sampler2D normalSampler;
uniform sampler2D noiseSampler;

uniform vec3 samples[KERNEL_SIZE];

uniform mat4 projectionMatrix;
uniform mat4 invProjectionMatrix;

uniform vec2 noiseScale;
uniform float radius;
uniform float bias;

void main() {
    float depth = texture(depthSampler, outTextCoord).r;
    if(depth == 1.0) {
        fragColor = 1.0;
        return;
    }

    // Reconstruction of view space positions
    float depth_vs = depth * 2.0 - 1.0;
    vec4 clip_pos = vec4(outTextCoord * 2.0 - 1.0, depth_vs, 1.0);
    vec4 view_pos_h = invProjectionMatrix * clip_pos;
    vec3 Vpos = view_pos_h.xyz / view_pos_h.w;

    vec3 N = normalize(texture(normalSampler, outTextCoord).rgb * 2.0 - 1.0);
    vec3 randomVec = texture(noiseSampler, outTextCoord * noiseScale).xyz;

    vec3 tangent = normalize(randomVec - N * dot(randomVec, N));
    vec3 bitangent = cross(N, tangent);
    mat3 TBN = mat3(tangent, bitangent, N);

    float occlusion = 0.0;
    for(int i = 0; i < KERNEL_SIZE; i++) {
        vec3 samplePos = TBN * samples[i];
        samplePos = Vpos + samplePos * radius;

        vec4 offset = vec4(samplePos, 1.0);
        offset = projectionMatrix * offset;
        offset.xyz /= offset.w;
        offset.xyz = offset.xyz * 0.5 + 0.5;

        float sampleDepthRaw = texture(depthSampler, offset.xy).r;
        float sampleDepth_vs = sampleDepthRaw * 2.0 - 1.0;
        vec4 sampleClip = vec4(offset.xy * 2.0 - 1.0, sampleDepth_vs, 1.0);
        vec4 sampleView_h = invProjectionMatrix * sampleClip;
        float sampleDepthView = (sampleView_h.xyz / sampleView_h.w).z;

        float rangeCheck = smoothstep(0.0, 1.0, radius / abs(Vpos.z - sampleDepthView));
        occlusion += (sampleDepthView >= samplePos.z + bias ? 1.0 : 0.0) * rangeCheck;
    }

    occlusion = 1.0 - (occlusion / float(KERNEL_SIZE));
    fragColor = occlusion;
}