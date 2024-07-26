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
    @ConfigProperty(name = "assets.folder.name")
    String assetsFolderName;
    @ConfigProperty(name = "hcms.sevice.url")
    String hcmsServiceUrl;
    

    public void loadDocuments(String path) {
        repositoryPort.startReload();
        logger.debug("loading documents");
        logger.debug("actual path: " + Paths.get(".").toAbsolutePath().normalize().toString());
        logger.debug("getDocuments: " + path);
        logger.debug("complete path: " + root+path);
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
        //EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Path p;
        try {
            p=Paths.get(root+path);
            logger.info("absolute path: " + p.toAbsolutePath().toString());
            //Files.walkFileTree(p, opts, 100, visitor);
            Files.walkFileTree(p, visitor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        files = visitor.getList();
        logger.info("found: " + files.size() + " documents");
        Document doc;
        for(int i=0; i<files.size(); i++) {
            //logger.info("  " + files.get(i).path);
            doc=DocumentTransformer.transform(files.get(i), markdownFileExtension, assetsFolderName, hcmsServiceUrl);
            if(null!=doc){
                repositoryPort.addDocument(doc);
            }
        }
        // for (Document doc : files) {
        //    repositoryPort.addDocument(DocumentTransformer.transform(doc, markdownFileExtension));
        //}
        logger.info("loaded: " + files.size() + " documents");
        logger.info("repositoryPort database size: " + repositoryPort.getDocumentsCount());
        repositoryPort.stopReload();
    }
    
}
