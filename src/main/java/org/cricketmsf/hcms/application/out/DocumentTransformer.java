package org.cricketmsf.hcms.application.out;

import java.util.Arrays;

import org.cricketmsf.hcms.domain.Document;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class DocumentTransformer {

    //https://github.com/vsch/flexmark-java

    public static Document transform(Document doc, String markdownExtension) {
        if (doc.name.endsWith(markdownExtension)) {
            doc.content = getHtml(doc.content);
        }
        return doc;
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
}
