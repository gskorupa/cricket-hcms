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

    private String root;
    private String watchedFile;
    private String site;
    private String[] sitesList;
    private boolean mapImplementation;


    public FolderWatcher(
            HashMap<String, Site> siteMap,
            String root,
            String watchedFile,
            ForDocumentsLoaderIface loader,
            String site,
            String[] sitesList,
            boolean mapImplementation) {
        this.loader = loader;
        this.watchedFile = watchedFile;
        this.root = root;
        this.site = site;
        this.sitesList = sitesList;
        this.mapImplementation = mapImplementation;
        this.siteMap = siteMap;
    }

    @Override
    public void run() {
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
                    if (mapImplementation) {
                        // reloading all sites regardless of the event context
                        for (int i = 0; i < sitesList.length; i++) {
                            loader.loadDocuments(sitesList[i], siteMap, i == 0, i == sitesList.length - 1,
                                    System.currentTimeMillis());
                        }
                    } else if (event.context().toString().equals(watchedFile)) {
                        loader.loadDocuments(site, siteMap, true, true, System.currentTimeMillis());
                    }
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
        this.loader=loader;
    }

    @Override
    public List<ForChangeWatcherIface> getInstances(HashMap<String,Site> siteMap) {
        this.siteMap=siteMap;
        ArrayList<ForChangeWatcherIface> instances=new ArrayList<>();
        siteMap.keySet().forEach(siteName -> {
            Site site=siteMap.get(siteName);
            String root=site.name;
            String fileToWatch = site.watchedFile;
            FolderWatcher watcher = new FolderWatcher(siteMap, root, fileToWatch, loader, siteName, sitesList, mapImplementation);
            instances.add(watcher);
        });
        return instances;
    }

}
