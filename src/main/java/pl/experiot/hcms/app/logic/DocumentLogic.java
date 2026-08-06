package pl.experiot.hcms.app.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import pl.experiot.hcms.adapters.driving.DummyWatcher;
import pl.experiot.hcms.adapters.driving.FolderWatcher;
import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.logic.dto.Site;
import pl.experiot.hcms.app.ports.driven.ForDocumentRepositoryIface;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;
import pl.experiot.hcms.app.ports.driven.ForTranslatorIface;
import pl.experiot.hcms.app.ports.driving.ForAdministrationIface;
import pl.experiot.hcms.app.ports.driving.ForChangeWatcherIface;
import pl.experiot.hcms.app.ports.driving.ForDocumentsIface;

@ApplicationScoped
public class DocumentLogic implements ForDocumentsIface, ForAdministrationIface {

    @Inject
    Logger logger;

    ForDocumentRepositoryIface repositoryPort;
    ForDocumentsLoaderIface loader;
    ForChangeWatcherIface watcher;
    ForTranslatorIface translator;
    
    // Store executors and watchers for proper shutdown
    private List<ExecutorService> watcherExecutors = new ArrayList<>();
    private List<ForChangeWatcherIface> activeWatchers = new ArrayList<>();

    @ConfigProperty(name = "document.folders.root")
    String root;
    @ConfigProperty(name = "document.watcher.active")
    boolean watcherActive;
    @ConfigProperty(name = "document.watcher.file")
    String watchedFile;
    @ConfigProperty(name = "github.token")
    String githubToken;
    @ConfigProperty(name = "github.repository")
    String githubRepository;
    @ConfigProperty(name = "document.folders.sites")
    String sites;
    @ConfigProperty(name = "document.folders.assets")
    String assets;
    @ConfigProperty(name = "hcms.sevice.url")
    String hcmsServiceUrl;
    @ConfigProperty(name = "hcms.file.api")
    String hcmsFileApi;
    @ConfigProperty(name = "hcms.database.type")
    String databaseType;
    @ConfigProperty(name = "hcms.loader.type")
    String loaderType;
    @ConfigProperty(name = "hcms.watcher.type")
    String watcherType;
    @ConfigProperty(name = "document.folders.indexes")
    String indexFiles;

    @ConfigProperty(name = "document.folders.excluded")
    String excludes;
    @ConfigProperty(name = "document.syntax")
    String syntax; /* "obsidian", "github" */
    @ConfigProperty(name = "document.extension.markdown")
    String markdownFileExtension;
    @ConfigProperty(name = "document.extension.html")
    String htmlFileExtension;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    EventBus bus;

    @Inject
    Configurator2 configurator;

    String queueName = "to-translate";

    private HashMap<String, Site> siteMap = new HashMap<>();

    public boolean ready = false;

    @Override
    @CacheResult(cacheName = "document-list-cache")
    public List<Document> getDocuments(String path, boolean withContent) {
        return repositoryPort.getDocuments(path, withContent);
    }

    @Override
    public List<String> getPaths(String siteName) {
        return repositoryPort.getPaths(siteName);
    }

    @Override
    public List<String> getSiteNames() {
        return repositoryPort.getSiteNames();
    }

    @Override
    @CacheResult(cacheName = "document-cache")
    public Document getDocument(String path) {
        return repositoryPort.getDocument(path);
    }

