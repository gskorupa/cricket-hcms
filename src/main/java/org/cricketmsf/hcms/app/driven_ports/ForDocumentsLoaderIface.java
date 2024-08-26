package org.cricketmsf.hcms.app.driven_ports;

public interface ForDocumentsLoaderIface {

    void setRoot(String root);

    void setExcludes(String excludes);

    void setRepositoryPort(ForDocumentRepositoryIface repositoryPort);

    void loadDocuments(String siteRoot, boolean start, boolean stop, long timestamp);

    void setSyntax(String syntax);

    void setMarkdownFileExtension(String markdownFileExtension);

    void setHtmlFileExtension(String htmlFileExtension);

    void setHcmsServiceUrl(String hcmsServiceUrl);

    void setSites(String sites);

    void setAssets(String assets);

}