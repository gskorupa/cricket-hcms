package pl.experiot.hcms.adapters.driven.loader.test;

import java.util.HashMap;

import pl.experiot.hcms.app.logic.Site;
import pl.experiot.hcms.app.ports.driven.ForDocumentRepositoryIface;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;

public class TestDocLoader implements ForDocumentsLoaderIface {

    @Override
    public void setRoot(String root) {

    }

    @Override
    public void setExcludes(String excludes) {

    }

    @Override
    public void setRepositoryPort(ForDocumentRepositoryIface repositoryPort) {

    }

    @Override
    public void loadDocuments(String siteRoot, HashMap<String, Site> siteMap, boolean start, boolean stop,
            long timestamp) {

    }

    @Override
    public void setSyntax(String syntax) {

    }

    @Override
    public void setMarkdownFileExtension(String markdownFileExtension) {

    }

    @Override
    public void setHtmlFileExtension(String htmlFileExtension) {

    }

    @Override
    public void setHcmsServiceUrl(String hcmsServiceUrl) {

    }

    @Override
    public void setHcmsFileApi(String hcmsFileApi) {

    }

    @Override
    public void setSites(String sites) {

    }

    @Override
    public void setAssets(String assets) {

    }

    @Override
    public void loadDocuments(Site site, long timestamp) {
    }

    @Override
    public void setEventBus(io.vertx.mutiny.core.eventbus.EventBus eventBus, String queueName) {
    }
    
}
