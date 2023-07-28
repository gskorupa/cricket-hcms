package org.cricketmsf.hcms.application.out;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.cricketmsf.hcms.domain.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentRepositoryLoader {

    @Inject
    Logger logger;

    @Inject
    DocumentRepositoryPort repositoryPort;

    @Inject
    DocumentRepositoryLoader loader;
    
    @ConfigProperty(name = "document.folders.root")
    String root;
    @ConfigProperty(name = "document.folders.excluded")
    String excludes;
    @ConfigProperty(name = "document.syntax")
    String syntax; /* "obsidian", "github" */
    @ConfigProperty(name = "document.extension.markdown")
    String markdownFileExtension;
    @ConfigProperty(name = "document.extension.html")
    String htmlFileExtension;
    

    public void loadDocuments(String path) {
        logger.info("actual path: " + Paths.get(".").toAbsolutePath().normalize().toString());
        logger.info("getDocuments: " + path);
        logger.info("complete path: " + root+path);
        ArrayList<Document> files = new ArrayList<>();
        DocumentVisitor visitor = new DocumentVisitor();
        visitor.setRoot(Paths.get(root+path).toAbsolutePath().toString());
        visitor.setSyntax(syntax);
        visitor.setMarkdownFileExtension(markdownFileExtension);
        visitor.setHtmlFileExtension(htmlFileExtension);
        String excludeList[] = excludes.split(";");
        for (String exclude : excludeList) {
            visitor.exclude(exclude);
        }
        Path p;
        try {
            p=Paths.get(root+path);
            logger.info("absolute path: " + p.toAbsolutePath().toString());
            Files.walkFileTree(p, visitor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        files = visitor.getList();
        logger.info("found: " + files.size() + " documents");
        for(int i=0; i<files.size(); i++) {
            logger.info("  " + files.get(i).path);
            repositoryPort.addDocument(DocumentTransformer.transform(files.get(i), markdownFileExtension));
        }
        /* for (Document doc : files) {
            repositoryPort.addDocument(DocumentTransformer.transform(doc, markdownFileExtension));
        } */
        logger.info("loaded: " + files.size() + " documents");
        logger.info("repositoryPort database size: " + repositoryPort.getDocumentsCount());
    }
    
}
