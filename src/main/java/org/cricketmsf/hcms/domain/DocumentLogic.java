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

    public List<Document> getDocuments(String path) {
        return repositoryPort.getDocuments(path);
    }

    void onStart(@Observes StartupEvent ev) {              
        loader.loadDocuments("");
        if (watcherActive) {
            logger.info("Watching for changes in "+root+"/"+watchedFile);
            Executors.newSingleThreadExecutor().execute(new FolderWatcher(root, watchedFile, loader));
        }else{
            logger.info("Watcher is not active");
        }
    }

    public void reload() {
        loader.loadDocuments("");
    }

    public void addDocument(Document document){
        repositoryPort.addDocument(document);
    }

}
