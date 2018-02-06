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
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Element;
import org.jdom2.Namespace;

import de.intranda.digiverso.m2m.DataManager;
import de.intranda.digiverso.m2m.oai.RequestHandler;
import de.intranda.digiverso.m2m.oai.enums.Metadata;
import de.intranda.digiverso.m2m.oai.model.ErrorCode;
import de.intranda.digiverso.m2m.utils.SolrConstants;
import de.intranda.digiverso.m2m.utils.SolrSearchIndex;
import de.intranda.digiverso.m2m.utils.Utils;

/**
 * ESE
 */
public class EuropeanaFormat extends OAIDCFormat {
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#createListRecords(de.intranda.digiverso.m2m.oai.RequestHandler, int, int)
     */
    @Override
    public Element createListRecords(RequestHandler handler, int firstRow, int numRows) throws SolrServerException {
        QueryResponse qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRow, numRows, false,
                SolrSearchIndex.getAdditionalDocstructsQuerySuffix(DataManager.getInstance()
                        .getConfiguration()
                        .getAdditionalDocstructTypes()),
                null);
        if (qr.getResults()
                .isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        return generateESE(qr.getResults(), qr.getResults()
                .getNumFound(), firstRow, numRows, handler, "ListRecords");
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
            return generateESE(Collections.singletonList(doc), 1L, 0, 1, handler, "GetRecord");
        } catch (IOException e) {
            return new ErrorCode().getNoMetadataFormats();
        } catch (SolrServerException e) {
            return new ErrorCode().getNoMetadataFormats();
        }
    }

    /**
     * Mandatory elements: europeana:provider europeana:dataProvider europeana:rights europeana:type europeana:isShownBy and/or europeana:isShownAt
     * 
     * @param records
     * @param totalHits
     * @param firstRow
     * @param numRows
     * @param handler
     * @param recordType
     * @return
     * @throws SolrServerException
     */
    private Element generateESE(List<SolrDocument> records, long totalHits, int firstRow, int numRows, RequestHandler handler, String recordType)
            throws SolrServerException {
        Namespace xmlns = DataManager.getInstance()
                .getConfiguration()
                .getStandardNameSpace();
        // Namespace nsOaiDc = Namespace.getNamespace(Metadata.oai_dc.getMetadataPrefix(), Metadata.oai_dc.getMetadataNamespace());
        Namespace nsDc = Namespace.getNamespace(Metadata.dc.getMetadataPrefix(), Metadata.dc.getMetadataNamespace());
        Namespace nsDcTerms = Namespace.getNamespace("dcterms", "http://purl.org/dc/terms/");
        Namespace nsEuropeana = Namespace.getNamespace(Metadata.ese.getMetadataPrefix(), Metadata.ese.getMetadataNamespace());

        Element xmlListRecords = new Element(recordType, xmlns);

        if (records.size() < numRows) {
            numRows = records.size();
        }
        for (SolrDocument doc : records) {
            Element record = new Element("record", xmlns);

            boolean isWork = doc.getFieldValue(SolrConstants.ISWORK) != null && (boolean) doc.getFieldValue(SolrConstants.ISWORK);
            boolean isAnchor = doc.getFieldValue(SolrConstants.ISANCHOR) != null && (boolean) doc.getFieldValue(SolrConstants.ISANCHOR);
            SolrDocument topstructDoc = null;
            if (isWork || isAnchor) {
                topstructDoc = doc;
            } else {
                // If child element metadata fields are empty, get certain values from topstruct
                String iddocTopstruct = (String) doc.getFieldValue(SolrConstants.IDDOC_TOPSTRUCT);
                SolrDocumentList docList = solr.search(SolrConstants.IDDOC + ":" + iddocTopstruct);
                if (docList != null && !docList.isEmpty()) {
                    topstructDoc = docList.get(0);
                }
            }
            SolrDocument anchorDoc = null;
            if (!isAnchor) {
                SolrDocument childDoc = topstructDoc != null ? topstructDoc : doc;
                String iddocAnchor = (String) childDoc.getFieldValue(SolrConstants.IDDOC_PARENT);
                if (iddocAnchor != null) {
                    SolrDocumentList docList = solr.search(SolrConstants.IDDOC + ":" + iddocAnchor);
                    if (docList != null && !docList.isEmpty()) {
                        anchorDoc = docList.get(0);
                    }
                }
            }

            Element header = getHeader(doc, topstructDoc, handler);
            record.addContent(header);

            String identifier = null;
            String urn = null;
            String title = null;
            String creators = null;
            String publisher = null;
            String yearpublish = null;
            String placepublish = null;
            String type = null;

            // create the metadata element, special for dc
            Element eleMetadata = new Element("metadata", xmlns);
            // eleMetadata.setNamespace(Namespace.getNamespace(nsEuropeana.getURI()));
            eleMetadata.addNamespaceDeclaration(nsEuropeana);
            eleMetadata.addNamespaceDeclaration(nsDc);
            eleMetadata.addNamespaceDeclaration(nsDcTerms);

            Element eleEuropeanaRecord = new Element("record", nsEuropeana);

            // <dc:identifier>
            if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.URN))) {
                Element eleDcIdentifier = new Element("identifier", nsDc);
                urn = (String) doc.getFieldValue(SolrConstants.URN);
                eleDcIdentifier.setText(DataManager.getInstance()
                        .getConfiguration()
                        .getUrnResolverUrl() + urn);
                eleEuropeanaRecord.addContent(eleDcIdentifier);
            } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI))) {
                Element eleDcIdentifier = new Element("identifier", nsDc);
                identifier = (String) doc.getFieldValue(SolrConstants.PI);
                eleDcIdentifier.setText(DataManager.getInstance()
                        .getConfiguration()
                        .getPiResolverUrl() + identifier);
                eleEuropeanaRecord.addContent(eleDcIdentifier);
            } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))) {
                Element eleDcIdentifier = new Element("identifier", nsDc);
                identifier = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
                eleDcIdentifier.setText(DataManager.getInstance()
                        .getConfiguration()
                        .getPiResolverUrl() + identifier);
                eleEuropeanaRecord.addContent(eleDcIdentifier);
            }
            // <dc:language>
            {
                Element eleDcLanguage = new Element("language", nsDc);
                String language = "";
                if (doc.getFieldValues("MD_LANGUAGE") != null) {
                    language = (String) doc.getFieldValues("MD_LANGUAGE")
                            .iterator()
                            .next();
                    eleDcLanguage.setText(language);
                    eleEuropeanaRecord.addContent(eleDcLanguage);
                }
                // if (language.length() == 2) {
                // // Convert ISO 639-1 to ISO 639-2
                // Locale locale = new Locale(language);
                // language = locale.getISO3Language();
                // }
            }
            // MANDATORY: <dc:title>
            {
                Element dc_title = new Element("title", nsDc);
                if (doc.getFieldValues(SolrConstants.TITLE) != null) {
                    title = (String) doc.getFieldValues(SolrConstants.TITLE)
                            .iterator()
                            .next();
                    // logger.debug("MD_TITLE : " + title);
                }
                if (isWork && doc.getFieldValue(SolrConstants.IDDOC_PARENT) != null) {
                    // If this is a volume, add anchor title in front
                    String anchorTitle = getAnchorTitle(doc);
                    if (anchorTitle != null) {
                        title = anchorTitle + "; " + title;
                    }
                }
                dc_title.setText(title);
                eleEuropeanaRecord.addContent(dc_title);
            }
            // <dc:description>
            {
                String value = null;
                if (doc.getFieldValues("MD_INFORMATION") != null) {
                    value = (String) doc.getFieldValues("MD_INFORMATION")
                            .iterator()
                            .next();

                } else if (doc.getFieldValues("MD_DATECREATED") != null) {
                    value = (String) doc.getFieldValues("MD_DATECREATED")
                            .iterator()
                            .next();

                }
                if (value != null) {
                    Element eleDcDescription = new Element("description", nsDc);
                    eleDcDescription.setText(value);
                    eleEuropeanaRecord.addContent(eleDcDescription);
                }
            }
            // <dc:date>
            {
                if (doc.getFieldValues("MD_YEARPUBLISH") != null) {
                    yearpublish = (String) doc.getFieldValues("MD_YEARPUBLISH")
                            .iterator()
                            .next();
                } else if (doc.getFieldValues("MD_DATECREATED") != null) {
                    yearpublish = (String) doc.getFieldValues("MD_DATECREATED")
                            .iterator()
                            .next();
                } else if (topstructDoc != null && topstructDoc.getFieldValues("MD_YEARPUBLISH") != null) {
                    yearpublish = (String) topstructDoc.getFieldValues("MD_YEARPUBLISH")
                            .iterator()
                            .next();
                } else if (topstructDoc != null && topstructDoc.getFieldValues("MD_DATECREATED") != null) {
                    yearpublish = (String) topstructDoc.getFieldValues("MD_DATECREATED")
                            .iterator()
                            .next();
                }

                if (yearpublish != null) {
                    Element eleDcDate = new Element("date", nsDc);
                    eleDcDate.setText(yearpublish);
                    eleEuropeanaRecord.addContent(eleDcDate);
                }
            }
            // <dc:creator>
            {
                if (doc.getFieldValues("MD_CREATOR") != null) {
                    for (Object fieldValue : doc.getFieldValues("MD_CREATOR")) {
                        String value = (String) fieldValue;
                        if (StringUtils.isBlank(value)) {
                            continue;
                        }
                        if (StringUtils.isEmpty(creators)) {
                            creators = value;
                        } else {
                            creators += ", " + value;
                        }
                        Element dc_creator = new Element("creator", nsDc);
                        dc_creator.setText(value);
                        eleEuropeanaRecord.addContent(dc_creator);
                    }
                }
            }
            // <dc:created>
            {
                if (doc.getFieldValues("MD_DATECREATED") != null) {
                    String created = (String) doc.getFieldValues("MD_DATECREATED")
                            .iterator()
                            .next();
                    Element eleDcCreated = new Element("created", nsDc);
                    eleDcCreated.setText(created);
                    eleEuropeanaRecord.addContent(eleDcCreated);
                }
            }
            // <dc:issued>
            {
                if (doc.getFieldValues("MD_DATEISSUED") != null) {
                    String created = (String) doc.getFieldValues("MD_DATEISSUED")
                            .iterator()
                            .next();
                    Element eleDcCreated = new Element("created", nsDc);
                    eleDcCreated.setText(created);
                    eleEuropeanaRecord.addContent(eleDcCreated);
                }
            }
            // creating <dc:subject>
            if (doc.getFieldValues(SolrConstants.DC) != null) {
                for (Object fieldValue : doc.getFieldValues(SolrConstants.DC)) {
                    Element dc_type = new Element("subject", nsDc);
                    if (((String) fieldValue).equals("")) {
                        dc_type.setText((String) fieldValue);
                        eleEuropeanaRecord.addContent(dc_type);
                    }
                }
            }

            // <dc:publisher>
            if (doc.getFieldValues("MD_PUBLISHER") != null) {
                publisher = (String) doc.getFieldValues("MD_PUBLISHER")
                        .iterator()
                        .next();
            } else if (topstructDoc != null && topstructDoc.getFieldValues("MD_PUBLISHER") != null) {
                publisher = (String) topstructDoc.getFieldValues("MD_PUBLISHER")
                        .iterator()
                        .next();
            }
            if (publisher != null) {
                Element dc_publisher = new Element("publisher", nsDc);
                dc_publisher.setText(publisher);
                eleEuropeanaRecord.addContent(dc_publisher);
            }

            if (doc.getFieldValues("MD_PLACEPUBLISH") != null) {
                placepublish = (String) doc.getFieldValues("MD_PLACEPUBLISH")
                        .iterator()
                        .next();
            } else if (topstructDoc != null && topstructDoc.getFieldValues("MD_PLACEPUBLISH") != null) {
                placepublish = (String) topstructDoc.getFieldValues("MD_PLACEPUBLISH")
                        .iterator()
                        .next();
            }

            // <dc:type>
            {
                Element eleDcType = new Element("type", nsDc);
                if (doc.getFieldValue(SolrConstants.DOCSTRCT) != null) {
                    type = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
                    eleDcType.setText(type);
                    eleEuropeanaRecord.addContent(eleDcType);
                }
                // // always create a standard type with the default dc:type
                // eleDcType = new Element("type", nsDc);
                // eleDcType.setText("Text");
                // oai_dc.addContent(eleDcType);
            }

            // <dc:format>
            // <dc:format> second one appears always
            Element dc_format = new Element("format", nsDc);
            dc_format.setText("image/jpeg");
            eleEuropeanaRecord.addContent(dc_format);

            dc_format = new Element("format", nsDc);
            dc_format.setText("application/pdf");
            eleEuropeanaRecord.addContent(dc_format);

            // <dc:source>
            eleEuropeanaRecord.addContent(generateDcSource(doc, topstructDoc, anchorDoc, nsDc));

            // ESE elements have a mandatory order

            // MANDATORY: <europeana:provider>
            {
                Element eleEuropeanaProvider = new Element("provider", nsEuropeana);
                String value = DataManager.getInstance()
                        .getConfiguration()
                        .getEseDefaultProvider();
                String field = DataManager.getInstance()
                        .getConfiguration()
                        .getEseProviderField();
                if (doc.getFieldValues(field) != null) {
                    value = (String) doc.getFieldValues(field)
                            .iterator()
                            .next();
                }
                eleEuropeanaProvider.setText(value);
                eleEuropeanaRecord.addContent(eleEuropeanaProvider);
            }
            // MANDATORY: <europeana:type>
            {
                Element eleEuropeanaType = new Element("type", nsEuropeana);
                String europeanaType = "TEXT";
                if (type != null) {
                    // Retrieve coded ESE type, if available
                    Map<String, String> eseTypes = DataManager.getInstance()
                            .getConfiguration()
                            .getEseTypes();
                    if (eseTypes.get(type) != null) {
                        europeanaType = eseTypes.get(type);
                    }
                }
                eleEuropeanaType.setText(europeanaType);
                eleEuropeanaRecord.addContent(eleEuropeanaType);
            }
            // MANDATORY: <europeana:rights>
            {
                Element eleEuropeanaRights = new Element("rights", nsEuropeana);
                String value = DataManager.getInstance()
                        .getConfiguration()
                        .getEseDefaultRightsUrl();
                String field = DataManager.getInstance()
                        .getConfiguration()
                        .getEseRightsField();
                if (doc.getFieldValues(field) != null) {
                    value = (String) doc.getFieldValues(field)
                            .iterator()
                            .next();
                }
                eleEuropeanaRights.setText(value);
                eleEuropeanaRecord.addContent(eleEuropeanaRights);
            }
            // MANDATORY: <europeana:dataProvider>
            {
                Element eleEuropeanaDataProvider = new Element("dataProvider", nsEuropeana);
                String value = DataManager.getInstance()
                        .getConfiguration()
                        .getEseDefaultProvider();
                String field = DataManager.getInstance()
                        .getConfiguration()
                        .getEseDataProviderField();
                if (doc.getFieldValues(field) != null) {
                    value = (String) doc.getFieldValues(field)
                            .iterator()
                            .next();
                }
                eleEuropeanaDataProvider.setText(value);
                eleEuropeanaRecord.addContent(eleEuropeanaDataProvider);
            }
            // MANDATORY: <europeana:isShownBy> or <europeana:isShownAt>
            {
                Element eleEuropeanaIsShownAt = new Element("isShownAt", nsEuropeana);
                String isShownAt = "";
                if (urn != null) {
                    isShownAt = DataManager.getInstance()
                            .getConfiguration()
                            .getUrnResolverUrl() + urn;
                } else if (identifier != null) {
                    isShownAt = DataManager.getInstance()
                            .getConfiguration()
                            .getPiResolverUrl() + identifier;
                }
                eleEuropeanaIsShownAt.setText(isShownAt);
                eleEuropeanaRecord.addContent(eleEuropeanaIsShownAt);
            }
            eleMetadata.addContent(eleEuropeanaRecord);
            record.addContent(eleMetadata);
            xmlListRecords.addContent(record);
        }

        // Create resumption token
        if (totalHits > firstRow + numRows) {
            Element resumption = createResumptionTokenAndElement(totalHits, firstRow + numRows, xmlns, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }
}
