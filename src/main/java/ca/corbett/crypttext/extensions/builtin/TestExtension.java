package ca.corbett.crypttext.extensions.builtin;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.extensions.CryptTextExtension;
import ca.corbett.crypttext.ui.MainWindow;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is an example of a built-in extension for your application.
 * You can supply built-in extensions by supplying them directly
 * to your application's ExtensionManager implementation.
 * These don't need to be packaged into separate jar files and loaded
 * dynamically.
 * <p>
 * Extensions are typically built and distributed as jar files that can
 * be dynamically loaded by the application at runtime. Refer to the
 * <a href="https://www.corbett.ca/swing-extras-book/">swing-extras documentation</a>
 * for more information on building and distributing extensions.
 * </p>
 * <p>
 * This minimal TestExtension is just to give you an idea of what's
 * possible with the application extension framework!
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TestExtension extends CryptTextExtension {
    private static final Logger logger = Logger.getLogger(TestExtension.class.getName());
    private final AppExtensionInfo extInfo;

    private final String keyStrokeMessage = "You pressed the TestExtension keyboard shortcut!\n" +
            "This keyboard shortcut will only work when the extension is enabled.";

    private final String menuMessage = "You selected a TestExtension menu item!\n" +
            "This menu item will only be visible when the extension is enabled.\n\n" +
            "Disabling the TestExtension will remove this menu item.";

    public TestExtension() {
        // All extensions must supply a well-formed AppExtensionInfo object:
        extInfo = new AppExtensionInfo.Builder("TestExtension")
                .setVersion("1.0.0")
                .setAuthor("scorbo2")
                .setShortDescription("A test extension for demonstration purposes.")
                .setLongDescription(
                        "This is a test extension for demonstration purposes only. It doesn't actually do anything useful.")
                .setTargetAppName(Version.NAME)
                .setTargetAppVersion(Version.VERSION)
                .build();
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    public void onActivate() {
        // This method is invoked when the application starts up
        // or when the extension is enabled.
        logger.info("TestExtension has been activated!");
    }

    @Override
    public void onDeactivate() {
        // This method is invoked when the application is shutting down
        // or when the extension is disabled.
        logger.info("TestExtension has been deactivated!");
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        // We'll return some example config properties.
        // These will automatically be included in the application properties dialog
        // when this extension is enabled, and will automatically be hidden in the
        // properties dialog when the extension is disabled.
        List<AbstractProperty> props = new ArrayList<>();
        props.add(new LabelProperty("TestExtension.General.label1",
                                    "These properties were supplied by TestExtension."));
        props.add(new LabelProperty("TestExtension.General.label2",
                                    "They don't actually do anything."));
        props.add(new BooleanProperty("TestExtension.General.field1", "Option 1", true));
        props.add(new BooleanProperty("TestExtension.General.field2", "Option 2", true));
        props.add(new BooleanProperty("TestExtension.General.field3", "Option 3", true));
        props.add(new LabelProperty("TestExtension.General.label3",
                                    "If you disable TestExtension, these should vanish."));

        // We can register a keyboard shortcut property as well if we want:
        String propName = AppConfig.KEYSTROKE_PREFIX + "TestExtension.TestAction";
        KeyStrokeProperty keyProp = new KeyStrokeProperty(propName,
                                                          "Key shortcut:",
                                                          KeyStrokeManager.parseKeyStroke("Ctrl+T"),
                                                          new DummyAction("TestExtension Action", keyStrokeMessage));

        // We could prevent it from showing up on the app properties dialog if we wanted to:
        // keyProp.setExposed(false);

        // If we do expose it, we can give the user the option to unassign it by blanking out the field:
        keyProp.setAllowBlank(true);

        // Otherwise, the user can see it and customize it, and the new shortcut will be automatically persisted!
        props.add(keyProp);

        return props;
    }

    @Override
    protected void loadJarResources() {
        // Nothing to load here. If this were a typical, jar-loaded extension,
        // then we could load icon images or other resources from our jar file.
    }

    @Override
    public List<JMenu> getTopLevelMenus() {
        // We'll add a simple "Test" menu with one item:
        JMenu testMenu = new JMenu("Test");
        JMenuItem aboutItem = new JMenuItem(new DummyAction("About TestExtension", menuMessage));
        testMenu.add(aboutItem);
        return List.of(testMenu);
    }

    @Override
    public List<JMenuItem> getMenuItems(String topLevelMenu) {
        List<JMenuItem> items = new ArrayList<>();

        // We'll return a dummy option for the "File" menu:
        if ("File".equals(topLevelMenu)) {
            items.add(new JMenuItem(new DummyAction("TestExtension Info", menuMessage)));
        }

        // And a dummy option for the "Edit" menu:
        if ("Edit".equals(topLevelMenu)) {
            items.add(new JMenuItem(new DummyAction("TestExtension Settings", menuMessage)));
        }

        // And a dummy option for the "Help" menu:
        if ("Help".equals(topLevelMenu)) {
            items.add(new JMenuItem(new DummyAction("TestExtension Help", menuMessage)));
        }

        return items;
    }

    /**
     * Just a quick example action to show a dialog when the action is executed.
     */
    private static class DummyAction extends AbstractAction {

        private final String msg;

        public DummyAction(String name, String msg) {
            super(name);
            this.msg = msg;
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            // Just show our message:
            JOptionPane.showMessageDialog(MainWindow.getInstance(), msg);
        }
    }
}
