package pl.experiot.hcms.adapters.driven.translator;

import pl.experiot.hcms.app.logic.Document;
import pl.experiot.hcms.app.ports.driven.ForTranslatorIface;

public class DummyTranslator implements ForTranslatorIface {

    @Override
    public Document translate(Document document, String sourceLanguage, String targetLanguage) {
        // dummy translator, just return the same document content
        document.content = document.content;
        return document;
    }
    
}
