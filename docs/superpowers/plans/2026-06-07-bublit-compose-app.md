# Bublit Compose App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable Android app named Bublit that loads web pages, extracts real DOM images, and presents a local-first Korean web-comic translation reader.

**Architecture:** Create a native Android app with Compose and Material 3 for the UI, small Kotlin domain modules for deterministic extraction/classification/render planning, and Android adapters for WebView and ML Kit. The first runnable result includes real WebView image discovery and a reader pipeline with testable fake translation/OCR fallbacks, while ML Kit adapters are wired behind interfaces for device execution.

**Tech Stack:** Android Gradle Plugin 9.2.1, Gradle 9.4.1 wrapper, Kotlin 2.4.0, Jetpack Compose BOM 2026.05.00, Material 3, lifecycle ViewModel, Coil, Room, WorkManager, ML Kit Text Recognition and Translate.

---

## File Structure

- `settings.gradle.kts`: Gradle plugin management and module registration.
- `build.gradle.kts`: root plugin aliases.
- `gradle/libs.versions.toml`: dependency and plugin versions.
- `gradle/wrapper/*`: Gradle wrapper files.
- `gradle.properties`: AndroidX, Kotlin, and Gradle defaults.
- `local.properties`: Android SDK path for this machine.
- `scripts/build-debug.ps1`: local build helper that uses Android Studio JBR 21.
- `scripts/install-debug.ps1`: local install helper for connected devices or emulators.
- `app/build.gradle.kts`: Android app configuration and dependencies.
- `app/src/main/AndroidManifest.xml`: app metadata and Internet permission.
- `app/src/main/java/com/bublit/app/MainActivity.kt`: Compose entry point.
- `app/src/main/java/com/bublit/app/BublitApp.kt`: app shell and screen routing.
- `app/src/main/java/com/bublit/app/ui/*`: Compose theme and screen components.
- `app/src/main/java/com/bublit/app/domain/*`: pure Kotlin models, image filtering, script detection, speech bubble classification, render planning.
- `app/src/main/java/com/bublit/app/web/*`: WebView host and JavaScript DOM image extraction.
- `app/src/main/java/com/bublit/app/translation/*`: translator interfaces, fake local translator, ML Kit translator adapter.
- `app/src/main/java/com/bublit/app/ocr/*`: OCR interfaces, fake OCR for runnable fallback, ML Kit OCR adapter.
- `app/src/main/java/com/bublit/app/cache/*`: Room schema and cache placeholders.
- `app/src/test/java/com/bublit/app/domain/*`: JVM tests for extraction/filtering/classification/render planning.

