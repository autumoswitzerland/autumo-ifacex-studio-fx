# Shared helpers for the ifaceX Studio FX bin/ scripts.

# Resolve a JDK 25 (the project is built for Java 25; the JavaFX 25 class files
# cannot be read by an older JVM). Prints the JAVA_HOME path or nothing.
jdk25() {
	if [ -x /usr/libexec/java_home ]
	then
		jdk="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
		[ -n "$jdk" ] && { printf '%s' "$jdk"; return 0; }
	fi
	# No hardcoded JDK path: the installation directory differs per machine
	# (e.g. zulu25.36.205-ca-fx-jdk25.0.4.1-macosx_x64). Fall back to JAVA_HOME
	# when java_home is unavailable or knows no JDK 25.
	if [ -n "${JAVA_HOME:-}" ] && [ -d "$JAVA_HOME" ]
	then
		printf '%s' "$JAVA_HOME"
		return 0
	fi
	return 1
}

# Use the resolved JDK 25 for the rest of the script; warn and fall back to the
# ambient JAVA_HOME when none is found.
use_jdk25() {
	JDK25="$(jdk25 || true)"
	if [ -n "$JDK25" ]
	then
		JAVA_HOME="$JDK25"
		export JAVA_HOME
		echo "-> Using JDK 25: $JAVA_HOME"
	else
		echo "WARNING: no JDK 25 found (checked java_home and JAVA_HOME)."
		echo "         Falling back to JAVA_HOME='${JAVA_HOME:-<unset>}'; the build will likely fail."
	fi
}

# Turn a (possibly relative) path into an absolute one, resolved against the
# directory the script was invoked from (Gradle tasks run with the working
# directory of their module, not the caller's).
abspath() {
	case "$1" in
		/*)	printf '%s' "$1" ;;
		*)	printf '%s/%s' "$INVOKE_DIR" "$1" ;;
	esac
}
