# Viral Gaming Enhancer — Part 1 (v0.1.0)

**Status: source written but NOT yet compiled by the author.** Your first build is the first real test.
If it fails, paste the red error text back to Claude and it will be fixed before anything new is added.

## What works in Part 1 (implemented, untested on device)
Photo-Picker import (no storage permission) · metadata (duration, size, fps, codec) · ExoPlayer preview ·
original/enhanced toggle (same colour effects as export) · trim · undo/reset · 6 presets ·
saturation, contrast, brightness, temperature, tint · 9:16 / 1:1 / 4:5 crop · 30/60 fps frame DROP ·
bitrate · progress · cancel · save to chosen file · play · share · free-space + overheat checks.

## NOT implemented yet (shown honestly in the app)
Sharpen, vignette, glow/bloom, true teal/orange split-toning, motion blur, FPS interpolation (120 FPS),
AI upscaling, recent projects, background service (keep app open during export), scene detection,
zoom/slow-mo/transitions, music, captions. Planned for Part 2+.

## Build WITHOUT a computer (GitHub Actions, free)
1. Make a free GitHub account, create a **public** repo named ViralGamingEnhancer.
2. Get these files into the repo keeping the folder paths (Termux method below, or "Add file → Create new file" and type the full path).
3. Open the repo → **Actions** tab → "Build APK" → wait for green tick (~5–10 min).
4. Open the run → **Artifacts** → download ViralGamingEnhancer-debug-apk (zip) → unzip → install app-debug.apk
   (allow "Install unknown apps" for your browser/file manager).

Termux method: install Termux from F-Droid, then
    pkg install git unzip
    termux-setup-storage
    unzip ~/storage/downloads/ViralGamingEnhancer.zip -d vge && cd vge
    git init && git branch -M main && git add . && git commit -m init
    git remote add origin https://github.com/YOURNAME/ViralGamingEnhancer.git
    git push -u origin main      # use a Personal Access Token as the password

## Build WITH a computer (Android Studio)
1. Install Android Studio (it bundles JDK 17). 2. File → Open → this folder. 3. Let Gradle sync; accept SDK licences/downloads (API 36).
4. Phone: Settings → About → tap Build number 7× → Developer options → USB debugging. Plug in, press Run.
5. APK only: Build → Build APK(s), or in a terminal in the project folder (Android Studio's wrapper-less setup):
   Windows: `gradle :app:assembleDebug`   macOS/Linux: `gradle :app:assembleDebug`   (needs Gradle 8.11.1; Android Studio can generate a wrapper via Tools → Gradle).
   Output: app/build/outputs/apk/debug/app-debug.apk. Copy to phone and install. To update: install a newer APK over the old one (same debug key on the same machine/runner type may differ—if "App not installed", uninstall first).

## Why not build on the phone itself
Gradle + Android SDK + Compose need several GB RAM/disk and x86/ARM SDK tools that Termux/phone IDEs don't reliably provide. GitHub Actions is the dependable phone-only route.

## Dependencies & licences
AndroidX Media3 1.10.0 (ExoPlayer, Transformer, Effect, UI) — Apache 2.0 · Jetpack Compose/Material3/Activity/Lifecycle/Core — Apache 2.0 · Kotlin — Apache 2.0.
**No FFmpeg, OpenCV, or AI models are included.** FFmpeg builds are often GPL/LGPL (source-offer / relinking duties); not needed yet. Re-check licences before adding any.
Google's Android SDK/build tools are used under their own terms (free).

## FPS explained
Frame duplication repeats frames (looks the same, bigger file). Frame-rate conversion drops or repeats frames. AI interpolation synthesises new in-between frames — heavy, not in this app. Labelling a file "120 FPS" without real frames is fake; this app never does that.

## Troubleshooting
- "SDK location not found"/licence error → Android Studio: SDK Manager → install API 36 and accept licences.
- "Could not resolve androidx.media3…" → check internet; open Gradle → refresh.
- Unresolved reference in a Media3 class → paste the error to Claude (API names may differ by version).
- Export fails with an encoder error → lower bitrate, choose Original aspect, try a shorter clip.
- App not installed → uninstall the old copy first.
- Phone hot → app refuses at severe thermal level; wait and retry.
