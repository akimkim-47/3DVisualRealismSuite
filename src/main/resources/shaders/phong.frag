// ==========================================================
// FILE: src/main/resources/shaders/phong.frag (Fragment Shader)
// Minggu 11: Visual Realism Suite
// Indikator 1: Phong vs Blinn-Phong Reflection Model
// Indikator 2: Flat Shading vs Smooth Shading
// Indikator 4: Perspective-Correct UV Texture Mapping
// Indikator 5: 2-Pass Shadow Mapping dengan Slope-Scaled Bias & 3x3 PCF
// ==========================================================
#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
flat in vec3 FlatNormal;
in vec2 TexCoords;
in vec3 VertexColor;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform bool useVertexColor;

uniform sampler2D textureSampler;
uniform sampler2D shadowMap;

uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useFlatShading;
uniform bool useShadows;
uniform bool unlitMode;

uniform float ambientStrength;
uniform float specularStrength;
uniform float shininess;
uniform float shadowBiasMin;
uniform float shadowBiasMax;

float calculateShadowFactor(vec4 fragPosLightSpace, vec3 norm, vec3 lightDir) {
    if (!useShadows) {
        return 0.0;
    }
    // Transformasi ke koordinat NDC [-1, 1] lalu ke [0, 1]
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    // Di luar frustum cahaya tidak terkena bayangan gelap
    if (projCoords.z > 1.0) {
        return 0.0;
    }

    float currentDepth = projCoords.z;
    // Slope-Scaled Dynamic Bias untuk mencegah Shadow Acne & Peter Panning
    float bias = max(shadowBiasMax * (1.0 - max(dot(norm, lightDir), 0.0)), shadowBiasMin);

    // 3x3 Percentage-Closer Filtering (PCF) untuk tepi bayangan yang realistis
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += (currentDepth - bias > pcfDepth) ? 0.80 : 0.0;
        }
    }
    shadow /= 9.0;
    return shadow;
}

void main() {
    if (unlitMode) {
        FragColor = vec4(objectColor, 1.0);
        return;
    }

    // Pilih normal berdasarkan mode Flat Shading vs Smooth Shading (Indikator 2)
    vec3 norm = normalize(useFlatShading ? FlatNormal : Normal);

    // 1. Ambient Lighting (I_ambient = k_a * I_a)
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting - Lambertian Reflectance (I_diffuse = k_d * I_d * max(N . L, 0))
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular Lighting - Phong vs Blinn-Phong (Indikator 1)
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (diff > 0.0) {
        if (useBlinnPhong) {
            // Blinn-Phong: H = (L + V) / ||L + V||, I_spec = k_s * I_s * (N . H)^n
            vec3 halfwayDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfwayDir), 0.0), shininess * 2.0);
        } else {
            // Standard Phong: R = reflect(-L, N), I_spec = k_s * I_s * (R . V)^n
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
        }
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow Mapping Factor (Indikator 5)
    float shadow = calculateShadowFactor(FragPosLightSpace, norm, lightDir);

    // 5. Base Material / Texture Color (Indikator 4)
    vec3 materialColor = useVertexColor ? VertexColor : objectColor;
    vec3 baseColor = useTexture
            ? texture(textureSampler, TexCoords).rgb * materialColor
            : materialColor;

    // Kombinasi akhir: I_total = (I_ambient + (1 - shadow) * (I_diffuse + I_specular)) * BaseColor
    vec3 lighting = ambient + (1.0 - shadow) * (diffuse + specular);
    vec3 result = lighting * baseColor;
    FragColor = vec4(result, 1.0);
}
