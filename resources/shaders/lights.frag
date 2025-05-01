#version 410 core

const float PI = 3.14159265359;

const float SPECULAR_POWER = 10;

const int NUM_CASCADES = 3;
const float BIAS = 0.0005;

in vec2 outTextCoord;

out vec4 fragColor;

struct CascadeShadow {
    mat4 projViewMatrix;
    float splitDistance;
};

struct AmbientLight {
    vec3 color;
    float intensity;
};

struct DirectionalLight {
    vec3 color;
    float intensity;
    vec3 direction;
};

struct Fog {
    int activeFog;
    vec3 color;
    float density;
};

uniform int bypassLighting;

uniform sampler2D albedoSampler;
uniform sampler2D normalSampler;
uniform sampler2D materialSampler;
uniform sampler2D emissiveSampler;
uniform sampler2D depthSampler;

uniform mat4 invProjectionMatrix;
uniform mat4 invViewMatrix;
uniform mat4 viewMatrix;

uniform AmbientLight ambientLight;
uniform DirectionalLight directionalLight;

uniform Fog fog;

uniform CascadeShadow cascadeshadows[NUM_CASCADES];
uniform sampler2D shadowMap[NUM_CASCADES];

uniform sampler2D brdfLUT;
uniform samplerCube irradianceMap;
uniform samplerCube prefilterMap;
uniform int hasIBL;

float textureProj(vec4 shadowCoord, vec2 offset, int idx) {
    float shadow = 1.0;

    if (shadowCoord.z > -1.0 && shadowCoord.z < 1.0) {
        float dist = 0.0;
        float texelSize = 1.0 / textureSize(shadowMap[idx], 0).x;
        // PCF with 3x3 kernel
        float shadowCount = 0.0;
        for(int x = -1; x <= 1; x++) {
            for(int y = -1; y <= 1; y++) {
                vec2 offset = vec2(x, y) * texelSize;
                float pcfDepth = texture(shadowMap[idx], vec2(shadowCoord.xy + offset)).r;
                if (shadowCoord.w > 0 && pcfDepth < shadowCoord.z - BIAS) {
                    shadowCount += 1.0;
                }
            }
        }

        float shadowFactor = shadowCount / 9.0;
        shadow = mix(1.0, 0.3, shadowFactor);
    }
    return shadow;
}

float calcShadow(vec4 worldPosition, int idx) {
    vec4 shadowMapPosition = cascadeshadows[idx].projViewMatrix * worldPosition;
    vec4 shadowCoord = (shadowMapPosition / shadowMapPosition.w) * 0.5 + 0.5;
    return textureProj(shadowCoord, vec2(0, 0), idx);
}

vec4 calcFog(vec3 view_pos, vec4 color, Fog fog) {
    float distance = length(view_pos);
    float fogFactor = 1.0 / exp((distance * fog.density) * (distance * fog.density));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    vec3 resultColor = mix(fog.color, color.rgb, fogFactor);
    return vec4(resultColor, color.a);
}

// Normal Distribution Function (Trowbridge-Reitz GGX)
// D = a^2 / PI((N.H)^2(a^2 - 1) + 1)^2
// a represents the roughtness of the material ^ 2.
float DistributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;

    float nom   = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return nom / max(denom, 0.0000001); // Prevent divide by zero
}

// GeometrySchlickGGX used in correlation with the GeometrySmith function
// GeometrySchlickGGX = N.X / (N.X) (1 - k) + k
// X represents the L or V vectors.
// k represents roughness / 2. the old (roughness + 1.0)^2 was used by Unreal Engine but they reverted back to the
// roughness / 2
float GeometrySchlickGGX(float NdotX, float roughness) {
//    float r = (roughness + 1.0);
//    float k = (r * r) / 8.0;
    float k = roughness / 2.0;
    float nom   = NdotX;
    float denom = NdotX * (1.0 - k) + k;
    return nom / max(denom, 0.0000001); // Prevent divide by zero
}
// Geometry shadowing function is a combination of the smith model and the schlick model
// Gsmith = G(NdotV) * G(NdotL) where G is the GeometrySchlickGGX function
float GeometrySmith(float NdotV, float NdotL, float roughness) {
    float ggx2 = GeometrySchlickGGX(NdotV, roughness);
    float ggx1 = GeometrySchlickGGX(NdotL, roughness);
    return ggx1 * ggx2;
}

