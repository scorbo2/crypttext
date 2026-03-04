package ca.corbett.crypttext.extensions.builtin;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.extensions.CryptTextExtension;
import ca.corbett.crypttext.extensions.ExtraComponentPosition;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A special test extension that exercises every extension feature, for testing purposes.
 * This is a special extension! It is not enabled or displayed to the user by default at all.
 * To enable this extension, set the enableTestExtension property to any value.
 * All of the following examples will work:
 * <pre>
 *     -DenableTestExtension=true
 *     -DenableTestExtension=1
 *     -DenableTestExtension=anything
 *     -DenableTestExtension
 * </pre>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TestExtension extends CryptTextExtension {

    private static final Logger log = Logger.getLogger(TestExtension.class.getName());

    private final AppExtensionInfo extInfo;
    private final EnhancedAction logDumpAction = new LogDumpAction();

    public TestExtension() {
        extInfo = new AppExtensionInfo.Builder("TestExtension")
                .setVersion(Version.VERSION)
                .setTargetAppName(Version.NAME)
                .setTargetAppVersion(Version.VERSION)
                .setShortDescription("A test extension.")
                .setLongDescription("Exercises every extension feature, for manual testing purposes.")
                .build();
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        props.add(new KeyStrokeProperty(AppConfig.KEYSTROKE_PREFIX + "Test.Log dump",
                                        "Log dump",
                                        KeyStrokeManager.parseKeyStroke("ctrl+shift+alt+D"),
                                        logDumpAction)
                          .setHelpText("Generates a diagnostic log dump"));

        return props;
    }

    @Override
    protected void loadJarResources() {
        // Nothing to load here
    }

    @Override
    public List<JMenu> getTopLevelMenus() {
        JMenu testMenu = new JMenu("Test Menu");
        testMenu.add(new JMenuItem(logDumpAction));
        return List.of(testMenu);
    }

    @Override
    public List<JMenuItem> getMenuItems(String topLevelMenu) {
        return null; // Nothing from us here
    }

    @Override
    public boolean fileWillLoad(File toLoad) {
        log.info("fileWillLoad: " + toLoad.getAbsolutePath());
        return true;
    }

    @Override
    public boolean fileWillSave(File toSave, String newContents, File destFile) {
        log.info("fileWillSave: " + toSave.getAbsolutePath() + " -> " + destFile.getAbsolutePath());
        return true;
    }

    @Override
    public void fileLoaded(File loaded, String loadedContents) {
        log.info("fileLoaded: " + loaded.getAbsolutePath() + ", contents length: " + loadedContents.length());
    }

    @Override
    public void fileSaved(File source, File dest) {
        log.info("fileSaved: " + source.getAbsolutePath() + " -> " + dest.getAbsolutePath());
    }

    @Override
    public String textWillEncrypt(String textToEncrypt) {
        log.info("textWillEncrypt: text length: " + textToEncrypt.length());
        return null;
    }

    @Override
    public String textWillDecrypt(String textToDecrypt) {
        log.info("textWillDecrypt: text length: " + textToDecrypt.length());
        return null;
    }

    public JComponent getExtraComponent(ExtraComponentPosition position) {
        JPanel dummyPanel = new JPanel();
        dummyPanel.setBackground(Color.PINK);
        dummyPanel.setName("Test " + position.toString());
        return dummyPanel;
    }

    /**
     * Generates a diagnostic log dump. Can be invoked from our menu or from our keystroke property.
     */
    private void logDump() {
        log.info("======== TestExtension log dump begins ========");
        int cacheSize = MainWindow.getInstance().getEditorTabPane().getTextManager().size();
        int editorTabs = MainWindow.getInstance().getEditorTabPane().getEditorTabs().size();
        int totalTabs = MainWindow.getInstance().getEditorTabPane().getTabCount();
        log.info("Cache size: " + cacheSize);
        log.info("There are " + editorTabs + " editor tab instances open.");
        log.info("There are " + totalTabs + " total tabs open.");
        log.info("Memory stats: " + getMemoryStats());
        log.info("======== TestExtension log dump ends ========");
    }

    /**
     * This was copied from swing-extras AboutPanel component because it's private there.
     * This is handy enough that it should probably be promoted to a general utility somewhere...
     */
    private String getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long memoryUsed = totalMemory - freeMemory;
        int memoryUsagePercent = (int)(((float)memoryUsed / totalMemory) * 100);
        return "Using " + FileSystemUtil.getPrintableSize(memoryUsed) + " of "
                + FileSystemUtil.getPrintableSize(totalMemory) + " (" + memoryUsagePercent + "%), "
                + FileSystemUtil.getPrintableSize(maxMemory) + " available";
    }


    private class LogDumpAction extends EnhancedAction {
        public LogDumpAction() {
            super("Log dump");
            setTooltip("Generates a diagnostic log dump");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            logDump();
        }
    }
}
