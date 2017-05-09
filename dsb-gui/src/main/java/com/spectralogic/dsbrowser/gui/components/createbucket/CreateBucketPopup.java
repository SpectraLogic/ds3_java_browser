package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.spectralogic.dsbrowser.gui.util.Popup;

import java.util.ResourceBundle;

public final class CreateBucketPopup {
    public static void show(final CreateBucketWithDataPoliciesModel createBucketModel, final ResourceBundle resourceBundle) {
        final CreateBucketView createBucketView = new CreateBucketView(createBucketModel);
        Popup.show(createBucketView.getView(), resourceBundle.getString("createBucketContextMenu"));
    }
}
