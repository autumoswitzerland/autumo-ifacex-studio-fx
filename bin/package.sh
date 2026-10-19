#!/bin/sh
###############################################################################
#
#  ifaceX Studio FX — create a runtime image + OS installer (jpackage).
#
#  Notes:
#   - requires a JavaFX-capable JDK 25 at the path configured in
#     ui/build.gradle (runtime.jdkHome, default: Azul Zulu FX under
#     /Volumes/Fastdrive/...). Adjust the path there for your machine.
#   - the target platform is selected via the 'targetPlatformName' property
#     in ui/build.gradle (mac / macArch / win / linux).
#   - a summary of all produced artifacts is printed at the end (Gradle keeps
#     up-to-date tasks silent, so without it a successful run looks empty).
#
#  Usage:
#   bin/package.sh            package with the incremental build
#   bin/package.sh --fresh    force a full rebuild of all packaging tasks
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

# Optional '--fresh': Gradle reports up-to-date tasks and skips them, which
# looks like nothing happened. Force every packaging task to run again.
GRADLE_ARGS=":ui:runtime :ui:jpackage"
if [ "$1" = "--fresh" ]
then
	GRADLE_ARGS="$GRADLE_ARGS --rerun-tasks"
	echo "-> Forcing a full rebuild of all packaging tasks (--rerun-tasks)"
fi

./gradlew $GRADLE_ARGS

###############################################################################

JPACKAGE_DIR="$ROOT/ui/build/jpackage"
JRE_DIR="$ROOT/ui/build/jre"

echo ""
echo "------------------------------------------------------------------------------"
echo " Packaging result"
echo "------------------------------------------------------------------------------"

# The application image (.app on macOS, plain directory elsewhere).
APP_IMAGE="$(find "$JPACKAGE_DIR" -maxdepth 1 -name '*.app' -type d 2>/dev/null | head -1)"
if [ -n "$APP_IMAGE" ]
then
	echo " APP    $APP_IMAGE"
	echo "        $(du -sh "$APP_IMAGE" | cut -f1), $(ls -ld "$APP_IMAGE" | awk '{print $6, $7, $8}')"
else
	echo " APP    (none found in ui/build/jpackage)"
fi

# The installers: everything jpackage produced except the app image itself.
INSTALLERS="$(find "$JPACKAGE_DIR" -maxdepth 1 -type f \
	\( -name '*.pkg' -o -name '*.dmg' -o -name '*.msi' -o -name '*.deb' -o -name '*.rpm' \) \
	2>/dev/null | sort)"
if [ -n "$INSTALLERS" ]
then
	echo "$INSTALLERS" | while read -r f
	do
		echo " INSTALL $(basename "$f")  ($(du -h "$f" | cut -f1))"
		echo "         $f"
	done
else
	echo " INSTALL (none found in ui/build/jpackage)"
fi

# The jlink runtime image the app embeds (kept for inspection, not shipped alone).
if [ -d "$JRE_DIR" ]
then
	echo " JRE    $(du -sh "$JRE_DIR" | cut -f1) in ui/build/jre/"
fi

echo ""
echo " Start the app directly:"
if [ -n "$APP_IMAGE" ]
then
	echo "   open \"$APP_IMAGE\""
fi
echo "------------------------------------------------------------------------------"
