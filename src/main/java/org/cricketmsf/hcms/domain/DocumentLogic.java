package org.cricketmsf.hcms.domain;

import java.util.List;
import java.util.concurrent.Executors;

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

    public List<Document> getDocuments(String path) {
        return repositoryPort.getDocuments(path);
    }

    void onStart(@Observes StartupEvent ev) {              
        loader.loadDocuments("");
        if (watcherActive) {
            String[] filesToWatch = watchedFile.split(";");
            String docRoot;
            String docName;
            for (int i=0; i<filesToWatch.length; i++) {
                docRoot = "";
                if(filesToWatch[i].lastIndexOf("/")>0){
                    docName = filesToWatch[i].substring(filesToWatch[i].lastIndexOf("/")+1);
                    docRoot = filesToWatch[i].substring(0, filesToWatch[i].lastIndexOf("/"));
                }else{
                    docName = filesToWatch[i];
                }
                docRoot = root + "/" + docRoot;
                logger.info("Monitoring changes in " + docRoot + "/" + docName);
                Executors.newSingleThreadExecutor().execute(new FolderWatcher(docRoot, docName, loader));
            }
        }else{
            logger.info("Watcher is not active");
        }
    }

    public void reload() {
        //loader.loadDocuments("");
        // executing system command to pull, the repository
        String[] command = {"git", "pull", "https://"+githubToken+"@"+githubRepository};
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            logger.info("Repository updated");
            loader.loadDocuments("");
        } catch (Exception e) {
            logger.error("Error updating repository: " + e.getMessage());
        }

    }

    public void addDocument(Document document){
        repositoryPort.addDocument(document);
    }

}
