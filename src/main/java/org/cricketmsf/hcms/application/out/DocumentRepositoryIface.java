package org.cricketmsf.hcms.application.out;

import java.util.List;

import org.cricketmsf.hcms.domain.Document;

public interface DocumentRepositoryIface {
    public List<Document> getDocuments(String path);
    public List<Document> getAllDocuments();
    public void addDocument(Document doc);
    public void deleteDocument(String path);
    public long getDocumentsCount();
    public void startReload();
    public void stopReload();
}