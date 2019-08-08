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
package io.goobi.viewer.connector.oai.model.formats;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.XSLTransformException;
import org.jdom2.transform.XSLTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.utils.XmlTools;

/**
 * MARCXML
 */
public class MARCXMLFormat extends METSFormat {

    private final static Logger logger = LoggerFactory.getLogger(MARCXMLFormat.class);

    static final Namespace xmlns = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");
    static final Namespace nsMets = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
    static final Namespace nsMarc = Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim");
    static final Namespace nsMods = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createListRecords(io.goobi.viewer.connector.oai.RequestHandler, int, int, int, java.lang.String)
     */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField)
            throws IOException, SolrServerException {
        Element mets = super.createListRecords(handler, firstVirtualRow, firstRawRow, numRows, versionDiscriminatorField);
        if (mets.getName().equals("error")) {
            return mets;
        }
        return generateMarc(mets, null, "ListRecords");
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createGetRecord(io.goobi.viewer.connector.oai.RequestHandler)
     */
    @Override
    public Element createGetRecord(RequestHandler handler) {
        logger.trace("createGetRecord");
        Element mets = super.createGetRecord(handler);
        if (mets.getName().equals("error")) {
            return mets;
        }
        return generateMarc(mets, handler.getIdentifier(), "GetRecord");
    }

    /**
     * 
     * @param mets
     * @param identifier
     * @param recordType
     * @return
     */
    private static Element generateMarc(Element mets, String identifier, String recordType) {
        logger.trace("generateMarc");
        Element xmlListRecords = new Element(recordType, xmlns);
        Element token = mets.getChild("resumptionToken", xmlns);
        List<Element> records = mets.getChildren("record", xmlns);

        for (Element rec : records) {
            // logger.trace(new XMLOutputter().outputString(rec));
            Element header = rec.getChild("header", xmlns);
            Element rootMets = rec.getChild("metadata", null).getChild("mets", nsMets);
            Element rootMarc = null;
            Element rootMods = null;
            Element subMods = null;
            if (identifier != null) {
                // Look up DMDID via CONTENTIDS, then look up MODS element
                List<Element> eleListDmdid = XmlTools.evaluateToElements(
                        "mets:structMap[@TYPE='LOGICAL']/mets:div/mets:div[@CONTENTIDS='" + identifier + "']", rootMets, Arrays.asList(nsMets));
                if (eleListDmdid != null && !eleListDmdid.isEmpty()) {
                    String dmdid = eleListDmdid.get(0).getAttributeValue("DMDID");
                    List<Element> eleListMods =
                            XmlTools.evaluateToElements("mets:dmdSec[@ID='" + dmdid + "']/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods",
                                    rootMets, Arrays.asList(nsMets, nsMods));
                    if (eleListMods != null && !eleListMods.isEmpty()) {
                        subMods = eleListMods.get(0);
                    }
                }
            }
            if (subMods == null) {
                // Look up native MARC for the main record
                List<Element> eleListMarc = XmlTools.evaluateToElements("mets:dmdSec/mets:mdWrap[@MDTYPE='MARC']/mets:xmlData/marc:marc", rootMets,
                        Arrays.asList(nsMets, nsMarc));
                if (eleListMarc != null && !eleListMarc.isEmpty()) {
                    rootMarc = eleListMarc.get(0);
                } else {
                    List<Element> eleListMods = XmlTools.evaluateToElements("mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods",
                            rootMets, Arrays.asList(nsMets, nsMods));
                    if (eleListMods != null && !eleListMods.isEmpty()) {
                        rootMods = eleListMods.get(0);
                    }
                }
            }

            if (subMods != null) {
                // Element-specific MODS for the main record, convert to MARC
                logger.trace("subelement MODS");
                xmlListRecords.addContent(convertModsToMarc(subMods, header));
            } else if (rootMarc != null) {
                // Native root MARC
                logger.trace("root MARC");
                Element record = new Element("record", xmlns);
                Element newheader = new Element("header", xmlns);
                newheader.addContent(header.cloneContent());
                record.addContent(newheader);

                Element metadata = new Element("metadata", xmlns);
                Element answer = new Element("record", nsMarc);
                answer.addNamespaceDeclaration(XSI);
                answer.setAttribute("schemaLocation", "http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd",
                        XSI);
                answer.addContent(rootMarc.cloneContent());
                metadata.addContent(answer);
                record.addContent(metadata);
                xmlListRecords.addContent(record);
            } else if (rootMods != null) {
                // Root MODS to MARC
                logger.trace("root MODS");
                xmlListRecords.addContent(convertModsToMarc(rootMods, header));
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

    /**
     * 
     * @param mods
     * @param header
     * @return
     */
    static Element convertModsToMarc(Element mods, Element header) {
        Element newmods = new Element("mods", nsMods);
        newmods.addContent(mods.cloneContent());
        newmods.addNamespaceDeclaration(nsMarc);
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
            Element answer = new Element("record", nsMarc);
            answer.addNamespaceDeclaration(XSI);
            answer.setAttribute("schemaLocation", "http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd", XSI);
            answer.addContent(root.cloneContent());
            metadata.addContent(answer);
            record.addContent(metadata);
            return record;
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
}
