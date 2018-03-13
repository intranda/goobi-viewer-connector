/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package de.intranda.digiverso.m2m.oai.model.formats;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.XSLTransformException;
import org.jdom2.transform.XSLTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.m2m.DataManager;
import de.intranda.digiverso.m2m.oai.RequestHandler;
import de.intranda.digiverso.m2m.oai.model.ErrorCode;

/**
 * MARCXML
 */
public class MARCXMLFormat extends METSFormat {

    private final static Logger logger = LoggerFactory.getLogger(MARCXMLFormat.class);

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#createListRecords(de.intranda.digiverso.m2m.oai.RequestHandler, int, int)
     */
    @Override
    public Element createListRecords(RequestHandler handler, int firstRow, int numRows) throws IOException, SolrServerException {
        Element mets = super.createListRecords(handler, firstRow, numRows);
        if (mets.getName().equals("error")) {
            return mets;
        }
        return generateMarc(mets, "ListRecords");
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#createGetRecord(de.intranda.digiverso.m2m.oai.RequestHandler)
     */
    @Override
    public Element createGetRecord(RequestHandler handler) {
        Element mets = super.createGetRecord(handler);
        return generateMarc(mets, "GetRecord");
    }

    /**
     * 
     * @param mets
     * @param recordType
     * @return
     */
    private static Element generateMarc(Element mets, String recordType) {
        Namespace xmlns = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");
        Namespace metsNamespace = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
        Namespace marc = Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim");
        Namespace mods = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
        Element xmlListRecords = new Element(recordType, xmlns);
        Element token = mets.getChild("resumptionToken", xmlns);
        List<Element> records = mets.getChildren("record", xmlns);

        for (Element rec : records) {
            Element header = rec.getChild("header", xmlns);
            Element modsOnly = rec.getChild("metadata", xmlns)
                    .getChild("mets", metsNamespace)
                    .getChild("dmdSec", metsNamespace)
                    .getChild("mdWrap", metsNamespace)
                    .getChild("xmlData", metsNamespace)
                    .getChild("mods", mods);

            Element newmods = new Element("mods", mods);

            newmods.addContent(modsOnly.cloneContent());

            newmods.addNamespaceDeclaration(marc);
            org.jdom2.Document marcDoc = new org.jdom2.Document();

            marcDoc.setRootElement(newmods);

            String filename = DataManager.getInstance().getConfiguration().getMods2MarcXsl();

            try (FileInputStream fis = new FileInputStream(filename)) {
                XSLTransformer transformer = new XSLTransformer(fis);
                org.jdom2.Document docTrans = transformer.transform(marcDoc);
                Element root = docTrans.getRootElement();

                Element record = new Element("record", xmlns);
                Element newheader = new Element("header", xmlns);
                newheader.addContent(header.cloneContent());
                record.addContent(newheader);

                Element metadata = new Element("metadata", xmlns);
                Element answer = new Element("record", marc);
                answer.addNamespaceDeclaration(XSI);
                answer.setAttribute("schemaLocation", "http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd",
                        XSI);
                answer.addContent(root.cloneContent());
                metadata.addContent(answer);
                record.addContent(metadata);
                xmlListRecords.addContent(record);
            } catch (XSLTransformException e) {
                logger.error(e.getMessage(), e);
                return new ErrorCode().getCannotDisseminateFormat();
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
                return new ErrorCode().getCannotDisseminateFormat();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return new ErrorCode().getCannotDisseminateFormat();
            }
        }
        if (token != null) {
            Element resumption = new Element("resumptionToken", xmlns);
            resumption.setAttribute("expirationDate", token.getAttribute("expirationDate").getValue());
            resumption.setAttribute("completeListSize", token.getAttribute("completeListSize").getValue());
            resumption.setAttribute("cursor", token.getAttribute("cursor").getValue());
            resumption.setText(token.getValue());
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }
}
