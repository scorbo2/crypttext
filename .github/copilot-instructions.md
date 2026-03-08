# CryptText Codebase Guide for AI Coding Assistants

This guide provides comprehensive information about the CryptText project structure, architecture, and conventions to help AI assistants work efficiently with the codebase.

## 1. Project Overview

**CryptText** is a user-friendly Java Swing text editor with built-in encryption capabilities. Users can encrypt/decrypt text files using passwords without needing cryptographic knowledge. The application is built on the `swing-extras` framework and features an extensible plugin architecture.

- **Purpose**: Simple encryption/decryption text editor with extensibility
- **Main Features**:
  - Text file editing with line numbers and customizable fonts
  - Password-protected encryption/decryption (AES-GCM via Bouncy Castle)
  - Tab-based multi-file editing
  - Customizable Look & Feel and color themes
  - Keyboard shortcut customization
  - Built-in extensions: StatusBar and DirTree
  - Extension system for adding features
  - Recent files list
  - Single-instance mode option
  - Encryption metadata tracking

## 2. Project Structure

```
crypttext/
├── pom.xml                          # Maven configuration
├── README.md                        # User documentation
├── LICENSE                          # MIT License
├── .editorconfig                    # Editor configuration
├── installer.props                  # Installer properties
├── src/
│   ├── main/java/ca/corbett/crypttext/
│   │   ├── Main.java            # Application entry point
│   │   ├── Version.java         # Version and directory info
│   │   ├── AppConfig.java       # Centralized configuration manager
│   │   ├── RecentFilesManager.java
│   │   ├── CryptTextResourceLoader.java
│   │   ├── DecryptionFailedException.java
│   │   ├── VetoException.java
│   │   ├── crypt/               # Encryption/decryption logic
│   │   │   ├── CryptUtil.java
│   │   │   ├── CryptMetadata.java
│   │   │   ├── DefaultCryptMetadata.java
│   │   │   └── EncryptedText.java
│   │   ├── text/                # Text model and management
│   │   │   ├── Text.java
│   │   │   ├── TextManager.java
│   │   │   ├── TextChangedListener.java
│   │   │   ├── TextLoadedListener.java
│   │   │   ├── TextSavedListener.java
│   │   │   ├── TextWillLoadListener.java
│   │   │   └── TextWillSaveListener.java
│   │   ├── ui/                  # GUI components
│   │   │   ├── MainWindow.java
│   │   │   ├── EditorTab.java
│   │   │   ├── EditorTabPane.java
│   │   │   ├── EditorTabHeader.java
│   │   │   ├── LineNumberGutter.java
│   │   │   ├── MenuManager.java
│   │   │   ├── ColorTheme.java
│   │   │   ├── TabStateManager.java
│   │   │   ├── DefaultTabStateManager.java
│   │   │   ├── UIReloadable.java
│   │   │   ├── TextFileFilter.java
│   │   │   └── actions/         # UI action implementations
│   │   │       ├── AboutAction.java
│   │   │       ├── CryptAction.java
│   │   │       ├── ExitAction.java
│   │   │       ├── ExtensionManagerAction.java
│   │   │       ├── ForgetPasswordAction.java
│   │   │       ├── LogConsoleAction.java
│   │   │       ├── NewTabAction.java
│   │   │       ├── OpenFileAction.java
│   │   │       ├── PropertiesAction.java
│   │   │       ├── SaveAction.java
│   │   │       ├── SaveAsAction.java
│   │   │       ├── SaveUnencryptedAction.java
│   │   │       └── UIReloadAction.java
│   │   └── extensions/          # Extension system
│   │       ├── CryptTextExtension.java
│   │       ├── CryptTextExtensionManager.java
│   │       ├── ExtraComponentPosition.java
│   │       └── builtin/
│   │           ├── DirTreeExtension.java
│   │           ├── StatusBarExtension.java
│   │           └── TestExtension.java
│   ├── resources/ca/corbett/crypttext/
│   │   ├── logging.properties
│   │   ├── file_header.txt
│   │   ├── ReleaseNotes.txt
│   │   ├── images/
│   │   └── screenshots/
│   └── test/java/ca/corbett/crypttext/
│       ├── RecentFilesManagerTest.java
│       ├── crypt/CryptUtilTest.java
│       └── text/TextManagerTest.java
```

## 3. Build System

**Build Tool**: Maven 3.x  
**Java Version**: 17+

### Key Build Commands
```bash
mvn clean package          # Build the project
mvn test                   # Run all tests
mvn test -Dtest=CryptUtilTest  # Run specific test
```

