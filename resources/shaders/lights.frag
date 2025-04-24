#version 410 core

const float SPECULAR_POWER = 10;

const int NUM_CASCADES = 3;
const float BIAS = 0.0005;
const float SHADOW_FACTOR = 0.25;

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
uniform sampler2D specularSampler;
uniform sampler2D depthSampler;

uniform mat4 invProjectionMatrix;
uniform mat4 invViewMatrix;

uniform AmbientLight ambientLight;
uniform DirectionalLight directionalLight;

uniform Fog fog;

uniform CascadeShadow cascadeshadows[NUM_CASCADES];
uniform sampler2D shadowMap[NUM_CASCADES];

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

        // Calculate final shadow with minimum value to prevent too dark shadows
        // 9 samples for 3x3 kernel
        float shadowFactor = shadowCount / 9.0;
        shadow = mix(1.0, 0.5, shadowFactor);
    }
    return shadow;
}

float calcShadow(vec4 worldPosition, int idx) {
    vec4 shadowMapPosition = cascadeshadows[idx].projViewMatrix * worldPosition;
    float shadow = 1.0;
    vec4 shadowCoord = (shadowMapPosition / shadowMapPosition.w) * 0.5 + 0.5;
    shadow = textureProj(shadowCoord, vec2(0, 0), idx);
    return shadow;
}

vec4 calcFog(vec3 position, vec4 color, Fog fog, vec3 ambientLightColor, DirectionalLight directionalLight) {
    vec3 fogColor = fog.color * (ambientLightColor + directionalLight.color * directionalLight.intensity);
    float distance = length(position);
    float fogFactor = 1.0 / exp((distance * fog.density) * (distance * fog.density));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    vec3 resultColor = mix(fogColor, color.xyz, fogFactor);
    return vec4(resultColor.xyz, color.w);
}

vec4 calcAmbient(AmbientLight ambientLight, vec4 ambient) {
    return vec4(ambientLight.intensity * ambientLight.color, 1) * ambient;
}

vec4 calcLightColor(vec4 diffuse, vec4 specular, float reflectance, vec3 lightColor, float light_intensity
, vec3 position, vec3 to_light_dir, vec3 normal) {
    vec4 diffuseColor = vec4(0, 0, 0, 1);
    vec4 specColor = vec4(0, 0, 0, 1);

    // Diffuse Light
    float diffuseFactor = max(dot(normal, to_light_dir), 0.0);
    diffuseColor = diffuse * vec4(lightColor, 1.0) * light_intensity * diffuseFactor;

    // Specular Light
    vec3 camera_direction = normalize(-position);
    vec3 from_light_dir = -to_light_dir;
    vec3 reflected_light = normalize(reflect(from_light_dir, normal));
    float specularFactor = max(dot(camera_direction, reflected_light), 0.0);
    specularFactor = pow(specularFactor, SPECULAR_POWER);
    specColor = specular * light_intensity  * specularFactor * reflectance * vec4(lightColor, 1.0);

    return (diffuseColor + specColor);
}

vec4 calcDirLight(vec4 diffuse, vec4 specular, float reflectance, DirectionalLight light, vec3 position, vec3 normal) {
    return calcLightColor(diffuse, specular, reflectance, light.color, light.intensity, position
    , normalize(light.direction), normal);
}

void main() {
    vec4 albedoSamplerValue = texture(albedoSampler, outTextCoord);
    vec3 albedo  = albedoSamplerValue.rgb;
    vec4 diffuse = vec4(albedo, 1);

    if(bypassLighting > 0) {
        fragColor = diffuse;
        return;
    }

    float reflectance = albedoSamplerValue.a;
    vec3 normal = normalize(2.0 * texture(normalSampler, outTextCoord).rgb  - 1.0);
    vec4 specular = texture(specularSampler, outTextCoord);

    // Retrieve position from depth
    float depth = texture(depthSampler, outTextCoord).x * 2.0 - 1.0;
    if (depth == 1) {
        discard;
    }
    vec4 clip      = vec4(outTextCoord.x * 2.0 - 1.0, outTextCoord.y * 2.0 - 1.0, depth, 1.0);
    vec4 view_w    = invProjectionMatrix * clip;
    vec3 view_pos  = view_w.xyz / view_w.w;
    vec4 world_pos = invViewMatrix * vec4(view_pos, 1);

    vec4 diffuseSpecularComp = calcDirLight(diffuse, specular, reflectance, directionalLight, view_pos, normal);

    int cascadeIndex = 0;
    for (int i=0; i<NUM_CASCADES - 1; i++) {
        if (view_pos.z < cascadeshadows[i].splitDistance) {
            cascadeIndex = i + 1;
        }
    }
    float shadowFactor = calcShadow(world_pos, cascadeIndex);

    vec4 ambient = calcAmbient(ambientLight, diffuse);
    fragColor = ambient + (diffuseSpecularComp * shadowFactor);

    if (fog.activeFog == 1) {
        fragColor = calcFog(view_pos, fragColor, fog, ambientLight.color, directionalLight);
    }
}