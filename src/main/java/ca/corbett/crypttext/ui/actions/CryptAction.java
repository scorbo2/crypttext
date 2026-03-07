package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.DecryptionFailedException;
import ca.corbett.crypttext.crypt.CryptUtil;
import ca.corbett.crypttext.ui.EditorTab;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * Encrypts or decrypts the text in the current editor tab, depending on whether it is currently
 * encrypted or not. The user will be prompted for a password if one is not yet associated
 * with the current text. <b>The password is NEVER written to disk!</b> It exists only in memory,
 * at least with the application's built-in encryption scheme.
 * <p>
 * Extensions can supply their own encryption/decryption mechanism! We will poll
 * the extension manager here to enable that. If no extension handles the encryption/decryption,
 * then our default built-in scheme will be used.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class CryptAction extends EnhancedAction {
    private static final Logger log = Logger.getLogger(CryptAction.class.getName());
    private MessageUtil messageUtil;

    public CryptAction() {
        // No icon for now... maybe later
        super("Encrypt/decrypt");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // It's weird that our editor tab pane allows tabs that aren't EditorTab instances.
        // Let's just filter them out. Might add code later to enforce EditorTabs only.
        // This also covers the case where current tab is null.
        Component c = MainWindow.getInstance().getEditorTabPane().getCurrentTab();
        if (!(c instanceof EditorTab editorTab)) {
            getMessageUtil().info("No text-based tab is selected.");
            return;
        }

        if (CryptUtil.isCryptTextWrapped(editorTab.getMemoryContents())) {
            try {
                editorTab.decryptInMemory();
            }
            catch (DecryptionFailedException ex) {
                // Decryption failed due to wrong password or corrupted text. Show a user-friendly message.
                getMessageUtil().warning(
                        ex.getMessage()); // DON'T supply the exception, or it logs the whole stack trace.
            }
            catch (Exception ex) {
                // For all other exceptions, log the whole stack trace:
                getMessageUtil().error("Error decrypting text: " + ex.getMessage(), ex);
            }
        }
        else {
            try {
                editorTab.encryptInMemory();
            }
            catch (Exception ex) {
                getMessageUtil().error("Error encrypting text: " + ex.getMessage(), ex);
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
