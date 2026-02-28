package ca.corbett.crypttext;

import ca.corbett.extras.ResourceLoader;

import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

/**
 * Provides static convenience methods for loading application-specific
 * resources, such as images, used in the UI.
 * <p>
 * The parent class is worth exploring, as it exposes several static
 * utility methods that are very handy for easily loading resources
 * of various types!
 * </p>
 */
public class CryptTextResourceLoader extends ResourceLoader {

    // For most resources:
    private static final String PREFIX = "ca/corbett/crypttext/";

    // For image resources:
    private static final String IMAGE_PATH = "images/";

    private static final String SQUARE_ICON_PATH = IMAGE_PATH + "logo.png";
    private static final String WIDE_LOGO_PATH = IMAGE_PATH + "logo_wide.jpg";

    private CryptTextResourceLoader() {
    }

    /**
     * Returns an ImageIcon for a small, square logo, suitable for window icon use.
     */
    public static ImageIcon getSquareIcon() {
        return new ImageIcon(getSquareLogo());
    }

    /**
     * Returns a BufferedImage for a small, square logo, suitable for window icon use.
     */
    public static BufferedImage getSquareLogo() {
        return getImage(PREFIX + SQUARE_ICON_PATH);
    }

    /**
     * Returns a BufferedImage for a wide logo, suitable for display in the about dialog header.
     */
    public static BufferedImage getWideLogo() {
        return getImage(PREFIX + WIDE_LOGO_PATH);
    }
}
