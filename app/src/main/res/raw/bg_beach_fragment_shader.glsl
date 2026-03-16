// 背景テクスチャ用 フラグメントシェーダー
// サマービーチ系の背景用
// 引き伸ばし対策を一段強めた版

precision mediump float;

uniform sampler2D u_Texture;
uniform vec2 u_Tilt;
uniform float u_Time;
uniform float u_WaveIntensity;
uniform float u_WaveProgress;       // 0.0 -> 1.0
uniform float u_EnableWaterMotion;  // 1.0=揺らす, 0.0=静止

varying vec2 v_TexCoord;

const float WAVE_OSCILLATION_COUNT = 2.5;
const float PI = 3.14159265359;

// --- 引き伸ばし対策を強化 ---
const float UV_MARGIN_X = 0.05;      // 左右は 5%
const float UV_MARGIN_Y = 0.06;      // 上下は 6%
const float PARALLAX_SCALE = 0.0028; // さらに少し弱める
const float WAVE_DISTORTION_SCALE = 0.0095;

void main() {
    vec2 minUV = vec2(UV_MARGIN_X, UV_MARGIN_Y);
    vec2 maxUV = vec2(1.0 - UV_MARGIN_X, 1.0 - UV_MARGIN_Y);

    // 最初から少し内側だけを使う
    vec2 safeUV = mix(minUV, maxUV, v_TexCoord);

    // パララックス
    vec2 parallaxOffset = vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;

    // パララックス込みでも安全領域に収める
    vec2 baseUV = clamp(safeUV + parallaxOffset, minUV, maxUV);

    // 静止時は波計算をスキップ
    if (u_EnableWaterMotion < 0.5) {
        vec4 bgColor = texture2D(u_Texture, baseUV);
        gl_FragColor = vec4(bgColor.rgb, bgColor.a);
        return;
    }

    float phase = u_WaveProgress * (PI * 2.0) * WAVE_OSCILLATION_COUNT;

    float envelope = 1.0 - smoothstep(0.0, 1.0, u_WaveProgress);
    envelope *= envelope;

    vec2 waterDistortion = vec2(
        sin(baseUV.y * 10.0 + phase),
        cos(baseUV.x * 10.0 + phase)
    ) * WAVE_DISTORTION_SCALE * u_WaveIntensity * envelope;

    vec2 finalUV = clamp(baseUV + waterDistortion, minUV, maxUV);

    vec4 bgColor = texture2D(u_Texture, finalUV);
    gl_FragColor = vec4(bgColor.rgb, bgColor.a);
}