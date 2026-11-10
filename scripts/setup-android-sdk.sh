#!/usr/bin/env bash
# Install the Android SDK (cmdline-tools + platform 35 + build-tools 35.0.0).
# Used by .github/workflows/ci.yml; kept as a script so the steps stay readable.
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo ">> Installing Android command-line tools into $ANDROID_HOME"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  curl -sL -o /tmp/cmdtools.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q -o /tmp/cmdtools.zip -d "$ANDROID_HOME/cmdline-tools"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi

echo ">> Accepting SDK licenses"
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null

echo ">> Installing platform 35 + build-tools 35.0.0"
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --install \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null

# AGP reads these from the environment; export them for the rest of the job.
echo "ANDROID_HOME=$ANDROID_HOME" >> "${GITHUB_ENV:-/dev/null}"
echo "ANDROID_SDK_ROOT=$ANDROID_HOME" >> "${GITHUB_ENV:-/dev/null}"

"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --list_installed | head -10