    void onStart(@Observes StartupEvent ev) {

        // environment variables should be cleaned from non printable characters
        watcherType = watcherType.trim();
        loaderType = loaderType.trim();

        // repository adapter setup
        /*
         * switch (databaseType) {
         * case "h2":
         * repositoryPort = new DocumentRepositoryH2();
         * break;
         * case "map":
         * repositoryPort = new DocumentRepository();
         * break;
         * default:
         * repositoryPort = new DocumentRepositoryH2();
         * }
         */
        repositoryPort = configurator.getRepositoryPort();
        repositoryPort.setEventBus(bus, queueName);
        repositoryPort.init(dataSource);

        // translator adapter setup
        // TODO

        // document loader adapter setup
        /* switch (loaderType.toLowerCase()) {
            case "filesystem":
                loader = new FromFilesystemLoader();
                loader.setAssets(assets);
                loader.setExcludes(excludes);
                loader.setHcmsServiceUrl(hcmsServiceUrl);
                loader.setRoot(root);
                break;
            default:
                loader = new TestDocLoader();
        } */
        //loader= Configurator.getLoaderPort(loaderType, root, assets, excludes, hcmsServiceUrl);
        loader = configurator.getLoaderPort();
        loader.setEventBus(bus, queueName);
        loader.setRepositoryPort(repositoryPort);
        //loader.setSites(sites);
        //loader.setHtmlFileExtension(htmlFileExtension);
        //loader.setMarkdownFileExtension(markdownFileExtension);
        //loader.setSyntax(syntax);

        try {
            siteMap = getSiteMap();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error while reading configuration from environment variables: " + e.getMessage());
            logger.warn("Press Ctrl+C to stop the service");
            System.exit(1);
        }

        // watcher adapter setup
        logger.info("WATCHER TYPE: [" + watcherType + "]");
        watcher = createWatcher(loader, siteMap);
        watcher.setLoader(loader);

        ready = true;

        // start
        long timestamp = System.currentTimeMillis();
        int idx = 0;
        siteMap.values().forEach(site -> {
            logger.info("loading documents of site " + site.name);
            loader.loadDocuments(site.name, siteMap, idx == 0, idx == siteMap.size() - 1, timestamp);
        });

        // Stop any existing watchers before creating new ones to prevent resource leaks
        shutdown();
        
        List<ForChangeWatcherIface> watchers = watcher.getInstances();
        List<ExecutorService> executors = new ArrayList<>();
        for (ForChangeWatcherIface w : watchers) {
            logger.info("WATCHER " + w.getNameplate());
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute((Runnable) w);
            executors.add(executor);
        }
        
        // Store executors and watchers for later shutdown
        this.watcherExecutors = executors;
        this.activeWatchers = watchers;
    }

    /**
     * Creates a watcher instance based on the configured watcher type.
     * Extracted for testability.
     * 
     * @param loader the document loader
     * @param siteMap the site configuration map
     * @return a new watcher instance
     */
    ForChangeWatcherIface createWatcher(ForDocumentsLoaderIface loader, HashMap<String, Site> siteMap) {
        if (watcherType.equalsIgnoreCase("filesystem")) {
            return new FolderWatcher(root, siteMap, loader);
        } else {
            return new DummyWatcher();
        }
    }

    private HashMap<String, Site> getSiteMap() {
        String[] sitesList = sites.split(";");
        String[] assetsList = assets.split(";");
        String[] excludedList = excludes.split(";");
        String[] watchedList = watchedFile.split(";");
        String[] indexList = indexFiles.split(";");
        String[] hcmsServiceList = hcmsServiceUrl.split(";");
        String[] hcmsFileApiList = hcmsFileApi.split(";");

        HashMap<String, Site> siteMap = new HashMap<>();
        // TODO: handle error coused by lists sizes not equal
        for (int i = 0; i < sitesList.length; i++) {
            Site s = new Site();
            s.name = sitesList[i];
            s.assetsPath = assetsList[i];
            String[] excludedPaths = excludedList[i].split(",");
            HashSet<String> excluded = new HashSet<>();
            for (int j = 0; j < excludedPaths.length; j++) {
                excluded.add(excludedPaths[j]);
            }
            s.excludedPaths = excluded;
            Set<String> watchedFiles = new HashSet<>();
            watchedFiles.add(watchedList[i]);
            s.watchedFiles = watchedFiles;
            s.indexFile = indexList[i];
            s.hcmsServiceLocation = hcmsServiceList[i];
            s.hcmsFileApiPath = hcmsFileApiList[i];
            siteMap.put(s.name, s);
        }

        return siteMap;
    }

