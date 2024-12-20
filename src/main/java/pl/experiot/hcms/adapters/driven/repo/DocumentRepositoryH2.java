package pl.experiot.hcms.adapters.driven.repo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.vertx.mutiny.core.eventbus.EventBus;
import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.ports.driven.ForDocumentRepositoryIface;

public class DocumentRepositoryH2 implements ForDocumentRepositoryIface {

    private static Logger logger = Logger.getLogger(DocumentRepositoryH2.class);
    private EventBus eventBus;
    private String queueName = null;

    private static AgroalDataSource defaultDataSource;

    @Override
    public void init(AgroalDataSource dataSource) {
        defaultDataSource = dataSource;
        // create database tables
        logger.debug("Document repository initializing (H2) ...");

        String sql;
        // drop to update structure if needed
        /* sql = "DROP TABLE IF EXISTS documents; DROP TABLE IF EXISTS metadata";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } */

        // create table documents
        sql = "CREATE TABLE IF NOT EXISTS documents ("
                + "path VARCHAR(255) NOT NULL, "
                + "name VARCHAR(255) PRIMARY KEY, "
                + "file_name VARCHAR(255), "
                + "content TEXT NOT NULL DEFAULT '', "
                + "binary BOOLEAN, "
                + "binary_content BLOB,"
                + "media_type VARCHAR(100),"
                + "created TIMESTAMP, "
                + "modified TIMESTAMP,"
                + "refreshed TIMESTAMP,"
                + "site VARCHAR(255),"
                + "origin VARCHAR(255) DEFAULT ''"
                + "); commit;";

        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // create table metadata
        sql = "CREATE TABLE IF NOT EXISTS metadata ("
                + "d_name VARCHAR(255) NOT NULL, "
                + "m_name VARCHAR(255) NOT NULL, "
                + "m_value VARCHAR(255),"
                + "PRIMARY KEY (d_name, m_name)"
                + ");";

        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("Document repository started");

        String sql2 = "CREATE ALIAS IF NOT EXISTS FT_INIT FOR 'org.h2.fulltext.FullText.init'; CALL FT_INIT();";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(sql2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sql2 = "CALL FT_CREATE_INDEX('PUBLIC', 'DOCUMENTS', 'CONTENT');";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(sql2);
        } catch (Exception e) {
            //e.printStackTrace();
            logger.warn("It's probably OK if this isn't your first time running it: "+e.getMessage());
        }

        // document update timestamp by language
        sql = "CREATE TABLE IF NOT EXISTS document_updates ("
                + "name VARCHAR(255) NOT NULL, "
                + "modification_ts TIMESTAMP NOT NULL,"
                + "ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";

        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // create index
        sql = "CREATE INDEX IF NOT EXISTS document_updates_idx ON document_updates (name, ts);";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("Document repository initialized. Document count: " + getDocumentsCount());

    }

    @Override
    public void setEventBus(EventBus eventBus, String queName) {
        this.eventBus = eventBus;
        this.queueName = queName;
    }

