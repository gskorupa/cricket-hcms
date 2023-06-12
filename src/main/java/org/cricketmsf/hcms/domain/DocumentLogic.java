package org.cricketmsf.hcms.domain;

import java.util.List;

import org.cricketmsf.hcms.application.out.DocumentRepositoryLoader;
import org.cricketmsf.hcms.application.out.DocumentRepositoryPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentLogic {

    @Inject
    DocumentRepositoryPort repository;

    @Inject
    DocumentRepositoryLoader loader;

    public List<Document> getDocuments(String path) {
        return repository.getDocuments(path);
        //return repository.getAllDocuments();
    }

    public void reload() {
        loader.loadDocuments("");
    }
    
}
