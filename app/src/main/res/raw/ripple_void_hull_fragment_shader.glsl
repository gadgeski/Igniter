precision mediump float;

varying vec2 v_TexCoord;

uniform vec2 u_Touch;
uniform float u_Time;
uniform vec2 u_Resolution;

const float RIPPLE_DURATION = 2.0;

void main() {
    vec2 uv = v_TexCoord;
    vec2 aspectResolution = u_Resolution;
    if (aspectResolution.y > 0.0) {
        uv.x *= aspectResolution.x / aspectResolution.y;
        vec2 adjustedTouch = u_Touch;
        adjustedTouch.x *= aspectResolution.x / aspectResolution.y;

        float dist = distance(uv, adjustedTouch);
        float timeFactor = u_Time / RIPPLE_DURATION;

        if (timeFactor <= 1.0 && dist < 0.45) {
            float wave = sin(dist * 50.0 - u_Time * 14.0);
            float alpha = (1.0 - timeFactor) * smoothstep(0.45, 0.0, dist) * max(wave, 0.0) * 0.45;

            // 冷徹なシアン・ホワイトの波紋
            vec3 rippleColor = vec3(0.6, 0.85, 1.0);
            gl_FragColor = vec4(rippleColor * alpha, alpha);
            return;
        }
    }
    gl_FragColor = vec4(0.0);
}