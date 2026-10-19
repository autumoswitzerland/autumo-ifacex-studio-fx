#!/bin/sh
###############################################################################
#
#  ifaceX Studio FX.
#  Version: 0.1
#
#  Notes:
#   - starts the JavaFX desktop application (module "ui")
#   - forces JDK 25: the project is built for Java 25 and the JavaFX 25 class
#     files (major 67) cannot be read by an older JVM. An older JAVA_HOME is
#     therefore overridden, otherwise ":ui:run" fails with
#     "Unsupported major.minor version 67.0".
#
#  Usage:
#   bin/run.sh                       start with an empty editor
#   bin/run.sh path/to/config.ifcx   open a config file directly (.ifcx or the
#   bin/run.sh path/to/config.ifacex   legacy .ifacex; relative paths are resolved
#                                    against the directory you invoke the script from)
#
#------------------------------------------------------------------------------
#
#  2026 autumo GmbH
#  Date: 16.10.2026
#
###############################################################################
set -e

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
INVOKE_DIR="$PWD"

. "$HERE/lib.sh"
use_jdk25

cd "$ROOT"

echo "-> Starting ifaceX Studio FX ..."
if [ "$#" -gt 0 ]
then
	case "$1" in
		-*)	ARGS="--args=$*" ;;
		*)	ARGS="--args=--open $(abspath "$1")" ;;
	esac
	exec ./gradlew -q :ui:run $ARGS
fi
exec ./gradlew -q :ui:run
