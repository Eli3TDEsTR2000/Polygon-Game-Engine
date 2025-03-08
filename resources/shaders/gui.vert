#version 410

layout (location=0) in vec2 inPosition;
layout (location=1) in vec2 texCoords;
layout (location=2) in vec4 color;

out vec2 outTexCoords;
out vec4 outColor;

uniform vec2 scale;

void main() {
    outTexCoords = texCoords;
    outColor = color;
    gl_Position = vec4(inPosition * scale + vec2(-1.0, 1.0), 0.0, 1.0);
}