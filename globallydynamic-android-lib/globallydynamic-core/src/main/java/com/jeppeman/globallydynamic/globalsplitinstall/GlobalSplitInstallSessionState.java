package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.Activity;
import android.app.PendingIntent;

import java.util.List;

public interface GlobalSplitInstallSessionState {
    /**
     * Gets the session id of the installation
     *
     * @return the session id of installation
     */
    int sessionId();

    /**
     * Gets the current status of the installation
     *
     * @return the status of installation
     * @see GlobalSplitInstallSessionStatus
     */
    @GlobalSplitInstallSessionStatus
    int status();

    /**
     * Gets the error code of the installation, {@link GlobalSplitInstallErrorCode#NO_ERROR} if
     * no error has occurred.
     *
     * @return the error code of the installation
     * @see GlobalSplitInstallErrorCode
     */
    @GlobalSplitInstallErrorCode
    int errorCode();

    /**
     * The number of bytes downloaded for the installation
     *
     * @return the number of bytes downloaded
     */
    long bytesDownloaded();

    /**
     * The total amount of bytes to download for the installation
     *
     * @return the total amount of bytes to download
     */
    long totalBytesToDownload();

    /**
     * The modules that this installation will include
     *
     * @return the modules of the installation
     */
    List<String> moduleNames();

    /**
     * The languages that this installation will include
     *
     * @return the languages of the installation
     */
    List<String> languages();

    /**
     * @deprecated use {@link GlobalSplitInstallManager#startConfirmationDialogForResult(GlobalSplitInstallSessionState, Activity, int)}
     */
    PendingIntent resolutionIntent();
}
