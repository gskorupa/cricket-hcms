package pl.experiot.hcms.app.logic;

import org.junit.jupiter.api.*;
import pl.experiot.hcms.adapters.driving.FolderWatcher;
import pl.experiot.hcms.app.logic.dto.Site;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Memory leak prevention tests for DocumentLogic.
 * Focuses on proper cleanup of watcher resources.
 */
class DocumentLogicMemoryLeakTest {

    private Path tempDir;
    private DocumentLogic documentLogic;
    private ForDocumentsLoaderIface loader;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("hcms-doclogic-test");
        documentLogic = new DocumentLogic();
        loader = mock(ForDocumentsLoaderIface.class);
        
        // Mock the loader
        documentLogic.loader = loader;
    }

    @AfterEach
    void tearDown() throws IOException {
        // Cleanup
        if (documentLogic != null) {
            documentLogic.shutdown();
        }
        
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                 .sorted((a, b) -> -a.compareTo(b))
                 .forEach(p -> {
                     try { Files.deleteIfExists(p); } catch (IOException e) { /* ignore */ }
                 });
        }
    }

    // ==================== SHUTDOWN TESTS ====================

    @Test
    void testShutdownClearsEmptyLists() {
        // Given - empty state
        
        // When
        documentLogic.shutdown();
        
        // Then - should not throw
        assertNotNull(documentLogic.getWatcherExecutors());
        assertNotNull(documentLogic.getActiveWatchers());
    }

    @Test
    void testShutdownWithManualWatcherSetup() {
        // Given - manually create and add watchers
        documentLogic.root = tempDir.toString();
        documentLogic.watcherType = "filesystem";
        
        HashMap<String, Site> siteMap = new HashMap<>();
        Site site1 = new Site();
        site1.name = "site1";
        site1.watchedFiles = new HashSet<>();
        site1.watchedFiles.add("config.txt");
        siteMap.put("site1", site1);
        
        // Create watcher
        pl.experiot.hcms.app.ports.driving.ForChangeWatcherIface mainWatcher = 
            documentLogic.createWatcher(loader, siteMap);
        
        // Get instances from the watcher
        java.util.List<pl.experiot.hcms.app.ports.driving.ForChangeWatcherIface> instances = 
            mainWatcher.getInstances();
        
        // Manually add to activeWatchers and executors via reflection
        try {
            java.lang.reflect.Field activeWatchersField = DocumentLogic.class.getDeclaredField("activeWatchers");
            activeWatchersField.setAccessible(true);
            ((java.util.List<pl.experiot.hcms.app.ports.driving.ForChangeWatcherIface>) 
                activeWatchersField.get(documentLogic)).addAll(instances);
            
            java.lang.reflect.Field executorsField = DocumentLogic.class.getDeclaredField("watcherExecutors");
            executorsField.setAccessible(true);
            for (int i = 0; i < instances.size(); i++) {
                java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
                ((java.util.List<java.util.concurrent.ExecutorService>) 
                    executorsField.get(documentLogic)).add(executor);
            }
        } catch (Exception e) {
            fail("Failed to setup test: " + e.getMessage());
        }
        
        // Verify setup
        assertEquals(instances.size(), documentLogic.getActiveWatchers().size());
        assertEquals(instances.size(), documentLogic.getWatcherExecutors().size());
        
        // When
        documentLogic.shutdown();
        
        // Then
        assertTrue(documentLogic.getWatcherExecutors().isEmpty());
        assertTrue(documentLogic.getActiveWatchers().isEmpty());
    }

    @Test
    void testShutdownIsIdempotent() {
        // Given
        
        // When - call shutdown multiple times
        documentLogic.shutdown();
        documentLogic.shutdown();
        documentLogic.shutdown();
        
        // Then - should not throw
        assertDoesNotThrow(() -> documentLogic.shutdown());
    }

    @Test
    void testShutdownStopsFolderWatcher() throws InterruptedException {
        // Given
        documentLogic.root = tempDir.toString();
        documentLogic.watcherType = "filesystem";
        
        HashMap<String, Site> siteMap = new HashMap<>();
        Site site = new Site();
        site.name = "test-site";
        site.watchedFiles = Set.of("test.txt");
        siteMap.put("test-site", site);
        
        pl.experiot.hcms.app.ports.driving.ForChangeWatcherIface watcher = 
            documentLogic.createWatcher(loader, siteMap);
        
        // Start the watcher in a thread
        Thread watcherThread = new Thread((Runnable) watcher);
        watcherThread.start();
        
        Thread.sleep(100); // Let it start
        
        // When - stop the watcher
        watcher.stop();
        watcherThread.join(2000);
        
        // Then
        assertFalse(watcherThread.isAlive());
    }

    // ==================== SITE MAP TESTS ====================

    @Test
    void testSiteMapPopulatesWatchedFiles() {
        // Given
        documentLogic.watchedFile = "config.txt;version.txt";
        documentLogic.sites = "site1;site2";
        documentLogic.assets = "assets1;assets2";
        documentLogic.excludes = "exclude1;exclude2";
        documentLogic.indexFiles = "index1.md;index2.md";
        documentLogic.hcmsServiceUrl = "url1;url2";
        documentLogic.hcmsFileApi = "api1;api2";
        
        // When
        HashMap<String, Site> siteMap = documentLogic.getSiteMapForTesting();
        
        // Then
        assertEquals(2, siteMap.size());
        
        Site site1 = siteMap.get("site1");
        assertNotNull(site1);
        assertNotNull(site1.watchedFiles);
        assertTrue(site1.watchedFiles.contains("config.txt"));
        
        Site site2 = siteMap.get("site2");
        assertNotNull(site2);
        assertNotNull(site2.watchedFiles);
        assertTrue(site2.watchedFiles.contains("version.txt"));
    }

    @Test
    void testSiteMapCreatesNewSetForEachSite() {
        // Given
        documentLogic.watchedFile = "config.txt;version.txt";
        documentLogic.sites = "site1;site2";
        documentLogic.assets = "assets1;assets2";
        documentLogic.excludes = "exclude1;exclude2";
        documentLogic.indexFiles = "index1.md;index2.md";
        documentLogic.hcmsServiceUrl = "url1;url2";
        documentLogic.hcmsFileApi = "api1;api2";
        
        // When
        HashMap<String, Site> siteMap = documentLogic.getSiteMapForTesting();
        
        // Then - each site should have its own Set
        Site site1 = siteMap.get("site1");
        Site site2 = siteMap.get("site2");
        
        assertNotNull(site1.watchedFiles);
        assertNotNull(site2.watchedFiles);
        assertNotSame(site1.watchedFiles, site2.watchedFiles,
            "Each site should have its own Set instance");
    }

    // ==================== WATCHER FACTORY TESTS ====================

    @Test
    void testFilesystemWatcherTypeCreatesFolderWatcher() {
        // Given
        documentLogic.watcherType = "filesystem";
        
        // When
        var watcher = documentLogic.createWatcher(mock(ForDocumentsLoaderIface.class), new HashMap<>());
        
        // Then
        assertTrue(watcher instanceof FolderWatcher,
            "Filesystem watcher type should create FolderWatcher");
    }

    @Test
    void testDummyWatcherTypeCreatesDummyWatcher() {
        // Given
        documentLogic.watcherType = "dummy";
        
        // When
        var watcher = documentLogic.createWatcher(mock(ForDocumentsLoaderIface.class), new HashMap<>());
        
        // Then
        assertNotNull(watcher,
            "Watcher should be created");
    }

}
