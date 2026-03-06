package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.crypt.CryptUtil;
import ca.corbett.crypttext.crypt.DefaultCryptMetadata;
import ca.corbett.crypttext.extensions.CryptTextExtensionManager;
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
        Component c = MainWindow.getInstance().getEditorTabPane().getCurrentTab();
        if (!(c instanceof EditorTab editorTab)) {
            return;
        }

        if (CryptUtil.isCryptTextWrapped(editorTab.getCurrentText())) {
            log.info("Decrypt: decrypting " + editorTab.getTextInstance().getSourceFile().getAbsolutePath());
            handleDecrypt(editorTab);
        }
        else {
            log.info("Encrypt: encrypting " + editorTab.getTextInstance().getSourceFile().getAbsolutePath());
            handleEncrypt(editorTab);
        }
    }

    private void handleDecrypt(EditorTab editorTab) {
        final String toDecrypt = editorTab.getCurrentText();
        CryptTextExtensionManager extManager = CryptTextExtensionManager.getInstance();

        // First, give extensions a chance to handle the decryption:
        String decrypted = extManager.textWillDecrypt(toDecrypt, editorTab.getCryptMetadata());
        if (decrypted != null) {
            editorTab.setCurrentText(decrypted);
            return;
        }

        // If no one answered, then make sure the Text instance has a DefaultCryptMetadata:
        if (!(editorTab.getCryptMetadata() instanceof DefaultCryptMetadata cryptMetadata)) {
            log.warning("Unknown CryptMetadata type: " + editorTab.getCryptMetadata().getClass().getName());
            getMessageUtil().error("Unknown encryption scheme",
                                   "This text was encrypted by an extension that is not available." +
                                           "\nUnable to decrypt.");
            return;
        }

        // If the password is not already set, prompt the user for it:
        if (cryptMetadata.getPassword() == null || cryptMetadata.getPassword().isEmpty()) {
            String password = getMessageUtil().askText("Enter password:", "");
            if (password == null) {
                // User canceled the prompt, so just skip it.
                return;
            }

            cryptMetadata.setPassword(password);
        }

        try {
            // Use the current text in the tab, NOT the text from the Text instance!
            // User could have copy+pasted encrypted text into the tab since it was loaded.
            decrypted = CryptUtil.unwrapAndDecrypt(cryptMetadata.getPassword(), toDecrypt);
            editorTab.setCurrentText(decrypted); // tab is now dirty!
            editorTab.save(); // Force an immediate save.
        }
        catch (Exception ex) {
            // Decryption failed - probably wrong password or corrupted text.
            // Just show an error message and leave the text as-is.
            getMessageUtil().error("Decryption failed: " + ex.getMessage(), ex);
        }
    }

    private void handleEncrypt(EditorTab editorTab) {
        final String toEncrypt = editorTab.getCurrentText();
        CryptTextExtensionManager extManager = CryptTextExtensionManager.getInstance();

        // First, give extensions a chance to handle the encryption:
        String encrypted = extManager.textWillEncrypt(toEncrypt, editorTab.getCryptMetadata());
        if (encrypted != null) {
            editorTab.setCurrentText(encrypted);
            return;
        }

        // If no one answered, then check to see if we have a DefaultCryptMetadata already:
        DefaultCryptMetadata cryptMetadata;
        if (editorTab.getCryptMetadata() instanceof DefaultCryptMetadata existingCryptMetadata) {
            cryptMetadata = existingCryptMetadata;
        }

        // Otherwise, we can just create a new one.
        else {
            // Note that we overwrite whatever CryptMetadata was there before... it could be
            // that this text was originally encrypted by an extension that is no longer available.
            // That's not an error condition. We'll just switch it to use our built-in scheme.
            cryptMetadata = new DefaultCryptMetadata(CryptUtil.isCryptTextWrapped(toEncrypt));
            editorTab.setCryptMetadata(cryptMetadata);
        }

        // Prompt for a password if we don't already have one.
        String password = cryptMetadata.getPassword();
        if (password == null || password.isEmpty()) {
            password = getMessageUtil().askText("Enter password:", "");
            if (password == null) {
                // User canceled the prompt, so just skip it.
                // We do this after we replace the CryptMetadata, so that we have at
                // least overwritten the old, stale one.
                return;
            }
            cryptMetadata.setPassword(password);
        }

        // Now we're good to go:
        try {
            encrypted = CryptUtil.encryptAndWrap(cryptMetadata.getPassword(), toEncrypt);
            editorTab.setCurrentText(encrypted); // tab is now dirty!
            editorTab.save(); // Force an immediate save.
        }
        catch (Exception ex) {
            // Encryption failed - probably some unexpected error with the text or metadata.
            // Just show an error message and leave the text as-is.
            getMessageUtil().error("Encryption failed: " + ex.getMessage(), ex);
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
