#version 410 core

// Based on FXAA 3.11 by Timothy Lottes

// Input texture (the rendered scene)
uniform sampler2D sceneSampler;

// Inverse of screen resolution (1.0 / width, 1.0 / height)
uniform vec2 inverseScreenSize;

// Controls for tone-mapping / gamma correction.
uniform float exposure = 1.0;
uniform float gamma = 2.2;
uniform int enableToneGamma;

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

// ACES Filmic Tone Mapping Curve (Stephen Hill Fit)
// taken from https://github.com/TheRealMJP/BakingLab/blob/master/BakingLab/ACES.hlsl

// sRGB => XYZ => D65_2_D60 => AP1 => RRT_SAT
const mat3 ACESInputMat = mat3(
    vec3(0.59719, 0.07600, 0.02840),
    vec3(0.35458, 0.90834, 0.13383),
    vec3(0.04823, 0.01566, 0.83777)
);

// ODT_SAT => XYZ => D60_2_D65 => sRGB
const mat3 ACESOutputMat = mat3(
    vec3( 1.60475, -0.10208, -0.00327),
    vec3(-0.53108,  1.10813, -0.07276),
    vec3(-0.07367, -0.00605,  1.07602)
);

vec3 RRTAndODTFit(vec3 v)
{
    vec3 a = v * (v + 0.0245786f) - 0.000090537f;
    vec3 b = v * (0.983729f * v + 0.4329510f) + 0.238081f;
    return a / b;
}

// ACES Tone Mapping function
vec3 toneMapACESFitted(vec3 color)
{
    // Linear sRGB input needs conversion for ACES
    color = ACESInputMat * color; 
    
    // Apply RRT and ODT curve
    color = RRTAndODTFit(color);

    // Convert back to sRGB linear
    color = ACESOutputMat * color;

    // Clamp to [0, 1]
    color = clamp(color, 0.0, 1.0); 
    
    return color;
}

// Gamma Correction
vec3 gammaCorrect(vec3 color) {
    color = max(color, vec3(0.0)); 
    return pow(color, vec3(1.0/gamma));
}

void main() {
    vec3 finalResult;

    if(enableToneGamma == 1) {
        // Apply FXAA to the HDR scene texture
        vec3 hdrResult = FxaaPixelShader(vTexCoord, sceneSampler, inverseScreenSize);

        // Apply exposure control
        hdrResult *= exposure;

        // Apply ACES Fitted Tone Mapping 
        vec3 mappedResult = toneMapACESFitted(hdrResult);

        // Apply Gamma Correction for display
        finalResult = gammaCorrect(mappedResult);
    } else {
        finalResult = FxaaPixelShader(vTexCoord, sceneSampler, inverseScreenSize);
    }

    FragColor = vec4(finalResult, 1.0);
} 