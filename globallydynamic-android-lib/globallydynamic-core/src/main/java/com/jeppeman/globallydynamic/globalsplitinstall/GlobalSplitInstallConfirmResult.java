package com.jeppeman.globallydynamic.globalsplitinstall;

/**
 * Intent extras included in a result returned after a request package installs, this is only relevant
 * for Android 11 and a above in combination with the com.jeppeman.globallydynamic.android:selfhosted
 * artifact.
 *
 * Starting with R, the application needs to undergo a full restart after the user has allowed
 * it to request package installs; the flow of the first split install request therefore becomes a bit different:
 *
 * Q and below: Request install > Download splits > Prompt user confirmation > install
 * R and above: Request install > Prompt user confirmation > Restart app > Request again
 *
 * It can be handled as follows for the best possible user experience on R:
 * <pre>{@code
 * protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 *     if (requestCode == MY_REQUEST_CODE
 *              && data.hasExtra(GlobalSplitInstallConfirmResult.EXTRA_RESULT) {
 *          boolean confirmResult = data.getBooleanExtra(
 *              GlobalSplitInstallConfirmResult.EXTRA_RESULT,
 *              GlobalSplitInstallConfirmResult.RESULT_DENIED
 *          );
 *          if (confirmResult == GlobalSplitInstallConfirmResult.RESULT_CONFIRMED) {
 *              // Run the install again with newly acquired install privileges
 *              installMyModule();
 *          } else {
 *              // User did not accept to install packages, react if needed
 *          }
 *     }
 * }
 *
 * private void installMyModule() {
 *     GlobalSplitInstallRequest request = GlobalSplitInstallRequest.newBuilder()
 *         .addModule("myModule")
 *         .build();
 *
 *     globalSplitInstallManager.registerListener(new GlobalSplitInstallUpdatedListener() {
 *         @Override
 *         public void onStateUpdate(GlobalSplitInstallSessionState state) {
 *              if (state.status() == GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION) {
 *                  globalSplitInstallManager.startConfirmationDialogForResult(state, myActivity, MY_REQUEST_CODE);
 *              }
 *         }
 *     });
 *     globalSplitInstallManager.startInstall(request);
 * }
 * }</pre>
 */
public class GlobalSplitInstallConfirmResult {
    public static final String EXTRA_RESULT = "com.jeppeman.globallydynamic.EXTRA_RESULT";
    public static final int RESULT_CONFIRMED = 1;
    public static final int RESULT_DENIED = 0;
}
