#version 410 core

// Based on FXAA 3.11 by Timothy Lottes

// Input texture (the rendered scene)
uniform sampler2D sceneSampler;

// Inverse of screen resolution (1.0 / width, 1.0 / height)
uniform vec2 inverseScreenSize;

// Input from vertex shader
in vec2 vTexCoord;

// Output color
out vec4 FragColor;

// Tunable settings
#define FXAA_REDUCE_MIN   0.0078125  // (1.0/128.0)
#define FXAA_REDUCE_MUL   0.125      // (1.0/8.0)
#define FXAA_SPAN_MAX     8.0

vec3 FxaaPixelShader(vec2 posPos, sampler2D tex, vec2 rcpFrame) {
    vec3 rgbNW = textureLod(tex, posPos.xy + (vec2(-1.0,-1.0) * rcpFrame.xy), 0.0).xyz;
    vec3 rgbNE = textureLod(tex, posPos.xy + (vec2( 1.0,-1.0) * rcpFrame.xy), 0.0).xyz;
    vec3 rgbSW = textureLod(tex, posPos.xy + (vec2(-1.0, 1.0) * rcpFrame.xy), 0.0).xyz;
    vec3 rgbSE = textureLod(tex, posPos.xy + (vec2( 1.0, 1.0) * rcpFrame.xy), 0.0).xyz;
    vec3 rgbM  = textureLod(tex, posPos.xy, 0.0).xyz;

    vec3 luma = vec3(0.299, 0.587, 0.114);
    float lumaNW = dot(rgbNW, luma);
    float lumaNE = dot(rgbNE, luma);
    float lumaSW = dot(rgbSW, luma);
    float lumaSE = dot(rgbSE, luma);
    float lumaM  = dot(rgbM,  luma);

    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

    vec2 dir;
    dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
    dir.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));

    float dirReduce = max(
        (lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * FXAA_REDUCE_MUL),
        FXAA_REDUCE_MIN);

    float rcpDirMin = 1.0 / (min(abs(dir.x), abs(dir.y)) + dirReduce);
    dir = min(vec2(FXAA_SPAN_MAX, FXAA_SPAN_MAX),
          max(vec2(-FXAA_SPAN_MAX, -FXAA_SPAN_MAX),
          dir * rcpDirMin)) * rcpFrame.xy;

    vec3 rgbA = (1.0/2.0) * (
        textureLod(tex, posPos.xy + dir * (1.0/3.0 - 0.5), 0.0).xyz +
        textureLod(tex, posPos.xy + dir * (2.0/3.0 - 0.5), 0.0).xyz);
    vec3 rgbB = rgbA * (1.0/2.0) + (1.0/4.0) * (
        textureLod(tex, posPos.xy + dir * (0.0/3.0 - 0.5), 0.0).xyz +
        textureLod(tex, posPos.xy + dir * (3.0/3.0 - 0.5), 0.0).xyz);
    float lumaB = dot(rgbB, luma);

    if((lumaB < lumaMin) || (lumaB > lumaMax)) return rgbA;
    return rgbB;
}

void main() {
    vec3 result = FxaaPixelShader(vTexCoord, sceneSampler, inverseScreenSize);
    FragColor = vec4(result, 1.0);
} 