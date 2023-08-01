package org.cricketmsf.hcms.domain;

import java.util.List;

import org.cricketmsf.hcms.application.out.DocumentRepositoryLoader;
import org.cricketmsf.hcms.application.out.DocumentRepositoryPort;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentLogic {

    @Inject
    DocumentRepositoryPort repositoryPort;

    @Inject
    DocumentRepositoryLoader loader;

    public List<Document> getDocuments(String path) {
        return repositoryPort.getDocuments(path);
        //return repository.getAllDocuments();
    }

    void onStart(@Observes StartupEvent ev) {               
        loader.loadDocuments("");
    }

    public void reload() {
        loader.loadDocuments("");
    }
    
}