## Task 1: Scaffold Android Project

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`
- Create: `local.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/bublit/app/MainActivity.kt`

- [ ] **Step 1: Create Gradle/Android project files**

Use package `com.bublit.app`, app display name `Bublit`, `minSdk 23`, `targetSdk 36`, and `compileSdk 36`.

- [ ] **Step 2: Add Gradle wrapper**

Download Gradle 9.4.1, generate wrapper files, and make `./gradlew.bat --version` work with Android Studio JBR 21.

- [ ] **Step 3: Run baseline build**

Run: `.\scripts\build-debug.ps1`

Expected: build reaches Kotlin/Android compilation. If dependency or syntax errors appear, investigate root cause before editing.

- [ ] **Step 4: Commit**

Commit message: `🏗️ Scaffold Bublit Android project`

## Task 2: Add Pure Domain Tests and Models

**Files:**
- Create: `app/src/main/java/com/bublit/app/domain/Models.kt`
- Create: `app/src/main/java/com/bublit/app/domain/ImageCandidateFilter.kt`
- Create: `app/src/main/java/com/bublit/app/domain/ScriptDetector.kt`
- Create: `app/src/main/java/com/bublit/app/domain/SpeechBubbleClassifier.kt`
- Create: `app/src/main/java/com/bublit/app/domain/TypesetPlanner.kt`
- Create: `app/src/test/java/com/bublit/app/domain/ImageCandidateFilterTest.kt`
- Create: `app/src/test/java/com/bublit/app/domain/ScriptDetectorTest.kt`
- Create: `app/src/test/java/com/bublit/app/domain/SpeechBubbleClassifierTest.kt`
- Create: `app/src/test/java/com/bublit/app/domain/TypesetPlannerTest.kt`

- [ ] **Step 1: Write failing tests**

Tests must cover:

- Large DOM comic images are retained.
- Small UI images and duplicate URLs are rejected.
- English and Chinese text choose the correct source language for Korean translation.
- Bright speech bubble text is accepted.
- Background/effect text is rejected.
- Korean text render plans fit inside accepted bubble bounds.

- [ ] **Step 2: Verify RED**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: tests fail because domain classes are missing.

- [ ] **Step 3: Implement minimal domain logic**

Implement deterministic logic only. Do not call Android framework APIs from domain classes.

- [ ] **Step 4: Verify GREEN**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: domain unit tests pass.

- [ ] **Step 5: Commit**

Commit message: `🧪 Add Bublit domain pipeline tests`

## Task 3: Build Compose App Shell

**Files:**
- Create: `app/src/main/java/com/bublit/app/BublitApp.kt`
- Create: `app/src/main/java/com/bublit/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/bublit/app/ui/HomeScreen.kt`
- Create: `app/src/main/java/com/bublit/app/ui/ReaderScreen.kt`
- Create: `app/src/main/java/com/bublit/app/ui/ReaderComponents.kt`
- Modify: `app/src/main/java/com/bublit/app/MainActivity.kt`

- [ ] **Step 1: Add Compose UI**

Build a Material 3 app shell with URL input, load action, extraction status, reader mode, original/translated toggle, and per-image processing state.

- [ ] **Step 2: Use fake sample processing**

Until WebView and ML Kit adapters are connected, the load action should create a sample reader item so the UI is runnable.

- [ ] **Step 3: Verify build**

Run: `.\scripts\build-debug.ps1`

Expected: debug APK builds.

- [ ] **Step 4: Commit**

Commit message: `🎨 Add Bublit Compose reader shell`

## Task 4: Add WebView DOM Image Extraction

**Files:**
- Create: `app/src/main/java/com/bublit/app/web/WebImageExtractor.kt`
- Create: `app/src/main/java/com/bublit/app/web/WebPageLoader.kt`
- Modify: `app/src/main/java/com/bublit/app/BublitApp.kt`
- Test: `app/src/test/java/com/bublit/app/domain/ImageCandidateFilterTest.kt`

- [ ] **Step 1: Write extraction parser tests**

Add tests proving malformed entries are ignored and valid image metadata is converted into `ImageCandidate`.

- [ ] **Step 2: Verify RED**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: new parser tests fail before implementation.

- [ ] **Step 3: Implement WebView extraction**

Use `AndroidView` for WebView, enable JavaScript, wait for page finish, run JavaScript that returns JSON for actual DOM `<img>` elements, and feed results through `ImageCandidateFilter`.

- [ ] **Step 4: Verify GREEN and build**

Run: `.\gradlew.bat testDebugUnitTest`

Run: `.\scripts\build-debug.ps1`

- [ ] **Step 5: Commit**

Commit message: `🖼️ Extract DOM images from WebView`

## Task 5: Add OCR and Translation Adapters

**Files:**
- Create: `app/src/main/java/com/bublit/app/ocr/OcrEngine.kt`
- Create: `app/src/main/java/com/bublit/app/ocr/FakeOcrEngine.kt`
- Create: `app/src/main/java/com/bublit/app/ocr/MlKitOcrEngine.kt`
- Create: `app/src/main/java/com/bublit/app/translation/TranslationEngine.kt`
- Create: `app/src/main/java/com/bublit/app/translation/FakeTranslationEngine.kt`
- Create: `app/src/main/java/com/bublit/app/translation/MlKitTranslationEngine.kt`
- Modify: `app/src/main/java/com/bublit/app/BublitApp.kt`

- [ ] **Step 1: Write translation interface tests**

Add JVM tests for fake translator behavior and language routing.

- [ ] **Step 2: Verify RED**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: tests fail before interfaces exist.

- [ ] **Step 3: Implement adapters**

Fake adapters keep the app runnable without requiring model downloads. ML Kit adapters compile and can be selected in app code for real device execution.

- [ ] **Step 4: Verify GREEN and build**

Run: `.\gradlew.bat testDebugUnitTest`

Run: `.\scripts\build-debug.ps1`

- [ ] **Step 5: Commit**

Commit message: `🤖 Wire local OCR and translation adapters`

## Task 6: Cache and Run Helpers

**Files:**
- Create: `app/src/main/java/com/bublit/app/cache/BublitDatabase.kt`
- Create: `scripts/build-debug.ps1`
- Create: `scripts/install-debug.ps1`
- Modify: `README.md`

- [ ] **Step 1: Add minimal cache schema**

Add Room entities for image cache records and translation block records. This task only needs compile-ready persistence scaffolding.

- [ ] **Step 2: Add run documentation**

Document how to build, install, and run from Android Studio or PowerShell.

- [ ] **Step 3: Verify final build**

Run: `.\gradlew.bat testDebugUnitTest`

Run: `.\scripts\build-debug.ps1`

- [ ] **Step 4: Commit**

Commit message: `🚀 Add Bublit build and cache scaffolding`

## Self-Review

- The plan covers the approved spec's runnable first slice: URL input, WebView DOM image discovery, reading mode, local-first interfaces, speech bubble-only scope, and hybrid render planning.
- The plan intentionally uses fake OCR/translation fallbacks so the app can run before ML Kit models are downloaded, while compiling real ML Kit adapters behind interfaces.
- Canvas/JS-only viewers, background lettering, and sound effects remain out of scope.
