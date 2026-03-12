package ca.corbett.crypttext.ui;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.CryptTextResourceLoader;
import ca.corbett.crypttext.VetoException;
import ca.corbett.crypttext.crypt.CryptMetadata;
import ca.corbett.crypttext.crypt.CryptUtil;
import ca.corbett.crypttext.crypt.DefaultCryptMetadata;
import ca.corbett.crypttext.crypt.EncryptedText;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;
import ca.corbett.crypttext.text.Text;
import ca.corbett.crypttext.ui.actions.UIReloadAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.ScrollUtil;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import javax.swing.undo.UndoManager;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a single tab in the editor area of the main window.
 * This tab tracks "dirty" status, and gives visual indications of whether
 * there are unsaved changes. Metadata about the file currently
 * loaded in this tab is also stored here, and can be accessed by extensions
 * via the getDiskContents() method. The current text contents of the tab can be accessed
 * via getMemoryContents() - note that this may differ from the disk contents,
 * if there are unsaved changes. Use getDiskContents().getText() to retrieve the
 * text as it was when this EditorTab was created or last saved.
 * Use getMemoryContents() to retrieve the current text in the editor, which may include unsaved changes.
 * You can use setMemoryContents() to replace the text in this editor tab with new text - this
 * will automatically mark the tab as dirty.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class EditorTab extends JPanel implements UIReloadable {

    /**
     * Listeners can subscribe to receive caret position updates from this editor tab.
     */
    @FunctionalInterface
    public interface PositionListener {
        void onPositionUpdate(int row, int column);
    }

    /**
     * Listeners can subscribe to receive notifications when the text content of this
     * editor tab changes. This can be user modifications, programmatic changes via setCurrentText(),
     * or the result of an encryption or decryption action.
     */
    @FunctionalInterface
    public interface ContentChangeListener {
        void onContentChange(String newContent);
    }

    private static final Logger log = Logger.getLogger(EditorTab.class.getName());
    private MessageUtil messageUtil;

    private final List<PositionListener> positionListeners;
    private final List<ContentChangeListener> contentChangeListeners;
    private final EditorTabPane ownerPane;
    private final JTextPane textPane; // stores our memoryContents
    private final JScrollPane scrollPane;
    private final LineNumberGutter gutter;
    private final EditorTabHeader tabHeader;
    private final UndoManager undoManager;
    private final GroupingUndoableEditListener undoableEditListener;
    private String name;
    private Text diskContents;
    private CryptMetadata cryptMetadata;
    private boolean isDirty;
    private boolean eventsEnabled = true; // used to prevent firing events during programmatic text changes

    /**
     * Creates a new, empty editor tab with the given name.
     *
     * @param ownerPane The EditorTabPane that will contain this tab.
     * @param name      The name for this tab.
     * @param diskContents      The Text instance associated with this tab.
     */
    public EditorTab(EditorTabPane ownerPane, String name, Text diskContents) {
        if (ownerPane == null) {
            throw new IllegalArgumentException("ownerPane cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (diskContents == null) {
            throw new IllegalArgumentException("diskContents cannot be null");
        }
        this.positionListeners = new ArrayList<>();
        this.contentChangeListeners = new ArrayList<>();
        this.ownerPane = ownerPane;
        this.name = name;
        textPane = new JTextPane();
        textPane.setFont(AppConfig.getInstance().getEditorFont());
        textPane.addCaretListener(this::firePositionChangedEvent);
        setLayout(new BorderLayout());
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        scrollPane = ScrollUtil.buildScrollPane(textPane);
        gutter = new LineNumberGutter(textPane);
        scrollPane.setRowHeaderView(AppConfig.getInstance().isShowLineNumbers() ? gutter : null);
        wrapperPanel.add(scrollPane, BorderLayout.CENTER);
        add(wrapperPanel, BorderLayout.CENTER);
        this.diskContents = diskContents;
        textPane.setText(this.diskContents.getText()); // initial value
        textPane.setCaretPosition(0); // move caret to beginning of whatever text we just loaded.
        this.cryptMetadata = generateCryptMetadata();
        textPane.getDocument().addDocumentListener(new DocListener());
        undoManager = new UndoManager();
        undoManager.setLimit(AppConfig.getInstance().getUndoLimit());
        undoableEditListener = new GroupingUndoableEditListener(undoManager);
        textPane.getDocument().addUndoableEditListener(undoableEditListener);
        isDirty = false;
        tabHeader = new EditorTabHeader(this, name);
        MainWindow.configureDropTarget(textPane, getMessageUtil());
        UIReloadAction.getInstance().registerReloadable(this);
        reloadUI(); // force an immediate update to pick up the correct theme and color scheme
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
            return ownerPane.getTextManager().isScratchFile(diskContents.getSourceFile());
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
        return CryptUtil.isCryptTextWrapped(getMemoryContents());
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
     * Undoes the last edit in this editor tab, if possible.
     */
    public void undo() {
        undoableEditListener.flush(); // commit any in-progress group before undoing, so that undo will work as expected
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
    }

    /**
     * Redoes the last undone edit in this editor tab, if possible.
     */
    public void redo() {
        undoableEditListener.flush(); // commit any in-progress group before redoing, so that redo will work as expected
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
    }

    /**
     * Invoke this to try to set the keyboard focus to the text pane in this editor tab.
     * This is not guaranteed to work, just because of the way the focus system
     * works in general, but we'll give it a shot.
     */
    public void requestFocusInTextPane() {
        ownerPane.setSelectedComponent(this); // first, make sure we're the active tab, otherwise focus won't come to us
        SwingUtilities.invokeLater(textPane::requestFocusInWindow);
    }

    /**
     * Updates the name of this tab.
     */
    public void setTabName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Tab name cannot be null or blank");
        }
        this.name = newName;
        ownerPane.updateTabName(this, newName);
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
            newMetadata = new DefaultCryptMetadata(CryptUtil.isCryptTextWrapped(getMemoryContents()));
        }
        this.cryptMetadata = newMetadata;
    }

    /**
     * Closes this tab. User will be prompted to save changes if this tab is marked as dirty,
     * or if it is a scratch file with non-empty contents.
     */
    public void close() {
        if (ownerPane.closeTab(this)) {
            // Our request to close the tab was not canceled or vetoed, so we are actually closing:
            dispose();
        }
    }

    /**
     * Performs any necessary cleanup when this tab is closed, such as unregistering from listeners.
     */
    public void dispose() {
        UIReloadAction.getInstance().unregisterReloadable(this); // stop listening
        undoableEditListener.flush(); // commit any pending group and stop the timer
        textPane.getDocument().removeUndoableEditListener(undoableEditListener);
    }

    /**
     * Commits the contents of this tab to disk, and marks this tab as clean.
     * If the text contents were loaded from an encrypted file, this method will ONLY save encrypted contents.
     * If our current contents are still encrypted, we just save as-is.
     * If our current contents have been decrypted in-memory, we will re-encrypt before saving,
     * without changing the in-memory contents. That is, the user will still see the decrypted contents in this tab.
     * The tab will NOT be marked as dirty in that case (even though in-memory contents and disk contents will differ).
     * <p>
     * If the password associated with this tab has been forgotten, we will prompt for it before saving.
     * This method NEVER saves decrypted text to disk if the text was originally encrypted - the user must
     * use the "save unencrypted" action to explicitly do that.
     * </p>
     * <p>
     * If this tab is associated with a scratch file, this method will immediately defer to the "save as" flow.
     * </p>
     * <p>
     * Application extensions have veto power over save operations!
     * If any extension vetoes the save, then the save is canceled, a VetoException is thrown,
     * and this tab remains dirty.
     * </p>
     *
     * @throws Exception on I/O error, encryption error, or if any extension vetoes the save operation.
     */
    public void save() throws Exception {
        // If this is a scratch file, force a "save as" flow instead:
        if (isScratchFile()) {
            saveAs();
            return;
        }

        // Resolve the text that we want to save (handles encryption if necessary):
        EncryptedText textToSave = resolveTextForSave();
        if (textToSave == null) {
            return; // user canceled during encryption step, so we can abort here
        }

        // Now we can try to save the encrypted text to disk, without changing our in-memory contents:
        // This will throw a VetoException if any extension vetoes the save, or possibly an IOException:
        diskContents = ownerPane.getTextManager().saveText(diskContents, textToSave.getText());

        // Update our CryptMetadata, which may have changed above:
        // (we do this after the save, in case the save fails or was vetoed):
        setCryptMetadata(textToSave.getCryptMetadata());

        // Okay, if we get here, our contents were saved in an encrypted state. Hooray!
        // We have noted our new disk contents, and we can now mark ourselves as NOT dirty.
        // That may seem wrong, because our in-memory contents do not match our disk contents,
        // but it makes sense when you consider the above flow: the in-memory contents were
        // successfully encrypted and saved to disk.
        markClean();
    }

    /**
     * Prompts the user for a new save location for this tab, saves the contents
     * to that location, then marks this tab as clean.
     * If any extension vetoes the save, then the save is canceled, and this tab remains dirty.
     * Text that was loaded from an encrypted file will be saved in an encrypted state.
     */
    public void saveAs() throws Exception {
        boolean wasDirty = isDirty(); // make a note of our current state, so we can restore it if the save is vetoed
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
                // Handle encryption of our in-memory contents before saving, if needed:
                EncryptedText textToSave = resolveTextForSave();
                if (textToSave == null) {
                    return; // user canceled encryption prompt
                }
                File newFile = fileChooser.getSelectedFile();
                diskContents = ownerPane.getTextManager().saveTextAs(diskContents, textToSave.getText(), newFile);
                setCryptMetadata(textToSave.getCryptMetadata()); // update this, as it may have changed
                markClean();

                // If we were a scratch file, we now have an actual name.
                // If we weren't a scratch file, our name has likely changed.
                setTabName(newFile.getName());
            }
            catch (VetoException ignored) {
                // Save was vetoed by an extension!
                // Veto already logged by TextManager - just stay dirty and do nothing here.
                if (wasDirty) {
                    markDirty();
                }
            }
        }
    }

    /**
     * Decrypts the contents of this tab in memory (if needed) and saves the
     * raw plaintext to a file of the user's choosing. We will discard any
     * CryptMetadata associated with this tab, and immediately forget any
     * associated password. It is assumed that whatever action triggers
     * this method has already prompted the user for confirmation.
     *
     * @throws Exception If the save operation fails or is vetoed by an extension, or if the decrypt fails.
     */
    public void saveUnencrypted() throws Exception {
        boolean wasDirty = isDirty();
        boolean wasEncrypted = isEncrypted();
        File sourceFile = diskContents.getSourceFile();
        JFileChooser fileChooser = MainWindow.getInstance().getFileChooser();
        fileChooser.setCurrentDirectory(diskContents.getSourceFile().getParentFile());
        fileChooser.setSelectedFile(sourceFile); // default to the source file, but user must confirm it.
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
                if (isEncrypted()) {
                    decryptInMemory(); // Will handle prompting for password as needed, may throw if this fails.
                }

                File newFile = fileChooser.getSelectedFile();
                diskContents = ownerPane.getTextManager().saveTextAs(diskContents, getMemoryContents(), newFile);
                markClean();

                // If we were a scratch file, we now have an actual name.
                // If we weren't a scratch file, our name has likely changed.
                setTabName(newFile.getName());

                // Overwrite any CryptMetadata we had and immediately forget our password:
                setCryptMetadata(new DefaultCryptMetadata(false));
            }
            catch (VetoException ignored) {
                // Save was vetoed by an extension!
                // We're in a wonky state now, because we've possibly already decrypted in memory.
                // So, mark ourselves as dirty so the user will know that we need a save.
                if (wasDirty) {
                    markDirty();
                }

                // If we performed an in-memory decryption above, the user should be warned:
                if (wasEncrypted) {
                    markDirty();
                    getMessageUtil().warning("Save vetoed",
                                             "The save was vetoed by an extension. Note: your tab is now showing decrypted content.");
                }
            }
        }
    }

    /**
     * Encrypts the current in-memory text contents of this tab,
     * and updates the in-memory contents to the encrypted version.
     * This does NOT trigger a save, or modify the disk contents in any way.
     * If the text was already encrypted, this method does nothing.
     * This operation is subject to user cancellation,
     * in which case the in-memory contents will remain unchanged.
     *
     * @return true on success, false for user cancel.
     * @throws Exception if encryption fails due to an unexpected error
     */
    public boolean encryptInMemory() throws Exception {
        log.info("Encrypt: encrypting " + getDiskContents().getSourceFile().getAbsolutePath());
        EncryptedText encrypted = handleEncrypt(getMemoryContents(), getCryptMetadata());
        if (encrypted != null) {
            // User successfully encrypted the text, so update our in-memory contents:
            // (note: we don't disable event handling here... extensions may want to know our content has changed)
            setMemoryContents(encrypted.getText());
            setCryptMetadata(encrypted.getCryptMetadata()); // update to match the new scheme and/or password

            // Unlike decryptInMemory(), we DO want to mark ourselves as dirty here,
            // even if we were originally loaded from an encrypted file, because every
            // call to encrypt() generates new ciphertext that will not match our
            // original disk contents. So, we are dirty now, even if the user changed
            // nothing after encryption but before now.
            // "But wait," you ask, "how can the ciphertext be different every time,
            // even if the user selects the same password?"
            // The answer is because the salt and IV are randomly generated each time.
            markDirty();

            return true;
        }

        return false;
    }

    /**
     * Decrypts the current in-memory text contents of this tab,
     * and updates the in-memory contents to the decrypted version.
     * This does NOT trigger a save, or modify the disk contents in any way.
     * If the text was not actually encrypted, this method does nothing.
     * This operation is subject to user cancellation, extension veto,
     * or decryption failure due to wrong password or corrupted text,
     * in which case the in-memory contents will remain unchanged.
     *
     * @return true on success, false for user cancel
     * @throws Exception if decryption fails due to an unexpected error
     */
    public boolean decryptInMemory() throws Exception {
        log.info("Decrypt: decrypting " + getDiskContents().getSourceFile().getAbsolutePath());
        boolean wasDirty = isDirty(); // make a note of our current state
        String decrypted = handleDecrypt(getMemoryContents());
        if (decrypted != null) {
            // User successfully decrypted the text, so update our in-memory contents:
            // (note: we don't disable event handling here... extensions may want to know our content has changed)
            setMemoryContents(decrypted);

            // If we weren't dirty before, then we still aren't.
            // All we did was decrypt. Our in-memory contents don't match our disk contents,
            // but conceptually, we have changed nothing.
            if (!wasDirty) {
                markClean();
            }

            return true;
        }

        return false;
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
     * This will not match the value of getDiskContents() if there are unsaved changes,
     * or if encryption/decryption has taken place since this EditorTab was created or last saved.
     */
    public String getMemoryContents() {
        return textPane.getText();
    }

    /**
     * Replaces the text currently held in this editor tab with the given new text, and marks the tab as dirty.
     * Does NOT commit anything to disk.
     */
    public void setMemoryContents(String newText) {
        textPane.setText(newText);
        markDirty();
    }

    /**
     * Returns the Text instance that was used to create this EditorTab, or which was
     * created the last time this EditorTab was saved. The returned instance is immutable.
     * To retrieve the current (possibly unsaved) text contents of this tab, use getMemoryContents() instead.
     */
    public Text getDiskContents() {
        return diskContents;
    }

    /**
     * Invoked internally to mark this editor tab as clean (no unsaved changes).
     */
    private void markClean() {
        isDirty = false;
        tabHeader.updateLabel(name); // removes the visual dirty indicator
        tabHeader.resetIcon(); // swap icon colors for more visual indication of clean state
    }

    /**
     * Invoked internally to mark this editor tab as dirty (has unsaved changes).
     */
    private void markDirty() {
        if (!eventsEnabled) {
            return; // don't mark dirty if we're in the middle of a programmatic text change
        }
        isDirty = true;
        tabHeader.updateLabel(name); // adds the visual dirty indicator
        tabHeader.resetIcon(); // swap icon colors for more visual indication of dirty state
    }

    /**
     * Invoked when the user has changed application settings and the UI must reload.
     * Our cosmetic options may have changed, so let's update accordingly.
     */
    @Override
    public void reloadUI() {
        final AppConfig appConfig = AppConfig.getInstance();

        // Weirdly, some of these calls will trigger a change event.
        // Example: textPane.setFont() schedules a changedUpdate via SwingUtilities.invokeLater
        // deep inside DefaultStyledDocument.styleChanged. That means the changedUpdate fires
        // AFTER our finally block has already re-enabled events, causing a spurious markDirty().
        // The fix is to re-enable events inside our own invokeLater, so it is queued AFTER
        // the style-change event and will therefore run after eventsEnabled has been checked.
        eventsEnabled = false;
        undoableEditListener.setEnabled(false);
        undoManager.setLimit(appConfig.getUndoLimit());
        try {
            scrollPane.setRowHeaderView(appConfig.isShowLineNumbers() ? gutter : null);
            textPane.setFont(appConfig.getEditorFont());
            gutter.setLineNumberFont(appConfig.getGutterFont());
            gutter.updateColors(); // tell our gutter to update its colors based on the current theme
            textPane.setBackground(appConfig.getEditorBackgroundColor());
            textPane.setForeground(appConfig.getEditorForegroundColor());
            textPane.setCaretColor(appConfig.getEditorForegroundColor());
            if (appConfig.isUseBlockCursor()) {
                textPane.setCaret(new BlockCursor(appConfig.getCursorBlinkRate()));
            }
            else {
                // boring cursor activated!
                textPane.setCaret(new DefaultCaret());
                textPane.getCaret().setBlinkRate(appConfig.getCursorBlinkRate());
            }
            repaint();
        }
        finally {
            SwingUtilities.invokeLater(() -> {
                eventsEnabled = true;
                undoableEditListener.setEnabled(true);
            });
        }

        // Note: our EditorTabHeader is updated via MainWindow's reloadUI handler,
        // which speaks directly to our ownerPane to update all tabs at once.
    }

    /**
     * Invoked internally to generate a CryptMetadata instance for the given Text instance.
     * This may be supplied by extensions, or a default one will be used.
     */
    private CryptMetadata generateCryptMetadata() {
        // Check if any extension wants to provide custom CryptMetadata for this Text instance:
        CryptMetadata cryptMetadata = CryptTextExtensionManager.getInstance()
                                                               .generateCryptMetadata(getMemoryContents());
        if (cryptMetadata != null) {
            return cryptMetadata;
        }

        // If not, use the default implementation:
        return new DefaultCryptMetadata(CryptUtil.isCryptTextWrapped(getMemoryContents()));
    }

    /**
     * Determines the text content to write to disk, re-encrypting if necessary.
     * If the text was loaded from an encrypted file but is currently decrypted in-memory,
     * it will be re-encrypted before being returned.
     * <p>
     *     Note that the EncryptedText that is returned will have a new CryptMetadata instance
     *     that may not match the one currently associated with this EditorTab, if re-encryption was necessary.
     * </p>
     *
     * @return The text to write to disk, or null if the user canceled the operation.
     * @throws Exception if encryption fails.
     */
    private EncryptedText resolveTextForSave() throws Exception {
        // Easy case #1: if this text was never encrypted, and is not encrypted now, just return it as-is:
        // Easy case #2: if the text is encrypted in-memory, regardless where it came from, we can just return as-is:
        if ((!getCryptMetadata().wasEncryptedWhenLoaded() && !isEncrypted()) // case 1
                || isEncrypted()) { // case 2
            return new EncryptedText(getMemoryContents(), getCryptMetadata()); // return what we had
        }

        // If we get here, then the text was loaded from an encrypted file, but is currently decrypted in-memory.
        // This is the tricky case: we need to re-encrypt before saving.
        EncryptedText encryptedText = handleEncrypt(getMemoryContents(), getCryptMetadata());
        if (encryptedText == null) {
            return null; // user canceled, so we can abort here
        }

        // Update whatever CryptMetadata we had with the new one supplied by handleEncrypt above:
        return new EncryptedText(encryptedText.getText(), encryptedText.getCryptMetadata());
    }

    /**
     * Internal utility method to handle the logic of encrypting the given text.
     * Does not affect the contents of this EditorTab.
     *
     * @param toEncrypt     Any text contents.
     * @param cryptMetadata The CryptMetadata instance associated with the text to be encrypted.
     * @return The encrypted version of the given text, or null if encryption is canceled by the user.
     * @throws Exception if the encryption operation fails.
     */
    private EncryptedText handleEncrypt(String toEncrypt, CryptMetadata cryptMetadata) throws Exception {
        if (toEncrypt == null) {
            throw new IllegalArgumentException("toEncrypt cannot be null");
        }

        // Really wonky case: if we are given a string that is already encrypted, just return it:
        if (CryptUtil.isCryptTextWrapped(toEncrypt)) {
            log.warning("handleEncrypt: This text is already encrypted... returning as-is.");
            return new EncryptedText(toEncrypt, cryptMetadata);
        }

        // First, give extensions a chance to handle the encryption:
        CryptTextExtensionManager extManager = CryptTextExtensionManager.getInstance();
        EncryptedText encryptedText = extManager.textWillEncrypt(toEncrypt, cryptMetadata);
        if (encryptedText != null) {
            return encryptedText; // we're done - some extension did the work for us!
        }

        // If no one answered, then check to see if we have a DefaultCryptMetadata already:
        DefaultCryptMetadata defaultCryptMetadata;
        if (cryptMetadata instanceof DefaultCryptMetadata existingCryptMetadata) {
            defaultCryptMetadata = existingCryptMetadata;
        }

        // Otherwise, we can just create a new one.
        else {
            // How did we get here? It could be that this text was originally encrypted by an extension
            // that is no longer available. That's not an error condition.
            // We'll just switch it to use our built-in scheme. This will force a password prompt below.
            defaultCryptMetadata = new DefaultCryptMetadata(true); // unconditional true
        }

        // Prompt for a password if we don't already have one:
        String password = defaultCryptMetadata.getPassword();
        if (password == null || password.isEmpty()) {
            password = getMessageUtil().askText("Enter password:", "");
            if (password == null) {
                return null; // user canceled the prompt, so abort the encryption action
            }
            defaultCryptMetadata.setPassword(password);
        }

        // Now we're good to go:
        String cipherText = CryptUtil.encryptAndWrap(defaultCryptMetadata.getPassword(), toEncrypt);
        return new EncryptedText(cipherText, defaultCryptMetadata);
    }

    /**
     * Internal utility method to handle the logic of decrypting the given text.
     * Does not change the text contents of this EditorTab.
     * May update our cryptMetadata as a side effect, to store the password if it was not already set.
     *
     * @param toDecrypt Any text contents.
     * @return The decrypted version of the given text, or null if user cancels.
     * @throws Exception if any extension vetoes the decryption operation or if the decrypt itself fails.
     */
    private String handleDecrypt(String toDecrypt) throws Exception {
        if (toDecrypt == null) {
            throw new IllegalArgumentException("toDecrypt cannot be null");
        }

        // Very wonky case: if the given text is not actually encrypted, just return it as-is:
        if (!CryptUtil.isCryptTextWrapped(toDecrypt)) {
            log.warning("handleDecrypt: This text is not encrypted... returning as-is.");
            return toDecrypt;
        }

        // First, give extensions a chance to handle the decryption:
        CryptTextExtensionManager extManager = CryptTextExtensionManager.getInstance();
        String decrypted = extManager.textWillDecrypt(new EncryptedText(toDecrypt, getCryptMetadata()));
        if (decrypted != null) {
            return decrypted; // we're done - some extension did the work for us!
        }

        // If no one answered, then make sure our Text instance has a DefaultCryptMetadata:
        if (!(getCryptMetadata() instanceof DefaultCryptMetadata defaultCryptMetadata)) {
            log.warning("Unknown CryptMetadata type: " + getCryptMetadata().getClass().getName());
            throw new Exception("Unknown encryption scheme! " +
                                        "This text was encrypted by an extension that is not available." +
                                        " Unable to decrypt.");
        }

        // If the password is not already set, prompt the user for it:
        if (defaultCryptMetadata.getPassword() == null || defaultCryptMetadata.getPassword().isEmpty()) {
            String password = getMessageUtil().askText("Enter password:", "");
            if (password == null) {
                // User canceled the prompt, so just skip it.
                return null;
            }

            defaultCryptMetadata.setPassword(password); // store this password (deliberate side effect)
        }

        try {
            // Decrypt and return the result:
            return CryptUtil.unwrapAndDecrypt(defaultCryptMetadata.getPassword(), toDecrypt);
        }
        catch (Exception ex) {
            // If anything goes wrong, immediately forget the password we just tried:
            defaultCryptMetadata.setPassword(null);
            throw ex; // caller can handle the exception
        }
    }

    /**
     * Will return a "locked" or an "unlocked" icon for this tab,
     * depending on the state of the diskContents (NOT the in-memory contents!).
     * Even if the tab is currently showing decrypted contents, what matters
     * is whether the contents on disk are encrypted.
     * <p>
     * If tab header icons are disabled in application settings,
     * this method will return null.
     * </p>
     */
    public ImageIcon getIcon() {
        if (!AppConfig.getInstance().isTabLockIconsEnabled()) {
            return null; // easy path
        }

        // Note we deliberately do NOT check getMemoryContents() here... we don't care!
        // What matters is whether the saved contents are encrypted or not.
        boolean isEncrypted = getCryptMetadata().wasEncryptedWhenLoaded();
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

    public void addContentChangeListener(ContentChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        contentChangeListeners.add(listener);
    }

    public void removeContentChangeListener(ContentChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        contentChangeListeners.remove(listener);
    }

    private void fireContentChangedEvent() {
        // If no one is listening, don't bother:
        if (contentChangeListeners.isEmpty()) {
            return;
        }

        // Notify listeners:
        try {
            final String newContents = getMemoryContents();
            // Iterate over a copy of the list to avoid ConcurrentModificationExceptions:
            for (ContentChangeListener listener : new ArrayList<>(contentChangeListeners)) {
                listener.onContentChange(newContents);
            }
        }
        catch (Exception e) {
            // If a listener throws a runtime exception, don't let it interfere with this EditorTab:
            log.warning("Failed to fire content changed event: " + e.getMessage());
        }
    }

    /**
     * A very simple DocumentListener that will mark this editor tab as dirty
     * whenever any change is made.
     */
    private class DocListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            if (!eventsEnabled) {
                return; // don't fire events if we're in the middle of a programmatic change
            }
            markDirty();
            fireContentChangedEvent();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (!eventsEnabled) {
                return; // don't fire events if we're in the middle of a programmatic change
            }
            markDirty();
            fireContentChangedEvent();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (!eventsEnabled) {
                return; // don't fire events if we're in the middle of a programmatic change
            }
            markDirty();
            fireContentChangedEvent();
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(ownerPane, log);
        }
        return messageUtil;
    }
}
