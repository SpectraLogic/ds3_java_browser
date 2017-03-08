package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.spectralogic.dsbrowser.gui.util.Popup;

public final class CreateBucketPopup {
    public static void show(final CreateBucketWithDataPoliciesModel createBucketModel) {
        final CreateBucketView createBucketView = new CreateBucketView(createBucketModel);
        Popup.show(createBucketView.getView(), "New Bucket");
    }
}
