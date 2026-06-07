# Bublit

Bublit is an Android web-comic translation reader prototype. It loads a web page in a WebView, extracts real DOM `<img>` elements, and opens an internal Compose reader. The current runnable build includes:

- Jetpack Compose + Material 3 app shell.
- URL input and WebView image extraction.
- Reader mode with original/translated toggle.
- Real extracted image display through Coil.
- Pure Kotlin domain pipeline tests for image filtering, language detection, speech bubble classification, and typeset planning.
- ML Kit OCR and translation adapters behind local-first interfaces.
- Fake OCR/translation fallback data so the app is runnable before model downloads are wired into the UI flow.

## Scope

Supported:

- Android app.
- Web pages that expose real `<img>` tags.
- English or Chinese source text toward Korean.
- Bright speech-bubble dialogue as the target rendering case.

Not supported:

- Canvas or JavaScript-only image viewers.
- Background lettering and sound effects.
- Full web page text translation.
- Artwork inpainting.

## Build

From PowerShell:

```powershell
.\scripts\build-debug.ps1
```

The script uses Android Studio's bundled JBR at:

```text
C:\Program Files\Android\Android Studio\jbr
```

The debug APK is produced at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Install

Connect a device or start an emulator, then run:

```powershell
.\scripts\install-debug.ps1
```

You can also open this folder in Android Studio and run the `app` configuration.

## Tests

```powershell
.\gradlew.bat testDebugUnitTest
```

## Current Runtime Flow

1. Enter a page URL.
2. Tap `Load`.
3. Bublit loads the page in a WebView and extracts large DOM image candidates.
4. If candidates are found, the reader displays them.
5. If no candidates are found, the reader shows local sample panels so the UI remains testable.

The next implementation step is to connect downloaded image bitmaps to the OCR, speech-bubble classifier, translation engine, and hybrid completed-bitmap cache.

## License

MIT
