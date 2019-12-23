package com.jeppeman.globallydynamic.globalsplitinstall;

class GlobalSplitInstallSessionStatusMapper {
    @GlobalSplitInstallSessionStatus
    static int fromDownloadStatus(ApkDownloadRequest.Status status) {
        if (status instanceof ApkDownloadRequest.Status.Enqueued) {
            return GlobalSplitInstallSessionStatus.PENDING;
        }
        if (status instanceof ApkDownloadRequest.Status.Pending) {
            return GlobalSplitInstallSessionStatus.PENDING;
        }
        if (status instanceof ApkDownloadRequest.Status.Successful) {
            return GlobalSplitInstallSessionStatus.DOWNLOADED;
        }
        if (status instanceof ApkDownloadRequest.Status.Unknown) {
            return GlobalSplitInstallSessionStatus.UNKNOWN;
        }
        if (status instanceof ApkDownloadRequest.Status.Failed) {
            return GlobalSplitInstallSessionStatus.FAILED;
        }
        if (status instanceof ApkDownloadRequest.Status.Paused) {
            return GlobalSplitInstallSessionStatus.PENDING;
        }
        if (status instanceof ApkDownloadRequest.Status.Running) {
            return GlobalSplitInstallSessionStatus.DOWNLOADING;
        }
        if (status instanceof ApkDownloadRequest.Status.Canceled) {
            return GlobalSplitInstallSessionStatus.CANCELED;
        }

        return GlobalSplitInstallSessionStatus.FAILED;
    }

    @GlobalSplitInstallSessionStatus
    static int fromApkInstallerStatus(ApkInstaller.Status status) {
        if (status instanceof ApkInstaller.Status.Installing) {
            return GlobalSplitInstallSessionStatus.INSTALLING;
        }
        if (status instanceof ApkInstaller.Status.Installed) {
            return GlobalSplitInstallSessionStatus.INSTALLED;
        }
        if (status instanceof ApkInstaller.Status.Failed) {
            return GlobalSplitInstallSessionStatus.FAILED;
        }
        if (status instanceof ApkInstaller.Status.Pending) {
            return GlobalSplitInstallSessionStatus.PENDING;
        }
        if (status instanceof ApkInstaller.Status.RequiresUserPermission) {
            return GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION;
        }
        if (status instanceof ApkInstaller.Status.Canceled) {
            return GlobalSplitInstallSessionStatus.CANCELED;
        }

        return GlobalSplitInstallSessionStatus.FAILED;
    }
}
