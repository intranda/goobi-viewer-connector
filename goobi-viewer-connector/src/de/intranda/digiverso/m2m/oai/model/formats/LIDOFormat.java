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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.m2m.DataManager;
import de.intranda.digiverso.m2m.oai.RequestHandler;
import de.intranda.digiverso.m2m.oai.enums.Metadata;
import de.intranda.digiverso.m2m.oai.model.ErrorCode;
import de.intranda.digiverso.m2m.utils.SolrConstants;
import de.intranda.digiverso.m2m.utils.Utils;

/**
 * LIDO
 */
public class LIDOFormat extends AbstractFormat {

    private final static Logger logger = LoggerFactory.getLogger(LIDOFormat.class);

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#createListRecords(de.intranda.digiverso.m2m.oai.RequestHandler, int, int)
     */
    @Override
    public Element createListRecords(RequestHandler handler, int firstRow, int numRows) throws IOException, SolrServerException {
        QueryResponse qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRow, numRows, false,
                " AND " + SolrConstants.SOURCEDOCFORMAT + ":LIDO", null);
        if (qr.getResults()
                .isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        try {
            Element xmlListRecords = generateLido(qr.getResults(), qr.getResults()
                    .getNumFound(), firstRow, numRows, handler, "ListRecords");
            return xmlListRecords;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#createGetRecord(de.intranda.digiverso.m2m.oai.RequestHandler)
     */
    @Override
    public Element createGetRecord(RequestHandler handler) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier());
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            Element record = generateLido(Collections.singletonList(doc), 1L, 0, 1, handler, "GetRecord");
            return record;
        } catch (IOException e) {
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        } catch (SolrServerException e) {
            return new ErrorCode().getIdDoesNotExist();
        }
    }

    /**
     * Creates LIDO records
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
     */
    private Element generateLido(List<SolrDocument> records, long totalHits, int firstRow, int numRows, RequestHandler handler, String recordType)
            throws JDOMException, IOException, SolrServerException {
        Namespace xmlns = DataManager.getInstance()
                .getConfiguration()
                .getStandardNameSpace();
        Element xmlListRecords = new Element(recordType, xmlns);

        Namespace lido = Namespace.getNamespace(Metadata.lido.getMetadataPrefix(), Metadata.lido.getMetadataNamespace());
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        if (records.size() < numRows) {
            numRows = records.size();
        }
        for (SolrDocument doc : records) {
            String url = new StringBuilder(DataManager.getInstance()
                    .getConfiguration()
                    .getDocumentResolverUrl()).append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))
                            .toString();
            String xml = Utils.getWebContent(url);
            if (StringUtils.isEmpty(xml)) {
                xmlListRecords.addContent(new ErrorCode().getCannotDisseminateFormat());
                continue;
            }

            org.jdom2.Document xmlDoc = Utils.getDocumentFromString(xml, null);
            Element xmlRoot = xmlDoc.getRootElement();
            Element newLido = new Element(Metadata.lido.getMetadataPrefix(), lido);
            newLido.addNamespaceDeclaration(xsi);
            newLido.setAttribute(
                    new Attribute("schemaLocation", "http://www.lido-schema.org http://www.lido-schema.org/schema/v1.0/lido-v1.0.xsd", xsi));
            newLido.addContent(xmlRoot.cloneContent());

            Element record = new Element("record", xmlns);
            Element header = getHeader(doc, null, handler);
            record.addContent(header);
            Element metadata = new Element("metadata", xmlns);
            metadata.addContent(newLido);
            // metadata.addContent(mets_root.cloneContent());
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
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#getTotalHits(java.util.Map)
     */
    @Override
    public long getTotalHits(Map<String, String> params) throws IOException, SolrServerException {
        return solr.getTotalHitNumber(params, false, " AND " + SolrConstants.SOURCEDOCFORMAT + ":LIDO", null);
    }

}
