package ca.corbett.crypttext.text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * This class acts as a model object for some text to be viewed or edited in the UI.
 * Information related to the text, such as its associated file and encryption key,
 * are also stored here. Instances of this class are immutable.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class Text {

    private final String text;
    private final File sourceFile;

    public Text(String text, File sourceFile) {
        this.text = text;
        this.sourceFile = sourceFile;
    }

    public String getText() {
        return text;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public boolean isSameSourceFile(Text other) {
        if (other == null) {
            throw new IllegalArgumentException("Given Text instance cannot be null");
        }
        try {
            return Files.isSameFile(sourceFile.toPath(), other.sourceFile.toPath());
        }
        catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) { return false; }
        Text text1 = (Text)o;
        return Objects.equals(text, text1.text) && Objects.equals(sourceFile, text1.sourceFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, sourceFile);
    }
}
