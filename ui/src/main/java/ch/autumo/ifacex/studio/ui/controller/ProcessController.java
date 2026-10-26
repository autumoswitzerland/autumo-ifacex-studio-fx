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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.commons.utils.system.OSUtils;
import ch.autumo.ifacex.studio.ui.StudioApp;
import ch.autumo.ifacex.studio.ui.StudioHelper;
import ch.autumo.ui.utils.UI;
import javafx.application.Platform;


/**
 * Process controller.
 */
public class ProcessController {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessController.class.getName());

	private MainController mainController = null;
	private boolean debugMode = true;
	private Process process = null;
	private final List<String> testSwitches = new ArrayList<String>();

	public ProcessController(MainController mainController, boolean debugMode) {
		this.mainController = mainController;
		this.debugMode = debugMode;
	}

	private void addTestSwitch(String switcher) {
		if (!testSwitches.contains(switcher)) {
			testSwitches.add(switcher);
		}
	}

	private String consumeSwitchesAsString() {
		StringBuilder sb = new StringBuilder();
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
			return;
		}
		final ExternalProcessWatchdog watchDog = new ExternalProcessWatchdog(this, studioApp);
		final Runnable runner = new Runnable() {
			@Override
			public void run() {
				String path = studioApp.getIfacexFolder();
				if (path == null || path.length() == 0) {
					Platform.runLater(() -> UI.showFineWarningDialog(studioApp,
							"<html>You need to specify a ifaceX instance in the general settings<br>before you can run a configuration!</html>"));
					return;
				}

				if (!StudioHelper.checkIfacexFolder(studioApp, path)) {
					Platform.runLater(() -> UI.showFineWarningDialog(studioApp, StudioHelper.t("No autumo ifaceX instance was found at the selected location!")));
					return;
				}

				if (OSUtils.isWindows()) {
					path += "bin\\run.bat";
				} else {
					path += "bin/run.sh";
				}

				if (mainController == null || mainController.config == null) {
					return;
				}
				String configPath = mainController.sourceFile != null ? mainController.sourceFile.toAbsolutePath().toString() : null;
				if (configPath == null) {
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

				String workDir = studioApp.getIfacexFolder();
				if (workDir != null && !workDir.endsWith(File.separator) && !workDir.endsWith("/")) {
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

				BufferedReader reader = null;
				final StringBuilder consoleOutput = new StringBuilder();
				int exitCode = 0;
				try {
					ProcessBuilder builder = new ProcessBuilder(pathExec, configArg, "s", switches);
					builder.redirectErrorStream(true);
					builder.directory(new File(wd));
					process = builder.start();
					final InputStream stdout = process.getInputStream();
					reader = new BufferedReader(new InputStreamReader(stdout));
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
					final boolean hasErrorPatterns = output.contains("Exception") || output.contains("ERROR") || output.contains("FATAL");
					if (exitCode != 0 || hasErrorPatterns) {
						Platform.runLater(() -> {
							UI.showFineErrorDialog(studioApp,
									"<html><b>External ifaceX test instance error</b><br><br>"
											+ "The external ifaceX test instance exited with an error. "
											+ "Check the console output for details.</html>");
						});
					}
					Platform.runLater(() -> {
						if (mainController != null) {
							mainController.highlightConsole();
						}
					});
				} catch (Exception e) {
					LOG.error("Couldn't execute external test instance!", e);
					Platform.runLater(() -> {
						UI.showFineErrorDialog(studioApp,
								"<html><b>External ifaceX test instance error</b><br><br>"
										+ "Couldn't execute external ifaceX test instance! "
										+ "Check test instance path.</html>");
					});
				} finally {
					try {
						if (reader != null) {
							reader.close();
						}
					} catch (IOException e) {
						LOG.error("Couldn't close reader from ifaceX process!", e);
					}
				}

				// Re-enable Run buttons in every case: normal end, error, or watchdog timeout.
				final int finalExitCode = exitCode;
				Platform.runLater(() -> {
					if (mainController != null) {
						mainController.processFinished(finalExitCode);
					}
				});

				appendConsoleLine("");
			}
		};
		new Thread(runner).start();
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

	public void kill(int timeout) {
		if (process != null && process.isAlive()) {
			try {
				process.destroy();
				if (!process.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)) {
					process.destroyForcibly();
				}
			} catch (Exception e) {
				LOG.error("Couldn't kill process!", e);
			}
		}
	}

}
