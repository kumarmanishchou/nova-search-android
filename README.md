# NOVA Search for Android

This Android prototype has a GitHub Actions workflow that builds a debug APK in the cloud.

**Phone-only build instructions:** open `BUILD-APK-WITH-GITHUB-ON-ANDROID.txt`.

The cloud workflow is `.github/workflows/android-apk.yml`. After uploading all
project contents (including `.github`) to a private GitHub repository, run
**Actions → Build NOVA Android APK → Run workflow**. Download the
`NOVA-Search-debug-APK` artifact from the completed run.

The app uses an on-device SQLite index. It starts empty and only indexes
websites that the user submits. It is not an internet-wide search engine.
