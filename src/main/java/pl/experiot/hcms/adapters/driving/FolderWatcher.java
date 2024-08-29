package pl.experiot.hcms.adapters.driving;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jboss.logging.Logger;

import pl.experiot.hcms.app.logic.Site;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;
import pl.experiot.hcms.app.ports.driving.ForChangeWatcherIface;

public class FolderWatcher implements Runnable, ForChangeWatcherIface {

    Logger logger = Logger.getLogger(FolderWatcher.class);

    private ForDocumentsLoaderIface loader;
    private HashMap<String, Site> siteMap;
    private String watchedFile;
    private boolean mapImplementation;
    private Site site = null;

    public FolderWatcher(HashMap<String, Site> siteMap, ForDocumentsLoaderIface loader) {
        this.siteMap = siteMap;
        this.loader = loader;
        logger.info("Creating FolderWatcher "+getClass().getSimpleName());
    }

    public FolderWatcher(
            Site site,
            ForDocumentsLoaderIface loader,
            boolean mapImplementation) {
        this.site = site;
        this.loader = loader;
        this.watchedFile = site.watchedFile;
        this.mapImplementation = mapImplementation;
    }

    @Override
    public void run() {
        Path path = Path.of(site.name);
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
                    loader.loadDocuments(site, System.currentTimeMillis());
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error accessing watched file or folder: " + e.getMessage());
            logger.error("Watcher stopped");
        }
    }

    @Override
    public void setLoader(ForDocumentsLoaderIface loader) {
        this.loader = loader;
    }

    @Override
    public List<ForChangeWatcherIface> getInstances() {
        logger.info("Creating instances of FolderWatcher "+getClass().getSimpleName());
        ArrayList<ForChangeWatcherIface> instances = new ArrayList<>();
        siteMap.keySet().forEach(siteName -> {
            Site site = siteMap.get(siteName);
            logger.info("instance "+site.name);
            instances.add(new FolderWatcher(site, loader, mapImplementation));
        });
        return instances;
    }

    @Override
    public String getNameplate() {
        return getClass().getSimpleName() + " for " + watchedFile;
    }

}
