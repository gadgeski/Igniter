// 背景テクスチャ用 フラグメントシェーダー
// SUNLIGHT_TREES専用
// 明るい部分に放射状の色収差をかける
// 波パルス・傾きに連動して強弱がつく
precision mediump float;

uniform sampler2D u_Texture;
uniform vec2 u_Tilt;
uniform float u_Time;
uniform float u_WaveIntensity;
uniform float u_WaveProgress;
uniform float u_EnableWaterMotion;

varying vec2 v_TexCoord;

const float PARALLAX_SCALE   = 0.0025;
const float BASE_ABERRATION  = 0.004;
const float MAX_ABERRATION   = 0.018;
const float EDGE_FADE_MARGIN = 0.25;

void main() {
    vec2 baseUV = v_TexCoord + vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;

    float tiltStrength = clamp(length(u_Tilt) / 9.8, 0.0, 1.0);

    float pulseStrength = 0.0;
    if (u_EnableWaterMotion > 0.5) {
        float envelope = 1.0 - smoothstep(0.0, 1.0, u_WaveProgress);
        envelope *= envelope;
        pulseStrength = envelope * u_WaveIntensity;
    }

    float aberrationStrength = BASE_ABERRATION
        + (MAX_ABERRATION - BASE_ABERRATION)
        * clamp(tiltStrength + pulseStrength * 0.6, 0.0, 1.0);

    vec2 center = vec2(0.5, 0.5);
    vec2 dir = normalize(baseUV - center);
    float dist = length(baseUV - center);

    float radialFade = smoothstep(0.0, 0.3, dist);

    // 上下端に近いほど offset を抑える
    float edgeFade = smoothstep(0.0, EDGE_FADE_MARGIN, v_TexCoord.y)
                   * smoothstep(0.0, EDGE_FADE_MARGIN, 1.0 - v_TexCoord.y);

    vec2 offset = dir * aberrationStrength * radialFade * edgeFade;

    float r = texture2D(u_Texture, baseUV + offset).r;
    float g = texture2D(u_Texture, baseUV).g;
    float b = texture2D(u_Texture, baseUV - offset).b;

    vec3 aberratedColor = vec3(r, g, b);

    float brightness = dot(aberratedColor, vec3(0.299, 0.587, 0.114));
    float brightMask = smoothstep(0.45, 0.85, brightness);

    vec4 baseColor = texture2D(u_Texture, baseUV);
    vec3 finalColor = mix(baseColor.rgb, aberratedColor, brightMask);

    gl_FragColor = vec4(finalColor, baseColor.a);
}