// 穏やかなサマービーチ向け 波紋エフェクト用 フラグメントシェーダー
// タッチ座標を中心に、透明感のある水面の波紋を描画する。

precision mediump float;

// --- Uniform変数 ---
uniform vec2  u_Touch;       // 正規化タッチ座標 (0.0〜1.0, Y軸は上が0)
uniform float u_Time;        // タッチからの経過時間 (秒)
uniform vec2  u_Resolution;  // 画面解像度 (px)

varying vec2 v_TexCoord;     // UV座標 (0.0〜1.0)

// --- 定数 ---
const float RIPPLE_SPEED    = 0.45;   // 波紋の広がる速さ
const float RIPPLE_WIDTH    = 0.03;   // 波紋リングの太さ (少し太めにして透明感を出す)
const float RIPPLE_DURATION = 2.0;    // 波紋の持続秒数

// サマービーチ 水面カラー定義
const vec3 COLOR_RIPPLE = vec3(0.8, 0.9, 1.0);  // 淡い水色の陰影・波紋
const vec3 COLOR_CORE   = vec3(1.0, 1.0, 1.0);  // 白のハイライト

void main() {
    // タッチ座標系: 画面のアスペクト比を考慮して距離計算を正確にする
    float aspect = u_Resolution.x / u_Resolution.y;

    // UV座標をアスペクト比補正した空間へ変換
    vec2 uv = v_TexCoord;
    vec2 touch = u_Touch;

    // アスペクト比補正 (X軸を伸ばして正円を保つ)
    vec2 uvAspect    = vec2(uv.x * aspect, uv.y);
    vec2 touchAspect = vec2(touch.x * aspect, touch.y);

    float dist = length(uvAspect - touchAspect);

    // --- フェードアウト係数 ---
    // RIPPLE_DURATION 秒以降はゼロに向けてスムーズにフェード
    float fadeOut = 1.0 - smoothstep(RIPPLE_DURATION * 0.6, RIPPLE_DURATION, u_Time);

    // --- 1. 波紋リング ---
    float rippleRadius = u_Time * RIPPLE_SPEED;
    float rippleDist   = abs(dist - rippleRadius);
    float rippleAlpha  = smoothstep(RIPPLE_WIDTH, 0.0, rippleDist);

    // リングの内側では輝度を下げてエッジを強調しつつ、水面のような柔らかさを残す
    rippleAlpha *= (1.0 - smoothstep(0.0, rippleRadius * 0.8, dist - rippleRadius + RIPPLE_WIDTH));

    // --- 2. 中心コア (白のハイライト) ---
    // タッチ直後、タッチ地点に明るいハイライト（水しぶきの反射のような）を表示
    float coreRadius = 0.02;
    float coreAlpha  = smoothstep(coreRadius, 0.0, dist) * (1.0 - smoothstep(0.0, 0.4, u_Time));

    // --- 最終合成 ---
    vec3 finalColor = vec3(0.0);
    float finalAlpha = 0.0;

    // 波紋リング（ベースの波紋）
    finalColor = COLOR_RIPPLE;
    finalAlpha = rippleAlpha;

    // 中心コア（最前面のハイライト、加算的に乗せる）
    finalColor = mix(finalColor, COLOR_CORE, coreAlpha);
    finalAlpha = max(finalAlpha, coreAlpha);

    // 水面らしい透明感を持たせるため全体のアルファを調整
    finalAlpha *= fadeOut * 0.7; // 爽やかで穏やかな波紋のため少し透明にする

    gl_FragColor = vec4(finalColor, finalAlpha);
}
