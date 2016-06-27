package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.airhacks.afterburner.views.FXMLView;

/**
 * Created by Rahul on 6/16/2016.
 */
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
