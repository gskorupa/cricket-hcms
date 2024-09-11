package pl.experiot.hcms.adapters.driven.translator;

import java.util.HashSet;
import java.util.Map;

import org.jboss.logging.Logger;

import com.deepl.api.SentenceSplittingMode;
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
        String[] metadataToTranslate = ((String)options.getOrDefault("deepl.doc.metadata", "")).split(",");
        Document translatedDocument = document.clone(true);
        try {
            logger.info("Translating (deepl) " + document.name + " from " + sourceLanguage + " to " + targetLanguage);
            Translator translator = new Translator(authKey);

            // Set content translation options
            TextTranslationOptions contentTranslationOptions = new TextTranslationOptions();
            contentTranslationOptions.setTagHandling("html");
            contentTranslationOptions.setPreserveFormatting(true);
            HashSet<String> ignoreTags = new HashSet<>();
            ignoreTags.add("code");
            contentTranslationOptions.setIgnoreTags(ignoreTags);

            // Set metadata translation options
            TextTranslationOptions metadataTranslationOptions = new TextTranslationOptions();
            metadataTranslationOptions.setSentenceSplittingMode(SentenceSplittingMode.NoNewlines);

            // Translate document content
            TextResult result = translator.translateText(document.content, sourceLanguage, getCode(targetLanguage), contentTranslationOptions);
            logger.debug("Translated (deepl): "+result.getText());
            translatedDocument.content = result.getText();

            // Translate metadata
            String metadataValue;
            for (String key : metadataToTranslate) {
                if (document.metadata.containsKey(key)) {
                    metadataValue = document.metadata.get(key);
                    if(metadataValue == null) {
                        continue;
                    }
                    result = translator.translateText(metadataValue, sourceLanguage, getCode(targetLanguage), metadataTranslationOptions);
                    translatedDocument.metadata.put(key, result.getText());
                }
            }

            // Mark document as translated
            translatedDocument.metadata.put("language", targetLanguage);
            translatedDocument.metadata.put("translator", "DeepL");
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
