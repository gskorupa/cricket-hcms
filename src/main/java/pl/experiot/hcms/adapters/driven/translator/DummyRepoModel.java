package pl.experiot.hcms.adapters.driven.translator;

import pl.experiot.hcms.app.logic.Document;
import pl.experiot.hcms.app.ports.driven.ForMultilanguageRepoModelIface;

public class DummyRepoModel implements ForMultilanguageRepoModelIface {
        
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
            return "no_language"; // won't be translated
        }
        
        public Document setDocumentLanguage(Document document, String language) {
            return document;
        }

        @Override
        public String translateRepoLinks(String content, String targetLanguage) {
            return content;
        }
}
