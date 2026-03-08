package ca.corbett.crypttext.ui;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * A helper class that can build up menus for a window menu bar, and also
 * rebuild them dynamically if any changes occur that would affect them.
 * <p>
 * <b>Extensions can supply menu items!</b> - extensions will be queried for top-level menus,
 * if they supply any, and extensions can also optionally supply extra menu items for the File,
 * Edit, Crypt, and Help menus.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class MenuManager {

    private final JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu cryptMenu;
    private JMenu helpMenu;

    /**
     * Creates and populates a new MenuManager.
     */
    public MenuManager() {
        menuBar = new JMenuBar();
        rebuildAll();
    }

    /**
     * Returns the main JMenuBar for the application.
     */
    public JMenuBar getMainMenuBar() {
        return menuBar;
    }

    /**
     * Can be invoked to rebuild all menus from scratch.
     */
    public void rebuildAll() {
        rebuildMenuBar();
        rebuildFileMenu();
        rebuildEditMenu();
        rebuildCryptMenu();
        rebuildHelpMenu();
    }

    private void rebuildMenuBar() {
        menuBar.removeAll();

        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(editMenu);

        cryptMenu = new JMenu("Crypt");
        cryptMenu.setMnemonic(KeyEvent.VK_C);
        menuBar.add(cryptMenu);

        // Any extension-provided top-level menu can go in between Crypt and Help:
        List<JMenu> extensionMenus = CryptTextExtensionManager.getInstance().getTopLevelMenus();
        if (!extensionMenus.isEmpty()) {
            for (JMenu extensionMenu : extensionMenus) {
                menuBar.add(extensionMenu);
            }
        }

        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpMenu);
    }

    private void rebuildFileMenu() {
        fileMenu.removeAll();

        fileMenu.add(new JMenuItem(AppConfig.getInstance().getNewTabAction()));
        fileMenu.add(new JMenuItem(AppConfig.getInstance().getOpenFileAction()));
        fileMenu.add(new JMenuItem(AppConfig.getInstance().getFileSaveAction()));
        fileMenu.add(new JMenuItem(AppConfig.getInstance().getFileSaveAsAction()));
        fileMenu.add(new JMenuItem(AppConfig.getInstance().getSaveUnencryptedAction()));

        // Add any items to this list from our extensions, if any:
        List<JMenuItem> items = CryptTextExtensionManager.getInstance().getMenuItems("File");
        if (!items.isEmpty()) {
            for (JMenuItem item : items) {
                fileMenu.add(item);
            }
            fileMenu.addSeparator();
        }

        fileMenu.add(new JMenuItem(AppConfig.getInstance().getExitAction()));
    }

    private void rebuildEditMenu() {
        editMenu.removeAll();

        // TODO add your "Edit" menu items here.

        // Add any items to this list from our extensions, if any:
        List<JMenuItem> extensionItems = CryptTextExtensionManager.getInstance().getMenuItems("Edit");
        if (!extensionItems.isEmpty()) {
            for (JMenuItem extensionItem : extensionItems) {
                editMenu.add(extensionItem);
            }
            editMenu.addSeparator();
        }

        editMenu.add(new JMenuItem(AppConfig.getInstance().getPropertiesAction()));
        editMenu.add(new JMenuItem(AppConfig.getInstance().getExtensionManagerAction()));
    }

    private void rebuildCryptMenu() {
        cryptMenu.removeAll();

        cryptMenu.add(new JMenuItem(AppConfig.getInstance().getCryptAction()));
        cryptMenu.add(new JMenuItem(AppConfig.getInstance().getForgetPasswordAction()));

        // Add any items to this list from our extensions, if any:
        List<JMenuItem> items = CryptTextExtensionManager.getInstance().getMenuItems("Crypt");
        if (!items.isEmpty()) {
            fileMenu.addSeparator();
            for (JMenuItem item : items) {
                cryptMenu.add(item);
            }
        }

    }

    private void rebuildHelpMenu() {
        helpMenu.removeAll();

        // TODO add your "Help" menu items here.

        // Add any items to this list from our extensions, if any:
        List<JMenuItem> items = CryptTextExtensionManager.getInstance().getMenuItems("Help");
        if (!items.isEmpty()) {
            for (JMenuItem extensionItem : items) {
                helpMenu.add(extensionItem);
            }
            helpMenu.addSeparator();
        }

        helpMenu.add(new JMenuItem(AppConfig.getInstance().getLogConsoleAction()));
        helpMenu.add(new JMenuItem(AppConfig.getInstance().getAboutAction()));
    }
}
