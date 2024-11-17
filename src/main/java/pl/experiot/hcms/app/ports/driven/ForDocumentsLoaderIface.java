package pl.experiot.hcms.app.ports.driven;

import java.util.HashMap;

import io.vertx.mutiny.core.eventbus.EventBus;
import pl.experiot.hcms.app.logic.dto.Site;

public interface ForDocumentsLoaderIface {

    void setRoot(String root);

    void setExcludes(String excludes);

    void setRepositoryPort(ForDocumentRepositoryIface repositoryPort);
    void setEventBus(EventBus eventBus, String queueName);

    void loadDocuments(String siteRoot, HashMap<String, Site> siteMap,  boolean start, boolean stop, long timestamp);
    void loadDocuments(Site site, long timestamp);
    void setSyntax(String syntax);

    void setMarkdownFileExtension(String markdownFileExtension);

    void setHtmlFileExtension(String htmlFileExtension);

    @Deprecated
    void setHcmsServiceUrl(String hcmsServiceUrl);
    
    void setHcmsFileApi(String hcmsFileApiUrl);

    void setSites(String sites);

    void setAssets(String assets);

}