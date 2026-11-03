#!/bin/sh
set -eu

adb_timeout=180
adb_max_attempts=36
adb_attempt=0

echo "Waiting up to ${adb_timeout}s for the API 36 emulator to become ADB-ready..."
while [ "$adb_attempt" -lt "$adb_max_attempts" ]; do
  adb devices -l || true
  device_state="$(adb devices | awk 'NR > 1 && $1 ~ /^emulator-/ { print $2; exit }')"
  if [ "$device_state" = "device" ]; then
    break
  fi
  echo "Emulator state: ${device_state:-not present}; attempt $((adb_attempt + 1))/$adb_max_attempts."
  adb_attempt=$((adb_attempt + 1))
  if [ "$adb_attempt" -lt "$adb_max_attempts" ]; then
    sleep 5
  fi
done

device_state="$(adb devices | awk 'NR > 1 && $1 ~ /^emulator-/ { print $2; exit }')"
if [ "$device_state" != "device" ]; then
  echo "::error::Emulator did not become ADB-ready within ${adb_timeout}s."
  adb devices -l || true
  exit 1
fi

boot_timeout=240
boot_max_attempts=48
boot_attempt=0

echo "Waiting up to ${boot_timeout}s for Android boot completion..."
while [ "$boot_attempt" -lt "$boot_max_attempts" ]; do
  boot_state="$(adb shell getprop sys.boot_completed 2>/dev/null || true)"
  if [ "$boot_state" = "1" ]; then
    break
  fi
  echo "Android boot state: ${boot_state:-not complete}; attempt $((boot_attempt + 1))/$boot_max_attempts."
  boot_attempt=$((boot_attempt + 1))
  if [ "$boot_attempt" -lt "$boot_max_attempts" ]; then
    sleep 5
  fi
done

boot_state="$(adb shell getprop sys.boot_completed 2>/dev/null || true)"
if [ "$boot_state" != "1" ]; then
  echo "::error::Android boot did not complete within ${boot_timeout}s."
  adb devices -l || true
  adb shell getprop ro.product.model ro.build.version.release ro.build.version.sdk 2>/dev/null || true
  exit 1
fi

api_level="$(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' || true)"
if [ "$api_level" != "36" ]; then
  echo "::error::Expected API 36, but emulator reports API ${api_level:-unknown}."
  adb devices -l || true
  adb shell getprop ro.build.version.release ro.build.version.sdk 2>/dev/null || true
  exit 1
fi

echo "Emulator is ready and reports API ${api_level}."
