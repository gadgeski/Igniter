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

const float PARALLAX_SCALE = 0.0025;
const float BASE_ABERRATION = 0.004;  // 常時かかる最小量
const float MAX_ABERRATION  = 0.018;  // 波パルス時の最大量

void main() {
    vec2 baseUV = v_TexCoord + vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;

    // 傾きに応じた収差の強さ
    float tiltStrength = clamp(length(u_Tilt) / 9.8, 0.0, 1.0);

    // 波パルスに応じた収差の強さ
    float pulseStrength = 0.0;
    if (u_EnableWaterMotion > 0.5) {
        float envelope = 1.0 - smoothstep(0.0, 1.0, u_WaveProgress);
        envelope *= envelope;
        pulseStrength = envelope * u_WaveIntensity;
    }

    float aberrationStrength = BASE_ABERRATION
        + (MAX_ABERRATION - BASE_ABERRATION)
        * clamp(tiltStrength + pulseStrength * 0.6, 0.0, 1.0);

    // 放射状のズレ方向（UV中心から外側へ）
    vec2 center = vec2(0.5, 0.5);
    vec2 dir = normalize(baseUV - center);
    float dist = length(baseUV - center);

    // 中心付近はズレを抑える
    float radialFade = smoothstep(0.0, 0.3, dist);

    vec2 offset = dir * aberrationStrength * radialFade;

    // RGB を別々にサンプリング
    float r = texture2D(u_Texture, baseUV + offset).r;
    float g = texture2D(u_Texture, baseUV).g;
    float b = texture2D(u_Texture, baseUV - offset).b;

    vec3 aberratedColor = vec3(r, g, b);

    // 輝度計算（明るい部分ほど収差を強く見せる）
    float brightness = dot(aberratedColor, vec3(0.299, 0.587, 0.114));
    float brightMask = smoothstep(0.45, 0.85, brightness);

    // 暗い部分は元のテクスチャ、明るい部分は収差あり
    vec4 baseColor = texture2D(u_Texture, baseUV);
    vec3 finalColor = mix(baseColor.rgb, aberratedColor, brightMask);

    gl_FragColor = vec4(finalColor, baseColor.a);
}