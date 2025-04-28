#version 410 core

out vec4 FragColor;

in vec3 WorldPos;

uniform sampler2D equirectangularMap;

const vec2 invAtan = vec2(0.1591, 0.3183); // 1 / (2*PI), 1 / PI

vec2 SampleSphericalMap(vec3 v)
{
    // Convert direction vector v to spherical coordinates (theta, phi)
    vec2 uv = vec2(atan(v.z, v.x), asin(v.y));
    // Map spherical coordinates to [0, 1] range for texture sampling
    uv *= invAtan;
    uv += 0.5;
    return uv;
}

void main()
{     
    // Restore original code: Sample the equirectangular map 
    vec2 uv = SampleSphericalMap(normalize(WorldPos)); 
    vec3 color = texture(equirectangularMap, uv).rgb;
    FragColor = vec4(color, 1.0);
} 