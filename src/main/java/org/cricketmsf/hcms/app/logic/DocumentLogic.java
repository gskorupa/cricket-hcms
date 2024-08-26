package org.cricketmsf.hcms.app.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.cricketmsf.hcms.adapter.driven.loader.FromFilesystemLoader;
import org.cricketmsf.hcms.adapter.driven.repo.DocumentRepository;
import org.cricketmsf.hcms.adapter.driven.repo.DocumentRepositoryH2;
import org.cricketmsf.hcms.adapter.driving.FolderWatcher;
import org.cricketmsf.hcms.app.driven_ports.ForDocumentRepositoryIface;
import org.cricketmsf.hcms.app.driven_ports.ForDocumentsLoaderIface;
import org.cricketmsf.hcms.app.driven_ports.ForTranslatorIface;
import org.cricketmsf.hcms.app.driving_ports.ForDocumentsIface;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentLogic implements ForDocumentsIface {

    @Inject
    Logger logger;

    ForDocumentRepositoryIface repositoryPort;
    ForDocumentsLoaderIface loader;
    ForTranslatorIface translator;

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
    @ConfigProperty(name = "hcms.database.type")
    String databaseType;

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

    @Override
    public List<Document> getDocuments(String path, boolean withContent) {
        return repositoryPort.getDocuments(path, withContent);
    }

    @Override
    public List<String> getPaths(String siteName){
        return repositoryPort.getPaths(siteName);
    }

    @Override
    public List<String> getSiteNames(){
        return repositoryPort.getSiteNames();
    }

    @Override
    public Document getDocument(String path) {
        return repositoryPort.getDocument(path);
    }

    void onStart(@Observes StartupEvent ev) {
        // repository setup
        switch (databaseType) {
            case "h2":
                repositoryPort = new DocumentRepositoryH2();
                break;
            case "map":
                repositoryPort = new DocumentRepository();
                break;
            default:
                repositoryPort = new DocumentRepositoryH2();
        }
        repositoryPort.init(dataSource);

        // translator setup
        // TODO

        // document loader setup
        loader = new FromFilesystemLoader();
        loader.setAssets(assets);
        loader.setExcludes(excludes);
        loader.setHcmsServiceUrl(hcmsServiceUrl);
        loader.setHtmlFileExtension(htmlFileExtension);
        loader.setMarkdownFileExtension(markdownFileExtension);
        loader.setRepositoryPort(repositoryPort);
        loader.setRoot(root);
        loader.setSites(sites);
        loader.setSyntax(syntax);

        long timestamp = System.currentTimeMillis();
        String[] sitesList = sites.split(";");
        String[] assetsList = assets.split(";");
        String[] hcmsServiceList = hcmsServiceUrl.split(";");
        for (int i = 0; i < sitesList.length; i++) {
            logger.info("loading documents from " + sitesList[i]);
            loader.loadDocuments(sitesList[i], i == 0, i == sitesList.length - 1, timestamp);
        }
        if (watcherActive) {
            boolean mapImplementation = false;
            if (databaseType.equals("h2")) {
                mapImplementation = false;
            } else {
                mapImplementation = true;
            }
            String[] filesToWatch = watchedFile.split(";");
            String docRoot;
            String docName;
            for (int i = 0; i < filesToWatch.length; i++) {
                docRoot = "";
                if (filesToWatch[i].lastIndexOf("/") > 0) {
                    docName = filesToWatch[i].substring(filesToWatch[i].lastIndexOf("/") + 1);
                    docRoot = filesToWatch[i].substring(0, filesToWatch[i].lastIndexOf("/"));
                } else {
                    docName = filesToWatch[i];
                }
                docRoot = root + "/" + sitesList[i] + docRoot;
                logger.info("Monitoring changes in " + docRoot + "/" + docName);
                Executors.newSingleThreadExecutor().execute(
                        new FolderWatcher(docRoot, docName, loader, sitesList[i], sitesList, mapImplementation));
            }
        } else {
            logger.info("Watcher is not active");
        }
    }

    @Override
    public void reload() {
        long timestamp = System.currentTimeMillis();
        // executing system command to pull, the repository
        String[] sitesList = sites.split(";");
        String[] command = { "git", "pull", "https://" + githubToken + "@" + githubRepository };
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            logger.info("Repository updated");
            for (int i = 0; i < sitesList.length; i++) {
                loader.loadDocuments(sitesList[i], i == 0, i == sitesList.length - 1, timestamp);
            }
        } catch (Exception e) {
            logger.error("Error updating repository: " + e.getMessage());
        }

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
}
