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

import ch.autumo.ifacex.studio.ui.StudioApp;


/**
 * Process watchdog, if call hangs.
 */
public final class ExternalProcessWatchdog implements Runnable {
	
	private boolean isReseted = false;
	private boolean isStopped = false;
	private ProcessController controller = null;
	private StudioApp studioApp = null;
	private WatchDogLock lock = new WatchDogLock() {};
	
	public ExternalProcessWatchdog(ProcessController controller, StudioApp studioApp) {
		this.controller = controller;
		this.studioApp = studioApp;
	}
	
	public void stop() {
		isStopped = true;
	}
	
	public void watch() {
		isReseted = false;
	}
	
	public void reset() {
		isReseted = true;
	}
	
	@Override
	public void run() {
		final int timeout = studioApp.getTimeOut() * 1000; // milliseconds here!
		try {
			synchronized (lock) {
				while (true) {
					lock.wait(timeout);
					if (isStopped)
						break;
					if (!isReseted) {
						controller.kill(timeout);
						break;
					}
				}
			}
		} catch (Exception e) {
		}
	}
	
	private interface WatchDogLock {}
}

