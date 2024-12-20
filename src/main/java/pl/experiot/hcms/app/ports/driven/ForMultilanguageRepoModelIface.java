package pl.experiot.hcms.app.ports.driven;
import pl.experiot.hcms.app.logic.dto.Document;

public interface ForMultilanguageRepoModelIface {
    public void setRepoLanguages(String[] languages);
    public void setMainLanguage(String language);
    public String getLanguage();
    public String[] getLanguages();
    public String getMainLanguage();
    public String getDocumentLanguage(Document document);
    public Document setDocumentLanguage(Document document, String targetLanguage);
    public String translateRepoLinks(String content, String targetLanguage);
}
