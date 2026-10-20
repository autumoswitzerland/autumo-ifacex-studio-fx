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
#   bin/run.sh -Dapp.livereload=true path/to/config.ifcx
#                                    -D system properties are handed to the JVM
#                                    (e.g. -Dapp.livereload=true turns on the
#                                    development-only CSS live reload,
#                                    -Dapp.lang=de forces a language)
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

# Split the command line into leading '-D...' system properties (handed to the
# JVM) and the remaining argument (a config file to open). Everything after the
# first non '-D' argument is treated as the file to open.
JVM_PROPS=""
FILE=""
for arg in "$@"
do
	if [ -z "$FILE" ]
	then
		case "$arg" in
			-D*)	JVM_PROPS="$JVM_PROPS $arg" ;;
			*)		FILE="$arg" ;;
		esac
	else
		echo "WARNING: ignoring extra argument '$arg'"
	fi
done

ARGS=""
if [ -n "$JVM_PROPS" ]
then
	ARGS="-PstudioJvmArgs=$(printf '%s' "$JVM_PROPS" | sed 's/^ //')"
fi
if [ -n "$FILE" ]
then
	ARGS="$ARGS --args=--open $(abspath "$FILE")"
fi

if [ -n "$ARGS" ]
then
	# shellcheck disable=SC2086
	exec ./gradlew -q :ui:run $ARGS
fi
exec ./gradlew -q :ui:run
