package org.cricketmsf.hcms.application.out;

import java.util.Arrays;

import org.cricketmsf.hcms.domain.Document;
import org.jboss.logging.Logger;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class DocumentTransformer {
    private static Logger logger = Logger.getLogger(DocumentTransformer.class);

    // https://github.com/vsch/flexmark-java

    public static Document transform(
            Document doc,
            String markdownExtension,
            String assetsFolderName,
            String hcmsServiceUrl) {
        try {
            if (doc.name.endsWith(markdownExtension)) {
                doc.content = getHtml(doc.content);
            }
            if (assetsFolderName != null && !assetsFolderName.isEmpty()) {
                doc.content = transformImageLinks4(doc.content,"<img src=\"", "XYZ", assetsFolderName, hcmsServiceUrl);
            }
            logger.debug("doc to save name: " + doc.name);
            logger.debug("doc to save path: " + doc.path);
            return doc;
        } catch (Exception e) {
            logger.error("transformer error: " + e.getMessage());
            return null;
        }
    }

    private static String getHtml(String markdown) {
        MutableDataSet options = new MutableDataSet();

        // optional extensions
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

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
    private static String transformImageLinks4(String content, String tag, String param1, String assetsFolderName,
            String hcmsServiceUrl) {
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
                result+=tag+replaceFragment(fragment,param1, assetsFolderName,hcmsServiceUrl);
            } while (content.length() > 0);
        } catch (Exception e) {
            logger.error("transformImageLinks4 error: " + e.getMessage());
            e.printStackTrace();
            return content;
        }
        return result;
    }

    private static String replaceFragment(String fragment, String param1, String assetsFolderName,
    String hcmsServiceUrl) {
        logger.info("REPLACE FRAGMENT: "+fragment + " => " + param1);
        String[] parts=fragment.split("/");
        String fragmentToReplace="";
        String result;
        for(int i=0; i<parts.length; i++){
            if(parts[i].equals(".") || parts[i].equals("..")){
                continue;
            }
            fragmentToReplace+=parts[i];
            if(i<parts.length-1){
                fragmentToReplace+="/";
            }
        }
        logger.info("REPLACE FRAGMENT: "+fragmentToReplace);
        if(fragmentToReplace.startsWith(assetsFolderName) && hcmsServiceUrl!=null && !hcmsServiceUrl.isEmpty()){
            result=hcmsServiceUrl+"/api/file?path="+fragmentToReplace;
        }else{
            result=fragmentToReplace;
        }
        logger.info("REPLACE FRAGMENT: "+result);
        return result;
    }

}
