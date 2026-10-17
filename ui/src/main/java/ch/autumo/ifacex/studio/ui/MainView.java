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
 * 
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
     * @param app               owning application instance
     * @param stage             primary stage this view is shown on
     * @param styleResourcePath relative style file or {@code null} to use the autumo-ui base style
     */
    public MainView(BaseApp app, Stage stage, String styleResourcePath) {
        super(app, stage, styleResourcePath);
    }
}
