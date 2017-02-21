package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class CreateBucketView extends FXMLView {
    public CreateBucketView(final CreateBucketWithDataPoliciesModel createBucketTask, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        super(name -> {
            switch (name) {
                case StringConstants.CASE_BUCKETWITHDATAPOLOCY:
                    return createBucketTask;
                case StringConstants.CASE_DEEPSTORAGEBROWSER:
                    return deepStorageBrowserPresenter;
                default:
                    return null;
            }
        });
    }
}
