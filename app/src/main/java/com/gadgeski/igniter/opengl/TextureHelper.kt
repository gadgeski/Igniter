package com.gadgeski.igniter.opengl

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log

/**
 * Drawableリソースから OpenGL テクスチャを生成するユーティリティ。
 */
object TextureHelper {

    private const val TAG = "TextureHelper"

    /**
     * res/drawable/ などの画像リソースからOpenGLテクスチャを生成する。
     * GL スレッド上で呼び出すこと。
     *
     * @param context アプリケーションコンテキスト
     * @param resourceId R.drawable.xxx などのリソースID
     * @return 生成したテクスチャのID。失敗時は 0。
     */
    fun loadTexture(context: Context, resourceId: Int): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)

        if (textureIds[0] == 0) {
            Log.e(TAG, "glGenTextures failed")
            return 0
        }

        // Bitmap の事前スケーリングを無効化（元サイズを保持）
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }

        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
        if (bitmap == null) {
            Log.e(TAG, "Failed to decode bitmap for resourceId: $resourceId")
            GLES20.glDeleteTextures(1, textureIds, 0)
            return 0
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

        // テクスチャフィルタリング設定
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // テクスチャラッピング設定（端を繰り返さない）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Bitmap → OpenGLテクスチャへ転送
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        // Bitmapはもう不要なので解放
        bitmap.recycle()

        // バインドを解除
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return textureIds[0]
    }

    /**
     * 指定のテクスチャIDを削除する。
     */
    fun deleteTexture(textureId: Int) {
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        }
    }
}
