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

package com.spectralogic.dsbrowser.gui.components.version;

import java.util.Date;
import java.util.UUID;

import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormatKt;

public class VersionItem {
    private final String key;
    private final Date lastModified;
    private final UUID versionId;
    private final Long size;

    public VersionItem(final String key, final Date lastModified, final Long size, final UUID versionId) {
        this.key = key;
        this.lastModified = lastModified;
        this.versionId = versionId;
        this.size = size;
    }

    public String getName() {
        final int lastSlash = key.lastIndexOf("/");
        return key.substring(lastSlash+1);
    }

    public String getKey() {
        return key;
    }

    public String getCreated() {
        return new DateTimeUtils().format(lastModified);
    }

    public String getVersionId() {
        return versionId.toString();
    }

    public String getSize() {
        return FileSizeFormatKt.toByteRepresentation(size);
    }


}
