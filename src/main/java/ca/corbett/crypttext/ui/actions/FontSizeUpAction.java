package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.extras.EnhancedAction;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * A simple action to increase the editor font size by 2 points,
 * then trigger an immediate preferences save and UI reload.
 * Changes will take effect immediately, and will persist across application restarts.
 * There is no upper bounds checking! Don't go crazy with it.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class FontSizeUpAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(FontSizeUpAction.class.getName());

    public FontSizeUpAction() {
        super("Increase editor font size");
        setTooltip("Increase the font size used in the editor by 2 points.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int currentSize = AppConfig.getInstance().getEditorFontSize();
        int newSize = currentSize + 2;
        log.info("Increasing editor font size from " + currentSize + " to " + newSize);
        AppConfig.getInstance().setEditorFontSize(newSize); // triggers an immediate preference save
        UIReloadAction.getInstance().actionPerformed(null); // force a reload of the UI
    }
}
