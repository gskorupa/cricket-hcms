package org.cricketmsf.hcms.application.out;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import org.cricketmsf.hcms.domain.Document;

public class DocumentVisitor extends SimpleFileVisitor<Path> {

    ArrayList<Document> files = new ArrayList<>();
    String root;
    ArrayList<String> excludes = new ArrayList<>();
    String syntax = "github";
    String markdownFileExtension = ".md";
    String htmlFileExtension = ".html";

    GithubWikiReader githubWikiReader = new GithubWikiReader();
    HtmlReader htmlReader = new HtmlReader();
    BinaryReader binaryReader = new BinaryReader();

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
        return files;
    }

    @Override
    public FileVisitResult visitFile(Path file,
            BasicFileAttributes attr) {
        String name;
        String path;
        Document doc;
        long udateTimestamp = attr.lastModifiedTime().toMillis();
        if (attr.isSymbolicLink()) {
            // not supported
            return CONTINUE;
        } else if (attr.isRegularFile() || attr.isSymbolicLink()) {
            path = getRelativePath(file, attr);
            name = file.getFileName().toString();
            if (!isExcluded(path)) {
                if (name.endsWith(markdownFileExtension)) {
                    githubWikiReader.parse(file);
                    doc = githubWikiReader.getDocument();
                    System.out.println("path: " + path);
                    System.out.println("root: " + root);
                    // doc.path = path.substring(path.indexOf(root)+root.length());
                    doc.name = path;
                    doc.path = path.substring(0, path.lastIndexOf("/") + 1);
                    System.out.println("doc.name: " + doc.name);
                    System.out.println("doc.path: " + doc.path);
                    doc.updateTimestamp = udateTimestamp;
                    doc.mediaType = "text/html";
                    files.add(doc);
                } else if (name.endsWith(htmlFileExtension)) {
                    htmlReader.parse(file);
                    doc = htmlReader.getDocument();
                    System.out.println("path: " + path);
                    System.out.println("root: " + root);
                    doc.name = path;
                    doc.path = path.substring(0, path.lastIndexOf("/") + 1);
                    System.out.println("doc.name: " + doc.name);
                    System.out.println("doc.path: " + doc.path);
                    doc.updateTimestamp = udateTimestamp;
                    doc.mediaType = "text/html";
                    files.add(doc);
                } else {
                    // binary file
                    binaryReader.parse(file);
                    doc = binaryReader.getDocument();
                    doc.name = path;
                    doc.path = path.substring(0, path.lastIndexOf("/") + 1);
                    System.out.println("doc.name: " + doc.name);
                    System.out.println("doc.path: " + doc.path);
                    doc.updateTimestamp = udateTimestamp;
                    doc.binaryFile = true;
                    doc.mediaType = URLConnection.guessContentTypeFromName(doc.name);
                    files.add(doc);
                }
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
        /* if(attr.isSymbolicLink()){
            try {
                String tmp=file.toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
                System.out.println("real path: " + tmp);
                //return file.toRealPath().toString().substring(root.length());
                return tmp;
            } catch (IOException e) {
                e.printStackTrace();
                return file.toAbsolutePath().toString().substring(root.length());
            }
        }else{ */
            return file.toAbsolutePath().toString().substring(root.length());
        //}
    }

    private boolean isExcluded(String path) {
        for (String exclude : excludes) {
            if (path.startsWith(exclude)) {
                return true;
            }
        }
        return false;
    }
}
