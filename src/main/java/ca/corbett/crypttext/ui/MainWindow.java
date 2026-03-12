package ca.corbett.crypttext.ui;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.CryptTextResourceLoader;
import ca.corbett.crypttext.Main;
import ca.corbett.crypttext.RecentFilesManager;
import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.VetoException;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;
import ca.corbett.crypttext.extensions.ExtraComponentPosition;
import ca.corbett.crypttext.text.Text;
import ca.corbett.crypttext.text.TextManager;
import ca.corbett.crypttext.ui.actions.UIReloadAction;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.SingleInstanceManager;
import ca.corbett.extras.ToggleableTabbedPane;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.io.TextFileDetector;
import ca.corbett.extras.logging.LogConsole;
import ca.corbett.extras.logging.LogConsoleStyle;
import ca.corbett.extras.logging.LogConsoleTheme;
import ca.corbett.extras.properties.KeyStrokeProperty;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main application window for your application.
 * This is a singleton class - use MainWindow.getInstance() to get the instance.
 */
public final class MainWindow extends JFrame implements UIReloadable {

    private static MainWindow instance;
    private static final Logger logger = Logger.getLogger(MainWindow.class.getName());
    private boolean isSingleInstanceModeEnabled;
    private final MenuManager menuManager;
    private final KeyStrokeManager keyStrokeManager;
    private final EditorTabPane editorTabPane;
    private TabStateManager tabStateManager;
    private final RecentFilesManager recentFilesManager;
    private MessageUtil messageUtil;
    private boolean cleanupComplete;

    private MainWindow() {
        setTitle(Version.FULL_NAME);
        setIconImage(CryptTextResourceLoader.getSquareLogo());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowCloseHandler());
        cleanupComplete = false;
        keyStrokeManager = new KeyStrokeManager(this);
        menuManager = new MenuManager();
        recentFilesManager = new RecentFilesManager();
        recentFilesManager.setListLimit(AppConfig.getInstance().getRecentFilesLimit());
        recentFilesManager.load();
        tabStateManager = CryptTextExtensionManager.getInstance().getTabStateManager();
        setJMenuBar(menuManager.getMainMenuBar());
        UIReloadAction.getInstance().registerReloadable(this);
        isSingleInstanceModeEnabled = AppConfig.getInstance().isSingleInstanceEnabled();
        configureLogConsole();
        editorTabPane = new EditorTabPane();
        editorTabPane.addChangeListener(e -> updateTitleBar());
        setLayout(new BorderLayout());

        // We will eventually support command-line args for opening specific files on launch.
        // We will also have an option for restoring previously-opened tabs on startup.
        // For now, we will just start with a single blank tab:
        newTab();

