// 背景テクスチャ用 フラグメントシェーダー
// SPLASHING_INK専用
// 波パルス・傾き連動で瞬間的にグリッチが発生する
precision mediump float;

uniform sampler2D u_Texture;
uniform vec2 u_Tilt;
uniform float u_Time;
uniform float u_WaveIntensity;
uniform float u_WaveProgress;
uniform float u_EnableWaterMotion;

varying vec2 v_TexCoord;

const float PARALLAX_SCALE     = 0.0025;
const float MAX_GLITCH_SHIFT   = 0.035; // 水平ズレの最大量
const float SCANLINE_INTENSITY = 0.06;  // スキャンラインの強さ

// シンプルなハッシュ関数（疑似乱数）
float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

void main() {
    vec2 baseUV = v_TexCoord + vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;

    // --- グリッチ強度の計算 ---
    float glitchStrength = 0.0;

    // 傾き連動
    float tiltStrength = clamp(length(u_Tilt) / 9.8, 0.0, 1.0);
    glitchStrength += tiltStrength * 0.6;

    // 波パルス連動（瞬間的に強く出る）
    if (u_EnableWaterMotion > 0.5) {
        float envelope = 1.0 - smoothstep(0.0, 0.4, u_WaveProgress);
        envelope *= envelope;
        glitchStrength += envelope * u_WaveIntensity * 0.85;
    }

    glitchStrength = clamp(glitchStrength, 0.0, 1.0);

    // --- グリッチ計算 ---
    vec2 glitchedUV = baseUV;

    if (glitchStrength > 0.01) {
        // 行単位でランダムにズレを発生させる
        float rowId    = floor(baseUV.y * 40.0);
        float randVal  = hash(rowId + floor(u_Time * 8.0));

        // ランダムな行だけズレる（全体には適用しない）
        float rowGlitch = step(0.55, randVal) * glitchStrength;
        float shift     = (hash(rowId * 3.7 + u_Time) * 2.0 - 1.0)
                        * MAX_GLITCH_SHIFT * rowGlitch;

        glitchedUV.x = baseUV.x + shift;

        // RGBチャンネルを微妙にズラして色収差を加える
        float aberration = shift * 0.4;
        float r = texture2D(u_Texture, vec2(glitchedUV.x + aberration, glitchedUV.y)).r;
        float g = texture2D(u_Texture, glitchedUV).g;
        float b = texture2D(u_Texture, vec2(glitchedUV.x - aberration, glitchedUV.y)).b;

        vec3 glitchedColor = vec3(r, g, b);

        // スキャンライン
        float scanline = sin(baseUV.y * 800.0) * SCANLINE_INTENSITY * glitchStrength;
        glitchedColor -= scanline;

        gl_FragColor = vec4(clamp(glitchedColor, 0.0, 1.0), 1.0);
        return;
    }

    // グリッチなし時は通常描画
    gl_FragColor = texture2D(u_Texture, baseUV);
}