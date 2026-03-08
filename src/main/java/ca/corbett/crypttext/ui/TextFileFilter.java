package ca.corbett.crypttext.ui;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A configurable FileFilter implementation that accepts files with certain extensions.
 * You can set an optional description for the filter with setDescription().
 * <p>
 *     Side note: how bizarre is it that there's a javax.swing.filechooser.FileFilter and a java.io.FileFilter,
 *     and they are completely separate interfaces with no common parent?
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TextFileFilter extends FileFilter implements java.io.FileFilter {

    public static final TextFileFilter DEFAULT = new TextFileFilter(List.of("txt"));

    private final List<String> extensions;
    private String description = "Text files";

    /**
     * Creates a new TextFileFilter that accepts files with the given extensions.
     * A null or empty list means "accept all files".
     * <p>
     * The input list should list the extensions WITHOUT the leading dot.
     * For example, "txt" instead of ".txt". The filter will handle the dot internally.
     * Case sensitivity is deliberately removed from checks, so "txt" and "TXT" will be treated the same.
     * </p>
     */
    public TextFileFilter(List<String> extensions) {
        if (extensions == null) {
            extensions = List.of();
        }
        this.extensions = new ArrayList<>(extensions);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean accept(File f) {
        boolean anyMatch = extensions.isEmpty();
        String fName = f.getName().toLowerCase();
        for (String ext : extensions) {
            if (fName.endsWith("." + ext.toLowerCase())) {
                anyMatch = true;
                break;
            }
        }
        return f.isDirectory() || anyMatch;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
