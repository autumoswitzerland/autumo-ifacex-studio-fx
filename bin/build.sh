#!/bin/sh
###############################################################################
#
#  ifaceX Studio FX — build all modules (Gradle: clean + build + tests).
#
#  Usage:
#   bin/build.sh
#
#------------------------------------------------------------------------------
#
#  2026 autumo GmbH
#
###############################################################################
set -e

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"

. "$HERE/lib.sh"
use_jdk25

cd "$ROOT"
exec ./gradlew clean build
