// 背景テクスチャ用 フラグメントシェーダー
// サマービーチ系の背景用
precision mediump float;

uniform sampler2D u_Texture;
uniform vec2 u_Tilt;
uniform float u_Time;
uniform float u_WaveIntensity;
uniform float u_WaveProgress;
uniform float u_EnableWaterMotion;

varying vec2 v_TexCoord;

const float WAVE_OSCILLATION_COUNT = 2.5;
const float PI = 3.14159265359;
const float PARALLAX_SCALE = 0.0035;
const float WAVE_DISTORTION_SCALE = 0.012;

void main() {
    vec2 baseUV = v_TexCoord + vec2(u_Tilt.x, -u_Tilt.y) * PARALLAX_SCALE;

    if (u_EnableWaterMotion < 0.5) {
        gl_FragColor = texture2D(u_Texture, baseUV);
        return;
    }

    float phase = u_WaveProgress * (PI * 2.0) * WAVE_OSCILLATION_COUNT;
    float envelope = 1.0 - smoothstep(0.0, 1.0, u_WaveProgress);
    envelope *= envelope;

    vec2 waterDistortion = vec2(
        sin(baseUV.y * 10.0 + phase),
        cos(baseUV.x * 10.0 + phase)
    ) * WAVE_DISTORTION_SCALE * u_WaveIntensity * envelope;

    gl_FragColor = texture2D(u_Texture, baseUV + waterDistortion);
}