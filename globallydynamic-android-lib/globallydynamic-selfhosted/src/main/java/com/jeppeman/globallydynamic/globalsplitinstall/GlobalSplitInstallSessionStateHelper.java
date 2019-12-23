package com.jeppeman.globallydynamic.globalsplitinstall;

import androidx.annotation.Nullable;

class GlobalSplitInstallSessionStateHelper {
    private static String statusToString(@GlobalSplitInstallSessionStatus int status) {
        switch (status) {
            case GlobalSplitInstallSessionStatus.CANCELED:
                return "CANCELED(" + GlobalSplitInstallSessionStatus.CANCELED + ")";
            case GlobalSplitInstallSessionStatus.CANCELING:
                return "CANCELING(" + GlobalSplitInstallSessionStatus.CANCELING + ")";
            case GlobalSplitInstallSessionStatus.DOWNLOADED:
                return "DOWNLOADED(" + GlobalSplitInstallSessionStatus.DOWNLOADED + ")";
            case GlobalSplitInstallSessionStatus.DOWNLOADING:
                return "DOWNLOADING(" + GlobalSplitInstallSessionStatus.DOWNLOADING + ")";
            case GlobalSplitInstallSessionStatus.FAILED:
                return "FAILED(" + GlobalSplitInstallSessionStatus.FAILED + ")";
            case GlobalSplitInstallSessionStatus.INSTALLED:
                return "INSTALLED(" + GlobalSplitInstallSessionStatus.INSTALLED + ")";
            case GlobalSplitInstallSessionStatus.INSTALLING:
                return "INSTALLING(" + GlobalSplitInstallSessionStatus.INSTALLING + ")";
            case GlobalSplitInstallSessionStatus.PENDING:
                return "PENDING(" + GlobalSplitInstallSessionStatus.PENDING + ")";
            case GlobalSplitInstallSessionStatus.REQUIRES_PERSON_AGREEMENT:
                return "REQUIRES_PERSON_AGREEMENT(" + GlobalSplitInstallSessionStatus.REQUIRES_PERSON_AGREEMENT + ")";
            case GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION:
                return "REQUIRES_USER_CONFIRMATION(" + GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION + ")";
            case GlobalSplitInstallSessionStatus.UNKNOWN:
                return "UNKNOWN(" + GlobalSplitInstallSessionStatus.UNKNOWN + ")";
            default:
                return "NON_EXISTING()";
        }
    }

    private static String errorCodeToString(@GlobalSplitInstallErrorCode int errorCode) {
        switch (errorCode) {
            case GlobalSplitInstallErrorCode.ACCESS_DENIED:
                return "ACCESS_DENIED(" + GlobalSplitInstallErrorCode.ACCESS_DENIED + ")";
            case GlobalSplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED:
                return "ACTIVE_SESSIONS_LIMIT_EXCEEDED(" + GlobalSplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED + ")";
            case GlobalSplitInstallErrorCode.API_NOT_AVAILABLE:
                return "API_NOT_AVAILABLE(" + GlobalSplitInstallErrorCode.API_NOT_AVAILABLE + ")";
            case GlobalSplitInstallErrorCode.INCOMPATIBLE_WITH_EXISTING_SESSION:
                return "INCOMPATIBLE_WITH_EXISTING_SESSION(" + GlobalSplitInstallErrorCode.INCOMPATIBLE_WITH_EXISTING_SESSION + ")";
            case GlobalSplitInstallErrorCode.INSTALL_APKS_FAILED:
                return "INSTALL_APKS_FAILED(" + GlobalSplitInstallErrorCode.INSTALL_APKS_FAILED + ")";
            case GlobalSplitInstallErrorCode.INSUFFICIENT_STORAGE:
                return "INSUFFICIENT_STORAGE(" + GlobalSplitInstallErrorCode.INSUFFICIENT_STORAGE + ")";
            case GlobalSplitInstallErrorCode.INTERNAL_ERROR:
                return "INTERNAL_ERROR(" + GlobalSplitInstallErrorCode.INTERNAL_ERROR + ")";
            case GlobalSplitInstallErrorCode.INVALID_REQUEST:
                return "INVALID_REQUEST(" + GlobalSplitInstallErrorCode.INVALID_REQUEST + ")";
            case GlobalSplitInstallErrorCode.MODULE_UNAVAILABLE:
                return "MODULE_UNAVAILABLE(" + GlobalSplitInstallErrorCode.MODULE_UNAVAILABLE + ")";
            case GlobalSplitInstallErrorCode.NETWORK_ERROR:
                return "NETWORK_ERROR(" + GlobalSplitInstallErrorCode.NETWORK_ERROR + ")";
            case GlobalSplitInstallErrorCode.NOT_SIGN_AGREEMENT:
                return "NOT_SIGN_AGREEMENT(" + GlobalSplitInstallErrorCode.NOT_SIGN_AGREEMENT + ")";
            case GlobalSplitInstallErrorCode.NO_ERROR:
                return "NO_ERROR(" + GlobalSplitInstallErrorCode.NO_ERROR + ")";
            case GlobalSplitInstallErrorCode.NO_INSTALL_PERMISSION:
                return "NO_INSTALL_PERMISSION(" + GlobalSplitInstallErrorCode.NO_INSTALL_PERMISSION + ")";
            case GlobalSplitInstallErrorCode.REQUIRE_INSTALL_CONFIRM:
                return "REQUIRE_INSTALL_CONFIRM(" + GlobalSplitInstallErrorCode.REQUIRE_INSTALL_CONFIRM + ")";
            case GlobalSplitInstallErrorCode.SERVICE_DIED:
                return "SERVICE_DIED(" + GlobalSplitInstallErrorCode.SERVICE_DIED + ")";
            case GlobalSplitInstallErrorCode.SESSION_NOT_FOUND:
                return "SESSION_NOT_FOUND(" + GlobalSplitInstallErrorCode.SESSION_NOT_FOUND + ")";
            case GlobalSplitInstallErrorCode.SPLITCOMPAT_COPY_ERROR:
                return "SPLITCOMPAT_COPY_ERROR(" + GlobalSplitInstallErrorCode.SPLITCOMPAT_COPY_ERROR + ")";
            case GlobalSplitInstallErrorCode.SPLITCOMPAT_EMULATION_ERROR:
                return "SPLITCOMPAT_EMULATION_ERROR(" + GlobalSplitInstallErrorCode.SPLITCOMPAT_EMULATION_ERROR + ")";
            case GlobalSplitInstallErrorCode.SPLITCOMPAT_VERIFICATION_ERROR:
                return "SPLITCOMPAT_VERIFICATION_ERROR(" + GlobalSplitInstallErrorCode.SPLITCOMPAT_VERIFICATION_ERROR + ")";
            default:
                return "UNKNOWN_ERROR()";
        }
    }

    static String toString(@Nullable GlobalSplitInstallSessionState state) {
        return state == null
                ? "null"
                : "GlobalSplitInstallSessionState(" +
                "sessionId=" + state.sessionId() +
                ", status=" + statusToString(state.status()) +
                ")";
    }
}
