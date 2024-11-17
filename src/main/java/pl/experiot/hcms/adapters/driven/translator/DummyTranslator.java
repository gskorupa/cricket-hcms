package pl.experiot.hcms.adapters.driven.translator;

import java.util.Map;

import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.ports.driven.ForTranslatorIface;

public class DummyTranslator implements ForTranslatorIface {

    @Override
    public Document translate(Document document, String sourceLanguage, String targetLanguage, Map<String, Object> options) {
        // dummy translator, just return the same document content
        document.content = document.content;
        return document;
    }
    
}
