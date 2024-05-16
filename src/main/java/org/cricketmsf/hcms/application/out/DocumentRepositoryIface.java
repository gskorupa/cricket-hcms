package org.cricketmsf.hcms.application.out;

import java.util.List;

import org.cricketmsf.hcms.domain.Document;

public interface DocumentRepositoryIface {
    public List<Document> getDocuments(String path, boolean noContent);
    public List<Document> getAllDocuments(boolean noContent);
    public Document getDocument(String path);
    public void addDocument(Document doc);
    public void deleteDocument(String path);
    public long getDocumentsCount();
    public void startReload();
    public void stopReload();
}