package org.cricketmsf.hcms.application.in;

import java.util.List;

import org.cricketmsf.hcms.domain.Document;
import org.cricketmsf.hcms.domain.DocumentLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentPort {

    @Inject
    DocumentLogic logic;

    public List<Document> getDocs(String path, boolean withContent){
        return logic.getDocuments(path, withContent);
    }

    public Document getDocument(String path){
        return logic.getDocument(path);
    }
    

    public void reload(){
        logic.reload();
    }

    public List<Document> findDocs(String path, String tagName, String tagValue, String sortBy, String sortOrder, boolean withContent){
        return logic.findDocumentsSorted(path, tagName, tagValue, sortBy, sortOrder);
    }

    public Document findFirstDocument(String path, String tagName, String tagValue, String sortBy, String sortOrder){
        return logic.findFirstDocument(path, tagName, tagValue, sortBy, sortOrder);
    }
    
}
