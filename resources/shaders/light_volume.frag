#version 410 core

const float SPECULAR_POWER = 10;

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
uniform sampler2D specularSampler;
uniform sampler2D depthSampler;

uniform vec2 screenSize;

uniform mat4 invProjectionMatrix;

uniform int lightType; // 0 = Point, 1 = Spot
uniform PointLight pointLight;
uniform SpotLight spotLight;

in vec3 FragPos_view;

// Output
out vec4 FragColor;


vec3 reconstructViewPos(float depth, vec2 texCoords) {
    // Depth from G-Buffer is in [0, 1] range
    // Convert depth to clip space Z [-1, 1]
    float z = depth * 2.0 - 1.0;

    // Convert texture coordinates to clip space XY [-1, 1]
    vec2 clipXY = texCoords * 2.0 - 1.0;

    // Create clip space position
    vec4 clipPos = vec4(clipXY, z, 1.0);

    // Unproject to view space
    vec4 viewPos_w = invProjectionMatrix * clipPos;
    return viewPos_w.xyz / viewPos_w.w; // Perspective divide
}

vec4 calcLightColor(vec4 diffuse, vec4 specular, float reflectance, vec3 lightColor, float light_intensity,
                    vec3 fragPos_view, vec3 to_light_dir_view, vec3 normal_view)
{
    vec4 diffuseColor = vec4(0);
    vec4 specColor = vec4(0);

    // Diffuse Light
    float diffuseFactor = max(dot(normal_view, to_light_dir_view), 0.0);
    diffuseColor = diffuse * vec4(lightColor, 1.0) * light_intensity * diffuseFactor;

    // Specular Light
    vec3 camera_direction_view = normalize(-fragPos_view);
    vec3 from_light_dir_view = -to_light_dir_view;
    vec3 reflected_light_view = normalize(reflect(from_light_dir_view, normal_view));
    float specularFactor = max(dot(camera_direction_view, reflected_light_view), 0.0);
    if (diffuseFactor > 0.0)
    {
         specularFactor = pow(specularFactor, SPECULAR_POWER);
         specColor = specular * light_intensity * specularFactor * reflectance * vec4(lightColor, 1.0);
    }


    return diffuseColor + specColor;
}

vec4 calcPointLightContrib(vec4 diffuse, vec4 specular, float reflectance, PointLight light, vec3 fragPos_view, vec3 normal_view)
{
    vec3 light_direction_view = light.position_view - fragPos_view;
    float distance = length(light_direction_view);
    vec3 to_light_dir_view = normalize(light_direction_view);

    vec4 light_color = calcLightColor(diffuse, specular, reflectance, light.color, light.intensity, fragPos_view, to_light_dir_view, normal_view);

    // Apply Attenuation
    float attenuationInv = light.attenuation.constant + light.attenuation.linear * distance +
                           light.attenuation.exponent * distance * distance;

    // Avoid division by zero or negative attenuation
    if (attenuationInv <= 0.0) {
        return vec4(0.0);
    }

    return light_color / attenuationInv;
}

vec4 calcSpotLightContrib(vec4 diffuse, vec4 specular, float reflectance, SpotLight light, vec3 fragPos_view, vec3 normal_view)
{
    vec3 light_direction_view = light.position_view - fragPos_view;
    vec3 to_light_dir_view = normalize(light_direction_view);
    vec3 from_light_dir_view = -to_light_dir_view;

    float spot_alfa = dot(from_light_dir_view, normalize(light.coneDirection_view));

    vec4 color = vec4(0);

    if (spot_alfa > light.cutOff) {
        float distance = length(light_direction_view);
        float attenuationInv = light.attenuation.constant + light.attenuation.linear * distance +
                               light.attenuation.exponent * distance * distance;

        // Avoid division by zero or negative attenuation
        if (attenuationInv <= 0.0) {
            return vec4(0.0);
        }

        vec4 light_color = calcLightColor(diffuse, specular, reflectance, light.color, light.intensity, fragPos_view, to_light_dir_view, normal_view);
        color = light_color / attenuationInv;

        float spotEffect = smoothstep(light.cutOff, light.cutOff + 0.05, spot_alfa);
        color *= spotEffect;
    }

    return color;
}


void main()
{
    // Calculate texture coordinates from fragment's screen position
    vec2 texCoords = gl_FragCoord.xy / screenSize;

    // Sample G-Buffer
    vec4 albedoSamplerValue = texture(albedoSampler, texCoords);
    vec3 albedo = albedoSamplerValue.rgb;
    float reflectance = albedoSamplerValue.a;
    vec3 normal_view = normalize(texture(normalSampler, texCoords).rgb * 2.0 - 1.0);
    vec4 specular = texture(specularSampler, texCoords);
    float depth = texture(depthSampler, texCoords).r;

    // Discard fragments beyond the far plane or exactly at it (skybox/background)
    if (depth >= 1.0) {
        discard;
    }

    // Reconstruct fragment's position in view space
    vec3 fragPos_view_reconstructed = reconstructViewPos(depth, texCoords);

    vec4 diffuse = vec4(albedo, 1.0);
    vec4 lightContribution = vec4(0.0);

    // Calculate lighting based on light type
    if (lightType == 0) // Point Light
    {
        lightContribution = calcPointLightContrib(diffuse, specular, reflectance, pointLight
        , fragPos_view_reconstructed, normal_view);
    }
    else // Spot Light
    {
        lightContribution = calcSpotLightContrib(diffuse, specular, reflectance, spotLight
        , fragPos_view_reconstructed, normal_view);
    }

    FragColor = lightContribution;
} 