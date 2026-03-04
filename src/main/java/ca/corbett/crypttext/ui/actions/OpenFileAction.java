package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.crypttext.text.Text;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.crypttext.ui.TextFileFilter;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;

import javax.swing.JFileChooser;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Browses for a file to open and loads it in a new editor tab.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class OpenFileAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(OpenFileAction.class.getName());
    private MessageUtil messageUtil;

    public OpenFileAction() {
        // No icon for now, maybe later
        super("Open file...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(AppConfig.getInstance().getLastBrowseDirectory());
        fileChooser.setFileFilter(TextFileFilter.DEFAULT);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showDialog(MainWindow.getInstance(), "Open");
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            AppConfig.getInstance().setLastBrowseDirectory(selectedFile.getParentFile());
            try {
                Text text = MainWindow.getInstance().getTextManager().fromFile(selectedFile);
                MainWindow.getInstance().getEditorTabPane().clearIfScratch();
                MainWindow.getInstance().getEditorTabPane().newTextTab(text, selectedFile.getName());
            }
            catch (IOException ioe) {
                getMessageUtil().error("Error opening file: " + ioe.getMessage(), ioe);
            }
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
