// デジタル波紋エフェクト用 フラグメントシェーダー
// タッチ座標を中心に、円形の波紋とグリッドが広がるサイバーパンクエフェクトを描画する。

precision mediump float;

// --- Uniform変数 ---
uniform vec2  u_Touch;       // 正規化タッチ座標 (0.0〜1.0, Y軸は上が0)
uniform float u_Time;        // タッチからの経過時間 (秒)
uniform vec2  u_Resolution;  // 画面解像度 (px)

varying vec2 v_TexCoord;     // UV座標 (0.0〜1.0)

// --- 定数 ---
const float RIPPLE_SPEED    = 0.45;   // 波紋の広がる速さ
const float RIPPLE_WIDTH    = 0.012;  // 波紋リングの太さ
const float RIPPLE_DURATION = 2.0;    // 波紋の持続秒数
const float GRID_SCALE      = 18.0;   // グリッドの格子数
const float GRID_LINE_WIDTH = 0.04;   // グリッドラインの太さ (UV単位)
const float GRID_RADIUS     = 0.55;   // グリッドが表示される半径 (UV単位)

// サイバーパンクカラー定義
const vec3 COLOR_RIPPLE = vec3(0.0, 1.0, 0.95);   // シアン #00FFF2
const vec3 COLOR_GRID   = vec3(0.0, 0.5, 0.55);   // ダークシアン #00808C
const vec3 COLOR_CORE   = vec3(0.6, 1.0, 1.0);    // 明るいシアン (中心コア)

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
    float fadeOut = 1.0 - smoothstep(RIPPLE_DURATION * 0.7, RIPPLE_DURATION, u_Time);

    // --- 1. 波紋リング ---
    float rippleRadius = u_Time * RIPPLE_SPEED;
    float rippleDist   = abs(dist - rippleRadius);
    float rippleAlpha  = smoothstep(RIPPLE_WIDTH, 0.0, rippleDist);

    // リングの内側では輝度を下げてエッジを強調
    rippleAlpha *= (1.0 - smoothstep(0.0, rippleRadius * 0.5, dist - rippleRadius + RIPPLE_WIDTH));

    // --- 2. グリッド ---
    // グリッドはタッチ中心からの距離でフェードイン→フェードアウト
    float gridMask = smoothstep(GRID_RADIUS, GRID_RADIUS * 0.3, dist / (rippleRadius + 0.001));
    gridMask *= smoothstep(0.0, GRID_RADIUS * 0.15, dist); // 中心コア部分は非表示

    // UV座標をグリッド空間に変換
    vec2 gridUV = fract(uv * GRID_SCALE);
    // グリッドライン: X方向またはY方向がラインの太さ以内
    float gridX = smoothstep(GRID_LINE_WIDTH, 0.0, min(gridUV.x, 1.0 - gridUV.x));
    float gridY = smoothstep(GRID_LINE_WIDTH, 0.0, min(gridUV.y, 1.0 - gridUV.y));
    float gridAlpha = max(gridX, gridY) * gridMask;

    // --- 3. 中心コア ---
    // タッチ直後、タッチ地点に明るいコアを表示
    float coreRadius = 0.018;
    float coreAlpha  = smoothstep(coreRadius, 0.0, dist) * (1.0 - smoothstep(0.0, 0.3, u_Time));

    // --- 最終合成 ---
    vec3 finalColor = vec3(0.0);
    float finalAlpha = 0.0;

    // グリッド（最背面）
    finalColor += COLOR_GRID * gridAlpha;
    finalAlpha  = max(finalAlpha, gridAlpha);

    // 波紋リング（上に重ねる）
    finalColor = mix(finalColor, COLOR_RIPPLE, rippleAlpha);
    finalAlpha  = max(finalAlpha, rippleAlpha);

    // 中心コア（最前面）
    finalColor = mix(finalColor, COLOR_CORE, coreAlpha);
    finalAlpha  = max(finalAlpha, coreAlpha);

    // フェードアウトを適用
    finalAlpha *= fadeOut;

    gl_FragColor = vec4(finalColor, finalAlpha);
}
