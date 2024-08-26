package org.cricketmsf.hcms.app.driving_ports;

import java.util.List;

import org.cricketmsf.hcms.app.logic.Document;

public interface ForDocumentsIface {

    List<Document> getDocuments(String path, boolean withContent);
    List<String> getPaths(String siteRoot);
    List<String> getSiteNames();

    Document getDocument(String path);

    void reload();

    List<Document> findDocuments(String path, String tagName, String tagValue);

    List<Document> findDocumentsSorted(String path, String tagName, String tagValue, String sortBy, String sortOrder);
    List<Document> findDocuments(String path, String tagName, String tagValue, String sortBy, String sortOrder, boolean withContent);

    Document findFirstDocument(String path, String tagName, String tagValue, String sortBy, String sortOrder);

}