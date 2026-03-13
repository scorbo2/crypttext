package ca.corbett.crypttext.extensions.builtin;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.extensions.CryptTextExtension;
import ca.corbett.crypttext.ui.EditorTab;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.ComboProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.extras.properties.LabelProperty;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * An experimental extension to enable "immersive mode", where we'll hide
 * all UI elements except the editor, for a distraction-free writing experience.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ImmersiveModeExtension extends CryptTextExtension implements ChangeListener {

    private static final Logger log = Logger.getLogger(ImmersiveModeExtension.class.getName());

    private static final String TOGGLE_PROP = AppConfig.KEYSTROKE_PREFIX + "General.immersiveModeToggle";
    private static final String ESC_PROP = AppConfig.KEYSTROKE_PREFIX + "General.immersiveModeExit";
    private static final String INTRO_LABEL_PROP = "Immersive Mode.Options.introLabel";
    private static final String MONITOR_PROP = "Immersive Mode.Options.monitorIndex";

    private ImmersiveModeWindow immersiveWindow;
    private MessageUtil messageUtil;
    private final ImmersiveModeAction toggleAction;
    private final EscapeAction exitAction;
    private final JCheckBoxMenuItem toggleMenuItem;
    private final AppExtensionInfo extInfo;
    private boolean isImmersiveMode;

    public ImmersiveModeExtension() {
        extInfo = new AppExtensionInfo.Builder("Immersive Mode")
                .setVersion(Version.VERSION)
                .setTargetAppName(Version.NAME)
                .setTargetAppVersion(Version.VERSION)
                .setShortDescription("Hides all UI elements except the editor")
                .setLongDescription("Enables an immersive mode that hides all UI elements " +
                                            "except the text editor, for a distraction-free writing experience.")
                .build();
        toggleAction = new ImmersiveModeAction();
        exitAction = new EscapeAction();
        toggleMenuItem = new JCheckBoxMenuItem(toggleAction);
        toggleMenuItem.setSelected(false);
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    public void onActivate() {
        immersiveWindow = null;
        isImmersiveMode = false;
        LookAndFeelManager.addChangeListener(this);
    }

    @Override
    public void onDeactivate() {
        exitAction.actionPerformed(null); // make sure we exit immersive mode if we're currently in it
        LookAndFeelManager.removeChangeListener(this);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        // Make sure our menu item reflects changes to look and feel:
        SwingUtilities.updateComponentTreeUI(toggleMenuItem);
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        props.add(new KeyStrokeProperty(TOGGLE_PROP, "Immersive Mode:",
                                        KeyStrokeManager.parseKeyStroke("F11"),
                                        toggleAction)
                          .setAllowBlank(true));
        props.add(new KeyStrokeProperty(ESC_PROP, "Exit immersion:",
                                        KeyStrokeManager.parseKeyStroke("ESC"),
                                        exitAction)
                          .setAllowBlank(true));

        props.add(new LabelProperty(INTRO_LABEL_PROP,
                                    "<html>Immersive mode brings the current editor" +
                                            "<br>into full-screen mode, hiding all other " +
                                            "<br>UI elements for a distraction-free" +
                                            "<br>writing experience.</html>"));

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        List<String> options = new ArrayList<>(gs.length);
        options.add("Primary monitor");
        for (int i = 1; i < gs.length; i++) { // All monitors after index 0 are secondary
            options.add("Secondary monitor " + i);
        }
        props.add(new ComboProperty<>(MONITOR_PROP, "Monitor:", options, 0, false)
                          .setHelpText("If the selected monitor is invalid, the primary will be used."));

        return props;
    }

    @Override
    protected void loadJarResources() {
        // Nothing to load here
    }

    @Override
    public List<JMenuItem> getMenuItems(String topLevelMenu) {
        if (topLevelMenu.equals("Edit")) { // we should probably have a "view" menu...
            List<JMenuItem> items = new ArrayList<>();
            items.add(toggleMenuItem);
            return items;
        }

        return null;
    }

    private int getConfiguredMonitorIndex() {
        AbstractProperty prop = AppConfig.getInstance().getPropertiesManager().getProperty(MONITOR_PROP);
        if (prop instanceof ComboProperty<?> comboProp) {
            return comboProp.getSelectedIndex();
        }
        return 0; // default to primary monitor
    }

    /**
     * A simple action to immediately exit immersive mode if it's currently active.
     */
    private class EscapeAction extends EnhancedAction {

        public EscapeAction() {
            super("Exit Immersion");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (immersiveWindow != null) {
                immersiveWindow.setVisible(false);
                immersiveWindow.dispose();
                immersiveWindow = null;
                toggleMenuItem.setSelected(false);
            }
        }
    }

    /**
     * A simple action to toggle immersive mode on and off.
     * Currently, we create an undecorated JWindow, and duplicate the current
     * tab into a new EditorTab inside that window. Any changes that are made
     * in the immersive window are synced back to the main window automatically.
     */
    private class ImmersiveModeAction extends EnhancedAction {

        public ImmersiveModeAction() {
            super("Toggle Immersive Mode");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = MainWindow.getInstance().getEditorTabPane().getCurrentTab();
            if (!(c instanceof EditorTab editorTab)) {
                getMessageUtil().info("No editor tab is selected.");
                return;
            }

            isImmersiveMode = !isImmersiveMode;
            toggleMenuItem.setSelected(isImmersiveMode);

            if (isImmersiveMode) {
                // If this is somehow invoked when we already have one, dispose it:
                if (immersiveWindow != null) {
                    immersiveWindow.setVisible(false);
                    immersiveWindow.dispose();
                    immersiveWindow = null;
                }

                // Fire up a new one:
                immersiveWindow = new ImmersiveModeWindow(MainWindow.getInstance(),
                                                          editorTab,
                                                          getConfiguredMonitorIndex());

                // Listen for close events so we can update our state accordingly:
                immersiveWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent windowEvent) {
                        isImmersiveMode = false;
                        toggleMenuItem.setSelected(false);
                    }
                });

                immersiveWindow.setVisible(true);
            }

            else {
                // Shut down and close:
                if (immersiveWindow != null) {
                    immersiveWindow.setVisible(false);
                    immersiveWindow.dispose();
                    immersiveWindow = null;
                }
            }
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
