package com.spectralogic.dsbrowser.gui.components.physicalplacement;

import com.spectralogic.ds3client.models.PhysicalPlacement;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ResourceBundle;

public final class PhysicalPlacementPopup {

    public static void show(final PhysicalPlacement physicalPlacement, final ResourceBundle resourceBundle) {
        final PhysicalPlacementView view = new PhysicalPlacementView(physicalPlacement);

        final Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        final Scene popupScene = new Scene(view.getView());
        popup.getIcons().add(new Image(resourceBundle.getString("dsbIconPath")));
        popup.setScene(popupScene);
        popup.setTitle(resourceBundle.getString("physicalPlacementLocation"));
        popup.setAlwaysOnTop(false);
        popup.setResizable(true);
        popup.showAndWait();
    }

}
