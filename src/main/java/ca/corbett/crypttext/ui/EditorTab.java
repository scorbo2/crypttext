package ca.corbett.crypttext.ui;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.CryptTextResourceLoader;
import ca.corbett.crypttext.VetoException;
import ca.corbett.crypttext.crypt.CryptMetadata;
import ca.corbett.crypttext.crypt.CryptUtil;
import ca.corbett.crypttext.crypt.DefaultCryptMetadata;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;
import ca.corbett.crypttext.text.Text;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.ScrollUtil;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a single tab in the editor area of the main window.
 * This tab tracks "dirty" status, and gives visual indications of whether
 * there are unsaved changes. Metadata about the file currently
 * loaded in this tab is also stored here, and can be accessed by extensions
 * via the getTextInstance() method. The current text contents of the tab can be accessed
 * via getCurrentText() - note that this may differ from the text in the Text instance,
 * if there are unsaved changes. Use getTextInstance().getText() to retrieve the
 * text as it was when this EditorTab was created or last saved.
 * Use getCurrentText() to retrieve the current text in the editor, which may include unsaved changes.
 * You can use setCurrentText() to replace the text in this editor tab with new text - this
 * will automatically mark the tab as dirty.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class EditorTab extends JPanel {

    /**
     * Listeners can subscribe to receive caret position updates from this editor tab.
     */
    @FunctionalInterface
    public interface PositionListener {
        void onPositionUpdate(int row, int column);
    }

    private static final Logger log = Logger.getLogger(EditorTab.class.getName());
    private MessageUtil messageUtil;

    private final List<PositionListener> positionListeners;
    private final EditorTabPane ownerPane;
    private final JTextPane textPane;
    private final EditorTabHeader tabHeader;
    private String name;
    private Text text;
    private CryptMetadata cryptMetadata;
    private boolean isDirty;

    /**
     * Creates a new, empty editor tab with the given name.
     *
     * @param ownerPane The EditorTabPane that will contain this tab.
     * @param name      The name for this tab.
     * @param text      The Text instance associated with this tab.
     */
    public EditorTab(EditorTabPane ownerPane, String name, Text text) {
        if (ownerPane == null) {
            throw new IllegalArgumentException("ownerPane cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (text == null) {
            throw new IllegalArgumentException("Given Text instance cannot be null");
        }
        this.positionListeners = new ArrayList<>();
        this.ownerPane = ownerPane;
        this.name = name;
        textPane = new JTextPane();
        textPane.addCaretListener(this::firePositionChangedEvent);
        setLayout(new BorderLayout());
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = ScrollUtil.buildScrollPane(textPane);
        wrapperPanel.add(scrollPane, BorderLayout.CENTER);
        add(wrapperPanel, BorderLayout.CENTER);
        this.text = text;
        textPane.setText(this.text.getText());
        this.cryptMetadata = generateCryptMetadata();
        textPane.getDocument().addDocumentListener(new DocListener());
        isDirty = false;
        tabHeader = new EditorTabHeader(this, name);
    }

    /**
     * Returns the name of this tab. This is either the name that was supplied to the constructor,
     * or the name of the source file, if this tab was created from a file.
     *
     * @return The name of this tab.
     */
    public String getTabName() {
        return name;
    }

    /**
     * Returns true if this tab is a scratch file, or false if it is not. Scratch files are files that
     * have been created in memory but not yet saved to a specific location on disk. Attempting to close
     * a tab that contains a scratch file will prompt the user to save the file, unless the current
     * text contents are empty, in which case the tab will be closed without prompting.
     */
    public boolean isScratchFile() {
        try {
            return ownerPane.getTextManager().isScratchFile(text.getSourceFile());
        }
        catch (IOException ignored) {
        }
        return false;
    }

    /**
     * Returns whether the CURRENT text contents of this tab are encrypted.
     * The text may be encrypted on disk, but this method will return false if the
     * user has decrypted the text in memory. To learn whether the text
     * was loaded from an encrypted file, you can use getCryptMetadata().wasEncryptedWhenLoaded().
     */
    public boolean isEncrypted() {
        return CryptUtil.isCryptTextWrapped(getCurrentText());
    }

    /**
     * Returns the current (one-based) row number of the caret in this editor tab.
     */
    public int getCaretRow() {
        int pos = textPane.getCaretPosition();
        return textPane.getDocument().getDefaultRootElement().getElementIndex(pos) + 1;
    }

    /**
     * Returns the current (one-based) column number of the caret in this editor tab.
     */
    public int getCaretColumn() {
        int pos = textPane.getCaretPosition();
        int row = textPane.getDocument().getDefaultRootElement().getElementIndex(pos) + 1;
        return pos - textPane.getDocument().getDefaultRootElement().getElement(row - 1).getStartOffset() + 1;
    }

    /**
     * Updates the name of this tab.
     */
    public void setTabName(String newName) {
        this.name = newName;
        tabHeader.updateLabel(newName);
    }

    /**
     * Returns the CryptMetadata instance associated with the text in this tab.
     */
    public CryptMetadata getCryptMetadata() {
        return cryptMetadata;
    }

    /**
     * Updates the CryptMetadata associated with the text in this tab.
     * Setting null is valid here, and will result in a DefaultCryptMetadata being used.
     */
    public void setCryptMetadata(CryptMetadata newMetadata) {
        if (newMetadata == null) {
            newMetadata = new DefaultCryptMetadata(CryptUtil.isCryptTextWrapped(getCurrentText()));
        }
        this.cryptMetadata = newMetadata;
    }

    /**
     * Closes this tab. User will be prompted to save changes if this tab is marked as dirty,
     * or if it is a scratch file with non-empty contents.
     */
    public void close() {
        ownerPane.closeTab(this);
    }

    /**
     * Commits the contents of this tab to disk, and marks this tab as clean.
     * If this tab is associated with a scratch file, the user will be prompted to choose a save location.
     * If any extension vetoes the save, then the save is canceled, a VetoException is thrown,
     * and this tab remains dirty.
     *
     * @throws IOException if an I/O error occurs during the save process.
     * @throws VetoException if any extension vetoes the save operation.
     */
    public void save() throws IOException, VetoException {
        if (isScratchFile()) {
            saveAs();
            return;
        }

        // Execute the save and mark ourselves as clean:
        // If an extension vetoes the save, then saveText will throw a VetoException,
        // and we will remain dirty.
        text = ownerPane.getTextManager().saveText(text, getCurrentText());
        isDirty = false;
        tabHeader.updateLabel(name); // removes the visual dirty indicator
        tabHeader.resetIcon(); // swap icon colors for more visual indication of clean state
    }

    /**
     * Prompts the user for a new save location for this tab, saves the contents
     * to that location, then marks this tab as clean.
     * If any extension vetoes the save, then the save is canceled, and this tab remains dirty.
     */
    public void saveAs() throws IOException {
        JFileChooser fileChooser = MainWindow.getInstance().getFileChooser();
        int result = fileChooser.showSaveDialog(MainWindow.getInstance());
        if (result == JFileChooser.APPROVE_OPTION) {

            // If the target file already exists, prompt to confirm the overwrite:
            // (our TextManager doesn't care and will happily overwrite, but we can add a safety check here)
            if (fileChooser.getSelectedFile().exists()) {
                int overwriteResult = getMessageUtil().askYesNo(
                        "Confirm overwrite",
                        "The file \""
                                + fileChooser.getSelectedFile().getName()
                                + "\" already exists. Do you want to overwrite it?"
                );
                if (overwriteResult != MessageUtil.YES) {
                    return; // user chose not to overwrite, so abort the save action
                }
            }

            try {
                text = ownerPane.getTextManager().saveTextAs(text, getCurrentText(), fileChooser.getSelectedFile());
                isDirty = false;
                setTabName(fileChooser.getSelectedFile().getName());
                tabHeader.resetIcon();
            }
            catch (VetoException ignored) {
                // Save was vetoed by an extension!
                // Veto already logged by TextManager - just stay dirty and do nothing here.
            }
        }
    }

    /**
     * Returns a custom tab header component to replace Java Swing's built-in option, which is insufficient.
     * Our custom tab header contains not only the optional icon, and the tab name, but also a close
     * button and an "is dirty" indicator.
     *
     * @return A tab header component for this editor tab.
     */
    public JPanel getTabHeader() {
        return tabHeader;
    }

    /**
     * Reports whether this tab has unsaved changes.
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Returns the text currently held in this editor tab.
     */
    public String getCurrentText() {
        return textPane.getText();
    }

    /**
     * Replaces the text currently held in this editor tab with the given new text, and marks the tab as dirty.
     */
    public void setCurrentText(String newText) {
        textPane.setText(newText);
        markDirty();
    }

    /**
     * Returns the Text instance associated with this editor tab.
     * Text instances are immutable. That means the returned instance's text
     * may not match the current text in this editor tab. To retrieve
     * the current (possibly not yet saved) text contents of this
     * tab, use the getCurrentText() method instead.
     */
    public Text getTextInstance() {
        return text;
    }

    private void markDirty() {
        isDirty = true;
        tabHeader.updateLabel(name); // adds the visual dirty indicator
        tabHeader.resetIcon(); // swap icon colors for more visual indication of dirty state
    }

    /**
     * Invoked internally to generate a CryptMetadata instance for the given Text instance.
     * This may be supplied by extensions, or a default one will be used.
     */
    private CryptMetadata generateCryptMetadata() {
        // Check if any extension wants to provide custom CryptMetadata for this Text instance:
        CryptMetadata cryptMetadata = CryptTextExtensionManager.getInstance().generateCryptMetadata(getCurrentText());
        if (cryptMetadata != null) {
            return cryptMetadata;
        }

        // If not, use the default implementation:
        return new DefaultCryptMetadata(CryptUtil.isCryptTextWrapped(getCurrentText()));
    }

    /**
     * Will return an Icon appropriate for this editor tab, based on current
     * application settings. There is no caching - a new icon with appropriate
     * sizing will be returned every time this method is invoked.
     * <p>
     * If tab header icons are disabled in application settings,
     * this method will return null.
     * </p>
     */
    public ImageIcon getIcon() {
        if (!AppConfig.getInstance().isTabLockIconsEnabled()) {
            return null; // easy path
        }

        boolean isEncrypted = CryptUtil.isCryptTextWrapped(getCurrentText());
        int iconSize = AppConfig.getInstance().getTabIconSize();
        return isEncrypted
                ? CryptTextResourceLoader.getLockIcon(iconSize)
                : CryptTextResourceLoader.getUnlockIcon(iconSize);

    }

    public void addPositionListener(PositionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        positionListeners.add(listener);
    }

    public void removePositionListener(PositionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        positionListeners.remove(listener);
    }

    private void firePositionChangedEvent(CaretEvent caretEvent) {
        // If no one is listening, don't bother:
        if (positionListeners.isEmpty()) {
            return;
        }

        // Translate CaretEvent's "dot" into row and column numbers:
        int pos = caretEvent.getDot();
        int row = textPane.getDocument().getDefaultRootElement().getElementIndex(pos) + 1;
        int col = pos - textPane.getDocument().getDefaultRootElement().getElement(row - 1).getStartOffset() + 1;

        // Notify listeners:
        try {
            // Iterate over a copy of the list to avoid ConcurrentModificationExceptions:
            for (PositionListener listener : new ArrayList<>(positionListeners)) {
                listener.onPositionUpdate(row, col);
            }
        }
        catch (Exception e) {
            // If a listener throws a runtime exception, don't let it interfere with this EditorTab:
            log.warning("Failed to fire position changed event: " + e.getMessage());
        }
    }

    /**
     * A very simple DocumentListener that will mark this editor tab as dirty
     * whenever any change is made.
     */
    private class DocListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            markDirty();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            markDirty();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            markDirty();
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
