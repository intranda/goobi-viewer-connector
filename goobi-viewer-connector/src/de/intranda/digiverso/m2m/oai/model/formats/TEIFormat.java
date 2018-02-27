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
import de.intranda.digiverso.m2m.oai.model.ErrorCode;
import de.intranda.digiverso.m2m.oai.model.language.Language;
import de.intranda.digiverso.m2m.utils.SolrConstants;
import de.intranda.digiverso.m2m.utils.SolrSearchIndex;
import de.intranda.digiverso.m2m.utils.Utils;

/**
 * Format for TEI and CMDI records.
 */
public class TEIFormat extends AbstractFormat {

    private static final Logger logger = LoggerFactory.getLogger(TEIFormat.class);

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#createListRecords(de.intranda.digiverso.m2m.oai.RequestHandler, int, int)
     */
    @Override
    public Element createListRecords(RequestHandler handler, int firstRow, int numRows) throws SolrServerException {
        // &stats=true&stats.field=LANGUAGE
        QueryResponse qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRow, numRows, false,
                " AND " + SolrConstants.LANGUAGE + ":*", Collections.singletonList(SolrConstants.LANGUAGE));
        if (qr.getResults()
                .isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        long numFound = SolrSearchIndex.getFieldCount(qr, SolrConstants.LANGUAGE);
        try {
            Element xmlListRecords = generateTeiCmdi(qr.getResults(), numFound, firstRow, numRows, handler, "ListRecords", null);
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

        String[] identifierSplit = Utils.splitIdentifierAndLanguageCode(handler.getIdentifier(), 3);
        try {
            SolrDocument doc = solr.getListRecord(identifierSplit[0]);
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            Element record = generateTeiCmdi(Collections.singletonList(doc), 1L, 0, 1, handler, "GetRecord", identifierSplit[1]);
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
     * Creates TEI or CMDI records.
     * 
     * @param records
     * @param totalHits
     * @param firstRow
     * @param numRows
     * @param handler
     * @param recordType "GetRecord" or "ListRecords"
     * @param requestedLanguage
     * @return
     * @throws IOException
     * @throws JDOMException
     * @throws SolrServerException
     */
    private static Element generateTeiCmdi(List<SolrDocument> records, long totalHits, int firstRow, int numRows, RequestHandler handler,
            String recordType, String requestedLanguage) throws JDOMException, IOException, SolrServerException {
        Namespace xmlns = DataManager.getInstance()
                .getConfiguration()
                .getStandardNameSpace();
        Element xmlListRecords = new Element(recordType, xmlns);

        Namespace namespace = Namespace.getNamespace(handler.getMetadataPrefix()
                .getMetadataPrefix(),
                handler.getMetadataPrefix()
                        .getMetadataNamespace());
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        if (records.size() < numRows) {
            numRows = records.size();
        }
        List<String> languages = Collections.singletonList(requestedLanguage);
        for (SolrDocument doc : records) {
            if (requestedLanguage == null) {
                languages = SolrSearchIndex.getMetadataValues(doc, SolrConstants.LANGUAGE);
            }
            for (String language : languages) {
                String url = new StringBuilder(DataManager.getInstance()
                        .getConfiguration()
                        .getContentApiUrl()).append(handler.getMetadataPrefix()
                                .getMetadataPrefix())
                                .append('/')
                                .append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))
                                .append('/')
                                .append(language)
                                .append('/')
                                .toString();
                // logger.trace(url);
                String xml = Utils.getWebContent(url);
                if (StringUtils.isEmpty(xml)) {
                    xmlListRecords.addContent(new ErrorCode().getCannotDisseminateFormat());
                    continue;
                }

                org.jdom2.Document xmlDoc = Utils.getDocumentFromString(xml, null);
                Element teiRoot = xmlDoc.getRootElement();
                String elementName = null;
                switch (handler.getMetadataPrefix()) {
                    case tei:
                        elementName = "tei";
                        break;
                    case cmdi:
                        elementName = "CMD";
                        break;
                    default:
                        break;
                }
                Element newDoc = new Element(elementName, namespace);
                newDoc.addNamespaceDeclaration(xsi);
                newDoc.setAttribute(new Attribute("schemaLocation", handler.getMetadataPrefix()
                        .getSchema(), xsi));
                newDoc.addContent(teiRoot.cloneContent());

                String iso3code = language;
                // Make sure to add the ISO-3 language code
                if (iso3code.length() == 2) {
                    Language lang = DataManager.getInstance()
                            .getLanguageHelper()
                            .getLanguage(language);
                    if (lang != null) {
                        iso3code = lang.getIsoCode();
                    }
                }
                Element record = new Element("record", xmlns);
                Element header = getHeader(doc, null, handler, iso3code);
                record.addContent(header);
                Element metadata = new Element("metadata", xmlns);
                metadata.addContent(newDoc);
                // metadata.addContent(mets_root.cloneContent());
                record.addContent(metadata);
                xmlListRecords.addContent(record);
            }
        }

        // Create resumption token
        if (totalHits > firstRow + numRows) {
            Element resumption = createResumptionTokenAndElement(totalHits, firstRow + numRows, xmlns, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }

    /**
     * Modified header generation where identifers also contain the language code.
     * 
     * @param doc Document from which to extract values.
     * @param topstructDoc If not null, the datestamp value will be determined from this instead.
     * @param handler
     * @param language
     * @return
     * @throws SolrServerException
     */
    protected static Element getHeader(SolrDocument doc, SolrDocument topstructDoc, RequestHandler handler, String language)
            throws SolrServerException {
        Namespace xmlns = DataManager.getInstance()
                .getConfiguration()
                .getStandardNameSpace();
        Element header = new Element("header", xmlns);
        // identifier
        Element identifier = new Element("identifier", xmlns);
        identifier.setText(DataManager.getInstance()
                .getConfiguration()
                .getOaiIdentifier()
                .get("repositoryIdentifier") + (String) doc.getFieldValue(SolrConstants.PI) + '_' + language);
        header.addContent(identifier);
        // datestamp
        Element datestamp = new Element("datestamp", xmlns);
        long untilTimestamp = RequestHandler.getUntilTimestamp(handler.getUntil());
        long timestampModified = SolrSearchIndex.getLatestValidDateUpdated(topstructDoc != null ? topstructDoc : doc, untilTimestamp);
        datestamp.setText(Utils.parseDate(timestampModified));
        if (StringUtils.isEmpty(datestamp.getText()) && doc.getFieldValue(SolrConstants.ISANCHOR) != null) {
            datestamp.setText(Utils.parseDate(DataManager.getInstance()
                    .getSearchIndex()
                    .getLatestVolumeTimestamp(doc, untilTimestamp)));
        }
        header.addContent(datestamp);
        // setSpec
        List<String> setSpecFields = DataManager.getInstance()
                .getConfiguration()
                .getSetSpecFieldsForMetadataFormat(handler.getMetadataPrefix()
                        .name());
        if (!setSpecFields.isEmpty()) {
            for (String setSpecField : setSpecFields) {
                if (doc.containsKey(setSpecField)) {
                    for (Object fieldValue : doc.getFieldValues(setSpecField)) {
                        // TODO translation
                        Element setSpec = new Element("setSpec", xmlns);
                        setSpec.setText((String) fieldValue);
                        header.addContent(setSpec);
                    }
                }
            }
        }

        // status="deleted"
        if (doc.getFieldValues(SolrConstants.DATEDELETED) != null) {
            header.setAttribute("status", "deleted");
        }

        return header;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#getTotalHits(java.util.Map)
     */
    @Override
    public long getTotalHits(Map<String, String> params) throws IOException, SolrServerException {
        // Query Solr index for the count of the LANGUAGE field
        QueryResponse qr = solr.search(params.get("from"), params.get("until"), params.get("set"), params.get("metadataPrefix"), 0, 0, false,
                " AND " + SolrConstants.LANGUAGE + ":*", Collections.singletonList(SolrConstants.LANGUAGE));
        return SolrSearchIndex.getFieldCount(qr, SolrConstants.LANGUAGE);
    }
}
