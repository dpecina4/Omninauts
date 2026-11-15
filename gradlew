#!/usr/bin/env sh
# Lightweight Gradle bootstrap for this project. If Gradle is already installed
# (for example by GitHub Actions), use it; otherwise download the pinned release.
set -eu

GRADLE_VERSION="9.3.1"
GRADLE_SHA256="b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-${HOME}/.gradle}"

if command -v gradle >/dev/null 2>&1; then
  INSTALLED_VERSION="$(gradle --version 2>/dev/null | sed -n 's/^Gradle //p' | head -n 1)"
  if [ "${INSTALLED_VERSION}" = "${GRADLE_VERSION}" ]; then
    exec gradle "$@"
  fi
fi

GRADLE_ROOT="${GRADLE_USER_HOME}/cavernauts-bootstrap/gradle-${GRADLE_VERSION}"
GRADLE_BIN="${GRADLE_ROOT}/bin/gradle"
if [ ! -x "${GRADLE_BIN}" ]; then
  mkdir -p "${GRADLE_USER_HOME}/cavernauts-bootstrap"
  ZIP_PATH="${GRADLE_USER_HOME}/cavernauts-bootstrap/gradle-${GRADLE_VERSION}-bin.zip"
  TMP_DIR="${GRADLE_USER_HOME}/cavernauts-bootstrap/unpack-${GRADLE_VERSION}"
  if [ ! -f "${ZIP_PATH}" ]; then
    command -v curl >/dev/null 2>&1 || { echo "curl is required to download Gradle." >&2; exit 1; }
    curl --fail --location --retry 3 --output "${ZIP_PATH}.tmp" \
      "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
    mv "${ZIP_PATH}.tmp" "${ZIP_PATH}"
  fi
  if command -v sha256sum >/dev/null 2>&1; then
    echo "${GRADLE_SHA256}  ${ZIP_PATH}" | sha256sum --check --status || {
      rm -f "${ZIP_PATH}"
      echo "Gradle distribution checksum verification failed." >&2
      exit 1
    }
  elif command -v shasum >/dev/null 2>&1; then
    ACTUAL="$(shasum -a 256 "${ZIP_PATH}" | awk '{print $1}')"
    [ "${ACTUAL}" = "${GRADLE_SHA256}" ] || {
      rm -f "${ZIP_PATH}"
      echo "Gradle distribution checksum verification failed." >&2
      exit 1
    }
  else
    echo "sha256sum or shasum is required to verify the Gradle download." >&2
    exit 1
  fi
  rm -rf "${TMP_DIR}"
  mkdir -p "${TMP_DIR}"
  unzip -q "${ZIP_PATH}" -d "${TMP_DIR}"
  mv "${TMP_DIR}/gradle-${GRADLE_VERSION}" "${GRADLE_ROOT}"
  rmdir "${TMP_DIR}"
fi

exec "${GRADLE_BIN}" "$@"
