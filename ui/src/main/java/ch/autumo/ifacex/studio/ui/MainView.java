/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.ui;

import ch.autumo.ui.fx.BaseApp;
import ch.autumo.ui.fx.view.BaseBorderView;
import javafx.stage.Stage;

/**
 * The main application view (the window content).
 *
 * <p>It sub-classes the autumo-ui {@link BaseBorderView} on purpose: the framework resolves the
 * FXML file and the companion HTML resources relative to <em>this</em> class, i.e. to
 * {@code /ch/autumo/ifacex/studio/ui/}. That is why it lives in our own package and not in
 * {@code ch.autumo.ui.fx.view}.</p>
 *
 * <p>The view only carries the scene root and the {@code main.fxml} object graph; all behaviour
 * is in {@link MainController}. The controller must be set with
 * {@link BaseBorderView#setController(ch.autumo.ui.fx.controller.BaseController)} before the FXML
 * is loaded, see {@link StudioApp#preInitialize(Stage, java.util.Properties)}.</p>
 *
 * @see #MainView(BaseApp, Stage, String)
 */
public class MainView extends BaseBorderView {

    /**
     * Creates the main view and its scene on the given stage.
     *
     * <p>{@code null} as style sheet is deliberate: {@link BaseBorderView} then installs the
     * <em>autumo-ui base</em> look, and {@link StudioApp#initialize(Stage, java.util.Properties)}
     * puts the application's own stylesheet — the conventional
     * {@code style/application.css} of the autumo-ui resource structure — on top of it. Passing a
     * style sheet here would <em>replace</em> the base look instead of extending it.</p>
     *
     * @param app               owning application instance
     * @param stage             primary stage this view is shown on
     * @param styleResourcePath relative style file or {@code null} to use the autumo-ui base style
     */
    public MainView(BaseApp app, Stage stage, String styleResourcePath) {
        super(app, stage, styleResourcePath);
    }
}
