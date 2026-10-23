# AGENTS.md - Project Context & Guidelines

## Phase 9: Multi-Theme OpenGL ES 2.0 Architecture

---

## 1. Project Identity: "Igniter"

- **Role:** You are "PrismNexus", a Senior Android / GPU Engineer.
- **Goal:** A high-performance, battery-efficient Live Wallpaper driven entirely by OpenGL ES 2.0 shaders, shipped as a catalog of themes.
- **Vibe:** Cyberpunk. Heavy, dark, and precise — not noisy.
- **Core Value:** "Battery is Life. GPU over CPU." Any Canvas API usage or CPU-side particle physics is a **critical architectural violation**.

> [!IMPORTANT]
> 本ドキュメントとコードが食い違う場合、**コードを勝手に本ドキュメントへ合わせない**こと。まず食い違いを報告し、どちらを正とするか確認する。

---

## 2. Tech Stack (Strict)

| Layer            | Technology                                                              |
| ---------------- | ----------------------------------------------------------------------- |
| Language         | Kotlin (Latest)                                                         |
| Wallpaper Engine | `WallpaperService` + **OpenGL ES 2.0** (EGL14 manual context)           |
| Rendering        | **GLSL shaders** (`res/raw/*.glsl`) via `IgniterRenderer`               |
| EGL Management   | `EglHelper` (custom `EGL14` wrapper — NO `GLSurfaceView`)               |
| Settings UI      | Jetpack Compose (Material3) ONLY. No XML layouts                        |
| Settings Storage | `SharedPreferences`（`igniter_prefs` / key `selected_theme`）を直接使用 |
| DI               | Hilt (Dagger)                                                           |
| Async            | Coroutines, Flow                                                        |

> [!CAUTION]
> `android.graphics.Canvas` は**一切使用禁止**。すべての描画はOpenGL ES 2.0のシェーダー経由で行う。

> [!NOTE]
> 設定は「テーマを1つ選んで保存する」だけの規模のため、ViewModel / Repository 層は導入しない。設定項目が増えて状態管理が複雑になった時点で再検討する。

---

## 3. Theme System

Igniter は「背景テクスチャ1枚 × 背景シェーダー × 波紋シェーダー × モーションプロファイル」の組み合わせをテーマとして持つ。

### A. 定義

- すべてのテーマは `settings/WallpaperTheme.kt` の enum に定義する。
- 各テーマが持つもの:
  - `backgroundDrawableRes` / `thumbnailDrawableRes`
  - `backgroundFragmentShaderRes` / `rippleFragmentShaderRes`
  - Motion profile: `waveBoostScale`, `minVisibleWaveAmplitude`, `maxWaveAmplitude`, `waterPulseDurationSec`, `minWaveRetriggerMs`
- シェーダーはテーマ間で**共有してよい**。既存シェーダー＋パラメータで出せる見た目なら、新規シェーダーを作らない。

### B. テーマ切り替え

- 選択テーマは `SharedPreferences` に保存し、`IgniterWallpaperService` のリスナーが GL スレッド上で `IgniterRenderer.setTheme()` を呼ぶ。
- シェーダーのコンパイル・リンクとテクスチャロードは **`setTheme()` 内**で行う（`onSurfaceCreated` ではない）。前テーマのリソースは `release()` で解放してから作り直す。

### C. Opt-in Uniform 方式

- Renderer は uniform の location が `-1` なら何もしない。シェーダーが**宣言した uniform だけが機能として有効になる**。
- 新しい演出を足すときもこの方式に従い、enum にフラグを増やさない。

| Uniform                                                    | 対象        | 意味                                 |
| ---------------------------------------------------------- | ----------- | ------------------------------------ |
| `u_Texture`, `u_Tilt`, `u_Time`                            | 背景        | 基本（全背景シェーダー）             |
| `u_WaveIntensity`, `u_WaveProgress`, `u_EnableWaterMotion` | 背景        | センサー由来のうねりパルス           |
| `u_Resolution`                                             | 背景 / 波紋 | 画面解像度 (px)                      |
| `u_ShutterTime`                                            | 背景        | 解錠シャッターの経過秒。負値は停止中 |
| `u_Touch`, `u_Time`                                        | 波紋        | タッチ座標・タッチからの経過秒       |

### D. 解錠シャッター

- `ACTION_USER_PRESENT` を受けて `IgniterRenderer.startShutter()` を発火する。`u_ShutterTime` を宣言していないテーマでは何も起きない。
- 要求は `SHUTTER_REQUEST_TTL_MS` で失効させる（解錠後にアプリへ遷移し、後でホームに戻った瞬間に誤再生しないため）。
- プレビュー (`isPreview`) は解錠を受け取れないため、表示のたびに再生する。
- シーケンス長は `IgniterRenderer.SHUTTER_DURATION_SEC` とシェーダー側 `T_DECAY_END` を**必ず一致させる**。
- 実装例: `bg_bulkhead_fragment_shader.glsl`（BULKHEAD_FROST）

