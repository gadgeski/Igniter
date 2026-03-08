// デジタル波紋エフェクト用 フラグメントシェーダー
// タッチ座標を中心に、円形の波紋とグリッドが広がるサイバーパンクエフェクトを描画する。

precision mediump float;

uniform vec2  u_Touch;
uniform float u_Time;
uniform vec2  u_Resolution;

varying vec2 v_TexCoord;

const float RIPPLE_SPEED    = 0.45;
const float RIPPLE_WIDTH    = 0.012;
const float RIPPLE_DURATION = 2.0;
const float GRID_SCALE      = 18.0;
const float GRID_LINE_WIDTH = 0.04;
const float GRID_RADIUS     = 0.55;

const vec3 COLOR_RIPPLE = vec3(1.0, 0.37, 0.0);
const vec3 COLOR_GRID   = vec3(1.0, 0.4, 0.1);
const vec3 COLOR_CORE   = vec3(1.0, 0.55, 0.1);

void main() {
    float aspect = u_Resolution.x / u_Resolution.y;
    vec2 uv = v_TexCoord;
    vec2 touch = u_Touch;

    vec2 uvAspect    = vec2(uv.x * aspect, uv.y);
    vec2 touchAspect = vec2(touch.x * aspect, touch.y);
    float dist = length(uvAspect - touchAspect);

    float fadeOut = 1.0 - smoothstep(RIPPLE_DURATION * 0.7, RIPPLE_DURATION, u_Time);

    float rippleRadius = u_Time * RIPPLE_SPEED;
    float rippleDist   = abs(dist - rippleRadius);
    float rippleAlpha  = smoothstep(RIPPLE_WIDTH, 0.0, rippleDist);
    rippleAlpha *= (1.0 - smoothstep(0.0, rippleRadius * 0.5, dist - rippleRadius + RIPPLE_WIDTH));

    float gridMask = smoothstep(GRID_RADIUS, GRID_RADIUS * 0.3, dist / (rippleRadius + 0.001));
    gridMask *= smoothstep(0.0, GRID_RADIUS * 0.15, dist);

    vec2 gridUV = fract(uv * GRID_SCALE);
    float gridX = smoothstep(GRID_LINE_WIDTH, 0.0, min(gridUV.x, 1.0 - gridUV.x));
    float gridY = smoothstep(GRID_LINE_WIDTH, 0.0, min(gridUV.y, 1.0 - gridUV.y));
    float gridAlpha = max(gridX, gridY) * gridMask;

    float coreRadius = 0.018;
    float coreAlpha  = smoothstep(coreRadius, 0.0, dist) * (1.0 - smoothstep(0.0, 0.3, u_Time));

    vec3 finalColor = vec3(0.0);
    float finalAlpha = 0.0;

    finalColor += COLOR_GRID * gridAlpha;
    finalAlpha  = max(finalAlpha, gridAlpha);

    finalColor = mix(finalColor, COLOR_RIPPLE, rippleAlpha);
    finalAlpha  = max(finalAlpha, rippleAlpha);

    finalColor = mix(finalColor, COLOR_CORE, coreAlpha);
    finalAlpha  = max(finalAlpha, coreAlpha);

    finalAlpha *= fadeOut;

    gl_FragColor = vec4(finalColor, finalAlpha);
}
