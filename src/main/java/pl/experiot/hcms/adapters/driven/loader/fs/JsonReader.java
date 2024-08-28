package pl.experiot.hcms.adapters.driven.loader.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import pl.experiot.hcms.app.logic.Document;

public class JsonReader {

    Document doc = null;

    public JsonReader() {
    }

    public Document getDocument() {
        return doc;
    } 

    public void parse(Path file) {
        doc = new Document();
        StringBuilder sbContent = new StringBuilder();
        try (InputStream in = Files.newInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                    sbContent.append(line);
            }
        } catch (IOException x) {
            System.err.println(x);
        }
        doc.content = sbContent.toString();
    }
}
