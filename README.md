# DroidCode IDE 🚀
**Full-Featured Code Editor (VS Code Like) for Android.**  
Built with **Kotlin, Jetpack Compose (Material 3 Adaptive), Monaco Editor (WebView), LSP (Remote), JGit, SSHJ, QuickJS**.

> **Status:** Foundation Complete (Architecture, DI, Build, Core Interfaces).  
> **Next:** Core Feature Implementation (SAF File Explorer, Terminal UI, LSP Diagnostics, Git Diff).

---

## ✨ Fitur Arsitektur (Sudah Siap)

| Layer | Tech | Status |
| :--- | :--- | :--- |
| **UI Shell** | Compose Material 3 Adaptive (Phone/Tablet/Foldable) | ✅ Done |
| **Editor Engine** | Monaco Editor 0.48 via WebView (CDN) | ✅ Bridge Ready |
| **IntelliSense (LSP)** | JSON-RPC Client -> Remote (Codespaces/SSH/code-server) | ✅ Interface & Impl Ready |
| **Terminal** | Local PTY (Termux/sh) + SSH Client (SSHJ) | ✅ Interface & Impl Ready |
| **Git** | JGit (Pure Kotlin) - Clone, Commit, Push, Diff, Log | ✅ Interface & Impl Ready |
| **Extensions** | QuickJS Engine (VS Code API Subset Mock) | ✅ Host Ready |
| **Database** | Room (Projects, Settings) | ✅ Done |
| **DI** | Hilt (KSP) | ✅ Done |
| **Build** | Gradle KTS (Version Catalog), GitHub Actions (Sign/Release) | ✅ Done |

---

## 🚀 Quick Start (Build di HP via GitHub Actions)

Karena lo build di HP, **jangan** clone project ini ke HP. Ikuti alur ini:

### 1. Fork / Push ke GitHub Repo Lo Sendiri
```bash
git clone https://github.com/LO_USERNAME/DroidCodeIDE.git
cd DroidCodeIDE
# Copy semua file dari artifact ini ke folder ini
git add .
git commit -m "feat: initial foundation"
git push origin main
```

### 2. Setup Signing Key (WAJIB untuk Release APK)
1.  Generate Keystore di PC/Laptop (atau Termux kalau bisa):
    ```bash
    keytool -genkey -v -keystore droidcode-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias droidcode -storepass <PASSWORD> -keypass <PASSWORD> -dname "CN=DroidCode, OU=IDE, O=DroidCode, L=City, S=State, C=ID"
    ```
2.  Convert ke Base64:
    ```bash
    # Linux/Mac
    base64 -i droidcode-release.jks | pbcopy
    # Windows (PowerShell)
    [Convert]::ToBase64String([IO.File]::ReadAllBytes("droidcode-release.jks")) | clip
    ```
3.  Buka GitHub Repo Lo -> **Settings > Secrets and variables > Actions > New Repository Secrets** (Buat 4 buah):
    *   `KEYSTORE_BASE64` : Paste hasil base64 tadi.
    *   `KEYSTORE_PASSWORD` : Password keystore.
    *   `KEY_ALIAS` : `droidcode`
    *   `KEY_PASSWORD` : Password key (bisa sama).

### 3. Trigger Build
**Opsi A: Otomatis (Tagging)**
```bash
git tag v1.0.0
git push origin v1.0.0
```
**Opsi B: Manual (Workflow Dispatch)**
Buka tab **Actions** di GitHub -> Pilih `Build & Release APK/AAB` -> `Run workflow` -> Pilih `release` -> `Run workflow`.

### 4. Download & Install
*   Tunggu workflow selesai (5-10 menit).
*   Buka **Releases** di repo GitHub lo.
*   Download `DroidCodeIDE-v1.0.0-release.apk`.
*   Transfer ke HP (via Kabel/Drive/Telegram) -> Install.

---

## 🛠 Development Lokal (Kalau nanti punya PC/Laptop)

```bash
# 1. Copy template
cp local.properties.template local.properties

# 2. Edit local.properties isi sdk.dir & signing config (opsional untuk debug)

# 3. Run Debug
./gradlew installDebug

# 4. Run Lint/Test
./gradlew lintDebug testDebugUnitTest
```

---

## 📁 Struktur Project Penting

```text
projects/DroidCodeIDE/
├── .github/workflows/build.yml
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/strings.xml
│       │   ├── values/themes.xml
│       │   ├── values/colors.xml
│       │   └── xml/data_extraction_rules.xml, backup_rules.xml
│       └── java/com/droidcode/ide/
│           ├── DroidCodeApplication.kt
│           ├── di/NetworkModule.kt, DatabaseModule.kt
│           ├── data/db/ (Entity, Dao, Database)
│           ├── editor/ (MonacoBridge, EditorWebView, EditorViewModel)
│           ├── lsp/ (LspClient, LspClientImpl)
│           ├── terminal/ (TerminalSession, TerminalSessionImpl)
│           ├── git/ (GitManager, GitManagerImpl)
│           ├── extensions/ (ExtensionHost, ExtensionHostImpl)
│           ├── ui/
│           │   ├── main/ (MainActivity, MainViewModel, MainScreen)
│           │   └── theme/ (Theme, Type, Color)
│           └── ...
├── build.gradle.kts
├── settings.gradle.kts
├── local.properties.template
└── README.md
```

---

## 🗺️ Roadmap Selanjutnya (Phase 2: Core Features)

