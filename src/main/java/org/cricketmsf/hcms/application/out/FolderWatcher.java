package org.cricketmsf.hcms.application.out;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.jboss.logging.Logger;

public class FolderWatcher implements Runnable{

    Logger logger = Logger.getLogger(FolderWatcher.class);

    private String root;
    private String watchedFile;
    private DocumentRepositoryLoader loader;

    public FolderWatcher(String root, String watchedFile, DocumentRepositoryLoader loader) {
        this.loader = loader;
        this.watchedFile = watchedFile;
        this.root = root;
    }

    @Override
    public void run() {
        //logger.info("Starting watcher for " + watchedFile + " in " + root);
        Path path = Path.of(root);
        FileSystem fs = path.getFileSystem();
        try (WatchService service = fs.newWatchService();) {
            path.register(
                    service,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            WatchKey key;
            while ((key = service.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    logger.debug(event.kind() + " -> " + event.context());
                    if (event.context().toString().equals(watchedFile)) {
                        loader.loadDocuments("");
                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error accessing watched file or folder: "+e.getMessage());
            logger.error("Watcher stopped");
        }
    }

    
}
