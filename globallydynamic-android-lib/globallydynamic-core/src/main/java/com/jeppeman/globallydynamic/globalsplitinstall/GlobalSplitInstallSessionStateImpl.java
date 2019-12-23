package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.PendingIntent;

import java.util.List;

class GlobalSplitInstallSessionStateFactory {
    static GlobalSplitInstallSessionState create(
            int sessionId,
            int status,
            int errorCode,
            long bytesDownloaded,
            long totalBytesToDownload,
            List<String> moduleNames,
            List<String> languages,
            PendingIntent resolutionIntent
    ) {
        return new GlobalSplitInstallSessionStateImpl(
                sessionId, status, errorCode, bytesDownloaded, totalBytesToDownload, moduleNames, languages, resolutionIntent
        );
    }

    static GlobalSplitInstallSessionState create(
            int sessionId,
            int status,
            int errorCode,
            long bytesDownloaded,
            long totalBytesToDownload,
            List<String> moduleNames,
            List<String> languages
    ) {
        return new GlobalSplitInstallSessionStateImpl(
                sessionId, status, errorCode, bytesDownloaded, totalBytesToDownload, moduleNames, languages
        );
    }
}

class GlobalSplitInstallSessionStateImpl implements GlobalSplitInstallSessionState {
    private final int sessionId;
    private final int status;
    private final int errorCode;
    private final long bytesDownloaded;
    private final long totalBytesToDownload;
    private final List<String> moduleNames;
    private final List<String> languages;
    private final PendingIntent resolutionIntent;

    GlobalSplitInstallSessionStateImpl(
            int sessionId,
            int status,
            int errorCode,
            long bytesDownloaded,
            long totalBytesToDownload,
            List<String> moduleNames,
            List<String> languages
    ) {
        this(sessionId, status, errorCode, bytesDownloaded, totalBytesToDownload, moduleNames, languages, null);
    }

    GlobalSplitInstallSessionStateImpl(
            int sessionId,
            int status,
            int errorCode,
            long bytesDownloaded,
            long totalBytesToDownload,
            List<String> moduleNames,
            List<String> languages,
            PendingIntent resolutionIntent
    ) {
        this.sessionId = sessionId;
        this.status = status;
        this.errorCode = errorCode;
        this.bytesDownloaded = bytesDownloaded;
        this.totalBytesToDownload = totalBytesToDownload;
        this.moduleNames = moduleNames;
        this.languages = languages;
        this.resolutionIntent = resolutionIntent;
    }

    @Override
    public int sessionId() {
        return sessionId;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public int errorCode() {
        return errorCode;
    }

    @Override
    public long bytesDownloaded() {
        return bytesDownloaded;
    }

    @Override
    public long totalBytesToDownload() {
        return totalBytesToDownload;
    }

    @Override
    public List<String> moduleNames() {
        return moduleNames;
    }

    @Override
    public List<String> languages() {
        return languages;
    }

    @Override
    public PendingIntent resolutionIntent() {
        return resolutionIntent;
    }
}
