package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class CreateBucketView extends FXMLView {
    public CreateBucketView(final CreateBucketWithDataPoliciesModel createBucketTask) {
        super(name -> {
            if (name.equals(StringConstants.CASE_BUCKETWITHDATAPOLOCY)) {
                return createBucketTask;
            }
            return null;
        });
    }
}
