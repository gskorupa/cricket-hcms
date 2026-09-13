package pl.experiot.hcms.adapters.driven.loader.fs;

import io.quarkus.cache.CacheInvalidateAll;
import io.vertx.mutiny.core.eventbus.EventBus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jboss.logging.Logger;
import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.logic.dto.Site;
import pl.experiot.hcms.app.ports.driven.ForDocumentRepositoryIface;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;

public class FromFilesystemLoader implements ForDocumentsLoaderIface {

    Logger logger = Logger.getLogger(FromFilesystemLoader.class);

    ForDocumentRepositoryIface repositoryPort;

    String root;
    String excludes;
    String syntax; /* "obsidian", "github" */
    String markdownFileExtension;
    String htmlFileExtension;
    String hcmsServiceUrl;
    String hcmsFileApi;
    String sites;
    String assets;
    String[] languages;

    EventBus eventBus;
    String queueName;
    
    // Statistics tracking - use singleton instance
    private LoadStatistics loadStatistics = LoadStatistics.getInstance();

    @Override
    public void setEventBus(EventBus eventBus, String queueName) {
        this.eventBus = eventBus;
        this.queueName = queueName;
    }

    @Override
    public void setRoot(String root) {
        this.root = root;
    }

    @Override
    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }

    @Override
    public void setRepositoryPort(ForDocumentRepositoryIface repositoryPort) {
        this.repositoryPort = repositoryPort;
        this.repositoryPort.setEventBus(eventBus, queueName);
    }

    @Override
    @CacheInvalidateAll(cacheName = "document-cache")
    @CacheInvalidateAll(cacheName = "document-list-cache")
    public void loadDocuments(
        String siteName,
        HashMap<String, Site> siteMap,
        boolean start,
        boolean stop,
        long timestamp
    ) {
        String docPath = siteName;
        Site site = siteMap.get(siteName);
        if (!docPath.isEmpty()) {
            docPath = "/" + docPath;
        }
        if (start) {
            repositoryPort.startReload(siteName);
        }

        // Reset statistics for this load operation
        loadStatistics.reset();

        logger.debug("loading documents");
        logger.debug(
            "actual path: " +
                Paths.get(".").toAbsolutePath().normalize().toString()
        );
        logger.debug("getDocuments: " + docPath);
        logger.debug("complete path: " + root + docPath);
        ArrayList<Document> files = new ArrayList<>();
        DocumentVisitor visitor = new DocumentVisitor();
        visitor.setRoot(
            Paths.get(root + docPath)
                .toAbsolutePath()
                .toString()
        );
        visitor.setSyntax(syntax);
        visitor.setMarkdownFileExtension(markdownFileExtension);
        visitor.setHtmlFileExtension(htmlFileExtension);

        site.excludedPaths.forEach(path -> {
            visitor.exclude(path);
        });
        Path p;
        try {
            p = Paths.get(root + docPath);
            logger.info("absolute path: " + p.toAbsolutePath().toString());
            Files.walkFileTree(p, visitor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        files = visitor.getList();
        
        // Collect visitor statistics
        loadStatistics.incrementFilesRead(visitor.getTotalFilesCount());
        loadStatistics.incrementSkippedFiles(visitor.getSkippedFilesCount());
        loadStatistics.incrementSkippedFolders(visitor.getSkippedFoldersCount());
        
        logger.info("found1: " + files.size() + " documents");
        Document doc;
        Document updatedDoc;
        for (int i = 0; i < files.size(); i++) {
            doc = normalize(files.get(i), siteName);
            if (isExcluded(doc.path)) {
                logger.info("skipping excluded: " + doc.name);
                loadStatistics.incrementSkippedFiles();
                continue;
            }
            updatedDoc = repositoryPort.getDocument(doc.name);
            // skip if the document is already in the database and has not been updated
            if (null != updatedDoc) {
                if (updatedDoc.updateTimestamp >= doc.updateTimestamp) {
                    logger.info("skipping not modified: " + doc.name);
                    loadStatistics.incrementSkippedFiles();
                    continue;
                }
            }
            doc = DocumentTransformer.transform(
                doc,
                markdownFileExtension,
                siteName,
                site.assetsPath,
                site.hcmsServiceLocation,
                site.hcmsFileApiPath,
                languages
            );
            if (null != doc) {
                doc.refreshTimestamp = timestamp;
                repositoryPort.addDocument(doc);
                loadStatistics.incrementDocumentsSaved();
            }
        }
        logger.info("loaded: " + files.size() + " documents");
        logger.info(
            "repositoryPort database size: " +
                repositoryPort.getDocumentsCount()
        );
        if (stop) {
            int deletedCount = repositoryPort.stopReload(timestamp, docPath);
            loadStatistics.incrementDeletedDocuments(deletedCount);
            listAll();
        }
        
        // Log statistics after loading
        logStatistics(siteName);
    }

    @Override
    @CacheInvalidateAll(cacheName = "document-cache")
    @CacheInvalidateAll(cacheName = "document-list-cache")
    public void loadDocuments(Site site, long timestamp) {
        String docPath = site.name;
        if (!docPath.isEmpty()) {
            docPath = "/" + docPath;
        }
        
        // Reset statistics for this load operation
        loadStatistics.reset();
        
        repositoryPort.startReload(site.name);
        logger.debug("loading documents");
        logger.debug(
            "actual path: " +
                Paths.get(".").toAbsolutePath().normalize().toString()
        );
        logger.debug("getDocuments: " + docPath);
        logger.debug("complete path: " + root + docPath);
        ArrayList<Document> files = new ArrayList<>();
        DocumentVisitor visitor = new DocumentVisitor();
        visitor.setRoot(
            Paths.get(root + docPath)
                .toAbsolutePath()
                .toString()
        );
        visitor.setSyntax(syntax);
        visitor.setMarkdownFileExtension(markdownFileExtension);
        visitor.setHtmlFileExtension(htmlFileExtension);

        site.excludedPaths.forEach(path -> {
            visitor.exclude(path);
        });
        Path p;
        try {
            p = Paths.get(root + docPath);
            logger.info("absolute path: " + p.toAbsolutePath().toString());
            Files.walkFileTree(p, visitor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        files = visitor.getList();
        
        // Collect visitor statistics
        loadStatistics.incrementFilesRead(visitor.getTotalFilesCount());
        loadStatistics.incrementSkippedFiles(visitor.getSkippedFilesCount());
        loadStatistics.incrementSkippedFolders(visitor.getSkippedFoldersCount());
        
        logger.info("found2: " + files.size() + " documents");
        Document doc;
        Document updatedDoc = null;
        for (int i = 0; i < files.size(); i++) {
            // logger.info(" " + files.get(i).path);
            doc = normalize(files.get(i), site.name);
            if (isExcluded(doc.path)) {
                logger.info("skipping excluded: " + doc.name);
                loadStatistics.incrementSkippedFiles();
                continue;
            }
            updatedDoc = repositoryPort.getDocument(doc.name);
            // skip if the document is already in the database and has not been updated
            if (null != updatedDoc) {
                if (updatedDoc.updateTimestamp >= doc.updateTimestamp) {
                    logger.info("skipping not modified: " + doc.name);
                    loadStatistics.incrementSkippedFiles();
                    continue;
                }
            }
            doc = DocumentTransformer.transform(
                doc,
                markdownFileExtension,
                site.name,
                site.assetsPath,
                site.hcmsServiceLocation,
                site.hcmsFileApiPath,
                languages
            );
            if (null != doc) {
                doc.refreshTimestamp = timestamp;
                repositoryPort.addDocument(doc);
                loadStatistics.incrementDocumentsSaved();
            }
        }
        logger.info("loaded: " + files.size() + " documents");
        logger.info(
            "repositoryPort database size: " +
                repositoryPort.getDocumentsCount()
        );
        int deletedCount = repositoryPort.stopReload(timestamp, docPath);
        loadStatistics.incrementDeletedDocuments(deletedCount);
        listAll();
        
        // Log statistics after loading
        logStatistics(site.name);
    }

    private Document normalize(Document doc, String siteRootFolder) {
        logger.debug("pre doc.name: " + doc.name);
        logger.debug("pre doc.path: " + doc.path);
        doc.siteName = siteRootFolder;
        if (
            !(
                doc.path.startsWith(siteRootFolder) ||
                doc.path.startsWith("/" + siteRootFolder)
            )
        ) {
            doc.path = siteRootFolder + doc.path;
        }
        if (
            !(
                doc.name.startsWith(siteRootFolder) ||
                doc.name.startsWith("/" + siteRootFolder)
            )
        ) {
            doc.name = siteRootFolder + doc.name;
        }
        if (!doc.path.startsWith("/")) {
            doc.path = "/" + doc.path;
        }
        if (!doc.name.startsWith("/")) {
            doc.name = "/" + doc.name;
        }
        logger.debug("post doc.name: " + doc.name);
        logger.debug("post doc.path: " + doc.path);
        return doc;
    }

    private boolean isExcluded(String path) {
        if (excludes == null || excludes.isEmpty()) {
            return false;
        }
        String[] excludeList = excludes.split(",");
        for (String exclude : excludeList) {
            //if path contains exclude, return true
            if (path.contains(exclude)) {
                return true;
            }
        }
        return false;
    }

    private void listAll() {
        List<Document> docs = (List<Document>) repositoryPort.getAllDocuments(
            false
        );
        logger.info("repositoryPort database size: " + docs.size());
        logger.info("listing all documents");
        for (Document doc : docs) {
            logger.info(doc.name + " [" + doc.getSiteName() + "]");
        }
    }
    
    /**
     * Logs the collected statistics for the document loading process.
     * Also includes translation statistics from the LoadStatistics singleton.
     */
    private void logStatistics(String siteName) {
        // The loadStatistics already contains all the data including translation stats
        // since it's a singleton shared across the application
        logger.info("Site: " + siteName);
        logger.info(loadStatistics.toLogMessage());
    }

    private int getSiteIndex(String[] sitesList, String siteRoot) {
        for (int i = 0; i < sitesList.length; i++) {
            if (sitesList[i].equals(siteRoot)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    @Override
    public void setMarkdownFileExtension(String markdownFileExtension) {
        this.markdownFileExtension = markdownFileExtension;
    }

    @Override
    public void setHtmlFileExtension(String htmlFileExtension) {
        this.htmlFileExtension = htmlFileExtension;
    }

    @Override
    public void setHcmsServiceUrl(String hcmsServiceUrl) {
        this.hcmsServiceUrl = hcmsServiceUrl;
    }

    @Override
    public void setHcmsFileApi(String hcmsFileApi) {
        this.hcmsFileApi = hcmsFileApi;
    }

    @Override
    public void setSites(String sites) {
        this.sites = sites;
    }

    @Override
    public void setAssets(String assets) {
        this.assets = assets;
    }

    @Override
    public void setLanguages(String[] languages) {
        this.languages = languages;
    }
}
