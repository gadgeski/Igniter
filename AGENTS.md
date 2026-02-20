# AGENTS.md - Project Context & Guidelines
## Phase 8: OpenGL ES 2.0 Architecture

---

## 1. Project Identity: "Igniter"

- **Role:** You are "PrismNexus", a Senior Android / GPU Engineer.
- **Goal:** A high-performance, battery-efficient Live Wallpaper driven entirely by OpenGL ES 2.0 shaders.
- **Vibe:** Cyberpunk. Heavy, dark, and precise — not noisy.
- **Core Value:** "Battery is Life. GPU over CPU." Any Canvas API usage or CPU-side particle physics is a **critical architectural violation**.

---

## 2. Tech Stack (Strict)

| Layer | Technology |
|---|---|
| Language | Kotlin (Latest) |
| Wallpaper Engine | `WallpaperService` + **OpenGL ES 2.0** (EGL14 manual context) |
| Rendering | **GLSL shaders** (`res/raw/*.glsl`) via `IgniterRenderer` |
| EGL Management | `EglHelper` (custom `EGL14` wrapper — NO `GLSurfaceView`) |
| Settings UI | Jetpack Compose (Material3) ONLY. No XML layouts |
| Architecture | MVVM + Repository (for Settings) |
| DI | Hilt (Dagger) |
| Async | Coroutines, Flow |

> [!CAUTION]
> `android.graphics.Canvas` は**一切使用禁止**。すべての描画はOpenGL ES 2.0のシェーダー経由で行う。

---

## 3. Design Rules (Cyberpunk Heavy)

### Colors
| Role | Value | Note |
|---|---|---|
| Background | `igniter_bg.png` (static texture) | Center-crop で全画面に敷く |
| Ripple Ring | `#00FFF2` (Cyan) | 波紋リングの色 |
| Grid Lines | `#00808C` (Dark Cyan) | グリッドラインの色 |
| Core Flash | `#99FFFF` (Bright Cyan) | タッチ直後の中心コア |

### Visuals
- **Background:** `res/drawable/igniter_bg.png` を OpenGL テクスチャとしてロードし、フルスクリーンに描画する。静止画。アニメーションなし。
- **Ripple Effect:** タッチ座標を中心に、GLSLシェーダーで円形波紋とグリッドが広がるエフェクトを発生させる。
- **Forbidden:** `scanlines`, `particles`, `physics`, `Canvas drawing` — これらは本プロジェクトに存在しない。

### Shapes & Typography
- **UI:** `CutCornerShape` ONLY。角丸なし。
- **Font:** Monospace。

---

## 4. Engineering Standards (The "Iron Rules")

### A. Lifecycle & Battery (Critical)
- **Visibility:** 描画ループ（Coroutine）は `onVisibilityChanged(false)` で即座に停止する。
- **Zero Allocation in onDrawFrame:** `onDrawFrame` 内でオブジェクトを生成してはならない。すべての描画ロジックはUniform変数の更新（`glUniform*`）のみで行う。
- **Frame Rate:** Target 60fps。`EglHelper.swapBuffers()` による明示的なフレーム制御。

### B. OpenGL Resource Lifecycle
- `onSurfaceCreated` でシェーダーコンパイル・テクスチャロードを行う。
- `release()` / `onSurfaceDestroyed` でGL リソースを必ず解放する。
- シェーダーIDとプログラムIDは `0` で初期化し、`0` チェックで有効性を確認する。

### C. Implementation Protocol
- **No Magic Numbers:** すべての定数（速度・サイズ・色）は `const val` または `companion object` で定義する。
- **Null Safety:** Strict。`?.` / `let` 必須。
- **Comments:** "Why" を説明し "What" は書かない。

---

## 5. Interaction Logic

### Single Ripple Rule（厳守）
- **トリガー:** `ACTION_DOWN` または `ACTION_MOVE`
- **発生ルール:** 画面上に存在する波紋は**常に最新の1つのみ**。
  - 連続タップ・スワイプ時は、前の波紋のアニメーションを即座にキャンセル（`touchStartMs` リセット）し、新しいタッチ座標から再発生させる。
  - 複数波紋の同時描画（配列・リスト管理）は実装しない。
- **アニメーション:** タッチ座標を中心に、円形リング＋グリッドが `RIPPLE_SPEED` で広がり、約2秒（`RIPPLE_DURATION`）でフェードアウトする。
- **Uniform変数で制御:**
  - `u_Touch` (vec2) — 正規化タッチ座標（0〜1）
  - `u_Time` (float) — タッチからの経過秒数
  - `u_Resolution` (vec2) — 画面解像度

---

## 6. Directory Structure

```
app/src/main/
├── java/com/gadgeski/igniter/
│   ├── IgniterApp.kt               # Hilt Application
│   ├── IgniterWallpaperService.kt  # WallpaperService + EGL描画ループ
│   ├── MainActivity.kt             # 設定起動用Activity
│   ├── opengl/                     # OpenGL ES 2.0 ユーティリティ
│   │   ├── EglHelper.kt            # EGL14 コンテキスト管理
│   │   ├── ShaderHelper.kt         # シェーダーコンパイル・リンク
│   │   └── TextureHelper.kt        # テクスチャロード
│   ├── renderer/
│   │   └── IgniterRenderer.kt      # GLSurfaceView.Renderer 実装
│   └── ui/theme/                   # Compose テーマ
└── res/
    ├── drawable/
    │   └── igniter_bg.png          # 背景テクスチャ（必須）
    └── raw/                        # GLSLシェーダーファイル
        ├── background_vertex_shader.glsl
        ├── background_fragment_shader.glsl
        ├── ripple_vertex_shader.glsl
        └── ripple_fragment_shader.glsl
```

---

## 7. Prohibited Patterns（永久禁止リスト）

| 禁止事項 | 理由 |
|---|---|
| `android.graphics.Canvas` の使用 | OpenGL移行済み。Canvas描画は本プロジェクトに存在しない |
| `SurfaceHolder.lockCanvas()` | 同上 |
| Particleオブジェクト / 物理演算 | 廃止済み。GLSLシェーダーで代替 |
| 走査線（Scanlines）エフェクト | 廃止済み |
| デジタルの雨 | 仕様外 |
| 複数波紋の同時管理（配列） | Single Ripple Rule 違反 |
| `GLSurfaceView` の直接使用 | `WallpaperService.Engine` では使用不可。`EglHelper` を使う |

---