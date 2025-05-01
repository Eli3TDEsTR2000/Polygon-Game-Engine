#version 410 core

in vec4 outViewPosition;
in vec3 outNormal;
in vec3 outTangent;
in vec3 outBitangent;
in vec2 outTextCoord;

// GBuffer textures
layout (location = 0) out vec4 out_Albedo;
layout (location = 1) out vec4 out_Normal;
// R: Metallic, G: Roughness, B: AO
layout (location = 2) out vec3 out_Material;
layout (location = 3) out vec3 out_Emissive;

struct Material {
    vec4 diffuseColor;
    float metallic;
    float roughness;
    float aoStrength;
    int hasTexture;
    int hasNormalMap;
    int hasMetallicMap;
    int hasRoughnessMap;
    int hasAoMap;
    int hasEmissiveMap;
};

uniform sampler2D baseColorSampler;
uniform sampler2D normalSampler;
uniform sampler2D metallicSampler;
uniform sampler2D roughnessSampler;
uniform sampler2D aoSampler;
uniform sampler2D emissiveSampler;

uniform Material material;

vec3 calcTangentSpaceNormal(vec3 tangent_vs, vec3 bitangent_vs, vec3 normal_vs, vec2 textCoord) {
    mat3 TBN = mat3(normalize(tangent_vs), normalize(bitangent_vs), normalize(normal_vs));
    vec3 normal_map = texture(normalSampler, textCoord).rgb;
    //map to [-1, 1]
    normal_map = normalize(normal_map * 2.0 - 1.0);
    vec3 normal_view = normalize(TBN * normal_map);
    return normal_view;
}

void main()
{
    // Albedo
    vec4 baseColorTex = vec4(1.0);
    if (material.hasTexture > 0) {
        baseColorTex = texture(baseColorSampler, outTextCoord);
    }
    vec4 albedo = baseColorTex * material.diffuseColor;

    // Basic alpha test (optional, can be handled differently in PBR)
    if(albedo.a < 0.1) {
        discard;
    }

    // Normal
    vec3 finalNormal_vs = normalize(outNormal);
    if(material.hasNormalMap > 0) {
        finalNormal_vs = calcTangentSpaceNormal(outTangent, outBitangent, outNormal, outTextCoord);
    }

    // Metallic
    float metallic = material.metallic;
    if (material.hasMetallicMap > 0) {
        metallic = texture(metallicSampler, outTextCoord).r;
    }

    // Roughness
    float roughness = material.roughness;
    if (material.hasRoughnessMap > 0) {
        roughness = texture(roughnessSampler, outTextCoord).g;
    }

    // Ambient Occlusion
    float ao = 1.0;
    if (material.hasAoMap > 0) {
        ao = texture(aoSampler, outTextCoord).r;
    }
    ao *= material.aoStrength;

    vec3 emissive = vec3(0.0);
    if (material.hasEmissiveMap > 0) {
        emissive = texture(emissiveSampler, outTextCoord).rgb;
    }

    // GBuffer Albedo.
    out_Albedo = albedo;

    // GBuffer Normal encoded to [0, 1] range.
    out_Normal = vec4(0.5 * finalNormal_vs + 0.5, 1.0);

    // GBuffer Material Properties (Metallic, Roughness, AO)
    out_Material = vec3(metallic, roughness, clamp(ao, 0.0, 1.0));

    // GBuffer emissive
    out_Emissive = emissive;
}