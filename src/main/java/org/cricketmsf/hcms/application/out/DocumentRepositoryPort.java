package org.cricketmsf.hcms.application.out;

import java.util.HashMap;
import java.util.List;

import org.cricketmsf.hcms.adapter.out.DocumentRepository;
import org.cricketmsf.hcms.adapter.out.DocumentRepositoryH2;
import org.cricketmsf.hcms.domain.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentRepositoryPort implements DocumentRepositoryIface {

    @Inject
    Logger logger;

    @Inject
    AgroalDataSource dataSource;

    private static DocumentRepositoryIface repository=null;

    @ConfigProperty(name = "hcms.database.type")
    String databaseType;

    
    @Override
    public List<Document> getDocuments(String path, boolean withContent) {
        return getRepository().getDocuments(path, withContent);
    }

    @Override
    public Document getDocument(String name) {
        Document doc = getRepository().getDocument(name);
        return doc;
    }

    @Override
    public void addDocument(Document doc) {
        getRepository().deleteMetadata(doc.name);
        getRepository().addDocument(doc);
        getRepository().addMetadata(doc.name, doc.metadata);
    }


    @Override
    public void deleteDocument(String path) {
        getRepository().deleteMetadata(path);
        getRepository().deleteDocument(path);
    }

    @Override
    public long getDocumentsCount() {
        return getRepository().getDocumentsCount();
    }

    @Override
    public List<Document> getAllDocuments(boolean noContent) {
        return getRepository().getAllDocuments(noContent);
    }

    @Override
    public void startReload() {
        getRepository().startReload();
    }

    @Override
    public void stopReload(long timestamp, String siteName) {
        getRepository().stopReload(timestamp, siteName);
    }

    @Override
    public List<Document> findDocuments(String path, String propertyName, String propertyValue, boolean withContent) {
        return getRepository().findDocuments(path, propertyName, propertyValue, withContent);
    }

    @Override
    public List<Document> findDocumentsSorted(String path, String propertyName, String propertyValue, boolean withContent, String sortBy, String sortOrder) {
        return getRepository().findDocumentsSorted(path, propertyName, propertyValue, withContent, sortBy, sortOrder);
    }

    @Override
    public List<Document> filter(List<Document> docs, String propertyName, String propertyValue) {
        return getRepository().filter(docs, propertyName, propertyValue);
    }

    @Override
    public void init(AgroalDataSource dataSource) {
        getRepository().init(dataSource);
    }

    private DocumentRepositoryIface getRepository(){
        if(repository==null){
            logger.info("database type: " + databaseType);
            if(databaseType.equals("h2")){
                repository = new DocumentRepositoryH2();
            }else{
                repository = new DocumentRepository();
            }
            repository.init(dataSource);
        }
        return repository;
    }

    @Override
    public HashMap<String, String> getMetadata(String name) {
        return getRepository().getMetadata(name);
    }

    @Override
    public void addMetadata(String name, HashMap<String, String> metadata) {
        getRepository().addMetadata(name, metadata);
    }

    @Override
    public void deleteMetadata(String name) {
        getRepository().deleteMetadata(name);
    }

    @Override
    public Document findFirstDocument(String path, String metadataName, String metadataValue, boolean withContent,
            String sortBy, String sortOrder) {
        return getRepository().findFirstDocument(path, metadataName, metadataValue, withContent, sortBy, sortOrder);
    }
    
}
