# FreyTube - Android APK Build Guide

> **For AI Copilot sessions and developers** — Step-by-step guide to build the FreyTube APK locally or via GitHub Actions.

---

## 📋 Prerequisites

### Local Build Requirements
| Tool | Version | Location |
|------|---------|----------|
| **Android SDK** | Build-tools 35.0.0, Platform android-35 | `D:\AndroidBuildTools\sdk` |
| **JDK** | OpenJDK 17.0.18 (Temurin) | `D:\AndroidBuildTools\jdk` |
| **Gradle** (system, for wrapper regeneration only) | 8.7 | `D:\AndroidBuildTools\gradle-bin` |
| **Gradle Wrapper** (in-project) | 8.5 | `gradle/wrapper/gradle-wrapper.jar` |

### Version Matrix
| Component | Version | Notes |
|-----------|---------|-------|
| Android Gradle Plugin (AGP) | 8.3.2 | In `build.gradle.kts` |
| Kotlin | 1.9.22 | Must match Compose compiler |
| Compose Compiler | 1.5.8 | Compatible with Kotlin 1.9.22 |
| Compose BOM | 2024.02.00 | In `app/build.gradle.kts` |
| KSP | 1.9.22-1.0.17 | For Room annotation processing |
| compileSdk / targetSdk | 35 | Requires android-35 platform |
| minSdk | 26 | Android 8.0+ |

---

## 🔨 Method 1: Local Build (CLI)

### Step 1: Set Environment Variables
```batch
set JAVA_HOME=D:\AndroidBuildTools\jdk
set ANDROID_HOME=D:\AndroidBuildTools\sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%
```

### Step 2: Ensure `local.properties` exists
```
sdk.dir=D\:\\AndroidBuildTools\\sdk
```
> ⚠️ Note the escaped colons (`\\:`) — required for Gradle on Windows.

### Step 3: Build Debug APK
```bash
cd D:\Projects\Feb-2026\Youtube_Adfree
gradlew.bat assembleDebug --no-daemon
```

### Step 4: Find the APK
```
app/build/outputs/apk/debug/app-debug.apk  (~20 MB)
```

### Quick Build Script
Create `run_build.bat`:
```batch
@echo off
set JAVA_HOME=D:\AndroidBuildTools\jdk
set ANDROID_HOME=D:\AndroidBuildTools\sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%
cd /d D:\Projects\Feb-2026\Youtube_Adfree
call gradlew.bat assembleDebug --no-daemon
echo EXIT_CODE=%ERRORLEVEL%
```

### Build Times (approximate)
| Scenario | Time |
|----------|------|
| First build (cold cache) | 15-25 min |
| Incremental (code changes) | 1-2 min |
| Kotlin-only recompile | 30-60 sec |

---

## 🚀 Method 2: GitHub Actions (Recommended)

The project has a CI/CD workflow at `.github/workflows/build-and-release.yml`.

### Triggering a Build
- **On push to `main`**: Builds debug + release APK automatically
- **On tag push** (`v*`): Builds APK + creates GitHub Release with APK attached
- **Manual dispatch**: Go to Actions tab → "Build & Release FreyTube APK" → "Run workflow"

### Creating a Release
```bash
git tag v1.0.0
git push origin v1.0.0
```
This triggers the workflow which:
1. Sets up JDK 17 + Android SDK
2. Builds debug and release APKs
3. Signs the release APK with a keystore
4. Creates a GitHub Release with both APKs attached

### Downloading APK from GitHub
- Go to: `https://github.com/Rajamr550/FreyTube/releases`
- Download `FreyTube-v1.0.0-debug.apk` or `FreyTube-v1.0.0-release.apk`
- Or get the latest artifact from the Actions tab

---

## 🐛 Common Build Errors & Fixes

### Error 1: `Icons.AutoMirrored.Filled.ArrowBack` — Unresolved reference
**Cause**: Wildcard import `import androidx.compose.material.icons.filled.*` does NOT include `AutoMirrored` icons.
**Fix**: Add explicit import:
```kotlin
import androidx.compose.material.icons.automirrored.filled.ArrowBack
```

### Error 2: `AnimatedVisibility` — ColumnScope receiver mismatch
**Cause**: When `AnimatedVisibility` is called inside a `Box` that's nested inside a `Column`, the Kotlin compiler resolves `ColumnScope.AnimatedVisibility` (from the outer Column scope) but can't use it because the immediate scope is `BoxScope`.
**Fix**: Use fully-qualified call:
```kotlin
// Instead of:
AnimatedVisibility(visible = ...) { ... }

// Use:
androidx.compose.animation.AnimatedVisibility(visible = ...) { ... }
```
Or replace `import androidx.compose.animation.*` with explicit imports for only what's needed (e.g., `fadeIn`, `fadeOut`, `slideInVertically`, `slideOutVertically`).

