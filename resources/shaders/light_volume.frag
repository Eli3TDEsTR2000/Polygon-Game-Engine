#version 410 core

const float PI = 3.14159265359;

const float SPECULAR_POWER = 10;

const int DEBUG_LIGHT_VOLUME = 0;

struct Attenuation {
    float constant;
    float linear;
    float exponent;
};

struct PointLight {
    vec3 color;
    float intensity;
    vec3 position_view;
    Attenuation attenuation;
};

struct SpotLight {
    vec3 color;
    float intensity;
    vec3 position_view;
    Attenuation attenuation;
    vec3 coneDirection_view;
    float cutOff;
};

// G-Buffer samplers
uniform sampler2D albedoSampler;
uniform sampler2D normalSampler;
uniform sampler2D materialSampler;
uniform sampler2D depthSampler;

uniform vec2 screenSize;
uniform mat4 invProjectionMatrix;

uniform int lightType;
uniform PointLight pointLight;
uniform SpotLight spotLight;

// Vertex position in view space
in vec3 FragPos_view;

// Output Additive blending is handled by OpenGL state in the renderLightVolumes() method.
out vec4 FragColor;

// Reconstruct View Position from Depth Buffer
vec3 reconstructViewPos(float depth, vec2 texCoords) {
    float z = depth * 2.0 - 1.0;
    vec2 clipXY = texCoords * 2.0 - 1.0;
    vec4 clipPos = vec4(clipXY, z, 1.0);
    vec4 viewPos_w = invProjectionMatrix * clipPos;
    return viewPos_w.xyz / viewPos_w.w;
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
    return nom / max(denom, 0.0000001);
}

// GeometrySchlickGGX used in correlation with the GeometrySmith function
// GeometrySchlickGGX = N.X / (N.X) (1 - k) + k
// X represents the L or V vectors.
// k represents roughness / 2. the old (roughness + 1.0)^2 was used by Unreal Engine but they reverted back to the
// roughness / 2
float GeometrySchlickGGX(float NdotV, float roughness) {
    //    float r = (roughness + 1.0);
    //    float k = (r * r) / 8.0;
    float k = roughness / 2.0;
    float nom   = NdotV;
    float denom = NdotV * (1.0 - k) + k;
    return nom / max(denom, 0.0000001);
}

// Geometry shadowing function is a combination of the smith model and the schlick model
// Gsmith = G(NdotV) * G(NdotL) where G is the GeometrySchlickGGX function
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
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

// Shared PBR Calculation for Point/Spot Lights
vec3 calcPBRContrib(vec3 lightColor, float lightIntensity, vec3 lightPos_view,
                    vec3 fragPos_view, vec3 N, vec3 V,
                    vec3 albedo, float metallic, float roughness)
{
    // calculating the L vector and the H half-way vector between the view vector and the light vector.
    vec3 L = normalize(lightPos_view - fragPos_view);
    vec3 H = normalize(V + L);

    // Pre-calculated the dot products of the needed vectors because of their frequent usage.
    // Avoid the negative dot products because they represents Vectors that are out of the BRDF hemisphere.
    float NdotL = max(dot(N, L), 0.0);
    if (NdotL <= 0.0) {
        return vec3(0.0);
    }
    float NdotV = max(dot(N, V), 0.0);
    float HdotV = max(dot(H, V), 0.0);

    // Calculate base reflectivity F0 for Fresnel
    // For dielectrics, F0 is usually vec3(0.04). For metals, it's the albedo color.
    vec3 F0 = vec3(0.04);
    F0 = mix(F0, albedo, metallic);

    // Cook-Torrance terms
    float D = DistributionGGX(N, H, roughness);
    float G = GeometrySmith(N, V, L, roughness);
    vec3 F = fresnelSchlick(HdotV, F0);

    // Specular BRDF component (Cook-Torrance)
    // Cook-Torrance equation (specular) = D*G*F / 4(V.N)(L.N)
    // D represents the Normal Distribution function
    // G represents the Geometry Shadowing function
    // F represents the Fresnel Function
    vec3 numerator = D * G * F;
    float denominator = 4.0 * NdotV * NdotL + 0.001;
    vec3 specular = numerator / denominator;

    // Diffuse BRDF component (Lambertian)
    // BRDF = kD.fdiffuse + kS.fspecular
    // kS is the factor of specular contribution and kD is the factor of diffuse contribution for the light.
    // PBR should conserve energy so kD + kS should add up to 1
    // We get the specular factor from the Fresnel function
    // kS's only usage is to calculate the kD since we are using the Frensel function in the Cook-Torrance function.
    vec3 kS = F;
    vec3 kD = vec3(1.0) - kS;
    kD *= (1.0 - metallic);
    vec3 diffuse = kD * albedo / PI;

    // Combine and modulate by light color/intensity and angle
    vec3 radiance = lightColor * lightIntensity;
    return (diffuse + specular) * radiance * NdotL;
}

