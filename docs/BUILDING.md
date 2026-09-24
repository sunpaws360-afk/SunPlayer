# Building SunPlayer

SunPlayer is built with Gradle 8.9, Android Gradle Plugin 8.5.0, and Java 17.

## Codespaces

Use a Java 17 Codespace image or install a Java 17 runtime before running Gradle.
The Kotlin 1.9.22 compiler used by this project does not recognize newer Java
version strings such as the OpenJDK 25 version available in some containers.

Verify the toolchain and SDK before building:

```bash
java -version
echo "sdk.dir=$HOME/android-sdk" > local.properties
./gradlew testDebugUnitTest assembleDebug
```

`local.properties` is machine-specific and is ignored by Git. The debug APK is
written to `app/build/outputs/apk/debug/app-debug.apk`.

## CI

GitHub Actions installs Temurin 17, runs the JVM unit tests, and builds both
debug and release variants. The workflow runs for pushes to every branch and
for pull requests targeting `main` or `master`.