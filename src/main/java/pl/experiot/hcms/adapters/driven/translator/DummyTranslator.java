package pl.experiot.hcms.adapters.driven.translator;

import jakarta.inject.Inject;
import java.util.Map;
import org.jboss.logging.Logger;
import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.ports.driven.ForTranslatorIface;

public class DummyTranslator implements ForTranslatorIface {

    @Inject
    Logger logger;

    @Override
    public Document translate(
        Document document,
        String sourceLanguage,
        String targetLanguage,
        Map<String, Object> options
    ) {
        // dummy translator, just return the same document content
        logger.info("Translating document (dummy translator)");
        document.content = document.content;
        return document;
    }
}
