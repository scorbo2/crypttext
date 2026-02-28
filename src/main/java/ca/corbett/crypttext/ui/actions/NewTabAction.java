package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.extras.EnhancedAction;

import java.awt.event.ActionEvent;

/**
 * This action adds a new, untitled editor tab in the main window's editor tab pane.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class NewTabAction extends EnhancedAction {
    public NewTabAction() {
        super("New");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainWindow.getInstance().newTab();
    }
}
