package ca.corbett.crypttext;

import ca.corbett.extras.about.AboutInfo;

import java.io.File;

/**
 * This class contains version and directory information that your application code
 * can use to identify itself, and to locate important directories.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class Version {
    private static final AboutInfo aboutInfo;

    public static String NAME = "CryptText";
    public static String VERSION = "1.0-SNAPSHOT";
    public static String FULL_NAME = NAME + " " + VERSION;
    public static String COPYRIGHT = "Copyright © 2026 Steve Corbett";
    public static String PROJECT_URL = "https://github.com/scorbo2/crypttext";
    public static String LICENSE = "https://opensource.org/license/mit";

    /**
     * The directory where the application was installed -
     * caution, this might be null! We can't guess a
     * value for this property, it has to be supplied
     * by the launcher script. But our launcher script
     * might have been modified by the user, or the user
     * might have started the app without using the launcher,
     * in which case this property might not be set at all.
     * <b>Don't assume there will always be a value here!</b>
     * <p>
     * The installer script for linux defaults this
     * to /opt/${NAME}, but the user can override that.
     * </p>
     */
    public static final File INSTALL_DIR;

    /**
     * The directory where application configuration and
     * log files can go. If not given to us explicitly by
     * the launcher script, we default it a directory named
     * ".CryptText" in the user's home directory.
     * (The application name with a dot in front to
     * make it hidden on unix-like systems.)
     * <p>
     * This property will always have a value.
     * </p>
     */
    public static final File SETTINGS_DIR;

    /**
     * If we were packed with an update sources json file,
     * it will be located in the application install directory,
     * with an optional override in the user settings dir.
     * <p>
     * This will be null if no update sources file was provided.
     * </p>
     */
    public static final File UPDATE_SOURCES_FILE;

    /**
     * The directory to scan for extension jars at startup.
     * If not given to us explicitly by the launcher script,
     * we default it to a directory called "extensions"
     * inside of SETTINGS_DIR.
     * <p>
     * This property will always have a value.
     * </p>
     */
    public static final File EXTENSIONS_DIR;

    static {
        // This AboutInfo will be used to build the AboutDialog.
        aboutInfo = new AboutInfo();
        aboutInfo.applicationName = NAME;
        aboutInfo.applicationVersion = VERSION;
        aboutInfo.copyright = COPYRIGHT;
        aboutInfo.license = LICENSE;
        aboutInfo.projectUrl = PROJECT_URL;
        aboutInfo.showLogConsole = false; // If true, makes the app logo clickable to show the log console.
        aboutInfo.releaseNotesLocation = "/ca/corbett/crypttext/ReleaseNotes.txt";

        aboutInfo.logoImageLocation = "/ca/corbett/crypttext/images/logo_wide.jpg";
        aboutInfo.logoDisplayMode = AboutInfo.LogoDisplayMode.STRETCH;

        aboutInfo.shortDescription = "User-friendly extensible encrypted text editor.";

        // See if we were given an installation directory:
        String installDir = System.getProperty("INSTALL_DIR", null);
        INSTALL_DIR = installDir == null ? null : new File(installDir);

        // If a user settings directory was not supplied, we can provide a default in user's home:
        String appDir = System.getProperty("SETTINGS_DIR",
                new File(System.getProperty("user.home"), "." + NAME).getAbsolutePath());
        SETTINGS_DIR = new File(appDir);
        if (!SETTINGS_DIR.exists()) {
            SETTINGS_DIR.mkdirs();
        }

        // The extensions directory will live under the user settings directory:
        String extDir = System.getProperty("EXTENSIONS_DIR", new File(SETTINGS_DIR, "extensions").getAbsolutePath());
        EXTENSIONS_DIR = new File(extDir);
        if (!EXTENSIONS_DIR.exists()) {
            EXTENSIONS_DIR.mkdirs();
        }

        // We may optionally have been provided an update sources file in user settings dir:
        File updateSourcesFile = new File(SETTINGS_DIR, "update_sources.json");
        if (!updateSourcesFile.exists() && INSTALL_DIR != null) {
            // If it's not in user settings, try again in the installation dir:
            updateSourcesFile = new File(INSTALL_DIR, "update_sources.json");
        }
        UPDATE_SOURCES_FILE = updateSourcesFile.exists() ? updateSourcesFile : null;
    }

    public static AboutInfo getAboutInfo() {
        return aboutInfo;
    }
}
