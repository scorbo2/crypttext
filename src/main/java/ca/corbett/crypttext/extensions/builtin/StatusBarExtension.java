package ca.corbett.crypttext.extensions.builtin;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.extensions.CryptTextExtension;
import ca.corbett.crypttext.extensions.ExtraComponentPosition;
import ca.corbett.crypttext.ui.EditorTab;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.crypttext.ui.UIReloadable;
import ca.corbett.crypttext.ui.actions.UIReloadAction;
import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.io.FileSystemUtil;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.FontProperty;
import ca.corbett.extras.properties.PropertiesManager;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A simple built-in extension for CryptText that supplies a status bar at the bottom
 * of the editor tab pane. The status bar will display information about the currently
 * selected editor tab, such as the file name, size, date, and basic metadata
 * about the encryption used (if any).
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class StatusBarExtension extends CryptTextExtension implements ChangeListener, UIReloadable {

    private final static String PREFIX = "Status bar.Options.";
    private final static String FONT_PROP = PREFIX+"font";
    private final static String PATH_VISIBLE_PROP = PREFIX+"pathLabelVisible";
    private final static String DATE_VISIBLE_PROP = PREFIX + "dateLabelVisible";
    private final static String DISK_SIZE_VISIBLE_PROP = PREFIX+"sizeOnDiskLabelVisible";
    private final static String MEM_SIZE_VISIBLE_PROP = PREFIX+"sizeInMemoryLabelVisible";
    private final static String CRYPT_VISIBLE_PROP = PREFIX+"cryptLabelVisible";
    private final static Font DEFAULT_FONT = new Font(Font.DIALOG, Font.PLAIN, 12);

    private final AppExtensionInfo extInfo;
    private final StatusBarComponent statusBar;

    public StatusBarExtension() {
        extInfo = new AppExtensionInfo.Builder("Status bar")
                .setVersion(Version.VERSION)
                .setTargetAppName(Version.NAME)
                .setTargetAppVersion(Version.VERSION)
                .setShortDescription("Adds a status bar to the editor")
                .setLongDescription("Adds a simple status bar to the bottom of the editor tabs, " +
                                            "showing file info and encryption metadata.")
                .build();
        statusBar = new StatusBarComponent();
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    public void onActivate() {
        // Listen for changes to the current tab:
        MainWindow.getInstance().getEditorTabPane().addChangeListener(this);

        // Listen for UI updates, so we can update our cosmetic properties:
        UIReloadAction.getInstance().registerReloadable(this);

        // Force an initial update to pick up our property values now:
        reloadUI();

        // Force an initial update to pick up the current selected tab now:
        stateChanged(new ChangeEvent(MainWindow.getInstance().getEditorTabPane()));
    }

    @Override
    public void onDeactivate() {
        // Stop listening for events:
        MainWindow.getInstance().getEditorTabPane().removeChangeListener(this);
        UIReloadAction.getInstance().unregisterReloadable(this);
        statusBar.setCurrentTab(null); // clear any listeners our status bar has, if any
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();
        props.add(new FontProperty(FONT_PROP, "Font:", DEFAULT_FONT));
        props.add(new BooleanProperty(PATH_VISIBLE_PROP, "Show file path", true));
        props.add(new BooleanProperty(DATE_VISIBLE_PROP, "Show last modified date", true));
        props.add(new BooleanProperty(DISK_SIZE_VISIBLE_PROP, "Show file size on disk", true));
        props.add(new BooleanProperty(MEM_SIZE_VISIBLE_PROP, "Show text statistics", true));
        props.add(new BooleanProperty(CRYPT_VISIBLE_PROP, "Show encryption metadata", true));

        return props;
    }

    @Override
    protected void loadJarResources() {
        // Nothing to load here
    }

    @Override
    public JComponent getExtraComponent(ExtraComponentPosition position) {
        if (position == ExtraComponentPosition.BOTTOM) {
            return statusBar;
        }

        return null;
    }

    /**
     * Invoked from the MainWindow's tab pane whenever the selected tab changes.
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        statusBar.setCurrentTab(MainWindow.getInstance().getEditorTabPane().getSelectedComponent());
    }

    /**
     * Invoked when the user has modified application settings and the UI must reload.
     * We query for our cosmetic properties and update accordingly.
     */
    @Override
    public void reloadUI() {
        final PropertiesManager propsManager = AppConfig.getInstance().getPropertiesManager();
        if (propsManager.getProperty(FONT_PROP) instanceof FontProperty fontProp) {
            statusBar.setLabelFont(fontProp.getFont());
        }
        if (propsManager.getProperty(PATH_VISIBLE_PROP) instanceof BooleanProperty booleanProp) {
            statusBar.setPathLabelVisible(booleanProp.getValue());
        }
        if (propsManager.getProperty(DATE_VISIBLE_PROP) instanceof BooleanProperty booleanProp1) {
            statusBar.setDateLabelVisible(booleanProp1.getValue());
        }
        if (propsManager.getProperty(DISK_SIZE_VISIBLE_PROP) instanceof BooleanProperty booleanProp2) {
            statusBar.setSizeOnDiskLabelVisible(booleanProp2.getValue());
        }
        if (propsManager.getProperty(MEM_SIZE_VISIBLE_PROP) instanceof BooleanProperty booleanProp3) {
            statusBar.setSizeInMemoryLabelVisible(booleanProp3.getValue());
        }
        if (propsManager.getProperty(CRYPT_VISIBLE_PROP) instanceof BooleanProperty booleanProp4) {
            statusBar.setCryptLabelVisible(booleanProp4.getValue());
        }
    }

    /**
     * Our actual status bar component.
     */
    private static class StatusBarComponent extends JPanel
            implements EditorTab.PositionListener, EditorTab.ContentChangeListener {
        private static final String DEFAULT_POS = "Ln 1, Col 1";
        private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        private final JLabel pathLabel;
        private final JLabel dateLabel;
        private final JLabel sizeOnDiskLabel;
        private final JLabel sizeInMemoryLabel;
        private final JLabel cryptLabel;
        private final JLabel caretPositionLabel;
        private EditorTab currentTab = null;

        /**
         * Creates a new StatusBarComponent. All labels are visible by default, and all will have no
         * content until we receive a call to setLabels with a non-null EditorTab.
         */
        public StatusBarComponent() {
            setLayout(new BorderLayout());
            setName("Status bar"); // In case we are added to a JTabbedPane

            // Caret position info on the right:
            JPanel wrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            wrapperPanel.setBorder(BorderFactory.createLoweredBevelBorder());
            caretPositionLabel = new JLabel(DEFAULT_POS);
            wrapperPanel.add(caretPositionLabel);
            add(wrapperPanel, BorderLayout.EAST);

            // Everything else stretched to the left:
            wrapperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
            wrapperPanel.setBorder(BorderFactory.createLoweredBevelBorder());
            pathLabel = new JLabel();
            dateLabel = new JLabel();
            sizeOnDiskLabel = new JLabel();
            sizeInMemoryLabel = new JLabel();
            cryptLabel = new JLabel();
            wrapperPanel.add(pathLabel);
            wrapperPanel.add(dateLabel);
            wrapperPanel.add(sizeOnDiskLabel);
            wrapperPanel.add(sizeInMemoryLabel);
            wrapperPanel.add(cryptLabel);
            add(wrapperPanel, BorderLayout.CENTER);

            // Set defaults:
            setLabelFont(DEFAULT_FONT);
            setLabels(null);
        }

        /**
         * Sets all labels to the given Font. If the given Font is null,
         * we will revert to the default font.
         */
        public void setLabelFont(Font newFont) {
            if (newFont == null) {
                newFont = DEFAULT_FONT;
            }
            pathLabel.setFont(newFont);
            dateLabel.setFont(newFont);
            sizeOnDiskLabel.setFont(newFont);
            sizeInMemoryLabel.setFont(newFont);
            cryptLabel.setFont(newFont);
            caretPositionLabel.setFont(newFont);
            refresh();
        }

        /**
         * Updates our label content based on the given EditorTab.
         * Passing null is perfectly valid here, it means "no tab is currently selected".
         * In that case, we'll blank our our labels.
         */
        public void setLabels(EditorTab tab) {
            if (tab == null) {
                pathLabel.setText("(No file)");
                dateLabel.setText("");
                sizeOnDiskLabel.setText("");
                sizeInMemoryLabel.setText("");
                cryptLabel.setText("");
            }
            else {
                // Show file stats, if the file exists and isn't a scratch file:
                File sourceFile = tab.getTextInstance().getSourceFile();
                if (sourceFile == null || !sourceFile.exists() || tab.isScratchFile()) {
                    pathLabel.setText("(No file)"); // this is a lie, but scratch files don't count as actual files
                    sizeOnDiskLabel.setText("");
                    dateLabel.setText("");
                }
                else {
                    pathLabel.setText(sourceFile.getAbsolutePath());
                    String fileSize = FileSystemUtil.getPrintableSize(sourceFile.length());
                    sizeOnDiskLabel.setText(String.format("Disk: %s", fileSize));
                    dateLabel.setText(format.format(sourceFile.lastModified()));
                }

                updateTextStatsLabel(tab.getCurrentText());

                // We'll show the encryption scheme name if the text was loaded from an
                // encrypted file, or if it is currently encrypted in memory:
                if (tab.isEncrypted() || tab.getCryptMetadata().wasEncryptedWhenLoaded()) {
                    String className = tab.getCryptMetadata().getClass().getSimpleName();
                    cryptLabel.setText("Scheme: "+className);
                }
                else {
                    // The text is not encrypted, so we'll just leave this blank:
                    cryptLabel.setText("");
                }
            }

            refresh();
        }

        private void refresh() {
            invalidate();
            revalidate();
            repaint();
        }

        private void updateTextStatsLabel(String text) {
            // Show character count and word count:
            int characters = text.length();
            int words = new StringTokenizer(text).countTokens();
            sizeInMemoryLabel.setText(String.format("Chars/Words: %d/%d", characters, words));
            sizeInMemoryLabel.repaint();
        }

        public void setPathLabelVisible(boolean isVisible) {
            pathLabel.setVisible(isVisible);
            refresh();
        }

        public void setDateLabelVisible(boolean isVisible) {
            dateLabel.setVisible(isVisible);
            refresh();
        }

        public void setSizeOnDiskLabelVisible(boolean isVisible) {
            sizeOnDiskLabel.setVisible(isVisible);
            refresh();
        }

        public void setSizeInMemoryLabelVisible(boolean isVisible) {
            sizeInMemoryLabel.setVisible(isVisible);
            refresh();
        }

        public void setCryptLabelVisible(boolean isVisible) {
            cryptLabel.setVisible(isVisible);
            refresh();
        }

        /**
         * Updates our current tab reference to the given Component. If the given Component is an EditorTab,
         * we will listen for caret position updates from it. If the given Component is not an
         * EditorTab (or is null), we will stop listening for caret position updates and blank our labels.
         */
        public void setCurrentTab(Component c) {
            // We only ever want to listen to at most one EditorTab at a time:
            if (currentTab != null) {
                currentTab.removePositionListener(this);
                currentTab.removeContentChangeListener(this);
            }

            // It's possible that the tab pane might contain Components that are not EditorTabs.
            // We can just ignore those.
            if (c instanceof EditorTab editorTab) {
                currentTab = editorTab;
                currentTab.addPositionListener(this);
                currentTab.addContentChangeListener(this);
            }
            else {
                currentTab = null;
            }

            // Update all labels based on the new tab (or lack thereof, it might be null here):
            setLabels(currentTab);

            // Get the caret position info updated immediately, since we won't get an onPositionUpdate call until the user moves the caret:
            if (currentTab != null) {
                onPositionUpdate(currentTab.getCaretRow(), currentTab.getCaretColumn());
            }
            else {
                caretPositionLabel.setText(DEFAULT_POS);
            }
        }

        /**
         * Invoked from whatever EditorTab we are currently watching, when the caret position changes.
         * The given row and column are 1-based indices of the caret position in the document.
         */
        @Override
        public void onPositionUpdate(int row, int column) {
            caretPositionLabel.setText(String.format("Ln %d, Col %d", row, column));
            caretPositionLabel.repaint(); // Don't refresh() on every caret update (too expensive)
        }

        /**
         * Invoked when the content of the currently selected EditorTab changes.
         * We need to update the label showing word and character count when this happens.
         */
        @Override
        public void onContentChange(String newContent) {
            updateTextStatsLabel(newContent);
        }
    }
}
