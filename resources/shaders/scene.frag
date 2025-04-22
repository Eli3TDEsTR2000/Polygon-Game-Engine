#version 410

in vec4 outViewPosition;
in vec4 outWorldPosition;
in vec3 outNormal;
in vec3 outTangent;
in vec3 outBitangent;
in vec2 outTextCoord;

layout (location = 0) out vec4 buffAlbedo;
layout (location = 1) out vec4 buffNormal;
layout (location = 2) out vec4 buffSpecular;

struct Material {
    vec4 diffuse;
    vec4 specular;
    float reflectance;
    int hasNormalMap;
};

uniform sampler2D textSampler;
uniform sampler2D normalSampler;
uniform Material material;

vec3 calcTangentSpaceNormal(vec3 tangent, vec3 bitangent, vec3 normal, vec2 textCoord) {
    mat3 TBN = mat3(tangent, bitangent, normal);
    vec3 localNormal = texture(normalSampler, textCoord).rgb;
    localNormal = normalize(localNormal * 2.0 - 1.0);
    localNormal = normalize(TBN * localNormal);

    return localNormal;
}

void main()
{
    vec4 text_color = texture(textSampler, outTextCoord);
    vec4 diffuse = text_color + material.diffuse;
    if(diffuse.a < 0.5) {
        discard;
    }
    vec4 specular = text_color + material.specular;

    vec3 normal = outNormal;
    if(material.hasNormalMap > 0) {
        normal = calcTangentSpaceNormal(outTangent, outBitangent, outNormal, outTextCoord);
    }

    buffAlbedo = vec4(diffuse.xyz, material.reflectance);
    buffNormal = vec4(0.5 * normal + 0.5, 1.0);
    buffSpecular = specular;
}