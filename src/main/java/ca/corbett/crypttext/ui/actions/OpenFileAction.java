package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.extras.EnhancedAction;

import javax.swing.JFileChooser;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Browses for a file to open and loads it in a new editor tab.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class OpenFileAction extends EnhancedAction {

    public OpenFileAction() {
        // No icon for now, maybe later
        super("Open file...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = MainWindow.getInstance().getFileChooser();
        int result = fileChooser.showDialog(MainWindow.getInstance(), "Open");
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            AppConfig.getInstance().setLastBrowseDirectory(selectedFile.getParentFile());
            MainWindow.getInstance().getEditorTabPane().newTextTab(selectedFile);
        }
    }
}
