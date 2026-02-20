package com.gadgeski.igniter.opengl

import android.content.Context
import android.opengl.GLES20
import android.util.Log

/**
 * GLSLシェーダーのコンパイルとプログラムのリンクを行うユーティリティ。
 */
object ShaderHelper {

    private const val TAG = "ShaderHelper"

    /**
     * res/raw/ からシェーダーのソースコードを読み込む。
     */
    fun loadShaderSource(context: Context, resourceId: Int): String {
        return context.resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }

    /**
     * シェーダーをコンパイルする。
     * @param shaderType GLES20.GL_VERTEX_SHADER または GLES20.GL_FRAGMENT_SHADER
     * @param shaderSource GLSLソースコード文字列
     * @return コンパイル済みシェーダーのハンドル。失敗時は 0 を返す。
     */
    fun compileShader(shaderType: Int, shaderSource: String): Int {
        val shaderId = GLES20.glCreateShader(shaderType)
        if (shaderId == 0) {
            Log.e(TAG, "glCreateShader failed for type: $shaderType")
            return 0
        }

        GLES20.glShaderSource(shaderId, shaderSource)
        GLES20.glCompileShader(shaderId)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            val infoLog = GLES20.glGetShaderInfoLog(shaderId)
            Log.e(TAG, "Shader compile error:\n$infoLog")
            GLES20.glDeleteShader(shaderId)
            return 0
        }

        return shaderId
    }

    /**
     * 頂点シェーダーとフラグメントシェーダーをリンクしてプログラムを作成する。
     * @return リンク済みプログラムのハンドル。失敗時は 0 を返す。
     */
    fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
        val programId = GLES20.glCreateProgram()
        if (programId == 0) {
            Log.e(TAG, "glCreateProgram failed")
            return 0
        }

        GLES20.glAttachShader(programId, vertexShaderId)
        GLES20.glAttachShader(programId, fragmentShaderId)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)

        if (linkStatus[0] == 0) {
            val infoLog = GLES20.glGetProgramInfoLog(programId)
            Log.e(TAG, "Program link error:\n$infoLog")
            GLES20.glDeleteProgram(programId)
            return 0
        }

        // シェーダー単体はプログラムに紐付いたので削除可能
        GLES20.glDeleteShader(vertexShaderId)
        GLES20.glDeleteShader(fragmentShaderId)

        return programId
    }

    /**
     * res/raw/ から読み込み→コンパイル→リンクを一括で行うヘルパー。
     * @return プログラムハンドル。失敗時は 0。
     */
    fun buildProgram(
        context: Context,
        vertexShaderResId: Int,
        fragmentShaderResId: Int
    ): Int {
        val vertSrc = loadShaderSource(context, vertexShaderResId)
        val fragSrc = loadShaderSource(context, fragmentShaderResId)

        val vertId = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        if (vertId == 0) return 0

        val fragId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        if (fragId == 0) {
            GLES20.glDeleteShader(vertId)
            return 0
        }

        return linkProgram(vertId, fragId)
    }
}
