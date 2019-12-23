package com.jeppeman.globallydynamic.globalsplitinstall;

/**
 * An exception containing the underlying error in the form of a {@link GlobalSplitInstallErrorCode}
 *
 * @see GlobalSplitInstallErrorCode
 */
public class GlobalSplitInstallException extends RuntimeException {
    private final int errorCode;

    GlobalSplitInstallException(Throwable cause) {
        this(GlobalSplitInstallErrorCode.INTERNAL_ERROR, cause.getMessage());
    }

    GlobalSplitInstallException(int errorCode, String message) {
        this(errorCode, message, null);
    }

    GlobalSplitInstallException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @GlobalSplitInstallErrorCode
    public int getErrorCode() {
        return errorCode;
    }
}