// Fresnel Schlick Function.
// F0 represents the base reflectivity of the material.
// F = F0 + (1 - F0) (1 - (V.H))^5.
// V is the view vector and H is the half-way vector (half-way between the view V and light L vector).
// Since V vector and H vector are normalized the dot product will be cos the angle between them.
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// Fresnel function using Schlick's approximation but using roughness
vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness) {
    return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

void main() {
    // Sample G-Buffer
    vec4 albedoColor = texture(albedoSampler, outTextCoord);
    vec3 normal_encoded = texture(normalSampler, outTextCoord).rgb;
    vec3 materialProps = texture(materialSampler, outTextCoord).rgb;
    vec3 emissiveColor = texture(emissiveSampler, outTextCoord).rgb; // Added back
    float depth = texture(depthSampler, outTextCoord).r;

    // Skip skybox or background fragments
    if (depth == 1.0) {
        discard;
    }

    // Decode G-Buffer data
    vec3 albedo = albedoColor.rgb;
    float alpha = albedoColor.a;
    vec3 N = normalize(normal_encoded * 2.0 - 1.0);
    float metallic = materialProps.r;
    float roughness = materialProps.g;
    float ao = materialProps.b;

    // Reconstruct view-space position from depth
    float depth_vs = depth * 2.0 - 1.0;
    vec4 clip_pos = vec4(outTextCoord * 2.0 - 1.0, depth_vs, 1.0);
    vec4 view_pos_h = invProjectionMatrix * clip_pos;
    vec3 Vpos = view_pos_h.xyz / view_pos_h.w;

    // Reconstruct world-space position for shadows
    vec4 world_pos = invViewMatrix * vec4(Vpos, 1.0);

    // Calculate view direction (points from surface to camera)
    vec3 V = normalize(-Vpos);

    // Calculate base reflectivity F0 for Fresnel
    // For dielectrics, F0 is usually vec3(0.04). For metals, it's the albedo color.
    vec3 F0 = vec3(0.04);
    F0 = mix(F0, albedo, metallic);

    float NdotV = max(dot(N, V), 0.0);

    // Diffuse BRDF component (Lambertian)
    // BRDF = kD.fdiffuse + kS.fspecular
    // kS is the factor of specular contribution and kD is the factor of diffuse contribution for the light.
    // PBR should conserve energy so kD + kS should add up to 1
    // We get the specular factor from the Fresnel function
    // kS's only usage is to calculate the kD since we are using the Frensel function in the Cook-Torrance function.
    vec3 F_IBL = fresnelSchlickRoughness(NdotV, F0, roughness);
    vec3 kS = F_IBL;
    vec3 kD = vec3(1.0) - kS;
    kD *= (1.0 - metallic); // Metals have no diffuse reflection

    // Lo represents the final light (outgoing radiance).
    vec3 Lo = vec3(0.0);

    // Diffuse IBL
    vec3 ambient;
    if (hasIBL == 1) {
        mat3 normalMatrix = transpose(mat3(viewMatrix));
        vec3 N_world = normalize(normalMatrix * N);
        N_world.g = -N_world.g;

        // Sample irradiance map
        vec3 irradiance = texture(irradianceMap, N_world).rgb;
        vec3 diffuseIBL = irradiance * kD * albedo * ao;

        vec3 R = reflect(-V, N); // View space reflection vector
        vec3 R_world = normalize(normalMatrix * R);
        R_world.g = -R_world.g;

        // Sample prefilter map
        const float MAX_REFLECTION_LOD = 4.0;
        vec3 prefilteredColor = textureLod(prefilterMap, R_world,  roughness * MAX_REFLECTION_LOD).rgb;
        vec2 brdf  = texture(brdfLUT, vec2(NdotV, roughness)).rg;
        vec3 specularIBL = prefilteredColor * (F_IBL * brdf.r + brdf.g);

        Lo += diffuseIBL + specularIBL;

    } else {
        // Fallback to simple ambient light if IBL is not active
        ambient = ambientLight.color * ambientLight.intensity * albedo * ao;
        Lo += ambient; // Add fallback ambient
    }

    // bypass base light rendering.
    if (bypassLighting > 0) {
        fragColor = vec4(albedo, 1.0);
        return;
    }

    // calculating the L vector and the H half-way vector between the view vector and the light vector.
    vec3 L = normalize(directionalLight.direction);
    vec3 H = normalize(V + L);
    vec3 radiance = directionalLight.color * directionalLight.intensity;

    // Pre-calculated the dot products of the needed vectors because of their frequent usage.
    // Avoid the negative dot products because they represents Vectors that are out of the BRDF hemisphere.
    float NdotL = max(dot(N, L), 0.0);
    float HdotV = max(dot(H, V), 0.0);

    // Calculate Cook-Torrence terms
    float D = DistributionGGX(N, H, roughness);
    float G = GeometrySmith(NdotV, NdotL, roughness);
    vec3 F = fresnelSchlick(HdotV, F0);

    // Specular BRDF component (Cook-Torrance)
    // Cook-Torrance equation (specular) = D*G*F / 4(V.N)(L.N)
    // D represents the Normal Distribution function
    // G represents the Geometry Shadowing function
    // F represents the Fresnel Function
    vec3 numerator = D * G * F;
    float denominator = 4.0 * NdotV * NdotL + 0.001;
    vec3 specularDirect = numerator / denominator;

    // diffuse color is the kD factor * fLambert (Lambertian model for diffuse)
    // fLambert = color / PI * (L dot N)
    // (L dot N) when the light angle is perpendicular to the normal of the surface, the diffuse color is at max.
    // but since (L dot N) is calculated in the final Lo, it's removed from the lambertian equation.
    vec3 diffuseDirect = kD * albedo / PI;

    // Calculate Shadow Factor
    int cascadeIndex = 0;
    for (int i=0; i<NUM_CASCADES - 1; i++) {
        if (Vpos.z < cascadeshadows[i].splitDistance) {
            cascadeIndex = i + 1;
        }
    }
    float shadow = calcShadow(world_pos, cascadeIndex);

    // Add directional light contribution with shadow.
    Lo += (diffuseDirect + specularDirect) * radiance * NdotL * shadow;

    // Add Emissive component
    Lo += emissiveColor;

    // Apply Fog (if active)
    vec4 finalColor = vec4(Lo, alpha);
    if (fog.activeFog == 1) {
        finalColor = calcFog(Vpos, finalColor, fog);
    }

    fragColor = finalColor;
}