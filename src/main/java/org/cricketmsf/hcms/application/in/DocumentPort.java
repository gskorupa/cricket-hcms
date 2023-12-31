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

    public List<Document> getDocs(String path){
        return logic.getDocuments(path);
    }

    

    public void reload(){
        logic.reload();
    }

    public void addDocument(Document document){
        logic.addDocument(document);
    }
    
}
