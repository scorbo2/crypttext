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
    private static final String LOCK_ICON_PATH = IMAGE_PATH + "icon_lock.png";
    private static final String UNLOCK_ICON_PATH = IMAGE_PATH + "icon_unlock.png";
    private static final String CLOSE_CLEAN_ICON_PATH = IMAGE_PATH + "icon_close_clean.png";
    private static final String CLOSE_DIRTY_ICON_PATH = IMAGE_PATH + "icon_close_dirty.png";

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

    /**
     * Returns an icon representing a locked state, suitable for display in the UI when a file is encrypted.
     */
    public static ImageIcon getLockIcon(int size) {
        return getIcon(PREFIX + LOCK_ICON_PATH, size);
    }

    /**
     * Returns an icon representing an unlocked state, suitable for display in the UI when a file is decrypted.
     */
    public static ImageIcon getUnlockIcon(int size) {
        return getIcon(PREFIX + UNLOCK_ICON_PATH, size);
    }

    /**
     * Returns an icon representing a "close" action for something that has no unsaved changes.
     */
    public static ImageIcon getCloseCleanIcon(int size) {
        return getIcon(PREFIX + CLOSE_CLEAN_ICON_PATH, size);
    }

    /**
     * Returns an icon representing a "close" action for something that has unsaved changes.
     */
    public static ImageIcon getCloseDirtyIcon(int size) {
        return getIcon(PREFIX + CLOSE_DIRTY_ICON_PATH, size);
    }
}
