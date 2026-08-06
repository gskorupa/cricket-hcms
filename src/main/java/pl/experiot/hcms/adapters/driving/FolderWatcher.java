package pl.experiot.hcms.adapters.driving;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import pl.experiot.hcms.app.logic.dto.Site;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;
import pl.experiot.hcms.app.ports.driving.ForChangeWatcherIface;

public class FolderWatcher implements Runnable, ForChangeWatcherIface {

    Logger logger = Logger.getLogger(FolderWatcher.class);

    private ForDocumentsLoaderIface loader;
    private HashMap<String, Site> siteMap;
    private Set<String> watchedFiles;
    private boolean mapImplementation;
    private Site site = null;
    private String root;
    private Set<Path> watchedDirectories;
    private Map<Path, Set<String>> directoryToFilesMap;
    private Set<Path> normalizedWatchedPaths;
    private volatile boolean running = true;

    private long minimalDelay = 1000;

    public FolderWatcher(String root, HashMap<String, Site> siteMap, ForDocumentsLoaderIface loader) {
        this.siteMap = siteMap;
        this.loader = loader;
        this.root = root;
        logger.info("Creating FolderWatcher " + getClass().getSimpleName());
    }

    public FolderWatcher(
            String root,
            Site site,
            ForDocumentsLoaderIface loader,
            boolean mapImplementation) {
        this.root = root;
        this.site = site;
        this.loader = loader;
        // Defensive copy to prevent external modifications
        this.watchedFiles = new HashSet<>(site.watchedFiles);
        this.mapImplementation = mapImplementation;
        this.watchedDirectories = new HashSet<>();
        this.directoryToFilesMap = new HashMap<>();
        this.normalizedWatchedPaths = new HashSet<>();
        
        // Build directory-to-files mapping and normalize all watched paths once
        for (String filePath : watchedFiles) {
            Path fullPath = Paths.get(root, site.name, filePath).normalize();
            normalizedWatchedPaths.add(fullPath);
            
            Path parentDir = fullPath.getParent();
            if (parentDir != null) {
                watchedDirectories.add(parentDir);
                directoryToFilesMap.computeIfAbsent(parentDir, k -> new HashSet<>()).add(filePath);
                logger.info("Will monitor directory: " + parentDir + " for file: " + filePath);
            }
        }

        logger.info("Monitoring " + watchedFiles.size() + " file(s) for site " + site.name);
    }

    @Override
    public void run() {
        HashMap<String, Long> contexts = new HashMap<>();
        FileSystem fs = Path.of(root).getFileSystem();
        try (WatchService service = fs.newWatchService();) {
            // Register all directories that contain watched files
            for (Path dir : watchedDirectories) {
                dir.register(
                        service,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                logger.info("Registered watch on directory: " + dir);
            }
            
            WatchKey key;
            while (running && (key = service.take()) != null) {
                try {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        String changedFile = event.context().toString();
                        // Resolve the full path of the changed file
                        Path dir = (Path) key.watchable();
                        Path changedPath = dir.resolve(changedFile).normalize();
                        
                        // Check if this changed file is one we're watching using pre-normalized paths
                        for (Path watchedPath : normalizedWatchedPaths) {
                            if (changedPath.equals(watchedPath)) {
                                // Find the original file name for this path to use in debouncing
                                String watchedFile = watchedFiles.stream()
                                    .filter(f -> watchedPath.endsWith(f))
                                    .findFirst()
                                    .orElse(changedFile);
                                
                                long now = System.currentTimeMillis();
                                long last = contexts.getOrDefault(watchedFile, 0L);
                                if (now - last < minimalDelay) {
                                    logger.info(
                                            "watcherevent " + event.kind() + " -> " + changedFile + " already processed");
                                    continue;
                                } else {
                                    contexts.put(watchedFile, now);
                                    logger.info("watcherevent " + event.kind() + " -> " + changedFile + " "
                                            + System.currentTimeMillis());
                                    loader.loadDocuments(site, System.currentTimeMillis());
                                }
                            }
                        }
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        key.cancel();
                        logger.warn("Watch key no longer valid for directory: " + key.watchable());
                    }
                } catch (Exception e) {
                    logger.error("Error processing watch events: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("IO Error accessing watched file or folder: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.info("Watcher thread interrupted, stopping gracefully");
            Thread.currentThread().interrupt();
        } finally {
            running = false;
            logger.info("Watcher for site " + site.name + " stopped");
        }
    }

    @Override
    public void setLoader(ForDocumentsLoaderIface loader) {
        this.loader = loader;
    }

    /**
     * Stops the watcher thread gracefully.
     * This allows the WatchService to close properly via try-with-resources.
     */
    public void stop() {
        running = false;
        logger.info("Stop signal sent to watcher for site " + (site != null ? site.name : "unknown"));
    }

    // ==================== Test helper methods ====================
    
    /**
     * Returns the number of watched files (for testing).
     */
    int getWatchedFilesCount() {
        return watchedFiles != null ? watchedFiles.size() : 0;
    }

    /**
     * Returns whether the watcher is running (for testing).
     */
    boolean isRunning() {
        return running;
    }

    /**
     * Returns the number of normalized watched paths (for testing).
     */
    int getNormalizedPathsCount() {
        return normalizedWatchedPaths != null ? normalizedWatchedPaths.size() : 0;
    }

    /**
     * Returns the number of watched directories (for testing).
     */
    int getWatchedDirectoriesCount() {
        return watchedDirectories != null ? watchedDirectories.size() : 0;
    }

    @Override
    public List<ForChangeWatcherIface> getInstances() {
        logger.info("Creating instances of FolderWatcher " + getClass().getSimpleName());
        ArrayList<ForChangeWatcherIface> instances = new ArrayList<>();
        siteMap.keySet().forEach(siteName -> {
            Site site = siteMap.get(siteName);
            logger.info("instance " + site.name);
            instances.add(new FolderWatcher(root, site, loader, mapImplementation));
        });
        return instances;
    }

    @Override
    public String getNameplate() {
        return getClass().getSimpleName() + " for " + site.name + " (" + watchedFiles.size() + " files)";
    }

}
