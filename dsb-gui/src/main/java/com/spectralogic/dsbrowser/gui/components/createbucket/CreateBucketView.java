package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.airhacks.afterburner.views.FXMLView;

public class CreateBucketView extends FXMLView{
    public CreateBucketView(CreateBucketWithDataPoliciesModel createBucketTask) {
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
