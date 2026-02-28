package ca.corbett.crypttext;

import ca.corbett.extras.ResourceLoader;

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

    // For our example image resources:
    private static final String IMAGE_PATH = "example-images/";

    // TODO: Define your resource paths here
    //       Here is an example swing-extras logo path:
    private static final String SE_LOGO_PATH = IMAGE_PATH + "swing-extras-icon.jpg";

    private CryptTextResourceLoader() {
    }

    /**
     * We can add convenient wrapper methods here, so that callers don't
     * have to know or care about the resource paths. The alternative
     * is that callers can directly call our parent class's static methods,
     * like this, to retrieve specific resources:
     * <pre>
     *     logoImage = ResourceLoader.getImage("example-images/swing-extras-icon.jpg");
     *
     *     // Or, to scale to 32x32 and retrieve as an IconImage instead:
     *     logoIcon = ResourceLoader.getIcon("example-images/swing-extras-icon.jpg", 32);
     * </pre>
     * <p>
     * The benefit of adding wrapper methods here is that it makes the code more readable.
     * We could also build a basic caching mechanism here, to avoid repeated loads.
     * Here is an example wrapper method to retrieve the swing-extras logo image.
     * </p>
     * <p>
     * TODO add your own wrapper methods as needed!
     * </p>
     */
    public static BufferedImage getSwingExtrasIcon() {
        return getImage(SE_LOGO_PATH);
    }
}
