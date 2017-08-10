package com.spectralogic.dsbrowser.gui.components.ds3panel;/*
 * ****************************************************************************
 *    Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */

public class FilesCountModelClean {
    private final long numberOfFiles;
    private final long numberOfFolders;
    private final long sizeInBytes;

    public FilesCountModelClean(final long numberOfFiles, final long numberOfFolders, final long sizeInBytes) {
        this.numberOfFiles = numberOfFiles;
        this.numberOfFolders = numberOfFolders;
        this.sizeInBytes = sizeInBytes;
    }

    public long getNoOfFiles() {
        return numberOfFiles;
    }

    public long getNoOfFolders() {
        return numberOfFolders;
    }

    public long getTotalCapacity() {
        return sizeInBytes;
    }
}
