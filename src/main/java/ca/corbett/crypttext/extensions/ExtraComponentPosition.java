package ca.corbett.crypttext.extensions;

/**
 * Extensions can optionally supply "extra" components (typically panels) to be placed
 * around the main text area in the MainWindow. This enum defines the possible positions
 * for these extra components.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public enum ExtraComponentPosition {
    /**
     * The extra component will be added to the left of the main text area.
     */
    LEFT,

    /**
     * The extra component will be added to the right of the main text area.
     */
    RIGHT,

    /**
     * The extra component will be added above the main text area.
     */
    TOP,

    /**
     * The extra component will be added below the main text area.
     */
    BOTTOM
}