// Calculate Light Attenuation
float calcAttenuation(vec3 lightPos_view, vec3 fragPos_view, Attenuation att)
{
    float distance = length(lightPos_view - fragPos_view);
    float attenuation = att.constant + att.linear * distance + att.exponent * distance * distance;
    // Avoid division by zero, return 0 contribution if attenuation is non-positive
    return (attenuation > 0.0) ? (1.0 / attenuation) : 0.0;
}

void main()
{
    // Calculate texture coordinates from fragment's screen position
    vec2 texCoords = gl_FragCoord.xy / screenSize;

    // Sample G-Buffer
    vec4 albedoColor = texture(albedoSampler, texCoords);
    vec3 normal_encoded = texture(normalSampler, texCoords).rgb;
    vec3 materialProps = texture(materialSampler, texCoords).rgb;
    float depth = texture(depthSampler, texCoords).r;

    // Discard fragments beyond the far plane (skybox/background)
    if (depth >= 1.0) {
        discard;
    }

    // Decode G-Buffer data
    vec3 albedo = albedoColor.rgb;
    // float alpha = albedoColor.a; // Alpha not usually needed for lighting
    vec3 N = normalize(normal_encoded * 2.0 - 1.0); // View-space normal
    float metallic = materialProps.r;
    float roughness = materialProps.g;
    // float ao = materialProps.b; // AO typically applied to ambient, ignore for direct lights

    // Reconstruct fragment's position in view space
    vec3 fragPos_vs = reconstructViewPos(depth, texCoords);

    // Calculate View vector
    vec3 V = normalize(-fragPos_vs);

    vec3 lightResult = vec3(0.0);

    // Calculate lighting based on light type
    if (lightType == 0) // Point Light
    {
        lightResult = calcPBRContrib(pointLight.color, pointLight.intensity, pointLight.position_view,
                                     fragPos_vs, N, V, albedo, metallic, roughness);
        float attenuation = calcAttenuation(pointLight.position_view, fragPos_vs, pointLight.attenuation);
        lightResult *= attenuation;
    }
    else // Spot Light (Type == 1)
    {
        vec3 lightDir_view = normalize(spotLight.position_view - fragPos_vs);
        float spotFactor = dot(-lightDir_view, normalize(spotLight.coneDirection_view));

        // Check if fragment is inside the spotlight cone
        if (spotFactor > spotLight.cutOff) {
             lightResult = calcPBRContrib(spotLight.color, spotLight.intensity, spotLight.position_view,
                                     fragPos_vs, N, V, albedo, metallic, roughness);
            float attenuation = calcAttenuation(spotLight.position_view, fragPos_vs, spotLight.attenuation);
            // Add smooth falloff at the edge of the cone
            float spotEffect = smoothstep(spotLight.cutOff, mix(spotLight.cutOff, 1.0, 0.05), spotFactor);
            lightResult *= attenuation * spotEffect;
        }
    }

    if(DEBUG_LIGHT_VOLUME > 0) {
        vec4 test;
        vec2 texCoords = gl_FragCoord.xy / screenSize;

        float depth = texture(depthSampler, texCoords).r;
        if (depth >= 1.0) {
            discard;
        }

        FragColor = vec4(1.0, 0.0, 1.0, 1.0);
        test = vec4(lightResult, 1.0);
        if(test == vec4(lightResult, 1.0)) {
            return;
        }
    }

    FragColor = vec4(lightResult, 1.0);
} 