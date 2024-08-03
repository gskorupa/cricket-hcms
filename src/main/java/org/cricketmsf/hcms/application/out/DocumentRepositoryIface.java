package org.cricketmsf.hcms.application.out;

import java.util.List;

import org.cricketmsf.hcms.domain.Document;

import io.agroal.api.AgroalDataSource;

public interface DocumentRepositoryIface {
    public List<Document> getDocuments(String path, boolean withContent);
    public List<Document> getAllDocuments(boolean noContent);
    public List<Document> findDocuments(String path, String metadataName, String metadataValue, boolean withContent);
    public List<Document> filter(List<Document> docs, String metadataName, String metadataValue);
    public Document getDocument(String path);
    public void addDocument(Document doc);
    public void deleteDocument(String path);
    public long getDocumentsCount();
    public void startReload();
    public void stopReload(long timestamp);
    public void init(AgroalDataSource dataSource);
}