package pl.experiot.hcms.app.ports.driven;

import java.util.Map;

import pl.experiot.hcms.app.logic.Document;

public interface ForTranslatorIface {
    
    Document translate(Document document, String sourceLanguage, String targetLanguage, Map<String, Object> options);
}
