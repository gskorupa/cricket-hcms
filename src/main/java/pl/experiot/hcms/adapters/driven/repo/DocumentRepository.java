package pl.experiot.hcms.adapters.driven.repo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.vertx.mutiny.core.eventbus.EventBus;
import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.ports.driven.ForDocumentRepositoryIface;

public class DocumentRepository implements ForDocumentRepositoryIface {

    private static Logger logger = Logger.getLogger(DocumentRepository.class);

    private EventBus eventBus = null;
    private String queueName = null;


    /** In memory document database */
    public ConcurrentHashMap<String, Document> documents = null;
    /** Docoment database under construction */
    public ConcurrentHashMap<String, Document> underConstrDocs = null;

    private boolean reloadInProgress = false;

    @Override
    public void setEventBus(EventBus eventBus, String queName) {
        this.eventBus = eventBus;
        this.queueName = queName;
    }

    @Override
    public void init(AgroalDataSource dataSource) {

        logger.info("DocumentRepository initializing ...");
        if (documents == null) {
            documents = new ConcurrentHashMap<>();
        }
        if (underConstrDocs == null) {
            underConstrDocs = new ConcurrentHashMap<>();
        }

        documents.put("/index.html", new Document());
        documents.put("/documentation/index.html", new Document());

        eventBus.publish(queueName, "/index.html");
        eventBus.publish(queueName, "/documentation/index.html");
    }

    private ConcurrentHashMap<String, Document> getDocuments() {
        if (documents == null) {
            documents = new ConcurrentHashMap<>();
        }
        return documents;
    }

    private ConcurrentHashMap<String, Document> getUnderConstrDocs() {
        if (underConstrDocs == null) {
            underConstrDocs = new ConcurrentHashMap<>();
        }
        return underConstrDocs;
    }

    @Override
    public List<Document> getDocuments(String path, boolean withContent) {
        ArrayList<Document> docs = new ArrayList<>();
        String searchPath = path.startsWith("/") ? path : "/"+path;
        logger.debug("searching: " + searchPath);
        getDocuments().forEach((k, v) -> {
            if (isFolder(searchPath)) {
                if (k.startsWith(searchPath) && k.indexOf("/", searchPath.length() + 1) < 0) {
                    docs.add(v.clone(withContent));
                }
            } else {
                if (k.equals(searchPath)) {
                    docs.add(v.clone(withContent));
                }
            }
        });
        logger.debug("found: " + docs.size() + " documents");
        return docs;
    }

    @Override
    public Document getDocument(String path) {
        String searchPath=path.startsWith("/")?path:"/"+path;
        Document doc = getDocuments().get(searchPath);
        if (doc == null) {
            return null;
        }
        return doc.clone(true);
    }

    private boolean isFolder(String path) {
        return path.indexOf(".") < 0;
    }

    @Override
    public List<Document> getAllDocuments(boolean withContent) {
        ArrayList<Document> docs = new ArrayList<>();
        logger.info("database size: " + documents.size());
        getDocuments().forEach((k, v) -> {
            docs.add(v.clone(withContent));
        });
        return docs;
    }

    @Override
    public void addDocument(Document doc) {
        addDocument(doc, "");
    }

    @Override
    public void addDocument(Document doc, String origin) {
        if(!reloadInProgress){
            //TODO: for real, not in memory database, this won't be needed
            // Still update timestamps for translation tracking
            String language = extractLanguageFromPath(doc.name);
            updateDocumentTimestamp(doc, language);
            return;
        }
        logger.info("addDocument: " + doc.name);
        deleteMetadata(doc.name);
        getUnderConstrDocs().put(doc.name, doc);
        addMetadata(doc.name, doc.metadata);
        
        String language = extractLanguageFromPath(doc.name);
        updateDocumentTimestamp(doc, language);
    }

    /**
     * Extract language code from document path.
     * Assumes path format: /siteName/language/... or /siteName/... (for main language)
     */
    private String extractLanguageFromPath(String documentName) {
        if (documentName == null) {
            return null;
        }
        
        String normalizedName = documentName.startsWith("/") ? documentName : "/" + documentName;
        
        // Simple approach: extract second path segment as language code
        String[] parts = normalizedName.split("/");
        if (parts.length >= 3) {
            // parts[0] is empty (starts with /), parts[1] is site name, parts[2] is language
            String potentialLanguage = parts[2];
            if (potentialLanguage.length() >= 2 && potentialLanguage.length() <= 3) {
                return potentialLanguage;
            }
        }
        
        return null;
    }

