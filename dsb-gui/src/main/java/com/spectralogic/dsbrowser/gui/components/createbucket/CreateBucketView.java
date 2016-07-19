package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;

public class CreateBucketView extends FXMLView {
    public CreateBucketView(CreateBucketWithDataPoliciesModel createBucketTask, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        super(name -> {
            switch (name) {
                case "createBucketWithDataPoliciesModel":
                    return createBucketTask;
                case "deepStorageBrowserPresenter":
                    return deepStorageBrowserPresenter;
                default:
                    return null;
            }
        });
    }
}
