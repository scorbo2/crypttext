package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.DecryptionFailedException;
import ca.corbett.crypttext.ui.EditorTab;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * Saves the contents of the current editor tab without encryption.
 * If the contents of the tab were not encrypted, this is equivalent to SaveAsAction.
 * Otherwise, this will discard the encryption metadata associated with the
 * EditorTab, and force the save to happen without encryption.
 * The user is prompted to confirm the action.
 * If the contents of the EditorTab are currently encrypted, and no
 * password has yet been provided for the tab, the user will be prompted
 * for the password before proceeding. In any case, any password associated
 * with the tab will be immediately forgotten after the save.
 * <p>
 * Note that this is always implicitly a "save as" flow - we will never
 * silently overwrite an encrypted file with unencrypted content.
 * The user can browse back to the source file if they really want
 * to do that (there will be a "confirm overwrite" prompt in that case).
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class SaveUnencryptedAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(SaveUnencryptedAction.class.getName());
    private MessageUtil messageUtil;

    public SaveUnencryptedAction() {
        super("Save unencrypted");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // It's weird that our editor tab pane allows tabs that aren't EditorTab instances.
        // Let's just filter them out. Might add code later to enforce EditorTabs only.
        Component c = MainWindow.getInstance().getEditorTabPane().getCurrentTab();
        if (!(c instanceof EditorTab editorTab)) {
            return;
        }

        // Prompt to confirm if we're about to discard encryption:
        if (editorTab.isEncrypted() || editorTab.getCryptMetadata().wasEncryptedWhenLoaded()) {
            int answer = getMessageUtil().askYesNo("Really save unencrypted?",
                                                   "Are you sure you wish to discard encryption and save the file unencrypted?");
            if (answer != MessageUtil.YES) {
                return;
            }
        }

        // Now we can defer to the editor tab:
        try {
            editorTab.saveUnencrypted(); // will prompt for save destination and handle decryption if needed
        }
        catch (DecryptionFailedException ex) {
            // Decryption failed due to wrong password or corrupted text. Show a user-friendly message.
            getMessageUtil().warning(ex.getMessage()); // DON'T supply the exception, or it logs the whole stack trace.
        }
        catch (Exception ex) {
            // For all other exceptions, log the whole stack trace:
            getMessageUtil().error("Error saving file: " + ex.getMessage(), ex);
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
