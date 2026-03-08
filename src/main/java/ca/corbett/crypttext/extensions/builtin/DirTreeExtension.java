package ca.corbett.crypttext.extensions.builtin;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.Version;
import ca.corbett.crypttext.extensions.CryptTextExtension;
import ca.corbett.crypttext.extensions.ExtraComponentPosition;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.crypttext.ui.TextFileFilter;
import ca.corbett.crypttext.ui.actions.UIReloadAction;
import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.dirtree.DirTree;
import ca.corbett.extras.dirtree.DirTreeAdapter;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.extras.io.TextFileDetector;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.KeyStrokeProperty;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A simple built-in extension that supplies a DirTree component to the
 * left of the editor. This can be used to visually navigate the file system
 * and open files by double-clicking them in the tree, instead of using the file chooser.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class DirTreeExtension extends CryptTextExtension {
    private static final Logger log = Logger.getLogger(DirTreeExtension.class.getName());

    private static final String SHOW_TREE_PROP = "UI.General.showTree";
    private static final String KEY_PROP = AppConfig.KEYSTROKE_PREFIX + "General.toggleKey";

    private final CustomTreeListener treeListener = new CustomTreeListener();
    private final LaFChangeListener lafChangeListener = new LaFChangeListener();
    private MessageUtil messageUtil;
    private final AppExtensionInfo extInfo;
    private DirTree dirTree;

    public DirTreeExtension() {
        this.extInfo = new AppExtensionInfo.Builder("DirTree")
                .setVersion(Version.VERSION)
                .setTargetAppName(Version.NAME)
                .setTargetAppVersion(Version.VERSION)
                .setShortDescription("Shows a directory tree for file navigation")
                .setLongDescription("Adds a DirTree component to the left of the editor " +
                                            "to visually browse the file system and open files by double-clicking them.")
                .build();

    }

    @Override
    public void onActivate() {
        dirTree = new DirTree(AppConfig.getInstance().getLastBrowseDirectory());
        dirTree.setName("DirTree"); // in case we get added to a JTabbedPane
        dirTree.setShowFiles(true);
        dirTree.setFileFilter(TextFileFilter.DEFAULT);
        dirTree.addDirTreeListener(treeListener);
        LookAndFeelManager.addChangeListener(lafChangeListener);
    }

    @Override
    public void onDeactivate() {
        LookAndFeelManager.removeChangeListener(lafChangeListener);
        dirTree.removeDirTreeListener(treeListener);
        dirTree = null;
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        List<AbstractProperty> props = new ArrayList<>();

        props.add(new BooleanProperty(SHOW_TREE_PROP, "Show directory tree", true));
        props.add(new KeyStrokeProperty(KEY_PROP, "Show/hide DirTree",
                                        KeyStrokeManager.parseKeyStroke("F4"),
                                        new ToggleTreeAction())
                          .setAllowBlank(true));

        return props;
    }

    @Override
    protected void loadJarResources() {
        // Nothing to load here
    }

    @Override
    public JComponent getExtraComponent(ExtraComponentPosition position) {
        if (position == ExtraComponentPosition.LEFT && isTreeEnabled()) {
            return dirTree;
        }

        return null;
    }

    /**
     * Looks up the current value of our config prop and returns it if found.
     */
    private boolean isTreeEnabled() {
        AbstractProperty prop = AppConfig.getInstance().getPropertiesManager().getProperty(SHOW_TREE_PROP);
        if (prop instanceof BooleanProperty boolProp) {
            return boolProp.getValue();
        }
        return false;
    }

    /**
     * Invoked via our keyboard shortcut to toggle the visibility of our DirTree.
     */
    private void toggleTreeEnabled() {
        AbstractProperty prop = AppConfig.getInstance().getPropertiesManager().getProperty(SHOW_TREE_PROP);
        if (prop instanceof BooleanProperty boolProp) {
            boolProp.setValue(!boolProp.getValue());
        }
        AppConfig.getInstance().save(); // force immediate save
        UIReloadAction.getInstance().actionPerformed(null); // force a UI reload to show/hide the tree immediately
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }

    /**
     * An internal class to listen for double-click events in our DirTree
     * and open the selected file in a new editor tab.
     */
    private class CustomTreeListener extends DirTreeAdapter {
        @Override
        public void fileDoubleClicked(DirTree source, File file) {
            // Quick sanity check:
            try {
                if (!TextFileDetector.isTextFile(file)) {
                    getMessageUtil().error("The selected file does not appear to be a text file: " + file.getName());
                    return;
                }
            }
            catch (IOException ioe) {
                getMessageUtil().error("Error accessing file: " + ioe.getMessage(), ioe);
                return;
            }

            MainWindow.getInstance().getEditorTabPane().newTextTab(file);
        }
    }

    /**
     * A simple action to toggle the visibility of our DirTree.
     */
    private class ToggleTreeAction extends EnhancedAction {
        public ToggleTreeAction() {
            super("Show/hide DirTree"); // never shown, so name doesn't really matter
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            toggleTreeEnabled();
        }
    }

    /**
     * Application startup loads and activates all extensions before setting the
     * configured Look and Feel. This means that our DirTree is created before
     * the LaF is set. So, we need to listen for LaF changes and update the UI
     * of our DirTree accordingly when that happens. After initial application
     * startup, this listener becomes redundant, since our LaF manager will
     * handle updating all existing windows, but we need this for app startup.
     */
    private class LaFChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (dirTree != null) {
                SwingUtilities.updateComponentTreeUI(dirTree);
            }
        }
    }
}
