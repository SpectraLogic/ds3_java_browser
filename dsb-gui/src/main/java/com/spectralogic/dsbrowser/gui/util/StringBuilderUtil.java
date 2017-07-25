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

package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;

import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

import static com.spectralogic.dsbrowser.gui.util.StringConstants.*;

public class StringBuilderUtil {

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("lang",
            new Locale(ConfigProperties.getInstance().getLanguage()));

    /**
     * update title when recover job starts
     *
     * @param type         type of Job
     * @param endpointInfo endpointInfo
     * @param date         date
     * @return string
     */
    public static StringBuilder getRecoverJobTransferringForTitle(final String type, final String endpointInfo, final String date) {
        return new StringBuilder()
                .append(resourceBundle.getString("recovering"))
                .append(SPACE)
                .append(type)
                .append(SPACE)
                .append(resourceBundle.getString("jobOf"))
                .append(SPACE)
                .append(endpointInfo)
                .append(SPACE)
                .append(date);
    }

    /**
     * update title when recover job starts
     *
     * @param type type of job
     * @param date date
     * @return string
     */
    public static StringBuilder getRecoverJobTransferringForLogs(final String type, final String date) {
        return new StringBuilder()
                .append(resourceBundle.getString("recovering"))
                .append(SPACE)
                .append(type)
                .append(SPACE)
                .append(resourceBundle.getString("jobOf"))
                .append(SPACE)
                .append(date);
    }

    /**
     * get initiate string in case of put
     *
     * @param bucketName bucketName
     * @return String
     */
    public static StringBuilder getRecoverJobInitiateTransferTo(final String bucketName) {
        return new StringBuilder()
                .append(resourceBundle.getString("initiatingTransferTo"))
                .append(SPACE)
                .append(bucketName);
    }

    /**
     * get initiate transfer String in case of get
     *
     * @param bucketName
     * @return string
     */
    public static StringBuilder getRecoverJobInitiateTransferFrom(final String bucketName) {
        return new StringBuilder()
                .append(resourceBundle.getString("initiatingTransferFrom"))
                .append(SPACE)
                .append(bucketName);
    }

    public static StringBuilder jobInitiatedString(final String type, final String date, final String endPointInfo) {
        return new StringBuilder().append(type)
                .append(SPACE)
                .append(resourceBundle.getString("jobInitiated"))
                .append(SPACE)
                .append(resourceBundle.getString("withEndPoint"))
                .append(SPACE)
                .append(endPointInfo)
                .append(SPACE)
                .append(resourceBundle.getString("at"))
                .append(SPACE)
                .append(date);
    }

    public static StringBuilder transferringTotalJobString(final String totalJobSize, final String targetDirectory) {
        return new StringBuilder().append(resourceBundle.getString("transferring"))
                .append(SPACE).append(totalJobSize)
                .append(SPACE).append(resourceBundle.getString("to"))
                .append(SPACE).append(targetDirectory);
    }

