package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.ui.EditorTab;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.extras.EnhancedAction;

import java.awt.Component;
import java.awt.event.ActionEvent;

/**
 * An action that undoes the last edit in the current editor tab.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class UndoAction extends EnhancedAction {

    public UndoAction() {
        super("Undo");
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
        editorTab.undo();
    }
}
