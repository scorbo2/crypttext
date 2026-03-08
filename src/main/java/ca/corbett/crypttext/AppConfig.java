package ca.corbett.crypttext;

import ca.corbett.crypttext.extensions.CryptTextExtension;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;
import ca.corbett.crypttext.ui.ColorTheme;
import ca.corbett.crypttext.ui.actions.AboutAction;
import ca.corbett.crypttext.ui.actions.CryptAction;
import ca.corbett.crypttext.ui.actions.ExitAction;
import ca.corbett.crypttext.ui.actions.ExtensionManagerAction;
import ca.corbett.crypttext.ui.actions.ForgetPasswordAction;
import ca.corbett.crypttext.ui.actions.LogConsoleAction;
import ca.corbett.crypttext.ui.actions.NewTabAction;
import ca.corbett.crypttext.ui.actions.OpenFileAction;
import ca.corbett.crypttext.ui.actions.PropertiesAction;
import ca.corbett.crypttext.ui.actions.SaveAction;
import ca.corbett.crypttext.ui.actions.SaveAsAction;
import ca.corbett.crypttext.ui.actions.SaveUnencryptedAction;
import ca.corbett.extensions.AppProperties;
import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.gradient.ColorSelectionType;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.DirectoryProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.FontProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.extras.properties.LookAndFeelProperty;
import ca.corbett.extras.properties.PropertyFormFieldChangeListener;
import ca.corbett.extras.properties.PropertyFormFieldValueChangedEvent;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.CheckBoxField;
import ca.corbett.forms.fields.ColorField;
import ca.corbett.forms.fields.ComboField;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.Action;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
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

    public static final Font DEFAULT_EDITOR_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    public static final Font DEFAULT_GUTTER_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private static final String KEY_NEW_TAB = KEYSTROKE_PREFIX + "General.newTab";
    private static final String KEY_OPEN_FILE = KEYSTROKE_PREFIX + "General.openFile";
    private static final String KEY_SAVE_FILE = KEYSTROKE_PREFIX + "General.saveFile";
    private static final String KEY_SAVE_FILE_AS = KEYSTROKE_PREFIX + "General.saveFileAs";
    private static final String KEY_SAVE_UNENCRYPTED = KEYSTROKE_PREFIX + "General.saveUnencrypted";
    private static final String KEY_CRYPT = KEYSTROKE_PREFIX + "General.crypt";
    private static final String KEY_FORGET_PASSWORD = KEYSTROKE_PREFIX + "General.forgetPassword";
    private static final String KEY_PROPERTIES = KEYSTROKE_PREFIX + "General.properties";
    private static final String KEY_EXTENSIONS = KEYSTROKE_PREFIX + "General.extensionManager";
    private static final String KEY_LOG_CONSOLE = KEYSTROKE_PREFIX + "General.logConsole";
    private static final String KEY_ABOUT = KEYSTROKE_PREFIX + "General.about";
    private static final String KEY_EXIT = KEYSTROKE_PREFIX + "General.exit";

    private static final String PROPS_FILE_NAME = "CryptText.props";
    public static final File PROPS_FILE = new File(Version.SETTINGS_DIR, PROPS_FILE_NAME);

    private LookAndFeelProperty lookAndFeelProp;
    private BooleanProperty enableSingleInstance;
    private BooleanProperty showFullPathInTitleProp;
    private BooleanProperty enableTabLockIconsProp;
    private IntegerProperty tabIconSizeProp;
    private BooleanProperty closeLastTabExitsProp;
    private DirectoryProperty lastBrowseDirProp;
    private BooleanProperty restoreTabsOnStartupProp;
    private BooleanProperty showLineNumbersProp;
    private FontProperty editorFontProp;
    private FontProperty gutterFontProp;
    private BooleanProperty overrideLafProp;
    private EnumProperty<ColorTheme> editorThemeProp;
    private ColorProperty editorBackgroundColorProp;
    private ColorProperty editorForegroundColorProp;
    private ColorProperty gutterBackgroundColorProp;
    private ColorProperty gutterForegroundColorProp;

    // These will be used in the menu bar and with KeyStrokeManager:
    // (they could also be added to buttons or popup menus as needed)
    // (the advantage of centralizing them here is that they can be
    //  disabled/enabled/renamed or have their shortcut reassigned,
    //  and the changes will take effect wherever the action is used)
    private Action newTabAction;
    private Action openFileAction;
    private Action saveFileAction;
    private Action saveFileAsAction;
    private Action saveUnencryptedAction;
    private Action cryptAction;
    private Action forgetPasswordAction;
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
     * Overridden so we can set the initial enabled/disabled state our properties.
     */
    @Override
    public boolean showPropertiesDialog(Frame owner) {
        boolean isCustom = overrideLafProp.getValue();
        editorThemeProp.setInitiallyEditable(isCustom);
        editorBackgroundColorProp.setInitiallyEditable(isCustom);
        editorForegroundColorProp.setInitiallyEditable(isCustom);
        gutterBackgroundColorProp.setInitiallyEditable(isCustom);
        gutterForegroundColorProp.setInitiallyEditable(isCustom);
        return super.showPropertiesDialog(owner);
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

    public Action getSaveUnencryptedAction() {
        return saveUnencryptedAction;
    }

    public Action getCryptAction() {
        return cryptAction;
    }

    public Action getForgetPasswordAction() {
        return forgetPasswordAction;
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
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_SAVE_UNENCRYPTED));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_CRYPT));
        keyProps.add((KeyStrokeProperty)getPropertiesManager().getProperty(KEY_FORGET_PASSWORD));
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
     * Reports whether the "show full path in title bar" option is enabled.
     * If false, you just get application name + version.
     */
    public boolean isShowFullPathInTitleEnabled() {
        return showFullPathInTitleProp.getValue();
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
     * Reports whether "show line numbers in editor" is enabled.
     */
    public boolean isShowLineNumbers() {
        return showLineNumbersProp.getValue();
    }

    /**
     * Returns the configured editor font.
     */
    public Font getEditorFont() {
        return editorFontProp.getFont();
    }

    /**
     * Returns the configured "gutter" font (for showing line numbers).
     */
    public Font getGutterFont() {
        return gutterFontProp.getFont();
    }

    /**
     * Returns the configured editor background color.
     * This will come from the current Look and Feel if we are not set to override it.
     * Otherwise, this is the user-selected background color.
     */
    public Color getEditorBackgroundColor() {
        if (overrideLafProp.getValue()) {
            return editorBackgroundColorProp.getSolidColor();
        }
        else {
            return LookAndFeelManager.getLafColor("Panel.background", Color.LIGHT_GRAY);
        }
    }

    /**
     * Returns the configured editor foreground color.
     * This will come from the current Look and Feel if we are not set to override it.
     * Otherwise, this is the user-selected foreground color.
     */
    public Color getEditorForegroundColor() {
        if (overrideLafProp.getValue()) {
            return editorForegroundColorProp.getSolidColor();
        }
        else {
            return LookAndFeelManager.getLafColor("Panel.foreground", Color.BLACK);
        }
    }

    /**
     * Returns the configured gutter background color (for line numbers).
     * This will come from the current Look and Feel if we are not set to override it
     * - in this case, we will attempt to be a little smarter and return a color that is
     * slightly offset from the regular background color, to provide some visual contrast
     * while still fitting in with the overall theme.
     */
    public Color getGutterBackgroundColor() {
        if (overrideLafProp.getValue()) {
            return gutterBackgroundColorProp.getSolidColor();
        }
        else {
            // We want to offset the gutter background slightly, but we need
            // to be careful, because the current LaF might be light or dark:
            Color bg = LookAndFeelManager.getLafColor("Panel.background", Color.LIGHT_GRAY);
            if (LookAndFeelManager.isDark()) {
                bg = bg.brighter();
            }
            else {
                bg = bg.darker();
            }
            return bg;
        }
    }

    /**
     * Returns the configured gutter foreground color (for line numbers).
     * This will come from the current Look and Feel if we are not set to override it
     * - in this case, we will attempt to be a little smarter and return a color that is
     * slightly offset from the regular foreground color, to provide some visual contrast
     * while still fitting in with the overall theme.
     */
    public Color getGutterForegroundColor() {
        if (overrideLafProp.getValue()) {
            return gutterForegroundColorProp.getSolidColor();
        }
        else {
            Color fg = LookAndFeelManager.getLafColor("Panel.foreground", Color.BLACK);
            if (LookAndFeelManager.isDark()) {
                fg = fg.brighter();
            }
            else {
                fg = fg.darker();
            }
            return fg;
        }
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

        showFullPathInTitleProp = new BooleanProperty("UI.General.showFullPathInTitle",
                                                      "Show full file path in title bar",
                                                      true);
        props.add(showFullPathInTitleProp);

        // Let's create all our actions:
        newTabAction = new NewTabAction();
        openFileAction = new OpenFileAction();
        saveFileAction = new SaveAction();
        saveFileAsAction = new SaveAsAction();
        saveUnencryptedAction = new SaveUnencryptedAction();
        cryptAction = new CryptAction();
        forgetPasswordAction = new ForgetPasswordAction();
        propertiesAction = new PropertiesAction();
        extensionManagerAction = new ExtensionManagerAction();
        logConsoleAction = new LogConsoleAction();
        aboutAction = new AboutAction();
        exitAction = new ExitAction();

        // And we can set up our keyboard shortcuts while we're at it:
        props.addAll(createKeyboardProperties());

        // And now our various property categories:
        props.addAll(buildEditorProperties());
        props.addAll(buildEditorTabProperties());
        props.addAll(buildHiddenProperties());

        return props;
    }

    /**
     * Builds and returns properties related to the editor itself - appearance and behavior.
     */
    private List<AbstractProperty> buildEditorProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        editorFontProp = new FontProperty("UI.Editor.editorFont",
                                          "Editor Font:",
                                          DEFAULT_EDITOR_FONT);
        props.add(editorFontProp);

        gutterFontProp = new FontProperty("UI.Editor.gutterFont",
                                          "Gutter Font:",
                                          DEFAULT_GUTTER_FONT);
        props.add(gutterFontProp);

        showLineNumbersProp = new BooleanProperty("UI.Editor.showLineNumbers",
                                                  "Show line numbers in editor",
                                                  true);
        props.add(showLineNumbersProp);

        overrideLafProp = new BooleanProperty("UI.Editor.overrideLaF",
                                              "Override Look and Feel with custom editor colors",
                                              false);
        props.add(overrideLafProp);

        editorThemeProp = new EnumProperty<>("UI.Editor.colorTheme",
                                             "Set from theme:",
                                             ColorTheme.MATRIX);
        editorThemeProp.addFormFieldChangeListener(this::setColorTheme);
        props.add(editorThemeProp);

        editorBackgroundColorProp = new ColorProperty("UI.Editor.editorBackground",
                                                      "Editor bg:",
                                                      ColorSelectionType.SOLID)
                .setSolidColor(ColorTheme.MATRIX.getEditorBackground());
        editorBackgroundColorProp.addLeftPadding(20); // Indent a little bit
        props.add(editorBackgroundColorProp);

        editorForegroundColorProp = new ColorProperty("UI.Editor.editorForeground",
                                                      "Editor fg:",
                                                      ColorSelectionType.SOLID)
                .setSolidColor(ColorTheme.MATRIX.getEditorForeground());
        editorForegroundColorProp.addLeftPadding(20); // Indent a little bit
        props.add(editorForegroundColorProp);

        gutterBackgroundColorProp = new ColorProperty("UI.Editor.gutterBackground",
                                                      "Gutter bg:",
                                                      ColorSelectionType.SOLID)
                .setSolidColor(ColorTheme.MATRIX.getGutterBackground());
        gutterBackgroundColorProp.addLeftPadding(20); // Indent a little bit
        props.add(gutterBackgroundColorProp);

        gutterForegroundColorProp = new ColorProperty("UI.Editor.gutterForeground",
                                                      "Gutter fg:",
                                                      ColorSelectionType.SOLID)
                .setSolidColor(ColorTheme.MATRIX.getGutterForeground());
        gutterForegroundColorProp.addLeftPadding(20); // Indent a little bit
        props.add(gutterForegroundColorProp);

        // Set up a listener to ensure proper enabled/disabled state:
        overrideLafProp.addFormFieldChangeListener(new PropertyFormFieldChangeListener() {
            @Override
            public void valueChanged(PropertyFormFieldValueChangedEvent event) {
                boolean isCustom = ((CheckBoxField)event.formField()).isChecked();
                FormPanel fp = event.formPanel();
                fp.getFormField(editorThemeProp.getFullyQualifiedName()).setEnabled(isCustom);
                fp.getFormField(editorBackgroundColorProp.getFullyQualifiedName()).setEnabled(isCustom);
                fp.getFormField(editorForegroundColorProp.getFullyQualifiedName()).setEnabled(isCustom);
                fp.getFormField(gutterBackgroundColorProp.getFullyQualifiedName()).setEnabled(isCustom);
                fp.getFormField(gutterForegroundColorProp.getFullyQualifiedName()).setEnabled(isCustom);
            }
        });

        return props;
    }

    /**
     * Builds and returns properties related to the editor tabs.
     */
    private List<AbstractProperty> buildEditorTabProperties() {
        List<AbstractProperty> props = new ArrayList<>();

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

        return props;
    }

    /**
     * Builds and returns all hidden properties - these are internal properties
     * that the application itself will use without ever directly exposing to the user.
     */
    private List<AbstractProperty> buildHiddenProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        // Hidden props (persisted but never directly shown to the user):
        lastBrowseDirProp = new DirectoryProperty("hidden.props.lastBrowseDirectory",
                                                  "Last Browse Directory:",
                                                  true,
                                                  Version.SETTINGS_DIR);
        lastBrowseDirProp.setExposed(false);
        props.add(lastBrowseDirProp);

        return props;
    }

    /**
     * Builds all keyboard shortcut properties.
     * Note that we are using effectively singleton Action instances.
     * This allows the KeyStrokeManager to modify the accelerator
     * associated with the Action when the keyboard shortcut is changed.
     */
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
        props.add(new KeyStrokeProperty(KEY_SAVE_UNENCRYPTED, "Save Unencrypted:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+Shift+1"), // "!" for unsafe save.
                                        saveUnencryptedAction)
                          .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_CRYPT, "Encrypt/Decrypt:",
                                        KeyStrokeManager.parseKeyStroke("Ctrl+D"), // D for "decrypt/crypt" :)
                                        cryptAction)
                          .setAllowBlank(true));
        props.add(new KeyStrokeProperty(KEY_FORGET_PASSWORD, "Forget Password:",
                                        KeyStrokeManager.parseKeyStroke("F7"),
                                        forgetPasswordAction)
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

    /**
     * Invoked when the color theme chooser is modified. Will look up all relevant
     * color fields and set all their values according to the selected scheme.
     */
    private void setColorTheme(PropertyFormFieldValueChangedEvent event) {
        FormPanel fp = event.formPanel();
        if (fp == null) {
            return;
        }

        // Look up all our generated form fields:
        ColorField editorBgField = (ColorField)fp.getFormField(editorBackgroundColorProp.getFullyQualifiedName());
        ColorField editorFgField = (ColorField)fp.getFormField(editorForegroundColorProp.getFullyQualifiedName());
        ColorField gutterBgField = (ColorField)fp.getFormField(gutterBackgroundColorProp.getFullyQualifiedName());
        ColorField gutterFgField = (ColorField)fp.getFormField(gutterForegroundColorProp.getFullyQualifiedName());

        // Set them all!
        ColorTheme theme = (ColorTheme)((ComboField<?>)event.formField()).getSelectedItem();
        editorBgField.setColor(theme.getEditorBackground());
        editorFgField.setColor(theme.getEditorForeground());
        gutterBgField.setColor(theme.getGutterBackground());
        gutterFgField.setColor(theme.getGutterForeground());
    }
}
