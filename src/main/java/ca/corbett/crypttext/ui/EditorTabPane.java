package ca.corbett.crypttext.ui;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.CryptTextResourceLoader;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;
import ca.corbett.crypttext.text.Text;
import ca.corbett.crypttext.text.TextManager;
import ca.corbett.extras.MessageUtil;

import javax.swing.JTabbedPane;
import java.awt.Component;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This class extends JTabbedPane and offers convenience methods for
 * adding and managing editor tabs.
 * <p>
 * <b>Usage:</b> - you can treat this as a regular JTabbedPane, and add
 * any Component using the usual parent class methods. This class also
 * offers an addEditorTab() method that
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class EditorTabPane extends JTabbedPane {

    public static final String DEFAULT_TAB_NAME = "Untitled";

    private static final Logger log = Logger.getLogger(EditorTabPane.class.getName());
    private MessageUtil messageUtil;
    private final TextManager textManager;
    private static int UNTITLED_COUNT = 1; // used to generate default names for new tabs

    public EditorTabPane() {
        this.textManager = buildTextManager();
    }

    /**
     * Creates a new, untitled Text tab.
     */
    public void newTextTab() {
        newTextTab(null);
    }

    /**
     * Creates a new Text tab with the given title. If the title is null,
     * a default title will be assigned to the tab (e.g. "Untitled 1", "Untitled 2", etc.)
     *
     * @param title the title to assign to the new tab, or null for a default title
     */
    public void newTextTab(String title) {
        try {
            newTextTab(textManager.newText(), title);
        }
        catch (IOException ioe) {
            getMessageUtil().error("Error creating new tab",
                                   "An error occurred while creating a new tab:\n" + ioe.getMessage());
        }
    }

    /**
     * Creates a new Text tab for the given Text instance.
     * The tab's title will be set to the name of the Text's source file.
     *
     * @param text  the Text instance to display in the new tab
     * @param title The title for the new tab
     */
    public void newTextTab(Text text, String title) {
        if (text == null) {
            throw new IllegalArgumentException("Given Text instance cannot be null");
        }
        title = (title == null) ? DEFAULT_TAB_NAME + " " + UNTITLED_COUNT++ : title;

        // It could be that the given Text instance is already being shown in another tab.
        // In that case, let's see if we can find it, and just switch to that tab instead of adding a new one:
        for (int i = 0; i < getTabCount(); i++) {
            Component tab = getComponentAt(i);
            if (tab instanceof EditorTab editorTab) {
                if (editorTab.getTextInstance().isSameSourceFile(text)) {
                    setSelectedIndex(i);
                    return;
                }
            }
        }

        // If we get here, then we're good to add a new tab for this guy:
        EditorTab editorTab = new EditorTab(this, title, text);
        addTab(title, editorTab.getIcon(), editorTab.getScrollPane());
        setTabComponentAt(getTabCount() - 1, editorTab.getTabHeader());
    }

    TextManager getTextManager() {
        return textManager;
    }

    /**
     * Invoked from EditorTab.close() to remove the given tab from this tab pane.
     *
     * @param editorTab
     */
    void closeTab(EditorTab editorTab) {
        if (editorTab == null) {
            throw new IllegalArgumentException("Given EditorTab cannot be null");
        }

        // Check if a save is needed, and prompt if so:
        if (editorTab.isDirty() || (editorTab.isScratchFile() && !editorTab.getTextInstance().getText().isEmpty())) {
            int result = getMessageUtil().askYesNoCancel("Unsaved changes",
                                                         "This tab has unsaved changes. Do you want to save before closing?");
            if (result == MessageUtil.CANCEL) {
                return; // tab stays open because user canceled.
            }
            if (result == MessageUtil.YES) {
                // TODO prompt for location if isScratchFile() is true
                try {
                    // For now, just save to the same file even if it's a scratch file. TODO clean this up
                    Text originalText = editorTab.getTextInstance();
                    Text savedText = textManager.saveText(editorTab.getTextInstance(), editorTab.getCurrentText());

                    // If we get back the same instance, then the save was vetoed, so DON'T close the tab:
                    if (savedText == originalText) {
                        // TODO log this? we current rely on the vetoing extension to show a message, but did they?
                        return; // error dialog was presumably shown to user by whoever vetoed the save
                    }
                }
                catch (IOException ioe) {
                    getMessageUtil().error("Error saving file",
                                           "An error occurred while saving the file:\n" + ioe.getMessage());
                    return; // abort the close action if we couldn't save
                }
            }
        }
        int index = indexOfComponent(editorTab.getScrollPane());
        if (index != -1) {
            removeTabAt(index);
        }

        // If that was the last tab, we may have to close the app:
        if (getTabCount() == 0) {
            if (AppConfig.getInstance().isExitOnCloseLastTabEnabled()) {
                MainWindow.getInstance().dispose();
            }
        }
    }

    /**
     * Updates tab header icons as needed based on current application settings.
     */
    public void updateTabIcons() {
        // Start by removing all icons:
        for (int i = 0; i < getTabCount(); i++) {
            setIconAt(i, null);
        }

        // If enabled, add icons at the configured size:
        if (AppConfig.getInstance().isTabLockIconsEnabled()) {
            int iconSize = AppConfig.getInstance().getTabIconSize();
            for (int i = 0; i < getTabCount(); i++) {
                // We'll have to add smarts here once we add the ability to encrypt
                // data... for now, just show the "unlocked" icon for all tabs:
                setIconAt(i, CryptTextResourceLoader.getUnlockIcon(iconSize));
            }
        }
    }

    /**
     * Creates, configures, and returns a TextManager that is wired up to our extension manager.
     */
    private TextManager buildTextManager() {
        TextManager textManager = new TextManager();
        final CryptTextExtensionManager extManager = CryptTextExtensionManager.getInstance();
        textManager.addTextWillLoadListener((m, f) -> extManager.fileWillLoad(f));
        textManager.addTextWillSaveListener((m, t, f) -> extManager.fileWillSave(t.getSourceFile(), t.getText()));
        textManager.addTextLoadedListener((m, t) -> extManager.fileLoaded(t.getSourceFile(), t.getText()));
        textManager.addTextSavedListener((m, t) -> extManager.fileSaved(t.getSourceFile()));
        return textManager;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
