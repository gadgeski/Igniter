// 背景テクスチャ用 フラグメントシェーダー
// FLOWER_STORM専用
// 花びらが舞う雰囲気に上からオレンジ・黄色・マゼンタのグラデーションをふわっと重ねる
precision mediump float;

uniform sampler2D u_Texture;
uniform vec2 u_Tilt;
uniform float u_Time;
uniform float u_WaveIntensity;
uniform float u_WaveProgress;
uniform float u_EnableWaterMotion;

varying vec2 v_TexCoord;

const float WAVE_OSCILLATION_COUNT = 1.8;
const float PI = 3.14159265359;
const float PARALLAX_SCALE = 0.0030;
const float WAVE_DISTORTION_SCALE = 0.008;
const float EDGE_FADE_MARGIN = 0.15;

// グラデーションカラー定義
const vec3 COLOR_TOP    = vec3(0.96, 0.75, 0.00);
const vec3 COLOR_MID    = vec3(0.55, 0.65, 0.25);
const vec3 COLOR_BOTTOM = vec3(0.80, 0.54, 0.00);

// グラデーションの強さ
const float GRADIENT_INTENSITY = 0.52;

void main() {
    vec2 baseUV = v_TexCoord + vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;

    vec4 bgColor = texture2D(u_Texture, baseUV);

    // --- グラデーション計算 ---
    // 上端が強く、下に向かって消える
    float gradientBase = smoothstep(1.0, 0.4, v_TexCoord.y);

    // 上→中→下の3色グラデーション
    vec3 gradientColor = mix(COLOR_BOTTOM, COLOR_MID, smoothstep(0.0, 0.5, v_TexCoord.y));
    gradientColor = mix(gradientColor, COLOR_TOP, smoothstep(0.3, 0.0, v_TexCoord.y));

    // 傾きに応じてグラデーションが微妙に変化する
    float tiltIntensity = clamp(u_Tilt.y / 9.8, 0.0, 1.0);
    float intensity = gradientBase * (GRADIENT_INTENSITY + 0.15 * tiltIntensity);

    // 背景画像にグラデーションを自然に重ねる
    vec3 litColor = clamp(bgColor.rgb + gradientColor * intensity, 0.0, 1.0);
    vec3 blendedColor = mix(bgColor.rgb, litColor, intensity);

    // --- 波の歪み ---
    if (u_EnableWaterMotion < 0.5) {
        gl_FragColor = vec4(blendedColor, bgColor.a);
        return;
    }

    float phase = u_WaveProgress * (PI * 2.0) * WAVE_OSCILLATION_COUNT;
    float envelope = 1.0 - smoothstep(0.0, 1.0, u_WaveProgress);
    envelope *= envelope;

    float edgeFade = smoothstep(0.0, EDGE_FADE_MARGIN, v_TexCoord.y)
                   * smoothstep(0.0, EDGE_FADE_MARGIN, 1.0 - v_TexCoord.y);

    vec2 drift1 = vec2(
        sin(baseUV.y * 7.0 + phase),
        cos(baseUV.x * 5.0 + phase * 0.7)
    );
    vec2 drift2 = vec2(
        sin(baseUV.y * 13.0 + phase * 1.3 + 1.5),
        cos(baseUV.x * 9.0 + phase * 0.5)
    );

    vec2 waterDistortion = (drift1 * 0.6 + drift2 * 0.4)
        * WAVE_DISTORTION_SCALE * u_WaveIntensity * envelope * edgeFade;

    vec4 distortedBg = texture2D(u_Texture, baseUV + waterDistortion);
    vec3 distortedLit = clamp(distortedBg.rgb + gradientColor * intensity, 0.0, 1.0);
    vec3 finalColor = mix(distortedBg.rgb, distortedLit, intensity);

    gl_FragColor = vec4(finalColor, distortedBg.a);
}