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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.utils.SolrConstants;
import io.goobi.viewer.connector.utils.Utils;
import io.goobi.viewer.connector.utils.XmlConstants;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.HTTPException;

/**
 * METS
 */
public class METSFormat extends Format {

    private static final Logger logger = LogManager.getLogger(METSFormat.class);

    private static final String METS_FILTER_QUERY = " +(+" + SolrConstants.SOURCEDOCFORMAT + ":METS " + "-" + SolrConstants.DATEDELETED + ":*)";

    private List<String> setSpecFields =
            DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.METS.getMetadataPrefix());

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createListIdentifiers(io.goobi.viewer.connector.oai.RequestHandler, int, int, int, java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public Element createListIdentifiers(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField,
            String filterQuerySuffix) throws SolrServerException, IOException {
        Map<String, String> datestamp = Utils.filterDatestampFromRequest(handler);

        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListIdentifiers = new Element("ListIdentifiers", xmlns);

        List<String> fieldList = new ArrayList<>(Arrays.asList(IDENTIFIER_FIELDS));
        fieldList.addAll(Arrays.asList(DATE_FIELDS));
        fieldList.addAll(setSpecFields);

        QueryResponse qr;
        long totalVirtualHits;
        int virtualHitCount = 0;
        long totalRawHits;

        // One OAI record for each record proper
        qr = DataManager.getInstance()
                .getSearchIndex()
                .getListIdentifiers(datestamp, firstRawRow, numRows, METS_FILTER_QUERY, fieldList, null, filterQuerySuffix);
        if (qr.getResults().isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        totalVirtualHits = totalRawHits = qr.getResults().getNumFound();
        for (SolrDocument doc : qr.getResults()) {
            Element header = getHeader(doc, null, handler, null, setSpecFields, filterQuerySuffix);
            xmlListIdentifiers.addContent(header);
            virtualHitCount++;
        }

        // Create resumption token
        if (totalRawHits > firstRawRow + numRows) {
            Element resumption = createResumptionTokenAndElement(totalVirtualHits, totalRawHits, firstVirtualRow + virtualHitCount,
                    firstRawRow + numRows, xmlns, handler);
            xmlListIdentifiers.addContent(resumption);
        }

        return xmlListIdentifiers;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createListRecords(io.goobi.viewer.connector.oai.RequestHandler, int, int, int, java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField,
            String filterQuerySuffix) throws IOException, SolrServerException {
        List<String> fieldList = new ArrayList<>(Arrays.asList(IDENTIFIER_FIELDS));
        fieldList.addAll(Arrays.asList(DATE_FIELDS));
        fieldList.addAll(setSpecFields);
        QueryResponse qr =
                solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, false, METS_FILTER_QUERY, filterQuerySuffix,
                        fieldList, null);
        if (qr.getResults().isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }

        return generateMets(qr.getResults(), qr.getResults().getNumFound(), firstRawRow, numRows, handler, "ListRecords", setSpecFields,
                filterQuerySuffix);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createGetRecord(io.goobi.viewer.connector.oai.RequestHandler, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public Element createGetRecord(RequestHandler handler, String filterQuerySuffix) {
        logger.trace("createGetRecord");
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        List<String> fieldList = new ArrayList<>(Arrays.asList(IDENTIFIER_FIELDS));
        fieldList.addAll(Arrays.asList(DATE_FIELDS));
        fieldList.addAll(setSpecFields);
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), fieldList, filterQuerySuffix);
            if (doc == null) {
                logger.debug("Record not found in index: {}", handler.getIdentifier());
                return new ErrorCode().getIdDoesNotExist();
            }
            return generateMets(Collections.singletonList(doc), 1L, 0, 1, handler, "GetRecord", setSpecFields, filterQuerySuffix);
        } catch (IOException | SolrServerException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        }
    }

    /**
     * Creates a list of METS documents for the given Solr document list.
     * 
     * @param records
     * @param totalHits
     * @param firstRow
     * @param numRows
     * @param handler
     * @param recordType "GetRecord" or "ListRecords"
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return
     * @throws JDOMException
     * @throws SolrServerException
     */
    private static Element generateMets(List<SolrDocument> records, long totalHits, int firstRow, int numRows, RequestHandler handler,
            String recordType, List<String> setSpecFields, String filterQuerySuffix) throws SolrServerException {
        logger.trace("generateMets");
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListRecords = new Element(recordType, xmlns);

        Namespace mets = Namespace.getNamespace(Metadata.METS.getMetadataNamespacePrefix(), Metadata.METS.getMetadataNamespaceUri());
        Namespace mods = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
        Namespace dv = Namespace.getNamespace("dv", "http://dfg-viewer.de/");
        Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

        if (records.size() < numRows) {
            numRows = records.size();
        }
        for (SolrDocument doc : records) {
            String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
            if (pi == null) {
                pi = (String) doc.getFieldValue(SolrConstants.PI);
            }
            if (pi == null) {
                xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
                continue;
            }
            String url = new StringBuilder(DataManager.getInstance().getConfiguration().getDocumentResolverUrl()).append(pi).toString();
            String xml = null;
            try {
                xml = NetTools.getWebContentGET(url);
            } catch (HTTPException | IOException e) {
                logger.error("Could not retrieve METS: {}", url);
                xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
                continue;
            }

            if (StringUtils.isEmpty(xml)) {
                logger.error("METS document is empty: {}", url);
                xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
                continue;
            }

            try {
                org.jdom2.Document metsFile = XmlTools.getDocumentFromString(xml, null);
                Element metsRoot = metsFile.getRootElement();
                Element newMetsRoot = new Element(Metadata.METS.getMetadataPrefix(), mets);
                newMetsRoot.addNamespaceDeclaration(XSI);
                newMetsRoot.addNamespaceDeclaration(mods);
                newMetsRoot.addNamespaceDeclaration(dv);
                newMetsRoot.addNamespaceDeclaration(xlink);
                newMetsRoot.setAttribute("schemaLocation",
                        "http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-3.xsd http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/version17/mets.v1-7.xsd",
                        XSI);
                if (metsRoot.getAttributeValue(XmlConstants.ATT_NAME_OBJID) != null) {
                    newMetsRoot.setAttribute(XmlConstants.ATT_NAME_OBJID, metsRoot.getAttributeValue(XmlConstants.ATT_NAME_OBJID));
                }
                newMetsRoot.addContent(metsRoot.cloneContent());

                Element eleRecord = new Element("record", xmlns);
                try {
                    Element header = getHeader(doc, null, handler, null, setSpecFields, filterQuerySuffix);
                    eleRecord.addContent(header);
                    Element metadata = new Element("metadata", xmlns);
                    metadata.addContent(newMetsRoot);
                    eleRecord.addContent(metadata);
                    xmlListRecords.addContent(eleRecord);
                } catch (IOException e) {
                    logger.error("Could not generate header: {}", e.getMessage());
                    xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
                }
            } catch (IOException | JDOMException e) {
                logger.error("{}", e.getMessage());
                logger.trace(xml);
                xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
            }
        }

        // Create resumption token
        if (totalHits > firstRow + numRows) {
            Element resumption = createResumptionTokenAndElement(totalHits, firstRow + numRows, xmlns, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#getTotalHits(java.util.Map, java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public long getTotalHits(Map<String, String> params, String versionDiscriminatorField, String filterQuerySuffix)
            throws IOException, SolrServerException {
        return solr.getTotalHitNumber(params, false, METS_FILTER_QUERY, null, filterQuerySuffix);
    }

}
