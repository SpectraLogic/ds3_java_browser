package com.spectralogic.dsbrowser.gui.util;

import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

import static com.spectralogic.dsbrowser.gui.util.StringConstants.*;

public class JobStatusStrings {

    /**
     * update title when recover job starts
     *
     * @param resourceBundle resourceBundle
     * @param type           type of Job
     * @param endpointInfo   endpointInfo
     * @param date           date
     * @return string
     */
    public static String getRecoverJobTransferringForTitle(final ResourceBundle resourceBundle, final String type, final String endpointInfo, final String date) {
        final StringBuilder builder = new StringBuilder();
        builder.append(resourceBundle.getString("recovering"))
                .append(SPACE)
                .append(type)
                .append(SPACE)
                .append(resourceBundle.getString("jobOf"))
                .append(SPACE)
                .append(endpointInfo)
                .append(SPACE)
                .append(date);
        return builder.toString();
    }

    /**
     * update title when recover job starts
     *
     * @param resourceBundle resourceBundle
     * @param type           type of job
     * @param date           date
     * @return string
     */
    public static String getRecoverJobTransferringForLogs(final ResourceBundle resourceBundle, final String type, final String date) {
        final StringBuilder builder = new StringBuilder();
        builder.append(resourceBundle.getString("recovering"))
                .append(SPACE)
                .append(type)
                .append(SPACE)
                .append(resourceBundle.getString("jobOf"))
                .append(SPACE)
                .append(date);
        return builder.toString();
    }

    /**
     * get initiate string in case of put
     *
     * @param resourceBundle resourceBundle
     * @param bucketName     bucketName
     * @return String
     */
    public static String getRecoverJobInitiateTransferTo(final ResourceBundle resourceBundle, final String bucketName) {
        return new StringBuilder().append(resourceBundle.getString("initiatingTransferTo"))
                .append(SPACE)
                .append(bucketName).toString();
    }

    /**
     * get initiate transfer String in case of get
     *
     * @param resourceBundle resourceBundle
     * @param bucketName
     * @return string
     */
    public static String getRecoverJobInitiateTransferFrom(final ResourceBundle resourceBundle, final String bucketName) {
        return new StringBuilder().append(resourceBundle.getString("initiatingTransferFrom"))
                .append(SPACE)
                .append(bucketName).toString();
    }

    public static StringBuilder jobInitiatedString(final ResourceBundle resourceBundle, final String type, final String date, final String endPointInfo) {
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

    public static StringBuilder transferringTotalJobString(final ResourceBundle resourceBundle, final String totalJobSize, final String targetDirectory) {
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
    public static StringBuilder getTransferRateString(final ResourceBundle resourceBundle, final long transferRate, final long timeRemaining, final AtomicLong totalSent, final long totalJobSize, final String fromPath, final String topath) {
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

    public static StringBuilder objectSuccessfullyTransferredString(final ResourceBundle resourceBundle, final String fromPath, final String toPath, final String date, final String location) {
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

    public static StringBuilder jobSuccessfullyTransferredString(final ResourceBundle resourceBundle, final String type, final String jobSize, final String toPath, final String date, final String location, final boolean isCacheEnable) {
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
    public static String getJobFailedMessage(final ResourceBundle resourceBundle ,final String failureType, final String endpoint, final String reason, final Throwable e) {
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


}
