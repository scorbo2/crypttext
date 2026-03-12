package ca.corbett.crypttext.ui.actions;

import ca.corbett.crypttext.AppConfig;
import ca.corbett.extras.EnhancedAction;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * A simple action to decrease the editor font size by 2 points,
 * then trigger an immediate preferences save and UI reload.
 * Changes will take effect immediately, and will persist across application restarts.
 * There is a lower bound of 2 points, to prevent non-positive font sizes.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class FontSizeDownAction extends EnhancedAction {

    private static final Logger log = Logger.getLogger(FontSizeDownAction.class.getName());

    public FontSizeDownAction() {
        super("Decrease editor font size");
        setTooltip("Decrease the font size used in the editor by 2 points.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int currentSize = AppConfig.getInstance().getEditorFontSize();
        int newSize = currentSize - 2;
        if (newSize <= 0) {
            log.warning("Cannot decrease editor font size to zero!");
            return; // don't allow non-positive font sizes
        }
        log.info("Decreasing editor font size from " + currentSize + " to " + newSize);
        AppConfig.getInstance().setEditorFontSize(newSize); // triggers an immediate preference save
        UIReloadAction.getInstance().actionPerformed(null); // force a reload of the UI
    }
}