1.  **SAF File Explorer (Sidebar)**: DocumentFile Tree, Context Menu, Drag & Drop, Persist URI Permission.
2.  **Editor Polish**: Tab Bar, Split View, Minimap, **LSP Diagnostics Rendering** (Squiggly lines), **LSP Completion/Hover**.
3.  **Terminal UI (Panel)**: xterm.js di WebView (ANSI Color, Cursor), Session Management, Keyboard Helper.
4.  **Git UI (Sidebar)**: File Status Badges, Changes List, Commit Input, Diff Viewer.
5.  **Command Palette (Ctrl+Shift+P)**: Fuzzy Search Commands.
6.  **Settings (JSON)**: `settings.json` mirror VS Code.

---

## ❓ Arsitektur LSP / Monaco Bridge (Detail)

### Alur Data: User Ketik -> Monaco (JS) -> Bridge (Kotlin) -> LSP Server (Remote)

1.  **Monaco (WebView/JS)**:
    *   `onDidChangeModelContent` event trigger.
    *   Kirim JSON via `window.MonacoBridge.onContentChanged(jsonString)`.
    *   JSON: `{uri, content, version}`.

2.  **MonacoBridge (Kotlin - `@JavascriptInterface`)**:
    *   Terima `onContentChanged`.
    *   Parse JSON (Gson/JsonParser) di `Dispatchers.IO`.
    *   Update cache model lokal (`editorModels`).
    *   Forward ke `LspClient.notifyDidChange(uri, content, version)`.

3.  **LspClientImpl (Kotlin)**:
    *   Bangun JSON-RPC Notification `textDocument/didChange`.
    *   Kirim via **WebSocket** (OkHttp) ke LSP Server (code-server/GitHub Codespaces/SSH Port Forward).
    *   *Catatan: Koneksi WebSocket dibuka saat `connect(serverUrl)` dipanggil (misal dari Settings/Command Palette).*

4.  **LSP Server (Remote)**:
    *   Proses perubahan, update AST, hitung Diagnostics/Errors.
    *   Kirim balik Notification `textDocument/publishDiagnostics` via WebSocket.

5.  **LspClientImpl (Kotlin)**:
    *   Terima `onMessage` (WebSocketListener).
    *   Parse `publishDiagnostics` -> Extract `uri`, `diagnostics` array.
    *   **Kembali ke Main Thread** -> `webView.evaluateJavascript(...)` untuk memanggil Monaco API: `monaco.editor.setModelMarkers(model, 'lsp', markers)`.

6.  **Monaco (JS)**:
    *   Render squiggly lines (merah/kuning) di editor.

### Alur Completion / Hover (Request-Response)

1.  **Monaco (JS)**: User tekan `Ctrl+Space` -> `provideCompletionItems` dipanggil (Monaco Extension API).
2.  **Bridge**: Karena Monaco Extension API berjalan di WebWorker (isolated), kita **tidak bisa** panggil `@JavascriptInterface` langsung dari provider.
    *   **Solusi**: Monaco `editor.action.triggerSuggest` -> UI Kotlin handle -> Kirim `textDocument/completion` via LSP Client -> Tunggu Response -> Inject ke Monaco via `editor.executeEdits` atau custom widget.
    *   *Atau*: Gunakan `monaco.languages.registerCompletionItemProvider` di JS yang memanggil `window.MonacoBridge.requestCompletion(...)` (Async via Promise/Callback).
3.  **LspClient**: Kirim Request `textDocument/completion` (dengan `id`).
4.  **Server**: Balas Response `id`, `result: {items: [...]}`.
5.  **Bridge**: Resolve Promise/Callback di JS -> Return `CompletionList` ke Monaco.

### Kenapa WebView + Monaco? (Bukan Native Compose Editor)
| Fitur | Native Compose (BasicTextField2) | Monaco (WebView) |
| :--- | :--- | :--- |
| **Syntax Highlight** | Manual (Regex/Tree-sitter JNI) - **Bulan** | Built-in (Monarch/Tree-sitter WASM) - **Siap** |
| **Bracket Match/Pair Color** | Manual - **Sulit** | Built-in - **Siap** |
| **Minimap** | Custom Canvas - **Minggu** | Built-in - **Siap** |
| **Multi-cursor** | Complex State - **Minggu** | Built-in - **Siap** |
| **LSP UI (Hover/Signature)** | Custom Popup/Overlay - **Minggu** | Built-in Widgets - **Siap** |
| **Extensions (VS Code)** | Impossible | **Bisa (QuickJS/WASM)** |
| **Performance (HP)** | Lebih Ringan (Native) | Cukup Cepat (V8 Engine), RAM ~50-80MB |

**Kesimpulan**: Monaco di WebView adalah **satu-satunya cara realistis** bikin "VS Code di Android" oleh 1 orang dev.

---

## 🔐 Keamanan & Best Practice
*   **Scoped Storage (SAF)**: Tidak minta `MANAGE_EXTERNAL_STORAGE` (Play Store Reject). Pakai `ACTION_OPEN_DOCUMENT_TREE` + `takePersistableUriPermission`.
*   **Cleartext Traffic**: `android:usesCleartextTraffic="true"` hanya untuk `localhost` tunneling (ngrok/Cloudflare) ke LSP Server lokal. **Produksi**: Gunakan HTTPS/WSS.
*   **SSH Host Key**: `PromiscuousVerification` hanya untuk MVP. **Produksi**: Implement `KnownHosts` verification.
*   **ProGuard/R8**: Sudah dikonfigurasi ketat untuk Hilt, Room, JGit, QuickJS, BouncyCastle.