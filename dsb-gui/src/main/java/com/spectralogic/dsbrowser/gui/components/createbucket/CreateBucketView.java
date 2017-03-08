package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;

public class CreateBucketView extends FXMLView {
    public CreateBucketView(final CreateBucketWithDataPoliciesModel createBucketTask) {
        super(name -> {
            switch (name) {
                case "createBucketWithDataPoliciesModel":
                    return createBucketTask;
                default:
                    return null;
            }
        });
    }
}
