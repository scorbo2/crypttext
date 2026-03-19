package ca.corbett.crypttext.extensions.builtin;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.crypt.CryptMetadata;
import ca.corbett.crypttext.crypt.EncryptedText;
import ca.corbett.crypttext.extensions.CryptTextExtension;
import ca.corbett.crypttext.extensions.ExtraComponentPosition;
import ca.corbett.crypttext.text.Text;
import ca.corbett.crypttext.ui.EditorTabPane;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.crypttext.ui.TabStateManager;
import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A special test extension that exercises every extension feature, for testing purposes.
 * This is a special extension! It is not enabled or displayed to the user by default at all.
 * To enable this extension, set the enableTestExtension property to any value when launching
 * the application. You can use a "-D" system property for this on the command line.
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
    public Text handleFileLoad(File toLoad) throws IOException {
        log.info("handleFileLoad: " + toLoad.getAbsolutePath());
        return null; // Returning null means "I don't want to handle this load"
    }

    @Override
    public File handleFileSave(Text toSave, String resolvedText, File destinationFile) throws IOException {
        log.info("handleFileSave: " + toSave.getSourceFile() + " -> " + destinationFile);
        return null; // Returning null means "I don't want to handle this save"
    }

    @Override
    public void fileLoaded(Text loadedContent) {
        log.info("fileLoaded: " + loadedContent.getSourceFile().getAbsolutePath()
                         + ", contents length: "
                         + loadedContent.getSourceFile().length());
    }

    @Override
    public void fileSaved(Text text, File destFile) {
        log.info("fileSaved: " + text.getSourceFile().getAbsolutePath() + " -> " + destFile.getAbsolutePath());
    }

    @Override
    public CryptMetadata generateCryptMetadata(String rawText) {
        return null; // This extension does not supply a custom encryption scheme
    }

    @Override
    public EncryptedText textWillEncrypt(String textToEncrypt, CryptMetadata metadata) {
        log.info("textWillEncrypt: text length: " + textToEncrypt.length());
        return null; // This extension does not supply a custom encryption scheme
    }

    @Override
    public String textWillDecrypt(EncryptedText encryptedText) {
        log.info("textWillDecrypt: text length: " + encryptedText.getText().length());
        return null; // This extension does not supply a custom encryption scheme
    }

    @Override
    public JComponent getExtraComponent(ExtraComponentPosition position) {
        JPanel dummyPanel = new JPanel();
        dummyPanel.setBackground(Color.PINK);
        dummyPanel.setName("Test " + position.toString());
        return dummyPanel;
    }

    @Override
    public TabStateManager getTabStateManager() {
        return new TestTabStateManager();
    }

    /**
     * Quick shorthand method to look up the given boolean property and return its current value.
     * You'll get false if the named property is not found.
     */
    private boolean getBooleanProp(String propName) {
        AbstractProperty prop = AppConfig.getInstance().getPropertiesManager().getProperty(propName);
        if (prop instanceof BooleanProperty) {
            return ((BooleanProperty)prop).getValue();
        }
        else {
            log.warning("Expected boolean property for name: " + propName + ", but got: " + prop);
            return false;
        }
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


    /**
     * A very simple action to invoke our logDump() method.
     */
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

    /**
     * A very simple TabStateManager implementation that just logs stuff, for testing purposes.
     */
    private static class TestTabStateManager implements TabStateManager {

        private static final Logger log = Logger.getLogger(TestTabStateManager.class.getName());

        @Override
        public void saveTabState(EditorTabPane editorTabPane) {
            log.info("saveTabState: " + editorTabPane.getTabCount() + " tabs would be saved.");
        }

        @Override
        public void restoreTabState(EditorTabPane editorTabPane) {
            log.info("restoreTabState: would restore tab state into editorTabPane with "
                             + editorTabPane.getTabCount()
                             + " existing tabs");
        }
    }
}