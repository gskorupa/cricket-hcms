package pl.experiot.hcms.adapters.driven.translator;

import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.ports.driven.ForMultilanguageRepoModelIface;

public class PathBasedRepoModel implements ForMultilanguageRepoModelIface {

    private String[] languages;
    private String mainLanguage;

    @Override
    public void setRepoLanguages(String[] languages) {
        this.languages = languages;
    }

    @Override
    public void setMainLanguage(String language) {
        this.mainLanguage = language;
    }

    @Override
    public String getLanguage() {
        return this.mainLanguage;
    }

    @Override
    public String[] getLanguages() {
        return this.languages;
    }

    @Override
    public String getMainLanguage() {
        return this.mainLanguage;
    }

    /**
     * Get document language
     *
     * @param document
     * @return
     */
    @Override
    public String getDocumentLanguage(Document document) {
        // document name is started with /siteName/language code, e.g. "/demo/en/" for
        // English
        for (String lang : languages) {
            System.out.println(
                "Checking: " +
                    document.name +
                    " " +
                    document.siteName +
                    " " +
                    lang
            );
            if (
                document.name.startsWith(
                    "/" + document.siteName + "/" + lang + "/"
                )
            ) {
                System.out.println("Found document language: " + lang);
                return lang;
            }
        }
        return mainLanguage; // default language
    }

    /**
     * Set document language
     *
     * @param document
     * @param targetLanguage
     * @return
     */
    @Override
    public Document setDocumentLanguage(
        Document document,
        String targetLanguage
    ) {
        // document name is started with language code, e.g. "/en/" for English
        String docName = document.name;
        document.name =
            "/" +
            document.siteName +
            "/" +
            targetLanguage +
            "/" +
            docName.substring(
                docName.indexOf(
                    "/",
                    document.siteName.length() + mainLanguage.length() + 1
                ) + 1
            );
        String docPath = document.path;
        document.path =
            "/" +
            document.siteName +
            "/" +
            targetLanguage +
            "/" +
            docPath.substring(
                docPath.indexOf(
                    "/",
                    document.siteName.length() + mainLanguage.length() + 1
                ) + 1
            );
        document.content = translateRepoLinks(document.content, targetLanguage);
        return document;
    }

    /**
     * Construct document name for a specific language by replacing main language code
     * in the path with the target language code.
     * Example: /site/pl/page with target language "en" becomes /site/en/page
     */
    @Override
    public String getLanguageDocumentName(
        String documentName,
        String targetLanguage
    ) {
        if (
            documentName == null ||
            targetLanguage == null ||
            mainLanguage == null
        ) {
            return documentName;
        }

        // Replace mainLanguage with targetLanguage in the path
        // Handle both "/pl/page" -> "/en/page" and "/site/pl/page" -> "/site/en/page"
        String normalizedName = documentName.startsWith("/")
            ? documentName
            : "/" + documentName;

        // Replace "/pl/" with "/en/" or "/pl" at end with "/en"
        String result = normalizedName.replace(
            "/" + mainLanguage + "/",
            "/" + targetLanguage + "/"
        );

        // Handle case where mainLanguage is at the end without trailing slash
        if (
            !result.equals(normalizedName) ||
            normalizedName.endsWith("/" + mainLanguage)
        ) {
            result = normalizedName.replace(
                "/" + mainLanguage,
                "/" + targetLanguage
            );
        }

        return result;
    }

    @Override
    public String translateRepoLinks(String content, String targetLanguage) {
        // Content is a string (HTML) with links to other documents in the repository
        // The links are in the form of "/language/documentName"
        // The method should translate the links to the target language
        // e.g. "/en/documentName" to "/de/documentName" if targetLanguage is "de"
        // The method should return the translated content
        content = content.replaceAll(
            "href=\"/" + getMainLanguage() + "/",
            "href=\"/" + targetLanguage + "/"
        );
        return content;
    }
}
