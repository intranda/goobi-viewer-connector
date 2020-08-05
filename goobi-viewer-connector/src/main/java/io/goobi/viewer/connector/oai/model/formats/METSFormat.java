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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.exceptions.HTTPException;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.utils.SolrConstants;
import io.goobi.viewer.connector.utils.Utils;
import io.goobi.viewer.connector.utils.XmlTools;

/**
 * METS
 */
public class METSFormat extends Format {

    private static final Logger logger = LoggerFactory.getLogger(METSFormat.class);

    private static final String QUERY_SUFFIX = " +(" + SolrConstants.SOURCEDOCFORMAT + ":METS " + SolrConstants.DATEDELETED + ":*)";

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createListIdentifiers(io.goobi.viewer.connector.oai.RequestHandler, int, int, int, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public Element createListIdentifiers(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField)
            throws SolrServerException, IOException {
        Map<String, String> datestamp = Utils.filterDatestampFromRequest(handler);

        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListIdentifiers = new Element("ListIdentifiers", xmlns);

        QueryResponse qr;
        long totalVirtualHits;
        int virtualHitCount = 0;
        long totalRawHits;

        // One OAI record for each record proper
        qr = DataManager.getInstance().getSearchIndex().getListIdentifiers(datestamp, firstRawRow, numRows, QUERY_SUFFIX, null);
        if (qr.getResults().isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        totalVirtualHits = totalRawHits = qr.getResults().getNumFound();
        for (SolrDocument doc : qr.getResults()) {
            Element header = getHeader(doc, null, handler, null);
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
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createListRecords(io.goobi.viewer.connector.oai.RequestHandler, int, int, int, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField)
            throws IOException, SolrServerException {
        QueryResponse qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, false, QUERY_SUFFIX, null);
        if (qr.getResults().isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        try {
            return generateMets(qr.getResults(), qr.getResults().getNumFound(), firstRawRow, numRows, handler, "ListRecords");
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createGetRecord(io.goobi.viewer.connector.oai.RequestHandler)
     */
    /** {@inheritDoc} */
    @Override
    public Element createGetRecord(RequestHandler handler) {
        logger.trace("createGetRecord");
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier());
            if (doc == null) {
                logger.debug("Record not found in index: {}", handler.getIdentifier());
                return new ErrorCode().getCannotDisseminateFormat();
            }
            return generateMets(Collections.singletonList(doc), 1L, 0, 1, handler, "GetRecord");
        } catch (IOException e) {
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        } catch (SolrServerException e) {
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
     * @return
     * @throws IOException
     * @throws JDOMException
     * @throws SolrServerException
     * @throws HTTPException
     */
    private static Element generateMets(List<SolrDocument> records, long totalHits, int firstRow, int numRows, RequestHandler handler,
            String recordType) throws JDOMException, IOException, SolrServerException {
        logger.trace("generateMets");
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListRecords = new Element(recordType, xmlns);

        Namespace mets = Namespace.getNamespace(Metadata.mets.getMetadataNamespacePrefix(), Metadata.mets.getMetadataNamespaceUri());
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
                xml = Utils.getWebContentGET(url);
            } catch (HTTPException e) {
                xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
                continue;
            }
            if (StringUtils.isEmpty(xml)) {
                xmlListRecords.addContent(new ErrorCode().getIdDoesNotExist());
                continue;
            }

            org.jdom2.Document metsFile = XmlTools.getDocumentFromString(xml, null);
            Element metsRoot = metsFile.getRootElement();
            Element newMetsRoot = new Element(Metadata.mets.getMetadataPrefix(), mets);
            newMetsRoot.addNamespaceDeclaration(XSI);
            newMetsRoot.addNamespaceDeclaration(mods);
            newMetsRoot.addNamespaceDeclaration(dv);
            newMetsRoot.addNamespaceDeclaration(xlink);
            newMetsRoot.setAttribute("schemaLocation",
                    "http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-3.xsd http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/version17/mets.v1-7.xsd",
                    XSI);
            if (metsRoot.getAttributeValue("OBJID") != null) {
                newMetsRoot.setAttribute("OBJID", metsRoot.getAttributeValue("OBJID"));
            }
            newMetsRoot.addContent(metsRoot.cloneContent());

            Element record = new Element("record", xmlns);
            Element header = getHeader(doc, null, handler, null);
            record.addContent(header);
            Element metadata = new Element("metadata", xmlns);
            metadata.addContent(newMetsRoot);
            record.addContent(metadata);
            xmlListRecords.addContent(record);
        }

        // Create resumption token
        if (totalHits > firstRow + numRows) {
            Element resumption = createResumptionTokenAndElement(totalHits, firstRow + numRows, xmlns, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#getTotalHits(java.util.Map, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public long getTotalHits(Map<String, String> params, String versionDiscriminatorField) throws IOException, SolrServerException {
        return solr.getTotalHitNumber(params, false, QUERY_SUFFIX, null);
    }

}
