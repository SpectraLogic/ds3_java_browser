package com.spectralogic.dsbrowser.gui.components.newsession;

import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class NewSessionPopup {
    public static NewSessionModel show() {
        final Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setMaxWidth(1000);

        final NewSessionView view = new NewSessionView();

        final Scene popupScene = new Scene(view.getView());
        popup.setScene(popupScene);
        popup.setTitle("Sessions");
        popup.setAlwaysOnTop(true);
        popup.setResizable(false);
        popup.showAndWait();
        // get the presenter and the newSelectionModel
        final NewSessionPresenter presenter = (NewSessionPresenter) view.getPresenter();
        return presenter.getResult();
    }
}
