# Igniter ⚡️

_A GPU-driven cyberpunk live wallpaper for Android — 25 themes, rendered live in GLSL._

<!-- TODO: 実機で触っている様子（タップ・傾き・解錠シャッター）の GIF をここに置く -->

## Overview

**Igniter** は、OpenGL ES 2.0 と GLSL シェーダーだけで描画する Android ライブ壁紙です。

動画をループ再生するのではなく、毎フレームを GPU で描き直しています。そのため、指先のタップ、端末の傾き、画面ロックの解除といった「その瞬間の状態」に反応できます。

コンセプトは **"Heavy, dark, and precise — not noisy."**
普段はアイコンの邪魔をしない暗く静かな背景として一歩引き、解錠などの一瞬だけ主役になる壁紙を目指しています。

## Features

- **25 Themes:** 隔壁、回路、廃墟の摩天楼、夜の街など、彩度帯と光り方の異なるテーマを収録。設定画面からその場で切り替えられ、サービスの再起動は不要です。

- **Unlock Shutter:** 画面ロックを解除した瞬間、壁紙そのものが斜めに割れて冷たい光が漏れ出し、再び閉じる約2秒のシーケンスを再生します（BULKHEAD_FROST）。

- **Heat Haze:** 霞んだ空の明るく暖かい部分だけを抽出し、陽炎のように揺らします。手前の鉄骨や焼き込まれたグリッチ枠は動かしません（CYBERPUNK）。

- **Touch Ripples:** タップ・スワイプに合わせて SDF ベースの波紋とグリッドが広がります。波紋の色と形はテーマの光の温度に合わせて変わります。

- **Tilt Parallax & Motion:** 加速度センサーをローパスフィルタで平滑化し、背景を傾きに追従させます。端末を大きく動かすと、テーマに応じてうねりやグリッチが走ります。

- **Battery First:** 非表示時は描画ループとセンサーを完全に停止。表示中も操作がなければ 60 → 30 → 15fps と段階的にフレームレートを落とします。

## Tech Stack

- **Language:** Kotlin
- **Graphics API:** OpenGL ES 2.0 / GLSL（EGL14 を手動管理）
- **Settings UI:** Jetpack Compose (Material3)
- **Concurrency:** Kotlin Coroutines
- **Dependency Injection:** Dagger Hilt
- **Assets:** WebP（全テーマに一覧用の専用サムネイルを用意）

## Architecture Highlights

### 1. Single-Thread EGL Lifecycle

`WallpaperService.Engine` は `GLSurfaceView` を持てないため、EGL14 のコンテキストとサーフェスを自前で管理しています。`newSingleThreadExecutor` に束縛した `CoroutineScope` に GL 処理をすべて集約し、プレビューから本番適用への移行やテーマ切り替えでもコンテキストを失わない構成にしています。タッチ・センサー・解錠イベントは一度 pending 状態に積み、GL スレッド側で反映します。

### 2. Theme = Texture × Shaders × Motion Profile

各テーマは「背景テクスチャ」「背景シェーダー」「波紋シェーダー」「モーションのパラメータ」の組み合わせとして enum に定義しています。シェーダーはテーマ間で共有でき、絵に合わせて作り込んだものは専用として切り出しています。

### 3. Opt-in Uniforms

Renderer は、シェーダーが宣言している uniform だけを更新します。解錠シャッターも `u_ShutterTime` を宣言したシェーダーでだけ有効になるため、機能を足してもテーマ定義にフラグを増やす必要がありません。

### 4. Adaptive Frame Rate

最終操作からの経過時間で 60 / 30 / 15fps を切り替えます。シャッターのような一過性の演出の間だけ 60fps を維持し、終われば静かに落ちていきます。

## Development Notes

実機で使いながら見つけた問題と、その解決です。

### 揺らすと画像の端が伸びる

パララックスや揺らぎの演出では、テクスチャを読む位置（UV）をずらします。そのままでは画面の端で画像の外側を読みに行き、端のピクセルが引き伸ばされて見えていました。

- シェーダー側では、画像の内側 3.5%（`UV_MARGIN`）だけを使ってサンプリングし、ずらしても余白の範囲に収まるようにしました。
- 素材側では、流通している壁紙アプリの画像サイズを参考に 1400×3100 を基準とし、端に重要な要素を置かない構図で作るようにしました。

### テーマが増えて設定画面が引っかかる

この対策で素材を作りやすくなり、テーマの数が増えました。すると今度は、設定画面を開いたときにスクロールが引っかかるようになりました。一覧の小さなセルのために、毎回フルサイズの壁紙をデコードしていたのが原因です。

- 全テーマに 298×660 の一覧専用サムネイルを用意しました。
- デコード後のサイズは 1 枚あたり約 17MB から約 0.8MB になり、およそ 22 分の 1 です（ARGB 4 バイト/ピクセルでの計算値）。

## Getting Started

### Prerequisites

- Android Studio (Latest Version)
- Android 11 (API 30) or higher
- OpenGL ES 2.0 support

### Build & Install

1. Clone this repository.
2. Open the project in Android Studio.
3. Sync project with Gradle files.
4. Run the app on a physical device (live wallpapers and sensors are unreliable on emulators).
5. Tap **Set Wallpaper** in the app, or choose **Igniter** from your device's wallpaper settings.
