package org.cricketmsf.hcms.application.out;

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

   //@Inject
    //DocumentRepository repositoryMem;

    //@Inject
    //DocumentRepositoryH2 repository;

    private static DocumentRepositoryIface repository=null;

    @ConfigProperty(name = "hcms.database.type")
    String databaseType;
    
    
/*     @ConfigProperty(name = "document.syntax")
    String syntax; // "obsidian", "github" 
    @ConfigProperty(name = "document.extension.markdown")
    String markdownFileExtension;
    @ConfigProperty(name = "document.extension.html")
    String htmlFileExtension; */
    
    @Override
    public List<Document> getDocuments(String path, boolean withContent) {
        return getRepository().getDocuments(path, withContent);
    }

    @Override
    public Document getDocument(String path) {
        return getRepository().getDocument(path);
    }

    @Override
    public void addDocument(Document doc) {
        getRepository().addDocument(doc);
    }


    @Override
    public void deleteDocument(String path) {
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
    public void stopReload(long timestamp) {
        getRepository().stopReload(timestamp);
    }

    @Override
    public List<Document> findDocuments(String propertyName, String path, String propertyValue, boolean withContent) {
        return getRepository().findDocuments(path, propertyName, propertyValue, withContent);
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
    
}
