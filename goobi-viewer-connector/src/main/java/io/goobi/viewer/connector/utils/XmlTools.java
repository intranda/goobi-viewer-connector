/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.connector.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML utilities.
 */
public class XmlTools {

    private static final Logger logger = LoggerFactory.getLogger(XmlTools.class);

    /**
     *
     * @param filePath
     * @return
     * @throws FileNotFoundException if file not found
     * @throws IOException in case of errors
     * @throws JDOMException
     * @should build document from string correctly
     * @should throw FileNotFoundException if file not found
     */
    public static Document readXmlFile(String filePath) throws FileNotFoundException, IOException, JDOMException {
        try (FileInputStream fis = new FileInputStream(new File(filePath))) {
            return new SAXBuilder().build(fis);
        }
    }

    /**
     * Reads an XML document from the given URL and returns a JDOM2 document. Works with XML files within JARs.
     * 
     * @param url
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws JDOMException
     * @should build document from url correctly
     */
    public static Document readXmlFile(URL url) throws FileNotFoundException, IOException, JDOMException {
        try (InputStream is = url.openStream()) {
            return new SAXBuilder().build(is);
        }
    }

    /**
     * 
     * @param path
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws JDOMException
     * @should build document from path correctly
     */
    public static Document readXmlFile(Path path) throws FileNotFoundException, IOException, JDOMException {
        try (InputStream is = Files.newInputStream(path)) {
            return new SAXBuilder().build(is);
        }
    }

    /**
     * Create a JDOM document from an XML string.
     *
     * @param string
     * @return
     * @throws IOException
     * @throws JDOMException
     * @should build document correctly
     */
    public static Document getDocumentFromString(String string, String encoding) throws JDOMException, IOException {
        if (encoding == null) {
            encoding = Utils.DEFAULT_ENCODING;
        }

        byte[] byteArray = null;
        try {
            byteArray = string.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
        }
        ByteArrayInputStream baos = new ByteArrayInputStream(byteArray);

        // Reader reader = new StringReader(hOCRText);
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(baos);

        return document;
    }

    /**
     * @param element
     * @param encoding
     * @return
     * @should return XML string correctly for documents
     * @should return XML string correctly for elements
     */
    public static String getStringFromElement(Object element, String encoding) {
        if (element == null) {
            throw new IllegalArgumentException("element may not be null");
        }
        if (encoding == null) {
            encoding = Utils.DEFAULT_ENCODING;
        }
        Format format = Format.getRawFormat();
        XMLOutputter outputter = new XMLOutputter(format);
        Format xmlFormat = outputter.getFormat();
        if (StringUtils.isNotEmpty(encoding)) {
            xmlFormat.setEncoding(encoding);
        }
        xmlFormat.setExpandEmptyElements(true);
        outputter.setFormat(xmlFormat);

        String docString = null;
        if (element instanceof Document) {
            docString = outputter.outputString((Document) element);
        } else if (element instanceof Element) {
            docString = outputter.outputString((Element) element);
        }

        return docString;
    }

    /**
     * Evaluates the given XPath expression to a list of elements.
     * 
     * @param expr XPath expression to evaluate.
     * @param parent If not null, the expression is evaluated relative to this element.
     * @param namespaces
     * @return {@link ArrayList} or null
     * @should return all values
     */
    public static List<Element> evaluateToElements(String expr, Element element, List<Namespace> namespaces) {
        List<Element> retList = new ArrayList<>();

        List<Object> list = evaluate(expr, element, Filters.element(), namespaces);
        if (list == null) {
            return null;
        }
        for (Object object : list) {
            if (object instanceof Element) {
                retList.add((Element) object);
            }
        }
        return retList;
    }

    /**
     * XPath evaluation with a given return type filter.
     * 
     * @param expr XPath expression to evaluate.
     * @param parent If not null, the expression is evaluated relative to this element.
     * @param filter Return type filter.
     * @param namespaces
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List<Object> evaluate(String expr, Object parent, Filter filter, List<Namespace> namespaces) {
        XPathBuilder<Object> builder = new XPathBuilder<>(expr.trim().replace("\n", ""), filter);

        if (namespaces != null && !namespaces.isEmpty()) {
            builder.setNamespaces(namespaces);
        }

        XPathExpression<Object> xpath = builder.compileWith(XPathFactory.instance());
        return xpath.evaluate(parent);

    }

    public static String determineFileFormat(String xml, String encoding) throws JDOMException, IOException {
        if (xml == null) {
            return null;
        }
        Document doc = getDocumentFromString(xml, encoding);
        return determineFileFormat(doc);
    }

    /**
     * Determines the format of the given XML file by checking for namespaces.
     * 
     * @param path
     * @return
     * @should detect mets files correctly
     * @should detect lido files correctly
     * @should detect abbyy files correctly
     * @should detect tei files correctly
     */
    public static String determineFileFormat(Document doc) {
        if (doc == null || doc.getRootElement() == null) {
            return null;
        }

        if (doc.getRootElement().getNamespace("mets") != null) {
            return "METS";
        }
        if (doc.getRootElement().getNamespace("lido") != null) {
            return "LIDO";
        }
        if (doc.getRootElement().getNamespace().getURI().contains("abbyy")) {
            return "ABBYYXML";
        }
        if (doc.getRootElement().getName().equals("TEI") || doc.getRootElement().getName().equals("TEI.2")) {
            return "TEI";
        }

        return null;
    }

    /**
     * Transforms the given JDOM document via the given XSLT stylesheet.
     * 
     * @param doc JDOM document to transform
     * @param stylesheetPath Absolute path to the XSLT stylesheet file
     * @param params Optional transformer parameters
     * @return Transformed document; null in case of errors
     */
    public static Document transformViaXSLT(Document doc, String stylesheetPath, Map<String, String> params) {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }
        if (stylesheetPath == null) {
            throw new IllegalArgumentException("stylesheetPath may not be null");
        }

        try {
            JDOMSource docFrom = new JDOMSource(doc);
            JDOMResult docTo = new JDOMResult();

            Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(stylesheetPath));
            if (params != null && !params.isEmpty()) {
                for (String param : params.keySet()) {
                    transformer.setParameter(param, params.get(param));
                }
            }
            transformer.transform(docFrom, docTo);
            return docTo.getDocument();
        } catch (TransformerConfigurationException e) {
            logger.error(e.getMessage(), e);
        } catch (TransformerFactoryConfigurationError e) {
            logger.error(e.getMessage(), e);
        } catch (TransformerException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }
}
