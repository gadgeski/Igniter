// 背景テクスチャ用 フラグメントシェーダー
// igniter_bg テクスチャをサンプリングして全画面に描画する
// 端の引き伸ばし対策入り

precision mediump float;

uniform sampler2D u_Texture;   // 背景テクスチャ
uniform vec2 u_Tilt;           // デバイスの傾き（加速度: X, Y）
uniform float u_Time;          // 経過時間（使用しないが互換維持）
varying vec2 v_TexCoord;       // 頂点シェーダーから受け取ったUV

const float UV_MARGIN = 0.035;       // 3.5% だけ内側を使う
const float PARALLAX_SCALE = 0.0035; // 少し控えめに

void main() {
    // 最初から少し内側のUVだけを使う
    vec2 safeUV = mix(
        vec2(UV_MARGIN),
        vec2(1.0 - UV_MARGIN),
        v_TexCoord
    );

    // パララックス効果
    vec2 parallaxOffset = vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;

    // 安全領域内でサンプリング
    vec2 sampleUV = clamp(
        safeUV + parallaxOffset,
        vec2(UV_MARGIN),
        vec2(1.0 - UV_MARGIN)
    );

    vec4 bgColor = texture2D(u_Texture, sampleUV);

    // デジタル・サンセット
    vec3 neonOrange = vec3(1.0, 0.37, 0.0);
    vec3 deepMagenta = vec3(0.83, 0.0, 0.97);

    float gradientBase = smoothstep(0.4, 1.0, v_TexCoord.y);
    vec3 sunsetColor = mix(deepMagenta, neonOrange, smoothstep(0.6, 1.0, v_TexCoord.y));
    float tiltIntensity = clamp(u_Tilt.y / 9.8, 0.0, 1.0);
    float sunsetIntensity = gradientBase * (0.3 + 0.7 * tiltIntensity);

    // 明るさ加算は少し飽和しやすいので clamp で抑える
    vec3 litColor = clamp(bgColor.rgb + sunsetColor * sunsetIntensity, 0.0, 1.0);

    // 元の色と発光色を自然に混ぜる
    vec3 finalColor = mix(bgColor.rgb, litColor, sunsetIntensity);

    gl_FragColor = vec4(finalColor, bgColor.a);
}