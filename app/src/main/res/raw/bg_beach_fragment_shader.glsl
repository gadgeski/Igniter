// 背景テクスチャ用 フラグメントシェーダー
// サマービーチの穏やかな水面の揺らぎを表現する
// 入力後だけ 2〜3 回ほど揺れて自然に収束する版

precision mediump float;

uniform sampler2D u_Texture;
uniform vec2 u_Tilt;
uniform float u_Time;
uniform float u_WaveIntensity;
uniform float u_WaveProgress;       // 0.0 -> 1.0
uniform float u_EnableWaterMotion;  // 1.0=揺らす, 0.0=静止

varying vec2 v_TexCoord;

// 2〜3回の間くらいで止める
const float WAVE_OSCILLATION_COUNT = 2.5;
const float PI = 3.14159265359;

void main() {
    // 軽いパララックスは常時残す
    vec2 parallaxOffset = vec2(u_Tilt.x, -u_Tilt.y) * 0.005;
    vec2 baseUV = clamp(v_TexCoord + parallaxOffset, 0.0, 1.0);

    // 静止時は波計算を完全にスキップ
    if (u_EnableWaterMotion < 0.5) {
        vec4 bgColor = texture2D(u_Texture, baseUV);
        gl_FragColor = vec4(bgColor.rgb, bgColor.a);
        return;
    }

    // 進行率に応じて 2.5 周期だけ振動
    float phase = u_WaveProgress * (PI * 2.0) * WAVE_OSCILLATION_COUNT;

    // 後半に向かって自然に小さくする
    float envelope = 1.0 - smoothstep(0.0, 1.0, u_WaveProgress);
    envelope *= envelope;

    vec2 waterDistortion = vec2(
        sin(baseUV.y * 10.0 + phase),
        cos(baseUV.x * 10.0 + phase)
    ) * 0.012 * u_WaveIntensity * envelope;

    vec2 finalUV = clamp(baseUV + waterDistortion, 0.0, 1.0);
    vec4 bgColor = texture2D(u_Texture, finalUV);

    gl_FragColor = vec4(bgColor.rgb, bgColor.a);
}