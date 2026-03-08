// 背景テクスチャ用 フラグメントシェーダー
// igniter_bg.png テクスチャをサンプリングして全画面に描画する

precision mediump float;

uniform sampler2D u_Texture;   // 背景テクスチャ
uniform vec2 u_Tilt;           // デバイスの傾き（加速度: X, Y）
uniform float u_Time;          // 経過時間（使用しないが互換維持）
varying vec2 v_TexCoord;       // 頂点シェーダーから受け取ったUV

void main() {
    // 1. パララックス効果 (Parallax Effect)
    // u_Tilt（加速度）を用いて背景のUV座標を少しズラす
    vec2 parallaxOffset = vec2(u_Tilt.x, -u_Tilt.y) * 0.005;
    vec2 sampleUV = clamp(v_TexCoord + parallaxOffset, 0.0, 1.0);
    vec4 bgColor = texture2D(u_Texture, sampleUV);
    
    // 2. デジタル・サンセット (Digital Sunset)
    vec3 neonOrange = vec3(1.0, 0.37, 0.0);
    vec3 deepMagenta = vec3(0.83, 0.0, 0.97);
    
    float gradientBase = smoothstep(0.4, 1.0, v_TexCoord.y);
    vec3 sunsetColor = mix(deepMagenta, neonOrange, smoothstep(0.6, 1.0, v_TexCoord.y));
    float tiltIntensity = clamp(u_Tilt.y / 9.8, 0.0, 1.0);
    float sunsetIntensity = gradientBase * (0.3 + 0.7 * tiltIntensity);
    
    vec3 finalColor = mix(bgColor.rgb, bgColor.rgb + sunsetColor, sunsetIntensity);
    
    gl_FragColor = vec4(finalColor, bgColor.a);
}
