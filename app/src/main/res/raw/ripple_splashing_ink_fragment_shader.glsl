// Splashing Ink 向け 波紋エフェクト用 フラグメントシェーダー
// タッチ座標を中心に、インクが弾けるような深紫系の波紋を描画する
precision mediump float;

uniform vec2  u_Touch;
uniform float u_Time;
uniform vec2  u_Resolution;

varying vec2 v_TexCoord;

const float RIPPLE_SPEED    = 0.38;   // ゆっくり広がる
const float RIPPLE_WIDTH    = 0.022;
const float RIPPLE_DURATION = 2.0;

// Splashing Ink カラー定義
const vec3 COLOR_RIPPLE = vec3(0.45, 0.00, 0.70); // 深紫
const vec3 COLOR_CORE   = vec3(0.80, 0.40, 1.00); // 明るい紫
const vec3 COLOR_GLOW   = vec3(0.20, 0.00, 0.40); // 暗い紫

void main() {
    float aspect = u_Resolution.x / u_Resolution.y;

    vec2 uv          = v_TexCoord;
    vec2 touch       = u_Touch;
    vec2 uvAspect    = vec2(uv.x * aspect, uv.y);
    vec2 touchAspect = vec2(touch.x * aspect, touch.y);

    float dist = length(uvAspect - touchAspect);

    float fadeOut = 1.0 - smoothstep(RIPPLE_DURATION * 0.55, RIPPLE_DURATION, u_Time);

    // --- 波紋リング（2重にして広がりを表現）---
    float rippleRadius = u_Time * RIPPLE_SPEED;

    // メインリング
    float rippleDist  = abs(dist - rippleRadius);
    float rippleAlpha = smoothstep(RIPPLE_WIDTH, 0.0, rippleDist);
    rippleAlpha *= (1.0 - smoothstep(0.0, rippleRadius * 0.85, dist - rippleRadius + RIPPLE_WIDTH));

    // セカンドリング（少し遅れて追いかける）
    float rippleRadius2 = u_Time * RIPPLE_SPEED * 0.65;
    float rippleDist2   = abs(dist - rippleRadius2);
    float rippleAlpha2  = smoothstep(RIPPLE_WIDTH * 0.7, 0.0, rippleDist2) * 0.45;

    // --- 中心コア ---
    float coreRadius = 0.016;
    float coreAlpha  = smoothstep(coreRadius, 0.0, dist)
                     * (1.0 - smoothstep(0.0, 0.35, u_Time));

    // --- 外側グロー ---
    float glowRadius = rippleRadius + 0.008;
    float glowDist   = abs(dist - glowRadius);
    float glowAlpha  = smoothstep(RIPPLE_WIDTH * 2.2, 0.0, glowDist) * 0.35;

    // --- 最終合成 ---
    vec3 finalColor  = COLOR_RIPPLE;
    float finalAlpha = rippleAlpha;

    // セカンドリングを混ぜる
    finalColor  = mix(finalColor, COLOR_GLOW, rippleAlpha2);
    finalAlpha  = max(finalAlpha, rippleAlpha2);

    // 外側グロー
    finalColor  = mix(finalColor, COLOR_GLOW, glowAlpha * 0.5);
    finalAlpha  = max(finalAlpha, glowAlpha);

    // 中心コアを最前面に
    finalColor  = mix(finalColor, COLOR_CORE, coreAlpha);
    finalAlpha  = max(finalAlpha, coreAlpha);

    // インクらしく少し重めに残す
    finalAlpha *= fadeOut * 0.62;

    gl_FragColor = vec4(finalColor, finalAlpha);
}