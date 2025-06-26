package pl.experiot.hcms.adapters.driven.loader.fs;

import java.util.Arrays;
import java.util.Locale;

import org.jboss.logging.Logger;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import pl.experiot.hcms.app.logic.dto.Document;

public class DocumentTransformer {

    private static Logger logger = Logger.getLogger(DocumentTransformer.class);

    // https://github.com/vsch/flexmark-java

    public static Document transform(
            Document doc,
            String markdownExtension,
            String siteRootFolder,
            String assetsFolderName,
            String hcmsServiceUrl,
            String hcmsFileApi,
            String[] languages) {
        try {
            if (doc.name.endsWith(markdownExtension)) {
                doc.content = getHtml(doc.content);
            }
            if (assetsFolderName != null &&
                    !assetsFolderName.isEmpty() &&
                    hcmsServiceUrl != null &&
                    !hcmsServiceUrl.isEmpty() &&
                    !hcmsServiceUrl.equalsIgnoreCase("none")) {
                doc.content = transformImageLinks(
                        doc.content,
                        siteRootFolder,
                        assetsFolderName,
                        hcmsServiceUrl,
                        hcmsFileApi);
            } else {
                logger.debug(
                        "transform assetsFolderName or hcmsServiceUrl is empty, skipping image links transformation");
            }
            logger.debug("doc to save name: " + doc.name);
            logger.debug("doc to save path: " + doc.path);
            doc.content = transformDocumentLinks(
                    doc.content,
                    doc.path,
                    languages);
            return doc;
        } catch (Exception e) {
            logger.error("transformer error: " + e.getMessage());
            return null;
        }
    }

    private static String getHtml(String markdown) {
        MutableDataSet options = new MutableDataSet();

        // optional extensions
        options.set(
                Parser.EXTENSIONS,
                Arrays.asList(
                        TablesExtension.create(),
                        StrikethroughExtension.create()));

        // convert soft-breaks to hard breaks
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // You can re-use parser and renderer instances
        Node document = parser.parse(markdown);
        String html = renderer.render(document);
        return html;
    }

    /**
     * Finds all fragments in the input string starting with '<img src="' and ending
     * with '>'
     * then replaces them with the parameter param1.
     */
    private static String transformImageLinks(
            String content,
            String siteRootFolder,
            String assetsFolderName,
            String hcmsServiceUrl,
            String hcmsFileApi) {
        String tag = "<img src=\"";
        String fragment;
        String result = "";
        try {
            do {
                int start = content.indexOf(tag);
                if (start == -1) {
                    result += content;
                    break;
                }
                result += content.substring(0, start);
                content = content.substring(start + 10);
                int end = content.indexOf("\"");
                fragment = content.substring(0, end);
                content = content.substring(end);
                result += tag +
                        replaceFragment(
                                fragment,
                                siteRootFolder,
                                assetsFolderName,
                                hcmsServiceUrl,
                                hcmsFileApi);
            } while (content.length() > 0);
        } catch (Exception e) {
            logger.error("transformImageLinks4 error: " + e.getMessage());
            e.printStackTrace();
            return content;
        }
        return result;
    }

    /**
     * Replaces the fragment with the appropriate API call or path.
     */
    private static String replaceFragment(
            String fragment,
            String siteRootFolder,
            String assetsFolderName,
            String hcmsServiceUrl,
            String hcmsFileApi) {
        String[] parts = fragment.split("/");
        String fragmentToReplace = "";
        String siteRootFolderName = "";
        String folderSeparator = "/";
        if (assetsFolderName.startsWith("/")) {
            folderSeparator = "";
        }
        if (siteRootFolder != null &&
                !siteRootFolder.isEmpty() &&
                !siteRootFolder.equalsIgnoreCase("none")) {
            siteRootFolderName = siteRootFolder;
        }
        String result;
        // remove "." and ".." from the path
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals(".") || parts[i].equals("..")) {
                continue;
            }
            fragmentToReplace += parts[i];
            if (i < parts.length - 1) {
                fragmentToReplace += "/";
            }
        }
        logger.debug("REPLACE FRAGMENT: " + fragmentToReplace);
        logger.debug("hcmsServiceUrl: " + hcmsServiceUrl);
        logger.debug("siteRootFolderName: " + siteRootFolderName);
        logger.debug("assetsFolderName: " + assetsFolderName);
        // if the fragment starts with the assetsFolderName
        // and the hcmsServiceUrl is set
        // then replace the fragment with equivalent API call
        String fileApiPath = "/api/file";
        if (hcmsFileApi != null &&
                !hcmsFileApi.isEmpty() &&
                !hcmsFileApi.equalsIgnoreCase("none")) {
            fileApiPath = hcmsFileApi;
        }
        if (fragmentToReplace.startsWith(assetsFolderName) &&
                hcmsServiceUrl != null &&
                !hcmsServiceUrl.isEmpty()) {
            result = hcmsServiceUrl +
                    fileApiPath +
                    "?name=" +
                    siteRootFolderName +
                    folderSeparator +
                    fragmentToReplace;
        } else {
            result = fragmentToReplace;
        }
        logger.debug("REPLACE FRAGMENT: " + result);
        return result;
    }

    /**
     * Finds all links to documents related to parsed document. If link starts with
     * "/"
     * and not starts with "/"+language+"/"
     * then prepends with "/"+document language
     * 
     * @return transformed content
     */
    private static String transformDocumentLinks(
            String content,
            String documentPath,
            String[] languages) {
        String language = getDocumentLanguage(documentPath, languages);
        String result = "";
        String fragment;
        String tag = "<a href=\"";
        try {
            do {
                int start = content.indexOf(tag);
                if (start == -1) {
                    result += content;
                    break;
                }
                result += content.substring(0, start);
                content = content.substring(start + tag.length());
                int end = content.indexOf("\"");
                fragment = content.substring(0, end);
                content = content.substring(end);
                result += tag + replaceLinkFragment(fragment, language);
            } while (content.length() > 0);
        } catch (Exception e) {
            logger.error("transformImageLinks4 error: " + e.getMessage());
            e.printStackTrace();
            return content;
        }
        return result;
    }

    private static String getDocumentLanguage(String documentPath, String[] languages) {
        // Assuming the language code is the first part of the path after the root
        logger.debug("getDocumentLanguage: " + documentPath);
        String[] isoLanguages = Locale.getISOLanguages(); // TODO: use this to validate language codes
        String[] parts = documentPath.split("/");
        String languageCode = "en"; // Default language
        if (parts.length > 2) {
            languageCode = parts[2]; // Assuming the second part is the language code
        }
        if (languageCode.length() > 2) {
            logger.debug("Language code is too long, using default 'en': " + languageCode);
            return "en"; // Fallback to default if the code is not valid
        }
        return languageCode;
    }

    private static String replaceLinkFragment(
            String fragment,
            String languageCode) {
        try {
            logger.debug("replaceLinkFragment (" + languageCode + "): " + fragment.substring(0, 20));
        } catch (Exception e) {
        }
        if (fragment.startsWith("/" + languageCode + "/")) {
            return fragment;
        }

        // If the fragment does not start with a language code, prepend it
        if (fragment.startsWith("/")) {
            return "/" + languageCode + fragment;
        } else {
            return fragment;
        }

    }
}