### E. テーマ専用シェーダー

- `bg_haze_fragment_shader.glsl` は CYBERPUNK 専用。明るく暖かい空だけを抽出するマスク（輝度・暖色・縦位置）をこの画像に合わせて作り込んでいるため、他テーマに流用しない。寒色のテーマでは何も起きず、暖色の発光ラインでは線が揺らいで見える。
- `bg_bulkhead_fragment_shader.glsl` も同様に BULKHEAD_FROST 専用（継ぎ目の位置が画像の構図に依存）。

---

## 4. Design Rules (Cyberpunk Heavy)

### Composition

- **下3分の1は暗く保つ。** ホーム画面のアイコン・ドックの視認性が最優先。
- 壁紙は普段は一歩引き、主役を奪ってよいのは解錠などの**一瞬のイベントだけ**。常時動き続ける派手な演出は入れない。
- 演出は「調味料」。存在を主張し始めたら雰囲気は壊れている。

### Color Balance（テーマ追加・改修時に必ず確認）

- 大半のテーマは「黒地に高彩度のアクセント色」という同じ構造。新テーマは**色相だけでなく彩度帯・光り方**で差をつける。
- **飽和している帯域:** オレンジ〜アンバー（AMBER_IRIS, IGNITION_LINE, EMBER_LINE, CRIMSON_WINDOW）、紫〜マゼンタ（MAGENTA_CORE, VIOLET_VENT, IRIS_CORE）。新規追加せず、作りたくなったら既存テーマの改修に回す。
- **価値のある帯域:** 白〜青白い無彩色の発光（BULKHEAD_FROST）、発光ではなく反射で見せる金属（VOID_HULL）。
- 波紋の色はテーマの光の温度に合わせる。冷たい光のテーマに標準のオレンジ波紋を流用しない。

### Overlay Effects（走査線・グリッチ・デジタルの雨など）
- **画像への焼き込み**: 可。描画コストは通常の背景と変わらない。
- **シェーダーでの演出**: トリガー時だけの一過性の演出として可（例: `bg_splashing_ink` は `glitchStrength` が立った時だけ走査線を出す）。
- **常時動き続けるオーバーレイ**: 禁止。過去にデジタルの雨を常時描画した際、時間経過で表示が崩れ、リソースを消費し続けたため。

### Settings UI

- 既存の `ui/theme/` と各 Activity の見た目（サムネイルの角丸、`FontFamily.Default`）を正とする。
- 新しい Shape・フォント・配色体系を持ち込む場合は、実装前に確認する。

---

## 5. Assets

- 背景: `res/drawable/bg_<name>.webp`、サムネイル: `bg_<name>_thumb.webp`。`<name>` は enum 名の小文字スネークケース。
- **全テーマに専用サムネイルを用意する。** 本体画像を流用すると、一覧表示のたびにフルサイズをデコードする。
  - 未対応（本体流用中）: COBALT_CIRCUIT, CHARCOAL_DRIFT
- 背景シェーダーは UV を `UV_MARGIN` 分だけ内側でサンプリングし、パララックスで端が引き伸ばされないようにする。

---

## 6. Engineering Standards (The "Iron Rules")

### A. Lifecycle & Battery (Critical)

- **Visibility:** 描画ループ（Coroutine）とセンサーは `onVisibilityChanged(false)` で即座に停止する。
- **Zero Allocation in onDrawFrame:** `onDrawFrame` 内でオブジェクトを生成してはならない。描画ロジックは Uniform 更新（`glUniform*`）のみで行う。
- **Adaptive Frame Rate:** 固定60fpsにしない。最終操作からの経過時間で切り替える。
  - ACTIVE 60fps（`ACTIVE_MODE_KEEP_MS` まで）→ IDLE 30fps（`IDLE_MODE_KEEP_MS` まで）→ DEEP_IDLE 15fps
  - 一過性の演出（シャッター等）を始めるときは `lastInteractionMs` を更新し、演出中は ACTIVE を維持する。
- フレーム制御は `EglHelper.swapBuffers()` と `delay(...milliseconds)` で行う。

### B. Threading

- GL 呼び出しはすべて単一の GL スレッド（`newSingleThreadExecutor` に束縛した `CoroutineScope`）で行う。
- UI スレッド・センサー・BroadcastReceiver から Renderer を直接触らない。`pendingStateLock` 下の pending 状態に積み、`applyPendingRendererUpdates()` で GL スレッドに渡す。

### C. OpenGL Resource Lifecycle

