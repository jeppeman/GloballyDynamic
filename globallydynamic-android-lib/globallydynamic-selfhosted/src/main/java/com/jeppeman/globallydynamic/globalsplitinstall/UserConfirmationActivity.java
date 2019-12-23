package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import com.jeppeman.globallydynamic.selfhosted.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class UserConfirmationActivity extends Activity {
    private static final String EXTRA_PREFIX = "com.jeppeman.globallydynamic.PermissionActivity";
    private static final String STATE_HAS_LAUNCHED_INSTALL_CONFIRMATION
            = EXTRA_PREFIX + ".HAS_LAUNCHED_INSTALL_CONFIRMATION";

    private boolean hasLaunchedInstallConfirmation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_HAS_LAUNCHED_INSTALL_CONFIRMATION)) {
            finishWithInstallResult();
        } else {
            overridePendingTransition(R.anim.empty, R.anim.empty);
            ConfirmInstallFragment confirmInstallFragment = new ConfirmInstallFragment();
            confirmInstallFragment.show(getFragmentManager(), "ConfirmInstallFragment");
        }
    }

    private void finishWithInstallResult() {
        Intent extraData = new Intent();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q
                && getPackageManager().canRequestPackageInstalls()) {
            extraData.putExtra(
                    GlobalSplitInstallConfirmResult.EXTRA_RESULT,
                    GlobalSplitInstallConfirmResult.RESULT_CONFIRMED
            );
            setResult(Activity.RESULT_OK, extraData);
        } else {
            extraData.putExtra(
                    GlobalSplitInstallConfirmResult.EXTRA_RESULT,
                    GlobalSplitInstallConfirmResult.RESULT_DENIED
            );
            setResult(Activity.RESULT_CANCELED, extraData);
        }
        finish();
        overridePendingTransition(R.anim.empty, R.anim.empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasLaunchedInstallConfirmation) {
            finishWithInstallResult();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_HAS_LAUNCHED_INSTALL_CONFIRMATION, hasLaunchedInstallConfirmation);
        super.onSaveInstanceState(outState);
    }

    public static class ConfirmInstallFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Light_Dialog)
                    .setTitle(R.string.confirm_install_dialog_title)
                    .setMessage(getString(
                            R.string.confirm_install_dialog_message,
                            getActivity().getPackageManager()
                                    .getApplicationLabel(getActivity().getApplicationInfo())
                    ))
                    .setNegativeButton(R.string.confirm_install_dialog_negative_action, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ((UserConfirmationActivity) getActivity()).finishWithInstallResult();
                        }
                    })
                    .setPositiveButton(R.string.confirm_install_dialog_positive_action, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startConfirmInstallIntent();
                        }
                    })
                    .create();
        }

        private void startConfirmInstallIntent() {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setData(Uri.parse(String.format("package:%s", getActivity().getPackageName())));

            ((UserConfirmationActivity) getActivity()).hasLaunchedInstallConfirmation = true;
            getActivity().startActivity(intent);
            dismiss();
        }
    }
}