### Error 3: `Divider` is deprecated
**Fix**: Replace `Divider()` with `HorizontalDivider()` from `androidx.compose.material3`.

### Error 4: `compileSdk = 34` but only android-35 installed
**Fix**: Update `app/build.gradle.kts`:
```kotlin
compileSdk = 35
defaultConfig {
    targetSdk = 35
}
```

### Error 5: Missing `gradle-wrapper.jar`
**Fix**: Run from system Gradle:
```bash
set PATH=D:\AndroidBuildTools\gradle-bin\bin;%PATH%
gradle wrapper --gradle-version 8.5
```
This generates `gradle/wrapper/gradle-wrapper.jar` (~43 KB). Must be committed to Git (not in `.gitignore`).

### Error 6: AGP version warning for compileSdk 35
**Suppress** in `gradle.properties`:
```
android.suppressUnsupportedCompileSdk=35
```

---

## 📁 Project Structure (Key Files)

```
FreyTube/
├── build.gradle.kts            # Root: AGP 8.3.2, Kotlin 1.9.22, KSP
├── settings.gradle.kts         # Project name, includes :app
├── gradle.properties           # JVM args, AndroidX, non-transitive R
├── local.properties            # sdk.dir (NOT committed)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar  # MUST be committed (~43KB)
│       └── gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts        # compileSdk 35, all dependencies
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/freytube/app/
│       │   ├── FreyTubeApp.kt
│       │   ├── MainActivity.kt
│       │   ├── data/           # Models, Room DB, Repository, API
│       │   ├── service/        # Background play, Downloads
│       │   ├── ui/             # Compose screens, components, theme
│       │   └── viewmodel/      # ViewModels
│       └── res/                # Icons, strings, themes
├── keystore/                   # Signing config (git-ignored JKS)
└── .github/workflows/
    └── build-and-release.yml   # CI/CD workflow
```

---

## 🔑 Signing Configuration

### Debug Build
Uses Android's default debug keystore — no configuration needed.

### Release Build (GitHub Actions)
The workflow auto-generates a keystore via `keytool`:
```yaml
- name: Generate Keystore
  run: |
    keytool -genkeypair -v \
      -keystore keystore/freytube-release.jks \
      -alias freytube \
      -keyalg RSA -keysize 2048 \
      -validity 10000 \
      -storepass ${{ secrets.KEYSTORE_PASSWORD || 'freytube123' }} \
      -keypass ${{ secrets.KEY_PASSWORD || 'freytube123' }} \
      -dname "CN=FreyTube, OU=Dev, O=FreyTube, L=Unknown, ST=Unknown, C=US"
```

For production: Set `KEYSTORE_PASSWORD` and `KEY_PASSWORD` as GitHub repository secrets.

---

## 🔄 For AI Copilot Sessions

### Quick Reference Commands
```powershell
# Check build tools
dir D:\AndroidBuildTools\sdk\build-tools
dir D:\AndroidBuildTools\sdk\platforms

# Kill stuck Gradle daemons
taskkill /f /im java.exe

# Clean build
gradlew.bat clean assembleDebug --no-daemon

# Kotlin compile only (fast check)
gradlew.bat compileDebugKotlin --no-daemon

# Check GitHub Actions status
Invoke-WebRequest -Uri "https://api.github.com/repos/Rajamr550/FreyTube/actions/runs?per_page=3" -UseBasicParsing | ConvertFrom-Json | Select-Object -ExpandProperty workflow_runs | Select-Object id, status, conclusion
```

### Key Gotchas
1. **Never use `./gradlew`** on Windows — use `gradlew.bat`
2. **`--no-daemon`** flag is essential to avoid orphaned Java processes
3. **First build** downloads ~500MB of dependencies — be patient
4. **Terminal blocking**: Use `Start-Process cmd.exe -ArgumentList "/c build.bat" -WindowStyle Hidden -Wait` for background builds
5. **Build output capture**: Redirect inside the `.bat` file, not in PowerShell arguments
6. **Compose wildcard imports**: Always prefer explicit imports to avoid scope-resolution bugs

---

## 📌 GitHub Repository
- **URL**: https://github.com/Rajamr550/FreyTube
- **Owner**: Rajamr550 (Raja M)
- **Actions**: https://github.com/Rajamr550/FreyTube/actions
- **Releases**: https://github.com/Rajamr550/FreyTube/releases
