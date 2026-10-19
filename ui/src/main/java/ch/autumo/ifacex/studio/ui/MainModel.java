/*
 * Copyright 2026 autumo GmbH.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ch.autumo.ifacex.studio.ui;

import ch.autumo.ui.fx.model.BaseModel;
import javafx.scene.layout.Pane;

/**
 * The main application model.
 *
 * <p>The Studio is a desktop editor without a background job: the model only ties the controller
 * to the view, as the autumo-ui MVC contract requires a non-abstract {@code BaseModel} for
 * {@code BaseController}.</p>
 */
public class MainModel extends BaseModel {

    /**
     * Creates the model for the main view.
     *
     * @param view the main view
     */
    public MainModel(Pane view) {
        super(view);
    }
}
