// 背景テクスチャ用 フラグメントシェーダー
// igniter_bg.png テクスチャをサンプリングして全画面に描画する

precision mediump float;

uniform sampler2D u_Texture;   // 背景テクスチャ
uniform vec2 u_Tilt;           // デバイスの傾き（加速度: X, Y）
varying vec2 v_TexCoord;       // 頂点シェーダーから受け取ったUV

void main() {
    // 1. パララックス効果 (Parallax Effect)
    // u_Tilt（加速度）を用いて背景のUV座標を少しズラす
    // ※ 係数は微小に抑え、画面外が見えないようにする
    vec2 parallaxOffset = vec2(u_Tilt.x, -u_Tilt.y) * 0.005;
    // clampで端が見切れたときの折り返しやはみ出しを防ぐ
    vec2 sampleUV = clamp(v_TexCoord + parallaxOffset, 0.0, 1.0);
    vec4 bgColor = texture2D(u_Texture, sampleUV);
    
    // 2. デジタル・サンセット (Digital Sunset)
    // 画面下部（UVのYが1.0に近い領域）から上部（0.0）へのグラデーション
    vec3 neonOrange = vec3(1.0, 0.37, 0.0);
    vec3 deepMagenta = vec3(0.83, 0.0, 0.97);
    
    // Y座標によるデフォルトの高さ依存グラデーション (下部が強い)
    // UV.y は上が0.0、下が1.0 を想定
    float gradientBase = smoothstep(0.4, 1.0, v_TexCoord.y);
    
    // 夕暮れの色をミックス（上から下へマゼンタからオレンジへ近づく）
    vec3 sunsetColor = mix(deepMagenta, neonOrange, smoothstep(0.6, 1.0, v_TexCoord.y));
    
    // デバイスのY軸の傾き（u_Tilt.y）でサンセットの強さをコントロール
    // 垂直に立てた時（yが約9.8近辺）に強くなるようにスケール
    float tiltIntensity = clamp(u_Tilt.y / 9.8, 0.0, 1.0);
    
    // 最終的なサンセットのアルファ（強さ）を算出
    float sunsetIntensity = gradientBase * (0.3 + 0.7 * tiltIntensity);
    
    // 背景色とデジタルサンセットをスクリーン合成（加算）またはミックスする
    // サイバーパンクらしく、暗い背景に対して色が毒々しく乗るようにMixと加算を組み合わせる
    vec3 finalColor = mix(bgColor.rgb, bgColor.rgb + sunsetColor, sunsetIntensity);
    
    gl_FragColor = vec4(finalColor, bgColor.a);
}
