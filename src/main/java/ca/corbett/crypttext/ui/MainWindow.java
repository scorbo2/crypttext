package ca.corbett.crypttext.ui;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.SingleInstanceManager;
import ca.corbett.extras.logging.LogConsole;
import ca.corbett.extras.logging.LogConsoleStyle;
import ca.corbett.extras.logging.LogConsoleTheme;
import ca.corbett.extras.properties.KeyStrokeProperty;
import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.Main;
import ca.corbett.crypttext.CryptTextResourceLoader;
import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;
import ca.corbett.crypttext.ui.actions.UIReloadAction;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.logging.Logger;

/**
 * The main application window for your application.
 * This is a singleton class - use MainWindow.getInstance() to get the instance.
 */
public final class MainWindow extends JFrame implements UIReloadable {

    private static MainWindow instance;
    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());
    private boolean isSingleInstanceModeEnabled;
    private MenuManager menuManager;
    private final KeyStrokeManager keyStrokeManager;
    private MessageUtil messageUtil;

    private MainWindow() {
        setTitle(Version.FULL_NAME);
        setIconImage(CryptTextResourceLoader.getSwingExtrasIcon()); // TODO - set your application icon here
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowCloseHandler());
        keyStrokeManager = new KeyStrokeManager(this);
        setKeyStrokes();
        menuManager = new MenuManager();
        setJMenuBar(menuManager.getMainMenuBar());
        UIReloadAction.getInstance().registerReloadable(this);
        isSingleInstanceModeEnabled = AppConfig.getInstance().isSingleInstanceEnabled();
        configureLogConsole();
    }

    public static MainWindow getInstance() {
        if (instance == null) {
            instance = new MainWindow();
        }
        return instance;
    }

    /**
     * When single-instance mode is enabled, and a second instance of your application tries
     * to start up, it will detect that the primary instance is already running, send its
     * start arguments to the primary instance, and then immediately exit. The primary instance
     * will then invoke this method on the EDT to allow it to process those arguments.
     */
    public void processStartArgs(List<String> args) {
        // Bring the main window to the front:
        // (If running in single instance mode, we want to make sure the user sees it.)
        bringToFront();

        // If we were given no args, we're done:
        // But note that we do this AFTER bringing the window to the front.
        // If you try to launch a second instance when the first instance is up,
        // but it is obscured by some other window, we want to bring the single instance to the front.
        // Otherwise, it may seem to the user like nothing happened, because the new instance just exits.
        if (args == null || args.isEmpty()) {
            return;
        }

        // Process each argument:
        for (String arg : args) {
            // Strip wrapping single quotes if present:
            // (some OSes/shells may add these - tested on Linux Mint with Cinnamon, and it's a problem there):
            if (arg.startsWith("'") && arg.endsWith("'") && arg.length() > 1) {
                arg = arg.substring(1, arg.length() - 1);
            }

            // Now handle the argument:
            logger.info("Received start argument: " + arg);
        }
    }

    /**
     * Hook invoked as the application is shutting down,
     * or when the application needs to restart to pick up an extension change.
     */
    public void cleanup() {
        logger.info("Shutting down: MainWindow cleanup invoked.");
        CryptTextExtensionManager.getInstance().deactivateAll();

        // Do whatever cleanup your application requires here...
        // Close db connections, save window state, commit unsaved data, etc.

        logger.info("Cleanup completed.");
    }

    /**
     * Invoked when the application's UI needs to be reloaded,
     * either because the application properties have been updated,
     * or because extensions have been enabled/disabled.
     */
    @Override
    public void reloadUI() {
        // Single instance mode may have changed, so check that:
        if (isSingleInstanceModeEnabled != AppConfig.getInstance().isSingleInstanceEnabled()) {
            toggleSingleInstanceMode();
        }

        // Reassign our keyboard shortcuts, as they may have changed:
        setKeyStrokes();

        // Rebuild our main menu, as the available items may have changed:
        menuManager.rebuildAll();
    }

    /**
     * Sets up our KeyStrokeManager with the appropriate KeyStrokes from app config.
     */
    private void setKeyStrokes() {
        keyStrokeManager.clear();
        for (KeyStrokeProperty prop : AppConfig.getInstance().getKeyStrokeProperties()) {
            // If there's no Action attached, or if there is no keystroke assigned to it, skip it:
            if (prop.getAction() == null || prop.getKeyStroke() == null) {
                continue;
            }

            // Register it! This will update the shortcut attached to our menu items as well:
            keyStrokeManager.registerHandler(prop.getKeyStroke(), prop.getAction());
        }
    }

    /**
     * Who would've thunk that bringing a window to the front would be so
     * platform-dependent and require all sorts of goofy hacks?
     */
    private void bringToFront() {
        final boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
        setState(JFrame.NORMAL); // unminimize if needed
        if (isLinux) {
            setAlwaysOnTop(true); // cheesy trick to make this work on linux
        }
        toFront();
        requestFocus();
        if (isLinux) {
            setAlwaysOnTop(false); // linux mint cinnamon seems to ignore toFront() unless we do this
        }
    }

    /**
     * Invoked internally to toggle the state of single-instance mode.
     */
    private void toggleSingleInstanceMode() {
        // Toggle our cached value:
        isSingleInstanceModeEnabled = !isSingleInstanceModeEnabled;

        // If single instance mode is now enabled, try to acquire the lock:
        if (isSingleInstanceModeEnabled) {
            logger.info("Enabling single instance mode.");
            SingleInstanceManager instanceManager = SingleInstanceManager.getInstance();
            if (!instanceManager.tryAcquireLock(a -> MainWindow.getInstance().processStartArgs(a),
                                                Main.SINGLE_INSTANCE_PORT)) {
                // Another instance is already running, let's inform the user:
                getMessageUtil().error("Single Instance Mode",
                                       "Another instance of the application is already running.\n" +
                                               "Unable to enable single instance mode.");
                isSingleInstanceModeEnabled = false; // revert our cached value
            }
        }

        // Otherwise, if single instance mode is now disabled, release the lock if we have it:
        else {
            logger.info("Disabling single instance mode.");
            SingleInstanceManager.getInstance().release();
        }
    }

    /**
     * Invoked internally to set up the LogConsole. This is entirely optional,
     * and can be hidden entirely in the UI if you don't want users to see it.
     * This example application exposes the log console with a menu item in the "Help" menu.
     */
    private void configureLogConsole() {
        LogConsole.getInstance().setIconImage(getIconImage()); // use same logo as MainWindow

        // We can create our own LogConsole theme here.
        // This is more than just cosmetic, as we'll see.
        // Let's start by creating a new theme based on the "matrix" theme (green on black):
        LogConsoleTheme theme = LogConsoleTheme.createMatrixStyledTheme();

        // We can add custom styles to our theme.
        // This can help certain operations within your application stand out better in the LogConsole.
        // For example, let's create example styles for "dataImport" and "dataExport" operations:
        theme.setStyle("dataImport", createLogConsoleStyle("dataImport", Color.CYAN));
        theme.setStyle("dataExport", createLogConsoleStyle("dataExport", Color.MAGENTA));
        // TODO repeat for any other operations you wish to "stand out" in the LogConsole.

        // Now let's register our theme and switch to it immediately:
        LogConsole.getInstance().registerTheme("CryptTextTheme", theme, true);

        // To test it out, let's log some test messages.
        logger.info("The LogConsole has been initialized!");

        // Any log message that contains our special tokens will now visually stand out in the LogConsole:
        logger.info("dataImport: This is a simulated data import operation log message.");
        logger.info("dataExport: This is a simulated data export operation log message.");

        // Of course, that only works when viewing log messages in the LogConsole.
        // That's it! It's very easy to configure and use.
        logger.info("By styling your application operations, you can quickly spot important events!");
        logger.info("This makes monitoring and debugging much easier.");
    }

    /**
     * Creates a LogConsoleStyle for the given token and font color.
     * Any log message that contains the given token will be styled with the given color
     * when viewed in the LogConsole.
     */
    private LogConsoleStyle createLogConsoleStyle(String token, Color fontColor) {
        LogConsoleStyle style = new LogConsoleStyle();
        style.setLogToken(token, true);
        style.setFontColor(fontColor);
        style.setIsBold(true);
        return style;
    }

    /**
     * Lazily creates and returns our MessageUtil instance for reporting errors, warnings, info, etc.
     */
    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this);
        }
        return messageUtil;
    }

    /**
     * This class can ensure that our cleanup() method is invoked
     * whenever the main window is closed, whether by user action
     * or programmatically.
     */
    private static class WindowCloseHandler extends WindowAdapter {
        /**
         * Invoked when the user manually closes a window by clicking its X button
         * or using a keyboard shortcut like Ctrl+Q or whatever. This event handler
         * is NOT invoked when you manually dispose() the window (at least in my
         * testing on linux mint). We need BOTH windowClosing() and windowClosed() handlers
         * to ensure cleanup() is always invoked.
         */
        @Override
        public void windowClosing(WindowEvent e) {
            MainWindow.getInstance().cleanup();
        }

        /**
         * Invoked when you programmatically dispose() of the window. Note that the
         * user manually closing the window via the OS does NOT invoke this handler
         * (at least in my testing on linux mint). We need BOTH windowClosing() and windowClosed() handlers
         * to ensure cleanup() is always invoked.
         */
        @Override
        public void windowClosed(WindowEvent e) {
            MainWindow.getInstance().cleanup();
        }
    }
}
