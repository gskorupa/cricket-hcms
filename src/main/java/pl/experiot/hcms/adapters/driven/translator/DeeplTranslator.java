package pl.experiot.hcms.adapters.driven.translator;

import pl.experiot.hcms.app.logic.Document;
import pl.experiot.hcms.app.ports.driven.ForTranslatorIface;

public class DeeplTranslator implements ForTranslatorIface {

    @Override
    public Document translate(Document document, String sourceLanguage, String targetLanguage) {
        // dummy translator, just return the same document content
        Document translatedDocument = document.clone(true);
        return translatedDocument;
    }
    
}
