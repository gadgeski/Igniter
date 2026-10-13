// BULKHEAD_FROST 用 背景フラグメントシェーダー
// 平常時は背景テクスチャ＋パララックスのみ。
// u_ShutterTime >= 0 の間だけ、画像そのものを斜めの継ぎ目で2枚の隔壁に割り、
// 「点火 → 開放 → 保持 → 閉鎖（衝撃） → 余熱」のシーケンスを描く。
// 冷たい白の発光と干渉するため、cyberpunk 版のサンセット加算は入れていない。

precision mediump float;

uniform sampler2D u_Texture;
uniform vec2 u_Tilt;
uniform float u_Time;          // 互換維持（未使用）
uniform vec2 u_Resolution;
uniform float u_ShutterTime;   // シャッター開始からの経過秒。負値なら停止中
varying vec2 v_TexCoord;

const float UV_MARGIN = 0.035;
const float PARALLAX_SCALE = 0.0035;

// --- 継ぎ目の幾何 ---
// 中心フレームの斜めフィン（左上→右下, 45°）に継ぎ目を沿わせ、割れ目が絵の構造から生えて見えるようにする
const vec2 SEAM_CENTER = vec2(0.50, 0.51);
const vec2 SEAM_DIR = vec2(0.70710678, 0.70710678);
const vec2 SEAM_NORMAL = vec2(0.70710678, -0.70710678);
const float SEAM_HALF_LENGTH = 0.62;   // 画面対角を覆う長さ（高さ正規化）

// --- タイムライン（秒） ---
const float T_IGNITE_END = 0.18;
const float T_OPEN_END = 0.60;
const float T_HOLD_END = 1.20;
const float T_CLOSE_END = 1.50;
const float T_DECAY_END = 2.20;

// --- 動き ---
const float MAX_GAP = 0.050;           // 開放時の隙間（高さ正規化）
const float MAX_SHEAR = 0.020;         // 継ぎ目方向へのずれ。真横に割れるだけだと機械感が弱い
const float OPEN_OVERSHOOT = 1.7;
const float SLAM_SHAKE = 0.0035;
const float SHAKE_FREQ = 95.0;
const float SHAKE_DECAY = 22.0;

// --- 光 ---
const vec3 CORE_COLOR = vec3(1.0, 1.0, 1.0);
const vec3 HALO_COLOR = vec3(0.60, 0.82, 1.0);
const float CORE_WIDTH = 0.0022;
const float CORE_SPREAD = 1.2;         // 開くほど芯が太る
const float HALO_WIDTH = 0.024;
const float GAP_INTERIOR_GLOW = 0.15;  // 隙間の奥の明るさ。上げすぎると割れ目が読めず、ただの光線に見える
const float HALO_STRENGTH = 0.85;
const float PANEL_LIGHT_GAIN = 1.4;    // 漏れた光が隔壁の金属面を照らす量
const float IGNITE_HEAD_WIDTH = 0.035;
const float IGNITE_HEAD_GAIN = 2.5;
const float HOLD_FLICKER_FREQ = 47.0;
const float HOLD_FLICKER_DEPTH = 0.08;
const float SLAM_FLASH_GAIN = 1.8;
const float SLAM_FLASH_DECAY = 14.0;
const float SEAM_RESIDUAL_DECAY = 6.0;

vec3 sampleBackground(vec2 uv) {
    vec2 safeUV = mix(vec2(UV_MARGIN), vec2(1.0 - UV_MARGIN), uv);
    vec2 parallaxOffset = vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;
    vec2 sampleUV = clamp(safeUV + parallaxOffset, vec2(UV_MARGIN), vec2(1.0 - UV_MARGIN));
    return texture2D(u_Texture, sampleUV).rgb;
}

float easeOutBack(float x) {
    float c3 = OPEN_OVERSHOOT + 1.0;
    float m = x - 1.0;
    return 1.0 + c3 * m * m * m + OPEN_OVERSHOOT * m * m;
}

