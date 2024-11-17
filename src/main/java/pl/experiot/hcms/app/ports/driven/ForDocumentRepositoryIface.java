package pl.experiot.hcms.app.ports.driven;

import java.util.HashMap;
import java.util.List;

import io.agroal.api.AgroalDataSource;
import io.vertx.mutiny.core.eventbus.EventBus;
import pl.experiot.hcms.app.logic.dto.Document;

public interface ForDocumentRepositoryIface {
    public List<Document> getDocuments(String path, boolean withContent);
    public List<String> getPaths(String siteRoot);
    public List<String> getSiteNames();
    public List<Document> getAllDocuments(boolean noContent);
    public List<Document> findDocuments(String path, String metadataName, String metadataValue, boolean withContent);
    public List<Document> findDocumentsSorted(String path, String metadataName, String metadataValue, boolean withContent, String sortBy, String sortOrder);
    public Document findFirstDocument(String path, String metadataName, String metadataValue, boolean withContent, String sortBy, String sortOrder);
    public List<Document> filter(List<Document> docs, String metadataName, String metadataValue);
    public Document getDocument(String path);
    public void addDocument(Document doc);
    public void deleteDocument(String path);
    public long getDocumentsCount();
    public void startReload(String siteName);
    public void stopReload(long timestamp, String siteName);
    public void init(AgroalDataSource dataSource);
    public HashMap<String, String> getMetadata(String name);
    public void addMetadata(String name, HashMap<String, String> metadata);
    public void deleteMetadata(String name);
    public List<String> searchDocuments(String textToSearch, String languageCode);
    public void setEventBus(EventBus eventBus, String queName);
}