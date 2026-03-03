# Igniter ⚡️

_A Cyberpunk Digital Sunset Live Wallpaper for Android_

## Overview

**Igniter** は、デバイスの物理的な動きとユーザーのタップに呼応する、高度に洗練されたAndroidライブ壁紙（Live Wallpaper）です。

漆黒のサイバースペースを舞台に、デバイスを傾けることで下部から滲み上がる「退廃的なデジタル・サンセット（ネオンオレンジ＆マゼンタ）」と、画面の奥へと滑る「パララックス（視差）効果」が、圧倒的な没入感を生み出します。

画面に触れれば、デジタルの光として洗練されたエレクトリック・オレンジの波紋が広がり、システム全体に熱を帯びさせます。

単なるループアニメーションではなく、ユーザーの環境（重力）と指先にリアルタイムで反応する、生きているデジタルアートです。

## Features

- **Electric Orange Ripples:** 画面をタップ・スワイプすると、微細なグリッド線の奥から純粋なネオンオレンジの波紋（SDFベース）が広がります。
    
- **Digital Sunset Gradient:** スマホを垂直に立てる（加速度センサーのY軸）ことで、画面下部からマゼンタとオレンジの毒々しい夕暮れがダイナミックに侵食します。
    
- **2.5D Parallax Scrolling:** デバイスの傾きに連動し、背景空間が滑らかに追従。独自にチューニングされたローパスフィルタ（Low-Pass Filter）が手ブレを吸収し、高級感のある立体空間を演出します。
    
- **Zero Battery Drain:** 壁紙が非表示（他のアプリを起動中やスリープ時）の際は、センサーのリスナーと描画ループを完全に停止し、バッテリー消費を極限まで抑えます。
    

## Tech Stack

- **Language:** Kotlin
    
- **Graphics API:** OpenGL ES 2.0 / GLSL (Vertex & Fragment Shaders)
    
- **Concurrency:** Kotlin Coroutines
    
- **Dependency Injection:** Dagger Hilt
    
- **Assets:** WebP (Highly optimized for VRAM)
    

## Architecture Highlights

Igniterは、見た目の美しさだけでなく、Androidの壁紙サービス特有の過酷な環境に耐えうる堅牢な基盤を持っています。

### 1. Single-Thread EGL Lifecycle Management

壁紙のプレビュー画面から本番適用へのシームレスな移行を実現するため、`CoroutineScope` に `newSingleThreadExecutor` をバインドし、**「EGLコンテキストを単一スレッドに完全に固定する」**独自のアーキテクチャを構築しています。これにより、非同期処理によるコンテキスト喪失（`no current context`）やリソースの解放漏れを完全に防ぎ、クラッシュ率0%の安定性を誇ります。

### 2. 100% GPU-Driven Rendering

ピクセルの計算、カラーブレンド、波紋のSDF演算、パララックスのUV座標オフセットなど、すべての視覚的演算をCPUではなく **GLSLフラグメントシェーダー** にオフロードしています。これにより、CPU負荷と電力消費を最小限に抑えつつ、常時安定した60fpsの描画を実現しています。

## Getting Started

### Prerequisites

- Android Studio (Latest Version)
    
- Android SDK 24 (Nougat) or higher
    

### Build & Install

1. Clone this repository.
    
2. Open the project in Android Studio.
    
3. Sync project with Gradle files.
    
4. Run the app on a physical device (Emulators may not fully support hardware-accelerated live wallpapers and sensors).
    
5. Follow the on-screen prompt or go to your device's Wallpaper settings to apply **Igniter**.