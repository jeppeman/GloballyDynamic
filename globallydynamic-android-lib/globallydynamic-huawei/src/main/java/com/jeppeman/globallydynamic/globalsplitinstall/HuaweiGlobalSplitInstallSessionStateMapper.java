package com.jeppeman.globallydynamic.globalsplitinstall;

import android.os.Bundle;

import com.huawei.hms.feature.model.InstallState;

import java.util.ArrayList;

class HuaweiGlobalSplitInstallSessionStateMapper {
    static InstallState toInstallState(GlobalSplitInstallSessionState from) {
        Bundle state = new Bundle();
        state.putInt("session_id", from.sessionId());
        state.putInt("status", from.status());
        state.putInt("error_code", from.errorCode());
        state.putLong("bytes_downloaded", from.bytesDownloaded());
        state.putLong("total_bytes_to_download", from.totalBytesToDownload());
        state.putStringArrayList("module_names", (ArrayList<String>) from.moduleNames());
        state.putStringArrayList("languages", (ArrayList<String>) from.languages());
        state.putParcelable("user_confirmation_intent", from.resolutionIntent());
        Bundle bundle = new Bundle();
        bundle.putBundle("state", state);
        return InstallState.makeSessionState(bundle);

    }

    static GlobalSplitInstallSessionState toGlobalSplitInstallSessionState(InstallState from) {
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
