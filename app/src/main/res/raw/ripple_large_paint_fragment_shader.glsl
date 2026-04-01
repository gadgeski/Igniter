// Large Paint 向け 波紋エフェクト用 フラグメントシェーダー
// 炎・オレンジ系の波紋
precision mediump float;

uniform vec2  u_Touch;
uniform float u_Time;
uniform vec2  u_Resolution;

varying vec2 v_TexCoord;

const float RIPPLE_SPEED    = 0.52;
const float RIPPLE_WIDTH    = 0.026;
const float RIPPLE_DURATION = 2.0;

// Large Paint カラー定義
const vec3 COLOR_RIPPLE = vec3(1.00, 0.42, 0.00); // 炎オレンジ
const vec3 COLOR_CORE   = vec3(1.00, 0.95, 0.60); // 白熱イエロー
const vec3 COLOR_GLOW   = vec3(0.90, 0.20, 0.00); // 深い赤オレンジ

void main() {
    float aspect = u_Resolution.x / u_Resolution.y;

    vec2 uv          = v_TexCoord;
    vec2 touch       = u_Touch;
    vec2 uvAspect    = vec2(uv.x * aspect, uv.y);
    vec2 touchAspect = vec2(touch.x * aspect, touch.y);

    float dist = length(uvAspect - touchAspect);

    float fadeOut = 1.0 - smoothstep(RIPPLE_DURATION * 0.55, RIPPLE_DURATION, u_Time);

    // 波紋リング
    float rippleRadius = u_Time * RIPPLE_SPEED;
    float rippleDist   = abs(dist - rippleRadius);
    float rippleAlpha  = smoothstep(RIPPLE_WIDTH, 0.0, rippleDist);
    rippleAlpha *= (1.0 - smoothstep(0.0, rippleRadius * 0.85,
                    dist - rippleRadius + RIPPLE_WIDTH));

    // 中心コア
    float coreRadius = 0.022;
    float coreAlpha  = smoothstep(coreRadius, 0.0, dist)
                     * (1.0 - smoothstep(0.0, 0.40, u_Time));

    // 外側グロー
    float glowRadius = rippleRadius + 0.012;
    float glowDist   = abs(dist - glowRadius);
    float glowAlpha  = smoothstep(RIPPLE_WIDTH * 2.0, 0.0, glowDist) * 0.40;

    vec3 finalColor  = COLOR_RIPPLE;
    float finalAlpha = rippleAlpha;

    finalColor  = mix(finalColor, COLOR_GLOW, glowAlpha * 0.50);
    finalAlpha  = max(finalAlpha, glowAlpha);

    finalColor  = mix(finalColor, COLOR_CORE, coreAlpha);
    finalAlpha  = max(finalAlpha, coreAlpha);

    finalAlpha *= fadeOut * 0.65;

    gl_FragColor = vec4(finalColor, finalAlpha);
}