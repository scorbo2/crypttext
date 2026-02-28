package ca.corbett.crypttext.ui.actions;

import ca.corbett.extras.logging.LogConsole;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An action that opens the log console window.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class LogConsoleAction extends AbstractAction {

    public LogConsoleAction() {
        super("Show Log Console");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LogConsole.getInstance().setVisible(true);
    }
}
