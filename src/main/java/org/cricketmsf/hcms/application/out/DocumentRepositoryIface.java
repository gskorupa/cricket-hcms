package org.cricketmsf.hcms.application.out;

import java.util.List;

import org.cricketmsf.hcms.domain.Document;

public interface DocumentRepositoryIface {
    public List<Document> getDocuments(String path, boolean withContent);
    public List<Document> getAllDocuments(boolean noContent);
    public List<Document> findDocuments(String path, String propertyName, String propertyValue, boolean withContent);
    public List<Document> filter(List<Document> docs, String propertyName, String propertyValue);
    public Document getDocument(String path);
    public void addDocument(Document doc);
    public void deleteDocument(String path);
    public long getDocumentsCount();
    public void startReload();
    public void stopReload();
}