    @Override
    @CacheInvalidateAll(cacheName = "document-cache")
    @CacheInvalidateAll(cacheName = "document-list-cache")
    public void reload() {
        long timestamp = System.currentTimeMillis();
        // executing system command to pull, the repository
        String[] sitesList = sites.split(";");
        String[] command = { "git", "pull", "https://" + githubToken + "@" + githubRepository };
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            logger.info("Repository updated, exit code: " + exitCode);
            for (int i = 0; i < sitesList.length; i++) {
                loader.loadDocuments(sitesList[i], siteMap, i == 0, i == sitesList.length - 1, timestamp);
            }
        } catch (Exception e) {
            logger.error("Error updating repository: " + e.getMessage());
        }

    }
    
    // Shutdown executors and watchers when application stops
    public void shutdown() {
        // First, signal all watchers to stop
        for (ForChangeWatcherIface w : activeWatchers) {
            try {
                w.stop();
                if (logger != null) {
                    logger.info("Stop signal sent to watcher: " + w.getNameplate());
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Error stopping watcher " + w.getNameplate() + ": " + e.getMessage());
                }
            }
        }
        activeWatchers.clear();
        
        // Then shutdown all executors
        for (ExecutorService executor : watcherExecutors) {
            try {
                executor.shutdownNow();
                if (logger != null) {
                    logger.info("Shutting down watcher executor");
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Error shutting down executor: " + e.getMessage());
                }
            }
        }
        watcherExecutors.clear();
    }
    
    // ==================== Test helper methods ====================
    
    /**
     * Returns the list of watcher executors (for testing).
     */
    public List<ExecutorService> getWatcherExecutors() {
        return new ArrayList<>(watcherExecutors);
    }

    /**
     * Returns the list of active watchers (for testing).
     */
    public List<ForChangeWatcherIface> getActiveWatchers() {
        return new ArrayList<>(activeWatchers);
    }

    /**
     * Returns the site map (for testing).
     */
    public HashMap<String, Site> getSiteMapForTesting() {
        return getSiteMap();
    }
    
    @PreDestroy
    public void onShutdown() {
        shutdown();
        logger.info("DocumentLogic shutdown completed");
    }

    @Override
    public List<Document> findDocuments(String path, String tagName, String tagValue) {
        List<Document> docs = new ArrayList<>();
        docs = repositoryPort.findDocuments(path, tagName, tagValue, false);
        return docs;
    }

    @Override
    public List<Document> findDocuments(String path, String tagName, String tagValue, String sortBy, String sortOrder,
            boolean withContent) {
        List<Document> docs = new ArrayList<>();
        docs = repositoryPort.findDocumentsSorted(path, tagName, tagValue, withContent, sortBy, sortOrder);
        return docs;
    }

    @Override
    public List<Document> findDocumentsSorted(String path, String tagName, String tagValue, String sortBy,
            String sortOrder) {
        List<Document> docs = new ArrayList<>();
        docs = repositoryPort.findDocumentsSorted(path, tagName, tagValue, false, sortBy, sortOrder);
        return docs;
    }

    @Override
    public Document findFirstDocument(String path, String tagName, String tagValue, String sortBy, String sortOrder) {
        Document doc = repositoryPort.findFirstDocument(path, tagName, tagValue, true, sortBy, sortOrder);
        return doc;
    }

    @Override
    public List<String> searchDocuments(String textToSearch, String languageCode) {
        List<String> docs = new ArrayList<>();
        docs = repositoryPort.searchDocuments(textToSearch, languageCode);
        return docs;
    }

    @Override
    public Long getRepositorySize() {
        return repositoryPort.getSize();
    }
}
