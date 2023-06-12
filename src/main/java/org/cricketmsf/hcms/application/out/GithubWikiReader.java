package org.cricketmsf.hcms.application.out;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.cricketmsf.hcms.domain.Document;

public class GithubWikiReader {

    Document doc = new Document();

    public GithubWikiReader() {
    }

    public Document getDocument() {
        return doc;
    } 

    public void parse(Path file) {
        boolean comment = false;
        boolean summary = false;
        boolean metadata = false;
        StringBuilder sbContent = new StringBuilder();
        StringBuilder sbSummary = new StringBuilder();
        try (InputStream in = Files.newInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if(line.trim().startsWith("<!-- metadata")){
                    comment=true;
                    metadata=true;
                    continue;
                }else if(line.trim().startsWith("<!-- summary")){
                    comment=true;
                    summary=true;
                    continue;
                }else if(line.trim().startsWith("-->")){
                    comment=false;
                    summary=false;
                    metadata=false;
                    continue;
                }
                if(!comment){
                    sbContent.append(line).append("\r\n");
                }else if(metadata){
                    String[] parts = line.split(":");
                    if(parts.length>1){
                        doc.metadata.put(parts[0].trim(), parts[1].trim());
                    }
                } else if(summary){
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