### Build Configuration
- **Main Class**: `ca.corbett.crypttext.Main`
- **JAR Assembly**: Executable jar with lib/ classpath
- **Plugins**: maven-dependency-plugin, maven-surefire-plugin (JUnit 5), maven-jar-plugin

## 4. Technology Stack

### Core Dependencies
- **Java 17** - Language and runtime
- **Swing** - GUI framework (bundled with Java)
- **swing-extras 2.8.0** - Custom Swing components and utilities
- **Bouncy Castle (bcprov-jdk18on 1.83)** - Cryptography provider

### Testing Dependencies
- **JUnit 5 (junit-jupiter 5.12.1)**
- **Mockito 5.14.2**

### Encryption
- **Algorithm**: AES-256 in GCM mode (authenticated encryption)
- **Key Derivation**: Argon2 (memory-hard password hashing)
- **Encoding**: Base64 (for file wrapper)

## 5. Key Source Packages

### ca.corbett.crypttext.crypt
Handles cryptographic operations:
- **CryptUtil**: Main encryption/decryption API
  - `encrypt(password, plaintext)` / `decrypt(password, ciphertext)`
  - `encryptAndWrap()` / `decryptAndUnwrap()`
  - `isCryptTextWrapped()` - Detects encrypted files
- **CryptMetadata**: Abstract base for encryption metadata (extensible)
- **DefaultCryptMetadata**: Default implementation
- **EncryptedText**: Data class for encrypted content

### ca.corbett.crypttext.text
Manages text loading/saving lifecycle with listeners:
- **Text**: Immutable model object (text + source file)
- **TextManager**: Controller for text operations, file I/O, listener dispatch
- **Listeners**: TextWillLoadListener, TextLoadedListener, TextWillSaveListener, TextSavedListener, TextChangedListener

