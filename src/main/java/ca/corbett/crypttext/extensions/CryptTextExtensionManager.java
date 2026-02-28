package ca.corbett.crypttext.extensions;

import ca.corbett.extensions.ExtensionManager;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.crypttext.Version;
import ca.corbett.updates.UpdateManager;

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
        // TODO built-ins get loaded here... addExtension(new WhateverExtension(), true);

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
     * @param topLevelMenu The name of the top-level menu: File, Edit, or Help.
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
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            if (!extension.fileWillLoad(toLoad)) {
                return false; // if any extension vetoes, we're done.
            }
        }
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
     * @return true to allow the save to proceed, or false to veto the save.
     */
    public boolean fileWillSave(File toSave, String newContents) {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            if (!extension.fileWillSave(toSave, newContents)) {
                return false; // if any extension vetoes, we're done.
            }
        }
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
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            extension.fileLoaded(loaded, loadedContents);
        }
    }

    /**
     * Invoked after a file is saved. Extensions can override this method to be notified when a file is saved.
     * If you wish to veto a save, override fileWillSave instead and return false.
     *
     * @param saved The file that was just saved.
     */
    public void fileSaved(File saved) {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            extension.fileSaved(saved);
        }
    }

    /**
     * Invoked before text is encrypted. Extensions can return a non-null value to prevent the application
     * from using its built-in encryption scheme to encrypt the data. The return in that case is a String
     * representation of the encrypted data (typically base64-encoded, but this is not enforced).
     * Returning null here will allow the application to handle encryption.
     * <p>
     *     The first extension that returns a non-null value from this method will prevent
     *     subsequent extensions in the load order sequence from being sent this message.
     * </p>
     *
     * @param textToEncrypt The plaintext that is about to be encrypted.
     * @return an encrypted version of the text, or null to allow the application to handle encryption.
     */
    public String textWillEncrypt(String textToEncrypt) {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            String result = extension.textWillEncrypt(textToEncrypt);
            if (result != null) {
                return result; // if any extension returns a non-null value, we're done.
            }
        }
        return null; // No extension supplied an encrypted version, so allow the application to handle it.
    }

    /**
     * Invoked before text is decrypted. Extensions can return a non-null value to prevent the application
     * from using its built-in decryption scheme to decrypt the data. The return in that
     * case is a String representation of the decrypted data.
     * Returning null here will allow the application to handle decryption.
     * <p>
     *     The first extension that returns a non-null value from this method will prevent
     *     subsequent extensions in the load order sequence from being sent this message.
     * </p>
     *
     * @param textToDecrypt The encrypted text that is about to be decrypted (typically base64 encoded).
     * @return a decrypted version of the text, or null to allow the application to handle decryption.
     */
    public String textWillDecrypt(String textToDecrypt) {
        for (CryptTextExtension extension : getEnabledLoadedExtensions()) {
            String result = extension.textWillDecrypt(textToDecrypt);
            if (result != null) {
                return result; // if any extension returns a non-null value, we're done.
            }
        }
        return null; // No extension supplied a decrypted version, so allow the application to handle it.
    }

}
