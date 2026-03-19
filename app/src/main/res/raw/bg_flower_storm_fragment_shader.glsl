// 背景テクスチャ用 フラグメントシェーダー
// FLOWER_STORM専用
// 花びらが舞う雰囲気にオーロラ効果を重ねた幻想的な表現
precision mediump float;

uniform sampler2D u_Texture;
uniform vec2 u_Tilt;
uniform float u_Time;
uniform float u_WaveIntensity;
uniform float u_WaveProgress;
uniform float u_EnableWaterMotion;

varying vec2 v_TexCoord;

const float PI = 3.14159265359;
const float PARALLAX_SCALE = 0.0025;
const float EDGE_FADE_MARGIN = 0.15;

// オーロラ定数
const float AURORA_SPEED = 0.4;
const float AURORA_INTENSITY = 0.40;
const float AURORA_BAND_COUNT = 2.0;

// オーロラカラー定義
const vec3 AURORA_COLOR_1 = vec3(1.00, 0.45, 0.00); // オレンジ
const vec3 AURORA_COLOR_2 = vec3(1.00, 0.90, 0.00); // 黄色
const vec3 AURORA_COLOR_3 = vec3(1.00, 0.00, 0.70); // マゼンタ

void main() {
    vec2 baseUV = v_TexCoord + vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;

    // 背景テクスチャをサンプリング
    vec4 bgColor = texture2D(u_Texture, baseUV);

    // --- オーロラ計算 ---
    float t = u_Time * AURORA_SPEED;

    // 縦方向に流れる帯を複数重ねる
    float band1 = sin(v_TexCoord.x * PI * AURORA_BAND_COUNT + t) * 0.5 + 0.5;
    float band2 = sin(v_TexCoord.x * PI * AURORA_BAND_COUNT * 1.7 + t * 1.3 + 1.2) * 0.5 + 0.5;
    float band3 = sin(v_TexCoord.x * PI * AURORA_BAND_COUNT * 0.6 + t * 0.7 + 2.4) * 0.5 + 0.5;

    // 縦方向のフェード（画面中央付近に出る）
    float verticalFade = smoothstep(0.0, 0.25, v_TexCoord.y)
                       * smoothstep(0.0, 0.25, 1.0 - v_TexCoord.y);

    // 3色を混合
    vec3 auroraColor = mix(AURORA_COLOR_1, AURORA_COLOR_2, band1);
    auroraColor = mix(auroraColor, AURORA_COLOR_3, band2 * 0.5);

    float auroraAlpha = (band1 * 0.5 + band2 * 0.3 + band3 * 0.2)
                      * AURORA_INTENSITY * verticalFade;

    // 波パルスがある場合はオーロラをさらに強調
    if (u_EnableWaterMotion > 0.5) {
        float envelope = 1.0 - smoothstep(0.0, 1.0, u_WaveProgress);
        envelope *= envelope;
        auroraAlpha += envelope * u_WaveIntensity * 0.18;
    }

    // 背景画像にオーロラを加算合成
    vec3 finalColor = bgColor.rgb + auroraColor * auroraAlpha;

    // 端フェード（オーロラには適用しない、背景の歪みがないため不要）
    gl_FragColor = vec4(finalColor, bgColor.a);
}