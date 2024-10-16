package pl.experiot.hcms.app.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import pl.experiot.hcms.app.ports.driving.ForDictionaryIface;

@ApplicationScoped
public class DictionaryLogic implements ForDictionaryIface {

    @Inject
    Logger logger;

    public Dictionary getDictionary(String xmlData) {

        Dictionary dictionary = null;
        /*
         * try {
         * logger.info("Unmarshalling dictionary...");
         * logger.info("xmlData: " + xmlData);
         * XStream xStream = new XStream();
         * xStream.alias("dictionary", Dictionary.class);
         * xStream.alias("entry", Entry.class);
         * xStream.addPermission(AnyTypePermission.ANY);
         * dictionary = (Dictionary) xStream.fromXML(xmlData);
         * } catch (Exception e) {
         * e.printStackTrace();
         * logger.error("Error unmarshalling dictionary: " + e.getMessage());
         * }
         */
        dictionary = getDictionary2(xmlData);
        return dictionary;
    }

    private Dictionary getDictionary2(String xmlData) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Dictionary dictionary = null;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlData)));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression xPathExpr = xpath.compile("/dictionary/entry");
            NodeList nodes = (NodeList) xPathExpr.evaluate(doc, XPathConstants.NODESET);
            dictionary = new Dictionary();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                Entry element = new Entry(
                        node.getElementsByTagName("key").item(0).getTextContent(),
                        node.getElementsByTagName("value").item(0).getTextContent()
                );
                //dictionary.entrMap.put(element.key, element.value);
                dictionary.put(element.key, element.value);
            }
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return dictionary;
    }
}
