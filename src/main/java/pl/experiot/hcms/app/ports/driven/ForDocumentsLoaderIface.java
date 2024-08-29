package pl.experiot.hcms.app.ports.driven;

import java.util.HashMap;

import pl.experiot.hcms.app.logic.Site;

public interface ForDocumentsLoaderIface {

    void setRoot(String root);

    void setExcludes(String excludes);

    void setRepositoryPort(ForDocumentRepositoryIface repositoryPort);

    void loadDocuments(String siteRoot, HashMap<String, Site> siteMap,  boolean start, boolean stop, long timestamp);
    void loadDocuments(Site site, long timestamp);
    void setSyntax(String syntax);

    void setMarkdownFileExtension(String markdownFileExtension);

    void setHtmlFileExtension(String htmlFileExtension);

    void setHcmsServiceUrl(String hcmsServiceUrl);

    void setSites(String sites);

    void setAssets(String assets);

}