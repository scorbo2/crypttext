package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;
import ca.corbett.crypttext.ui.MainWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An action that, when invoked, will open the ExtensionManagerDialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ExtensionManagerAction extends AbstractAction {

    public ExtensionManagerAction() {
        super("Extension Manager...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (AppConfig.getInstance().showExtensionDialog(MainWindow.getInstance(),
                CryptTextExtensionManager.getInstance().getUpdateManager())) {
            // Reload UI to reflect any changes in extensions:
            UIReloadAction.getInstance().actionPerformed(null);
        }
    }
}
