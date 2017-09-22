/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.spectralogic.dsbrowser.gui.util.BaseTreeModel;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

import java.io.Serializable;

public class Ds3TreeTableValueCustom implements Serializable {

    private final String bucketName;
    private final String name;
    private final String fullName;
    private final Ds3TreeTableValue.Type type;
    private final long size;
    private final String lastModified;
    private final String owner;
    private final boolean searchOn;

    public Ds3TreeTableValueCustom(final String bucketName, final String name, final Ds3TreeTableValue.Type type, final long size, final String lastModified, final String owner, final boolean searchOn) {
        this.bucketName = bucketName;
        this.fullName = name;
        this.name = getLastPart(name, type);
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;
        this.owner = owner;
        this.searchOn = searchOn;
    }

    public boolean isContainer() { return (type == Ds3TreeTableValue.Type.Bucket || type == Ds3TreeTableValue.Type.File); }

    public boolean isSearchOn() {
        return searchOn;
    }

    private static String getLastPart(final String name, final Ds3TreeTableValue.Type type) {
        if (type == Ds3TreeTableValue.Type.Bucket) {
            return name;
        } else if (type == Ds3TreeTableValue.Type.Directory) {
            final String strippedName = name.substring(0, name.length() - 1);
            final int index = strippedName.lastIndexOf('/');
            return strippedName.substring(index + 1);
        }
        final int index = name.lastIndexOf('/');
        return name.substring(index + 1);
    }

    public String getName() {
        return name;
    }

    public Ds3TreeTableValue.Type getType() {
        return type;
    }

    public long getSize() {
        return size;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getFullName() {
        return fullName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getDirectoryName() {
        switch (type) {
            case Directory:
                return getFullName();
            case Bucket:
                return StringConstants.EMPTY_STRING;
            default:
                return getParentDir(this.getFullName());
        }
    }


    private String getParentDir(final String fullName) {
        final int index = fullName.lastIndexOf('/');
        if (index < 0) {
            return StringConstants.EMPTY_STRING;
        }
        return fullName.substring(0, index);
    }

    public String getOwner() {
        return owner;
    }

}
