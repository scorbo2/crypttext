package ca.corbett.crypttext;

import ca.corbett.extensions.AppProperties;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.extras.properties.LookAndFeelProperty;
import ca.corbett.crypttext.ui.actions.AboutAction;
import ca.corbett.crypttext.ui.actions.ExitAction;
import ca.corbett.crypttext.ui.actions.ExtensionManagerAction;
import ca.corbett.crypttext.ui.actions.LogConsoleAction;
import ca.corbett.crypttext.ui.actions.PropertiesAction;
import com.formdev.flatlaf.FlatLightLaf;

import ca.corbett.crypttext.extensions.CryptTextExtension;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;

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

    // These will be used in the menu bar and with KeyStrokeManager:
    // (they could also be added to buttons or popup menus as needed)
    // (the advantage of centralizing them here is that they can be
    //  disabled/enabled/renamed or have their shortcut reassigned,
    //  and the changes will take effect wherever the action is used)
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

        return props;
    }

    private List<AbstractProperty> createKeyboardProperties() {
        List<AbstractProperty> props = new ArrayList<>();

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
