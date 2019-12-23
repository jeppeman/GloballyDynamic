package com.jeppeman.globallydynamic.globalsplitinstall;

/**
 * A listener that can registered for {@link GlobalSplitInstallManager}s to be notified
 * of state updates during installation of splits
 */
public interface GlobalSplitInstallUpdatedListener {
    /**
     * Invoked when the state of an installation changes
     *
     * @param state the current state of an installation
     */
    void onStateUpdate(GlobalSplitInstallSessionState state);
}
