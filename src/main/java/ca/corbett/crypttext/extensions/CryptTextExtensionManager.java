package ca.corbett.crypttext.extensions;

import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.crypt.CryptMetadata;
import ca.corbett.crypttext.extensions.builtin.StatusBarExtension;
import ca.corbett.crypttext.extensions.builtin.TestExtension;
import ca.corbett.crypttext.ui.TabStateManager;
import ca.corbett.extensions.ExtensionManager;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.updates.UpdateManager;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * This class manages loaded extensions and is responsible for invoking them when needed.
 * Extension hooks are defined in the CryptTextExtension class, and this manager will invoke
 * those hooks at the appropriate times.
 * <p>
 *     <b>Extension load order matters!</b> - several of the extension hooks will stop as
 *     soon as one extension supplies a meaningful value. For example, if one extension
 *     vetoes a file load, subsequent extensions will not be sent the fileWillLoad message.
 *     Extension load order can be controlled via the ext-load-order.txt file as described
 *     in our parent class Javadocs. By default, if no ext-load-order.txt file is supplied,
 *     extensions are loaded alphabetically by their jar file names.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class CryptTextExtensionManager extends ExtensionManager<CryptTextExtension> {

    private static final CryptTextExtensionManager instance = new CryptTextExtensionManager();

    private UpdateManager updateManager;

    private CryptTextExtensionManager() {
    }

    public static CryptTextExtensionManager getInstance() {
        return instance;
    }

    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    /**
     * Optional - if your application has an UpdateManager, you can
     * set it here to enable dynamic extension discovery and download from remote update sources.
     */
    public void setUpdateManager(UpdateManager updateManager) {
        this.updateManager = updateManager;
    }

    /**
     * Scans our EXTENSION_DIR looking for jar files containing classes that extend CryptTextExtension.
     * All found classes will be instantiated and made available as extensions, enabled by default.
     */
    public void loadAll() {
        // Load our built-in extensions first:
        addExtension(new StatusBarExtension(), true);

        // TestExtension is a bit special... we won't show it at all unless you've gone
        // out of your way to enable it. This is not intended for general users:
        boolean enableTestExtension = System.getProperty("enableTestExtension", null) != null;
        if (enableTestExtension) {
            addExtension(new TestExtension(), true);
        }

        // Now look for external extensions in jar files in our EXTENSIONS_DIR:
        try {
            loadExtensions(Version.EXTENSIONS_DIR,
                           CryptTextExtension.class,
                    Version.NAME,     // Extensions must target this application name!
                    Version.VERSION); // Extensions must target our major version!
        } catch (LinkageError le) {
            // The parent class is pretty good about trapping errors that occur during extension load.
            // These are presented to the user on an "errors" tab that will be added automatically
            // to the ExtensionManagerDialog. For example, an extension may target an older version
            // of our application, or perhaps a malformed jar file was copied to our extensions dir.
            logger.log(Level.SEVERE, "One or more extensions could not be loaded.", le);
        }
    }

    /**
     * Returns all KeyStrokeProperty instances supplied by enabled extensions.
     * Extensions can supply KeyStrokeProperty instances as part of their usual
     * configuration properties. We have a separate getter for them here as a
     * convenience when registering keyboard shortcuts with our KeyStrokeManager.
     * Properties from currently-disabled extensions will not be included.
     *
     * @return A List of KeyStrokeProperty instances supplied by enabled extensions.
     */
    public List<KeyStrokeProperty> getKeyStrokeProperties() {
        return getAllEnabledExtensionProperties()
                .stream()
                .filter(p -> p instanceof KeyStrokeProperty)
                .map(p -> (KeyStrokeProperty) p)
                .toList();
    }

    /**
     * Interrogates extensions to see if they have any top-level menus that they want
     * to add to the MainWindow's main menu.
     *
     * @return A list of 0 or more JMenus supplied by enabled extensions.
     */
    public List<JMenu> getTopLevelMenus() {
        List<JMenu> list = new ArrayList<>();
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            List<JMenu> toAdd = extension.getTopLevelMenus();
            if (toAdd != null) {
                list.addAll(toAdd);
            }
        }
        return list;
    }

    /**
     * Interrogates extensions to see if they have JMenuItems that they want to add
     * to one of our built-in top-level menus.
     *
     * @param topLevelMenu The name of the top-level menu: File, Edit, Crypt, or Help.
     * @return A list of 0 or more menu items supplied by enabled extensions.
     */
    public List<JMenuItem> getMenuItems(String topLevelMenu) {
        List<JMenuItem> list = new ArrayList<>();
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            List<JMenuItem> toAdd = extension.getMenuItems(topLevelMenu);
            if (toAdd != null) {
                list.addAll(toAdd);
            }
        }
        return list;
    }

    /**
     * Allows any loaded extension to veto a file load.
     * <p>
     * If any extension vetoes a load, subsequent extensions in the load order sequence
     * are not sent this message.
     * </p>
     *
     * @param toLoad The file that is about to be loaded.
     * @return true to allow the load to proceed, or false to veto the load.
     */
    public boolean fileWillLoad(File toLoad) {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            if (!extension.fileWillLoad(toLoad)) {
                return false; // if any extension vetoes, we're done.
            }
        }
        return true;
    }

    /**
     * Allows any loaded extension to veto a file save.
     * <p>
     *     If any extension vetoes a save, subsequent extensions in the load order sequence
     *     are not sent this message.
     * </p>
     *
     * @param toSave The file that is about to be saved.
     * @param newContents The new (pre-encryption) contents that are about to be written to the file.
     * @param destFile For save operations, this will be the same as toSave. For save as operations, this is the new file.
     * @return true to allow the save to proceed, or false to veto the save.
     */
    public boolean fileWillSave(File toSave, String newContents, File destFile) {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            if (!extension.fileWillSave(toSave, newContents, destFile)) {
                return false; // if any extension vetoes, we're done.
            }
        }
        return true;
    }

    /**
     * A simple notification sent to all loaded extensions that a file has been loaded.
     *
     * @param loaded The file that was just loaded.
     * @param loadedContents The decrypted contents of the file that was just loaded.
     */
    public void fileLoaded(File loaded, String loadedContents) {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            extension.fileLoaded(loaded, loadedContents);
        }
    }

    /**
     * A simple notification sent to all loaded extensions that a file has been saved.
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
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            extension.fileSaved(source, dest);
        }
    }

    /**
     * Queries all loaded extensions to see if any of them want to supply crypt metadata
     * about the raw text that is about to be encrypted or decrypted.
     * The first extension that returns a non-null value from this method will be used as the
     * CryptMetadata for the text in question. If no extension returns a non-null value,
     * then the application will use DefaultCryptMetadata.
     *
     * @param rawText The raw text that is about to be encrypted or decrypted.
     * @return a CryptMetadata instance supplied by an extension, or null to allow the application to use the default.
     */
    public CryptMetadata generateCryptMetadata(String rawText) {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            CryptMetadata metadata = extension.generateCryptMetadata(rawText);
            if (metadata != null) {
                return metadata; // if any extension returns a non-null value, we're done.
            }
        }
        return null; // No extension supplied metadata, so allow the application to use the default.
    }

    /**
     * Gives all loaded extensions an opportunity to override the application's built-in encryption
     * scheme by returning a non-null (and typically base64-encoded) String representation of the encrypted data.
     * <p>
     *     The first extension that returns a non-null value from this method will prevent
     *     subsequent extensions in the load order sequence from being sent this message.
     * </p>
     *
     * @param textToEncrypt The text that is about to be encrypted.
     * @param cryptMetadata The CryptMetadata instance associated with the text that is about to be encrypted.
     * @return an encrypted version of the text, or null to allow the application to handle encryption.
     */
    public String textWillEncrypt(String textToEncrypt, CryptMetadata cryptMetadata) {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            String result = extension.textWillEncrypt(textToEncrypt, cryptMetadata);
            if (result != null) {
                return result; // if any extension returns a non-null value, we're done.
            }
        }
        return null; // No extension supplied an encrypted version, so allow the application to handle it.
    }

    /**
     * Gives all loaded extensions an opportunity to override the application's built-in decryption
     * scheme by returning a non-null String representation of the decrypted data.
     * <p>
     *     The first extension that returns a non-null value from this method will prevent
     *     subsequent extensions in the load order sequence from being sent this message.
     * </p>
     *
     * @param textToDecrypt The encrypted text that is about to be decrypted (typically base64 encoded).
     * @param cryptMetadata The CryptMetadata instance associated with the text that is about to be decrypted.
     * @return a decrypted version of the text, or null to allow the application to handle decryption.
     */
    public String textWillDecrypt(String textToDecrypt, CryptMetadata cryptMetadata) {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            String result = extension.textWillDecrypt(textToDecrypt, cryptMetadata);
            if (result != null) {
                return result; // if any extension returns a non-null value, we're done.
            }
        }
        return null; // No extension supplied a decrypted version, so allow the application to handle it.
    }

    /**
     * Queries all loaded extensions to see if they have any extra components (typically panels) that they want
     * to add around the main text area in the MainWindow. The position parameter indicates where the component
     * should be added. Extensions that return a non-null component from this method will have that
     * component added to the MainWindow in the specified position. Multiple extensions can return
     * components for the same position, in which case all of those components will be added in load order sequence.
     *
     * @param position The position around the main text area where the component should be added.
     * @return A list of 0 or more components supplied by enabled extensions for the specified position.
     */
    public List<JComponent> getExtraComponents(ExtraComponentPosition position) {
        List<JComponent> list = new ArrayList<>();
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            JComponent toAdd = extension.getExtraComponent(position);
            if (toAdd != null) {
                list.add(toAdd);
            }
        }
        return list;
    }

    /**
     * Queries all loaded extensions to see if any of them want to supply a TabStateManager to handle
     * the saving and restoring of editor tab state. The first extension that returns a non-null value from this method
     * will be used as the application's TabStateManager, and subsequent extensions in the load order sequence
     * will not be sent this message. If no extension returns a non-null value, then the application will use
     * its built-in DefaultTabStateManager.
     */
    public TabStateManager getTabStateManager() {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            TabStateManager manager = extension.getTabStateManager();
            if (manager != null) {
                return manager; // if any extension returns a non-null value, we're done.
            }
        }
        return null; // No extension supplied a TabStateManager, so allow the application to use the default.
    }
}
