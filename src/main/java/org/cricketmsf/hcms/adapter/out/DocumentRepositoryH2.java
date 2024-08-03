package org.cricketmsf.hcms.adapter.out;

import java.util.List;

import org.cricketmsf.hcms.application.out.DocumentRepositoryIface;
import org.cricketmsf.hcms.domain.Document;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;

//@ApplicationScoped
public class DocumentRepositoryH2 implements DocumentRepositoryIface {

    //@Inject
    private static Logger logger = Logger.getLogger(DocumentRepositoryH2.class);
    
    private static AgroalDataSource defaultDataSource;

    public void init(AgroalDataSource dataSource) {
        defaultDataSource = dataSource;
        // create database tables
        logger.info("Document repository initializing (H2) ...");

        // create table documents
        String sql = "CREATE TABLE IF NOT EXISTS documents ("
                + "path VARCHAR(255) NOT NULL, "
                + "name VARCHAR(255) PRIMARY KEY, "
                + "file_name VARCHAR(255), "
                + "content TEXT, "
                + "binary BOOLEAN, "
                + "binary_content BLOB,"
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
        logger.info("Document repository started");

    }

    @Override
    public List<Document> getDocuments(String path, boolean withContent) {
        String pathToSearch = path.startsWith("/") ? path : "/" + path;
        pathToSearch = pathToSearch.endsWith("/") ? pathToSearch : pathToSearch + "/";
        logger.info("getDocuments: " + pathToSearch);
        String sql = "SELECT * FROM documents WHERE path = ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, pathToSearch);
            var resultSet = statement.executeQuery();
            var docs = new java.util.ArrayList<Document>();
            while (resultSet.next()) {
                var doc = new Document();
                doc.path = resultSet.getString("path");
                doc.name = resultSet.getString("name");
                doc.fileName = resultSet.getString("file_name");
                doc.content = resultSet.getString("content");
                doc.binaryFile = resultSet.getBoolean("binary");
                doc.binaryContent = resultSet.getBytes("binary_content");
                doc.updateTimestamp = resultSet.getTimestamp("modified").getTime();
                doc.refreshTimestamp = resultSet.getTimestamp("refreshed").getTime();
                docs.add(doc);
            }
            resultSet.close();
            return docs;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }

    }

    @Override
    public List<Document> getAllDocuments(boolean noContent) {
        String sql = "SELECT * FROM documents";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery(sql)) {
            var docs = new java.util.ArrayList<Document>();
            while (resultSet.next()) {
                var doc = new Document();
                doc.path = resultSet.getString("path");
                doc.name = resultSet.getString("name");
                doc.fileName = resultSet.getString("file_name");
                doc.content = resultSet.getString("content");
                doc.binaryFile = resultSet.getBoolean("binary");
                doc.binaryContent = resultSet.getBytes("binary_content");
                doc.updateTimestamp = resultSet.getTimestamp("modified").getTime();
                doc.refreshTimestamp = resultSet.getTimestamp("refreshed").getTime();
                docs.add(doc);
            }
            return docs;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @Override
    public List<Document> findDocuments(String path, String metadataName, String metadataValue, boolean withContent) {
        String sql = "SELECT d.* FROM documents d JOIN metadata m ON d.name = m.d_name WHERE d.path = ? AND m.m_name = ? AND m.m_value = ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, path);
            statement.setString(2, metadataName);
            statement.setString(3, metadataValue);
            var resultSet = statement.executeQuery();
            if (resultSet.next()) {
                var doc = new Document();
                doc.path = resultSet.getString("path");
                doc.name = resultSet.getString("name");
                doc.fileName = resultSet.getString("file_name");
                doc.content = resultSet.getString("content");
                doc.binaryFile = resultSet.getBoolean("binary");
                doc.binaryContent = resultSet.getBytes("binary_content");
                doc.updateTimestamp = resultSet.getTimestamp("modified").getTime();
                doc.refreshTimestamp = resultSet.getTimestamp("refreshed").getTime();
                return List.of(doc);
            } else {
                return List.of();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
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
        String nameToSearch = name.startsWith("/") ? name : "/" + name;
        logger.info("getDocument: " + nameToSearch);
        String sql = "SELECT * FROM documents WHERE name = ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, nameToSearch);
            var resultSet = statement.executeQuery();
            if (resultSet.next()) {
                var doc = new Document();
                doc.path = resultSet.getString("path");
                doc.name = resultSet.getString("name");
                doc.fileName = resultSet.getString("file_name");
                doc.content = resultSet.getString("content");
                doc.binaryFile = resultSet.getBoolean("binary");
                doc.binaryContent = resultSet.getBytes("binary_content");
                doc.updateTimestamp = resultSet.getTimestamp("modified").getTime();
                doc.refreshTimestamp = resultSet.getTimestamp("refreshed").getTime();
                return doc;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void addDocument(Document doc) {
        String sql = "INSERT INTO documents (path, name, file_name, content, binary, binary_content, created, modified, refreshed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, doc.path);
            statement.setString(2, doc.name);
            statement.setString(3, doc.fileName);
            statement.setString(4, doc.content);
            statement.setBoolean(5, doc.binaryFile);
            statement.setBytes(6, doc.binaryContent);
            statement.setTimestamp(7, new java.sql.Timestamp(doc.updateTimestamp));
            statement.setTimestamp(8, new java.sql.Timestamp(doc.updateTimestamp));
            statement.setTimestamp(9, new java.sql.Timestamp(doc.refreshTimestamp));
            statement.executeUpdate();
        } catch (Exception e) {
            logger.error("Error adding document: " + doc.path);
        }
    }

    @Override
    public void deleteDocument(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteDocument'");
    }

    @Override
    public long getDocumentsCount() {
        String sql = "SELECT COUNT(*) FROM documents";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
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
    public void stopReload(long timestamp) {
        // remove all documents from the repository which were refreshed before the timestamp -
        // it means that they were not been read from file system during the last reload (they were deleted)
        String sql = "DELETE FROM documents WHERE refreshed < ?";
        try (var connection = defaultDataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, new java.sql.Timestamp(timestamp));
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
