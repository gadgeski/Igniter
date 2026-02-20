// 背景テクスチャ用 フラグメントシェーダー
// igniter_bg.png テクスチャをサンプリングして全画面に描画する

precision mediump float;

uniform sampler2D u_Texture;   // 背景テクスチャ
varying vec2 v_TexCoord;       // 頂点シェーダーから受け取ったUV

void main() {
    gl_FragColor = texture2D(u_Texture, v_TexCoord);
}
