package org.cricketmsf.hcms.application.out;

import java.util.List;

import org.cricketmsf.hcms.adapter.out.DocumentRepository;
import org.cricketmsf.hcms.domain.Document;
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
    public List<Document> getDocuments(String path, boolean withContent) {
        return repository.getDocuments(path, withContent);
    }

    @Override
    public Document getDocument(String path) {
        return repository.getDocument(path);
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
    public List<Document> getAllDocuments(boolean noContent) {
        return repository.getAllDocuments(noContent);
    }

    @Override
    public void startReload() {
        repository.startReload();
    }

    @Override
    public void stopReload() {
        repository.stopReload();
    }

    @Override
    public List<Document> findDocuments(String propertyName, String path, String propertyValue, boolean withContent) {
        return repository.findDocuments(path, propertyName, propertyValue, withContent);
    }

    @Override
    public List<Document> filter(List<Document> docs, String propertyName, String propertyValue) {
        return repository.filter(docs, propertyName, propertyValue);
    }
    
}
