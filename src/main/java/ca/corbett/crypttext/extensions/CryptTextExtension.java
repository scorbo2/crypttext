package ca.corbett.crypttext.extensions;

import ca.corbett.crypttext.crypt.CryptMetadata;
import ca.corbett.crypttext.crypt.EncryptedText;
import ca.corbett.crypttext.text.Text;
import ca.corbett.crypttext.ui.TabStateManager;
import ca.corbett.extensions.AppExtension;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This is the starting point for extensions for the CryptText application.
 * Extensions can supply instances of this class, which will be loaded by the CryptTextExtensionManager and
 * invoked at various points in the application to allow extensions to add functionality. Review the
 * method Javadocs in this class to learn what functionality you can provide via extensions!
 * <p>
 *     <b>Retrieving/modifying editor tab contents</b> - your extension can query for the names
 *     of open editor tabs using MainWindow's getTabNames() method. Retrieve the current
 *     contents of an editor tab with getTabContents(tabName), and modify the contents
 *     of an editor tab with setTabContents(tabName, newContents).
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public abstract class CryptTextExtension extends AppExtension {

    /**
     * Invoked when the application wants to know if the extension has its own top-level
     * menu to add to the MainWindow's main menu. These will be inserted in between
     * the "Crypt" and "Help" menus.
     *
     * @return an optional list of JMenu objects for the main menu, or null for none.
     */
    public List<JMenu> getTopLevelMenus() {
        return null;
    }

    /**
     * Invoked when the application is building the MainWindow's main menu and wants
     * to know if the extension has anything to add to one of the built-in top-level
     * menus.
     *
     * @param topLevelMenu The name of the top-level menu being built: File, Edit, Crypt, or Help.
     * @return an optional list of menu items to insert into the given menu, or null for nothing.
     */
    public List<JMenuItem> getMenuItems(String topLevelMenu) {
        return null;
    }

    /**
     * Gives extensions an opportunity to handle file loading themselves, overriding the application's built-in
     * mechanism for loading. The first extension that returns a non-null Text instance from this method
     * will result in the load operation being considered complete. Subsequent extensions in the load order
     * sequence will not be sent a handleFileSave message for this load operation.
     * All extensions, including the one that just handled the load, will then receive a fileLoaded
     * message for this load operation.
     * <p>
     * Note that the sourceFile in the resulting Text instance doesn't necessarily have to match the
     * supplied toLoad file. Extensions are free to move and/or rename the file as part of their load
     * handling. The application will respond accordingly (that is, hitting "save" after the load will
     * respect the sourceFile in the Text instance, not the original toLoad file).
     * </p>
     *
     * @param toLoad the file that is about to be loaded.
     * @return non-null to indicate that the load has been handled, or null to pass on the operation.
     * @throws IOException can be thrown if the load fails. Application will handle the exception.
     */
    public Text handleFileLoad(File toLoad) throws IOException {
        return null;
    }

    /**
     * Gives extensions an opportunity to handle file saving themselves, overriding the application's built-in
     * mechanism for saving. The first extension that returns a non-null File instance from this method
     * will result in the save operation being considered complete. Subsequent extensions in the load order
     * sequence will not be sent a handleFileSave message for this save operation.
     * All extensions, including the one that just handled the save, will then receive a
     * fileSaved message for this save operation.
     * <p>
     * Note that the returned File does not need to match the sourceFile of the supplied Text instance!
     * Extensions are free to move and/or rename the file as part of their save handling. The application will
     * respond accordingly (that is, the MainWindow will now consider the returned File to be
     * the sourceFile for the Text instance, and hitting "save" again will save to that file,
     * not the original toSave file).
     * </p>
     * <p>
     * For unencrypted files, the Text's memoryContents will exactly match the resolvedText parameter.
     * For encrypted files that have been decrypted in-memory, the Text's memoryContents will be the
     * decrypted text, while the resolvedText parameter will be the encrypted version of that text.
     * Extensions are encouraged to save the resolvedText, and ignore the Text's memoryContents,
     * to preserve the encryption that has been done, but this is not enforced.
     * </p>
     *
     * @param toSave the Text instance containing the in-memory text.
     * @param resolvedText the actual text content that is about to be saved. May be encrypted.
     * @param destinationFile may not match toSave's sourceFile if this is a "save as" operation.
     * @return a File to indicate that the save has been handled, or null to pass on the operation.
     * @throws IOException can be thrown if the save fails. Application will handle the exception.
     */
    public File handleFileSave(Text toSave, String resolvedText, File destinationFile) throws IOException {
        return null;
    }

    /**
     * Invoked after a file is loaded. Extensions can override this method to be notified when a file is loaded.
     * Note that if the file is encrypted, the text supplied here may be unreadable. Use the
     * textDecrypted event instead if you want to be notified when something is decrypted.
     *
     * @param loadedContent the Text instance that was just loaded. May be encrypted.
     */
    public void fileLoaded(Text loadedContent) {
        // No-op by default. Extensions can override this method to be notified when a file is loaded.
    }

    /**
     * Invoked after a file is saved. Extensions can override this method to be notified when a file is saved.
     * <p>
     *     For "save" operations, the destFile parameter should usually match the sourceFile of the Text instance.
     *     For "save as" operations, the Text instance may reference the file from which the content in question
     *     was originally loaded, and the second parameter is the new destination file.
     * </p>
     *
     * @param text The Text instance which was just saved.
     * @param destFile The file to which the content in question was saved.
     */
    public void fileSaved(Text text, File destFile) {
        // No-op by default. Extensions can override this method to be notified when a file is saved.
    }

    /**
     * Extensions can subclass the CryptMetadata class to provide custom metadata
     * related to encryption of a Text instance. If an extension wishes to override
     * the application's built-in encryption/decryption scheme, it should also override
     * textWillEncrypt and textWillDecrypt to provide the actual encryption/decryption functionality.
     * Extensions can scan the supplied rawText to determine whether it contains any encrypted content
     * that the extension can handle, and if so, return a CryptMetadata instance with the relevant
     * metadata for that content.
     *
     * @param rawText The raw text (encrypted or not) for which the application is requesting CryptMetadata.
     * @return a CryptMetadata instance with relevant metadata, or null if the extension has none for this rawText.
     */
    public CryptMetadata generateCryptMetadata(String rawText) {
        return null;
    }

    /**
     * Invoked before text is encrypted. Extensions can return a non-null value to prevent the application
     * from using its built-in encryption scheme to encrypt the data. The return in that case is a String
     * representation of the encrypted data (typically base64-encoded, but this is not enforced).
     * Returning null here will allow the application to handle encryption with the built-in scheme.
     * <p>
     *     The first extension that returns a non-null value from this method will prevent
     *     subsequent extensions in the load order sequence from being sent this message.
     * </p>
     * <p>
     *     Extensions are free to ignore the supplied CryptMetadata instance, and can
     *     return one of their own in the resulting EncryptedText instance.
     *     This allows extensions to embed metadata regarding key management, encryption parameters,
     *     or whatever else.
     * </p>
     *
     * @param textToEncrypt The text that is about to be encrypted.
     * @param cryptMetadata The CryptMetadata instance associated with the text that is about to be encrypted.
     * @return an encrypted version of the text, or null to allow the application to handle encryption.
     */
    public EncryptedText textWillEncrypt(String textToEncrypt, CryptMetadata cryptMetadata) {
        return null;
    }

    /**
     * Invoked before text is decrypted. Extensions can return a non-null value to prevent the application
     * from using its built-in decryption scheme to decrypt the data. The return in that
     * case is a String representation of the decrypted data.
     * Returning null here will allow the application to handle decryption using the built-in scheme.
     * <p>
     *     The first extension that returns a non-null value from this method will prevent
     *     subsequent extensions in the load order sequence from being sent this message.
     * </p>
     *
     * @param encryptedText The text and metadata of the text that is about to be decrypted.
     * @return a decrypted version of the text, or null to allow the application to handle decryption.
     */
    public String textWillDecrypt(EncryptedText encryptedText) {
        return null;
    }

    /**
     * A notification message that the given EncryptedText instance was just decrypted.
     * This notification is sent regardless of whether the decryption was handled by the application
     * or by an extension.
     *
     * @param cryptText     the EncryptedText instance that was decrypted.
     * @param decryptedText the resulting decrypted text.
     */
    public void textWasDecrypted(EncryptedText cryptText, String decryptedText) {
        // No-op by default. Extensions can override this method to be notified when text is decrypted.
    }

    /**
     * A notification message that the given Text instance was just encrypted.
     * This notification is sent regardless of whether the encryption was handled by the application
     * or by an extension.
     *
     * @param plaintext the plaintext that was encrypted.
     * @param cryptText the resulting EncryptedText instance that was created from the encryption.
     */
    public void textWasEncrypted(String plaintext, EncryptedText cryptText) {
        // No-op by default. Extensions can override this method to be notified when text is encrypted.
    }

    /**
     * Extensions can optionally supply "extra" components (typically panels) to be placed around the
     * main text area in the MainWindow. This method will be invoked for each position defined in
     * ExtraComponentPosition, and any non-null component returned will be added to the
     * MainWindow in the specified position. If more than one extension supplies a component
     * for the same position, the components will be placed in a JTabbedPane in the order of the
     * extensions' load sequence.
     * <p>
     *     <b>Hint:</b> use setName() on your returned component to give it a useful tab name
     *     in the JTabbedPane if there are multiple components in the same position.
     *     The tab header will not be shown if only one extension returns a component for a given position.
     * </p>
     *
     * @param position The position for which the application is requesting an extra component.
     * @return a JComponent to be added to the MainWindow in the specified position, or null for none.
     */
    public JComponent getExtraComponent(ExtraComponentPosition position) {
        return null;
    }

    /**
     * An extension can optionally return an implementation of TabStateManager to handle the
     * saving and restoring of editor tab state in a custom way. Null is a perfectly acceptable
     * return here, in which case the built-in default implementation is used.
     * If more than one extension returns a non-null TabStateManager, the first one in the load order sequence is used.
     */
    public TabStateManager getTabStateManager() {
        return null;
    }
}
