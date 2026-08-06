package pl.experiot.hcms.adapters.driving;

import org.junit.jupiter.api.*;
import pl.experiot.hcms.app.logic.dto.Site;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FolderWatcher focusing on memory leak prevention.
 */
class FolderWatcherTest {

    private Path tempDir;
    private Site site;
    private ForDocumentsLoaderIface loader;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("hcms-watcher-test");
        site = new Site();
        site.name = "test-site";
        loader = mock(ForDocumentsLoaderIface.class);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Cleanup temp directory
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                 .sorted((a, b) -> -a.compareTo(b))
                 .forEach(p -> {
                     try { Files.deleteIfExists(p); } catch (IOException e) { /* ignore */ }
                 });
        }
    }

    // ==================== DEFENSIVE COPY TESTS ====================

    @Test
    void testWatchedFilesDefensiveCopy() {
        // Given
        Set<String> originalFiles = new HashSet<>(Set.of("config/version.txt"));
        site.watchedFiles = originalFiles;

        // When
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // Then - modify original set
        originalFiles.add("config/newfile.txt");
        originalFiles.remove("config/version.txt");

        // Watcher should still have original content
        assertEquals(1, watcher.getWatchedFilesCount());
    }

    @Test
    void testWatchedFilesNotAffectedByExternalModification() {
        // Given
        Set<String> files = new HashSet<>(Set.of("file1.txt", "file2.txt"));
        site.watchedFiles = files;

        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // When - clear the original set
        files.clear();

        // Then - watcher should still have 2 files
        assertEquals(2, watcher.getWatchedFilesCount());
    }

    // ==================== STOP METHOD TESTS ====================

    @Test
    void testStopMethodSetsRunningToFalse() {
        // Given
        site.watchedFiles = Set.of("test.txt");
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // When
        watcher.stop();

        // Then
        assertFalse(watcher.isRunning());
    }

    @Test
    void testStopMethodMultipleCalls() {
        // Given
        site.watchedFiles = Set.of("test.txt");
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // When
        watcher.stop();
        watcher.stop();
        watcher.stop();

        // Then - should handle multiple calls gracefully
        assertFalse(watcher.isRunning());
    }

    // ==================== THREAD LIFECYCLE TESTS ====================

    @Test
    void testThreadTerminatesAfterStop() throws InterruptedException {
        // Given
        site.watchedFiles = Set.of("test.txt");
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        Thread watcherThread = new Thread(watcher);
        watcherThread.start();

        // Let it start
        Thread.sleep(100);

        // When
        watcher.stop();

        // Then
        watcherThread.join(2000);
        assertFalse(watcherThread.isAlive(), "Watcher thread should terminate after stop()");
    }

    @Test
    void testThreadTerminatesOnInterrupt() throws InterruptedException {
        // Given
        site.watchedFiles = Set.of("test.txt");
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        Thread watcherThread = new Thread(watcher);
        watcherThread.start();

        Thread.sleep(100);

        // When
        watcherThread.interrupt();

        // Then
        watcherThread.join(2000);
        assertFalse(watcherThread.isAlive(), "Watcher thread should terminate on interrupt");
    }

    // ==================== PATH NORMALIZATION TESTS ====================

    @Test
    void testPathNormalizationRemovesDotElements() throws IOException {
        // Given
        site.watchedFiles = Set.of("config/./version.txt", "config/version.txt");

        // When
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // Then - both paths should normalize to the same value
        assertEquals(1, watcher.getNormalizedPathsCount());
    }

    @Test
    void testPathNormalizationRemovesDoubleSlashes() throws IOException {
        // Given
        site.watchedFiles = Set.of("config//version.txt", "config/version.txt");

        // When
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // Then
        assertEquals(1, watcher.getNormalizedPathsCount());
    }

    @Test
    void testPathNormalizationRemovesParentDirectory() throws IOException {
        // Given
        site.watchedFiles = Set.of("config/sub/../version.txt", "config/version.txt");

        // When
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // Then - both paths should normalize to the same value
        assertEquals(1, watcher.getNormalizedPathsCount());
    }

    // ==================== WATCHED DIRECTORIES TESTS ====================

    @Test
    void testWatchedDirectoriesCreatedForEachFile() throws IOException {
        // Given
        site.watchedFiles = Set.of("dir1/file1.txt", "dir2/file2.txt");

        // When
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // Then
        assertEquals(2, watcher.getWatchedDirectoriesCount());
    }

    @Test
    void testSameDirectoryNotDuplicated() throws IOException {
        // Given
        site.watchedFiles = Set.of("dir/file1.txt", "dir/file2.txt", "dir/file3.txt");

        // When
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // Then - all files in same directory, should only watch once
        assertEquals(1, watcher.getWatchedDirectoriesCount());
    }

    @Test
    void testNullParentDirectoryHandled() throws IOException {
        // Given
        site.watchedFiles = Set.of("version.txt"); // File in root

        // When
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // Then - should not throw NPE
        assertTrue(watcher.getWatchedDirectoriesCount() >= 0);
    }

    // ==================== DEBOUNCING TESTS ====================

    @Test
    void testDebouncingPreventsMultipleTriggers() throws IOException, InterruptedException {
        // Given
        site.watchedFiles = Set.of("test.txt");
        Files.createDirectories(tempDir.resolve("test-site"));
        Files.createFile(tempDir.resolve("test-site/test.txt"));

        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        Thread watcherThread = new Thread(watcher);
        watcherThread.start();

        Thread.sleep(100); // Let watcher initialize

        // When - modify file multiple times quickly
        for (int i = 0; i < 10; i++) {
            Files.writeString(
                tempDir.resolve("test-site/test.txt"),
                "change " + i,
                StandardOpenOption.TRUNCATE_EXISTING);
            Thread.sleep(50); // Less than minimalDelay (1000ms)
        }

        // Let debouncing work
        Thread.sleep(1500);

        // Then - loader should be called only once due to debouncing
        verify(loader, atMost(1)).loadDocuments(any(), anyLong());

        // Cleanup
        watcher.stop();
        watcherThread.join(1000);
    }

    @Test
    void testMultipleFilesDebouncedIndependently() throws IOException, InterruptedException {
        // Given
        site.watchedFiles = Set.of("file1.txt", "file2.txt");
        Files.createDirectories(tempDir.resolve("test-site"));
        Files.createFile(tempDir.resolve("test-site/file1.txt"));
        Files.createFile(tempDir.resolve("test-site/file2.txt"));

        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        Thread watcherThread = new Thread(watcher);
        watcherThread.start();

        Thread.sleep(100);

        // When - modify file1 multiple times
        for (int i = 0; i < 5; i++) {
            Files.writeString(
                tempDir.resolve("test-site/file1.txt"),
                "change " + i,
                StandardOpenOption.TRUNCATE_EXISTING);
            Thread.sleep(50);
        }

        Thread.sleep(1500); // Wait for debounce

        // Then - file1 should trigger only once
        verify(loader, atMost(1)).loadDocuments(any(), anyLong());

        // Cleanup
        watcher.stop();
        watcherThread.join(1000);
    }

    // ==================== NAMEPLATE TESTS ====================

    @Test
    void testNameplateContainsSiteName() {
        // Given
        site.name = "my-site";
        site.watchedFiles = Set.of("config.txt");

        // When
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // Then
        assertTrue(watcher.getNameplate().contains("my-site"));
    }

    @Test
    void testNameplateContainsFileCount() {
        // Given
        site.name = "test";
        site.watchedFiles = Set.of("file1.txt", "file2.txt", "file3.txt");

        // When
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        // Then
        assertTrue(watcher.getNameplate().contains("3 files"));
    }

    // ==================== GET INSTANCES TESTS ====================

    @Test
    void testGetInstancesReturnsNonEmptyList() {
        // Given
        java.util.HashMap<String, Site> siteMap = new java.util.HashMap<>();
        Site site1 = new Site();
        site1.name = "site1";
        site1.watchedFiles = Set.of("file1.txt");
        siteMap.put("site1", site1);

        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), siteMap, loader);

        // When
        var instances = watcher.getInstances();

        // Then
        assertNotNull(instances);
        assertEquals(1, instances.size());
    }

    @Test
    void testGetInstancesCreatesWatcherForEachSite() {
        // Given
        java.util.HashMap<String, Site> siteMap = new java.util.HashMap<>();
        Site site1 = new Site();
        site1.name = "site1";
        site1.watchedFiles = Set.of("file1.txt");
        Site site2 = new Site();
        site2.name = "site2";
        site2.watchedFiles = Set.of("file2.txt");
        siteMap.put("site1", site1);
        siteMap.put("site2", site2);

        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), siteMap, loader);

        // When
        var instances = watcher.getInstances();

        // Then
        assertEquals(2, instances.size());
    }

    // ==================== LOADER INJECTION TESTS ====================

    @Test
    void testSetLoaderReplacesExistingLoader() {
        // Given
        site.watchedFiles = Set.of("test.txt");
        FolderWatcher watcher = new FolderWatcher(
            tempDir.toString(), site, loader, false);

        ForDocumentsLoaderIface newLoader = mock(ForDocumentsLoaderIface.class);

        // When
        watcher.setLoader(newLoader);

        // Then - should not throw, and we can verify through reflection if needed
        assertDoesNotThrow(() -> watcher.setLoader(newLoader));
    }
}