    @Override
    public List<Document> getDocuments(String path, boolean withContent) {
        ArrayList<Document> docs = new java.util.ArrayList<Document>();
        String pathToSearch = path.startsWith("/") ? path : "/" + path;
        pathToSearch = pathToSearch.endsWith("/") ? pathToSearch : pathToSearch + "/";
        logger.debug("getDocuments: " + pathToSearch);
        String sql = "SELECT * FROM documents WHERE path = ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, pathToSearch);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    var doc = new Document();
                    doc.path = resultSet.getString("path");
                    doc.name = resultSet.getString("name");
                    doc.fileName = resultSet.getString("file_name");
                    doc.binaryFile = resultSet.getBoolean("binary");
                    if (withContent) {
                        doc.content = resultSet.getString("content");
                        doc.binaryContent = resultSet.getBytes("binary_content");
                    } else {
                        doc.content = "";
                        doc.binaryContent = new byte[0];
                    }
                    doc.mediaType = resultSet.getString("media_type");
                    doc.updateTimestamp = resultSet.getTimestamp("modified").getTime();
                    doc.refreshTimestamp = resultSet.getTimestamp("refreshed").getTime();
                    doc.siteName = resultSet.getString("site");
                    docs.add(doc);
                }
                resultSet.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            doc.metadata = getMetadata(doc.name);
            docs.set(i, doc);
        }
        return docs;
    }

    @Override
    public List<Document> getAllDocuments(boolean noContent) {
        ArrayList<Document> docs = new java.util.ArrayList<Document>();
        String sql = "SELECT * FROM documents";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                var doc = new Document();
                doc.path = resultSet.getString("path");
                doc.name = resultSet.getString("name");
                doc.fileName = resultSet.getString("file_name");
                if (!noContent) {
                    doc.content = resultSet.getString("content");
                    doc.binaryFile = resultSet.getBoolean("binary");
                }
                doc.binaryContent = new byte[0];
                doc.mediaType = resultSet.getString("media_type");
                doc.updateTimestamp = resultSet.getTimestamp("modified").getTime();
                doc.refreshTimestamp = resultSet.getTimestamp("refreshed").getTime();
                doc.siteName = resultSet.getString("site");
                docs.add(doc);
            }
            resultSet.close();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            doc.metadata = getMetadata(doc.name);
            docs.set(i, doc);
        }
        return docs;
    }

    @Override
    public List<Document> findDocuments(String path, String metadataName, String metadataValue, boolean withContent) {
        logger.info("findDocuments: " + path + " " + metadataName + ":" + metadataValue);
        ArrayList<Document> docs = new java.util.ArrayList<Document>();
        String sql = "SELECT * FROM documents WHERE path = ? AND name IN (SELECT d_name FROM metadata WHERE m_name = ? AND m_value = ?)";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, path);
            statement.setString(2, metadataName);
            statement.setString(3, metadataValue);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    var doc = new Document();
                    doc.path = resultSet.getString("path");
                    doc.name = resultSet.getString("name");
                    doc.fileName = resultSet.getString("file_name");
                    doc.binaryFile = resultSet.getBoolean("binary");
                    if (withContent) {
                        doc.content = resultSet.getString("content");
                        doc.binaryContent = resultSet.getBytes("binary_content");
                    } else {
                        doc.content = "";
                        doc.binaryContent = new byte[0];
                    }
                    doc.mediaType = resultSet.getString("media_type");
                    doc.updateTimestamp = resultSet.getTimestamp("modified").getTime();
                    doc.refreshTimestamp = resultSet.getTimestamp("refreshed").getTime();
                    doc.siteName = resultSet.getString("site");
                    docs.add(doc);
                }
                resultSet.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            doc.metadata = getMetadata(doc.name);
            docs.set(i, doc);
        }
        return docs;
    }

    @Override
    public List<Document> filter(List<Document> docs, String metadataName, String metadataValue) {
        List<Document> filtered = new java.util.ArrayList<>();
        for (var doc : docs) {
            if (doc.metadata.containsKey(metadataName) && doc.metadata.get(metadataName).equals(metadataValue)) {
                filtered.add(doc);
            }
        }
        return filtered;
    }

    @Override
    public Document getDocument(String name) {
        Document doc = null;
        String nameToSearch = name.startsWith("/") ? name : "/" + name;
        logger.debug("getDocument: " + nameToSearch);
        String sql = "SELECT * FROM documents WHERE name = ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, nameToSearch);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    doc = new Document();
                    doc.path = resultSet.getString("path");
                    doc.name = resultSet.getString("name");
                    doc.fileName = resultSet.getString("file_name");
                    doc.content = resultSet.getString("content");
                    doc.mediaType = resultSet.getString("media_type");
                    doc.binaryFile = resultSet.getBoolean("binary");
                    doc.binaryContent = resultSet.getBytes("binary_content");
                    doc.updateTimestamp = resultSet.getTimestamp("modified").getTime();
                    doc.refreshTimestamp = resultSet.getTimestamp("refreshed").getTime();
                    doc.siteName = resultSet.getString("site");
                    logger.debug("binary content size: " + doc.binaryContent.length);
                }
                resultSet.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (doc != null) {
            doc.metadata = getMetadata(doc.name);
        }
        return doc;
    }

    @Override
    public void addDocument(Document doc) {
        addDocument(doc, "");
    }

    @Override
    public void addDocument(Document doc, String origin) {
        logger.info("addDocumentToH2: " + doc.name);
        /*
         * if(getAllDocuments(false).size()>100){
         * logger.
         * info("Too many documents in the repository. Skipping adding document: " +
         * doc.name);
         * return;
         * }
         */
        deleteMetadata(doc.name);
        String sql = """
                MERGE INTO documents (path, name, file_name, content, binary, binary_content, created, modified, refreshed, media_type, site, origin)
                KEY (NAME)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, doc.path);
            statement.setString(2, doc.name);
            statement.setString(3, doc.fileName);
            statement.setString(4, doc.content);
            statement.setBoolean(5, doc.binaryFile);
            if (doc.binaryContent == null) {
                doc.binaryContent = new byte[0];
            }
            logger.debug("binary content size: " + doc.binaryContent.length);
            statement.setBytes(6, doc.binaryContent);
            statement.setTimestamp(7, new java.sql.Timestamp(doc.updateTimestamp));
            statement.setTimestamp(8, new java.sql.Timestamp(doc.updateTimestamp));
            statement.setTimestamp(9, new java.sql.Timestamp(doc.refreshTimestamp));
            statement.setString(10, doc.mediaType);
            statement.setString(11, doc.siteName);
            statement.setString(12, origin);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error("Error adding document: " + doc.path);
            e.printStackTrace();
        }
        addMetadata(doc.name, doc.metadata);
        updateDocumentTimestamp(doc);
        eventBus.publish(queueName, doc.name + ";" + doc.updateTimestamp);
    }

    private void updateDocumentTimestamp(Document doc) {
        String sql = "INSERT INTO document_updates (name, modification_ts) VALUES (?, ?)";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, doc.name);
            statement.setTimestamp(2, new java.sql.Timestamp(doc.updateTimestamp));
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get document's previous update timestamp.
     */
    @Override
    public long getPreviousUpdateTimestamp(String documentName) {
        String sql = "SELECT modification_ts FROM document_updates WHERE name = ? ORDER BY modification_ts DESC LIMIT 2";
        long[] timestamps = new long[2];
        timestamps[0] = 0;
        timestamps[1] = 0;
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, documentName);
            try (var resultSet = statement.executeQuery()) {
                int i = 0;
                while (resultSet.next()) {
                    timestamps[i] = resultSet.getTimestamp("modification_ts").getTime();
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("Last update timestamps: " + timestamps[0] + " " + timestamps[1]);
        return timestamps[1];
    }

    @Override
    public void deleteDocument(String name) {
        deleteMetadata(name);
        String sql = "DELETE FROM documents WHERE name = ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        deleteMetadata(name);
    }

    @Override
    public long getDocumentsCount() {
        String sql = "SELECT COUNT(*) FROM documents";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            long result = resultSet.getLong(1);
            resultSet.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public void startReload(String siteName) {
        // nothing to do
    }

    @Override
    public void stopReload(long timestamp, String siteName) {
        // remove all documents from the repository which were refreshed before the
        // timestamp -
        // it means that they were not been read from file system during the last reload
        // (they were deleted)
/*         String sql = "DELETE FROM documents WHERE name LIKE ? AND refreshed < ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, "/" + siteName + "/%");
            statement.setTimestamp(2, new java.sql.Timestamp(timestamp));
            int rows = statement.executeUpdate();
            logger.info("Documents removed: " + rows);
        } catch (Exception e) {
            e.printStackTrace();
        } */

        // get document names
        ArrayList<String> docNames = new ArrayList<>();
        String sql = "SELECT name FROM documents WHERE name LIKE ? AND refreshed < ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, "/" + siteName + "/%");
            statement.setTimestamp(2, new java.sql.Timestamp(timestamp));
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    docNames.add(resultSet.getString("name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        docNames.forEach((name) -> {
            deleteLanguageVersions(sql);
            deleteDocument(name);
            deleteMetadata(siteName);
        });
    }

    private void deleteLanguageVersions(String origin) {
        ArrayList<String> docNames = new ArrayList<>();
        String sql = "SELECT name FROM documents WHERE origin=?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, origin);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    docNames.add(resultSet.getString("name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        docNames.forEach((name) -> {
            deleteDocument(name);
            deleteMetadata(name);
        });
    }

    private String getExtendedLogMessage(int level, String message) {
        String result = "";
        try {
            String fullClassName = Thread.currentThread().getStackTrace()[level].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[level].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[level].getLineNumber();
            result = className + "." + methodName + "()[" + lineNumber + "]: " + message;
        } catch (Exception e) {
            result = "[bad StackTrace level " + level + "] " + message;
        }
        return result;
    }

    public HashMap<String, String> getMetadata(String name) {
        logger.debug(getExtendedLogMessage(3, "getMetadata: " + name));
        String sql = "SELECT * FROM metadata WHERE d_name = ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql);) {
            statement.setString(1, name);
            try (var resultSet = statement.executeQuery()) {
                var metadata = new HashMap<String, String>();
                String key;
                String value;
                while (resultSet.next()) {
                    key = resultSet.getString("m_name");
                    value = resultSet.getString("m_value");
                    metadata.put(key, value);
                    logger.debug("metadata: " + key + ":" + value);
                }
                resultSet.close();
                logger.debug("metadata size: " + metadata.size());
                return metadata;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public void addMetadata(String name, HashMap<String, String> metadata) {
        String sql = """
                MERGE INTO metadata (d_name, m_name, m_value)
                KEY (D_NAME, M_NAME)
                VALUES (?, ?, ?)
                """;
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            for (var entry : metadata.entrySet()) {
                statement.setString(1, name);
                statement.setString(2, entry.getKey());
                statement.setString(3, entry.getValue());
                statement.executeUpdate();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteMetadata(String name) {
        String sql = "DELETE FROM metadata WHERE d_name = ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Document> findDocumentsSorted(String path, String metadataName, String metadataValue,
            boolean withContent,
            String sortBy, String sortOrder) {
        // Find document names by metadata and use it to get document list sorted by
        // document's
        // metadata 'published' value
        // Then get the first document from the list
        List<Document> docs = findDocuments(path, metadataName, metadataValue, withContent);
        if (docs.size() == 0) {
            return List.of();
        }
        return sort(docs, sortBy, sortOrder);
    }

    @Override
    public Document findFirstDocument(String path, String metadataName, String metadataValue, boolean withContent,
            String sortBy, String sortOrder) {
        // Find document names by metadata and use it to get document list sorted by
        // document's
        // metadata 'published' value
        // Then get the first document from the list
        List<Document> docs = findDocuments(path, metadataName, metadataValue, withContent);
        if (docs.size() == 0) {
            return null;
        }
        return sort(docs, sortBy, sortOrder).get(0);
    }

    /**
     * Sort the list of documents by the given metadata field value.
     * 
     * @param docs
     * @param sortBy
     * @param sortOrder
     * @return
     */
    private List<Document> sort(List<Document> docs, String sortBy, String sortOrder) {
        if (sortBy == null || sortBy.isEmpty()) {
            return docs;
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "asc";
        }
        if (sortOrder.equals("asc")) {
            docs.sort((Document d1, Document d2) -> {
                try {
                    return d1.metadata.get(sortBy).compareTo(d2.metadata.get(sortBy));
                } catch (NullPointerException e) {
                    // In this case the document will go to the end of the list regardless of the
                    // sorting direction
                    return 1;
                }
            });
        } else {
            docs.sort((Document d1, Document d2) -> {
                try {
                    return d2.metadata.get(sortBy).compareTo(d1.metadata.get(sortBy));
                } catch (NullPointerException e) {
                    // In this case the document will go to the end of the list regardless of the
                    // sorting direction
                    return 1;
                }
            });
        }
        return docs;
    }

    @Override
    public List<String> getPaths(String siteRoot) {
        String sql = "SELECT DISTINCT path FROM documents WHERE site = ? ORDER BY path";
        ArrayList<String> paths = new ArrayList<>();
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, siteRoot);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    paths.add(resultSet.getString("path"));
                }
                resultSet.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
        return paths;
    }

    @Override
    public List<String> getSiteNames() {
        String sql = "SELECT DISTINCT site FROM documents ORDER BY site";
        ArrayList<String> sites = new ArrayList<>();
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sites.add(resultSet.getString("site"));
                }
                resultSet.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
        return sites;
    }

    @Override
    public List<String> searchDocuments(String textToSearch, String languageCode) {
        // full text search
        ArrayList<String> docs = new java.util.ArrayList<>();
        String sql = "SELECT * FROM FT_SEARCH_DATA(?, 0, 0);";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, textToSearch);
            try (ResultSet resultSet = statement.executeQuery()) {
                String documentName;
                while (resultSet.next()) {
                    documentName = resultSet.getString(4);
                    // remove [ and ] from beginning and end of the document name, because H2 full
                    // text search returns array of strings here
                    documentName = documentName.substring(1, documentName.length() - 1);
                    docs.add(documentName);
                }
                resultSet.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
        return docs;
    }

}
