package ca.corbett.crypttext;

import ca.corbett.crypttext.extensions.CryptTextExtension;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;
import ca.corbett.crypttext.ui.actions.AboutAction;
import ca.corbett.crypttext.ui.actions.CryptAction;
import ca.corbett.crypttext.ui.actions.ExitAction;
import ca.corbett.crypttext.ui.actions.ExtensionManagerAction;
import ca.corbett.crypttext.ui.actions.LogConsoleAction;
import ca.corbett.crypttext.ui.actions.NewTabAction;
import ca.corbett.crypttext.ui.actions.OpenFileAction;
import ca.corbett.crypttext.ui.actions.PropertiesAction;
import ca.corbett.crypttext.ui.actions.SaveAction;
import ca.corbett.crypttext.ui.actions.SaveAsAction;
import ca.corbett.extensions.AppProperties;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.DirectoryProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.extras.properties.LookAndFeelProperty;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.Action;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralizes and manages application configuration properties.
 * Extensions can supply additional properties as needed.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class AppConfig extends AppProperties<CryptTextExtension> {

    /**
     * For thread-safe lazy-loaded singleton pattern.
     */
    private static class SingletonHolder {
        private static final AppConfig INSTANCE = new AppConfig();
    }

    /**
     * Property name for enabling/disabling single-instance mode.
     * We expose this one because it's referenced elsewhere in the code.
     */
    public static final String SINGLE_INSTANCE_PROP = "UI.General.singleInstance";

    /**
     * Extensions can use this prefix when defining their own keystroke properties,
     * so that they show up on the same properties dialog tab as the other ones.
     * This is optional! Extensions can opt to keep all of their properties
     * on their own separate tab if they prefer.
     * <p>
     * Suggested format: KEYSTROKE_PREFIX + ExtensionUserFriendlyName + "." + ActionName
     * </p>
     */
    public static final String KEYSTROKE_PREFIX = "Keystrokes.";

    private static final String KEY_NEW_TAB = KEYSTROKE_PREFIX + "General.newTab";
    private static final String KEY_OPEN_FILE = KEYSTROKE_PREFIX + "General.openFile";
    private static final String KEY_SAVE_FILE = KEYSTROKE_PREFIX + "General.saveFile";
    private static final String KEY_SAVE_FILE_AS = KEYSTROKE_PREFIX + "General.saveFileAs";
    private static final String KEY_CRYPT = KEYSTROKE_PREFIX + "General.crypt";
    private static final String KEY_PROPERTIES = KEYSTROKE_PREFIX + "General.properties";
    private static final String KEY_EXTENSIONS = KEYSTROKE_PREFIX + "General.extensionManager";
    private static final String KEY_LOG_CONSOLE = KEYSTROKE_PREFIX + "General.logConsole";
    private static final String KEY_ABOUT = KEYSTROKE_PREFIX + "General.about";
    private static final String KEY_EXIT = KEYSTROKE_PREFIX + "General.exit";

    private static final String PROPS_FILE_NAME = "CryptText.props";
    public static final File PROPS_FILE = new File(Version.SETTINGS_DIR, PROPS_FILE_NAME);

    private LookAndFeelProperty lookAndFeelProp;
    private BooleanProperty enableSingleInstance;
    private BooleanProperty enableTabLockIconsProp;
    private IntegerProperty tabIconSizeProp;
    private BooleanProperty closeLastTabExitsProp;
    private DirectoryProperty lastBrowseDirProp;
    private BooleanProperty restoreTabsOnStartupProp;

    // These will be used in the menu bar and with KeyStrokeManager:
    // (they could also be added to buttons or popup menus as needed)
    // (the advantage of centralizing them here is that they can be
    //  disabled/enabled/renamed or have their shortcut reassigned,
    //  and the changes will take effect wherever the action is used)
    private Action newTabAction;
    private Action openFileAction;
    private Action saveFileAction;
    private Action saveFileAsAction;
    private Action cryptAction;
    private Action propertiesAction;
    private Action extensionManagerAction;
    private Action logConsoleAction;
    private Action aboutAction;
    private Action exitAction;

    private AppConfig() {
        super(Version.FULL_NAME, PROPS_FILE, CryptTextExtensionManager.getInstance());
    }

    public static AppConfig getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Get the class name of the currently selected Look and Feel.
     */
    public String getLookAndFeelClassName() {
        return lookAndFeelProp.getSelectedLafClass();
    }

    public boolean isSingleInstanceEnabled() {
        return enableSingleInstance.getValue();
    }

    /**
     * We'll add a convenience wrapper around the static peek() method so that
     * our callers don't have to specify our props file each time.
     */
    public static String peek(String propName) {
        return AppProperties.peek(PROPS_FILE, propName);
    }

    public Action getNewTabAction() {
        return newTabAction;
    }

    public Action getOpenFileAction() {
        return openFileAction;
    }

    public Action getFileSaveAction() {
        return saveFileAction;
    }

    public Action getFileSaveAsAction() {
        return saveFileAsAction;
    }

    public Action getCryptAction() {
        return cryptAction;
    }

    public Action getPropertiesAction() {
        return propertiesAction;
    }

    public Action getExtensionManagerAction() {
        return extensionManagerAction;
    }

    public Action getLogConsoleAction() {
        return logConsoleAction;
    }

    public Action getAboutAction() {
        return aboutAction;
    }

    public Action getExitAction() {
        return exitAction;
    }

    /**
     * Returns all KeyStrokeProperty instances defined in the application config,
     * or offered by any currently-enabled extension.
     */
    public List<KeyStrokeProperty> getKeyStrokeProperties() {
        List<KeyStrokeProperty> keyProps = new ArrayList<>();

        // Add the ones we control:
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_NEW_TAB));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_OPEN_FILE));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_SAVE_FILE));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_SAVE_FILE_AS));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_CRYPT));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_PROPERTIES));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_EXTENSIONS));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_LOG_CONSOLE));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_ABOUT));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_EXIT));

        // And now ask our ExtensionManager:
        keyProps.addAll(CryptTextExtensionManager.getInstance().getKeyStrokeProperties());

        return keyProps;
    }

    /**
     * Reports whether the user has enabled lock icons on tabs in the UI.
     */
    public boolean isTabLockIconsEnabled() {
        return enableTabLockIconsProp.getValue();
    }

    /**
     * If tab lock icons are enabled, this returns the size (in pixels) that the user has selected for them.
     */
    public int getTabIconSize() {
        return tabIconSizeProp.getValue();
    }

    /**
     * Reports whether the user wishes to exit the application when the last remaining
     * editor tab is closed. If false, closing the last tab leaves you with a blank window.
     */
    public boolean isExitOnCloseLastTabEnabled() {
        return closeLastTabExitsProp.getValue();
    }

    /**
     * Gets the last directory that was browsed to in a file chooser.
     * This can be used to initialize the next file chooser, for
     * a more consistent user experience.
     */
    public File getLastBrowseDirectory() {
        return lastBrowseDirProp.getDirectory();
    }

    /**
     * Updates the last-browsed directory, and triggers an immediate
     * save to persist this change.
     */
    public void setLastBrowseDirectory(File dir) {
        lastBrowseDirProp.setDirectory(dir);
        save(); // immediate save to persist this change
    }

    /**
     * Reports whether the previously-loaded tabs from the last application run
     * should be automatically restored on startup.
     */
    public boolean isRestoreTabsOnStartup() {
        return restoreTabsOnStartupProp.getValue();
    }

    /**
     * This is where you can define the configuration properties for your application.
     * These properties will be displayed in the PropertiesDialog, and persisted
     * to the properties file automatically.
     */
    @Override
    protected List<AbstractProperty> createInternalProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        // We'll create a Look and Feel property to allow users to select
        // their preferred Look and Feel for the application:
        lookAndFeelProp = new LookAndFeelProperty("UI.General.lookAndFeel",
                "Look and Feel:",
                FlatLightLaf.class.getName());
        props.add(lookAndFeelProp);

        // We'll create a property to allow enabling/disabling single-instance mode:
        enableSingleInstance = new BooleanProperty(SINGLE_INSTANCE_PROP,
                                                   "Allow only a single instance of the application",
                                                   true);
        props.add(enableSingleInstance);

        // Let's create all our actions:
        newTabAction = new NewTabAction();
        openFileAction = new OpenFileAction();
        saveFileAction = new SaveAction();
        saveFileAsAction = new SaveAsAction();
        cryptAction = new CryptAction();
        propertiesAction = new PropertiesAction();
        extensionManagerAction = new ExtensionManagerAction();
        logConsoleAction = new LogConsoleAction();
        aboutAction = new AboutAction();
        exitAction = new ExitAction();

        // And we can set up our keyboard shortcuts while we're at it:
        props.addAll(createKeyboardProperties());

        enableTabLockIconsProp = new BooleanProperty("UI.Editor tabs.showLockIcons",
                                                     "Show lock icons on editor tabs",
                                                     true);
        props.add(enableTabLockIconsProp);

        tabIconSizeProp = new IntegerProperty("UI.Editor tabs.tabIconSize",
                                              "Tab Icon Size (px)",
                                              16, 8, 64, 2);
        props.add(tabIconSizeProp);

        closeLastTabExitsProp = new BooleanProperty("UI.Editor tabs.closeLastTabExits",
                                                    "Exit application when the last editor tab is closed",
                                                    true); // completely arbitrary default value here
        props.add(closeLastTabExitsProp);

        restoreTabsOnStartupProp = new BooleanProperty("UI.Editor tabs.restoreTabsOnStartup",
                                                       "Restore previously-open tabs on startup",
                                                       true);
        props.add(restoreTabsOnStartupProp);

        // Hidden props (persisted but never directly shown to the user):
        lastBrowseDirProp = new DirectoryProperty("hidden.props.lastBrowseDirectory",
                                                  "Last Browse Directory:",
                                                  true,
                                                  Version.SETTINGS_DIR);
        lastBrowseDirProp.setExposed(false);
        props.add(lastBrowseDirProp);

        return props;
    }

    private List<AbstractProperty> createKeyboardProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        props.add(new KeyStrokeProperty(KEY_NEW_TAB, "New Tab:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+N"),
                                        newTabAction)
                          .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_OPEN_FILE, "Open File:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+O"),
                                        openFileAction)
                          .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_SAVE_FILE, "Save File:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+S"),
                                        saveFileAction)
                          .setAllowBlank(false));
        props.add(new KeyStrokeProperty(KEY_SAVE_FILE_AS, "Save File As:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+Shift+S"),
                                        saveFileAsAction)
                          .setAllowBlank(false));
        props.add(new KeyStrokeProperty(KEY_CRYPT, "Encrypt/Decrypt:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+D"), // D for "decrypt/crypt" :)
                                        cryptAction)
                          .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_PROPERTIES, "Properties Dialog:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+P"),
                                        propertiesAction)
                          .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_EXTENSIONS, "Extension Manager:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+E"),
                                        extensionManagerAction)
                          .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_LOG_CONSOLE,  "Log Console:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+L"),
                                        logConsoleAction)
                          .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_ABOUT, "About Dialog:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+A"),
                                        aboutAction)
                          .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_EXIT, "Exit Application:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+Q"),
                                        exitAction)
                          .setAllowBlank(true));

        return props;
    }
}
