package pl.experiot.hcms.app.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
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
    String deeplApiKey;
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
        if (options == null) {
            Path filePath = Path.of(deeplApiKeyFile);
            try {
                deeplApiKey = Files.readString(filePath).trim();
            } catch (IOException e) {
                logger.warn("Error reading Deepl API key from file: " + filePath);
                e.printStackTrace();
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
    public void translate(String documentName) {
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
        Document document = repositoryPort.getDocument(documentName);
        if (document != null) {
            if (localizationModelPort.getDocumentLanguage(document).equals(mainLanguage)) {
                for (String language : languages) {
                    if (language.equals(mainLanguage)) {
                        continue;
                    }
                    if (document.binaryFile) {
                        continue;
                    }
                    logger.info("Translating: " + document.name + " to " + language);
                    Document translatedDocument = translatorPort.translate(document, mainLanguage, language,
                            getOptions());
                    if (null != translatedDocument) {
                        translatedDocument = localizationModelPort.setDocumentLanguage(translatedDocument, language);
                        repositoryPort.addDocument(translatedDocument);
                    }
                }
            } else {
                logger.info("Skipping: " + document.name);
            }
        }
    }

}
