package pl.experiot.hcms.adapters.driven.loader.fs;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to collect and manage loading statistics for document loading process.
 * This is a singleton class to collect statistics across the entire application.
 */
public class LoadStatistics {
    
    // Singleton instance
    private static final LoadStatistics instance = new LoadStatistics();
    
    public static LoadStatistics getInstance() {
        return instance;
    }
    
    // Private constructor to prevent instantiation
    private LoadStatistics() {}
    
    // Total number of files read from filesystem
    private int totalFilesRead = 0;
    
    // Total number of documents saved to database
    private int totalDocumentsSaved = 0;
    
    // Total number of skipped folders
    private int skippedFolders = 0;
    
    // Total number of skipped files
    private int skippedFiles = 0;
    
    // Total number of deleted documents detected (removed from database)
    private int deletedDocuments = 0;
    
    // Number of documents sent to translation, per language version
    private Map<String, Integer> documentsSentToTranslation = new HashMap<>();
    
    // Total number of translation API errors
    private int translationApiErrors = 0;

    public void incrementFilesRead() {
        totalFilesRead++;
    }

    public void incrementDocumentsSaved() {
        totalDocumentsSaved++;
    }

    public void incrementSkippedFolders() {
        skippedFolders++;
    }

    public void incrementSkippedFiles() {
        skippedFiles++;
    }

    public void incrementDeletedDocuments(int count) {
        deletedDocuments += count;
    }

    public void incrementDocumentsSentToTranslation(String language) {
        documentsSentToTranslation.merge(language, 1, Integer::sum);
    }

    public void incrementTranslationApiErrors() {
        translationApiErrors++;
    }
    
    public void incrementFilesRead(int count) {
        totalFilesRead += count;
    }
    
    public void incrementSkippedFiles(int count) {
        skippedFiles += count;
    }
    
    public void incrementSkippedFolders(int count) {
        skippedFolders += count;
    }

    public int getTotalFilesRead() {
        return totalFilesRead;
    }

    public int getTotalDocumentsSaved() {
        return totalDocumentsSaved;
    }

    public int getSkippedFolders() {
        return skippedFolders;
    }

    public int getSkippedFiles() {
        return skippedFiles;
    }

    public int getDeletedDocuments() {
        return deletedDocuments;
    }

    public Map<String, Integer> getDocumentsSentToTranslation() {
        return new HashMap<>(documentsSentToTranslation);
    }

    public int getTranslationApiErrors() {
        return translationApiErrors;
    }

    /**
     * Generates a formatted log message with all statistics.
     */
    public String toLogMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Load Statistics ===\n");
        sb.append("Total files read: ").append(totalFilesRead).append("\n");
        sb.append("Total documents saved to database: ").append(totalDocumentsSaved).append("\n");
        sb.append("Skipped folders: ").append(skippedFolders).append("\n");
        sb.append("Skipped files: ").append(skippedFiles).append("\n");
        sb.append("Deleted documents detected: ").append(deletedDocuments).append("\n");
        
        sb.append("Documents sent to translation by language:\n");
        if (documentsSentToTranslation.isEmpty()) {
            sb.append("  No documents sent to translation\n");
        } else {
            for (Map.Entry<String, Integer> entry : documentsSentToTranslation.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        sb.append("Translation API errors: ").append(translationApiErrors).append("\n");
        sb.append("=== End of Statistics ===");
        
        return sb.toString();
    }

    /**
     * Resets all statistics.
     */
    public void reset() {
        totalFilesRead = 0;
        totalDocumentsSaved = 0;
        skippedFolders = 0;
        skippedFiles = 0;
        deletedDocuments = 0;
        documentsSentToTranslation.clear();
        translationApiErrors = 0;
    }
}