        // Set up drag-and-drop support so users can drag files from OS file manager:
        MainWindow.configureDropTarget(this, getMessageUtil());
    }

    public static MainWindow getInstance() {
        if (instance == null) {
            instance = new MainWindow();
        }
        return instance;
    }

    /**
     * Overridden so we can populate our UI when the main window becomes visible.
     */
    @Override
    public void setVisible(boolean isVisible) {
        if (isVisible) {
            reloadUI();
        }
        super.setVisible(isVisible);
    }

    /**
     * Shorthand for getEditorTabPane().getTextManager() - returns the TextManager instance for this main window.
     */
    public TextManager getTextManager() {
        return editorTabPane.getTextManager();
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

            // For each argument, make sure it's a valid file path:
            File argFile = new File(arg);
            if (!argFile.exists() || !argFile.isFile() || !argFile.canRead()) {
                getMessageUtil().warning("Invalid file argument",
                                         "The given argument is not a valid, readable file:\n" + arg);
                continue;
            }

            // If so, let's load each one into a new tab:
            try {
                Text text = editorTabPane.getTextManager().fromFile(argFile);
                editorTabPane.clearIfScratch(); // Don't leave the default "Untitled" tab open if we load something.
                editorTabPane.newTextTab(text, argFile.getName());
            }
            catch (VetoException ignored) {
                // An extension vetoed the load!
                // Just skip it - TextManager has already logged the veto.
            }
            catch (IOException | IllegalArgumentException e) {
                getMessageUtil().error("File error",
                                       "An error occurred while trying to open the file:\n" + arg + "\n\n" +
                                               "Error message: " + e.getMessage(),
                                       e);
            }
        }
    }

    /**
     * Adds a recently-accessed file to our RecentFilesManager, which manages the list and persistence of recent files.
     * This will update the "recent files" submenu in the File menu.
     */
    public void addRecentFile(File file) {
        recentFilesManager.add(file);
        menuManager.rebuildRecentFilesMenu(recentFilesManager.getRecentFiles());
    }

    /**
     * Hook invoked as the application is shutting down,
     * or when the application needs to restart to pick up an extension change.
     */
    public void cleanup() {
        // Make the method idempotent, just in case:
        if (cleanupComplete) {
            return;
        }

        cleanupComplete = true;
        logger.info("Shutting down: MainWindow cleanup invoked.");

        // Persist our list of "most recently accessed" files:
        recentFilesManager.save();

        // There is no "cancel" option possible here, because this method is invoked
        // when the application is definitely about to close (for example, via UpdateManager
        // when we do a restart to pick up extension changes). But we can still do
        // a basic "save changes?" prompt with yes/no options.
        boolean isAnyTabDirty = false;
        for (EditorTab editorTab : editorTabPane.getEditorTabs()) {
            if (editorTab.isDirty()) {
                isAnyTabDirty = true;
                break;
            }
        }
        if (isAnyTabDirty) {
            if (getMessageUtil().askYesNo("Save changes?",
                                          "Save changes before exiting?") == MessageUtil.YES) {
                for (EditorTab editorTab : editorTabPane.getEditorTabs()) {
                    if (editorTab.isDirty()) {
                        try {
                            editorTab.save();
                        }
                        catch (VetoException ignored) {
                            // An extension vetoed the save!
                            // Just skip it - EditorTab has already logged the veto (indirectly via TextManager).
                        }
                        catch (Exception ioe) {
                            // Just log it at this point... we are exiting:
                            logger.log(Level.SEVERE, "Error saving tab \""
                                               + editorTab.getTabName()
                                               + "\" during cleanup: "
                                               + ioe.getMessage(),
                                       ioe);
                        }
                    }
                }
            }
        }

        // Now save our tab state:
        // (we do this after the above save code, because some of those save operations
        //  may have turned into "save as" operations if the tab had no source file yet.
        //  By deferring tab state save until here, we guarantee we get the actual
        //  save locations of those files).
        saveTabState();

        try {
            // Clean up any scratch files as needed.
            getTextManager().dispose();
        }
        catch (IOException ioe) {
            // Log and continue shutdown; failure to dispose should not block exit.
            logger.log(Level.WARNING,
                       "Error disposing TextManager during cleanup: " + ioe.getMessage(),
                       ioe);
        }

        // Deactivate all extensions:
        CryptTextExtensionManager.getInstance().deactivateAll();

        // Release our Single instance lock if we have it. (This is idempotent, safe to invoke)
        SingleInstanceManager.getInstance().release();

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
        recentFilesManager.setListLimit(AppConfig.getInstance().getRecentFilesLimit());
        menuManager.rebuildAll();
        menuManager.rebuildRecentFilesMenu(recentFilesManager.getRecentFiles());

        // User may have enabled or disabled the option to show lock icons on tabs:
        editorTabPane.updateTabIcons();

        // Make sure we register a TabStateManager, if any extension supplies one:
        tabStateManager = CryptTextExtensionManager.getInstance().getTabStateManager();

        // Update our title bar in case the user toggled the option to show full paths there:
        updateTitleBar();

        // Our list of extra components around the main tab pane may have changed,
        // if extensions were installed/uninstalled/enabled/disabled, so let's
        // rebuild the whole content pane from scratch:
        getContentPane().removeAll();
        add(editorTabPane, BorderLayout.CENTER);
        addExtraComponents(ExtraComponentPosition.LEFT, BorderLayout.WEST);
        addExtraComponents(ExtraComponentPosition.RIGHT, BorderLayout.EAST);
        addExtraComponents(ExtraComponentPosition.TOP, BorderLayout.NORTH);
        addExtraComponents(ExtraComponentPosition.BOTTOM, BorderLayout.SOUTH);

        // Ensure the updated content pane is laid out and repainted immediately.
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    /**
     * Returns the EditorTabPane instance for this main window.
     */
    public EditorTabPane getEditorTabPane() {
        return editorTabPane;
    }

    /**
     * Shorthand for getEditorTabPane().newTab() - creates a new, blank tab in the main editor tab pane.
     */
    public void newTab() {
        editorTabPane.newTextTab();
    }

    /**
     * Returns a configured JFileChooser instance for use in our Open and Save dialogs.
     * The FileFilter is set to TextFileFilter.DEFAULT (text files).
     */
    public JFileChooser getFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(AppConfig.getInstance().getLastBrowseDirectory());
        fileChooser.setFileFilter(TextFileFilter.DEFAULT);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        return fileChooser;
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
        try {
            toFront();
            requestFocus();
        }
        finally {
            if (isLinux) {
                setAlwaysOnTop(false); // linux mint cinnamon seems to ignore toFront() unless we do this
            }
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
     * Invoked internally to add any extra components provided by extensions in the given position.
     *
     * @param position the position to get extra components for (e.g. left, right, top, bottom)
     * @param borderLayoutPosition the corresponding BorderLayout position for the given position
     */
    private void addExtraComponents(ExtraComponentPosition position, String borderLayoutPosition) {
        List<JComponent> extraComponents = CryptTextExtensionManager.getInstance().getExtraComponents(position);
        ToggleableTabbedPane extraTabPane = new ToggleableTabbedPane();
        if (extraComponents != null && ! extraComponents.isEmpty()) {
            for (JComponent extraComponent : extraComponents) {
                extraTabPane.add(extraComponent);
            }
            // Don't show the tab header row if there's only one component:
            extraTabPane.setTabHeaderVisible(extraComponents.size() > 1);
            add(extraTabPane, borderLayoutPosition);
        }
    }

    /**
     * Invoked internally to set up the LogConsole with our custom CryptText theme and styles.
     */
    private void configureLogConsole() {
        LogConsole.getInstance().setIconImage(getIconImage()); // use same logo as MainWindow

        // Our custom theme will be based on the "matrix" theme (green on black):
        LogConsoleTheme theme = LogConsoleTheme.createMatrixStyledTheme();

        // We'll customize colors for our known operation types so they stand out in the log:
        theme.setStyle("Encrypt", createLogConsoleStyle("Encrypt:", Color.RED));
        theme.setStyle("Decrypt", createLogConsoleStyle("Decrypt:", Color.CYAN));
        theme.setStyle("Load", createLogConsoleStyle("Load:", Color.MAGENTA));
        theme.setStyle("Save", createLogConsoleStyle("Save:", Color.MAGENTA));
        theme.setStyle("Veto", createLogConsoleStyle("Veto:", Color.ORANGE));

        // Now let's register our theme and switch to it immediately:
        LogConsole.getInstance().registerTheme("CryptTextTheme", theme, true);
    }

    /**
     * Creates a bold style for the given token and font color, to be used in our LogConsole theme.
     */
    private LogConsoleStyle createLogConsoleStyle(String token, Color fontColor) {
        LogConsoleStyle style = new LogConsoleStyle();
        style.setLogToken(token, true);
        style.setFontColor(fontColor);
        style.setIsBold(true);
        return style;
    }

    /**
     * Writes out a list of currently-open tabs so that it can be optionally restored
     * on the next application run. Note that we don't store this in AppConfig, as it's
     * not really a preference - it's more of a "session" state.
     * But the user can enable/disable this behavior in AppConfig.
     */
    public void saveTabState() {
        // If no extension supplied a TabStateManager, use the default implementation:
        if (tabStateManager == null) {
            tabStateManager = new DefaultTabStateManager();
        }

        // Delegate to our utility class:
        tabStateManager.saveTabState(editorTabPane);
    }

    /**
     * Attempts to restore previously-opened tabs from the last application run.
     * Note that we don't store this in AppConfig, as it's not really a preference - it's more of a "session" state.
     * But the user can enable/disable this behavior in AppConfig.
     */
    public void restoreTabState() {
        // If no extension supplied a TabStateManager, use the default implementation:
        if (tabStateManager == null) {
            tabStateManager = new DefaultTabStateManager();
        }

        // Delegate to our utility class:
        tabStateManager.restoreTabState(editorTabPane);
    }

    /**
     * Intelligently updates the main window title bar based on currently-selected file, if anyu.
     * If the "show full path in title bar" option is enabled, we'll show
     * the full path and name of the currently-shown file.
     * For scratch files (files created in-memory and not yet saved), we'll just show
     * the tab name instead (example: "Untitled 1").
     */
    public void updateTitleBar() {
        // Start with our default title:
        setTitle(Version.FULL_NAME);

        // If the option is disabled, we're done:
        if (!AppConfig.getInstance().isShowFullPathInTitleEnabled()) {
            return;
        }

        // Get the current tab (it might not be an EditorTab! - ignore if not):
        Component c = editorTabPane.getSelectedComponent();
        if (c instanceof EditorTab editorTab) { // this also filters out null (no tab open)

            // for scratch files or null files, just show the tab name (e.g. "Untitled 1")
            if (editorTab.isScratchFile() || editorTab.getDiskContents().getSourceFile() == null) {
                setTitle(Version.FULL_NAME + " - " + editorTab.getTabName());
            }

            // Otherwise, use the full path:
            else {
                setTitle(Version.FULL_NAME + " - " + editorTab.getDiskContents().getSourceFile().getAbsolutePath());
            }
        }
    }

    /**
     * Uses TextFileDetector and other heuristics to determine if the given
     * candidate file is a valid, existing, and readable text-based file.
     * No errors are logged or reported to the user - a simple true/false
     * return is all you get here.
     *
     * @param file Any candidate File.
     * @return true only if all checks succeed - the file exists, is readable, and looks like text.
     */
    public static boolean isValidTextFile(File file) {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            return false;
        }
        try {
            if (!TextFileDetector.isTextFile(file)) {
                return false;
            }
        }
        catch (IOException ignored) {
            return false;
        }

        return true;
    }

    /**
     * Configures the given component to accept file drops from the OS file manager.
     * Dropped files are validated using TextFileDetector and opened in new editor tabs.
     * This overrides any built-in drag-and-drop behavior on the target component
     * (e.g. JTextPane, as we invoke this method from each EditorTab as well).
     *
     * @param target      The component to register the drop target on.
     * @param messageUtil A MessageUtil instance used to report errors to the user.
     */
    public static void configureDropTarget(Component target, MessageUtil messageUtil) {
        new DropTarget(target, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                boolean dropSucceeded = false;
                try {
                    Transferable transferable = event.getTransferable();
                    if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        logger.warning("Rejecting invalid drop: not a file list.");
                        event.rejectDrop();
                        return;
                    }
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>)transferable
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (droppedFiles.isEmpty()) {
                        // Rare but could happen.
                        logger.warning("Rejecting invalid drop: empty file list.");
                        return;
                    }
                    int successCount = 0;
                    for (File file : droppedFiles) {
                        if (!isValidTextFile(file)) {
                            final String path = file == null ? "null" : file.getAbsolutePath();
                            final String msg = "Rejecting invalid drop: not a text file: " + path;
                            if (!SwingUtilities.isEventDispatchThread()) {
                                // dispatch via EDT and wait for response.
                                // We wait to avoid popping multiple error dialogs at once
                                // if more than one file is invalid.
                                SwingUtilities.invokeAndWait(() -> messageUtil.error(msg));
                            }
                            else {
                                // We're on the EDT, just show it:
                                messageUtil.error(msg);
                            }
                            continue;
                        }

                        // Open the file in a new tab on the EDT:
                        SwingUtilities.invokeLater(() -> getInstance().getEditorTabPane().newTextTab(file));
                        successCount++;
                    }
                    dropSucceeded = (successCount > 0); // if any succeeded, we'll call it a win.
                }
                catch (Exception e) {
                    logger.log(Level.WARNING, "Unexpected error handling drag-and-drop: " + e.getMessage(), e);
                }
                finally {
                    event.dropComplete(dropSucceeded);
                }
            }
        });
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
