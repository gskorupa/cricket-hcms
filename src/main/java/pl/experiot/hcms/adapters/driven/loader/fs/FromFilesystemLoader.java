package pl.experiot.hcms.adapters.driven.loader.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import org.jboss.logging.Logger;

import io.vertx.mutiny.core.eventbus.EventBus;
import pl.experiot.hcms.app.logic.Document;
import pl.experiot.hcms.app.logic.Site;
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
    String sites;
    String assets;

    EventBus eventBus;
    String queueName;

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
    public void loadDocuments(String siteName, HashMap<String, Site> siteMap, boolean start, boolean stop,
            long timestamp) {
        String docPath = siteName;
        Site site = siteMap.get(siteName);
        if (!docPath.isEmpty()) {
            docPath = "/" + docPath;
        }
        if (start) {
            repositoryPort.startReload(siteName);
        }
        /*
         * String[] sitesList = sites.split(";");
         * String[] assetsList = assets.split(";");
         * String[] hcmsServiceList = hcmsServiceUrl.split(";");
         * String[] excludedList = excludes.split(";");
         */
        logger.debug("loading documents");
        logger.debug("actual path: " + Paths.get(".").toAbsolutePath().normalize().toString());
        logger.debug("getDocuments: " + docPath);
        logger.debug("complete path: " + root + docPath);
        ArrayList<Document> files = new ArrayList<>();
        DocumentVisitor visitor = new DocumentVisitor();
        visitor.setRoot(Paths.get(root + docPath).toAbsolutePath().toString());
        visitor.setSyntax(syntax);
        visitor.setMarkdownFileExtension(markdownFileExtension);
        visitor.setHtmlFileExtension(htmlFileExtension);
        // int siteIndex = getSiteIndex(sitesList, siteName);

        /*
         * String siteExcludedFolders[];
         * if (siteIndex >= 0) {
         * siteExcludedFolders = excludedList[siteIndex].split(",");
         * for (String exclude : siteExcludedFolders) {
         * visitor.exclude(exclude);
         * }
         * }
         */
        site.excludedPaths.forEach((path) -> {
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
        logger.info("found1: " + files.size() + " documents");
        Document doc;
        for (int i = 0; i < files.size(); i++) {
            // logger.info(" " + files.get(i).path);
            doc = normalize(files.get(i), siteName);
            doc = DocumentTransformer.transform(doc, markdownFileExtension, siteName, site.assetsPath,
                    site.hcmsServiceLocation);
            // doc = DocumentTransformer.transform(doc, markdownFileExtension, siteName,
            // assetsList[0],hcmsServiceList[0]);
            if (null != doc) {
                doc.refreshTimestamp = timestamp;
                repositoryPort.addDocument(doc);
            }
        }
        logger.info("loaded: " + files.size() + " documents");
        logger.info("repositoryPort database size: " + repositoryPort.getDocumentsCount());
        if (stop) {
            repositoryPort.stopReload(timestamp, docPath);
            listAll();
        }
    }

    @Override
    public void loadDocuments(Site site, long timestamp) {
        String docPath = site.name;
        if (!docPath.isEmpty()) {
            docPath = "/" + docPath;
        }
        repositoryPort.startReload(site.name);
        logger.debug("loading documents");
        logger.debug("actual path: " + Paths.get(".").toAbsolutePath().normalize().toString());
        logger.debug("getDocuments: " + docPath);
        logger.debug("complete path: " + root + docPath);
        ArrayList<Document> files = new ArrayList<>();
        DocumentVisitor visitor = new DocumentVisitor();
        visitor.setRoot(Paths.get(root + docPath).toAbsolutePath().toString());
        visitor.setSyntax(syntax);
        visitor.setMarkdownFileExtension(markdownFileExtension);
        visitor.setHtmlFileExtension(htmlFileExtension);

        site.excludedPaths.forEach((path) -> {
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
        logger.info("found2: " + files.size() + " documents");
        Document doc;
        for (int i = 0; i < files.size(); i++) {
            // logger.info(" " + files.get(i).path);
            doc = normalize(files.get(i), site.name);
            doc = DocumentTransformer.transform(doc, markdownFileExtension, site.name, site.assetsPath,
                    site.hcmsServiceLocation);
            // doc = DocumentTransformer.transform(doc, markdownFileExtension, siteName,
            // assetsList[0],hcmsServiceList[0]);
            if (null != doc) {
                doc.refreshTimestamp = timestamp;
                repositoryPort.addDocument(doc);
            }
        }
        logger.info("loaded: " + files.size() + " documents");
        logger.info("repositoryPort database size: " + repositoryPort.getDocumentsCount());
        repositoryPort.stopReload(timestamp, docPath);
        listAll();
    }

    private Document normalize(Document doc, String siteRootFolder) {
        logger.debug("pre doc.name: " + doc.name);
        logger.debug("pre doc.path: " + doc.path);
        doc.siteName = siteRootFolder;
        if (!(doc.path.startsWith(siteRootFolder) || doc.path.startsWith("/" + siteRootFolder))) {
            doc.path = siteRootFolder + doc.path;
        }
        if (!(doc.name.startsWith(siteRootFolder) || doc.name.startsWith("/" + siteRootFolder))) {
            doc.name = siteRootFolder + doc.name;
        }
        if (!(doc.path.startsWith("/"))) {
            doc.path = "/" + doc.path;
        }
        if (!(doc.name.startsWith("/"))) {
            doc.name = "/" + doc.name;
        }
        logger.debug("post doc.name: " + doc.name);
        logger.debug("post doc.path: " + doc.path);
        return doc;
    }

    private void listAll() {
        ArrayList<Document> docs = (ArrayList<Document>) repositoryPort.getAllDocuments(false);
        logger.info("repositoryPort database size: " + docs.size());
        logger.info("listing all documents");
        for (Document doc : docs) {
            logger.info(doc.name + " [" + doc.getSiteName() + "]");
        }

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
    public void setSites(String sites) {
        this.sites = sites;
    }

    @Override
    public void setAssets(String assets) {
        this.assets = assets;
    }

}
