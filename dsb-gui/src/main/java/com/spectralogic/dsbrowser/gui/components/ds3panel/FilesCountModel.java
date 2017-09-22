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

package com.spectralogic.dsbrowser.gui.components.ds3panel;

public class FilesCountModel {

    private int noOfFolders = 0;
    private int noOfFiles = 0;
    private long totalCapacity = 0;

    public int getNoOfFolders() {
        return noOfFolders;
    }

    public void setNoOfFolders(final int noOfFolders) {
        this.noOfFolders = noOfFolders;
    }

    public int getNoOfFiles() {
        return noOfFiles;
    }

    public void setNoOfFiles(final int noOfFiles) {
        this.noOfFiles = noOfFiles;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(final long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
}
