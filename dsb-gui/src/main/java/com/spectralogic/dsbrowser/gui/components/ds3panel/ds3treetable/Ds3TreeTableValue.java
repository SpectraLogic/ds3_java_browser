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
import javafx.scene.layout.HBox;

import java.io.Serializable;

public class Ds3TreeTableValue extends BaseTreeModel implements Serializable {

    private final String bucketName;
    private final String fullName;
    private final long size;
    private final String lastModified;
    private final transient HBox physicalPlacementHBox;
    private final String owner;
    private final boolean searchOn;
    private String marker = "";
    private String fullPath;
    private long logicalCapacity;

    public Ds3TreeTableValue(final String bucketName, final String name, final Type type, final long size, final String lastModified, final String owner, final boolean searchOn, final HBox physicalPlacementHBox) {
        this.bucketName = bucketName;
        this.fullName = name;
        this.name = getLastPart(name, type);
        this.fullPath = getPathExcludeName(bucketName, fullName);
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;
        this.physicalPlacementHBox = physicalPlacementHBox;
        this.owner = owner;
        this.searchOn = searchOn;
    }

    //constructor with marker
    public Ds3TreeTableValue(final String bucketName, final String name, final Type type, final long size, final String lastModified, final String owner, final boolean searchOn, final HBox physicalPlacementHBox, final String marker) {
        this.bucketName = bucketName;
        this.fullName = name;
        this.name = getLastPart(name, type);
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;
        this.physicalPlacementHBox = physicalPlacementHBox;
        this.owner = owner;
        this.searchOn = searchOn;
        this.marker = marker;
        this.fullPath = getPathExcludeName(bucketName, fullName);
    }

    private static String getLastPart(final String name, final Type type) {
        if (type == Type.Bucket) {
            return name;
        } else if (type == Type.Directory) {
            final String strippedName = name.substring(0, name.length() - 1);
            final int index = strippedName.lastIndexOf('/');
            return strippedName.substring(index + 1);
        }
        final int index = name.lastIndexOf('/');
        return name.substring(index + 1);
    }

    private static String getPathExcludeName(final String bucketName, final String fullPath) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(bucketName);
        stringBuilder.append(StringConstants.FORWARD_SLASH);
        final int index = fullPath.lastIndexOf("/");
        if (index != -1) {
            stringBuilder.append(fullPath.substring(0, index));
        }
        return stringBuilder.toString();
    }

    public boolean isSearchOn() {
        return searchOn;
    }

    public HBox getPhysicalPlacementHBox() {
        return physicalPlacementHBox;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
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

    public String getMarker() {
        return marker;
    }

    public void setMarker(final String marker) {
        this.marker = marker;
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

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(final String fullPath) {
        this.fullPath = fullPath;
    }

    public long getLogicalCapacity() {
        return logicalCapacity;
    }

    public void setLogicalCapacity(final long logicalCapacity) {
        this.logicalCapacity = logicalCapacity;
    }


}