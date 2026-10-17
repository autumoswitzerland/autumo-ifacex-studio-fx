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
