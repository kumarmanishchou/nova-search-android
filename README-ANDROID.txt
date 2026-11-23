NOVA Search — Android Studio source project

This is source code for a native Android app, not a prebuilt APK.

Build:
1. Install Android Studio on a computer.
2. Open this folder as an existing project.
3. Let Android Studio download the Android Gradle plugin and SDK components.
4. Connect an Android phone with USB debugging enabled, or use an emulator.
5. Choose Build > Build Bundle(s) / APK(s) > Build APK(s).

The APK is normally created at:
app/build/outputs/apk/debug/app-debug.apk

Features:
- Native Android search screen.
- On-device SQLite index.
- HTTPS-only same-host crawler running on a background worker thread while app remains open.
- Search results from the pages stored on the device.

Prototype limitations:
- It is not an internet-wide search engine; the index starts empty.
- It crawls only the host you submit, with a cap of 40 pages per crawl.
- This starter implementation does not yet implement robots.txt parsing, robust HTML parsing,
  media/PDF indexing, durable scheduled background work, or production-grade crawler safeguards.
- The app requires internet permission to crawl public HTTPS websites.
