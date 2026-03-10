// 背景テクスチャ用 フラグメントシェーダー
// サマービーチの穏やかな水面の揺らぎを表現する
// 静止時は水面計算（sin/cos）をスキップする版

precision mediump float;

uniform sampler2D u_Texture;          // 背景テクスチャ
uniform vec2 u_Tilt;                  // デバイスの傾き（加速度: X, Y）
uniform float u_Time;                 // 経過時間（秒）
uniform float u_WaveIntensity;        // 波の強さ
uniform float u_EnableWaterMotion;    // 1.0=波計算あり, 0.0=波計算なし

varying vec2 v_TexCoord;              // 頂点シェーダーから受け取ったUV

void main() {
    // 1. パララックス効果
    vec2 parallaxOffset = vec2(u_Tilt.x, -u_Tilt.y) * 0.005;

    // 静止時はまずパララックスだけ適用した UV を使う
    vec2 baseUV = clamp(v_TexCoord + parallaxOffset, 0.0, 1.0);

    // 2. 静止時は水面アニメーション計算を行わない
    if (u_EnableWaterMotion < 0.5) {
        vec4 bgColor = texture2D(u_Texture, baseUV);
        gl_FragColor = vec4(bgColor.rgb, bgColor.a);
        return;
    }

    // 3. 動いている時だけ穏やかな水面の揺らぎを計算
    vec2 waterDistortion = vec2(
        sin(v_TexCoord.y * 10.0 + u_Time * 2.0),
        cos(v_TexCoord.x * 10.0 + u_Time * 2.0)
    ) * 0.01 * u_WaveIntensity;

    // 4. 最終UV
    vec2 finalUV = clamp(baseUV + waterDistortion, 0.0, 1.0);

    vec4 bgColor = texture2D(u_Texture, finalUV);
    gl_FragColor = vec4(bgColor.rgb, bgColor.a);
}