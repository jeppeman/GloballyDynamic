package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.Activity;
import android.content.IntentSender;

import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTask;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Manages installations of splits; all methods delegate to the underlying SplitInstallManagers
 */
public interface GlobalSplitInstallManager {
    /**
     * Registers a listener that gets notified as the state of installation requests changes.
     *
     * @param listener the listener to register
     */
    void registerListener(GlobalSplitInstallUpdatedListener listener);

    /**
     * Unregisters a previously registered listener
     * @param listener the listener to unregister
     */
    void unregisterListener(GlobalSplitInstallUpdatedListener listener);

    /**
     * Launches a confirmation dialog from the provided {@link GlobalSplitInstallSessionState}
     *
     * @param sessionState the {@link GlobalSplitInstallSessionState} to launch based on
     * @param activity the {@link Activity} to launch from
     * @param requestCode the request code to launch with
     * @return if the state is valid or not, i.e. the dialog was launched
     *
     * @throws IntentSender.SendIntentException if launching the dialog fails
     */
    boolean startConfirmationDialogForResult(GlobalSplitInstallSessionState sessionState, Activity activity, int requestCode) throws IntentSender.SendIntentException;

    /**
     * Starts an installation of splits based on the provided {@link GlobalSplitInstallRequest}.
     *
     * @param request an object specifying which splits to download
     * @return a {@link GlobalSplitInstallTask} performing the request
     */
    GlobalSplitInstallTask<Integer> startInstall(GlobalSplitInstallRequest request);

    /**
     * Starts an installation of the splits that are missing (if any) from this application, i.e.
     * missing install time features apk:s, language apk:s, density apk:s etc.
     *
     * <p>This is useful if you use Dynamic Delivery on an App Store that does not support it natively,
     * e.g. Amazon App Store and Samsung Galaxy Store. There you will have to upload a universal apk
     * or the base apk - if you only upload the base apk you can use this method to install any
     * missing splits from you GloballyDynamic Server.</p>
     *
     * <b>Note</b>: this only has any effect in the com.jeppeman.globallydynamic.android:selfhosted
     * artifact, for gplay and huawei it is a no-op Google Play Store and Huawei App Gallery
     * support Dynamic Delivery natively.
     *
     * @return a {@link GlobalSplitInstallTask} performing the request
     */
    GlobalSplitInstallTask<Integer> installMissingSplits();

    /**
     * Tries to cancel an ongoing installation
     *
     * @param sessionId the session id of the installation to cancel
     * @return a {@link GlobalSplitInstallTask} performing the cancellation
     */
    GlobalSplitInstallTask<Void> cancelInstall(int sessionId);

    /**
     * Performs a deferred installation of the provided modules.
     *
     * @param moduleNames the modules to install
     * @return a {@link GlobalSplitInstallTask} performing the deferred installation
     */
    GlobalSplitInstallTask<Void> deferredInstall(List<String> moduleNames);

    /**
     * Performs a deferred uninstallation of the provided modules
     *
     * @param moduleNames the modules to uninstall
     * @return a {@link GlobalSplitInstallTask} performing the deferred uninstallation
     */
    GlobalSplitInstallTask<Void> deferredUninstall(List<String> moduleNames);

    /**
     * Performs a deferred language installation
     *
     * @param languages the languages to install
     * @return a {@link GlobalSplitInstallTask} performing the deferred installation
     */
    GlobalSplitInstallTask<Void> deferredLanguageInstall(List<Locale> languages);

    /**
     * Performs a deferred language uninstallation
     *
     * @param languages the languages to uninstall
     * @return a {@link GlobalSplitInstallTask} performing the deferred uninstallation
     */
    GlobalSplitInstallTask<Void> deferredLanguageUninstall(List<Locale> languages);

    /**
     * Gets the {@link GlobalSplitInstallSessionState} of an existing session.
     *
     * @param sessionId the session id of the state to get
     * @return a {@link GlobalSplitInstallTask} that gets state of the session
     */
    GlobalSplitInstallTask<GlobalSplitInstallSessionState> getSessionState(int sessionId);

    /**
     * Gets the {@link GlobalSplitInstallSessionState} all existing sessions.
     *
     * @return a {@link GlobalSplitInstallTask} that gets state of all existing sessions.
     */
    GlobalSplitInstallTask<List<GlobalSplitInstallSessionState>> getSessionStates();

    /**
     * Gets all installed language splits.
     *
     * @return the installed language splits
     */
    Set<String> getInstalledLanguages();

    /**
     * Get all installed split modules
     *
     * @return the installed split modules
     */
    Set<String> getInstalledModules();
}