    /**
     * get string for transfer rate
     *
     * @param transferRate  transferRate
     * @param timeRemaining timeRemaining
     * @param totalSent     totalSent
     * @param totalJobSize  totalJobSize
     * @param fromPath      from path
     * @param topath        to Path
     * @return get string for transfer rate
     */
    public static StringBuilder getTransferRateString(final long transferRate, final long timeRemaining,
                                                      final AtomicLong totalSent, final long totalJobSize,
                                                      final String fromPath, final String topath) {
        if (transferRate != 0) {
            return new StringBuilder().append(SPACE)
                    .append(resourceBundle.getString("transferRate"))
                    .append(SPACE).append(FileSizeFormat.getFileSizeType(transferRate))
                    .append(resourceBundle.getString("perSecond")).append(SPACE)
                    .append(resourceBundle.getString("timeRemaining"))
                    .append(SPACE).append(DateFormat.timeConversion(timeRemaining))
                    .append(FileSizeFormat.getFileSizeType(totalSent.get() / 2))
                    .append(FORWARD_SLASH).append(FileSizeFormat.getFileSizeType(totalJobSize))
                    .append(SPACE).append(resourceBundle.getString("transferredFile"))
                    .append(SPACE)
                    .append(FORWARD_ARROW)
                    .append(SPACE)
                    .append(fromPath)
                    .append(SPACE).append(resourceBundle.getString("to"))
                    .append(SPACE).append(topath);
        } else {
            return new StringBuilder()
                    .append(resourceBundle.getString("transferRate"))
                    .append(SPACE)
                    .append(FileSizeFormat.getFileSizeType(transferRate))
                    .append(resourceBundle.getString("perSecond"))
                    .append(SPACE)
                    .append(resourceBundle.getString("timeRemaining"))
                    .append(SPACE).append(COLON)
                    .append(resourceBundle.getString("calculating"))
                    .append(DOUBLE_DOTS)
                    .append(SPACE)
                    .append(FileSizeFormat.getFileSizeType(totalSent.get() / 2))
                    .append(FORWARD_SLASH)
                    .append(FileSizeFormat.getFileSizeType(totalJobSize))
                    .append(SPACE)
                    .append(resourceBundle.getString("transferredFile"))
                    .append(SPACE)
                    .append(FORWARD_ARROW)
                    .append(SPACE)
                    .append(fromPath)
                    .append(SPACE)
                    .append(resourceBundle.getString("to"))
                    .append(SPACE)
                    .append(topath);
        }
    }

    public static StringBuilder objectSuccessfullyTransferredString(final String fromPath, final String toPath,
                                                                    final String date, final String location) {
        final StringBuilder builder = new StringBuilder();
        builder.append(resourceBundle.getString("successfullyTransfered"))
                .append(SPACE)
                .append(fromPath)
                .append(SPACE).append(resourceBundle.getString("to"))
                .append(SPACE).append(toPath)
                .append(SPACE)
                .append(resourceBundle.getString("at"))
                .append(SPACE)
                .append(date);
        if (location != null) {
            builder.append(SPACE)
                    .append(resourceBundle.getString("location"))
                    .append(SPACE)
                    .append(location);
        }
        return builder;
    }

    public static StringBuilder jobSuccessfullyTransferredString(final String type, final String jobSize,
                                                                 final String toPath, final String date,
                                                                 final String location, final boolean isCacheEnable) {
        final StringBuilder builder = new StringBuilder();
        builder.append(type)
                .append(resourceBundle.getString("job"))
                .append(OPEN_BRACE)
                .append(resourceBundle.getString("fileSize"))
                .append(jobSize)
                .append(CLOSING_BRACE)
                .append(SPACE)
                .append(resourceBundle.getString("successfullyTransfered"))
                .append(SPACE)
                .append(resourceBundle.getString("to"))
                .append(SPACE).append(toPath)
                .append(SPACE)
                .append(resourceBundle.getString("at"))
                .append(SPACE)
                .append(date);
        if (location != null) {
            builder.append(SPACE)
                    .append(resourceBundle.getString("location"))
                    .append(SPACE)
                    .append(location);
        }
        if (isCacheEnable) {
            builder.append(resourceBundle.getString("waiting"));
        }
        return builder;
    }

    /**
     * show message in case job failure
     *
     * @param failureType whether cancelled or failed
     * @param reason      reason for failure
     * @param e           Exception
     * @return String of job failure/cancelled
     */
    public static String getJobFailedMessage(final String failureType, final String endpoint, final String reason,
                                             final Throwable e) {
        if (null != e) {
            return new StringBuilder()
                    .append(failureType)
                    .append(SPACE)
                    .append(endpoint)
                    .append(SPACE)
                    .append(DateFormat.formatDate(new Date()))
                    .append(resourceBundle.getString("reason"))
                    .append(SPACE)
                    .append(reason)
                    .append(SPACE)
                    .append(e).toString();
        } else {
            return new StringBuilder()
                    .append(failureType)
                    .append(SPACE)
                    .append(endpoint)
                    .append(SPACE)
                    .append(DateFormat.formatDate(new Date()))
                    .append(resourceBundle.getString("reason"))
                    .append(SPACE)
                    .append(reason).toString();
        }
    }

    /****************************************************
     * StringBuilders for Search job                     *
     ****************************************************/

