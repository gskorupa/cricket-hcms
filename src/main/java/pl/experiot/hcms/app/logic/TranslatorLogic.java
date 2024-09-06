package pl.experiot.hcms.app.logic;

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
/* 
    @ConfigProperty(name = "hcms.database.type")
    String databaseType;
    @ConfigProperty(name = "hcms.localization.model")
    String localizationModel; */
    @ConfigProperty(name = "hcms.repository.language.main")
    String mainLanguage;
    @ConfigProperty(name = "hcms.repository.languages")
    String[] languages;
    //@ConfigProperty(name = "hcms.translator.type")
    //String translatorType;

    String queueName = "hcms";

    ForDocumentRepositoryIface repositoryPort;
    ForTranslatorIface translatorPort;
    ForMultilanguageRepoModelIface localizationModelPort;

    void onStart(@Observes StartupEvent ev) {
        logger.info("TranslatorLogic starting...");
/*         repositoryPort = Configurator.getRepositoryPort(databaseType);
        translatorPort = Configurator.getTranslatorPort(translatorType);
        localizationModelPort = Configurator.getRepoModelPort(localizationModel);
 */        
        repositoryPort = configurator.getRepositoryPort();
        repositoryPort.setEventBus(bus, queueName);
        //repositoryPort.init(dataSource);
        translatorPort = configurator.getTranslatorPort();
        localizationModelPort = configurator.getRepoModelPort();
        //localizationModelPort.setMainLanguage(mainLanguage);
        //localizationModelPort.setRepoLanguages(languages);
    }

    @ConsumeEvent("hcms")
    public void translate(String documentName) {
        if(repositoryPort == null) {
            repositoryPort = configurator.getRepositoryPort();
            repositoryPort.setEventBus(bus, queueName);
        }
        if(translatorPort == null) {
            translatorPort = configurator.getTranslatorPort();
        }
        if(localizationModelPort == null) {
            localizationModelPort = configurator.getRepoModelPort();
        }
        Document document = repositoryPort.getDocument(documentName);
        if (document != null) {
            if (localizationModelPort.getDocumentLanguage(document).equals(mainLanguage)) {
/*                 String tmps="";
                for(int i=0;i<languages.length;i++){
                    tmps+=languages[i];
                    if(i<languages.length-1){
                        tmps+=" ";
                    }
                } */
                for (String language : languages) {
                    if(language.equals(mainLanguage)) {
                        continue;
                    }
                    if(document.binaryFile){
                        continue;
                    }
                    logger.info("Translating: " + document.name + " to " + language);
                    Document translatedDocument = translatorPort.translate(document, mainLanguage, language);
                    translatedDocument = localizationModelPort.setDocumentLanguage(translatedDocument, language);
                    repositoryPort.addDocument(translatedDocument);
                }
            }else{
                logger.info("Skipping: " + document.name);
            }
        }
    }

}
