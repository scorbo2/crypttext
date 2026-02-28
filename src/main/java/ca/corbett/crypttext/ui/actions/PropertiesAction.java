package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.ui.MainWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An action to show the PropertiesDialog for the application.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class PropertiesAction extends AbstractAction {

    public PropertiesAction() {
        super("Application settings...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (AppConfig.getInstance().showPropertiesDialog(MainWindow.getInstance())) {
            // User OK'd the dialog, reload the UI:
            UIReloadAction.getInstance().actionPerformed(null);
        }
    }
}
