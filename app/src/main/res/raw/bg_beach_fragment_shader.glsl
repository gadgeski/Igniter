// 背景テクスチャ用 フラグメントシェーダー
// サマービーチの穏やかな水面の揺らぎを表現する

precision mediump float;

uniform sampler2D u_Texture;   // 背景テクスチャ
uniform vec2 u_Tilt;           // デバイスの傾き（加速度: X, Y）
uniform float u_Time;          // 経過時間（秒）
varying vec2 v_TexCoord;       // 頂点シェーダーから受け取ったUV

void main() {
    // 1. パララックス効果 (Parallax Effect)
    // u_Tilt（加速度）を用いて背景のUV座標を少しズラす
    vec2 parallaxOffset = vec2(u_Tilt.x, -u_Tilt.y) * 0.005;
    
    // 2. 穏やかな水面の揺らぎ (Water Ripple / Caustics)
    // u_Time と sin/cos関数を用いて、UV座標をわずかに歪ませる
    vec2 waterDistortion = vec2(
        sin(v_TexCoord.y * 15.0 + u_Time * 1.5) * 0.015,
        cos(v_TexCoord.x * 15.0 + u_Time * 1.5) * 0.015
    );

    // uvを計算、端でのはみ出しを防ぐためにclamp
    vec2 finalUV = clamp(v_TexCoord + parallaxOffset + waterDistortion, 0.0, 1.0);
    vec4 bgColor = texture2D(u_Texture, finalUV);
    
    gl_FragColor = vec4(bgColor.rgb, bgColor.a);
}
