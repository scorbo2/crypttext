package ca.corbett.crypttext.ui;

/**
 * An interface for managing the saving and restoring of editor tab state.
 * Extensions can supply their own instance of this interface to handle
 * tab state management in a custom way. If no extension supplies
 * an instance, then DefaultTabStateManager will be used.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public interface TabStateManager {

    /**
     * Writes out a list of currently-open tabs so that it can be optionally restored
     * on the next application run. This method is invoked on application shutdown,
     * AFTER the user has been prompted to save any unsaved tabs.
     * <p>
     * You can enumerate the tabs to save with editorTabPane.getEditorTabs()
     * </p>
     * <p>
     * You should probably make sure the tab to be saved is not a scratch tab,
     * and that it actually has some content to save. But these are just suggestions.
     * </p>
     *
     * @param editorTabPane The EditorTabPane whose state should be saved.
     */
    void saveTabState(EditorTabPane editorTabPane);

    /**
     * Restores the previously-saved list of open tabs, if any. If no saved state is found,
     * or if the saved state is invalid, then this method should do nothing and simply return.
     * This method is invoked on application startup, before the main window is shown.
     * The editorTab will likely have one "untitled" empty tab open at this point (assuming
     * no command-line args were given). You can use editorTabPane.clearIfScratch() to remove
     * the "untitled" tab before populating the restored tabs, if you want.
     * <p>
     * You can load text content from a tab with: editorTabPane.getTextManager().fromFile(fileToRestore);
     * </p>
     * <p>
     * You can populate a new tab with: editorTabPane.newTextTab(restoredText, fileToRestore.getName());
     * </p>
     *
     * @param editorTabPane The EditorTabPane into which the saved state should be restored.
     */
    void restoreTabState(EditorTabPane editorTabPane);
}
