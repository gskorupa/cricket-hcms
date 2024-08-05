package org.cricketmsf.hcms.adapter.out;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.cricketmsf.hcms.application.out.DocumentRepositoryIface;
import org.cricketmsf.hcms.domain.Document;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;

public class DocumentRepositoryH2 implements DocumentRepositoryIface {

    private static Logger logger = Logger.getLogger(DocumentRepositoryH2.class);

    private static AgroalDataSource defaultDataSource;

    public void init(AgroalDataSource dataSource) {
        defaultDataSource = dataSource;
        // create database tables
        logger.debug("Document repository initializing (H2) ...");

        // create table documents
        String sql = "CREATE TABLE IF NOT EXISTS documents ("
                + "path VARCHAR(255) NOT NULL, "
                + "name VARCHAR(255) PRIMARY KEY, "
                + "file_name VARCHAR(255), "
                + "content TEXT, "
                + "binary BOOLEAN, "
                + "binary_content BLOB,"
                + "media_type VARCHAR(100),"
                + "created TIMESTAMP, "
                + "modified TIMESTAMP,"
                + "refreshed TIMESTAMP"
                + ")";

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
                + ")";

        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("Document repository started");

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
        String sql = "SELECT d.* FROM documents d, metadata m WHERE d.name = m.d_name AND d.path = ? AND m.m_name = ? AND m.m_value = ?";
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
        deleteMetadata(doc.name);
        String sql = """
                MERGE INTO documents (path, name, file_name, content, binary, binary_content, created, modified, refreshed, media_type)
                KEY (NAME)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error("Error adding document: " + doc.path);
            e.printStackTrace();
        }
        addMetadata(doc.name, doc.metadata);
    }

    @Override
    public void deleteDocument(String name) {
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
    public void startReload() {
        // nothing to do
    }

    @Override
    public void stopReload(long timestamp, String siteName) {
        // remove all documents from the repository which were refreshed before the
        // timestamp -
        // it means that they were not been read from file system during the last reload
        // (they were deleted)
        String sql = "DELETE FROM documents WHERE name LIKE ? AND refreshed < ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, "/"+siteName + "/%");
            statement.setTimestamp(2, new java.sql.Timestamp(timestamp));
            int rows=statement.executeUpdate();
            logger.info("Documents removed: "+rows);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            result = "[bad StackTrace level "+level+"] "+message;
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

}
