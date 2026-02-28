package ca.corbett.crypttext.ui;

/**
 * UI classes can implement this interface if they are capable
 * of reloading themselves when application preferences change,
 * or when extensions are loaded/unloaded. You can respond to that message
 * by redrawing whatever UI elements may have been changed.
 * <p>
 * This interface and the global UIReloadAction provide a very
 * clean way of handling UI reloads. The application code will invoke
 * the UIReloadAction automatically whenever it detects that a UI reload is needed,
 * and your UI components just need to implement the reloadUI() method to
 * respond appropriately.
 * </p>
 *
 * @author scorbo2
 * @since 2025-03-25
 */
public interface UIReloadable {

    /**
     * Invoked by UIReloadAction when it's time to reload the UI.
     * AppConfig should be queried for the latest state of all
     * user-configurable application settings, as they may have changed.
     */
    void reloadUI();
}
