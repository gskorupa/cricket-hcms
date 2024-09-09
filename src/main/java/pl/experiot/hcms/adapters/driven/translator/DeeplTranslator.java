package pl.experiot.hcms.adapters.driven.translator;

import java.util.HashSet;
import java.util.Map;

import org.jboss.logging.Logger;

import com.deepl.api.TextResult;
import com.deepl.api.TextTranslationOptions;
import com.deepl.api.Translator;

import pl.experiot.hcms.app.logic.Document;
import pl.experiot.hcms.app.ports.driven.ForTranslatorIface;

public class DeeplTranslator implements ForTranslatorIface {

    private static Logger logger = Logger.getLogger(DeeplTranslator.class);

    @Override
    public Document translate(Document document, String sourceLanguage, String targetLanguage, Map<String, Object> options) {
        String authKey = (String) options.getOrDefault("deepl.api.key", "");
        Document translatedDocument = document.clone(true);
        try {
            logger.info("Translating (deepl) " + document.name + " from " + sourceLanguage + " to " + targetLanguage);
            TextTranslationOptions textTranslationOptions = new TextTranslationOptions();
            textTranslationOptions.setTagHandling("html");
            textTranslationOptions.setPreserveFormatting(true);
            HashSet<String> ignoreTags = new HashSet<>();
            textTranslationOptions.setIgnoreTags(ignoreTags);
            Translator translator = new Translator(authKey);
            TextResult result = translator.translateText(document.content, sourceLanguage, getCode(targetLanguage), textTranslationOptions);
            logger.info("Translated (deepl): "+result.getText());
            translatedDocument.content = result.getText();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error translating document " + document.name + ": " + e.getMessage());
            return null;
        }
        return translatedDocument;
    }

    private String getCode(String language) {
        switch (language) {
            case "en":
                return "en-US";
            default:
                return language;
        }
    }

}