float easeInCubic(float x) {
    return x * x * x;
}

float openAmount(float t) {
    if (t < T_IGNITE_END) return 0.0;
    if (t < T_OPEN_END) return easeOutBack((t - T_IGNITE_END) / (T_OPEN_END - T_IGNITE_END));
    if (t < T_HOLD_END) return 1.0;
    if (t < T_CLOSE_END) return 1.0 - easeInCubic((t - T_HOLD_END) / (T_CLOSE_END - T_HOLD_END));
    return 0.0;
}

// 継ぎ目沿いの発光強度。along は継ぎ目方向の座標
float seamIntensity(float t, float along) {
    if (t < T_IGNITE_END) {
        // 点火: 光の先端が継ぎ目を走り、通過した部分だけが灯る
        float head = mix(-SEAM_HALF_LENGTH, SEAM_HALF_LENGTH, t / T_IGNITE_END);
        float lit = step(along, head);
        float headGlow = exp(-abs(along - head) / IGNITE_HEAD_WIDTH) * IGNITE_HEAD_GAIN;
        return lit + headGlow;
    }
    if (t < T_HOLD_END) {
        return 1.0 + HOLD_FLICKER_DEPTH * sin(t * HOLD_FLICKER_FREQ);
    }
    if (t < T_CLOSE_END) {
        return 1.0;
    }
    float since = t - T_CLOSE_END;
    return exp(-since * SEAM_RESIDUAL_DECAY) + SLAM_FLASH_GAIN * exp(-since * SLAM_FLASH_DECAY);
}

void main() {
    float t = u_ShutterTime;

    if (t < 0.0 || t > T_DECAY_END) {
        gl_FragColor = vec4(sampleBackground(v_TexCoord), 1.0);
        return;
    }

    vec2 aspect = vec2(u_Resolution.x / max(u_Resolution.y, 1.0), 1.0);
    vec2 p = (v_TexCoord - SEAM_CENTER) * aspect;

    // 閉鎖の衝撃で一瞬だけ画面ごと継ぎ目と直交方向に揺らす
    if (t > T_CLOSE_END) {
        float shakeEnv = exp(-(t - T_CLOSE_END) * SHAKE_DECAY);
        p += SEAM_NORMAL * SLAM_SHAKE * shakeEnv * sin(t * SHAKE_FREQ);
    }

    float open = openAmount(t);
    float halfGap = 0.5 * MAX_GAP * open;
    float shear = MAX_SHEAR * open;

    float d = dot(p, SEAM_NORMAL);
    float along = dot(p, SEAM_DIR);
    float side = d >= 0.0 ? 1.0 : -1.0;
    float edgeDist = abs(d) - halfGap;   // 隔壁の縁からの距離。負なら隙間の中

    vec3 color = vec3(0.0);
    if (edgeDist > 0.0) {
        vec2 src = p - SEAM_NORMAL * (halfGap * side) - SEAM_DIR * (shear * side);
        color = sampleBackground(src / aspect + SEAM_CENTER);
    }

    float intensity = seamIntensity(t, along);
    float coreWidth = CORE_WIDTH * (1.0 + CORE_SPREAD * open);
    float haloWidth = HALO_WIDTH * (0.35 + 0.65 * open);

    // 芯は継ぎ目の中心線から、照り返しは隔壁の縁から測る
    float core = exp(-abs(d) / coreWidth);
    float halo = exp(-max(edgeDist, 0.0) / haloWidth) * HALO_STRENGTH;
    // 隙間の中は縁だけを光らせ、中心へ向かって暗く落とす
    float interior = mix(GAP_INTERIOR_GLOW, 1.0, smoothstep(-halfGap, 0.0, edgeDist));
    halo *= interior;

    color *= 1.0 + halo * intensity * PANEL_LIGHT_GAIN;
    color += (CORE_COLOR * core + HALO_COLOR * halo) * intensity;

    gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
