#!/bin/sh
###############################################################################
#
#  ifaceX Studio FX.
#  Version: 0.1
#
#  Notes:
#   - starts the JavaFX desktop application (module "ui")
#   - builds/installs the "core" module first, so "ui" resolves it locally
#
#  Usage:
#   bin/run.sh
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

# The project requires JDK 25; prefer the Azul Zulu 25 install if JAVA_HOME is unset
if [ -z "$JAVA_HOME" ] && [ -d "/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home" ]
then
	JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home"
	export JAVA_HOME
fi

cd "$ROOT"

echo "-> Building core module ..."
mvn -B -ntp -q -pl core install -DskipTests

echo "-> Starting ifaceX Studio FX ..."
exec mvn -B -ntp -pl ui javafx:run
