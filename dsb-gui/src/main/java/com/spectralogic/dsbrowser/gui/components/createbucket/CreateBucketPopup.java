package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.util.Popup;

public final class CreateBucketPopup {
    public static void show(final CreateBucketWithDataPoliciesModel createBucketModel, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        final CreateBucketView createBucketView = new CreateBucketView(createBucketModel, deepStorageBrowserPresenter);
        Popup.show(createBucketView.getView(), "New Bucket");
    }
}
