package org.cricketmsf.hcms.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.cricketmsf.hcms.adapter.out.DocumentRepositoryH2;
import org.cricketmsf.hcms.application.out.DocumentRepositoryLoader;
import org.cricketmsf.hcms.application.out.DocumentRepositoryPort;
import org.cricketmsf.hcms.application.out.FolderWatcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentLogic {

    @Inject
    Logger logger;
    //@Inject
    //DocumentRepositoryH2 repositoryH2;
    @Inject
    DocumentRepositoryPort repositoryPort;
    @Inject
    DocumentRepositoryLoader loader;

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

    public List<Document> getDocuments(String path, boolean withContent) {
        return repositoryPort.getDocuments(path, withContent);
    }

    public Document getDocument(String path) {
        return repositoryPort.getDocument(path);
    }

    void onStart(@Observes StartupEvent ev) {
        long timestamp = System.currentTimeMillis();
        String[] sitesList = sites.split(";");
        String[] assetsList = assets.split(";");
        String[] hcmsServiceList = hcmsServiceUrl.split(";");
        //repositoryPort.init();
        for(int i=0; i<sitesList.length; i++){
            logger.info("loading documents from " + sitesList[i]);
            loader.loadDocuments(sitesList[i], i==0, i==sitesList.length-1, timestamp);
        }
        //loader.loadDocuments("");
        if (watcherActive) {
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
                docRoot = root +"/"+sitesList[i] + docRoot;
                logger.info("Monitoring changes in " + docRoot + "/" + docName);
                Executors.newSingleThreadExecutor().execute(new FolderWatcher(docRoot, docName, loader, sitesList[i]));
            }
        } else {
            logger.info("Watcher is not active");
        }
    }

    public void reload() {
        long timestamp = System.currentTimeMillis();
        // loader.loadDocuments("");
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
            //loader.loadDocuments("");
        } catch (Exception e) {
            logger.error("Error updating repository: " + e.getMessage());
        }

    }

    // public void addDocument(Document document) {
    // repositoryPort.addDocument(document);
    // }

    public List<Document> findDocuments(String path, String[] props) {
        List<Document> docs = new ArrayList<>();
        String[] prop;
        prop = props[0].split(":");
        if (prop.length != 2) {
            logger.error("Invalid property definition: " + props[0]);
            return new ArrayList<>(); // TODO: throw exception
        }
        docs = repositoryPort.findDocuments(path, prop[0], prop[1], false);
        for (int i = 1; i < props.length; i++) {
            prop = props[i].split(":");
            if (prop.length != 2) {
                logger.error("Invalid property definition: " + props[i]);
                return new ArrayList<>(); // TODO: throw exception
            }
            docs = repositoryPort.filter(docs, prop[0], prop[1]);
        }
        return docs;
    }
}
