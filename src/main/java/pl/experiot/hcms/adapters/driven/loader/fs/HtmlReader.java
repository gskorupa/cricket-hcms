package pl.experiot.hcms.adapters.driven.loader.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import pl.experiot.hcms.app.logic.dto.Document;

public class HtmlReader {

    Document doc = null;

    public HtmlReader() {
    }

    public Document getDocument() {
        return doc;
    }

    public void parse(Path file) {
        doc = new Document();
        boolean comment = false;
        boolean summary = false;
        boolean metadata = false;
        StringBuilder sbContent = new StringBuilder();
        StringBuilder sbSummary = new StringBuilder();
        try (InputStream in = Files.newInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = null;
            String lineTrimmed = null;
            while ((line = reader.readLine()) != null) {
                lineTrimmed = line.trim().toLowerCase();
                if (lineTrimmed.startsWith("<!--")) {
                    comment = true;
                    if (lineTrimmed.contains("metadata")) {
                        metadata = true;
                        summary = false;
                    }else if(lineTrimmed.contains("summary")){
                        metadata = false;
                        summary = true;
                    }
                    if(lineTrimmed.endsWith("-->")){
                        comment = false;
                        metadata = false;
                        summary = false;
                    }
                    continue;
                } else if (lineTrimmed.endsWith("-->")) {
                    comment = false;
                    summary = false;
                    metadata = false;
                    continue;
                }
                if (!comment) {
                    sbContent.append(line).append("\r\n");
                } else if (metadata) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        doc.metadata.put(parts[0].trim(), parts[1].trim());
                    }
                } else if (summary) {
                    sbSummary.append(line).append("\r\n");
                }
            }
        } catch (IOException x) {
            System.err.println(x);
        }
        doc.content = sbContent.toString();
        doc.metadata.put("summary", sbSummary.toString());
    }
}
