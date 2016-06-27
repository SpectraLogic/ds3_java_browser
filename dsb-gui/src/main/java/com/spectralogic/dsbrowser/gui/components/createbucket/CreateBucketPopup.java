package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.spectralogic.dsbrowser.gui.util.Popup;

/**
 * Created by Rahul on 6/16/2016.
 */
public class CreateBucketPopup {
    public static void show(final CreateBucketWithDataPoliciesModel createBucketModel) {
        final CreateBucketView createBucketView = new CreateBucketView(createBucketModel);
        Popup.show(createBucketView.getView(), "New Bucket");
    }
}
