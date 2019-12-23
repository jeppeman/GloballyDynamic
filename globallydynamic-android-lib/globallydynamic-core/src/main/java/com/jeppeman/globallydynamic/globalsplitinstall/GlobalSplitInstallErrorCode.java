package com.jeppeman.globallydynamic.globalsplitinstall;

import androidx.annotation.IntDef;

/**
 * Matches the error codes of the underlying dynamic delivery mechanisms.
 * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode">SplitInstallErrorCode</a>
 * and <a target="_blank" href="https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-References/FeatureInstallErrorCode">FeatureInstallErrorCode</a>
 * for more information.
 */
@IntDef({
        GlobalSplitInstallErrorCode.NO_ERROR,
        GlobalSplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED,
        GlobalSplitInstallErrorCode.MODULE_UNAVAILABLE,
        GlobalSplitInstallErrorCode.INVALID_REQUEST,
        GlobalSplitInstallErrorCode.SESSION_NOT_FOUND,
        GlobalSplitInstallErrorCode.API_NOT_AVAILABLE,
        GlobalSplitInstallErrorCode.NETWORK_ERROR,
        GlobalSplitInstallErrorCode.ACCESS_DENIED,
        GlobalSplitInstallErrorCode.INCOMPATIBLE_WITH_EXISTING_SESSION,
        GlobalSplitInstallErrorCode.SERVICE_DIED,
        GlobalSplitInstallErrorCode.INSUFFICIENT_STORAGE,
        GlobalSplitInstallErrorCode.SPLITCOMPAT_VERIFICATION_ERROR,
        GlobalSplitInstallErrorCode.SPLITCOMPAT_EMULATION_ERROR,
        GlobalSplitInstallErrorCode.SPLITCOMPAT_COPY_ERROR,
        GlobalSplitInstallErrorCode.INTERNAL_ERROR,
        GlobalSplitInstallErrorCode.NOT_SIGN_AGREEMENT,
        GlobalSplitInstallErrorCode.REQUIRE_INSTALL_CONFIRM,
        GlobalSplitInstallErrorCode.NO_INSTALL_PERMISSION,
        GlobalSplitInstallErrorCode.INSTALL_APKS_FAILED
})
public @interface GlobalSplitInstallErrorCode {
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#NO_ERROR">NO_ERROR</a>
     */
    int NO_ERROR = 0;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#ACTIVE_SESSIONS_LIMIT_EXCEEDED">ACTIVE_SESSIONS_LIMIT_EXCEEDED</a>
     */
    int ACTIVE_SESSIONS_LIMIT_EXCEEDED = -1;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#MODULE_UNAVAILABLE">MODULE_UNAVAILABLE</a>
     */
    int MODULE_UNAVAILABLE = -2;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#INVALID_REQUEST">INVALID_REQUEST</a>
     */
    int INVALID_REQUEST = -3;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#SESSION_NOT_FOUND">SESSION_NOT_FOUND</a>
     */
    int SESSION_NOT_FOUND = -4;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#SESSION_NOT_FOUND">SESSION_NOT_FOUND</a>
     */
    int API_NOT_AVAILABLE = -5;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#NETWORK_ERROR">NETWORK_ERROR</a>
     */
    int NETWORK_ERROR = -6;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#ACCESS_DENIED">ACCESS_DENIED</a>
     */
    int ACCESS_DENIED = -7;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#INCOMPATIBLE_WITH_EXISTING_SESSION">INCOMPATIBLE_WITH_EXISTING_SESSION</a>
     */
    int INCOMPATIBLE_WITH_EXISTING_SESSION = -8;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#SERVICE_DIED">SERVICE_DIED</a>
     */
    int SERVICE_DIED = -9;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#INSUFFICIENT_STORAGE">INSUFFICIENT_STORAGE</a>
     */
    int INSUFFICIENT_STORAGE = -10;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#SPLITCOMPAT_VERIFICATION_ERROR">SPLITCOMPAT_VERIFICATION_ERROR</a>
     */
    int SPLITCOMPAT_VERIFICATION_ERROR = -11;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#SPLITCOMPAT_EMULATION_ERROR">SPLITCOMPAT_EMULATION_ERROR</a>
     */
    int SPLITCOMPAT_EMULATION_ERROR = -12;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#SPLITCOMPAT_COPY_ERROR">SPLITCOMPAT_COPY_ERROR</a>
     */
    int SPLITCOMPAT_COPY_ERROR = -13;
    /**
     * See <a target="_blank" href="https://developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode.html#INTERNAL_ERROR">INTERNAL_ERROR</a>
     */
    int INTERNAL_ERROR = -100;

    /*
     * Huawei specific error codes
     */

    /**
     * See <a target="_blank" href="https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-References/FeatureInstallErrorCode">Huawei error codes</a>
     */
    int NOT_SIGN_AGREEMENT = 901;
    /**
     * See <a target="_blank" href="https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-References/FeatureInstallErrorCode">Huawei error codes</a>
     */
    int REQUIRE_INSTALL_CONFIRM = 902;
    /**
     * See <a target="_blank" href="https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-References/FeatureInstallErrorCode">Huawei error codes</a>
     */
    int NO_INSTALL_PERMISSION = 903;
    /**
     * See <a target="_blank" href="https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-References/FeatureInstallErrorCode">Huawei error codes</a>
     */
    int INSTALL_APKS_FAILED = 904;
}
