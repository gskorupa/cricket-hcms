package org.cricketmsf.hcms.app.driven_ports;

import com.vladsch.flexmark.util.ast.Document;

public interface ForTranslatorIface {
    
    Document translate(Document document, String sourceLanguage, String targetLanguage);
    void setMultilanguageRepoModel(String modelName);
}
