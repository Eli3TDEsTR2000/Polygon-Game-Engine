#version 410

const int MAX_POINT_LIGHT = 5;
const int MAX_SPOT_LIGHT = 5;
const float SPECULAR_POWER = 10;

in vec3 outPosition;
in vec3 outNormal;
in vec2 outTextCoord;

out vec4 fragColor;

struct Attenuation {
    float constant;
    float linear;
    float exponent;
};

struct Material {
    vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    float reflectance;
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

struct PointLight {
    vec3 color;
    float intensity;
    vec3 position;
    Attenuation attenuation;
};

struct SpotLight {
    vec3 color;
    float intensity;
    vec3 position;
    vec3 coneDirection;
    float cutOff;
    Attenuation attenuation;
};

struct Fog {
    int activeFog;
    vec3 color;
    float density;
};

uniform sampler2D textSampler;
uniform Material material;
uniform AmbientLight ambientLight;
uniform DirectionalLight directionalLight;
uniform PointLight pointLights[MAX_POINT_LIGHT];
uniform SpotLight spotLights[MAX_SPOT_LIGHT];
uniform Fog fog;
uniform int bypassLighting;

vec4 calcFog(vec3 position, vec4 color, Fog fog, vec3 ambientLightColor, DirectionalLight directionalLight) {
    vec3 fogColor = fog.color * (ambientLightColor + directionalLight.color * directionalLight.intensity);
    float distance = length(position);
    float fogFactor = 1.0 / exp((distance * fog.density) * (distance * fog.density));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    vec3 resultColor = mix(fogColor, color.xyz, fogFactor);
    return vec4(resultColor.xyz, color.w);
}

vec4 calcAmbient(AmbientLight ambientLight, vec4 ambientMat) {
    return vec4(ambientLight.intensity * ambientLight.color, 1) * ambientMat;
}

vec4 calcLightColor(vec4 diffuseMat, vec4 specularMat, vec3 lightColor, float light_intensity
, vec3 position, vec3 to_light_dir, vec3 normal) {

    vec4 diffuseColor = vec4(0, 0, 0, 1);
    vec4 specularColor = vec4(0, 0, 0, 1);

    // Diffuse Light
    float diffuseFactor = max(dot(normal, to_light_dir), 0.0);
    diffuseColor = diffuseMat * vec4(lightColor, 1.0) * diffuseFactor * light_intensity;

    // Specular Light
    vec3 camera_direction = normalize(-position);
    vec3 from_light_dir = -to_light_dir;
    vec3 reflected_light = normalize(reflect(from_light_dir, normal));
    float specularFactor = max(dot(camera_direction, reflected_light), 0.0);
    specularFactor = pow(specularFactor, SPECULAR_POWER);
    specularColor = specularMat * vec4(lightColor, 1.0) * material.reflectance * specularFactor * light_intensity;

    return (diffuseColor + specularColor);
}

vec4 calcPointLight(vec4 diffuseMat, vec4 specularMat, PointLight pointLight, vec3 position, vec3 normal) {
    vec3 light_direction = pointLight.position - position;
    vec3 to_light_direction = normalize(light_direction);
    vec4 light_color = calcLightColor(diffuseMat, specularMat, pointLight.color, pointLight.intensity, position
    , to_light_direction, normal);

    // Attenuation
    float distance = length(light_direction);
    float attenuationInverse = pointLight.attenuation.constant + pointLight.attenuation.linear * distance
    + pointLight.attenuation.exponent * distance * distance;
    return light_color / attenuationInverse;
}

vec4 calcSpotLight(vec4 diffuseMat, vec4 specularMat, SpotLight spotLight, vec3 position, vec3 normal) {
    vec3 light_direction = spotLight.position - position;
    vec3 to_light_direction = normalize(light_direction);
    vec3 from_light_direction = -to_light_direction;
    float spot_alfa = dot(from_light_direction, normalize(spotLight.coneDirection));

    vec4 color = vec4 (0, 0, 0, 0);

    if(spot_alfa > spotLight.cutOff) {
        vec4 light_color = calcLightColor(diffuseMat, specularMat, spotLight.color, spotLight.intensity
        , position, to_light_direction, normal);
        float distance = length(light_direction);
        float attenuationInverse = spotLight.attenuation.constant + spotLight.attenuation.linear * distance
        + spotLight.attenuation.exponent * distance * distance;
        color = light_color / attenuationInverse;
        color *= (1.0 - (1.0 - spot_alfa) / (1.0 - spotLight.cutOff));
    }

    return color;
}

vec4 calcDirectionalLight(vec4 diffuseMat, vec4 specularMat
, DirectionalLight directionalLight, vec3 position, vec3 normal) {

    return calcLightColor(diffuseMat, specularMat, directionalLight.color, directionalLight.intensity
    , position, normalize(directionalLight.direction), normal);
}

void main()
{
    vec4 text_color = texture(textSampler, outTextCoord);
    vec4 diffuse = text_color + material.diffuse;
    vec4 specular = text_color + material.specular;

    if(bypassLighting == 1) {
        fragColor = diffuse;
        return;
    }

    vec4 ambient = calcAmbient(ambientLight, text_color + material.ambient);

    vec4 diffuseSpecular = calcDirectionalLight(diffuse, specular, directionalLight, outPosition, outNormal);

    for(int i = 0; i < MAX_POINT_LIGHT; i++) {
        if(pointLights[i].intensity > 0) {
            diffuseSpecular += calcPointLight(diffuse, specular, pointLights[i], outPosition, outNormal);
        }
    }

    for(int i = 0; i < MAX_SPOT_LIGHT; i++) {
        if(spotLights[i].intensity > 0) {
            diffuseSpecular += calcSpotLight(diffuse, specular, spotLights[i], outPosition, outNormal);
        }
    }

    fragColor = ambient + diffuseSpecular;

    if(fog.activeFog == 1) {
        fragColor = calcFog(outPosition, fragColor, fog, ambientLight.color, directionalLight);
    }
}