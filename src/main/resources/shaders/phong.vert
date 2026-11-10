// ==========================================================
// FILE: src/main/resources/shaders/phong.vert (Vertex Shader)
// Minggu 11: Visual Realism Suite (Phong/Blinn-Phong, Flat/Smooth, UV, & Shadow Mapping)
// ==========================================================
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec3 aColor;

out vec3 FragPos;
out vec3 Normal;
flat out vec3 FlatNormal;
out vec2 TexCoords;
out vec3 VertexColor;
out vec4 FragPosLightSpace;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;

void main() {
    FragPos = vec3(model * vec4(aPos, 1.0));
    mat3 normalMatrix = mat3(transpose(inverse(model)));
    vec3 worldNormal = normalize(normalMatrix * aNormal);
    Normal = worldNormal;
    FlatNormal = worldNormal;
    TexCoords = aTexCoords;
    VertexColor = aColor;
    FragPosLightSpace = lightSpaceMatrix * vec4(FragPos, 1.0);
    gl_Position = projection * view * vec4(FragPos, 1.0);
}
