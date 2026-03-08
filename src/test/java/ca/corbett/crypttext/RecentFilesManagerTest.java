package ca.corbett.crypttext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive unit tests for RecentFilesManager.
 */
class RecentFilesManagerTest {

    private RecentFilesManager manager;

    @TempDir
    Path tempDir;

    /** Points to the "recent_files" file inside the test's temporary directory. */
    private File mrfFile;

    @BeforeEach
    void setUp() {
        manager = new RecentFilesManager();
        mrfFile = new File(tempDir.toFile(), "recent_files");
    }

    // ==================== Constructor Tests ====================

    @Test
    void constructor_shouldCreateEmptyList() {
        assertTrue(manager.isEmpty());
        assertEquals(0, manager.size());
    }

    @Test
    void constructor_shouldUseDefaultLimit() {
        assertEquals(RecentFilesManager.DEFAULT_LIMIT, manager.getListLimit());
    }

    // ==================== add() Tests ====================

    @Test
    void add_withNullFile_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> manager.add(null));
    }

    @Test
    void add_singleFile_shouldIncreaseSize() {
        // GIVEN a new manager and a file:
        File file = new File(tempDir.toFile(), "file1.txt");

        // WHEN we add the file:
        manager.add(file);

        // THEN the list should contain one item:
        assertEquals(1, manager.size());
        assertFalse(manager.isEmpty());
    }

    @Test
    void add_singleFile_shouldBeInList() {
        // GIVEN a file:
        File file = new File(tempDir.toFile(), "file1.txt");

        // WHEN we add it:
        manager.add(file);

        // THEN it should appear in the list:
        List<File> recentFiles = manager.getRecentFiles();
        assertTrue(recentFiles.contains(file));
    }

    @Test
    void add_multipleFiles_shouldAddInOrder() {
        // GIVEN three files:
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        File file3 = new File(tempDir.toFile(), "file3.txt");

        // WHEN we add them in order:
        manager.add(file1);
        manager.add(file2);
        manager.add(file3);

        // THEN they should appear in the list in the order added (newest at end):
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(3, recentFiles.size());
        assertEquals(file1, recentFiles.get(0));
        assertEquals(file2, recentFiles.get(1));
        assertEquals(file3, recentFiles.get(2));
    }

    @Test
    void add_whenAtLimit_shouldRemoveOldestFile() {
        // GIVEN a manager at its default limit:
        manager.setListLimit(3);
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        File file3 = new File(tempDir.toFile(), "file3.txt");
        File file4 = new File(tempDir.toFile(), "file4.txt");
        manager.add(file1);
        manager.add(file2);
        manager.add(file3);

        // WHEN we add a fourth file beyond the limit:
        manager.add(file4);

        // THEN the oldest file should have been removed and the new file added:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(3, recentFiles.size());
        assertFalse(recentFiles.contains(file1));
        assertTrue(recentFiles.contains(file2));
        assertTrue(recentFiles.contains(file3));
        assertTrue(recentFiles.contains(file4));
    }

    @Test
    void add_duplicateFile_shouldMoveToEndWithoutDuplicating() {
        // GIVEN a manager with two files:
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        manager.add(file1);
        manager.add(file2);

        // WHEN we add the first file again:
        manager.add(file1);

        // THEN there should still be only two items, with file1 moved to the end:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(2, recentFiles.size());
        assertEquals(file2, recentFiles.get(0));
        assertEquals(file1, recentFiles.get(1));
    }

    @Test
    void add_withLimitZero_shouldDoNothing() {
        // GIVEN a manager with limit of 0:
        manager.setListLimit(0);
        File file = new File(tempDir.toFile(), "file1.txt");

        // WHEN we add a file:
        manager.add(file);

        // THEN the list should remain empty:
        assertTrue(manager.isEmpty());
        assertEquals(0, manager.size());
    }

    @Test
    void add_withLimitOne_shouldReplaceExistingEntry() {
        // GIVEN a manager with limit of 1:
        manager.setListLimit(1);
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        manager.add(file1);

        // WHEN we add another file:
        manager.add(file2);

        // THEN only the newest file should be in the list:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(1, recentFiles.size());
        assertEquals(file2, recentFiles.get(0));
    }

    // ==================== remove() Tests ====================

    @Test
    void remove_withNullFile_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> manager.remove(null));
    }

    @Test
    void remove_existingFile_shouldRemoveFromList() {
        // GIVEN a manager with a file:
        File file = new File(tempDir.toFile(), "file1.txt");
        manager.add(file);

        // WHEN we remove it:
        manager.remove(file);

        // THEN the list should be empty:
        assertTrue(manager.isEmpty());
    }

    @Test
    void remove_nonExistentFile_shouldNotThrowException() {
        // GIVEN a file not in the list:
        File file = new File(tempDir.toFile(), "notInList.txt");

        // WHEN we remove it, THEN no exception should be thrown:
        manager.remove(file);
        assertTrue(manager.isEmpty());
    }

    @Test
    void remove_oneOfMultipleFiles_shouldPreserveOthers() {
        // GIVEN a manager with three files:
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        File file3 = new File(tempDir.toFile(), "file3.txt");
        manager.add(file1);
        manager.add(file2);
        manager.add(file3);

        // WHEN we remove the middle file:
        manager.remove(file2);

        // THEN only file1 and file3 should remain:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(2, recentFiles.size());
        assertTrue(recentFiles.contains(file1));
        assertFalse(recentFiles.contains(file2));
        assertTrue(recentFiles.contains(file3));
    }

    // ==================== setListLimit() Tests ====================

    @Test
    void setListLimit_withNegativeValue_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> manager.setListLimit(-1));
    }

    @Test
    void setListLimit_withZero_shouldBeValidAndClearList() {
        // GIVEN a manager with files:
        File file1 = new File(tempDir.toFile(), "file1.txt");
        manager.add(file1);

        // WHEN we set limit to 0:
        manager.setListLimit(0);

        // THEN the list should be empty:
        assertEquals(0, manager.getListLimit());
        assertTrue(manager.isEmpty());
    }

    @Test
    void setListLimit_smallerThanCurrentListSize_shouldTrimOldestEntries() {
        // GIVEN a manager with 5 files:
        manager.setListLimit(5);
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        File file3 = new File(tempDir.toFile(), "file3.txt");
        File file4 = new File(tempDir.toFile(), "file4.txt");
        File file5 = new File(tempDir.toFile(), "file5.txt");
        manager.add(file1);
        manager.add(file2);
        manager.add(file3);
        manager.add(file4);
        manager.add(file5);
        assertEquals(5, manager.size());

        // WHEN we reduce the limit to 3:
        manager.setListLimit(3);

        // THEN the list should be trimmed to 3 entries (oldest removed first):
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(3, recentFiles.size());
        assertFalse(recentFiles.contains(file1));
        assertFalse(recentFiles.contains(file2));
        assertTrue(recentFiles.contains(file3));
        assertTrue(recentFiles.contains(file4));
        assertTrue(recentFiles.contains(file5));
    }

    @Test
    void setListLimit_sameAsCurrentLimit_shouldNotChangeList() {
        // GIVEN a manager with some files:
        manager.setListLimit(5);
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        manager.add(file1);
        manager.add(file2);

        // WHEN we set the same limit again:
        manager.setListLimit(5);

        // THEN the list should be unchanged:
        assertEquals(2, manager.size());
        assertTrue(manager.getRecentFiles().contains(file1));
        assertTrue(manager.getRecentFiles().contains(file2));
    }

    @Test
    void setListLimit_largerThanCurrentList_shouldNotChangeList() {
        // GIVEN a manager with 2 files and limit of 3:
        manager.setListLimit(3);
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        manager.add(file1);
        manager.add(file2);

        // WHEN we increase the limit to 10:
        manager.setListLimit(10);

        // THEN the list should still have both files:
        assertEquals(2, manager.size());
        assertEquals(10, manager.getListLimit());
    }

    // ==================== getRecentFiles() Tests ====================

    @Test
    void getRecentFiles_shouldReturnDefensiveCopy() {
        // GIVEN a manager with a file:
        File file = new File(tempDir.toFile(), "file1.txt");
        manager.add(file);

        // WHEN we get the list and modify it:
        List<File> copy = manager.getRecentFiles();
        copy.clear();

        // THEN the manager's internal list should be unchanged:
        assertEquals(1, manager.size());
    }

    // ==================== load() Tests ====================

    @Test
    void load_whenMrfFileDoesNotExist_shouldResultInEmptyList() {
        // GIVEN no "recent_files" file in tempDir

        // WHEN we load using tempDir:
        manager.load(tempDir.toFile());

        // THEN the list should be empty:
        assertTrue(manager.isEmpty());
    }

    @Test
    void load_withLimitZero_shouldResultInEmptyList() throws IOException {
        // GIVEN a limit of 0 and a persistence file with entries in tempDir:
        manager.setListLimit(0);
        File realFile = Files.createTempFile(tempDir, "real", ".txt").toFile();
        Files.write(mrfFile.toPath(), realFile.getAbsolutePath().getBytes());

        // WHEN we load using tempDir:
        manager.load(tempDir.toFile());

        // THEN the list should be empty:
        assertTrue(manager.isEmpty());
    }

    @Test
    void load_withExistingReadableFiles_shouldPopulateList() throws IOException {
        // GIVEN two real files and a persistence file referencing them:
        File realFile1 = Files.createTempFile(tempDir, "real1", ".txt").toFile();
        File realFile2 = Files.createTempFile(tempDir, "real2", ".txt").toFile();
        String content = realFile1.getAbsolutePath() + System.lineSeparator()
                + realFile2.getAbsolutePath();
        Files.write(mrfFile.toPath(), content.getBytes());

        // WHEN we load using tempDir:
        manager.load(tempDir.toFile());

        // THEN both files should be in the list:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(2, recentFiles.size());
        assertTrue(recentFiles.contains(realFile1));
        assertTrue(recentFiles.contains(realFile2));
    }

    @Test
    void load_shouldSkipNonExistentFiles() throws IOException {
        // GIVEN a persistence file that references one real and one non-existent file:
        File realFile = Files.createTempFile(tempDir, "real", ".txt").toFile();
        File nonExistentFile = new File(tempDir.toFile(), "ghost.txt");
        String content = nonExistentFile.getAbsolutePath() + System.lineSeparator()
                + realFile.getAbsolutePath();
        Files.write(mrfFile.toPath(), content.getBytes());

        // WHEN we load using tempDir:
        manager.load(tempDir.toFile());

        // THEN only the real file should be in the list:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(1, recentFiles.size());
        assertTrue(recentFiles.contains(realFile));
        assertFalse(recentFiles.contains(nonExistentFile));
    }

    @Test
    void load_shouldRespectListLimit() throws IOException {
        // GIVEN a limit of 2 and a persistence file with 4 entries:
        manager.setListLimit(2);
        File realFile1 = Files.createTempFile(tempDir, "real1", ".txt").toFile();
        File realFile2 = Files.createTempFile(tempDir, "real2", ".txt").toFile();
        File realFile3 = Files.createTempFile(tempDir, "real3", ".txt").toFile();
        File realFile4 = Files.createTempFile(tempDir, "real4", ".txt").toFile();
        String content = realFile1.getAbsolutePath() + System.lineSeparator()
                + realFile2.getAbsolutePath() + System.lineSeparator()
                + realFile3.getAbsolutePath() + System.lineSeparator()
                + realFile4.getAbsolutePath();
        Files.write(mrfFile.toPath(), content.getBytes());

        // WHEN we load using tempDir:
        manager.load(tempDir.toFile());

        // THEN only the first 2 files (up to limit) should be in the list:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(2, recentFiles.size());
        assertTrue(recentFiles.contains(realFile1));
        assertTrue(recentFiles.contains(realFile2));
        assertFalse(recentFiles.contains(realFile3));
        assertFalse(recentFiles.contains(realFile4));
    }

    @Test
    void load_shouldClearExistingListBeforeLoading() throws IOException {
        // GIVEN a manager that already has items in-memory, and a persistence file:
        File preExistingFile = new File(tempDir.toFile(), "preExisting.txt");
        manager.add(preExistingFile);
        assertEquals(1, manager.size());

        File realFile = Files.createTempFile(tempDir, "real", ".txt").toFile();
        Files.write(mrfFile.toPath(), realFile.getAbsolutePath().getBytes());

        // WHEN we load using tempDir:
        manager.load(tempDir.toFile());

        // THEN the pre-existing in-memory item should be gone, and only the loaded file present:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(1, recentFiles.size());
        assertTrue(recentFiles.contains(realFile));
        assertFalse(recentFiles.contains(preExistingFile));
    }

    @Test
    void load_shouldSkipBlankLines() throws IOException {
        // GIVEN a persistence file with blank lines:
        File realFile = Files.createTempFile(tempDir, "real", ".txt").toFile();
        String content = System.lineSeparator()
                + realFile.getAbsolutePath() + System.lineSeparator()
                + System.lineSeparator();
        Files.write(mrfFile.toPath(), content.getBytes());

        // WHEN we load using tempDir:
        manager.load(tempDir.toFile());

        // THEN only the real file should be in the list (blank lines ignored):
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(1, recentFiles.size());
        assertTrue(recentFiles.contains(realFile));
    }

    // ==================== save() Tests ====================

    @Test
    void save_withEmptyList_shouldCreateEmptyPersistenceFile() throws IOException {
        // GIVEN an empty manager

        // WHEN we save using tempDir:
        manager.save(tempDir.toFile());

        // THEN the persistence file should exist but be empty (or contain no meaningful entries):
        assertTrue(mrfFile.exists());
        String content = new String(Files.readAllBytes(mrfFile.toPath())).trim();
        assertTrue(content.isEmpty());
    }

    @Test
    void save_withFiles_shouldPersistAllFiles() throws IOException {
        // GIVEN a manager with two files:
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        manager.add(file1);
        manager.add(file2);

        // WHEN we save using tempDir:
        manager.save(tempDir.toFile());

        // THEN the persistence file should contain both file paths:
        String content = new String(Files.readAllBytes(mrfFile.toPath()));
        assertTrue(content.contains(file1.getAbsolutePath()));
        assertTrue(content.contains(file2.getAbsolutePath()));
    }

    // ==================== load/save Roundtrip Tests ====================

    @Test
    void saveAndLoad_roundtrip_shouldPreserveList() throws IOException {
        // GIVEN two real files added to the manager:
        File realFile1 = Files.createTempFile(tempDir, "real1", ".txt").toFile();
        File realFile2 = Files.createTempFile(tempDir, "real2", ".txt").toFile();
        manager.add(realFile1);
        manager.add(realFile2);

        // WHEN we save and then load in a new manager (both using tempDir):
        manager.save(tempDir.toFile());
        RecentFilesManager manager2 = new RecentFilesManager();
        manager2.load(tempDir.toFile());

        // THEN the new manager should have the same files:
        List<File> recentFiles = manager2.getRecentFiles();
        assertEquals(2, recentFiles.size());
        assertTrue(recentFiles.contains(realFile1));
        assertTrue(recentFiles.contains(realFile2));
    }

    // ==================== Interaction / Edge Case Tests ====================

    @Test
    void reduceLimitAfterLoad_shouldTrimLoadedList() throws IOException {
        // GIVEN a manager with limit 5 loading 4 files:
        manager.setListLimit(5);
        File realFile1 = Files.createTempFile(tempDir, "real1", ".txt").toFile();
        File realFile2 = Files.createTempFile(tempDir, "real2", ".txt").toFile();
        File realFile3 = Files.createTempFile(tempDir, "real3", ".txt").toFile();
        File realFile4 = Files.createTempFile(tempDir, "real4", ".txt").toFile();
        String content = realFile1.getAbsolutePath() + System.lineSeparator()
                + realFile2.getAbsolutePath() + System.lineSeparator()
                + realFile3.getAbsolutePath() + System.lineSeparator()
                + realFile4.getAbsolutePath();
        Files.write(mrfFile.toPath(), content.getBytes());
        manager.load(tempDir.toFile());
        assertEquals(4, manager.size());

        // WHEN we reduce the limit to 2 after loading:
        manager.setListLimit(2);

        // THEN the list should be trimmed to 2, removing the oldest entries:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(2, recentFiles.size());
        assertFalse(recentFiles.contains(realFile1));
        assertFalse(recentFiles.contains(realFile2));
        assertTrue(recentFiles.contains(realFile3));
        assertTrue(recentFiles.contains(realFile4));
    }

    @Test
    void addDuplicate_atLimit_shouldNotExceedLimit() {
        // GIVEN a manager at its limit with 3 files:
        manager.setListLimit(3);
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        File file3 = new File(tempDir.toFile(), "file3.txt");
        manager.add(file1);
        manager.add(file2);
        manager.add(file3);

        // WHEN we re-add a file already in the list:
        manager.add(file2);

        // THEN the size should still be 3, with file2 moved to the end:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(3, recentFiles.size());
        assertTrue(recentFiles.contains(file1));
        assertTrue(recentFiles.contains(file3));
        assertEquals(file2, recentFiles.get(2));
    }

    @Test
    void addMultipleDuplicates_shouldAlwaysAppearOnceAtEnd() {
        // GIVEN a manager with two files:
        manager.setListLimit(5);
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        manager.add(file1);
        manager.add(file2);

        // WHEN we add file1 multiple times:
        manager.add(file1);
        manager.add(file1);

        // THEN file1 should appear exactly once at the end:
        List<File> recentFiles = manager.getRecentFiles();
        assertEquals(2, recentFiles.size());
        assertEquals(file2, recentFiles.get(0));
        assertEquals(file1, recentFiles.get(1));
    }

    @Test
    void setLimitToZeroAfterLoad_shouldClearList() throws IOException {
        // GIVEN a loaded manager with files:
        File realFile1 = Files.createTempFile(tempDir, "real1", ".txt").toFile();
        File realFile2 = Files.createTempFile(tempDir, "real2", ".txt").toFile();
        String content = realFile1.getAbsolutePath() + System.lineSeparator()
                + realFile2.getAbsolutePath();
        Files.write(mrfFile.toPath(), content.getBytes());
        manager.load(tempDir.toFile());
        assertEquals(2, manager.size());

        // WHEN we set limit to 0:
        manager.setListLimit(0);

        // THEN the list should be empty:
        assertTrue(manager.isEmpty());
        assertEquals(0, manager.getListLimit());
    }
}
