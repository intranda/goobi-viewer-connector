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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.m2m.DataManager;
import de.intranda.digiverso.m2m.messages.MessageResourceBundle;
import de.intranda.digiverso.m2m.oai.RequestHandler;
import de.intranda.digiverso.m2m.oai.enums.Metadata;
import de.intranda.digiverso.m2m.oai.enums.Verb;
import de.intranda.digiverso.m2m.oai.model.ErrorCode;
import de.intranda.digiverso.m2m.oai.model.FieldConfiguration;
import de.intranda.digiverso.m2m.oai.model.language.Language;
import de.intranda.digiverso.m2m.utils.SolrConstants;
import de.intranda.digiverso.m2m.utils.SolrSearchIndex;
import de.intranda.digiverso.m2m.utils.Utils;

/**
 * oai_dc
 */
public class OAIDCFormat extends AbstractFormat {

    private static final Logger logger = LoggerFactory.getLogger(OAIDCFormat.class);

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#createListRecords(de.intranda.digiverso.m2m.oai.RequestHandler, int, int, int, java.lang.String)
     */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField)
            throws SolrServerException {
        QueryResponse qr;
        long totalVirtualHits;
        long totalRawHits;
        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            // One OAI record for each record version
            qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, false,
                    SolrSearchIndex.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes())
                            + " AND " + versionDiscriminatorField + ":*",
                    Collections.singletonList(versionDiscriminatorField));
            totalVirtualHits = SolrSearchIndex.getFieldCount(qr, versionDiscriminatorField);
            totalRawHits = qr.getResults().getNumFound();
        } else {
            // One OAI record for each record proper
            qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, false,
                    SolrSearchIndex.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes()),
                    null);
            totalVirtualHits = totalRawHits = qr.getResults().getNumFound();

        }
        if (qr.getResults().isEmpty()) {
            logger.trace("Results are empty");
            return new ErrorCode().getNoRecordsMatch();
        }

        return generateDC(qr.getResults(), totalVirtualHits, totalRawHits, firstVirtualRow, firstRawRow, numRows, handler, "ListRecords",
                versionDiscriminatorField, null);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#createGetRecord(de.intranda.digiverso.m2m.oai.RequestHandler)
     */
    @Override
    public Element createGetRecord(RequestHandler handler) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        String versionDiscriminatorField =
                DataManager.getInstance().getConfiguration().getVersionDisriminatorFieldForMetadataFormat(handler.getMetadataPrefix().name());
        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            String[] identifierSplit = Utils.splitIdentifierAndLanguageCode(handler.getIdentifier(), 3);
            try {
                SolrDocument doc = solr.getListRecord(identifierSplit[0]);
                if (doc == null) {
                    return new ErrorCode().getIdDoesNotExist();
                }
                return generateDC(Collections.singletonList(doc), 1L, 1L, 0, 0, 1, handler, "GetRecord", versionDiscriminatorField,
                        identifierSplit[1]);
            } catch (IOException e) {
                return new ErrorCode().getNoMetadataFormats();
            } catch (SolrServerException e) {
                return new ErrorCode().getNoMetadataFormats();
            }
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier());
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            return generateDC(Collections.singletonList(doc), 1L, 1L, 0, 0, 1, handler, "GetRecord", null, null);
        } catch (IOException e) {
            return new ErrorCode().getNoMetadataFormats();
        } catch (SolrServerException e) {
            return new ErrorCode().getNoMetadataFormats();
        }
    }

    /**
     * generates oai_dc records
     * 
     * @param records
     * @param totalHits
     * @param firstVirtualRow
     * @param firstRawRow
     * @param numRows
     * @param handler
     * @param recordType
     * @param versionDiscriminatorField
     * @param requestedVersion
     * @return
     * @throws SolrServerException
     */
    private Element generateDC(List<SolrDocument> records, long totalVirtualHits, long totalRawHits, int firstVirtualRow, int firstRawRow,
            int numRows, RequestHandler handler, String recordType, String versionDiscriminatorField, String requestedVersion)
            throws SolrServerException {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Namespace nsOaiDoc = Namespace.getNamespace(Metadata.oai_dc.getMetadataNamespacePrefix(), Metadata.oai_dc.getMetadataNamespaceUri());
        Element xmlListRecords = new Element(recordType, xmlns);

        if (records.size() < numRows) {
            numRows = records.size();
        }

        int virtualHitCount = 0;
        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            List<String> versions = Collections.singletonList(requestedVersion);
            for (SolrDocument doc : records) {
                if (requestedVersion == null) {
                    versions = SolrSearchIndex.getMetadataValues(doc, versionDiscriminatorField);
                }
                for (String version : versions) {
                    virtualHitCount++;
                    String iso3code = version;
                    // Make sure to add the ISO-3 language code
                    if (SolrConstants.LANGUAGE.equals(versionDiscriminatorField) && iso3code.length() == 2) {
                        Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(version);
                        if (lang != null) {
                            iso3code = lang.getIsoCode();
                        }
                    }
                    xmlListRecords.addContent(generateSingleDCRecord(doc, handler, iso3code, xmlns, nsOaiDoc));
                }
            }
        } else {
            for (SolrDocument doc : records) {
                xmlListRecords.addContent(generateSingleDCRecord(doc, handler, null, xmlns, nsOaiDoc));
                virtualHitCount++;
            }
        }

        // Create resumption token
        if (totalRawHits > firstRawRow + numRows) {
            Element resumption = createResumptionTokenAndElement(totalVirtualHits, totalRawHits, firstVirtualRow + virtualHitCount,
                    firstRawRow + numRows, xmlns, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }

    /**
     * 
     * @param doc
     * @param handler
     * @param requestedVersion
     * @param xmlns
     * @param nsOaiDoc
     * @return
     * @throws SolrServerException
     */
    private Element generateSingleDCRecord(SolrDocument doc, RequestHandler handler, String requestedVersion, Namespace xmlns, Namespace nsOaiDoc)
            throws SolrServerException {
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

        Element header = getHeader(doc, topstructDoc, handler, requestedVersion);
        record.addContent(header);

        //            Map<String, String> collectedValues = new HashMap<>();

        // create the metadata element, special for dc
        Element metadata = new Element("metadata", xmlns);

        // creating Element <oai_dc:dc ....> </oai_dc:dc>
        Element oai_dc = new Element("dc", nsOaiDoc);
        Namespace nsDc = Namespace.getNamespace(Metadata.dc.getMetadataNamespacePrefix(), Metadata.dc.getMetadataNamespaceUri());
        oai_dc.addNamespaceDeclaration(nsDc);
        oai_dc.addNamespaceDeclaration(XSI);
        oai_dc.setAttribute("schemaLocation", "http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd", XSI);

        // Configured fields
        List<FieldConfiguration> fieldConfigurations = DataManager.getInstance().getConfiguration().getFieldForMetadataFormat(Metadata.oai_dc.name());
        if (fieldConfigurations != null && !fieldConfigurations.isEmpty()) {
            for (FieldConfiguration fieldConfiguration : fieldConfigurations) {
                boolean added = false;
                String singleValue = null;
                // Alternative 1: get value from source
                if (StringUtils.isNotEmpty(fieldConfiguration.getValueSource())) {
                    if ("#AUTO#".equals(fieldConfiguration.getValueSource())) {
                        // #AUTO# means a hardcoded value is added
                        switch (fieldConfiguration.getFieldName()) {
                            case "identifier":
                                if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.URN))) {
                                    singleValue = DataManager.getInstance().getConfiguration().getUrnResolverUrl()
                                            + (String) doc.getFieldValue(SolrConstants.URN);
                                } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI))) {
                                    singleValue = DataManager.getInstance().getConfiguration().getPiResolverUrl()
                                            + (String) doc.getFieldValue(SolrConstants.PI);
                                } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))) {
                                    singleValue = DataManager.getInstance().getConfiguration().getPiResolverUrl()
                                            + (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
                                }
                                break;
                            case "rights":
                                if (doc.getFieldValues(SolrConstants.ACCESSCONDITION) != null) {
                                    for (Object o : doc.getFieldValues(SolrConstants.ACCESSCONDITION)) {
                                        if (SolrConstants.OPEN_ACCESS_VALUE.equals(o)) {
                                            singleValue = "info:eu-repo/semantics/openAccess";
                                            break;
                                        }
                                        singleValue = "info:eu-repo/semantics/closedAccess";
                                    }
                                }
                                break;
                            case "source":
                                oai_dc.addContent(generateDcSource(doc, topstructDoc, anchorDoc, nsDc));
                                break;
                            default:
                                singleValue = "No automatic configuration possible for field: " + fieldConfiguration.getFieldName();
                                break;
                        }
                    } else if (doc.getFieldValues(fieldConfiguration.getValueSource()) != null) {
                        if (fieldConfiguration.isMultivalued()) {
                            // Multivalued
                            StringBuilder sbAggregatedValue = new StringBuilder();
                            for (Object fieldValue : doc.getFieldValues(fieldConfiguration.getValueSource())) {
                                Element eleField = new Element(fieldConfiguration.getFieldName(), nsDc);
                                if (StringUtils.isNotEmpty(String.valueOf(fieldValue))) {
                                    StringBuilder sbValue = new StringBuilder();
                                    if (fieldConfiguration.getPrefix() != null) {
                                        sbValue.append(MessageResourceBundle.getTranslation(fieldConfiguration.getPrefix(),
                                                DataManager.getInstance().getConfiguration().getDefaultLocale()));
                                    }
                                    if (fieldConfiguration.isTranslate()) {
                                        sbValue.append(MessageResourceBundle.getTranslation(String.valueOf(fieldValue),
                                                DataManager.getInstance().getConfiguration().getDefaultLocale()));
                                    } else {
                                        sbValue.append(String.valueOf(fieldValue));
                                    }
                                    if (fieldConfiguration.getSuffix() != null) {
                                        sbValue.append(MessageResourceBundle.getTranslation(fieldConfiguration.getSuffix(),
                                                DataManager.getInstance().getConfiguration().getDefaultLocale()));
                                    }
                                    eleField.setText(sbValue.toString());
                                    oai_dc.addContent(eleField);
                                    added = true;
                                }
                            }

                        } else {
                            Element eleField = new Element(fieldConfiguration.getFieldName(), nsDc);
                            singleValue = String.valueOf(doc.getFieldValues(fieldConfiguration.getValueSource()).iterator().next());
                            // If no value found use the topstruct's value (if so configured)
                            if (fieldConfiguration.isUseTopstructValueIfNoneFound() && singleValue == null && topstructDoc != null
                                    && topstructDoc.getFieldValues(fieldConfiguration.getFieldName()) != null) {
                                singleValue = String.valueOf(topstructDoc.getFieldValues(fieldConfiguration.getFieldName()).iterator().next());
                            }
                        }
                    }
                }
                if (!added) {
                    // Alternative 2: get default value
                    if (singleValue == null && fieldConfiguration.getDefaultValue() != null) {
                        singleValue = fieldConfiguration.getDefaultValue();
                    }
                    switch (fieldConfiguration.getFieldName()) {
                        case "title":
                            if (isWork && doc.getFieldValue(SolrConstants.IDDOC_PARENT) != null) {
                                // If this is a volume, add anchor title in front
                                String anchorTitle = getAnchorTitle(doc);
                                if (anchorTitle != null) {
                                    singleValue = anchorTitle + "; " + singleValue;
                                }
                            }
                            break;
                    }

                    if (singleValue != null) {
                        Element eleField = new Element(fieldConfiguration.getFieldName(), nsDc);
                        if (fieldConfiguration.isTranslate()) {
                            eleField.setText(MessageResourceBundle.getTranslation(singleValue,
                                    DataManager.getInstance().getConfiguration().getDefaultLocale()));
                        } else {
                            StringBuilder sbValue = new StringBuilder();
                            if (fieldConfiguration.getPrefix() != null) {
                                sbValue.append(MessageResourceBundle.getTranslation(fieldConfiguration.getPrefix(),
                                        DataManager.getInstance().getConfiguration().getDefaultLocale()));
                            }
                            sbValue.append(singleValue);
                            if (fieldConfiguration.getSuffix() != null) {
                                sbValue.append(MessageResourceBundle.getTranslation(fieldConfiguration.getSuffix(),
                                        DataManager.getInstance().getConfiguration().getDefaultLocale()));
                            }
                            eleField.setText(sbValue.toString());
                        }
                        oai_dc.addContent(eleField);
                    }
                }
            }
        }

        metadata.addContent(oai_dc);
        record.addContent(metadata);

        return record;
    }

    /**
     * 
     * @param doc
     * @return
     */
    protected String getAnchorTitle(SolrDocument doc) {
        String iddocParent = (String) doc.getFieldValue(SolrConstants.IDDOC_PARENT);
        try {
            SolrDocumentList hits = solr.search(SolrConstants.IDDOC + ":" + iddocParent);
            if (hits != null && !hits.isEmpty()) {
                return (String) hits.get(0).getFirstValue(SolrConstants.TITLE);
            }
        } catch (SolrServerException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * @param doc
     * @param topstructDoc
     * @param anchorDoc
     * @param namespace
     * @return
     */
    protected static Element generateDcSource(SolrDocument doc, SolrDocument topstructDoc, SolrDocument anchorDoc, Namespace namespace) {
        if (topstructDoc == null) {
            logger.debug(doc.getFieldValueMap().toString());
            throw new IllegalArgumentException("topstructDoc may not be null");
        }

        Element dc_source = new Element("source", namespace);

        StringBuilder sbSourceCreators = new StringBuilder();
        if (doc != null && doc.getFieldValues("MD_CREATOR") != null) {
            for (Object fieldValue : doc.getFieldValues("MD_CREATOR")) {
                if (sbSourceCreators.length() > 0) {
                    sbSourceCreators.append(", ");
                }
                sbSourceCreators.append((String) fieldValue);
            }
        } else if (topstructDoc.getFieldValues("MD_CREATOR") != null) {
            for (Object fieldValue : topstructDoc.getFieldValues("MD_CREATOR")) {
                if (sbSourceCreators.length() > 0) {
                    sbSourceCreators.append(", ");
                }
                sbSourceCreators.append((String) fieldValue);
            }
        }
        if (sbSourceCreators.length() == 0) {
            sbSourceCreators.append('-');
        }

        StringBuilder sbSourceTitle = new StringBuilder();
        if (doc != null && doc.getFirstValue("MD_TITLE") != null) {
            sbSourceTitle.append((String) doc.getFirstValue("MD_TITLE"));
        }
        if (anchorDoc != null && anchorDoc.getFirstValue("MD_TITLE") != null) {
            if (sbSourceTitle.length() > 0) {
                sbSourceTitle.append("; ");
            }
            sbSourceTitle.append((String) anchorDoc.getFirstValue("MD_TITLE"));
        }
        if (sbSourceTitle.length() == 0) {
            sbSourceTitle.append('-');
        }
        if (topstructDoc != doc && topstructDoc.getFirstValue("MD_TITLE") != null) {
            if (sbSourceTitle.length() > 0) {
                sbSourceTitle.append("; ");
            }
            sbSourceTitle.append((String) topstructDoc.getFirstValue("MD_TITLE"));
        }

        // Publisher info
        String sourceYearpublish = (String) topstructDoc.getFirstValue("MD_YEARPUBLISH");
        if (sourceYearpublish == null) {
            sourceYearpublish = "-";
        }
        String sourcePlacepublish = (String) topstructDoc.getFirstValue("MD_PLACEPUBLISH");
        if (sourcePlacepublish == null) {
            sourcePlacepublish = "-";
        }
        String sourcePublisher = (String) topstructDoc.getFirstValue("MD_PUBLISHER");
        if (sourcePublisher == null) {
            sourcePublisher = "-";
        }

        // Page range
        String orderLabelFirst = null;
        String orderLabelLast = null;
        if (doc != null) {
            orderLabelFirst = (String) doc.getFirstValue(SolrConstants.ORDERLABELFIRST);
            orderLabelLast = (String) doc.getFirstValue(SolrConstants.ORDERLABELLAST);
        }

        StringBuilder sbSourceString = new StringBuilder();
        sbSourceString.append(sbSourceCreators.toString()).append(": ").append(sbSourceTitle.toString());

        if (doc == topstructDoc || doc == anchorDoc) {
            // Only top level docs should display publisher information
            sbSourceString.append(", ").append(sourcePlacepublish).append(": ").append(sourcePublisher).append(' ').append(sourceYearpublish);
            // sbSourceString.append('.');
        } else if (orderLabelFirst != null && orderLabelLast != null && !"-".equals(orderLabelFirst.trim()) && !"-".equals(orderLabelLast.trim())) {
            // Add page range for lower level docstructs, if available
            sbSourceString.append(", P ").append(orderLabelFirst).append(" - ").append(orderLabelLast);
        }

        sbSourceString.append('.');
        dc_source.setText(sbSourceString.toString());

        return dc_source;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#getTotalHits(java.util.Map, java.util.String)
     */
    @Override
    public long getTotalHits(Map<String, String> params, String versionDiscriminatorField) throws IOException, SolrServerException {
        String querySuffix = "";
        if (!Verb.ListIdentifiers.getTitle().equals(params.get("verb"))) {
            querySuffix +=
                    SolrSearchIndex.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes());
        }
        // Query Solr index for the total hits number

        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            // Query Solr index for the count of the discriminator field
            QueryResponse qr = solr.search(params.get("from"), params.get("until"), params.get("set"), params.get("metadataPrefix"), 0, 0, false,
                    querySuffix + " AND " + versionDiscriminatorField + ":*", Collections.singletonList(versionDiscriminatorField));
            return SolrSearchIndex.getFieldCount(qr, versionDiscriminatorField);
        }
        return solr.getTotalHitNumber(params, false, querySuffix, null);
    }

}
