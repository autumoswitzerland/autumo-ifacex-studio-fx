/**
 * Copyright 2026 autumo GmbH, Michael Gasche.
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of autumo GmbH The intellectual and technical
 * concepts contained herein are proprietary to autumo GmbH
 * and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from autumo GmbH.
 */
package ch.autumo.ifacex.studio.ui.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.commons.utils.system.OSUtils;
import ch.autumo.ifacex.studio.ui.StudioApp;
import ch.autumo.ifacex.studio.ui.Sounds;
import ch.autumo.ifacex.studio.ui.StudioHelper;
import ch.autumo.ui.fx.controller.DefaultCodeEditorController;
import ch.autumo.ui.fx.view.JsonFormatterView;
import ch.autumo.ui.utils.UI;
import javafx.application.Platform;
import javafx.scene.Cursor;


/**
 * Process controller.
 */
public class ProcessController {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessController.class.getName());

	/** Prefix the engine requires for the extra argument that carries the test switches. */
	private static final String TEST_SWITCH_PREFIX = "testswitches:";

	/** Milliseconds a process gets to end gracefully when it is killed by the user. */
	private static final int KILL_GRACE_MS = 2000;

	private final String ROOT_CALL_START_MARKER = "-------- Root call test: --------";
	private final String ROOT_CALL_END_MARKER   = "-------- End root call ----------";
	private final int ROOT_CALL_MAX_CHARS       = 20000;
	

	private MainController mainController = null;
	private boolean debugMode = true;
	private Process process = null;
	/** Set as soon as the process is killed (by the Kill button or by the watchdog). */
	private volatile boolean killed = false;
	private final List<String> testSwitches = new ArrayList<String>();

	public ProcessController(MainController mainController, boolean debugMode) {
		this.mainController = mainController;
		this.debugMode = debugMode;
	}

	void addTestSwitch(String switcher) {
		if (!testSwitches.contains(switcher)) {
			testSwitches.add(switcher);
		}
	}

	/**
	 * The test switches as the engine expects them: the argument must start with
	 * {@code testswitches:}, otherwise {@code App} ignores it completely - neither {@code DEBUG}
	 * nor {@code ROOTCALL} would reach the engine.
	 *
	 * @return e.g. {@code testswitches:ROOTCALL,DEBUG}
	 */
	String consumeSwitchesAsString() {
		final StringBuilder sb = new StringBuilder(TEST_SWITCH_PREFIX);
		for (int i = 0; i < testSwitches.size(); i++) {
			sb.append(testSwitches.get(i));
			if (i < testSwitches.size() - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	public void execute(boolean rootCall) {
		final StudioApp studioApp = StudioApp.get();
		if (studioApp == null) {
			if (mainController != null) {
				mainController.processFinished(-1, false);
			}
			return;
		}
		final ExternalProcessWatchdog watchDog = new ExternalProcessWatchdog(this, studioApp);
		final Runnable runner = new Runnable() {
			@Override
			public void run() {
				// The Studio is released in every case - normal end, error, kill, timeout or an
				// error that is not an Exception (the UI would stay locked otherwise).
				boolean finished = false;
				try {
				String path = StudioHelper.getIfacexFolder(studioApp);
				if (path == null || path.length() == 0) {
					Platform.runLater(() -> UI.showFineWarningDialog(studioApp,
							"<html>You need to specify a ifaceX instance in the general settings<br>before you can run a configuration!</html>"));
					finish(studioApp, -1, false, true, false);
					return;
				}

				if (!StudioHelper.checkIfacexFolder(studioApp, path)) {
					Platform.runLater(() -> UI.showFineWarningDialog(studioApp, StudioHelper.t("No autumo ifaceX instance was found at the selected location!")));
					finish(studioApp, -1, false, true, false);
					return;
				}

				studioApp.getScene().setCursor(Cursor.WAIT);
				
				if (OSUtils.isWindows()) {
					path += "bin\\run.bat";
				} else {
					path += "bin/run.sh";
				}

				if (mainController == null || mainController.config == null) {
					finish(studioApp, -1, false, true, false);
					return;
				}
				String configPath = mainController.sourceFile != null ? mainController.sourceFile.toAbsolutePath().toString() : null;
				if (configPath == null) {
					finish(studioApp, -1, false, true, false);
					return;
				}
				String singleconfig = "config:" + configPath;
				singleconfig = singleconfig.replaceAll(" ", "#SPACE#");

				testSwitches.clear();
				if (rootCall) {
					addTestSwitch("ROOTCALL");
				}
				if (debugMode) {
					addTestSwitch("DEBUG");
				}

				String workDir = StudioHelper.getIfacexFolder(studioApp);
				if (workDir != null && !workDir.endsWith(File.separator)) {
					workDir += File.separator;
				}
				workDir = workDir + "bin";

				final String pathExec = path;
				final String configArg = singleconfig;
				final String switches = consumeSwitchesAsString();
				final String wd = workDir;

				Platform.runLater(() -> {
					if (mainController != null) {
						mainController.clearConsole();
					}
				});

				appendConsoleLine("");
				appendConsoleLine("DEBUG mode is: " + (debugMode ? "ON (forced)" : "OFF"));
				appendConsoleLine("");
				appendConsoleLine("Note: If you see debug output here and debug is switched off, then your default logging configuration of ifaceX is set to debug;");
				appendConsoleLine("");
				appendConsoleLine("      check/modify log-levels in 'cfg/logging.xml'.");
				appendConsoleLine("");
				appendConsoleLine("");

				appendConsoleLine("Executing: '" + pathExec + "' ");
				appendConsoleLine("");
				appendConsoleLine("with IPC : '" + configPath + "' ");
				appendConsoleLine("");
				appendConsoleLine("");

				appendConsoleLine("Launching external ifaceX test instance...");
				appendConsoleLine("");
				appendConsoleLine("");

				LineReader reader = null;
				final StringBuilder consoleOutput = new StringBuilder();
				int exitCode = 0;
				boolean failed = false;
				try {
					ProcessBuilder builder = new ProcessBuilder(pathExec, configArg, "s", switches);
					builder.redirectErrorStream(true);
					builder.directory(new File(wd));
					process = builder.start();
					final InputStream stdout = process.getInputStream();
					reader = new LineReader(stdout);
					final Thread wdRunner = new Thread(watchDog);
					wdRunner.start();
					String line = null;
					while (true) {
						watchDog.watch();
						line = reader.readLine();
						if (line == null) {
							break;
						}
						consoleOutput.append(line).append("\n");
						appendConsoleLine(line);
						watchDog.reset();
					}
					watchDog.stop();
					try {
						exitCode = process.waitFor();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						exitCode = -1;
					}
					final String output = consoleOutput.toString();
					final boolean hasErrorPatterns = output.contains("Exception")
							|| hasErrorLevel(output);
					failed = exitCode != 0 || hasErrorPatterns;
					if (killed) {
						// A process that was killed on purpose ends with a non-zero exit code and
						// often with error output - that is not a failure of the IPC, so it must
						// not be reported as one.
						Platform.runLater(() -> UI.showFineInformationDialog(studioApp,
								"<html>" + StudioHelper.t("The ifaceX process was stopped.")
										+ "<br><br>"
										+ StudioHelper.t("The console shows the output up to the moment it was stopped.")
										+ "</html>"));
					} else if (failed) {
						Platform.runLater(() -> {
							UI.showFineErrorDialog(studioApp,
									"<html><b>External ifaceX test instance error</b><br><br>"
											+ "The external ifaceX test instance exited with an error. "
											+ "Check the console output for details.</html>");
						});
					}
				} catch (Exception e) {
					
					studioApp.getScene().setCursor(Cursor.DEFAULT);
					
					LOG.error("Couldn't execute external test instance!", e);
					Platform.runLater(() -> {
						UI.showFineErrorDialog(studioApp,
								"<html><b>External ifaceX test instance error</b><br><br>"
										+ "Couldn't execute external ifaceX test instance! "
										+ "Check test instance path.</html>");
					});
				} finally {
					
					studioApp.getScene().setCursor(Cursor.DEFAULT);
					
					// Always: also when reading the output failed, so that the watchdog thread
					// ends with this run and cannot kill a process that belongs to nobody.
					watchDog.stop();
					
					try {
						if (reader != null) {
							reader.close();
						}
					} catch (IOException e) {
						LOG.error("Couldn't close reader from ifaceX process!", e);
					}
				}

				appendConsoleLine("");
				finish(studioApp, exitCode, killed, failed, rootCall);
				finished = true;
				} finally {
					if (!finished) {
						// Exception, Error or an early return: release the Studio and report a
						// failure, otherwise the form and the raw view stay disabled.
						finish(studioApp, -1, killed, true, false);
					}
				}
			}
		};
		new Thread(runner).start();
	}

	/**
	 * Releases the Studio when the run has ended - in every case: normal end, error, kill,
	 * watchdog timeout, or a run that did not even start.
	 *
	 * <p>Re-enables the run button, the form and the raw view, plays the finish sound and
	 * formats a root call result.</p>
	 *
	 * @param studioApp the application
	 * @param exitCode  exit code of the process ({@code -1} when it did not run)
	 * @param killed    whether the process was stopped on purpose
	 * @param failed    whether the run failed
	 * @param rootCall  whether this was a root call (its JSON is shown in the formatter)
	 */
	private void finish(StudioApp studioApp, int exitCode, boolean killed, boolean failed,
			boolean rootCall) {
		Platform.runLater(() -> {
			if (mainController != null) {
				mainController.processFinished(exitCode, killed);
			}
			// A process the user stopped is not a result - no sound for it.
			if (!killed) {
				Sounds.play(!failed);
			}
			if (rootCall) {
				ProcessController.this.formatResult(studioApp);
			}
		});
	}

	private void formatResult(StudioApp studioApp) {
		final String output = mainController.getConsole().getText();
		if (output != null) {
			// Both markers must be present; checking the indexOf() results (and not some
			// derived offset) is what keeps an error run out of substring().
			final int startMarker = output.indexOf(ROOT_CALL_START_MARKER);
			final int endMarker = output.indexOf(ROOT_CALL_END_MARKER);
			if (startMarker < 0 || endMarker < 0) {
				return;
			}
			final int start = startMarker + ROOT_CALL_START_MARKER.length() + 1;
			final int end = endMarker - 1;
			if (start >= end || end > output.length()) {
				return;
			}
			
			String json = output.substring(start, end);
			json = json.trim();
			int len = json.length();
			if (len > ROOT_CALL_MAX_CHARS) {
				json = json.substring(0, ROOT_CALL_MAX_CHARS);
			}
			final DefaultCodeEditorController ctrl = UI.showJsonFormatter(studioApp);
			final JsonFormatterView view = (JsonFormatterView) ctrl.getView();
			view.setText(json);
			Platform.runLater(() -> {
				view.format();
			});
		}
	}
	
	private void appendConsoleLine(String line) {
		final String l = line == null ? "" : line;
		Platform.runLater(() -> {
			if (mainController != null && mainController.getConsole() != null) {
				mainController.getConsole().appendLine(l);
			} else if (mainController != null) {
				mainController.appendConsole(l);
			}
		});
	}

	/**
	 * Reads the output of the external ifaceX process line by line.
	 *
	 * <p>The encoding of the child process output is not known in advance: a JVM writes
	 * {@code System.out} in its console/file encoding, which is UTF-8 on a current JDK on
	 * macOS/Linux but the platform ANSI/OEM code page on Windows. Reading with a fixed charset
	 * would garble every umlaut on the other platform. Every line is therefore decoded as strict
	 * UTF-8 first and falls back to the platform default charset when the bytes are not valid
	 * UTF-8.</p>
	 */
	static final class LineReader implements Closeable {

		private final InputStream in;
		private final ByteArrayOutputStream line = new ByteArrayOutputStream(256);

		LineReader(InputStream in) {
			this.in = new BufferedInputStream(in);
		}

		/**
		 * Reads the next line, without the line terminator.
		 *
		 * @return the line, or {@code null} at the end of the stream
		 * @throws IOException when the stream cannot be read
		 */
		String readLine() throws IOException {
			line.reset();
			int b;
			boolean any = false;
			while ((b = in.read()) != -1) {
				any = true;
				if (b == '\n') {
					break;
				}
				line.write(b);
			}
			if (!any && b == -1) {
				return null;
			}
			return decode(line.toByteArray());
		}

		@Override
		public void close() throws IOException {
			in.close();
		}
	}

	/** An ANSI escape sequence of the coloured engine output. */
	private static final java.util.regex.Pattern ANSI =
			java.util.regex.Pattern.compile("\u001B\\[[;\\d]*[ -/]*[@-~]");

	/**
	 * Whether the output contains a line that starts with the log level {@code ERROR} or
	 * {@code FATAL}.
	 *
	 * <p>These two words alone are not a reliable sign: a record may well carry them in a field,
	 * e.g. a status column, and then they are data. Only when one of them <em>starts</em> a line
	 * it is a message of the engine. The console appender renders the level at the beginning of
	 * the line ({@code %highlight{%-5p}} in {@code cfg/logging.xml}), so leading ANSI colour
	 * codes and blanks are ignored.</p>
	 *
	 * <p>Exceptions keep the plain check on the whole output; the exit code stays the primary
	 * signal.</p>
	 *
	 * @param output complete console output of the process
	 * @return {@code true} when a line starts with {@code ERROR} or {@code FATAL}
	 */
	static boolean hasErrorLevel(String output) {
		if (output == null || output.isEmpty()) {
			return false;
		}
		for (final String raw : output.split("\r?\n")) {
			final String line = ANSI.matcher(raw).replaceAll("").stripLeading();
			if (isLogLevel(line, "ERROR") || isLogLevel(line, "FATAL")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether a line starts with the given log level (and not with a word that merely begins
	 * with it).
	 *
	 * @param line  output line, ANSI codes already removed
	 * @param level log level, e.g. {@code ERROR}
	 * @return {@code true} when the level is at the beginning of the line
	 */
	private static boolean isLogLevel(String line, String level) {
		if (!line.startsWith(level)) {
			return false;
		}
		if (line.length() == level.length()) {
			return true;
		}
		final char next = line.charAt(level.length());
		return next == ' ' || next == '\t' || next == ':' || next == '[' || next == '|';
	}

	/**
	 * Decodes one output line: UTF-8 when the bytes are valid UTF-8, the platform charset
	 * otherwise.
	 *
	 * @param bytes raw bytes of the line, a trailing {@code \r} is dropped
	 * @return the decoded line
	 */
	static String decode(byte[] bytes) {
		int len = bytes.length;
		if (len > 0 && bytes[len - 1] == '\r') {
			len--;
		}
		boolean ascii = true;
		for (int i = 0; i < len; i++) {
			if ((bytes[i] & 0xFF) > 0x7F) {
				ascii = false;
				break;
			}
		}
		if (ascii) {
			return new String(bytes, 0, len, StandardCharsets.US_ASCII);
		}
		final CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		try {
			return utf8.decode(ByteBuffer.wrap(bytes, 0, len)).toString();
		} catch (CharacterCodingException e) {
			return new String(bytes, 0, len, Charset.defaultCharset());
		}
	}

	/**
	 * Kills the external ifaceX process - called by the toolbar's Kill button and by the
	 * watchdog when the process hangs.
	 *
	 * <p>The process is asked to end, and destroyed forcibly when it does not react within
	 * {@code timeout} milliseconds. The console reports the kill like the legacy Studio did.</p>
	 *
	 * @param timeout milliseconds the process gets to end gracefully before it is destroyed
	 */
	/**
	 * Kills the external ifaceX process on request of the user (the Kill button).
	 *
	 * <p>The process is asked to end and destroyed forcibly when it does not react within
	 * {@link #KILL_GRACE_MS} milliseconds.</p>
	 */
	public void kill() {
		kill(KILL_GRACE_MS, false);
	}

	/**
	 * Kills the external ifaceX process and reports the timeout that was exceeded - called by the
	 * watchdog when the process hangs.
	 *
	 * @param timeout milliseconds the watchdog waited before it gave up
	 */
	public void kill(int timeout) {
		kill(timeout, timeout > 0);
	}

	/**
	 * Kills the external ifaceX process.
	 *
	 * <p>The process is asked to end, and destroyed forcibly when it does not react within
	 * {@code timeout} milliseconds. The console reports the kill like the legacy Studio did.</p>
	 *
	 * @param timeout milliseconds the process gets to end gracefully before it is destroyed
	 * @param reportTimeout {@code true} to print the exceeded timeout in the console message
	 */
	private void kill(int timeout, boolean reportTimeout) {
		if (process == null || !process.isAlive()) {
			return;
		}
		killed = true;
		try {
			process.destroy();
			if (!process.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)) {
				process.destroyForcibly();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			process.destroyForcibly();
		} catch (Exception e) {
			LOG.error("Couldn't kill process!", e);
		}
		appendConsoleLine("");
		appendConsoleLine("");
		if (reportTimeout) {
			appendConsoleLine("**** PROCESS KILLED after " + (timeout / 1000) + " seconds ****");
		} else {
			appendConsoleLine("**** PROCESS KILLED ****");
		}
		appendConsoleLine("");
		appendConsoleLine("");
	}


}
