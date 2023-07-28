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

    public ConcurrentHashMap<String, Document> documents = null;

    void onStart(@Observes StartupEvent ev) {
        if (documents == null) {
            documents = new ConcurrentHashMap<>();
        }
    }

    private ConcurrentHashMap<String, Document> getDocuments() {
        if (documents == null) {
            documents = new ConcurrentHashMap<>();
        }
        return documents;
    }

    @Override
    public List<Document> getDocuments(String path) {
        ArrayList<Document> docs = new ArrayList<>();
        logger.info("database size: " + documents.size());
        String searchPath = "/"+path;
        logger.info("searching: " + searchPath);
        getDocuments().forEach((k, v) -> {
            if(isFolder(searchPath)){
                if(k.startsWith(searchPath) && k.indexOf("/", searchPath.length() + 1) < 0){
                    docs.add(v);
                }
            }else{
                if(k.equals(searchPath)){
                    docs.add(v);
                }
            }
        });
        logger.info("found: " + docs.size());
        return docs;
    }

    private boolean isFolder(String path) {
        return path.indexOf(".") < 0;
    }

    @Override
    public List<Document> getAllDocuments() {
        ArrayList<Document> docs = new ArrayList<>();
        logger.info("database size: " + documents.size());
        getDocuments().forEach((k, v) -> {
            docs.add(v);
        });
        return docs;
    }

    @Override
    public void addDocument(Document doc) {
        logger.info("addDocument: " + doc.path);
        getDocuments().put(doc.name, doc);
    }

    @Override
    public void deleteDocument(String path) {
        getDocuments().remove(path);
    }

    @Override
    public long getDocumentsCount() {
        return getDocuments().size();
    }

}
