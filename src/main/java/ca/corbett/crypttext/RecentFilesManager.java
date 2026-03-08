package ca.corbett.crypttext;

import ca.corbett.extras.io.FileSystemUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages a list of the most-recently-accessed files.
 * This can be configured in application settings.
 * <p>
 * Persistence is a simple flat text file in the application settings directory.
 * Unlike the TabStateManager, this behavior is not extensible.
 * Extensions can't supply their own RecentFilesManager implementation.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class RecentFilesManager {

    private static final Logger logger = Logger.getLogger(RecentFilesManager.class.getName());
    public static final File MRF_FILE = new File(Version.SETTINGS_DIR, "recent_files");
    public static final int DEFAULT_LIMIT = 5; // arbitrary default

    private int listLimit;
    private final List<File> recentFiles;

    /**
     * Creates a RecentFilesManager with an empty list of recent files, and a default list limit.
     */
    public RecentFilesManager() {
        this.recentFiles = new ArrayList<>();
        listLimit = DEFAULT_LIMIT;
    }

    /**
     * Retrieves the current limit on the size of the list.
     */
    public int getListLimit() {
        return listLimit;
    }

    /**
     * Sets an upper limit on the size of the list.
     * If the given limit is smaller than the size of the current list,
     * the list will be shrunk to fit the new limit by removing oldest items first.
     * <p>
     * Zero is a valid limit, and effectively disables the recent files list.
     * Negative values will get an IllegalArgumentException.
     * </p>
     */
    public void setListLimit(int listLimit) {
        if (listLimit < 0) {
            throw new IllegalArgumentException("List limit cannot be negative");
        }
        this.listLimit = listLimit;
        while (recentFiles.size() > listLimit) {
            recentFiles.remove(0); // remove from start of list
        }
    }

    /**
     * Returns the count of files currently in the list.
     */
    public int size() {
        return recentFiles.size();
    }

    /**
     * Reports whether the list is currently empty.
     */
    public boolean isEmpty() {
        return recentFiles.isEmpty();
    }

    /**
     * Provides access to the list itself via a defensive copy.
     */
    public List<File> getRecentFiles() {
        return new ArrayList<>(recentFiles); // return a copy to prevent external modification
    }

    /**
     * Adds the given file to the list.
     * If our list limit is zero, this method does nothing.
     * Otherwise, an older file may be removed from the list to make room for the new file, if we are at the limit.
     */
    public void add(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (listLimit == 0) {
            return; // list is disabled, so do nothing
        }

        // If the file is already in the list, remove it:
        recentFiles.remove(file);

        // If we're at the list limit, remove older files until we have room for the new file:
        while (recentFiles.size() >= listLimit) {
            recentFiles.remove(0); // remove from start of list
        }

        // Now add the new guy:
        recentFiles.add(file);
    }

    /**
     * Removes the given file from the list, if it is present.
     */
    public void remove(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        recentFiles.remove(file);
    }

    /**
     * Loads the list from persistence.
     * All IOExceptions are simply logged and swallowed, and the list will be empty.
     */
    public void load() {
        recentFiles.clear();
        if (!MRF_FILE.exists() || listLimit == 0) {
            return;
        }
        if (!MRF_FILE.isFile() || !MRF_FILE.canRead()) {
            logger.warning("Recent files list file is not a readable file: " + MRF_FILE.getAbsolutePath());
            return;
        }
        try {
            List<String> lines = FileSystemUtil.readFileLines(MRF_FILE);
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                File file = new File(line.trim());
                if (file.exists() && file.isFile() && file.canRead()) {
                    // Only add it to the list if it actually exists and is readable:
                    recentFiles.add(file);
                }
                else {
                    logger.warning("Skipping recent file that does not exist or is not readable: "
                                           + file.getAbsolutePath());
                }

                if (recentFiles.size() >= listLimit) {
                    break; // stop loading if we reach the limit; the rest are just ignored
                }
            }
        }
        catch (IOException ioe) {
            logger.warning("Failed to read recent files list from file: " + ioe.getMessage());
        }
    }

    /**
     * Persists our list to disk. Any previous persistence file is overwritten.
     * All IOExceptions are simply logged and swallowed.
     */
    public void save() {
        List<String> lines = new ArrayList<>();
        for (File file : recentFiles) {
            lines.add(file.getAbsolutePath());
        }

        // Persist it even if empty, as we want to overwrite the previous list:
        try {
            FileSystemUtil.writeLinesToFile(lines, MRF_FILE);
        }
        catch (IOException ioe) {
            logger.warning("Failed to write recent files list to file: " + ioe.getMessage());
        }
    }
}
