package pl.experiot.hcms.app.logic;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.ports.driven.ForDocumentRepositoryIface;
import pl.experiot.hcms.app.ports.driven.ForMultilanguageRepoModelIface;
import pl.experiot.hcms.app.ports.driven.ForTranslatorIface;

@ApplicationScoped
public class TranslatorLogic {

    @Inject
    Logger logger;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    Configurator2 configurator;

    @Inject
    EventBus bus;

    @ConfigProperty(name = "hcms.repository.language.main")
    String mainLanguage;
    @ConfigProperty(name = "hcms.repository.languages")
    String[] languages;
    @ConfigProperty(name = "deepl.api.key.file")
    String deeplApiKeyFile;
    @ConfigProperty(name = "deepl.doc.metadata")
    String metadataToTranslate;

    String queueName = "to-translate";

    ForDocumentRepositoryIface repositoryPort;
    ForTranslatorIface translatorPort;
    ForMultilanguageRepoModelIface localizationModelPort;

    HashMap<String, Object> options = null;

    void onStart(@Observes StartupEvent ev) {
        logger.info("TranslatorLogic starting...");

        repositoryPort = configurator.getRepositoryPort();
        repositoryPort.setEventBus(bus, queueName);

        translatorPort = configurator.getTranslatorPort();
        localizationModelPort = configurator.getRepoModelPort();
        options = getOptions();

    }

    private HashMap<String, Object> getOptions() {
        String deeplApiKey="";
        if (options == null) {
            if ("none".equalsIgnoreCase(deeplApiKeyFile)) {
                deeplApiKey = "";
            } else {
                Path filePath = Path.of(deeplApiKeyFile);
                try {
                    deeplApiKey = Files.readString(filePath).trim();
                } catch (IOException e) {
                    logger.warn("Error reading Deepl API key from file: " + filePath);
                    e.printStackTrace();
                }
            }
            options = new HashMap<>();
            options.put("deepl.api.key", deeplApiKey);
            if (metadataToTranslate != null && !metadataToTranslate.equalsIgnoreCase("none")) {
                options.put("deepl.doc.metadata", metadataToTranslate);
            }
        }
        return options;
    }

    @ConsumeEvent("to-translate")
    public void translate(String documentData) {
        if (repositoryPort == null) {
            repositoryPort = configurator.getRepositoryPort();
            repositoryPort.setEventBus(bus, queueName);
        }
        if (translatorPort == null) {
            translatorPort = configurator.getTranslatorPort();
        }
        if (localizationModelPort == null) {
            localizationModelPort = configurator.getRepoModelPort();
        }
        String[] params=documentData.split(";");
        if(params.length<2){
            logger.error("Invalid document data: "+documentData);
            return;
        }
        String documentName =   params[0];
    
        Document document = repositoryPort.getDocument(documentName);
        if(document==null){
            logger.error("Document not found: "+documentName);
            return;
        }
        long updateTimestamp = Long.parseLong(params[1]);
        long previousTimestamp = repositoryPort.getPreviousUpdateTimestamp(documentName);
        logger.info("Translating: " + documentName + " with timestamps: "+updateTimestamp+" "+previousTimestamp);

        if (previousTimestamp < updateTimestamp) {
            if (localizationModelPort.getDocumentLanguage(document).equals(mainLanguage)) {
                for (String language : languages) {
                    if (language.equals(mainLanguage)) {
                        continue;
                    }
                    if (document.binaryFile && !document.mediaType.equalsIgnoreCase("application/xml")) {
                        continue;
                    }
                    logger.info("Translating: " + document.name + " to " + language);
                    Document translatedDocument = translatorPort.translate(document, mainLanguage, language,
                            getOptions());
                    if (null != translatedDocument) {
                        translatedDocument = localizationModelPort.setDocumentLanguage(translatedDocument, language);
                        repositoryPort.addDocument(translatedDocument, documentName);
                    }
                }
            } else {
                logger.debug("Skipping (main language): " + document.name);
            }
        }else{
            logger.info("Skipping: "+document.name+" - up to date: "+previousTimestamp+">="+updateTimestamp);
        }
    }

}
