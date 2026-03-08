package ca.corbett.crypttext.ui;

import java.awt.Color;

/**
 * Cheesy built-in color schemes for users who don't want
 * to rely on the Look and Feel colors.
 * <p>
 * This could be externalized, to avoid hard-coding, but eh... it's
 * fine for a neat little extra feature that might rarely actually get used.
 * Relying on the Look and Feel for this stuff is a better option in general,
 * and CryptText ships with all the extra Look and Feels that come
 * for free out of the box with swing-extras.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public enum ColorTheme {
    MATRIX("Matrix",
           new Color(0, 0, 0),   // Editor background
           new Color(0, 255, 0), // Editor foreground
           new Color(0, 50, 0),  // Gutter background
           new Color(0, 200, 0)  // Gutter foreground
    ),
    DARK("Dark",
         new Color(45, 45, 45),    // Editor background
         new Color(220, 220, 220), // Editor foreground
         new Color(60, 60, 60),    // Gutter background
         new Color(200, 200, 200)  // Gutter foreground
    ),
    VERY_DARK("Very dark",
              new Color(25, 25, 25),    // Editor background
              new Color(205, 205, 205), // Editor foreground
              new Color(40, 40, 40),    // Gutter background
              new Color(180, 180, 180)  // Gutter foreground
    ),
    SHADES_OF_GREY("Shades of grey",
                   new Color(165, 165, 165),    // Editor background
                   new Color(20, 20, 20), // Editor foreground
                   new Color(135, 135, 135),    // Gutter background
                   new Color(60, 60, 60)  // Gutter foreground
    ),
    GOT_THE_BLUES("Got the blues",
                  new Color(30, 30, 60),   // Editor background
                  new Color(200, 200, 255),// Editor foreground
                  new Color(20, 20, 50),   // Gutter background
                  new Color(150, 150, 255) // Gutter foreground
    ),
    HOT_DOG_STAND("Hot dog stand", // The obligatory joke option
                  new Color(60, 30, 30), // Editor background
                  Color.ORANGE,                   // Editor foreground
                  new Color(50, 20, 20), // Gutter background
                  Color.YELLOW                    // Gutter foreground
    );

    private final String label;
    private final Color editorBackground;
    private final Color editorForeground;
    private final Color gutterBackground;
    private final Color gutterForeground;

    ColorTheme(String label, Color editorBackground, Color editorForeground, Color gutterBackground, Color gutterForeground) {
        this.label = label;
        this.editorBackground = editorBackground;
        this.editorForeground = editorForeground;
        this.gutterBackground = gutterBackground;
        this.gutterForeground = gutterForeground;
    }

    @Override
    public String toString() {
        return label;
    }

    public Color getEditorBackground() {
        return editorBackground;
    }

    public Color getEditorForeground() {
        return editorForeground;
    }

    public Color getGutterBackground() {
        return gutterBackground;
    }

    public Color getGutterForeground() {
        return gutterForeground;
    }
}
