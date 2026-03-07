package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.VetoException;
import ca.corbett.crypttext.ui.EditorTab;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * Saves the currently active editor tab.
 * If no tabs are open, this does nothing.
 * If the current tab is a scratch tab (that is, created in memory and not yet saved), this is
 * equivalent to SaveAsAction.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class SaveAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(SaveAction.class.getName());
    private MessageUtil messageUtil;

    public SaveAction() {
        // No icon for now, maybe later
        super("Save");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // It's weird that our editor tab pane allows tabs that aren't EditorTab instances.
        // Let's just filter them out. Might add code later to enforce EditorTabs only.
        Component c = MainWindow.getInstance().getEditorTabPane().getCurrentTab();
        if (!(c instanceof EditorTab editorTab)) {
            return;
        }

        // Now we can delegate to the tab itself:
        try {
            editorTab.save();

            // if it was a scratch file, it now has a proper name that we can show in the title bar:
            MainWindow.getInstance().updateTitleBar();
        }
        catch (VetoException ignored) {
            // An extension vetoed the save!
            // Just skip it - EditorTab has already logged the veto (indirectly via TextManager).
        }
        catch (Exception ioe) {
            getMessageUtil().error("Error saving file: " + ioe.getMessage(), ioe);
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
