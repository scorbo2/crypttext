package ca.corbett.crypttext;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.SingleInstanceManager;
import ca.corbett.updates.UpdateManager;
import ca.corbett.updates.UpdateSources;

import ca.corbett.crypttext.extensions.CryptTextExtensionManager;
import ca.corbett.crypttext.ui.MainWindow;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * The main entry point for the CryptText application.
 * GUI only - there is no command-line interface, but you can supply
 * file(s) to be loaded on startup as command-line arguments.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class Main {

    private static final String LOG_RESOURCE = "/ca/corbett/crypttext/logging.properties";

    public static final int SINGLE_INSTANCE_PORT = 54551; // arbitrary random port choice

    public static void main(String[] args) {
        // Before we do anything else, set up logging:
        configureLogging();

        // Peek at our config file to see if single instance mode is enabled:
        boolean isSingleInstanceEnabled = Boolean.parseBoolean(AppConfig.peek(AppConfig.SINGLE_INSTANCE_PROP));

        if (isSingleInstanceEnabled) {
            SingleInstanceManager instanceManager = SingleInstanceManager.getInstance();
            if (!instanceManager.tryAcquireLock(Main::handleStartArgs, SINGLE_INSTANCE_PORT)) {
                // Another instance is already running, let's send our args to it and exit:
                // Send even if empty, as this will force the main window to the front.
                SingleInstanceManager.getInstance().sendArgsToRunningInstance(args);
                return;
            }
        }

        // We are the only instance running, so we can start up normally:
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.log(Level.INFO,
                   Version.FULL_NAME + " starting up: installDir={0}, settingsDir={1}, extensionsDir={2}",
                   new Object[]{Version.INSTALL_DIR, Version.SETTINGS_DIR, Version.EXTENSIONS_DIR});

        // Load additional Look and Feels:
        LookAndFeelManager.installExtraLafs();

        // Load all extensions:
        CryptTextExtensionManager extManager = CryptTextExtensionManager.getInstance();
        extManager.loadAll();
        extManager.activateAll();
        logger.log(Level.INFO, "Loaded {0} extensions ({1} enabled).",
                   new Object[]{extManager.getLoadedExtensionCount(), extManager.getEnabledLoadedExtensions().size()});

        // Load our application configuration:
        AppConfig.getInstance().load();

        // Parse update sources if provided, to enable dynamic extension discovery:
        parseUpdateSources();

        // We're ready to start up the UI on the EDT:
        SwingUtilities.invokeLater(() -> {
            // Now we can set whatever Look and Feel the user has selected to be the default:
            // (note that the user can change this at runtime in the properties dialog)
            logger.info("Switching to Look and Feel: " + AppConfig.getInstance().getLookAndFeelClassName());
            LookAndFeelManager.switchLaf(AppConfig.getInstance().getLookAndFeelClassName());

            // Show the main window:
            MainWindow mainWindow = MainWindow.getInstance();
            mainWindow.setLocationRelativeTo(null); // center on screen
            mainWindow.setVisible(true);

            // Process any start arguments we were given:
            mainWindow.processStartArgs(Arrays.asList(args));
        });
    }

    /**
     * The startup code will invoke this to handle start arguments on the EDT.
     * This is invoked when a second instance tries to start up when
     * single instance mode is enabled. In that case, the new instance
     * will send its args to the running instance and immediately terminate.
     * The running instance can then process those args.
     */
    private static void handleStartArgs(List<String> args) {
        SwingUtilities.invokeLater(() -> MainWindow.getInstance().processStartArgs(args));
    }


    /**
     * If an update sources json was provided, we can parse it here and make it automatically
     * available to our ExtensionManager implementation. This enables dynamic extension
     * discovery and download at runtime.
     */
    private static void parseUpdateSources() {
        Logger logger = Logger.getLogger(Main.class.getName());
        if (Version.UPDATE_SOURCES_FILE != null) {
            try {
                UpdateSources updateSources = UpdateSources.fromFile(Version.UPDATE_SOURCES_FILE);
                UpdateManager updateManager = new UpdateManager(updateSources);

                // Let's register a shutdown hook for when UpdateManager restarts the app to pick up new extensions:
                updateManager.registerShutdownHook(MainWindow.getInstance()::cleanup);

                // Let our ExtensionManager know about this, so the "available" tab can
                // be loaded and shown automatically in the ExtensionManagerDialog:
                CryptTextExtensionManager.getInstance().setUpdateManager(updateManager);

                // Let our AboutInfo know about this too, so the About dialog can do application version checks:
                Version.getAboutInfo().updateManager = updateManager;

                logger.info("Update sources provided. Dynamic extension discovery is enabled.");
            }
            catch (Exception e) {
                logger.log(Level.SEVERE,
                           "Unable to parse update sources. Extension download will not be available. Error: "
                                   + e.getMessage(),
                           e);
            }
        }
        else {
            logger.log(Level.INFO, "No update sources provided. Dynamic extension discovery disabled.");
        }
    }

    /**
     * Logging can use the built-in configuration, or you can supply your own logging properties file.
     * <ol>
     *     <li><b>Built-in logging.properties</b>: the jar file comes packaged with a default logging.properties
     *     file that you can use. You don't need to do anything to activate this config: this is the default.</li>
     *     <li><b>Specify your own</b>: you can create a logging.properties file and put it $SETTINGS_DIR,
     *     OR you can start the application with the -Djava.util.logging.config.file=
     *     option, in which case you can point it to wherever your logging.properties file lives.</li>
     * </ol>
     */
    private static void configureLogging() {
        // If the java.util.logging.config.file System property exists, do nothing.
        // It will be used automatically.
        if (System.getProperties().containsKey("java.util.logging.config.file")) {
            //System.out.println("Using custom log file: " + System.getProperty("java.util.logging.config.file"));
            return;
        }

        // Otherwise, see if we can spot a logging.properties file in the application dir:
        File propsFile = new File(Version.SETTINGS_DIR, "logging.properties");
        if (propsFile.exists() && propsFile.canRead()) {
            System.setProperty("java.util.logging.config.file", propsFile.getAbsolutePath());
            //System.out.println("Using auto-detected log file: " + propsFile.getAbsolutePath());
            return;
        }

        // Otherwise, load the built-in config:
        try {
            //System.out.println("Using built-in logging.");
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream(LOG_RESOURCE));
        }
        catch (IOException ioe) {
            System.out.println("WARN: Unable to load log configuration: " + ioe.getMessage());
        }
    }
}