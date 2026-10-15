precision mediump float;

varying vec2 v_TexCoord;

uniform sampler2D u_Texture;
uniform vec2 u_Tilt;
uniform float u_Time;
uniform float u_WaveIntensity;
uniform float u_WaveProgress;
uniform float u_EnableWaterMotion;
uniform vec2 u_Resolution;
uniform float u_ShutterTime;

// AGENTS.md 5章: 端の引き伸ばしを防ぐためのUVマージン
const float UV_MARGIN = 0.012;

// シャッター解錠シーケンスの終了時間（Renderer側の SHUTTER_DURATION_SEC と一致させる）
const float T_DECAY_END = 2.2;

float getLuminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

void main() {
    // UVのマージンクランプ
    vec2 uv = mix(vec2(UV_MARGIN), vec2(1.0 - UV_MARGIN), v_TexCoord);

    // 1. パララックス（視差）
    vec3 rawCenter = texture2D(u_Texture, uv).rgb;
    float baseDepth = getLuminance(rawCenter);
    vec2 parallaxOffset = u_Tilt * (0.006 + baseDepth * 0.012);
    vec2 sampleUv = clamp(uv + parallaxOffset, 0.002, 0.998);

    // 2. 5-Tap サンプリングによる疑似法線（金属反射用）
    float eps = 0.0025;
    vec3 colCenter = texture2D(u_Texture, sampleUv).rgb;
    vec3 colRight  = texture2D(u_Texture, sampleUv + vec2(eps, 0.0)).rgb;
    vec3 colLeft   = texture2D(u_Texture, sampleUv - vec2(eps, 0.0)).rgb;
    vec3 colUp     = texture2D(u_Texture, sampleUv + vec2(0.0, eps)).rgb;
    vec3 colDown   = texture2D(u_Texture, sampleUv - vec2(0.0, eps)).rgb;

    float lumC = getLuminance(colCenter);
    float lumR = getLuminance(colRight);
    float lumL = getLuminance(colLeft);
    float lumU = getLuminance(colUp);
    float lumD = getLuminance(colDown);

    // 金属のエッジ検出と疑似法線
    vec2 gradient = vec2(lumR - lumL, lumU - lumD);
    float edgeStrength = length(gradient);
    vec3 normal = normalize(vec3(-gradient * 10.0, 1.0));

    // 3. 傾き連動の金属スペキュラ（発光ではなく「反射」で見せる）
    vec2 lightPos = u_Tilt * 1.2 + vec2(cos(u_Time * 0.3), sin(u_Time * 0.25)) * 0.2;
    vec3 lightDir = normalize(vec3(lightPos, 0.9));
    vec3 viewDir = vec3(0.0, 0.0, 1.0);
    vec3 halfDir = normalize(lightDir + viewDir);
    float specAngle = max(dot(normal, halfDir), 0.0);
    float specular = pow(specAngle, 28.0) * smoothstep(0.1, 0.5, lumC) * smoothstep(0.02, 0.3, edgeStrength) * 1.5;

    // 4. 解錠シャッター（u_ShutterTime 連動の一過性演出）
    float shutterEffect = 0.0;
    if (u_ShutterTime >= 0.0 && u_ShutterTime <= T_DECAY_END) {
        float progress = u_ShutterTime / T_DECAY_END;
        // シャッター開放時に冷たい白銀の光が装甲のエッジを走る
        float scanLine = smoothstep(0.0, 0.15, abs(sampleUv.y - progress));
        shutterEffect = (1.0 - progress) * (1.0 - scanLine) * 0.85;
    }

    // 5. うねりパルス（センサー連動）
    float pulseEffect = 0.0;
    if (u_WaveIntensity > 0.001) {
        float waveRing = sin(length(sampleUv - vec2(0.5)) * 30.0 - u_Time * 12.0);
        pulseEffect = waveRing * u_WaveIntensity * (1.0 - u_WaveProgress) * 0.3;
    }

    // 6. 最終合成（黒ベース＋反射スペキュラ＋シャッター／パルス演出）
    vec3 finalColor = colCenter + vec3(specular) + vec3(shutterEffect + max(pulseEffect, 0.0));

    // 下3分の1を暗く保つ設計（AGENTS.md 4章）
    float vignette = smoothstep(0.0, 0.85, sampleUv.y);
    finalColor *= mix(0.75, 1.0, vignette);

    gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}