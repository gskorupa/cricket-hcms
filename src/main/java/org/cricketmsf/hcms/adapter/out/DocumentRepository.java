package org.cricketmsf.hcms.adapter.out;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.cricketmsf.hcms.application.out.DocumentRepositoryIface;
import org.cricketmsf.hcms.domain.Document;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentRepository implements DocumentRepositoryIface {

    @Inject
    Logger logger;

    public ConcurrentHashMap<String, Document> documents;

    void onStart(@Observes StartupEvent ev) {
        documents = new ConcurrentHashMap<>();
    }

    @Override
    public List<Document> getDocuments(String path) {
        ArrayList<Document> docs = new ArrayList<>();
        logger.info("database size: " + documents.size());
        documents.forEach((k, v) -> {
            if (k.startsWith(path) && k.indexOf("/", path.length() + 1) < 0) {
                docs.add(v);
            }
        });
        return docs;
    }

    @Override
    public List<Document> getAllDocuments() {
        ArrayList<Document> docs = new ArrayList<>();
        logger.info("database size: " + documents.size());
        documents.forEach((k, v) -> {
            docs.add(v);
        });
        return docs;
    }

    @Override
    public void addDocument(Document doc) {
        documents.put(doc.path, doc);
    }

    @Override
    public void deleteDocument(String path) {
        documents.remove(path);
    }

    @Override
    public long getDocumentsCount() {
        return documents.size();
    }

}
