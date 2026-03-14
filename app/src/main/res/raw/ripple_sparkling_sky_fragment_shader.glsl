// Sparkling Sky 向け 波紋エフェクト用 フラグメントシェーダー
// タッチ座標を中心に、澄んだ空気に光が広がるような寒色系の波紋を描画する。

precision mediump float;

// --- Uniform変数 ---
uniform vec2  u_Touch;       // 正規化タッチ座標 (0.0〜1.0, Y軸は上が0)
uniform float u_Time;        // タッチからの経過時間 (秒)
uniform vec2  u_Resolution;  // 画面解像度 (px)

varying vec2 v_TexCoord;     // UV座標 (0.0〜1.0)

// --- 定数 ---
const float RIPPLE_SPEED    = 0.42;   // 少し穏やかに広がる
const float RIPPLE_WIDTH    = 0.024;  // Beach版よりやや細め
const float RIPPLE_DURATION = 2.0;    // 波紋の持続秒数

// Sparkling Sky カラー定義
const vec3 COLOR_RIPPLE = vec3(0.58, 0.96, 0.90); // ミントシアン
const vec3 COLOR_CORE   = vec3(0.88, 0.97, 1.00); // 青白いハイライト

void main() {
    // アスペクト比補正
    float aspect = u_Resolution.x / u_Resolution.y;

    vec2 uv = v_TexCoord;
    vec2 touch = u_Touch;

    vec2 uvAspect    = vec2(uv.x * aspect, uv.y);
    vec2 touchAspect = vec2(touch.x * aspect, touch.y);

    float dist = length(uvAspect - touchAspect);

    // フェードアウト
    float fadeOut = 1.0 - smoothstep(RIPPLE_DURATION * 0.55, RIPPLE_DURATION, u_Time);

    // --- 1. 波紋リング ---
    float rippleRadius = u_Time * RIPPLE_SPEED;
    float rippleDist   = abs(dist - rippleRadius);
    float rippleAlpha  = smoothstep(RIPPLE_WIDTH, 0.0, rippleDist);

    // リング中心を少しだけ繊細に見せる
    rippleAlpha *= (1.0 - smoothstep(0.0, rippleRadius * 0.9, dist - rippleRadius + RIPPLE_WIDTH));

    // --- 2. 中心コア ---
    // 水しぶきというより、光のきらめきに寄せた青白いハイライト
    float coreRadius = 0.018;
    float coreAlpha  = smoothstep(coreRadius, 0.0, dist) * (1.0 - smoothstep(0.0, 0.35, u_Time));

    // --- 3. ほんのり外側グロー ---
    // 少しだけ空気感を足す
    float glowRadius = rippleRadius + 0.01;
    float glowDist   = abs(dist - glowRadius);
    float glowAlpha  = smoothstep(RIPPLE_WIDTH * 2.2, 0.0, glowDist) * 0.35;

    // --- 最終合成 ---
    vec3 finalColor = COLOR_RIPPLE;
    float finalAlpha = rippleAlpha;

    // 外側の淡いグローを追加
    finalColor = mix(finalColor, COLOR_CORE, glowAlpha * 0.35);
    finalAlpha = max(finalAlpha, glowAlpha);

    // 中心コアを最前面へ
    finalColor = mix(finalColor, COLOR_CORE, coreAlpha);
    finalAlpha = max(finalAlpha, coreAlpha);

    // 全体をやや控えめにして透明感優先
    finalAlpha *= fadeOut * 0.48;

    gl_FragColor = vec4(finalColor, finalAlpha);
}