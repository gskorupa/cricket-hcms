package pl.experiot.hcms.app.ports.driven;

import com.vladsch.flexmark.util.ast.Document;

public interface ForTranslatorIface {
    
    Document translate(Document document, String sourceLanguage, String targetLanguage);
    void setMultilanguageRepoModel(String modelName);
}
