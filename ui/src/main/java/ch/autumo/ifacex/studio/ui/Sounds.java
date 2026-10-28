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
package ch.autumo.ifacex.studio.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.ifacex.studio.core.StudioConstants;
import javafx.scene.media.AudioClip;

/**
 * The finish sounds of the Studio: one for a successful run, one for a failed run.
 *
 * <p>Played when the external ifaceX process has finished, as the legacy Studio did. A process
 * that the user stopped is not a result - it stays silent.</p>
 *
 * <p>The setting {@code play.sounds} of the general settings switches the sounds off; it is
 * {@code true} by default. The files are the Kenney "Interface Sounds" set (CC0, see
 * {@code sounds/LICENSE-Kenney-UI-Sounds.txt}); replace them to change the sound, the names stay
 * the same.</p>
 */
public final class Sounds {

    private static final Logger LOG = LoggerFactory.getLogger(Sounds.class.getName());

    /** Sound of a successful run. */
    private static final String FINISH_OK = "sounds/finish_ok.wav";
    /** Sound of a failed run. */
    private static final String FINISH_NOK = "sounds/finish_nok.wav";

    /** Volume of the finish sounds - noticeable, but not louder than the rest of the desktop. */
    private static final double VOLUME = 0.7;

    /** The two clips, loaded on first use. */
    private static AudioClip ok;
    private static AudioClip nok;
    /** Set once loading failed, so a broken audio device is not retried on every run. */
    private static boolean unavailable;

    private Sounds() {
    }

    /**
     * Plays the finish sound of a run.
     *
     * @param success {@code true} for a successful run, {@code false} for a failed one
     */
    public static void play(boolean success) {
        if (!enabled()) {
            return;
        }
        final AudioClip clip = success ? ok() : nok();
        if (clip == null) {
            return;
        }
        try {
            clip.play();
        } catch (Exception e) {
            LOG.warn("Couldn't play the finish sound.", e);
        }
    }

    /**
     * Whether the user wants sounds ({@code play.sounds} in the general settings).
     *
     * @return {@code true} unless the setting is switched off
     */
    static boolean enabled() {
        final StudioApp app = StudioApp.get();
        if (app == null) {
            return false;
        }
        final String value = app.getConfigurationParameter(StudioConstants.PROP_KEY_PLAY_SOUNDS);
        return value == null || value.isBlank() || Boolean.parseBoolean(value.trim());
    }

    private static AudioClip ok() {
        if (ok == null && !unavailable) {
            ok = load(FINISH_OK);
        }
        return ok;
    }

    private static AudioClip nok() {
        if (nok == null && !unavailable) {
            nok = load(FINISH_NOK);
        }
        return nok;
    }

    /**
     * Loads a clip from the resources of this class.
     *
     * @param resource resource path, e.g. {@code sounds/finish_ok.wav}
     * @return the clip, or {@code null} when it cannot be loaded
     */
    private static AudioClip load(String resource) {
        try {
            final java.net.URL url = Sounds.class.getResource(resource);
            if (url == null) {
                unavailable = true;
                LOG.warn("Sound resource not found: {}", resource);
                return null;
            }
            final AudioClip clip = new AudioClip(url.toExternalForm());
            clip.setVolume(VOLUME);
            return clip;
        } catch (Exception | LinkageError e) {
            // No audio device, no media module: the Studio keeps working, just silently.
            unavailable = true;
            LOG.warn("Couldn't load the finish sounds; running without them.", e);
            return null;
        }
    }
}
