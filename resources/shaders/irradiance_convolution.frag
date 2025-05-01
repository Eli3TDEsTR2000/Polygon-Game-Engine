#version 410 core

out vec4 FragColor;
in vec3 WorldPos;

uniform samplerCube environmentMap;

const float PI = 3.14159265359;

void main()
{
    vec3 N = normalize(WorldPos);

    vec3 irradiance = vec3(0.0);   

    vec3 up    = vec3(0.0, 1.0, 0.0);
    vec3 right = normalize(cross(up, N));
         up    = normalize(cross(N, right));
       
    float sampleDelta = 0.025;
    float nrSamples = 0.0; 
    for(float phi = 0.0; phi < 2.0 * PI; phi += sampleDelta) {
        for(float theta = 0.0; theta < 0.5 * PI; theta += sampleDelta) {
            // Spherical to cartesian coordinates in tangent space
            vec3 tangentSample = vec3(sin(theta) * cos(phi),  sin(theta) * sin(phi), cos(theta));
            // Transform sample vector from tangent space to world space
            vec3 sampleVec = tangentSample.x * right + tangentSample.y * up + tangentSample.z * N; 

            // Sample the environment map, weight by cosine of angle (lambertian)
            // cos(theta) is the dot product N dot sampleVec in tangent space
            irradiance += texture(environmentMap, normalize(sampleVec)).rgb * cos(theta);
            nrSamples++;
        }
    }
    // Scale irradiance by solid angle factor and number of samples
    irradiance = PI * irradiance * (1.0 / nrSamples);
    
    FragColor = vec4(irradiance, 1.0);
} 