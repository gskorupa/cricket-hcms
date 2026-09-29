package pl.experiot.hcms.app.logic;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
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

    ForDocumentRepositoryIface repositoryPort = null;
    ForTranslatorIface translatorPort = null;
    ForMultilanguageRepoModelIface localizationModelPort = null;

    HashMap<String, Object> options = null;

    void onStart(@Observes StartupEvent ev) {
        options = getOptions();
    }

    private void logDebug(Supplier<String> messageSupplier) {
        if (logger.isDebugEnabled()) {
            logger.debug(messageSupplier.get());
        }
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
            logDebug(
                () ->
                    "Localization model port initialized: " +
                    localizationModelPort.getClass().getName()
            );
        }
    }

    /**
     * Construct document name for a specific language by replacing main language code
     * in the path with the target language code.
     * Example: /site/pl/page with target language "en" becomes /site/en/page
     */
    //     private String getLanguageDocumentName(
    //         String documentName,
    //         String targetLanguage
    //     ) {
    //         if (
    //             documentName == null ||
    //             targetLanguage == null ||
    //             mainLanguage == null
    //         ) {
    //             return documentName;
    //         }
    //
    //         // Replace mainLanguage with targetLanguage in the path
    //         // Handle both "/pl/page" -> "/en/page" and "/site/pl/page" -> "/site/en/page"
    //         String normalizedName = documentName.startsWith("/")
    //             ? documentName
    //             : "/" + documentName;
    //
    //         // Replace "/pl/" with "/en/" or "/pl" at end with "/en"
    //         String result = normalizedName.replace(
    //             "/" + mainLanguage + "/",
    //             "/" + targetLanguage + "/"
    //         );
    //
    //         // Handle case where mainLanguage is at the end without trailing slash
    //         if (
    //             !result.equals(normalizedName) ||
    //             normalizedName.endsWith("/" + mainLanguage)
    //         ) {
    //             result = normalizedName.replace(
    //                 "/" + mainLanguage,
    //                 "/" + targetLanguage
    //             );
    //         }
    //
    //         return result;
    //     }

    @ConsumeEvent("to-translate")
    public Uni<Void> translate(String documentData) {
        init();
        activeTranslations.incrementAndGet();

        return Uni.createFrom()
            .item(() -> {
                try {
                    String[] params = documentData.split(";");
                    if (params.length < 2) {
                        logger.error("Invalid document data: " + documentData);
                        LoadStatistics.getInstance().incrementTranslationApiErrors();
                        return null;
                    }
                    String documentName = params[0];
                    boolean updateLanguageVersions = params.length > 2;

                    Document document = repositoryPort.getDocument(
                        documentName
                    );
                    if (document == null) {
                        logger.error("Document not found: " + documentName);
                        LoadStatistics.getInstance().incrementTranslationApiErrors();
                        return null;
                    }
                    if (
                        document.binaryFile &&
                        !document.mediaType.equalsIgnoreCase("application/xml")
                    ) {
                        return null;
                    }

                    long updateTimestamp = Long.parseLong(params[1]);
                    long previousTimestamp = 0L;

                    if (updateLanguageVersions) {
                        // Update language versions: check each language timestamp individually
                        String tmpLanguage =
                            localizationModelPort.getDocumentLanguage(document);
                        logger.info(
                            "Translating (updating) from language: " +
                                tmpLanguage
                        );
                        if (mainLanguage.equals(tmpLanguage)) {
                            for (String language : languages) {
                                if (
                                    updateLanguageVersions &&
                                    language.equals(mainLanguage)
                                ) continue;
                                // if (
                                //     document.binaryFile &&
                                //     !document.mediaType.equalsIgnoreCase(
                                //         "application/xml"
                                //     )
                                // ) continue;

                                // Construct the language document name by replacing mainLanguage with target language
                                String languageDocumentName =
                                    localizationModelPort.getLanguageDocumentName(
                                        documentName,
                                        language
                                    );
                                long languageTimestamp =
                                    repositoryPort.getPreviousUpdateTimestamp(
                                        languageDocumentName,
                                        language
                                    );
                                logDebug(
                                    () ->
                                        "Checking language version: " +
                                        document.name +
                                        " for " +
                                        language +
                                        " (doc: " +
                                        languageDocumentName +
                                        ") with timestamps: " +
                                        updateTimestamp +
                                        " " +
                                        languageTimestamp
                                );

                                if (languageTimestamp < updateTimestamp) {
                                    startTranslationInNewThread(
                                        document,
                                        documentName,
                                        language,
                                        updateTimestamp
                                    );
                                } else {
                                    logDebug(
                                        () ->
                                            "Skipping language: " +
                                            document.name +
                                            " to " +
                                            language +
                                            " - up to date: " +
                                            languageTimestamp +
                                            ">=" +
                                            updateTimestamp
                                    );
                                }
                            }
                        } else {
                            logDebug(
                                () ->
                                    "Skipping translation (update): " +
                                    document.name
                            );
                        }
                    } else {
                        // Update all language versions based on main document timestamp

                        logger.debug(
                            "Translating: " +
                                documentName +
                                " with timestamps: " +
                                updateTimestamp +
                                " " +
                                previousTimestamp
                        );

                        //if (previousTimestamp < updateTimestamp) {
                        if (
                            localizationModelPort
                                .getDocumentLanguage(document)
                                .equals(mainLanguage)
                        ) {
                            for (String language : languages) {
                                if (language.equals(mainLanguage)) continue;
                                if (
                                    document.binaryFile &&
                                    !document.mediaType.equalsIgnoreCase(
                                        "application/xml"
                                    )
                                ) continue;
                                previousTimestamp =
                                    repositoryPort.getPreviousUpdateTimestamp(
                                        documentName,
                                        language
                                    );
                                if (previousTimestamp < updateTimestamp) {
                                    startTranslationInNewThread(
                                        document,
                                        documentName,
                                        language,
                                        updateTimestamp
                                    );
                                }
                            }
                        } else {
                            logDebug(
                                () ->
                                    "Skipping (main language): " + document.name
                            );
                        }
                        // } else {
                        //     logDebug(
                        //         () ->
                        //             "Skipping: " +
                        //             document.name +
                        //             " - up to date: " +
                        //             previousTimestamp +
                        //             ">=" +
                        //             updateTimestamp
                        //     );
                        // }
                    }
                } catch (Exception e) {
                    logger.error("Unexpected error in translation handler", e);
                    LoadStatistics.getInstance().incrementTranslationApiErrors();
                }
                return null;
            })
            .eventually(() -> {
                if (activeTranslations.decrementAndGet() == 0) {
                    scheduleStatsLogging();
                }
            })
            .replaceWithVoid();
    }

    private void startTranslationInNewThread(
        Document document,
        String documentName,
        String language,
        long updateTimestamp
    ) {
        logDebug(
            () ->
                "Starting translation in new thread: " +
                document.name +
                " to " +
                language
        );

        vertx
            .executeBlocking(
                (Callable<Void>) () -> {
                    // ✅ Callable<Void> jest poprawny dla Mutiny
                    try {
                        LoadStatistics.getInstance().incrementDocumentsSentToTranslation(
                            language
                        );

                        Document translatedDocument = translatorPort.translate(
                            document,
                            mainLanguage,
                            language,
                            getOptions()
                        );

                        if (translatedDocument != null) {
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
                        return null;
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
                        throw e; // ✅ Rzucamy wyjątek, Mutiny go złapie
                    }
                },
                false // ordered
            )
            .subscribe() // ✅ OBOWIĄZKOWE: subskrypcja uruchamia executesBlocking
            .with(
                result -> {}, // onSuccess
                error -> logger.error("Translation thread failed", error) // onFailure
            );
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
        sb.append("Translation Statistics:");
        Map<String, Integer> translationStats =
            stats.getDocumentsSentToTranslation();
        for (Map.Entry<String, Integer> entry : translationStats.entrySet()) {
            sb.append(" ")
                .append(entry.getKey())
                .append("=")
                .append(entry.getValue())
                .append(" ");
        }

        sb.append(", errors: ").append(stats.getTranslationApiErrors());
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
