package pl.experiot.hcms.adapters.driven.loader.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import pl.experiot.hcms.app.logic.Document;

public class BinaryReader {

    Document doc = null;

    public BinaryReader() {
    }

    public Document getDocument() {
        return doc;
    } 

    public void parse(Path file) {
        doc = new Document();
        doc.binaryFile=true;
        try {
            doc.binaryContent = Files.readAllBytes(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