### ca.corbett.crypttext.ui
GUI components and event handling:
- **MainWindow**: Main application frame (singleton)
- **EditorTab**: Individual editor tab (JPanel + JTextPane)
- **EditorTabPane**: Container for editor tabs
- **MenuManager**: Constructs main menu bar
- **LineNumberGutter**: Line number display
- **ColorTheme**: Editor color scheme definition
- **TabStateManager**: Persistence of open tabs (extensible interface)
- **actions/**: UI action classes (each action = menu command)

### ca.corbett.crypttext.extensions
Plugin architecture:
- **CryptTextExtension**: Abstract base class for extensions
  - Hooks: getTopLevelMenus(), getMenuItems(), fileWillLoad(), fileWillSave()
  - Access to MainWindow methods for tab manipulation
- **CryptTextExtensionManager**: Singleton manager for extension lifecycle
- **ExtraComponentPosition**: Enum for positioning extra UI components

### ca.corbett.crypttext (Core)
- **Main**: Entry point, initialization, single-instance management (port 54551)
- **Version**: Version info, directory paths (INSTALL_DIR, SETTINGS_DIR, EXTENSIONS_DIR)
- **AppConfig**: Centralized properties management (singleton)
  - Extends swing-extras AppProperties
  - Manages user settings, keyboard shortcuts, UI actions
  - Integrates with CryptTextExtensionManager
- **RecentFilesManager**: Recently-opened files list
- **DecryptionFailedException**: Custom exception for decryption failures
- **VetoException**: Extension veto signaling mechanism

## 6. Main Classes and Responsibilities

### Singleton Managers
| Class | Responsibility |
|-------|-----------------|
| **MainWindow** | Main application frame, coordinates UI, file I/O |
| **AppConfig** | All user settings, keystroke bindings, action creation |
| **TextManager** | Text loading/saving, listener dispatch, scratch directory |
| **CryptTextExtensionManager** | Extension lifecycle, hook invocation |
| **RecentFilesManager** | Recent files list persistence |

### UI Components
| Class | Responsibility |
|-------|-----------------|
| **EditorTab** | Single editor tab with text pane, dirty tracking |
| **EditorTabPane** | Container for editor tabs |
| **MenuManager** | Main menu bar construction |
| **LineNumberGutter** | Line numbers display |
| **ColorTheme** | Editor color scheme |

### Model Classes
| Class | Responsibility |
|-------|-----------------|
| **Text** | Immutable text + source file pair |
| **CryptMetadata** | Encryption metadata (abstract, extensible) |
| **EncryptedText** | Encrypted content wrapper |

## 7. Testing Conventions

### Framework
- **JUnit 5** (org.junit.jupiter)
- **Mockito 5.14.2** for mocking
- **Maven Surefire Plugin** for test execution

### Test Location & Naming
Tests follow Maven standard:
```
src/test/java/ca/corbett/crypttext/
├── RecentFilesManagerTest.java
├── crypt/CryptUtilTest.java
└── text/TextManagerTest.java
```

### Test Method Style
- **Naming**: `methodName_condition_expectedOutcome()`
- **Example**: `encrypt_withValidDataAndPassword_shouldSucceed()`
- **Pattern**: Given-When-Then structure

```java
@Test
void testExample() {
    // GIVEN setup
    String password = "test";
    
    // WHEN action
    byte[] result = CryptUtil.encrypt(password, data);
    
    // THEN verify
    assertNotNull(result);
}
```

### Current Tests
1. **CryptUtilTest** - Encryption/decryption edge cases
2. **TextManagerTest** - Text loading/saving lifecycle
3. **RecentFilesManagerTest** - Recent files persistence

## 8. Extension System

### How It Works
Extensions are loaded from `~/.CryptText/extensions/` directory. CryptTextExtensionManager discovers jar files containing CryptTextExtension subclasses.

### Extension Hooks
| Hook | Purpose | Return Value |
|------|---------|--------------|
| `getTopLevelMenus()` | Add top-level menus | List<JMenu> or null |
| `getMenuItems(topLevelMenu)` | Add items to existing menu | List<JMenuItem> or null |
| `fileWillLoad(File)` | Veto file loads | boolean (true=allow) |
| `fileWillSave(File, newContents, destFile)` | Veto file saves | boolean (true=allow) |

### Built-in Extensions
1. **DirTreeExtension**: Directory tree panel on left, double-click to open files
2. **StatusBarExtension**: Status bar at bottom, shows file info, encryption metadata
3. **TestExtension**: For internal testing only (enabled via `-DenableTestExtension`)

## 9. Configuration & Settings Files

### AppConfig Properties
Properties file: `~/.CryptText/CryptText.props`

**General Settings**
- `UI.General.singleInstance` - Enable single-instance mode
- `UI.General.showFullPath` - Show full file path in title bar
- `UI.General.recentFilesLimit` - Number of recent files

**Editor Settings**
- `UI.Editor.font` - Editor font
- `UI.Editor.gutterFont` - Line number font
- `UI.Editor.showLineNumbers` - Show line number gutter
- `UI.Editor.overrideLaf` - Use custom editor colors
- `UI.Editor.theme` - Color theme
- `UI.Editor.{bgColor,fgColor,gutterBgColor,gutterFgColor}` - Colors

**Tab Settings**
- `UI.Editor tabs.showLockIcons` - Show padlock on encrypted files
- `UI.Editor tabs.tabIconSize` - Padlock icon size
- `UI.Editor tabs.closeLastTabExits` - Exit when last tab closed
- `UI.Editor tabs.restoreTabsOnStartup` - Restore tabs from last session

**Keyboard Shortcuts** (Keystrokes.*)
Default: Ctrl+N (new), Ctrl+O (open), Ctrl+S (save), Ctrl+D (crypt), F7 (forget), Ctrl+P (properties), Ctrl+E (extensions), Ctrl+L (log), Ctrl+A (about), Ctrl+Q (exit)

### Application Directories
- **INSTALL_DIR**: Installation directory (null if not installed via installer)
- **SETTINGS_DIR**: `~/.CryptText/` (user settings)
- **EXTENSIONS_DIR**: `~/.CryptText/extensions/` (extension jars)
- **UPDATE_SOURCES_FILE**: `update_sources.json` (optional, for extension discovery)

### Resource Files
- **logging.properties** - Java logging configuration
- **file_header.txt** - Encrypted file wrapper header
- **ReleaseNotes.txt** - Version history
- **images/** - Icons and logos

## 10. Code Conventions and Patterns

### Design Patterns Used

1. **Singleton**: MainWindow, AppConfig, CryptTextExtensionManager
   - Private constructor + static getInstance() method
   
2. **Observer/Listener**: Text events, UI updates
   - Thread-safe with CopyOnWriteArrayList
   
3. **Action Pattern**: UI commands
   - Extend javax.swing.AbstractAction
   - Integrated with KeyStrokeManager
   
4. **Manager/Controller**: TextManager, MenuManager, RecentFilesManager
   
5. **Model-View Separation**: Clear separation of concerns

### Naming Conventions
- **Packages**: `ca.corbett.crypttext.[subsystem]` (lowercase)
- **Classes**: PascalCase
- **Methods**: camelCase
- **Constants**: UPPER_SNAKE_CASE
- **Properties**: Hierarchical dot notation

### Exception Handling
- **DecryptionFailedException**: Password/data validation failures
- **VetoException**: Extension veto signaling
- **Logging**: java.util.logging.Logger

### Thread Safety
- All Swing operations on EDT
- SwingUtilities.invokeLater() for cross-thread updates
- CopyOnWriteArrayList for listener collections

### Immutability
- Text class is immutable
- Models represent immutable snapshots
- Modification returns new instances or fires listener events

## 11. CI/Build Files

**Status**: No GitHub Actions configured

To add CI/CD, create:
- `.github/workflows/maven.yml` for build/test automation
- Consider separate workflow for Linux installer packaging

## 12. README Content Summary

- **Overview**: User-friendly encrypted text editor
- **Installation**: Linux installer tarball or Maven source build
- **User Guide**: Editor usage, encryption workflow, configuration
- **Features**: Look & Feel, editor settings, tab settings, keyboard shortcuts
- **Built-in Extensions**: StatusBar, DirTree
- **Built-in Windows/Actions**: Log Console
- **Extension Development**: Reference to swing-extras API
- **Repository**: GitHub issues for bugs/features
- **License**: MIT License

## 13. Version Class Structure

```java
public class Version {
    // Application Info
    public static String NAME = "CryptText";
    public static String VERSION = "1.0";
    public static String FULL_NAME = "CryptText 1.0";
    public static String COPYRIGHT = "Copyright © 2026 Steve Corbett";
    public static String PROJECT_URL = "https://github.com/scorbo2/crypttext";
    public static String LICENSE = "https://opensource.org/license/mit";
    
    // Directory Paths (initialized in static block)
    public static final File INSTALL_DIR;          // null if not installed
    public static final File SETTINGS_DIR;         // ~/.CryptText/
    public static final File UPDATE_SOURCES_FILE;  // null if not provided
    public static final File EXTENSIONS_DIR;       // ~/.CryptText/extensions/
    
    public static AboutInfo getAboutInfo();
}
```

Static initialization reads system properties and creates directories as needed.

## 14. AppConfig Architecture

```java
public class AppConfig extends AppProperties<CryptTextExtension> {
    public static AppConfig getInstance();
    public String getLookAndFeelClassName();
    public boolean isSingleInstanceEnabled();
    public static String peek(String propName);
    public Action getNewTabAction();
    // ... action getters for all commands
    public List<KeyStrokeProperty> getKeyStrokeProperties();
}
```

**Key Design Points**:
- Extends swing-extras AppProperties
- Generic over CryptTextExtension
- Centralizes all UI actions
- Integrates with extension manager
- Properties loaded from `~/.CryptText/CryptText.props`

**Configuration Flow**: Main.main() → AppConfig.getInstance().load() → Reads properties file → Creates/updates actions → Coordinates with extensions

## 15. Important Notes for Development

### Swing Development
- All GUI updates on EDT via SwingUtilities.invokeLater()
- JTextPane for rich text editing in tabs
- Look & Feel changes require LAF switch + UI reload

### Encryption
- Passwords are memory-sensitive
- Decryption failures = wrong password OR corrupted data
- AES-GCM provides authenticated encryption to detect tampering
- Argon2 intentionally slow for security

### Extension Development
- Loaded from jar files in extensions directory
- Load order matters (alphabetically by default)
- Can veto file operations
- Have access to MainWindow static methods

### Listener Pattern
- Thread-safe (CopyOnWriteArrayList)
- Extensions can subscribe to TextWillLoad/TextWillSave for veto hooks
- UI listeners on EDT, data listeners may be cross-thread

### File I/O
- TextManager handles actual file reading/writing
- Encrypted files wrapped with header before base64 encoding
- Recent files list in `~/.CryptText/recent_files`
- Tab state persisted

## 16. Useful Commands

```bash
mvn clean package                           # Build project
mvn test                                    # Run all tests
mvn test -Dtest=CryptUtilTest              # Run specific test
mvn clean package && java -jar target/crypttext-1.0.jar  # Build & run
find src -name "*.java" | xargs wc -l      # Count lines of code
```

## 17. Codebase Metrics

- **Total Lines of Code**: ~4,084 (source only)
- **Java Version**: 17
- **Test Classes**: 3 (CryptUtilTest, TextManagerTest, RecentFilesManagerTest)
- **Main Packages**: 8
- **Built-in Extensions**: 3 (DirTree, StatusBar, Test)

---

*For latest information: https://github.com/scorbo2/crypttext*