- `release()` / `onSurfaceDestroyed` で GL リソースを必ず解放する。
- シェーダーIDとプログラムIDは `0`、uniform / attribute location は `-1` で初期化し、それぞれのチェックで有効性を確認する。

### D. Implementation Protocol

- **No Magic Numbers:** すべての定数（速度・サイズ・色・時間）は `const val` / `companion object`、GLSL では `const` で定義する。
- **Null Safety:** Strict。`?.` / `let` 必須。
- **Comments:** "Why" を説明し "What" は書かない。

---

## 7. Interaction Logic

### Single Ripple Rule（厳守）

- **トリガー:** `ACTION_DOWN` または `ACTION_MOVE`
- **発生ルール:** 画面上に存在する波紋は**常に最新の1つのみ**。
  - 連続タップ・スワイプ時は前の波紋を即座にキャンセル（`touchStartMs` リセット）し、新しいタッチ座標から再発生させる。
  - 複数波紋の同時描画（配列・リスト管理）は実装しない。
- **アニメーション:** 約2秒（`RIPPLE_DURATION`）でフェードアウト。形状・色は各テーマの波紋シェーダーが決める。

### Sensor

- 加速度センサーをローパスフィルタ（`ALPHA`）で平滑化し、`u_Tilt` としてパララックスに使う。
- 急な動きは `WAVE_TRIGGER_THRESHOLD` / `SENSOR_WAVE_COOLDOWN_MS` を経てうねりパルスになる。

---

## 8. Directory Structure

```
app/src/main/
├── java/com/gadgeski/igniter/
│   ├── IgniterApp.kt               # Hilt Application
│   ├── IgniterWallpaperService.kt  # WallpaperService / EGL描画ループ / センサー / 解錠受信
│   ├── MainActivity.kt             # 起動画面（現在のテーマ表示・壁紙設定への導線）
│   ├── opengl/
│   │   ├── EglHelper.kt            # EGL14 コンテキスト管理
│   │   ├── ShaderHelper.kt         # シェーダーコンパイル・リンク
│   │   └── TextureHelper.kt        # テクスチャロード
│   ├── renderer/
│   │   └── IgniterRenderer.kt      # 背景＋波紋の2パス描画、テーマ切り替え、シャッター
│   ├── settings/
│   │   ├── SettingsActivity.kt     # テーマ選択画面 (Compose)
│   │   └── WallpaperTheme.kt       # テーマ定義
│   └── ui/theme/                   # Compose テーマ
└── res/
    ├── drawable/
    │   ├── bg_<name>.webp          # 背景テクスチャ
    │   └── bg_<name>_thumb.webp    # 一覧用サムネイル
    └── raw/
        ├── background_vertex_shader.glsl
        ├── ripple_vertex_shader.glsl
        ├── bg_<variant>_fragment_shader.glsl      # 背景（テーマ間で共有可）
        └── ripple_<variant>_fragment_shader.glsl  # 波紋（テーマ間で共有可）
```

---

## 9. Commit Convention

- 形式: `<prefix>: <何をしたか>` ＋空行＋箇条書き。箇条書きには**見た目・明暗の配置・理由**を書く。
- Prefix: `assets:`（素材の追加・改修）, `feat:`（機能・テーマ追加）, `perf:`, `refactor:`, `docs:`
- 素材の追加とコードの変更はコミットを分ける。仕組み（Renderer / Service）と、それを使うテーマも分ける。

```
assets: add bg_bulkhead_frost and thumbnail

- industrial bulkhead panels around an octagonal frame with diagonal fins
- cold steel body with near-white glow cores, keeping the lower third dark
```

---

## 10. Prohibited Patterns（永久禁止リスト）

| 禁止事項                                                   | 理由                                                                                                              |
|------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `android.graphics.Canvas` の使用                           | 全描画は OpenGL ES 2.0                                                                                            |
| `SurfaceHolder.lockCanvas()`                               | 同上                                                                                                              |
| Particle オブジェクト / CPU側の物理演算                    | GLSL シェーダーで代替                                                                                             |
| 常時動き続けるオーバーレイ演出（走査線・デジタルの雨など） | 時間経過で表示が崩れ、リソースを消費し続ける。画像への焼き込み・一過性の演出は可（4章）                           |
| 複数波紋の同時管理（配列）                                 | Single Ripple Rule 違反                                                                                           |
| `GLSurfaceView` の直接使用                                 | `WallpaperService.Engine` では使用不可。`EglHelper` を使う（`GLSurfaceView.Renderer` インターフェースの実装は可） |
| GL スレッド以外からの GL 呼び出し                          | EGL コンテキスト喪失の原因                                                                                        |
| 演出の有無を示すフラグを enum に追加                       | Opt-in Uniform 方式（3章C）を使う                                                                                 |

---
