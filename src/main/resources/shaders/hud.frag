// ==========================================================
// FILE: src/main/resources/shaders/hud.frag
// Shader 2D untuk HUD Telemetry & Preview Depth Map FBO
// ==========================================================
#version 330 core
in vec4 vColor;
in vec2 vTexCoord;

out vec4 FragColor;

uniform bool useDepthTexture;
uniform sampler2D depthTex;

void main() {
    if (useDepthTexture) {
        float d = texture(depthTex, vTexCoord).r;
        // Kontras visualisasi nilai kedalaman Z-buffer cahaya [0..1]
        float vis = pow(d, 1.5);
        FragColor = vec4(vec3(vis), 1.0);
    } else {
        FragColor = vColor;
    }
}
