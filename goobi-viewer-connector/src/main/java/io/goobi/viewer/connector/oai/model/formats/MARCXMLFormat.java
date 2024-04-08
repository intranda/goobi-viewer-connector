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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.XSLTransformException;
import org.jdom2.transform.XSLTransformer;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.utils.XmlConstants;
import io.goobi.viewer.controller.XmlTools;

/**
 * MARCXML
 */
public class MARCXMLFormat extends METSFormat {

    private static final Logger logger = LogManager.getLogger(MARCXMLFormat.class);

    static final Namespace NAMESPACE_XML = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");
    public static final Namespace NAMESPACE_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
    static final Namespace NAMESPACE_MARC = Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim");
    static final Namespace NAMESPACE_MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

    /** {@inheritDoc} */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField,
            String filterQuerySuffix) throws IOException, SolrServerException {
        Element mets = super.createListRecords(handler, firstVirtualRow, firstRawRow, numRows, versionDiscriminatorField, filterQuerySuffix);
        if (mets.getName().equals(XmlConstants.ELE_NAME_ERROR)) {
            return mets;
        }
        return generateMarc(mets, null, "ListRecords");
    }

    /** {@inheritDoc} */
    @Override
    public Element createGetRecord(RequestHandler handler, String filterQuerySuffix) {
        logger.trace("createGetRecord");
        Element mets = super.createGetRecord(handler, filterQuerySuffix);
        if (mets.getName().equals(XmlConstants.ELE_NAME_ERROR)) {
            return mets;
        }

        return generateMarc(mets, handler.getIdentifier(), "GetRecord");
    }

    /**
     * 
     * @param mets
     * @param identifier
     * @param recordType
     * @return {@link Element}
     * @should generate element correctly
     */
    static Element generateMarc(Element mets, String identifier, String recordType) {
        logger.trace("generateMarc");
        Element xmlListRecords = new Element(recordType, NAMESPACE_XML);
        Element token = mets.getChild("resumptionToken", NAMESPACE_XML);
        List<Element> records = mets.getChildren(XmlConstants.ELE_NAME_RECORD, NAMESPACE_XML);

        for (Element rec : records) {
            Element header = rec.getChild(XmlConstants.ELE_NAME_HEADER, NAMESPACE_XML);
            Element rootMets = rec.getChild(XmlConstants.ELE_NAME_METADATA, null).getChild("mets", NAMESPACE_METS);
            Element rootMarc = null;
            Element rootMods = null;
            Element subMods = null;
            if (identifier != null) {
                // Look up DMDID via CONTENTIDS, then look up MODS element
                List<Element> eleListDmdid = XmlTools.evaluateToElements(
                        "mets:structMap[@TYPE='LOGICAL']/mets:div/mets:div[@CONTENTIDS='" + identifier + "']", rootMets,
                        Arrays.asList(NAMESPACE_METS));
                if (!eleListDmdid.isEmpty()) {
                    String dmdid = eleListDmdid.get(0).getAttributeValue("DMDID");
                    List<Element> eleListMods =
                            XmlTools.evaluateToElements("mets:dmdSec[@ID='" + dmdid + "']/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods",
                                    rootMets, Arrays.asList(NAMESPACE_METS, NAMESPACE_MODS));
                    if (eleListMods != null && !eleListMods.isEmpty()) {
                        subMods = eleListMods.get(0);
                    }
                }
            }
            if (subMods == null) {
                // Look up native MARC for the main record
                List<Element> eleListMarc = XmlTools.evaluateToElements("mets:dmdSec/mets:mdWrap[@MDTYPE='MARC']/mets:xmlData/marc:marc", rootMets,
                        Arrays.asList(NAMESPACE_METS, NAMESPACE_MARC));
                // Alternative MARCXML embedding
                if (eleListMarc == null || eleListMarc.isEmpty()) {
                    eleListMarc = XmlTools.evaluateToElements("mets:dmdSec/mets:mdWrap[@MDTYPE='MARC']/mets:xmlData/bib/record", rootMets,
                            Arrays.asList(NAMESPACE_METS, NAMESPACE_MARC));
                }
                if (eleListMarc != null && !eleListMarc.isEmpty()) {
                    rootMarc = eleListMarc.get(0);
                } else {
                    // MODS for conversion
                    List<Element> eleListMods = XmlTools.evaluateToElements("mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods",
                            rootMets, Arrays.asList(NAMESPACE_METS, NAMESPACE_MODS));
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
                Element eleRecord = new Element(XmlConstants.ELE_NAME_RECORD, NAMESPACE_XML);
                Element newheader = new Element(XmlConstants.ELE_NAME_HEADER, NAMESPACE_XML);
                newheader.addContent(header.cloneContent());
                eleRecord.addContent(newheader);

                Element metadata = new Element(XmlConstants.ELE_NAME_METADATA, NAMESPACE_XML);
                Element answer = new Element(XmlConstants.ELE_NAME_RECORD, NAMESPACE_MARC);
                answer.addNamespaceDeclaration(XSI_NS);
                answer.setAttribute("schemaLocation", "http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd",
                        XSI_NS);
                answer.addContent(rootMarc.cloneContent());
                metadata.addContent(answer);
                eleRecord.addContent(metadata);
                xmlListRecords.addContent(eleRecord);
            } else if (rootMods != null) {
                // Root MODS to MARC
                logger.trace("root MODS");
                xmlListRecords.addContent(convertModsToMarc(rootMods, header));
            }
        }
        if (token != null) {
            Element resumption = new Element("resumptionToken", NAMESPACE_XML);
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
     * @return {@link Element}
     */
    static Element convertModsToMarc(Element mods, Element header) {
        Element newmods = new Element("mods", NAMESPACE_MODS);
        newmods.addContent(mods.cloneContent());
        newmods.addNamespaceDeclaration(NAMESPACE_MARC);
        org.jdom2.Document marcDoc = new org.jdom2.Document();
        marcDoc.setRootElement(newmods);

        String filename = DataManager.getInstance().getConfiguration().getMods2MarcXsl();
        try (FileInputStream fis = new FileInputStream(filename)) {
            XSLTransformer transformer = new XSLTransformer(fis);
            org.jdom2.Document docTrans = transformer.transform(marcDoc);
            Element root = docTrans.getRootElement();

            Element eleRecord = new Element(XmlConstants.ELE_NAME_RECORD, NAMESPACE_XML);
            Element newheader = new Element(XmlConstants.ELE_NAME_HEADER, NAMESPACE_XML);
            newheader.addContent(header.cloneContent());
            eleRecord.addContent(newheader);

            Element metadata = new Element(XmlConstants.ELE_NAME_METADATA, NAMESPACE_XML);
            Element answer = new Element(XmlConstants.ELE_NAME_RECORD, NAMESPACE_MARC);
            answer.addNamespaceDeclaration(XSI_NS);
            answer.setAttribute("schemaLocation", "http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd",
                    XSI_NS);
            answer.addContent(root.cloneContent());
            metadata.addContent(answer);
            eleRecord.addContent(metadata);
            return eleRecord;
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getCannotDisseminateFormat();
        } catch (IOException | XSLTransformException e) {
            logger.error(e.getMessage(), e);
            return new ErrorCode().getCannotDisseminateFormat();
        }
    }
}
