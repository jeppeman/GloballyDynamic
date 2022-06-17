package com.jeppeman.globallydynamic.globalsplitinstall;

import android.os.Bundle;

import com.google.android.play.core.splitinstall.SplitInstallSessionState;

import java.util.ArrayList;

class SelfHostedGlobalSplitInstallSessionStateMapper {
    static SplitInstallSessionState toInstallState(GlobalSplitInstallSessionState from) {
        Bundle bundle = new Bundle();
        bundle.putInt("session_id", from.sessionId());
        bundle.putInt("status", from.status());
        bundle.putInt("error_code", from.errorCode());
        bundle.putLong("bytes_downloaded", from.bytesDownloaded());
        bundle.putLong("total_bytes_to_download", from.totalBytesToDownload());
        bundle.putStringArrayList("module_names", (ArrayList<String>) from.moduleNames());
        bundle.putStringArrayList("languages", (ArrayList<String>) from.languages());
        bundle.putParcelable("user_confirmation_intent", from.resolutionIntent());
        return SplitInstallSessionState.zzd(bundle);
    }

    static GlobalSplitInstallSessionState toGlobalSplitInstallSessionState(SplitInstallSessionState from) {
        return GlobalSplitInstallSessionStateFactory.create(
                from.sessionId(),
                from.status(),
                from.errorCode(),
                from.bytesDownloaded(),
                from.totalBytesToDownload(),
                from.moduleNames(),
                from.languages(),
                from.resolutionIntent()
        );
    }
}
