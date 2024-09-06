package pl.experiot.hcms.adapters.driven.translator;

import pl.experiot.hcms.app.logic.Document;
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

    public Document setDocumentLanguage(Document document, String language) {
        // document name is started with language code, e.g. "/en/" for English
        String docName = document.name;
        document.name = "/" + document.siteName + "/" + language + "/" + docName.substring(docName.indexOf("/", document.siteName.length()+mainLanguage.length()+1) + 1);
        return document;
    }
}
