package ca.corbett.crypttext.extensions;

import ca.corbett.crypttext.crypt.CryptMetadata;
import ca.corbett.crypttext.crypt.EncryptedText;
import ca.corbett.crypttext.ui.TabStateManager;
import ca.corbett.extensions.AppExtension;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.io.File;
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
     * Invoked before a file is loaded. Extensions can veto the load by returning false.
     * Generally, when vetoing a load, an extension should also display a message to the
     * user explaining why the load was vetoed.
     * <p>
     * If any extension vetoes a load, subsequent extensions in the load order sequence
     * are not sent this message.
     * </p>
     *
     * @param toLoad The file that is about to be loaded.
     * @return true to allow the load to proceed, or false to veto the load.
     */
    public boolean fileWillLoad(File toLoad) {
        return true;
    }

    /**
     * Invoked before a file is saved. Extensions can veto the save by returning false.
     * Generally, when vetoing a save, an extension should also display a message to the
     * user explaining why the save was vetoed.
     * <p>
     *     If any extension vetoes a save, subsequent extensions in the load order sequence
     *     are not sent this message.
     * </p>
     *
     * @param toSave The file that is about to be saved.
     * @param newContents The new (pre-encryption) contents that are about to be written to the file.
     * @param destFile The destination file (for saves, same as toSave, for "save as", the new file).
     * @return true to allow the save to proceed, or false to veto the save.
     */
    public boolean fileWillSave(File toSave, String newContents, File destFile) {
        return true;
    }

    /**
     * Invoked after a file is loaded. Extensions can override this method to be notified when a file is loaded.
     * If you wish to veto a load, override fileWillLoad instead and return false.
     *
     * @param loaded The file that was just loaded.
     * @param loadedContents The decrypted contents of the file that was just loaded.
     */
    public void fileLoaded(File loaded, String loadedContents) {
        // No-op by default. Extensions can override this method to be notified when a file is loaded.
    }

    /**
     * Invoked after a file is saved. Extensions can override this method to be notified when a file is saved.
     * If you wish to veto a save, override fileWillSave instead and return false.
     * <p>
     *     For "save" operations, the two file parameters will be the same.
     *     For "save as" operations, the first parameter is the file from which the content in question
     *     was originally loaded, and the second parameter is the destination file.
     * </p>
     *
     * @param source The file from which the content in question was loaded.
     * @param dest The file to which the content in question was saved.
     */
    public void fileSaved(File source, File dest) {
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
