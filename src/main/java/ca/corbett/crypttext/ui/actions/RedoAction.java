package ca.corbett.crypttext.ui.actions;

import ca.corbett.extras.EnhancedAction;

import java.awt.event.ActionEvent;

/**
 * An action that redoes the last undone edit in the current editor tab.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class RedoAction extends EnhancedAction {

    public RedoAction() {
        super("Redo");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // It's weird that our editor tab pane allows tabs that aren't EditorTab instances.
        // Let's just filter them out. Might add code later to enforce EditorTabs only.
        var c = ca.corbett.crypttext.ui.MainWindow.getInstance().getEditorTabPane().getCurrentTab();
        if (!(c instanceof ca.corbett.crypttext.ui.EditorTab editorTab)) {
            return;
        }

        // Now we can delegate to the tab itself:
        editorTab.redo();
    }
}
