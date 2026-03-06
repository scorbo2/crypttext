package ca.corbett.crypttext.text;

import ca.corbett.crypttext.VetoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Comprehensive unit tests for the TextManager class.
 */
class TextManagerTest {

    private TextManager textManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        textManager = new TextManager();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (textManager != null) {
            textManager.dispose();
        }
    }

    // ==================== Basic State Tests ====================

    @Test
    void testNewTextManagerIsEmpty() {
        assertTrue(textManager.isEmpty());
        assertEquals(0, textManager.size());
    }

    @Test
    void testNewTextCreatesNonNullText() throws IOException {
        Text text = textManager.newText();
        assertNotNull(text);
        assertEquals("", text.getText());
        assertNotNull(text.getSourceFile());
    }

    @Test
    void testNewTextAddsToManager() throws IOException {
        assertTrue(textManager.isEmpty());
        textManager.newText();
        assertFalse(textManager.isEmpty());
        assertEquals(1, textManager.size());
    }

    @Test
    void testMultipleNewTextsIncreaseSize() throws IOException {
        textManager.newText();
        textManager.newText();
        textManager.newText();
        assertEquals(3, textManager.size());
    }

    // ==================== Remove Tests ====================

    @Test
    void testRemoveTextDecreasesSize() throws IOException {
        Text text = textManager.newText();
        assertEquals(1, textManager.size());
        textManager.remove(text);
        assertEquals(0, textManager.size());
        assertTrue(textManager.isEmpty());
    }

    @Test
    void testRemoveNullTextThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.remove(null));
    }

    @Test
    void testRemoveTextDeletesTempFile() throws IOException {
        Text text = textManager.newText();
        File tempFile = text.getSourceFile();
        assertTrue(tempFile.exists());
        textManager.remove(text);
        assertFalse(tempFile.exists());
    }

    @Test
    void testRemoveTextNotInManagerDoesNothing() throws IOException {
        Text text = textManager.newText();
        textManager.remove(text);
        // Should throw exception if called again because the temp file is already deleted
        assertThrows(IOException.class, () -> textManager.remove(text));
        assertEquals(0, textManager.size());
    }

    // ==================== Clear/Dispose Tests ====================

    @Test
    void testClearEmptiesManager() throws IOException {
        textManager.newText();
        textManager.newText();
        textManager.newText();
        assertEquals(3, textManager.size());
        textManager.clear();
        assertEquals(0, textManager.size());
        assertTrue(textManager.isEmpty());
    }

    @Test
    void testDisposeIsEquivalentToClear() throws IOException {
        textManager.newText();
        textManager.newText();
        assertEquals(2, textManager.size());
        textManager.dispose();
        assertEquals(0, textManager.size());
        assertTrue(textManager.isEmpty());
    }

    @Test
    void testClearDeletesAllTempFiles() throws IOException {
        Text text1 = textManager.newText();
        Text text2 = textManager.newText();
        File file1 = text1.getSourceFile();
        File file2 = text2.getSourceFile();
        assertTrue(file1.exists());
        assertTrue(file2.exists());
        textManager.clear();
        assertFalse(file1.exists());
        assertFalse(file2.exists());
    }

    // ==================== FromCache Tests ====================

    @Test
    void testFromCacheWithNullFileThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.fromCache(null));
    }

    @Test
    void testFromCacheReturnsNullForNonCachedFile() throws IOException {
        File file = tempDir.resolve("test.txt").toFile();
        assertTrue(file.createNewFile());
        assertNull(textManager.fromCache(file));
    }

    @Test
    void testFromCacheReturnsCachedText() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "test content");

        Text loadedText = textManager.fromFile(file);
        Text cachedText = textManager.fromCache(file);

        assertSame(loadedText, cachedText);
    }

    @Test
    void testFromCacheWithSameFileReturnsText() throws Exception {
        Text text = textManager.newText();
        Text cached = textManager.fromCache(text.getSourceFile());
        assertSame(text, cached);
    }

    // ==================== FromFile Tests ====================

    @Test
    void testFromFileWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.fromFile(null));
    }

    @Test
    void testFromFileWithNonExistentFileThrowsException() {
        File nonExistent = tempDir.resolve("nonexistent.txt").toFile();
        assertThrows(IllegalArgumentException.class, () -> textManager.fromFile(nonExistent));
    }

    @Test
    void testFromFileWithDirectoryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.fromFile(tempDir.toFile()));
    }

    @Test
    void testFromFileLoadsContent() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        String content = "Hello, World!";
        Files.writeString(file.toPath(), content);

        Text text = textManager.fromFile(file);

        assertNotNull(text);
        assertEquals(content, text.getText());
        assertEquals(file, text.getSourceFile());
    }

    @Test
    void testFromFileIncreasesSize() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        assertEquals(0, textManager.size());
        textManager.fromFile(file);
        assertEquals(1, textManager.size());
    }

    @Test
    void testFromFileReturnsCachedIfAlreadyLoaded() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        Text first = textManager.fromFile(file);
        Text second = textManager.fromFile(file);

        assertSame(first, second);
        assertEquals(1, textManager.size());
    }

    @Test
    void testFromFileThrowsVetoExceptionWhenVetoed() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        textManager.addTextWillLoadListener((manager, f) -> false);

        try {
            Text text = textManager.fromFile(file);
            assertNull(text);
            assertEquals(0, textManager.size());
            fail("Expected VetoException to be thrown");
        }
        catch (VetoException ignored) {
            // expected exception
        }
    }

    // ==================== SaveText Tests ====================

    @Test
    void testSaveTextWithNullTextThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.saveText(null, "content"));
    }

    @Test
    void testSaveTextWithUncachedTextThrowsException() throws IOException {
        TextManager otherManager = new TextManager();
        Text otherText = otherManager.newText();

        assertThrows(IllegalArgumentException.class, () -> textManager.saveText(otherText, "content"));

        otherManager.dispose();
    }

    @Test
    void testSaveTextUpdatesContent() throws Exception {
        Text text = textManager.newText();
        String newContent = "Updated content";

        Text savedText = textManager.saveText(text, newContent);

        assertNotNull(savedText);
        assertEquals(newContent, savedText.getText());
        assertEquals(text.getSourceFile(), savedText.getSourceFile());
    }

    @Test
    void testSaveTextWithNullValueSavesEmptyString() throws Exception {
        Text text = textManager.newText();

        Text savedText = textManager.saveText(text, null);

        assertEquals("", savedText.getText());
    }

    @Test
    void testSaveTextReplacesOldTextInCache() throws Exception {
        Text text = textManager.newText();
        File sourceFile = text.getSourceFile();

        Text savedText = textManager.saveText(text, "new content");

        assertNotSame(text, savedText);
        assertEquals(1, textManager.size());
        // fromCache searches by file, so it should return the new saved text
        assertSame(savedText, textManager.fromCache(sourceFile));
    }

    @Test
    void testSaveTextPersistsToFile() throws Exception {
        Text text = textManager.newText();
        String content = "Persisted content";

        Text savedText = textManager.saveText(text, content);
        String fileContent = Files.readString(savedText.getSourceFile().toPath());

        assertEquals(content, fileContent);
    }

    // ==================== SaveTextAs Tests ====================

    @Test
    void testSaveTextAsWithNullTextThrowsException() throws IOException {
        File file = tempDir.resolve("test.txt").toFile();
        assertTrue(file.createNewFile());
        assertThrows(IllegalArgumentException.class, () -> textManager.saveTextAs(null, "content", file));
    }

    @Test
    void testSaveTextAsWithNullFileThrowsException() throws IOException {
        Text text = textManager.newText();
        assertThrows(IllegalArgumentException.class, () -> textManager.saveTextAs(text, "content", null));
    }

    @Test
    void testSaveTextAsWithDirectoryThrowsException() throws IOException {
        Text text = textManager.newText();
        assertThrows(IllegalArgumentException.class, () -> textManager.saveTextAs(text, "content", tempDir.toFile()));
    }

    @Test
    void testSaveTextAsWithUncachedTextThrowsException() throws IOException {
        TextManager otherManager = new TextManager();
        Text otherText = otherManager.newText();
        File file = tempDir.resolve("test.txt").toFile();
        assertTrue(file.createNewFile());

        assertThrows(IllegalArgumentException.class, () -> textManager.saveTextAs(otherText, "content", file));

        otherManager.dispose();
    }

    @Test
    void testSaveTextAsChangesSourceFile() throws Exception {
        Text text = textManager.newText();
        File originalFile = text.getSourceFile();
        File newFile = tempDir.resolve("new.txt").toFile();
        assertTrue(newFile.createNewFile());
        String content = "New content";

        Text savedText = textManager.saveTextAs(text, content, newFile);

        assertNotEquals(originalFile, savedText.getSourceFile());
        assertEquals(newFile, savedText.getSourceFile());
        assertEquals(content, savedText.getText());
    }

    @Test
    void testSaveTextAsPersistsToNewFile() throws Exception {
        Text text = textManager.newText();
        File newFile = tempDir.resolve("new.txt").toFile();
        assertTrue(newFile.createNewFile());
        String content = "Content in new file";

        textManager.saveTextAs(text, content, newFile);
        String fileContent = Files.readString(newFile.toPath());

        assertEquals(content, fileContent);
    }

    @Test
    void testSaveTextAsRemovesOldTextFromCache() throws Exception {
        Text text = textManager.newText();
        File newFile = tempDir.resolve("new.txt").toFile();
        assertTrue(newFile.createNewFile());

        Text savedText = textManager.saveTextAs(text, "content", newFile);

        assertNotSame(text, savedText);
        assertEquals(1, textManager.size());
    }

    @Test
    void testSaveTextAsToSameFileDelegatesToSaveText() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "original");
        Text text = textManager.fromFile(file);

        Text savedText = textManager.saveTextAs(text, "updated", file);

        assertEquals("updated", savedText.getText());
        assertEquals(file, savedText.getSourceFile());
        assertEquals(1, textManager.size());
    }

    // ==================== Listener Tests - TextWillLoadListener ====================

    @Test
    void testAddTextWillLoadListenerWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.addTextWillLoadListener(null));
    }

    @Test
    void testRemoveTextWillLoadListenerWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.removeTextWillLoadListener(null));
    }

    @Test
    void testTextWillLoadListenerReceivesEvent() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        boolean[] called = {false};
        textManager.addTextWillLoadListener((manager, f) -> {
            called[0] = true;
            assertEquals(file, f);
            return true;
        });

        textManager.fromFile(file);
        assertTrue(called[0]);
    }

    @Test
    void testTextWillLoadListenerCanVetoLoad() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        textManager.addTextWillLoadListener((manager, f) -> false);

        try {
            Text text = textManager.fromFile(file);
            fail("Expected VetoException to be thrown");
        }
        catch (VetoException ignored) {
            // expected exception
        }
    }

    @Test
    void testMultipleTextWillLoadListenersAllCalled() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        int[] callCount = {0};
        textManager.addTextWillLoadListener((manager, f) -> {
            callCount[0]++;
            return true;
        });
        textManager.addTextWillLoadListener((manager, f) -> {
            callCount[0]++;
            return true;
        });

        textManager.fromFile(file);
        assertEquals(2, callCount[0]);
    }

    @Test
    void testTextWillLoadListenerStopsOnFirstVeto() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        boolean[] secondCalled = {false};
        textManager.addTextWillLoadListener((manager, f) -> false);
        textManager.addTextWillLoadListener((manager, f) -> {
            secondCalled[0] = true;
            return true;
        });

        try {
            textManager.fromFile(file);
            fail("Expected VetoException to be thrown");
        }
        catch (VetoException ignored) {
            // expected exception
        }

        assertFalse(secondCalled[0]);
    }

    @Test
    void testRemoveTextWillLoadListenerWorks() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        boolean[] called = {false};
        TextWillLoadListener listener = (manager, f) -> {
            called[0] = true;
            return true;
        };

        textManager.addTextWillLoadListener(listener);
        textManager.removeTextWillLoadListener(listener);
        textManager.fromFile(file);

        assertFalse(called[0]);
    }

    // ==================== Listener Tests - TextWillSaveListener ====================

    @Test
    void testAddTextWillSaveListenerWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.addTextWillSaveListener(null));
    }

    @Test
    void testRemoveTextWillSaveListenerWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.removeTextWillSaveListener(null));
    }

    @Test
    void testTextWillSaveListenerReceivesEvent() throws Exception {
        Text text = textManager.newText();

        boolean[] called = {false};
        textManager.addTextWillSaveListener((manager, t, n, file) -> {
            called[0] = true;
            assertEquals(text, t);
            return true;
        });

        textManager.saveText(text, "content");
        assertTrue(called[0]);
    }

    @Test
    void testTextWillSaveListenerCanVetoSave() throws Exception {
        Text text = textManager.newText();
        textManager.addTextWillSaveListener((manager, t, n, file) -> false);

        try {
            textManager.saveText(text, "new content");
            fail("Expected VetoException to be thrown");
        }
        catch (VetoException ignored) {
            // expected exception
        }
    }

    @Test
    void testTextWillSaveListenerReceivesCorrectDestFile() throws Exception {
        Text text = textManager.newText();
        File newFile = tempDir.resolve("new.txt").toFile();
        assertTrue(newFile.createNewFile());

        File[] receivedFile = {null};
        textManager.addTextWillSaveListener((manager, t, n, file) -> {
            receivedFile[0] = file;
            return true;
        });

        textManager.saveTextAs(text, "content", newFile);
        assertEquals(newFile, receivedFile[0]);
    }

    @Test
    void testRemoveTextWillSaveListenerWorks() throws Exception {
        Text text = textManager.newText();

        boolean[] called = {false};
        TextWillSaveListener listener = (manager, t, newContents, file) -> {
            called[0] = true;
            return true;
        };

        textManager.addTextWillSaveListener(listener);
        textManager.removeTextWillSaveListener(listener);
        textManager.saveText(text, "content");

        assertFalse(called[0]);
    }

    // ==================== Listener Tests - TextLoadedListener ====================

    @Test
    void testAddTextLoadedListenerWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.addTextLoadedListener(null));
    }

    @Test
    void testRemoveTextLoadedListenerWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.removeTextLoadedListener(null));
    }

    @Test
    void testTextLoadedListenerReceivesEvent() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        boolean[] called = {false};
        textManager.addTextLoadedListener((manager, t) -> {
            called[0] = true;
            assertNotNull(t);
        });

        textManager.fromFile(file);
        assertTrue(called[0]);
    }

    @Test
    void testTextLoadedListenerNotCalledWhenLoadVetoed() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        boolean[] loadedCalled = {false};
        textManager.addTextWillLoadListener((manager, f) -> false);
        textManager.addTextLoadedListener((manager, t) -> loadedCalled[0] = true);

        try {
            textManager.fromFile(file);
            fail("Expected VetoException to be thrown");
        }
        catch (VetoException ignored) {
            // expected exception
        }

        assertFalse(loadedCalled[0]);
    }

    @Test
    void testTextLoadedListenerNotCalledForCachedFile() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        textManager.fromFile(file);

        int[] callCount = {0};
        textManager.addTextLoadedListener((manager, t) -> callCount[0]++);

        textManager.fromFile(file); // Should return cached, not call listener
        assertEquals(0, callCount[0]);
    }

    @Test
    void testRemoveTextLoadedListenerWorks() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "content");

        boolean[] called = {false};
        TextLoadedListener listener = (manager, t) -> called[0] = true;

        textManager.addTextLoadedListener(listener);
        textManager.removeTextLoadedListener(listener);
        textManager.fromFile(file);

        assertFalse(called[0]);
    }

    // ==================== Listener Tests - TextSavedListener ====================

    @Test
    void testAddTextSavedListenerWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.addTextSavedListener(null));
    }

    @Test
    void testRemoveTextSavedListenerWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.removeTextSavedListener(null));
    }

    @Test
    void testTextSavedListenerReceivesEvent() throws Exception {
        Text text = textManager.newText();

        boolean[] called = {false};
        Text[] savedText = {null};
        textManager.addTextSavedListener((manager, s, t) -> {
            called[0] = true;
            savedText[0] = t;
        });

        Text result = textManager.saveText(text, "content");
        assertTrue(called[0]);
        assertEquals(result, savedText[0]);
    }

    @Test
    void testTextSavedListenerNotCalledWhenSaveVetoed() throws Exception {
        Text text = textManager.newText();

        boolean[] savedCalled = {false};
        textManager.addTextWillSaveListener((manager, t, n, file) -> false);
        textManager.addTextSavedListener((manager, s, t) -> savedCalled[0] = true);

        try {
            textManager.saveText(text, "content");
            fail("Expected VetoException to be thrown");
        }
        catch (VetoException ignored) {
            // expected exception
        }
        
        assertFalse(savedCalled[0]);
    }

    @Test
    void testTextSavedListenerReceivesEventForSaveAs() throws Exception {
        Text text = textManager.newText();
        File newFile = tempDir.resolve("new.txt").toFile();
        assertTrue(newFile.createNewFile());

        boolean[] called = {false};
        textManager.addTextSavedListener((manager, s, t) -> called[0] = true);

        textManager.saveTextAs(text, "content", newFile);
        assertTrue(called[0]);
    }

    @Test
    void testRemoveTextSavedListenerWorks() throws Exception {
        Text text = textManager.newText();

        boolean[] called = {false};
        TextSavedListener listener = (manager, s, t) -> called[0] = true;

        textManager.addTextSavedListener(listener);
        textManager.removeTextSavedListener(listener);
        textManager.saveText(text, "content");

        assertFalse(called[0]);
    }

    // ==================== Listener Tests - TextChangedListener ====================

    @Test
    void testAddTextChangedListenerWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> textManager.addTextChangedListener(null));
    }

    @Test
    void testAddTextChangedListenerReturnsManager() {
        TextChangedListener listener = (manager, staleText, newValue) -> {
        };
        TextManager result = textManager.addTextChangedListener(listener);
        assertSame(textManager, result);
    }

    // ==================== Chaining Tests ====================

    @Test
    void testListenerMethodsReturnManagerForChaining() {
        TextWillLoadListener willLoadListener = (manager, file) -> true;
        TextWillSaveListener willSaveListener = (manager, text, newContents, file) -> true;
        TextLoadedListener loadedListener = (manager, text) -> {
        };
        TextSavedListener savedListener = (manager, s, text) -> {
        };

        TextManager result = textManager
                .addTextWillLoadListener(willLoadListener)
                .addTextWillSaveListener(willSaveListener)
                .addTextLoadedListener(loadedListener)
                .addTextSavedListener(savedListener);

        assertSame(textManager, result);
    }

    // ==================== Integration Tests ====================

    @Test
    void testCompleteWorkflowNewSaveAndReload() throws Exception {
        // Create new text
        Text text1 = textManager.newText();
        assertEquals("", text1.getText());

        // Save with content
        String content = "Test content";
        Text text2 = textManager.saveText(text1, content);
        assertEquals(content, text2.getText());

        // Verify persisted
        String fileContent = Files.readString(text2.getSourceFile().toPath());
        assertEquals(content, fileContent);

        // Clear and reload
        File sourceFile = text2.getSourceFile();
        textManager.clear();
        assertTrue(textManager.isEmpty());

        // Create the file again since clear deletes temp files
        Files.writeString(sourceFile.toPath(), content);
        Text text3 = textManager.fromFile(sourceFile);
        assertEquals(content, text3.getText());
    }

    @Test
    void testCompleteWorkflowWithSaveAs() throws Exception {
        // Create and save with initial content
        Text text1 = textManager.newText();
        text1 = textManager.saveText(text1, "Initial content");

        // Save as to new location
        File newFile = tempDir.resolve("saveas.txt").toFile();
        assertTrue(newFile.createNewFile());
        String newContent = "New content";
        Text text2 = textManager.saveTextAs(text1, newContent, newFile);

        // Verify new location
        assertEquals(newFile, text2.getSourceFile());
        assertEquals(newContent, text2.getText());

        // Verify persisted
        String fileContent = Files.readString(newFile.toPath());
        assertEquals(newContent, fileContent);
    }

    @Test
    void testMultipleTextsCanCoexist() throws Exception {
        File file1 = tempDir.resolve("file1.txt").toFile();
        File file2 = tempDir.resolve("file2.txt").toFile();
        Files.writeString(file1.toPath(), "Content 1");
        Files.writeString(file2.toPath(), "Content 2");

        Text text1 = textManager.fromFile(file1);
        Text text2 = textManager.fromFile(file2);
        Text text3 = textManager.newText();

        assertEquals(3, textManager.size());
        assertEquals("Content 1", text1.getText());
        assertEquals("Content 2", text2.getText());
        assertEquals("", text3.getText());
    }

    @Test
    void testAllListenersCombinedWorkflow() throws Exception {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "initial");

        StringBuilder eventLog = new StringBuilder();

        textManager.addTextWillLoadListener((manager, f) -> {
            eventLog.append("willLoad,");
            return true;
        });

        textManager.addTextLoadedListener((manager, t) -> eventLog.append("loaded,"));

        textManager.addTextWillSaveListener((manager, t, n, destFile) -> {
            eventLog.append("willSave,");
            return true;
        });

        textManager.addTextSavedListener((manager, s, t) -> eventLog.append("saved,"));

        // Load
        Text text = textManager.fromFile(file);

        // Save
        textManager.saveText(text, "updated");

        assertEquals("willLoad,loaded,willSave,saved,", eventLog.toString());
    }

    @Test
    void testSaveTextUpdatesFileContent() throws Exception {
        Text text = textManager.newText();
        File file = text.getSourceFile();

        String content1 = "First save";
        Text saved1 = textManager.saveText(text, content1);
        assertEquals(content1, Files.readString(file.toPath()));

        String content2 = "Second save";
        textManager.saveText(saved1, content2);
        assertEquals(content2, Files.readString(file.toPath()));
    }

    @Test
    void testEmptyStringHandling() throws Exception {
        Text text = textManager.newText();

        // Save empty string
        Text saved = textManager.saveText(text, "");
        assertEquals("", saved.getText());
        assertEquals("", Files.readString(saved.getSourceFile().toPath()));
    }

    @Test
    void testWhitespaceContentHandling() throws Exception {
        Text text = textManager.newText();
        String whitespace = "   \n\t\r\n   ";

        Text saved = textManager.saveText(text, whitespace);
        assertEquals(whitespace, saved.getText());
        assertEquals(whitespace, Files.readString(saved.getSourceFile().toPath()));
    }

    @Test
    void testLargeContentHandling() throws Exception {
        Text text = textManager.newText();

        // Create a large string (1MB)
        String content = "0123456789".repeat(100000);

        Text saved = textManager.saveText(text, content);
        assertEquals(content, saved.getText());
        assertEquals(content.length(), Files.readString(saved.getSourceFile().toPath()).length());
    }
}