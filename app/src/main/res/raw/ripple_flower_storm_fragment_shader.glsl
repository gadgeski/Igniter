// Flower Storm 向け 波紋エフェクト用 フラグメントシェーダー
// タッチ座標を中心に、花びらの余韻のようなピンク〜マゼンタ系の波紋を描画する。

precision mediump float;

// --- Uniform変数 ---
uniform vec2  u_Touch;       // 正規化タッチ座標 (0.0〜1.0, Y軸は上が0)
uniform float u_Time;        // タッチからの経過時間 (秒)
uniform vec2  u_Resolution;  // 画面解像度 (px)

varying vec2 v_TexCoord;     // UV座標 (0.0〜1.0)

// --- 定数 ---
const float RIPPLE_SPEED    = 0.44;   // 穏やかに広がる
const float RIPPLE_WIDTH    = 0.028;  // 少し柔らかめ
const float RIPPLE_DURATION = 2.0;    // 波紋の持続秒数

// Flower Storm カラー定義
const vec3 COLOR_RIPPLE = vec3(0.96, 0.42, 0.74); // ローズピンク〜マゼンタ
const vec3 COLOR_CORE   = vec3(1.00, 0.90, 0.96); // 白寄りの薄ピンク
const vec3 COLOR_GLOW   = vec3(0.88, 0.62, 0.96); // ラベンダー寄りグロー

void main() {
    // アスペクト比補正
    float aspect = u_Resolution.x / u_Resolution.y;

    vec2 uv = v_TexCoord;
    vec2 touch = u_Touch;

    vec2 uvAspect    = vec2(uv.x * aspect, uv.y);
    vec2 touchAspect = vec2(touch.x * aspect, touch.y);

    float dist = length(uvAspect - touchAspect);

    // フェードアウト
    float fadeOut = 1.0 - smoothstep(RIPPLE_DURATION * 0.58, RIPPLE_DURATION, u_Time);

    // --- 1. 波紋リング ---
    float rippleRadius = u_Time * RIPPLE_SPEED;
    float rippleDist   = abs(dist - rippleRadius);
    float rippleAlpha  = smoothstep(RIPPLE_WIDTH, 0.0, rippleDist);

    // リングの中心を少し柔らかく整える
    rippleAlpha *= (1.0 - smoothstep(0.0, rippleRadius * 0.85, dist - rippleRadius + RIPPLE_WIDTH));

    // --- 2. 中心コア ---
    float coreRadius = 0.020;
    float coreAlpha  = smoothstep(coreRadius, 0.0, dist) * (1.0 - smoothstep(0.0, 0.38, u_Time));

    // --- 3. 外側グロー ---
    float glowRadius = rippleRadius + 0.012;
    float glowDist   = abs(dist - glowRadius);
    float glowAlpha  = smoothstep(RIPPLE_WIDTH * 2.0, 0.0, glowDist) * 0.42;

    // --- 最終合成 ---
    vec3 finalColor = COLOR_RIPPLE;
    float finalAlpha = rippleAlpha;

    // 外側のラベンダー寄りグロー
    finalColor = mix(finalColor, COLOR_GLOW, glowAlpha * 0.45);
    finalAlpha = max(finalAlpha, glowAlpha);

    // 中心コアを最前面に
    finalColor = mix(finalColor, COLOR_CORE, coreAlpha);
    finalAlpha = max(finalAlpha, coreAlpha);

    // Flower Storm は少し華やかにしつつ透明感を残す
    finalAlpha *= fadeOut * 0.56;

    gl_FragColor = vec4(finalColor, finalAlpha);
}