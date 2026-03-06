package ca.corbett.crypttext.ui;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.text.Text;
import ca.corbett.extras.io.FileSystemUtil;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This default implementation of TabStateManager is very simple, and
 * saves tab state to a flat text file in the application settings directory.
 * Extensions can supply their own implementation of TabStateManager to save
 * state in a different way.
 * <p>
 * This behavior is enabled or disabled
 * via the "Restore tabs on startup" option in AppConfig. If disabled,
 * then these methods deliberately do nothing.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class DefaultTabStateManager implements TabStateManager {

    private static final Logger logger = Logger.getLogger(DefaultTabStateManager.class.getName());
    protected static final File TAB_STATE_FILE = new File(Version.SETTINGS_DIR, "tab_state");

    public DefaultTabStateManager() {
    }

    /**
     * Writes out a list of currently-open tabs so that it can be optionally restored
     * on the next application run. Note that we don't store this in AppConfig, as it's
     * not really a preference - it's more of a "session" state.
     * But the user can enable/disable this behavior in AppConfig.
     */
    @Override
    public void saveTabState(EditorTabPane editorTabPane) {
        // If this option is disabled, do nothing:
        if (!AppConfig.getInstance().isRestoreTabsOnStartup()) {
            // Note we don't clear out the list of saved tabs, if any.
            // This allows the option to be re-enabled without
            // losing any saved tab state.
            return;
        }

        // This default implementation just uses a simple flat text file for persistence.
        // Extensions can optionally supply a database-backed implementation, or json, or yaml, or whatever.
        StringBuilder tabList = new StringBuilder();
        for (EditorTab editorTab : editorTabPane.getEditorTabs()) {

            // Skip scratch files and files with no content:
            if (editorTab.getTextInstance().getSourceFile() == null
                    || editorTab.isScratchFile()) {
                continue;
            }

            // This is a non-scratch file, so we'll save it:
            tabList.append(editorTab.getTextInstance().getSourceFile().getAbsolutePath()).append("\n");
        }
        try {
            FileSystemUtil.writeStringToFile(tabList.toString(), TAB_STATE_FILE);
        }
        catch (IOException ioe) {
            // Just log it... we don't want to block shutdown over this:
            logger.log(Level.WARNING, "Error saving tab state: " + ioe.getMessage(), ioe);
        }
    }

    /**
     * Attempts to restore previously-opened tabs from the last application run.
     * Note that we don't store this in AppConfig, as it's not really a preference - it's more of a "session" state.
     * But the user can enable/disable this behavior in AppConfig.
     */
    @Override
    public void restoreTabState(EditorTabPane editorTabPane) {
        // If this option is disabled, do nothing:
        if (!AppConfig.getInstance().isRestoreTabsOnStartup()) {
            // Note we don't clear out the list of saved tabs, if any.
            // This allows the option to be re-enabled without
            // losing any saved tab state.
            return;
        }

        // If there's no tab state file, then I guess we're done here:
        if (!TAB_STATE_FILE.exists() || !TAB_STATE_FILE.isFile() || !TAB_STATE_FILE.canRead()) {
            logger.info("No tab state file found, or file is not readable. Skipping tab restore.");
            return;
        }

        boolean cleared = false;
        try {
            // Read the raw list, and split it by newlines to get the individual file paths:
            for (String path : FileSystemUtil.readFileLines(TAB_STATE_FILE)) {
                File toRestore = new File(path);
                if (!toRestore.exists() || !toRestore.isFile() || !toRestore.canRead()) {
                    logger.log(Level.WARNING, "Unable to restore tab for file path: " + path
                            + " - file does not exist or is not readable.");
                    continue;
                }
                Text restoredText = editorTabPane.getTextManager().fromFile(toRestore);

                // TextManager may return null if some extension vetoes the load.
                if (restoredText == null) {
                    // No need to log... the vetoing extension presumably already notified the user.
                    continue;
                }

                // Remove the default "untitled" tab if one is present:
                if (!cleared) {
                    cleared = true; // only need to do this once
                    editorTabPane.clearIfScratch();
                }

                editorTabPane.newTextTab(restoredText, toRestore.getName());
            }
        }
        catch (IOException ioe) {
            // Just log it... we don't want to block startup over this:
            logger.log(Level.WARNING, "Error reading tab state file: " + ioe.getMessage(), ioe);
        }
    }
}
