package org.cricketmsf.hcms.application.out;

import java.util.List;

import org.cricketmsf.hcms.adapter.out.DocumentRepository;
import org.cricketmsf.hcms.domain.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentRepositoryPort implements DocumentRepositoryIface {

    @Inject
    Logger logger;

    @Inject
    DocumentRepository repository;
    
/*     @ConfigProperty(name = "document.syntax")
    String syntax; // "obsidian", "github" 
    @ConfigProperty(name = "document.extension.markdown")
    String markdownFileExtension;
    @ConfigProperty(name = "document.extension.html")
    String htmlFileExtension; */
    
    @Override
    public List<Document> getDocuments(String path) {
        return repository.getDocuments(path);
    }

    @Override
    public void addDocument(Document doc) {
        repository.addDocument(doc);
    }


    @Override
    public void deleteDocument(String path) {
        repository.deleteDocument(path);
    }

    @Override
    public long getDocumentsCount() {
        return repository.getDocumentsCount();
    }

    @Override
    public List<Document> getAllDocuments() {
        return repository.getAllDocuments();
    }

    @Override
    public void startReload() {
        repository.startReload();
    }

    @Override
    public void stopReload() {
        repository.stopReload();
    }
    
}
