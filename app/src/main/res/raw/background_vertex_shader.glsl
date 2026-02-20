// 背景テクスチャ用 頂点シェーダー
// フルスクリーンクワッドの頂点座標とUV座標を出力する

attribute vec4 a_Position;   // 頂点座標 (NDC空間: -1〜+1)
attribute vec2 a_TexCoord;   // UV座標 (0〜1)

varying vec2 v_TexCoord;     // フラグメントシェーダーへ渡すUV

void main() {
    gl_Position = a_Position;
    v_TexCoord = a_TexCoord;
}