    public static StringBuilder bucketFoundMessage(final String searchText, final String bucketName) {
        return new StringBuilder()
                .append(resourceBundle.getString("bucketFound"))
                .append(StringConstants.SPACE)
                .append(searchText)
                .append(StringConstants.SPACE)
                .append(StringConstants.IE)
                .append(StringConstants.SPACE)
                .append(bucketName);
    }

    public static StringBuilder searchInBucketMessage(final String bucketName, final int size) {
        return new StringBuilder()
                .append(resourceBundle.getString("found"))
                .append(StringConstants.SPACE)
                .append(size)
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("items"))
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("in"))
                .append(StringConstants.SPACE)
                .append(bucketName)
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("bucket"));
    }

    public static StringBuilder nObjectsFoundMessage(final int n) {
        return new StringBuilder()
                .append(resourceBundle.getString("searchResults"))
                .append(StringConstants.COLON)
                .append(StringConstants.SPACE)
                .append(n)
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("objectFound"))
                .append(StringConstants.SPACE);
    }

    public static StringBuilder searchFailedMessage() {
        return new StringBuilder().append(
                resourceBundle.getString("searchFailed"))
                .append(StringConstants.COLON)
                .append(StringConstants.SPACE);
    }

    public static StringBuilder getItemsCountInfoMessage(final int noOfFolders, final int noOfFiles) {
        return new StringBuilder()
                .append(resourceBundle.getString("contains"))
                .append(StringConstants.SPACE)
                .append(noOfFolders)
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("folders"))
                .append(StringConstants.SPACE)
                .append(noOfFiles)
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("files"));
    }

    public static StringBuilder getCapacityMessage(final long totalCapacity, final Ds3TreeTableValue.Type type) {
        if (type.equals(Ds3TreeTableValue.Type.Bucket)) {
            return new StringBuilder()
                    .append(resourceBundle.getString("bucket("))
                    .append(FileSizeFormat.getFileSizeType(totalCapacity))
                    .append(StringConstants.CLOSING_BRACE);
        } else {
            return new StringBuilder()
                    .append(resourceBundle.getString("folder("))
                    .append(FileSizeFormat.getFileSizeType(totalCapacity))
                    .append(StringConstants.CLOSING_BRACE);
        }
    }

    public static StringBuilder getSelectedItemCountInfo(final int expandedItemCount, final int selectedItemCount) {
        return new StringBuilder()
                .append(expandedItemCount)
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("items"))
                .append(StringConstants.COMMA)
                .append(StringConstants.SPACE)
                .append(selectedItemCount)
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("itemsSelected"));
    }

    public static StringBuilder getJobCompleted(final long totalJobSize, final Ds3Client ds3Client) {
        return new StringBuilder().append(resourceBundle.getString("getJobSize"))
                .append(StringConstants.SPACE)
                .append(FileSizeFormat.getFileSizeType(totalJobSize))
                .append(resourceBundle.getString("getCompleted"))
                .append(StringConstants.SPACE)
                .append(ds3Client.getConnectionDetails().getEndpoint())
                .append(StringConstants.SPACE)
                .append(DateFormat.formatDate(new Date()));

    }

    public static StringBuilder jobCancelled(final String jobType) {
        return new StringBuilder()
                .append(jobType)
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("jobCancelled"))
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("at"))
                .append(StringConstants.SPACE)
                .append(DateFormat.formatDate(new Date()));
    }

    public static StringBuilder jobFailed(final String jobType, final String endPoint, final Exception e) {
        return new StringBuilder()
                .append(jobType)
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("jobFailed"))
                .append(StringConstants.SPACE)
                .append(endPoint)
                .append(StringConstants.SPACE)
                .append(resourceBundle.getString("reason"))
                .append(e).append(StringConstants.SPACE)
                .append(resourceBundle.getString("at"))
                .append(StringConstants.SPACE)
                .append(DateFormat.formatDate(new Date()));

    }

    public static StringBuilder getPaneItemsString(final int expandedItemCount, final int size) {
        return new StringBuilder().append(expandedItemCount)
                .append(SPACE)
                .append(resourceBundle.getString("items"))
                .append(SPACE)
                .append(size)
                .append(SPACE)
                .append(resourceBundle.getString("itemsSelected"));
    }
}
