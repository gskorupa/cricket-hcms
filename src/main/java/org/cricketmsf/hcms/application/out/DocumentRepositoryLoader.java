package org.cricketmsf.hcms.application.out;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.cricketmsf.hcms.domain.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.arjuna.ats.internal.jdbc.drivers.modifiers.list;

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
    @ConfigProperty(name = "hcms.sevice.url")
    String hcmsServiceUrl;
    @ConfigProperty(name = "document.folders.sites")
    String sites;
    @ConfigProperty(name = "document.folders.assets")
    String assets;
    

    public void loadDocuments(String siteRoot, boolean start, boolean stop, long timestamp) {
        String docPath=siteRoot;
        if(!docPath.isEmpty()){
            docPath="/"+docPath;
        }
        if(start){
            repositoryPort.startReload();
        }
        String[] sitesList = sites.split(";");
        String[] assetsList = assets.split(";");
        String[] hcmsServiceList = hcmsServiceUrl.split(";");
        String[] excludedList = excludes.split(";");
        logger.debug("loading documents");
        logger.debug("actual path: " + Paths.get(".").toAbsolutePath().normalize().toString());
        logger.debug("getDocuments: " + docPath);
        logger.debug("complete path: " + root+docPath);
        ArrayList<Document> files = new ArrayList<>();
        DocumentVisitor visitor = new DocumentVisitor();
        visitor.setRoot(Paths.get(root+docPath).toAbsolutePath().toString());
        visitor.setSyntax(syntax);
        visitor.setMarkdownFileExtension(markdownFileExtension);
        visitor.setHtmlFileExtension(htmlFileExtension);
        int siteIndex = getSiteIndex(sitesList,siteRoot);
        String siteExcludedFolders[];
        if(siteIndex>=0){
            siteExcludedFolders = excludedList[siteIndex].split(",");
            for (String exclude : siteExcludedFolders) {
                visitor.exclude(exclude);
            }
        }
        //EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Path p;
        try {
            p=Paths.get(root+docPath);
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
            doc=DocumentTransformer.transform(files.get(i), markdownFileExtension, siteRoot, assetsList[0], hcmsServiceList[0]);
            if(null!=doc){
                doc.refreshTimestamp=timestamp;
                repositoryPort.addDocument(doc);
            }
        }
        // for (Document doc : files) {
        //    repositoryPort.addDocument(DocumentTransformer.transform(doc, markdownFileExtension));
        //}
        logger.info("loaded: " + files.size() + " documents");
        logger.info("repositoryPort database size: " + repositoryPort.getDocumentsCount());
        if(stop){
            repositoryPort.stopReload(timestamp);
            listAll();
        }
    }

    private void listAll(){
        ArrayList<Document> docs = (ArrayList<Document>) repositoryPort.getAllDocuments(false);
        logger.info("repositoryPort database size: " + docs.size());
        logger.info("listing all documents");
        for (Document doc : docs) {
            logger.info(doc.name);
        }

    }

    private int getSiteIndex(String[] sitesList, String siteRoot){
        for(int i=0; i<sitesList.length; i++){
            if(sitesList[i].equals(siteRoot)){
                return i;
            }
        }
        return -1;
    }
    
}
