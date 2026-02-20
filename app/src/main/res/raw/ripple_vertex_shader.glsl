// デジタル波紋エフェクト用 頂点シェーダー
// フルスクリーンクワッドを出力し、UV座標をフラグメントシェーダーへ渡す

attribute vec4 a_Position;   // NDC空間の頂点座標
attribute vec2 a_TexCoord;   // UV座標 (0〜1)

varying vec2 v_TexCoord;

void main() {
    gl_Position = a_Position;
    v_TexCoord = a_TexCoord;
}
