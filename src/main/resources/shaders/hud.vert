// ==========================================================
// FILE: src/main/resources/shaders/hud.vert
// Shader 2D untuk HUD Telemetry & Preview Depth Map FBO
// ==========================================================
#version 330 core
layout (location = 0) in vec2 aPos;
layout (location = 1) in vec4 aColor;
layout (location = 2) in vec2 aTexCoord;

out vec4 vColor;
out vec2 vTexCoord;

uniform mat4 orthoProjection;

void main() {
    vColor = aColor;
    vTexCoord = aTexCoord;
    gl_Position = orthoProjection * vec4(aPos.xy, 0.0, 1.0);
}
