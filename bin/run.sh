#!/bin/sh
###############################################################################
#
#  ifaceX Studio FX.
#  Version: 0.1
#
#  Notes:
#   - starts the JavaFX desktop application (module "ui")
#   - builds/installs the "core" module first, so "ui" resolves it locally
#   - forces JDK 25: the project is built for Java 25 and the JavaFX 25 class
#     files (major 67) cannot be read by an older JVM. An older JAVA_HOME is
#     therefore overridden, otherwise "javafx:run" fails with
#     "Unsupported major.minor version 67.0".
#
#  Usage:
#   bin/run.sh                       start with an empty editor
#   bin/run.sh path/to/config.ifacex open a config file directly
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

# --- Resolve the JDK 25 required by this project (override an older JAVA_HOME)
JDK25=""
if [ -x /usr/libexec/java_home ]
then
	JDK25="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
fi
if [ -z "$JDK25" ] && [ -d "/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home" ]
then
	JDK25="/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home"
fi
if [ -n "$JDK25" ]
then
	JAVA_HOME="$JDK25"
	export JAVA_HOME
else
	echo "WARNING: no JDK 25 found (checked java_home and Zulu 25)."
	echo "         Falling back to JAVA_HOME='${JAVA_HOME:-<unset>}'; the build will likely fail."
fi

cd "$ROOT"

echo "-> Using JDK: ${JAVA_HOME:-<default on PATH>}"
if [ -n "$JAVA_HOME" ]
then
	"$JAVA_HOME/bin/java" -version 2>&1 | head -1 || true
else
	java -version 2>&1 | head -1 || true
fi

echo "-> Building core module ..."
mvn -B -ntp -q -pl core install -DskipTests

echo "-> Starting ifaceX Studio FX ..."
if [ "$#" -gt 0 ]
then
	case "$1" in
		-*)	ARGS="$*" ;;
		*)	ARGS="--open $*" ;;
	esac
	exec mvn -B -ntp -pl ui javafx:run -Djavafx.args="$ARGS"
fi
exec mvn -B -ntp -pl ui javafx:run
