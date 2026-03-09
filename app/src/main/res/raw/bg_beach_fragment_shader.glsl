// 背景テクスチャ用 フラグメントシェーダー
// サマービーチの穏やかな水面の揺らぎを表現する

precision mediump float;

uniform sampler2D u_Texture;   // 背景テクスチャ
uniform vec2 u_Tilt;           // デバイスの傾き（加速度: X, Y）
uniform float u_Time;          // 経過時間（秒）
uniform float u_WaveIntensity; // 波の強さ
varying vec2 v_TexCoord;       // 頂点シェーダーから受け取ったUV

void main() {
    // 1. パララックス効果 (Parallax Effect)
    // u_Tilt（加速度）を用いて背景のUV座標を少しズラす
    vec2 parallaxOffset = vec2(u_Tilt.x, -u_Tilt.y) * 0.005;
    
    // 2. 穏やかな水面の揺らぎ (Water Ripple / Caustics)
    // v_TexCoord と sin/cos関数を用いて、UV座標をわずかに歪ませる
    // u_WaveIntensity を掛けることで、スマホを振った時に歪みが大きくなる
    vec2 waterDistortion = vec2(
        sin(v_TexCoord.y * 10.0 + u_Time * 2.0),
        cos(v_TexCoord.x * 10.0 + u_Time * 2.0)
    ) * 0.01 * u_WaveIntensity; // 0.01 はベースの揺れ幅の微調整

    // 3. 最終的なUV座標の計算
    // 端でのはみ出し（テクスチャの折り返しノイズ）を防ぐために clamp する
    vec2 finalUV = clamp(v_TexCoord + parallaxOffset + waterDistortion, 0.0, 1.0);

    // テクスチャから色を取得
    vec4 bgColor = texture2D(u_Texture, finalUV);

    // 描画
    gl_FragColor = vec4(bgColor.rgb, bgColor.a);
}