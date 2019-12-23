package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.play.core.splitinstall.SplitInstallSessionState;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public class GlobalSplitInstallSessionStateMapperTest {
    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void toInstallState_shouldMaintainDataConsistency() {
        GlobalSplitInstallSessionState from = GlobalSplitInstallSessionStateFactory.create(
                123,
                333,
                343,
                3434,
                23434,
                new ArrayList<String>(Arrays.asList("module1", "module2")),
                new ArrayList<String>(Arrays.asList("lang1", "lang2")),
                PendingIntent.getBroadcast(context, 123, new Intent(), 0)
        );

        SplitInstallSessionState installState = GPlayGlobalSplitInstallSessionStateMapper.toInstallState(from);

        assertThat(installState.sessionId()).isEqualTo(from.sessionId());
        assertThat(installState.status()).isEqualTo(from.status());
        assertThat(installState.errorCode()).isEqualTo(from.errorCode());
        assertThat(installState.bytesDownloaded()).isEqualTo(from.bytesDownloaded());
        assertThat(installState.totalBytesToDownload()).isEqualTo(from.totalBytesToDownload());
        assertThat(installState.moduleNames()).isEqualTo(from.moduleNames());
        assertThat(installState.languages()).isEqualTo(from.languages());
        assertThat(installState.resolutionIntent()).isEqualTo(from.resolutionIntent());
    }

    @Test
    public void toGlobalSplitInstallSessionState_shouldMaintainDataConsistency() {
        Bundle bundle = new Bundle();
        bundle.putInt("session_id", 132);
        bundle.putInt("status", 4354);
        bundle.putInt("error_code", 4535);
        bundle.putLong("bytes_downloaded", 432);
        bundle.putLong("total_bytes_to_download", 1312);
        bundle.putStringArrayList("module_names", new ArrayList<String>(Arrays.asList("module1", "module2")));
        bundle.putStringArrayList("languages", new ArrayList<String>(Arrays.asList("lang1", "lang2")));
        bundle.putParcelable("user_confirmation_intent", PendingIntent.getBroadcast(context, 123, new Intent(), 0));
        SplitInstallSessionState from = SplitInstallSessionState.a(bundle);

        GlobalSplitInstallSessionState globalSplitInstallSessionState = GPlayGlobalSplitInstallSessionStateMapper.toGlobalSplitInstallSessionState(from);

        assertThat(globalSplitInstallSessionState.sessionId()).isEqualTo(from.sessionId());
        assertThat(globalSplitInstallSessionState.status()).isEqualTo(from.status());
        assertThat(globalSplitInstallSessionState.errorCode()).isEqualTo(from.errorCode());
        assertThat(globalSplitInstallSessionState.bytesDownloaded()).isEqualTo(from.bytesDownloaded());
        assertThat(globalSplitInstallSessionState.totalBytesToDownload()).isEqualTo(from.totalBytesToDownload());
        assertThat(globalSplitInstallSessionState.moduleNames()).isEqualTo(from.moduleNames());
        assertThat(globalSplitInstallSessionState.languages()).isEqualTo(from.languages());
        assertThat(globalSplitInstallSessionState.resolutionIntent()).isEqualTo(from.resolutionIntent());
    }
}
