#version 410

in vec3 WorldPos;

uniform vec3 gCameraWorldPos;
uniform float gridSpacing = 10.0;
uniform float lineThickness = 0.5;
uniform vec3 gridColor = vec3(1.0, 0.0, 0.0);

out vec4 FragColor;

void main()
{
    vec2 coord = WorldPos.xz / gridSpacing;
    vec2 grid = abs(fract(coord - 0.5) - 0.5) / fwidth(coord);

    float line = min(grid.x, grid.y);

    // Only draw the grid lines, discard everything else
    if (line > lineThickness)
    discard;

    FragColor = vec4(gridColor, 1.0); // Full opacity on lines
}