    private void updateDocumentTimestamp(Document doc, String language) {
        if (documentTimestamps == null) {
            documentTimestamps = new ConcurrentHashMap<>();
        }
        
        String key = language != null ? language : "";
        documentTimestamps
            .computeIfAbsent(doc.name, k -> new ConcurrentHashMap<>())
            .put(key, doc.updateTimestamp);
    }

    @Override
    public void deleteDocument(String path) {
        deleteMetadata(path);
        getDocuments().remove(path);
    }

    @Override
    public long getDocumentsCount() {
        if(reloadInProgress){
            return getUnderConstrDocs().size();
        }else{
            return getDocuments().size();
        }
    }

    @Override
    public void startReload(String siteName) {
        reloadInProgress = true;
        underConstrDocs = new ConcurrentHashMap<>();
    }

    @Override
    public int stopReload(long timestamp, String siteName) {
        int deletedCount = getDocuments().size() - underConstrDocs.size();
        getDocuments().clear();
        documents.putAll(underConstrDocs);
        reloadInProgress = false;
        logger.info("Documents removed: " + deletedCount);
        return deletedCount;
    }

    @Override
    public List<Document> findDocuments(String path, String propertyName, String propertyValue, boolean withContent) {
        ArrayList<Document> docs = new ArrayList<>();
        /*
        if(path!=null){
            getDocuments().forEach((k, v) -> {
                if (k.startsWith(path) && v.hasProperty(propertyName) && v.getProperty(propertyName).equals(propertyValue)) {
                    docs.add(v.clone(withContent));
                }
            });
        }else{
            getDocuments().forEach((k, v) -> {
                if (v.hasProperty(propertyName) && v.getProperty(propertyName).equals(propertyValue)) {
                    docs.add(v.clone(withContent));
                }
            });
        }
        */
        docs.add(new Document());
        docs.add(new Document());
        return docs;
    }

    @Override
    public List<Document> filter(List<Document> docs, String propertyName, String propertyValue) {
        ArrayList<Document> filtered = new ArrayList<>();
        docs.forEach((doc) -> {
            if (doc.hasProperty(propertyName) && doc.getProperty(propertyName).equals(propertyValue)) {
                filtered.add(doc);
            }
        });
        return filtered;
    }

    @Override
    public HashMap<String, String> getMetadata(String name) {
        return null;
    }

    @Override
    public void addMetadata(String name, HashMap<String, String> metadata) {
        //
    }

    @Override
    public void deleteMetadata(String name) {
        //
    }

    @Override
    public Document findFirstDocument(String path, String metadataName, String metadataValue, boolean withContent,
            String sortBy, String sortOrder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findFirstDocument'");
    }

    @Override
    public List<Document> findDocumentsSorted(String path, String metadataName, String metadataValue,
            boolean withContent, String sortBy, String sortOrder) {
                logger.info("FIND DOCUMENTS SORTED");
        ArrayList<Document> docs = new ArrayList<>();
        docs.add(new Document());
        docs.add(new Document());
        return docs;
    }

    @Override
    public List<String> getPaths(String siteRoot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPaths'");
    }

    @Override
    public List<String> getSiteNames() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSiteNames'");
    }

    @Override
    public List<String> searchDocuments(String textToSearch, String languageCode) {
        ArrayList<String> docs = new ArrayList<>();
        docs.add("doc1");
        docs.add("doc2");
        return docs;
        // TODO: filter by languageCode
    }

    // In-memory storage for document timestamps by language
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> documentTimestamps = new ConcurrentHashMap<>();

    @Override
    public long getPreviousUpdateTimestamp(String documentName) {
        return getPreviousUpdateTimestamp(documentName, null);
    }

    @Override
    public long getPreviousUpdateTimestamp(String documentName, String language) {
        ConcurrentHashMap<String, Long> languageTimestamps = documentTimestamps.get(documentName);
        if (languageTimestamps == null) {
            return 0;
        }
        
        String key = language != null ? language : "";
        Long timestamp = languageTimestamps.get(key);
        return timestamp != null ? timestamp : 0;
    }

    @Override
    public long getSize() {
        return getDocuments().size();
    }

}
