package pl.experiot.hcms.app.ports.driving;

import java.util.List;

import pl.experiot.hcms.app.logic.Document;

public interface ForDocumentsIface {

    List<Document> getDocuments(String path, boolean withContent);
    List<String> getPaths(String siteRoot);
    List<String> getSiteNames();

    Document getDocument(String path);

    List<Document> findDocuments(String path, String tagName, String tagValue);

    List<Document> findDocumentsSorted(String path, String tagName, String tagValue, String sortBy, String sortOrder);
    List<Document> findDocuments(String path, String tagName, String tagValue, String sortBy, String sortOrder, boolean withContent);

    Document findFirstDocument(String path, String tagName, String tagValue, String sortBy, String sortOrder);

}