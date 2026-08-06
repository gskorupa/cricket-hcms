package pl.experiot.hcms.adapters.driven.translator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.ports.driven.ForTranslatorIface;

/**
 * Translator implementation using Google's Gemini AI service.
 * 
 * This translator uses the Gemini Generative AI API to translate text content.
 * Unlike specialized translation APIs, it uses a prompt-based approach with
 * the generative model to achieve translation.
 * 
 * Required options:
 * - gemini.api.key: Your Google Cloud API key for Gemini
 * - gemini.model: The model to use (default: gemini-1.5-flash)
 * 
 * Optional options:
 * - gemini.timeout: Request timeout in seconds (default: 30)
 * - gemini.temperature: Sampling temperature (default: 0.0 for deterministic output)
 * - gemini.metadata.fields: Comma-separated list of metadata fields to translate
 */
public class GeminiTranslator implements ForTranslatorIface {

    private static final Logger logger = Logger.getLogger(GeminiTranslator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final double DEFAULT_TEMPERATURE = 0.0;
    
    private final HttpClient httpClient;

    public GeminiTranslator() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public Document translate(Document document, String sourceLanguage, String targetLanguage,
            Map<String, Object> options) {
        
        String apiKey = (String) options.getOrDefault("gemini.api.key", "");
        String model = (String) options.getOrDefault("gemini.model", DEFAULT_MODEL);
        String[] metadataFields = ((String) options.getOrDefault("gemini.metadata.fields", "")).split(",");
        
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("Gemini API key is required. Set gemini.api.key in options.");
            return null;
        }

        try {
            logger.info("Translating (gemini) " + document.name + " from " + sourceLanguage + " to " + targetLanguage);
            
            Document translatedDocument = document.clone(true);
            
            // Build the translation prompt
            String prompt = buildTranslationPrompt(document.content, sourceLanguage, targetLanguage);
            
            // Translate content
            String translatedContent = translateText(apiKey, model, prompt, options);
            if (translatedContent != null) {
                translatedDocument.content = translatedContent;
            }
            
            // Translate metadata fields
            for (String fieldName : metadataFields) {
                if (fieldName != null && !fieldName.trim().isEmpty() && document.metadata.containsKey(fieldName)) {
                    String metadataValue = document.metadata.get(fieldName);
                    String metadataPrompt = buildTranslationPrompt(metadataValue, sourceLanguage, targetLanguage);
                    String translatedMetadata = translateText(apiKey, model, metadataPrompt, options);
                    if (translatedMetadata != null) {
                        translatedDocument.metadata.put(fieldName, translatedMetadata);
                    }
                }
            }
            
            // Mark document as translated
            translatedDocument.metadata.put("language", targetLanguage);
            translatedDocument.metadata.put("translator", "Gemini");
            
            logger.debug("Translated (gemini): " + translatedDocument.content.substring(0, 
                Math.min(100, translatedDocument.content.length())) + "...");
            
            return translatedDocument;
            
        } catch (Exception e) {
            logger.error("Error translating document " + document.name + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Builds a translation prompt for Gemini.
     */
    private String buildTranslationPrompt(String text, String sourceLanguage, String targetLanguage) {
        // Use a system instruction approach for better translation quality
        return "Translate the following text from " + sourceLanguage + " to " + targetLanguage + ". " +
               "Return only the translated text without any additional commentary, explanations, or formatting. " +
               "Preserve the original text structure, formatting, and any special characters.\n\n" +
               text;
    }

    /**
     * Translates text using the Gemini API.
     */
    private String translateText(String apiKey, String model, String prompt, Map<String, Object> options) {
        try {
            // Build the request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // Add contents array with the prompt
            requestBody.putArray("contents").addObject()
                .putArray("parts").addObject()
                .put("text", prompt);
            
            // Add generation config
            ObjectNode generationConfig = requestBody.putObject("generationConfig");
            generationConfig.put("temperature", 
                (Double) options.getOrDefault("gemini.temperature", DEFAULT_TEMPERATURE));
            generationConfig.put("maxOutputTokenCount", 8192);
            
            // Add safety settings
            requestBody.putArray("safetySettings").addObject()
                .put("category", "HARM_CATEGORY_HARASSMENT")
                .put("threshold", "BLOCK_LOW_AND_ABOVE");
            
            String requestBodyString = objectMapper.writeValueAsString(requestBody);
            
            // Build the URL
            String url = API_BASE_URL + "/" + model + ":generateContent";
            
            // Build the request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .timeout(Duration.ofSeconds(
                    Integer.parseInt(options.getOrDefault("gemini.timeout", DEFAULT_TIMEOUT_SECONDS).toString())))
                .build();
            
            // Send the request
            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());
            
            // Check response status
            if (response.statusCode() != 200) {
                logger.error("Gemini API error: " + response.statusCode() + " - " + response.body());
                return null;
            }
            
            // Parse the response
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode candidates = responseJson.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode contentNode = candidates.get(0).path("content");
                if (contentNode.isArray() && contentNode.size() > 0) {
                    JsonNode parts = contentNode.get(0).path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        return parts.get(0).path("text").asText();
                    }
                }
            }
            
            logger.warn("Unexpected response format from Gemini API");
            return null;
            
        } catch (Exception e) {
            logger.error("Error calling Gemini API: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts language code to a more descriptive name for the prompt.
     */
    private String getLanguageName(String languageCode) {
        switch (languageCode.toLowerCase()) {
            case "en": return "English";
            case "pl": return "Polish";
            case "de": return "German";
            case "fr": return "French";
            case "es": return "Spanish";
            case "it": return "Italian";
            case "ru": return "Russian";
            case "zh": return "Chinese";
            case "ja": return "Japanese";
            default: return languageCode;
        }
    }
}
