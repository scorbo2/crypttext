package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.crypt.CryptMetadata;
import ca.corbett.crypttext.crypt.DefaultCryptMetadata;
import ca.corbett.crypttext.ui.EditorTab;
import ca.corbett.crypttext.ui.MainWindow;
import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.MessageUtil;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * An action that can be used to "forget" the password for the current tab. This will not actually
 * change the text in any way, but it will disassociate the password from the current text instance,
 * so that the user will have to re-enter the password to encrypt/decrypt again.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class ForgetPasswordAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(ForgetPasswordAction.class.getName());
    private MessageUtil messageUtil;

    public ForgetPasswordAction() {
        super("Forget password for current tab");
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

        CryptMetadata cryptMetadata = editorTab.getCryptMetadata();
        if (cryptMetadata instanceof DefaultCryptMetadata defaultCryptMetadata) {
            defaultCryptMetadata.setPassword(null);
            getMessageUtil().info("Password forgotten", "The password for the current tab has been forgotten.");
        }
        else {
            // This is a very weird message to show to a user, but it is what it is:
            String className = cryptMetadata.getClass().getName();
            String msg = "The password for this tab is managed by an application extension." +
                    "\n\nScheme: " + className +
                    "\n\nTo forget this password, check the facilities provided by the extension that manages it," +
                    "\nor close the tab and re-open it.";
            getMessageUtil().info(msg);
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(MainWindow.getInstance(), log);
        }
        return messageUtil;
    }
}
