package pl.experiot.hcms.adapters.driven.translator;

import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.ports.driven.ForMultilanguageRepoModelIface;

public class PathBasedRepoModel implements ForMultilanguageRepoModelIface {

    private String[] languages;
    private String mainLanguage;

    public void setRepoLanguages(String[] languages) {
        this.languages = languages;
    }

    public void setMainLanguage(String language) {
        this.mainLanguage = language;
    }

    public String getLanguage() {
        return this.mainLanguage;
    }

    public String[] getLanguages() {
        return this.languages;
    }

    public String getMainLanguage() {
        return this.mainLanguage;
    }

    public String getDocumentLanguage(Document document) {
        // document name is started with /siteName/language code, e.g. "/demo/en/" for
        // English
        for (String lang : languages) {
            if (document.name.startsWith("/" + document.siteName + "/" + lang + "/")) {
                return lang;
            }
        }
        return "no_language"; // won't be translated
    }

    public Document setDocumentLanguage(Document document, String targetLanguage) {
        // document name is started with language code, e.g. "/en/" for English
        String docName = document.name;
        document.name = "/" + document.siteName + "/" + targetLanguage + "/" + docName.substring(docName.indexOf("/", document.siteName.length()+mainLanguage.length()+1) + 1);
        String docPath = document.path;
        document.path = "/" + document.siteName + "/" + targetLanguage + "/" + docPath.substring(docPath.indexOf("/", document.siteName.length()+mainLanguage.length()+1) + 1);
        document.content = translateRepoLinks(document.content, targetLanguage);
        return document;
    }

    @Override
    public String translateRepoLinks(String content, String targetLanguage) {
        // Content is a string (HTML) with links to other documents in the repository
        // The links are in the form of "/language/documentName"
        // The method should translate the links to the target language
        // e.g. "/en/documentName" to "/de/documentName" if targetLanguage is "de"
        // The method should return the translated content
        content = content.replaceAll("href=\"/" + getMainLanguage() + "/", "href=\"/" + targetLanguage + "/");
        return content;
    }
}
