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

package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;


public class JobInfoModel {
    private final String name;
    private final String jobId;
    private final String date;
    private final long size;
    private final String fullPath;
    private final String jobType;
    private final String status;
    private final Type type;
    private final String targetLocation;
    private final String bucket;


    public JobInfoModel(final String name, final String jobId, final String date, final long size, final String fullPath, final String jobType, final String status, final Type type, final String targetLocation, final String bucket) {
        this.fullPath = fullPath;
        this.name = name;
        this.jobType = jobType;
        this.status = status;
        this.type = type;
        this.date = date;
        this.size = size;
        this.jobId = jobId;
        this.targetLocation = targetLocation;
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }

    public String getTargetLocation() {
        return targetLocation;
    }

    public String getJobId() {
        return jobId;
    }

    public String getDate() {
        return date;
    }

    public long getSize() {
        return size;
    }

    public String getFullPath() {
        return fullPath;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getJobType() {
        return jobType;
    }

    public String getStatus() {
        return status;
    }

    public enum Type {
        File, Directory, JOBID
    }
}
