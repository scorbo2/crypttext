package ca.corbett.crypttext.extensions;

import ca.corbett.extensions.ExtensionManager;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.extensions.builtin.TestExtension;
import ca.corbett.updates.UpdateManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Here we extend the ExtensionManager class with our custom application extension
 * class: CryptTextExtension. This is the starting point for extensions for your application.
 * You can add application-specific hooks here as needed. Just follow the pattern
 * in the examples provided: when the application invokes an extension hook here,
 * we simply iterate through all enabled extensions and invoke the corresponding
 * method on each extension.
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
        addExtension(new TestExtension(), true);

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

}
