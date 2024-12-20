package pl.experiot.hcms.adapters.driven.loader.fs;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;

import org.jboss.logging.Logger;

import pl.experiot.hcms.app.logic.dto.Document;

public class DocumentVisitor extends SimpleFileVisitor<Path> {

    static Logger logger = Logger.getLogger(DocumentVisitor.class);

    ArrayList<Document> files = new ArrayList<>();
    HashMap<String, Document> documents = new HashMap<>();
    String root;
    ArrayList<String> excludes = new ArrayList<>();
    String syntax = "github"; // "obsidian", "github"
    String markdownFileExtension = ".md";
    String htmlFileExtension = ".html";
    String jsonFileExtension = ".json";
    String xmlFileExtension = ".xml";

    GithubWikiReader githubWikiReader = new GithubWikiReader();
    HtmlReader htmlReader = new HtmlReader();
    BinaryReader binaryReader = new BinaryReader();
    JsonReader jsonReader = new JsonReader();
    XmlReader xmlReader = new XmlReader();

    public void setRoot(String root) {
        this.root = root;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    public void setMarkdownFileExtension(String markdownFileExtension) {
        this.markdownFileExtension = markdownFileExtension;
    }

    public void setHtmlFileExtension(String htmlFileExtension) {
        this.htmlFileExtension = htmlFileExtension;
    }

    public void exclude(String exclude) {
        excludes.add(exclude);
    }

    public ArrayList<Document> getList() {
        logger.info("documents.size: " + documents.size());
        ArrayList<Document> docs = new ArrayList<>();
        for (Document doc : documents.values()) {
            docs.add(doc);
        }
        documents.clear();
        return docs;
    }

    @Override
    public FileVisitResult visitFile(Path file,
            BasicFileAttributes attr) {
        String name;
        String path;
        String fileName;
        Document doc;
        long updateTimestamp = attr.lastModifiedTime().toMillis();
        if (attr.isSymbolicLink()) {
            // not supported
            return CONTINUE;
        } else if (attr.isRegularFile() || attr.isSymbolicLink()) {
            path = getRelativePath(file, attr);
            name = file.getFileName().toString();
            fileName = path.substring(path.lastIndexOf("/") + 1);
            if (!isExcluded(path)) {
                if (name.endsWith(markdownFileExtension)) {
                    githubWikiReader.parse(file);
                    doc = githubWikiReader.getDocument();
                    doc.mediaType = "text/html";
                    //files.add(doc);
                } else if (name.endsWith(htmlFileExtension)) {
                    htmlReader.parse(file);
                    doc = htmlReader.getDocument();
                    doc.mediaType = "text/html";
                    //files.add(doc);
                } else if (name.toLowerCase().endsWith(jsonFileExtension)) {
                    jsonReader.parse(file);
                    doc = jsonReader.getDocument();
                    doc.mediaType = "application/json";
                    //files.add(doc);
                } else if (name.toLowerCase().endsWith(xmlFileExtension)) {
                    xmlReader.parse(file);
                    doc = xmlReader.getDocument();
                    doc.mediaType = "application/xml";
                    //files.add(doc);
                }else {
                    // binary file
                    binaryReader.parse(file);
                    doc = binaryReader.getDocument();
                    doc.binaryFile = true;
                    doc.mediaType = URLConnection.guessContentTypeFromName(fileName);
                }
                doc.name = path;
                doc.path = path.substring(0, path.lastIndexOf("/") + 1);
                doc.fileName = fileName;
                doc.siteName=doc.getSiteName();
                logger.info("doc.name: " + doc.name);
                logger.debug("doc.path: " + doc.path);
                doc.updateTimestamp = updateTimestamp;
                //files.add(doc);
                documents.put(doc.name, doc);

            } else {
                logger.debug("excluded: " + path);
            }
        } else {
            // not a regular file nor a symbolic link
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir,
            IOException exc) {
        // do nothing
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file,
            IOException exc) {

        return CONTINUE;
    }

    private String getRelativePath(Path file, BasicFileAttributes attr) {
        return file.toAbsolutePath().toString().substring(root.length());
    }

    private boolean isExcluded(String path) {
        for (String exclude : excludes) {
            if (path.startsWith(exclude) || path.startsWith("/" + exclude)) {
                return true;
            }
        }
        return false;
    }
}
