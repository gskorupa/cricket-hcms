package pl.experiot.hcms.adapters.driven.loader.fs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LoadStatistics singleton.
 */
class LoadStatisticsTest {
    
    @BeforeEach
    void setUp() {
        // Reset statistics before each test
        LoadStatistics.getInstance().reset();
    }
    
    @AfterEach
    void tearDown() {
        // Reset statistics after each test
        LoadStatistics.getInstance().reset();
    }
    
    @Test
    void testSingletonInstance() {
        LoadStatistics instance1 = LoadStatistics.getInstance();
        LoadStatistics instance2 = LoadStatistics.getInstance();
        assertSame(instance1, instance2, "LoadStatistics should be a singleton");
    }
    
    @Test
    void testFilesRead() {
        LoadStatistics stats = LoadStatistics.getInstance();
        assertEquals(0, stats.getTotalFilesRead());
        
        stats.incrementFilesRead(5);
        assertEquals(5, stats.getTotalFilesRead());
        
        stats.incrementFilesRead(3);
        assertEquals(8, stats.getTotalFilesRead());
    }
    
    @Test
    void testDocumentsSaved() {
        LoadStatistics stats = LoadStatistics.getInstance();
        assertEquals(0, stats.getTotalDocumentsSaved());
        
        stats.incrementDocumentsSaved();
        assertEquals(1, stats.getTotalDocumentsSaved());
        
        stats.incrementDocumentsSaved();
        stats.incrementDocumentsSaved();
        assertEquals(3, stats.getTotalDocumentsSaved());
    }
    
    @Test
    void testSkippedFoldersAndFiles() {
        LoadStatistics stats = LoadStatistics.getInstance();
        assertEquals(0, stats.getSkippedFolders());
        assertEquals(0, stats.getSkippedFiles());
        
        stats.incrementSkippedFolders(2);
        stats.incrementSkippedFiles(4);
        
        assertEquals(2, stats.getSkippedFolders());
        assertEquals(4, stats.getSkippedFiles());
    }
    
    @Test
    void testDeletedDocuments() {
        LoadStatistics stats = LoadStatistics.getInstance();
        assertEquals(0, stats.getDeletedDocuments());
        
        stats.incrementDeletedDocuments(10);
        assertEquals(10, stats.getDeletedDocuments());
        
        stats.incrementDeletedDocuments(5);
        assertEquals(15, stats.getDeletedDocuments());
    }
    
    @Test
    void testDocumentsSentToTranslation() {
        LoadStatistics stats = LoadStatistics.getInstance();
        Map<String, Integer> translations = stats.getDocumentsSentToTranslation();
        assertTrue(translations.isEmpty());
        
        stats.incrementDocumentsSentToTranslation("en");
        stats.incrementDocumentsSentToTranslation("en");
        stats.incrementDocumentsSentToTranslation("pl");
        
        translations = stats.getDocumentsSentToTranslation();
        assertEquals(2, translations.get("en"));
        assertEquals(1, translations.get("pl"));
    }
    
    @Test
    void testTranslationApiErrors() {
        LoadStatistics stats = LoadStatistics.getInstance();
        assertEquals(0, stats.getTranslationApiErrors());
        
        stats.incrementTranslationApiErrors();
        stats.incrementTranslationApiErrors();
        assertEquals(2, stats.getTranslationApiErrors());
    }
    
    @Test
    void testReset() {
        LoadStatistics stats = LoadStatistics.getInstance();
        
        stats.incrementFilesRead(10);
        stats.incrementDocumentsSaved();
        stats.incrementSkippedFolders(3);
        stats.incrementSkippedFiles(5);
        stats.incrementDeletedDocuments(2);
        stats.incrementDocumentsSentToTranslation("en");
        stats.incrementTranslationApiErrors();
        
        stats.reset();
        
        assertEquals(0, stats.getTotalFilesRead());
        assertEquals(0, stats.getTotalDocumentsSaved());
        assertEquals(0, stats.getSkippedFolders());
        assertEquals(0, stats.getSkippedFiles());
        assertEquals(0, stats.getDeletedDocuments());
        assertTrue(stats.getDocumentsSentToTranslation().isEmpty());
        assertEquals(0, stats.getTranslationApiErrors());
    }
    
    @Test
    void testToLogMessage() {
        LoadStatistics stats = LoadStatistics.getInstance();
        
        stats.incrementFilesRead(10);
        stats.incrementDocumentsSaved();
        stats.incrementSkippedFolders(2);
        stats.incrementSkippedFiles(3);
        stats.incrementDeletedDocuments(5);
        stats.incrementDocumentsSentToTranslation("en");
        stats.incrementDocumentsSentToTranslation("pl");
        stats.incrementTranslationApiErrors();
        
        String logMessage = stats.toLogMessage();
        
        assertNotNull(logMessage);
        assertTrue(logMessage.contains("=== Load Statistics ==="));
        assertTrue(logMessage.contains("Total files read: 10"));
        assertTrue(logMessage.contains("Total documents saved to database: 1"));
        assertTrue(logMessage.contains("Skipped folders: 2"));
        assertTrue(logMessage.contains("Skipped files: 3"));
        assertTrue(logMessage.contains("Deleted documents detected: 5"));
        assertTrue(logMessage.contains("en: 1"));
        assertTrue(logMessage.contains("pl: 1"));
        assertTrue(logMessage.contains("Translation API errors: 1"));
    }
}
