// 背景テクスチャ用 フラグメントシェーダー
// 明るい空・発光部のみに大気の揺らぎ（ヒートヘイズ）を与え、下部3分の1を引き締める

precision mediump float;

uniform sampler2D u_Texture;   // 背景テクスチャ
uniform vec2 u_Tilt;           // デバイスの傾き（加速度: X, Y）
uniform float u_Time;          // 経過時間
uniform float u_WaveIntensity; // センサー由来のうねり強度（Opt-in）
varying vec2 v_TexCoord;       // 頂点シェーダーから受け取ったUV

// --- 空間・パララックス定数 ---
const float UV_MARGIN = 0.035;
const float PARALLAX_SCALE = 0.0035;
const float GRAVITY_NORM = 9.8;

// --- ヒートヘイズ（大気揺らぎ）抽出マスク定数 ---
const vec3 LUMA_WEIGHTS = vec3(0.299, 0.587, 0.114);
const float HAZE_LUM_MIN = 0.20;      // 暗部（手前の鉄骨や黒背景）は揺らさない
const float HAZE_LUM_MAX = 0.55;      // 明るい空・発光部ほど強く揺らす
const float CYAN_PROTECT_SCALE = 0.85; // シアン系のグリッチ枠を歪ませないための保護係数
const float WARM_MASK_MIN = -0.08;
const float WARM_MASK_MAX = 0.12;
const float HAZE_TOP_FADE_START = 0.05;
const float HAZE_TOP_FADE_END = 0.20;
const float HAZE_BOTTOM_FADE_START = 0.75;
const float HAZE_BOTTOM_FADE_END = 0.45;

// --- 陽炎の波形パラメータ ---
// 周期の違う2つの波を重ね、規則的な縞に見えないようにする
const float WAVE1_FREQ_Y = 55.0;          // ビルの輪郭が揺らぐ程度の細かさ
const float WAVE1_FREQ_X = 7.0;
const float WAVE1_SPEED = 2.4;            // 熱気が立ち昇って見える速さ
const float WAVE2_FREQ_Y = 95.0;          // 主波より細かい副波で揺れを不規則にする
const float WAVE2_FREQ_X = 13.0;
const float WAVE2_SPEED = 3.8;
const float WAVE1_WEIGHT = 0.65;
const float WAVE2_WEIGHT = 0.35;

const float BASE_HAZE_STRENGTH = 0.0045;  // 静止時でも陽炎と分かる揺れ幅
const float BOOST_HAZE_STRENGTH = 0.0080; // 端末を動かしたときに上乗せする熱波
const float VERTICAL_DISTORT_RATIO = 0.35; // 縦揺れは横より控えめにし、像が崩れないようにする

// --- 呼吸・下部引き締め（AGENTS.md 4章準拠）定数 ---
const float BREATH_SPEED = 0.9;
const float BREATH_INTENSITY = 0.06;
const float TILT_GLOW_BOOST = 0.06;
const float BOTTOM_DARKEN_START = 0.55;
const float BOTTOM_DARKEN_END = 0.98;
const float BOTTOM_MIN_BRIGHTNESS = 0.55; // 下3分の1の霞みを抑えてドック視認性を確保

void main() {
    // 1. パララックス計算（安全領域内でサンプリング）
    vec2 safeUV = mix(
        vec2(UV_MARGIN),
        vec2(1.0 - UV_MARGIN),
        v_TexCoord
    );

    vec2 parallaxOffset = vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;
    vec2 baseUV = clamp(
        safeUV + parallaxOffset,
        vec2(UV_MARGIN),
        vec2(1.0 - UV_MARGIN)
    );

    // 2. マスク生成用の事前サンプリング（揺らす場所の特定）
    vec4 rawSample = texture2D(u_Texture, baseUV);
    float lum = dot(rawSample.rgb, LUMA_WEIGHTS);

    // 輝度マスク：手前の黒いアーチやシルエットは固定し、奥の明るい空・光源だけを抽出
    float brightMask = smoothstep(HAZE_LUM_MIN, HAZE_LUM_MAX, lum);

    // 色相マスク：焼き込まれたシアンのグリッチ枠（G/Bが強い箇所）が歪んでボケるのを防ぐ
    float warmDiff = rawSample.r - max(rawSample.g, rawSample.b) * CYAN_PROTECT_SCALE;
    float warmMask = smoothstep(WARM_MASK_MIN, WARM_MASK_MAX, warmDiff);

    // 縦位置マスク：画面の上〜中盤（空と摩天楼の領域）を中心に揺らす
    float verticalMask = smoothstep(HAZE_TOP_FADE_START, HAZE_TOP_FADE_END, v_TexCoord.y)
                       * smoothstep(HAZE_BOTTOM_FADE_START, HAZE_BOTTOM_FADE_END, v_TexCoord.y);

    float hazeMask = brightMask * warmMask * verticalMask;

    // 3. 大気スモッグ揺らぎ（ヒートヘイズ）のUV計算
    // 周期の異なる2つの波を重ね、下から上へ立ち昇る不規則な陽炎を作る
    float wave1 = sin(baseUV.y * WAVE1_FREQ_Y - u_Time * WAVE1_SPEED + baseUV.x * WAVE1_FREQ_X);
    float wave2 = cos(baseUV.y * WAVE2_FREQ_Y - u_Time * WAVE2_SPEED - baseUV.x * WAVE2_FREQ_X);
    float heatWave = wave1 * WAVE1_WEIGHT + wave2 * WAVE2_WEIGHT;

    float activeStrength = (BASE_HAZE_STRENGTH + u_WaveIntensity * BOOST_HAZE_STRENGTH) * hazeMask;
    vec2 hazeOffset = vec2(
        heatWave * activeStrength,
        wave1 * activeStrength * VERTICAL_DISTORT_RATIO
    );

    vec2 finalUV = clamp(
        baseUV + hazeOffset,
        vec2(UV_MARGIN),
        vec2(1.0 - UV_MARGIN)
    );

    // 4. 陽炎オフセット適用後のカラー取得
    vec4 hazyColor = texture2D(u_Texture, finalUV);

    // 5. 大気の微細な呼吸（元の画像の色味を壊さず、空の明るさだけをわずかに脈動させる）
    float tiltFactor = clamp(length(u_Tilt) / GRAVITY_NORM, 0.0, 1.0);
    float atmosphereBreath = (0.5 + 0.5 * sin(u_Time * BREATH_SPEED)) * BREATH_INTENSITY
                           + tiltFactor * TILT_GLOW_BOOST;
    vec3 finalColor = hazyColor.rgb + hazyColor.rgb * (hazeMask * atmosphereBreath);

    // 6. 下部3分の1の引き締め（AGENTS.md 4章: ドック・アイコンの視認性確保）
    float bottomMask = smoothstep(BOTTOM_DARKEN_START, BOTTOM_DARKEN_END, v_TexCoord.y);
    finalColor *= mix(1.0, BOTTOM_MIN_BRIGHTNESS, bottomMask);

    gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), hazyColor.a);
}