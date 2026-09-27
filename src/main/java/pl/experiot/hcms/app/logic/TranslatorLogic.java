package pl.experiot.hcms.app.logic;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import pl.experiot.hcms.adapters.driven.loader.fs.LoadStatistics;
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

    @Inject
    Vertx vertx;

    private final AtomicInteger activeTranslations = new AtomicInteger(0);
    private long statsLoggingTimerId = -1L;

    @ConfigProperty(name = "hcms.repository.language.main")
    String mainLanguage;

    @ConfigProperty(name = "hcms.repository.languages")
    String[] languages;

    @ConfigProperty(name = "deepl.api.key.file")
    String deeplApiKeyFile;

    @ConfigProperty(name = "doc.metadata") // change to doc.metadata
    String metadataToTranslate;

    @ConfigProperty(name = "google.api.key.file")
    String geminiApiKeyFile = "";

    @ConfigProperty(name = "gemini.model")
    String geminiModel = "";

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
        //TODO: use docker secrets to get the API keys
        String deeplApiKey = "";
        String geminiApiKey = "";
        if (options == null) {
            if ("none".equalsIgnoreCase(deeplApiKeyFile)) {
                deeplApiKey = "";
            } else {
                Path filePath = Path.of(deeplApiKeyFile);
                try {
                    deeplApiKey = Files.readString(filePath).trim();
                } catch (IOException e) {
                    logger.warn(
                        "Error reading Deepl API key from file: " + filePath
                    );
                }
            }
            if (
                geminiApiKeyFile == null ||
                geminiApiKeyFile.isEmpty() ||
                geminiApiKeyFile.equalsIgnoreCase("none")
            ) {
                geminiApiKey = "";
            } else {
                Path filePath = Path.of(geminiApiKeyFile);
                try {
                    geminiApiKey = Files.readString(filePath).trim();
                } catch (IOException e) {
                    logger.warn(
                        "Error reading Gemini API key from file: " + filePath
                    );
                }
            }
            options = new HashMap<>();
            options.put("deepl.api.key", deeplApiKey);
            options.put("gemini.api.key", geminiApiKey);
            options.put("gemini.model", geminiModel);
            if (
                metadataToTranslate != null &&
                !metadataToTranslate.equalsIgnoreCase("none")
            ) {
                options.put("doc.metadata", metadataToTranslate);
            }
        }
        return options;
    }

    private void init() {
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
    }

    @ConsumeEvent("to-translate")
    public void translate(String documentData) {
        init();
        activeTranslations.incrementAndGet();
        String[] params = documentData.split(";");
        if (params.length < 2) {
            logger.error("Invalid document data: " + documentData);
            LoadStatistics.getInstance().incrementTranslationApiErrors();
            if (activeTranslations.decrementAndGet() == 0) {
                scheduleStatsLogging();
            }
            return;
        }
        String documentName = params[0];
        if (params.length > 2) {
            //przetłumaczyć wersje językowe które nie są przetłumaczone lub mają starszą wersję niż podany timestamp
        } else {
            //jak obecnie
        }

        Document document = repositoryPort.getDocument(documentName);
        if (document == null) {
            logger.error("Document not found: " + documentName);
            LoadStatistics.getInstance().incrementTranslationApiErrors();
            if (activeTranslations.decrementAndGet() == 0) {
                scheduleStatsLogging();
            }
            return;
        }
        long updateTimestamp = Long.parseLong(params[1]);
        long previousTimestamp = repositoryPort.getPreviousUpdateTimestamp(
            documentName
        );
        logger.info(
            "Translating: " +
                documentName +
                " with timestamps: " +
                updateTimestamp +
                " " +
                previousTimestamp
        );

        if (previousTimestamp < updateTimestamp) {
            if (
                localizationModelPort
                    .getDocumentLanguage(document)
                    .equals(mainLanguage)
            ) {
                for (String language : languages) {
                    if (language.equals(mainLanguage)) {
                        continue;
                    }
                    if (
                        document.binaryFile &&
                        !document.mediaType.equalsIgnoreCase("application/xml")
                    ) {
                        continue;
                    }
                    logger.info(
                        "Translating: " + document.name + " to " + language
                    );
                    Document translatedDocument = null;
                    try {
                        LoadStatistics.getInstance().incrementDocumentsSentToTranslation(
                            language
                        );
                        translatedDocument = translatorPort.translate(
                            document,
                            mainLanguage,
                            language,
                            getOptions()
                        );
                        if (null != translatedDocument) {
                            translatedDocument =
                                localizationModelPort.setDocumentLanguage(
                                    translatedDocument,
                                    language
                                );
                            repositoryPort.addDocument(
                                translatedDocument,
                                documentName
                            );
                        }
                    } catch (Exception e) {
                        logger.error(
                            "Translation API error for document " +
                                document.name +
                                " to " +
                                language +
                                ": " +
                                e.getMessage()
                        );
                        LoadStatistics.getInstance().incrementTranslationApiErrors();
                    }
                }
            } else {
                logger.debug("Skipping (main language): " + document.name);
            }
        } else {
            logger.info(
                "Skipping: " +
                    document.name +
                    " - up to date: " +
                    previousTimestamp +
                    ">=" +
                    updateTimestamp
            );
        }
        if (activeTranslations.decrementAndGet() == 0) {
            scheduleStatsLogging();
        }
    }

    /**
     * Planuje odroczone zalogowanie statystyk po 2 sekundach braku nowych tłumaczeń.
     * Jeśli w międzyczasie nadejdzie nowy dokument, timer zostanie anulowany.
     */
    private void scheduleStatsLogging() {
        if (statsLoggingTimerId != -1L) {
            vertx.cancelTimer(statsLoggingTimerId);
        }
        statsLoggingTimerId = vertx.setTimer(2000, timerId -> {
            if (activeTranslations.get() == 0) {
                logTranslationStatistics();
            }
        });
    }

    /**
     * Logs translation statistics from LoadStatistics singleton.
     */
    private void logTranslationStatistics() {
        LoadStatistics stats = LoadStatistics.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("=== Translation Statistics ===\n");
        sb.append("Documents sent to translation by language:\n");
        Map<String, Integer> translationStats =
            stats.getDocumentsSentToTranslation();
        if (translationStats.isEmpty()) {
            sb.append("  No documents translated yet\n");
        } else {
            for (Map.Entry<
                String,
                Integer
            > entry : translationStats.entrySet()) {
                sb.append("  ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
            }
        }
        sb.append("Translation API errors: ")
            .append(stats.getTranslationApiErrors())
            .append("\n");
        sb.append("=== End of Translation Statistics ===");
        logger.info(sb.toString());
    }

    /**
     * Returns the number of documents sent to translation for a specific language.
     */
    public int getDocumentsTranslatedCount(String language) {
        return LoadStatistics.getInstance()
            .getDocumentsSentToTranslation()
            .getOrDefault(language, 0);
    }

    /**
     * Returns the total translation API errors count.
     */
    public int getTranslationApiErrors() {
        return LoadStatistics.getInstance().getTranslationApiErrors();
    }

    /**
     * Returns all translation statistics.
     */
    public Map<String, Integer> getDocumentsTranslatedByLanguage() {
        return new HashMap<>(
            LoadStatistics.getInstance().getDocumentsSentToTranslation()
        );
    }

    @PreDestroy
    public void onShutdown() {
        if (activeTranslations.get() == 0) {
            logTranslationStatistics();
        }
    }
}
