package com.jeppeman.globallydynamic.globalsplitinstall;

/**
 * Matches the error codes of the underlying dynamic delivery mechanisms.
 * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus">SplitInstallSessionStatus</a>
 * and <a target="_blank" href="https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-References/FeatureInstallSessionStatus">FeatureInstallSessionStatus</a>
 * for more information.
 */
public @interface GlobalSplitInstallSessionStatus {
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus.html#UNKNOWN">UNKNOWN</a>
     */
    int UNKNOWN = 0;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus.html#PENDING">PENDING</a>
     */
    int PENDING = 1;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus.html#REQUIRES_USER_CONFIRMATION">REQUIRES_USER_CONFIRMATION</a>
     */
    int REQUIRES_USER_CONFIRMATION = 8;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus.html#DOWNLOADING">DOWNLOADING</a>
     */
    int DOWNLOADING = 2;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus.html#DOWNLOADED">DOWNLOADED</a>
     */
    int DOWNLOADED = 3;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus.html#INSTALLING">INSTALLING</a>
     */
    int INSTALLING = 4;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus.html#INSTALLED">INSTALLED</a>
     */
    int INSTALLED = 5;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus.html#FAILED">FAILED</a>
     */
    int FAILED = 6;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus.html#CANCELING">CANCELING</a>
     */
    int CANCELING = 9;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallSessionStatus.html#CANCELED">CANCELED</a>
     */
    int CANCELED = 7;

    /*
     * Huawei specific statuses
     */

    /**
     * See <a target="_blank" href="https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-References/FeatureInstallSessionStatus">Huawei status codes</a>
     */
    int REQUIRES_PERSON_AGREEMENT = 10;

    int UNINSTALLING = 11;
    int UNINSTALLED = 12;
}
