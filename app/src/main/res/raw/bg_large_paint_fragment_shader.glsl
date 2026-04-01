// 背景テクスチャ用 フラグメントシェーダー
// LARGE_PAINT専用
// 中央光源の明暗脈動をメインに、波パルス時に放射状色収差を加える
precision mediump float;

uniform sampler2D u_Texture;
uniform vec2 u_Tilt;
uniform float u_Time;
uniform float u_WaveIntensity;
uniform float u_WaveProgress;
uniform float u_EnableWaterMotion;

varying vec2 v_TexCoord;

const float PARALLAX_SCALE    = 0.0025;
const float PULSE_SPEED       = 1.2;   // 脈動の速さ
const float PULSE_INTENSITY   = 0.18;  // 明暗の振れ幅
const float BASE_ABERRATION   = 0.002; // 常時かかる最小収差
const float MAX_ABERRATION    = 0.022; // 波パルス時の最大収差
const float EDGE_FADE_MARGIN  = 0.15;

void main() {
    vec2 baseUV = v_TexCoord + vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;

    // --- 明暗脈動 ---
    // 中央からの距離に応じて脈動の強さを変える
    vec2 center = vec2(0.5, 0.5);
    float distFromCenter = length(baseUV - center);

    // 中央ほど強く脈動する
    float radialMask = 1.0 - smoothstep(0.0, 0.7, distFromCenter);

    float pulse = sin(u_Time * PULSE_SPEED * 3.14159) * 0.5 + 0.5;
    float pulseStrength = pulse * PULSE_INTENSITY * radialMask;

    // 傾きに応じて脈動をわずかに強調
    float tiltStrength = clamp(length(u_Tilt) / 9.8, 0.0, 1.0);
    pulseStrength += tiltStrength * 0.06 * radialMask;

    // --- 色収差（波パルス時のみ強く出る）---
    float aberrationStrength = BASE_ABERRATION;

    if (u_EnableWaterMotion > 0.5) {
        float envelope = 1.0 - smoothstep(0.0, 1.0, u_WaveProgress);
        envelope *= envelope;
        aberrationStrength += envelope * u_WaveIntensity
                            * (MAX_ABERRATION - BASE_ABERRATION);
    }

    // 放射状のズレ方向
    vec2 dir = normalize(baseUV - center);
    float radialFade = smoothstep(0.0, 0.25, distFromCenter);

    float edgeFade = smoothstep(0.0, EDGE_FADE_MARGIN, v_TexCoord.y)
                   * smoothstep(0.0, EDGE_FADE_MARGIN, 1.0 - v_TexCoord.y);

    vec2 offset = dir * aberrationStrength * radialFade * edgeFade;

    // RGB を別々にサンプリング
    float r = texture2D(u_Texture, baseUV + offset).r;
    float g = texture2D(u_Texture, baseUV).g;
    float b = texture2D(u_Texture, baseUV - offset).b;

    vec3 aberratedColor = vec3(r, g, b);

    // 明暗脈動を加算
    vec3 finalColor = aberratedColor + pulseStrength;

    gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}