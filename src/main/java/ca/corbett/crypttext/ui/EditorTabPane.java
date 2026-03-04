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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
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
    private final List<EditorTab> editorTabs = new CopyOnWriteArrayList<>();
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
        editorTabs.add(editorTab);
        addTab(title, editorTab.getIcon(), editorTab);
        setTabComponentAt(getTabCount() - 1, editorTab.getTabHeader());

        // Select this tab immediately:
        setSelectedIndex(getTabCount() - 1);
    }

    /**
     * Returns a defensive copy of our EditorTab list.
     */
    public List<EditorTab> getEditorTabs() {
        return new ArrayList<>(editorTabs);
    }

    /**
     * Returns the TextManager that this EditorTabPane uses to manage text loading and saving.
     */
    public TextManager getTextManager() {
        return textManager;
    }

    /**
     * "Scratch state" means that there is only one tab open, the tab contains a scratch file,
     * and the tab contains no text. This will also return true if there are no tabs open.
     */
    public boolean isScratchState() {
        if (getTabCount() == 0) {
            return true;
        }

        if (getTabCount() == 1) {
            Component tab = getComponentAt(0);
            if (tab instanceof EditorTab editorTab) {
                return editorTab.isScratchFile() && editorTab.getCurrentText().isEmpty();
            }
        }

        return false;
    }

    /**
     * If isScratchState() is true, this will close any open scratch tabs.
     * This will not trigger the "exit if last tab closed" option even if enabled.
     */
    public void clearIfScratch() {
        if (isScratchState()) {
            if (getTabCount() > 0) {
                // We know there is only one tab, and it's a scratch file, so we can just remove it without prompting:
                // (this isn't terribly thread-safe, though)
                Component c = getComponentAt(0);
                if (c instanceof EditorTab editorTab) {
                    try {
                        textManager.remove(editorTab.getTextInstance());
                    }
                    catch (IOException ioe) {
                        // Just log it, not worth a popup dialog:
                        log.log(Level.WARNING, "Error cleaning up TextManager after clearing scratch tab for file: "
                                + editorTab.getTextInstance().getSourceFile(), ioe);
                    }
                }
                removeTabAt(0);
                editorTabs.clear();
            }
        }
    }

    /**
     * Returns the currently selected tab component, or null if there are no tabs.
     * The returned Component will <b>usually</b> be an instance of EditorTab,
     * but this is not guaranteed! Callers must use instanceof to see what they got.
     */
    public Component getCurrentTab() {
        if (getTabCount() == 0) {
            return null;
        }
        return getComponentAt(getSelectedIndex());
    }

    /**
     * Invoked from EditorTab.close() to remove the given tab from this tab pane.
     *
     * @param editorTab the tab to close. must not be null.
     */
    void closeTab(EditorTab editorTab) {
        if (editorTab == null) {
            throw new IllegalArgumentException("Given EditorTab cannot be null");
        }

        // Check if a save is needed, and prompt if so:
        if (editorTab.isDirty() || (editorTab.isScratchFile() && !editorTab.getCurrentText().isEmpty())) {
            int result = getMessageUtil().askYesNoCancel("Unsaved changes",
                                                         "This tab has unsaved changes. Do you want to save before closing?");
            if (result == MessageUtil.CANCEL) {
                return; // tab stays open because user canceled.
            }
            if (result == MessageUtil.YES) {
                try {
                    editorTab.save();
                }
                catch (IOException ioe) {
                    getMessageUtil().error("Error saving file",
                                           "An error occurred while saving the file:\n" + ioe.getMessage());
                    return; // abort the close action if we couldn't save
                }
            }
        }
        int index = indexOfComponent(editorTab);
        if (index != -1) {
            removeTabAt(index);
            editorTabs.remove(editorTab);

            // Also clean up TextManager (remove from cache, clean scratch file, etc.):
            try {
                textManager.remove(editorTab.getTextInstance());
            }
            catch (IOException ioe) {
                // Just log it, not worth a popup dialog:
                log.log(Level.WARNING, "Error cleaning up TextManager after closing tab for file: "
                        + editorTab.getTextInstance().getSourceFile(), ioe);
            }
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
        textManager.addTextWillSaveListener((m, t, n, f) -> extManager.fileWillSave(t.getSourceFile(), n, f));
        textManager.addTextLoadedListener((m, t) -> extManager.fileLoaded(t.getSourceFile(), t.getText()));
        textManager.addTextSavedListener((m, s, t) -> extManager.fileSaved(s, t.getSourceFile()));
        return textManager;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
