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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.exceptions.HTTPException;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.enums.Verb;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.oai.model.language.Language;
import io.goobi.viewer.connector.oai.model.metadata.MetadataParameter;
import io.goobi.viewer.connector.oai.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.connector.utils.SolrConstants;
import io.goobi.viewer.connector.utils.SolrSearchTools;
import io.goobi.viewer.connector.utils.Utils;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * oai_dc
 */
public class OAIDCFormat extends Format {

    private static final Logger logger = LoggerFactory.getLogger(OAIDCFormat.class);
    
    protected static Map<String, String> anchorTitles = new HashMap<>();

    private List<String> setSpecFields = DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.oai_dc.name());

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createListRecords(io.goobi.viewer.connector.oai.RequestHandler, int, int, int, java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField,
            String filterQuerySuffix) throws SolrServerException, IOException {
        QueryResponse qr;
        long totalVirtualHits;
        long totalRawHits;
        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            // One OAI record for each record version
            qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, false,
                    SolrSearchTools.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes())
                            + " AND " + versionDiscriminatorField + ":*",
                    filterQuerySuffix, null, Collections.singletonList(versionDiscriminatorField));
            totalVirtualHits = SolrSearchTools.getFieldCount(qr, versionDiscriminatorField);
            totalRawHits = qr.getResults().getNumFound();
        } else {
            // One OAI record for each record proper
            qr = solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, false,
                    SolrSearchTools.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes()),
                    filterQuerySuffix, null, null);
            totalVirtualHits = totalRawHits = qr.getResults().getNumFound();

        }
        if (qr.getResults().isEmpty()) {
            logger.trace("Results are empty");
            return new ErrorCode().getNoRecordsMatch();
        }

        return generateDC(qr.getResults(), totalVirtualHits, totalRawHits, firstVirtualRow, firstRawRow, numRows, handler, "ListRecords",
                versionDiscriminatorField, null, filterQuerySuffix);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createGetRecord(io.goobi.viewer.connector.oai.RequestHandler, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public Element createGetRecord(RequestHandler handler, String filterQuerySuffix) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        String versionDiscriminatorField =
                DataManager.getInstance().getConfiguration().getVersionDisriminatorFieldForMetadataFormat(handler.getMetadataPrefix().name());
        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            String[] identifierSplit = Utils.splitIdentifierAndLanguageCode(handler.getIdentifier(), 3);
            try {
                SolrDocument doc = solr.getListRecord(identifierSplit[0], null, filterQuerySuffix);
                if (doc == null) {
                    return new ErrorCode().getIdDoesNotExist();
                }
                return generateDC(Collections.singletonList(doc), 1L, 1L, 0, 0, 1, handler, "GetRecord", versionDiscriminatorField,
                        identifierSplit[1], filterQuerySuffix);
            } catch (IOException e) {
                return new ErrorCode().getNoMetadataFormats();
            } catch (SolrServerException e) {
                return new ErrorCode().getNoMetadataFormats();
            }
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), null, filterQuerySuffix);
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            return generateDC(Collections.singletonList(doc), 1L, 1L, 0, 0, 1, handler, "GetRecord", null, null, filterQuerySuffix);
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
     * @param totalVirtualHits
     * @param totalRawHits
     * @param firstVirtualRow
     * @param firstRawRow
     * @param numRows
     * @param handler
     * @param recordType
     * @param versionDiscriminatorField
     * @param requestedVersion
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    private Element generateDC(List<SolrDocument> records, long totalVirtualHits, long totalRawHits, int firstVirtualRow, int firstRawRow,
            int numRows, RequestHandler handler, String recordType, String versionDiscriminatorField, String requestedVersion,
            String filterQuerySuffix) throws SolrServerException, IOException {
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
                    versions = SolrSearchTools.getMetadataValues(doc, versionDiscriminatorField);
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
                    xmlListRecords.addContent(generateSingleDCRecord(doc, handler, iso3code, xmlns, nsOaiDoc, setSpecFields, filterQuerySuffix));
                }
            }
        } else {
            for (SolrDocument doc : records) {
                xmlListRecords.addContent(generateSingleDCRecord(doc, handler, null, xmlns, nsOaiDoc, setSpecFields, filterQuerySuffix));
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
     * @param setSpecFields
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    private Element generateSingleDCRecord(SolrDocument doc, RequestHandler handler, String requestedVersion, Namespace xmlns, Namespace nsOaiDoc,
            List<String> setSpecFields, String filterQuerySuffix) throws SolrServerException, IOException {
        Element record = new Element("record", xmlns);
        boolean isWork = doc.getFieldValue(SolrConstants.ISWORK) != null && (boolean) doc.getFieldValue(SolrConstants.ISWORK);
        boolean isAnchor = doc.getFieldValue(SolrConstants.ISANCHOR) != null && (boolean) doc.getFieldValue(SolrConstants.ISANCHOR);
        boolean openAccess = true;
        Set<String> accessConditions = new HashSet<>();
        if (doc.getFieldValues(SolrConstants.ACCESSCONDITION) != null) {
            for (Object o : doc.getFieldValues(SolrConstants.ACCESSCONDITION)) {
                accessConditions.add((String) o);
                if (!SolrConstants.OPEN_ACCESS_VALUE.equals(o)) {
                    openAccess = false;
                }
            }
        }
        SolrDocument topstructDoc = null;
        if (isWork || isAnchor) {
            topstructDoc = doc;
        } else {
            // If child element metadata fields are empty, get certain values from topstruct
            String iddocTopstruct = (String) doc.getFieldValue(SolrConstants.IDDOC_TOPSTRUCT);
            SolrDocumentList docList = solr.search("+" + SolrConstants.IDDOC + ":" + iddocTopstruct, filterQuerySuffix);
            if (docList != null && !docList.isEmpty()) {
                topstructDoc = docList.get(0);
            }
        }
        if (topstructDoc == null && !doc.containsKey(SolrConstants.DATEDELETED)) {
            logger.warn("No topstruct found for IDDOC:{} - is this a page document? Please check the base query.",
                    doc.getFieldValue(SolrConstants.IDDOC));
        }
        SolrDocument anchorDoc = null;
        if (!isAnchor) {
            SolrDocument childDoc = topstructDoc != null ? topstructDoc : doc;
            String iddocAnchor = (String) childDoc.getFieldValue(SolrConstants.IDDOC_PARENT);
            if (iddocAnchor != null) {
                SolrDocumentList docList = solr.search("+" + SolrConstants.IDDOC + ":" + iddocAnchor, filterQuerySuffix);
                if (docList != null && !docList.isEmpty()) {
                    anchorDoc = docList.get(0);
                }
            }
        }
        String docstruct = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);

        Element header = getHeader(doc, topstructDoc, handler, requestedVersion, setSpecFields, filterQuerySuffix);
        record.addContent(header);

        if ("deleted".equals(header.getAttributeValue("status"))) {
            return record;
        }

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
        List<io.goobi.viewer.connector.oai.model.metadata.Metadata> metadataList =
                DataManager.getInstance().getConfiguration().getMetadataConfiguration(Metadata.oai_dc.name(), docstruct);
        if (metadataList != null && !metadataList.isEmpty()) {
            for (io.goobi.viewer.connector.oai.model.metadata.Metadata md : metadataList) {
                boolean restrictedContent = false;
                List<String> finishedValues = new ArrayList<>();

                // Alternative 1: get value from source
                if ("#AUTO#".equals(md.getMasterValue())) {
                    // #AUTO# means a hardcoded value is added
                    String val = "";
                    switch (md.getLabel()) {
                        case "identifier":
                            if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.URN))) {
                                val = DataManager.getInstance().getConfiguration().getUrnResolverUrl()
                                        + (String) doc.getFieldValue(SolrConstants.URN);
                            } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI))) {
                                val = DataManager.getInstance().getConfiguration().getPiResolverUrl() + (String) doc.getFieldValue(SolrConstants.PI);
                            } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))) {
                                val = DataManager.getInstance().getConfiguration().getPiResolverUrl()
                                        + (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
                            }
                            finishedValues.add(val);
                            break;
                        case "rights":
                            if (openAccess) {
                                val = ACCESSCONDITION_OPENACCESS;
                            } else {
                                for (String accessCondition : accessConditions) {
                                    val = DataManager.getInstance()
                                            .getConfiguration()
                                            .getAccessConditionMappingForMetadataFormat(Metadata.oai_dc.name(), accessCondition);
                                }
                                if (StringUtils.isEmpty(val)) {
                                    val = ACCESSCONDITION_CLOSEDACCESS;
                                }
                            }
                            finishedValues.add(val);
                            break;
                        case "source":
                            if (topstructDoc == null) {
                                logger.warn("No topstruct found for IDDOC:{} - is this a page document? Please check the base query.",
                                        doc.getFieldValue(SolrConstants.IDDOC));
                                continue;
                            }
                            oai_dc.addContent(generateDcSource(doc, topstructDoc, anchorDoc, nsDc));
                            break;
                        case "fulltext":
                            if (topstructDoc == null) {
                                logger.warn("No topstruct found for IDDOC:{} - is this a page document? Please check the base query.",
                                        doc.getFieldValue(SolrConstants.IDDOC));
                                continue;
                            }
                            for (Element oai_fulltext : generateFulltextUrls((String) topstructDoc.getFieldValue(SolrConstants.PI_TOPSTRUCT),
                                    nsDc)) {
                                oai_dc.addContent(oai_fulltext);
                            }
                            break;
                        default:
                            val = "No automatic configuration possible for field: " + md.getLabel();
                            finishedValues.add(val);
                            break;
                    }
                } else if ("#TOC#".equals(md.getMasterValue())) {
                    // Generated TOC as plain text
                    //                    if (!openAccess) {
                    //                        continue;
                    //                    }

                    String url = DataManager.getInstance().getConfiguration().getRestApiUrl() + "records/"
                            + (String) doc.getFieldValue(SolrConstants.PI) + "/toc/";
                    try {
                        String val = null;
                        try {
                            val = Utils.getWebContentGET(url);
                        } catch (HTTPException e) {
                            // If the API end point was not found, try the fallback, otherwise re-throw the exception
                            if (e.getCode() != 404) {
                                throw e;
                            }
                        }
                        if (StringUtils.isEmpty(val)) {
                            // Old API fallback
                            url = DataManager.getInstance().getConfiguration().getRestApiUrl() + "records/toc/"
                                    + (String) doc.getFieldValue(SolrConstants.PI) + "/";
                            val = Utils.getWebContentGET(url);
                        }

                        if (StringUtils.isNotEmpty(val)) {
                            finishedValues.add(val);
                        }
                    } catch (ClientProtocolException e) {
                        logger.error(e.getMessage(), e);
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    } catch (HTTPException e) {
                        logger.error(e.getCode() + ": " + url);
                    }
                } else if (!md.getParams().isEmpty()) {
                    // Parameter configuration
                    String firstField = md.getParams().get(0).getKey();
                    int numValues = SolrSearchTools.getMetadataValues(doc, firstField).size();
                    // for each instance of the first field value
                    for (int i = 0; i < numValues; ++i) {
                        String val = md.getMasterValue();
                        int paramIndex = 0;
                        // for each parameter
                        for (MetadataParameter param : md.getParams()) {
                            if (SolrConstants.THUMBNAIL.equals(param.getKey())) {
                                restrictedContent = true;
                            }
                            String paramVal = "";
                            List<String> values = SolrSearchTools.getMetadataValues(doc, param.getKey());
                            if (values.isEmpty() && !param.isDontUseTopstructValue()) {
                                values = SolrSearchTools.getMetadataValues(topstructDoc, param.getKey());
                            }
                            if (!values.isEmpty()) {
                                paramVal = values.size() > i ? values.get(i) : "";
                                if (StringUtils.isNotEmpty(paramVal)) {
                                    if (MetadataParameterType.TRANSLATEDFIELD.equals(param.getType())) {
                                        paramVal = ViewerResourceBundle.getTranslation(paramVal, null);
                                    }
                                    if (StringUtils.isNotEmpty(param.getPrefix())) {
                                        String prefix = ViewerResourceBundle.getTranslation(param.getPrefix(), null);
                                        paramVal = prefix + paramVal;
                                    }
                                    if (StringUtils.isNotEmpty(param.getSuffix())) {
                                        String suffix = ViewerResourceBundle.getTranslation(param.getSuffix(), null);
                                        paramVal += suffix;
                                    }
                                }
                            }
                            val = val.replace("{" + paramIndex + '}', paramVal);
                            paramIndex++;
                        }
                        if (openAccess || !restrictedContent) {
                            finishedValues.add(val);
                        }
                        if (!md.isMultivalued()) {
                            continue;
                        }
                    }
                } else if (StringUtils.isNotEmpty(md.getMasterValue())) {
                    // Default value
                    String val = md.getMasterValue();
                    if ("title".equals(md.getLabel()) && isWork && doc.getFieldValue(SolrConstants.IDDOC_PARENT) != null) {
                        // If this is a volume, add anchor title in front
                        String iddocParent = (String) doc.getFieldValue(SolrConstants.IDDOC_PARENT);
                        String anchorTitle = anchorTitles.get(iddocParent);
                        if (anchorTitle == null) {
                            anchorTitle = getAnchorTitle(iddocParent, filterQuerySuffix);
                            if (anchorTitle != null) {
                                val = anchorTitle + "; " + val;
                                anchorTitles.put(iddocParent, anchorTitle);
                            }
                        }
                    }
                    finishedValues.add(val);
                }

                // Add all constructed values for the current field to XML
                for (String val : finishedValues) {
                    Element eleField = new Element(md.getLabel(), nsDc);
                    eleField.setText(val);
                    oai_dc.addContent(eleField);
                }
            }
        }

        metadata.addContent(oai_dc);
        record.addContent(metadata);

        return record;

    }

    /**
     * <p>
     * getAnchorTitle.
     * </p>
     *
     * @param iddocParent
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return a {@link java.lang.String} object.
     */
    protected String getAnchorTitle(String iddocParent, String filterQuerySuffix) {
        try {
            logger.trace("anchor title query: {}", SolrConstants.IDDOC + ":" + iddocParent);
            SolrDocumentList hits = solr.search("+" + SolrConstants.IDDOC + ":" + iddocParent, filterQuerySuffix);
            if (hits != null && !hits.isEmpty()) {
                return (String) hits.get(0).getFirstValue(SolrConstants.TITLE);
            }
        } catch (SolrServerException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * <p>
     * generateDcSource.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param topstructDoc a {@link org.apache.solr.common.SolrDocument} object.
     * @param anchorDoc a {@link org.apache.solr.common.SolrDocument} object.
     * @param namespace a {@link org.jdom2.Namespace} object.
     * @return a {@link org.jdom2.Element} object.
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

    /**
     * <p>
     * generateFulltextUrls.
     * </p>
     *
     * @param namespace a {@link org.jdom2.Namespace} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws IOException
     */
    protected static List<Element> generateFulltextUrls(String pi, Namespace namespace)
            throws SolrServerException, IOException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        Map<Integer, String> fulltextFilePaths = DataManager.getInstance().getSearchIndex().getFulltextFileNames(pi);
        if (fulltextFilePaths.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> orderedPageList = new ArrayList<>(fulltextFilePaths.keySet());
        Collections.sort(orderedPageList);

        String urlTemplate = DataManager.getInstance().getConfiguration().getFulltextUrl().replace("{pi}", pi);
        List<Element> ret = new ArrayList<>(orderedPageList.size());
        for (int i : orderedPageList) {
            String filePath = fulltextFilePaths.get(i); // file path relative to the data repository (Goobi viewer 3.2 and later)
            if (filePath == null) {
                continue;
            }
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1); // pure file name
            String url = urlTemplate.replace("{page}", String.valueOf(i)).replace("{fileName}", fileName);
            Element dc_fulltext = new Element("source", namespace);
            dc_fulltext.setText(url);
            ret.add(dc_fulltext);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#getTotalHits(java.util.Map, java.util.String, java.util.String)
     */
    /** {@inheritDoc} */
    @Override
    public long getTotalHits(Map<String, String> params, String versionDiscriminatorField, String filterQuerySuffix)
            throws IOException, SolrServerException {
        String additionalQuery = "";
        if (!Verb.ListIdentifiers.getTitle().equals(params.get("verb"))) {
            additionalQuery +=
                    SolrSearchTools.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes());
        }
        // Query Solr index for the total hits number

        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            // Query Solr index for the count of the discriminator field
            QueryResponse qr = solr.search(params.get("from"), params.get("until"), params.get("set"), params.get("metadataPrefix"), 0, 0, false,
                    additionalQuery + " AND " + versionDiscriminatorField + ":*", filterQuerySuffix, null,
                    Collections.singletonList(versionDiscriminatorField));
            return SolrSearchTools.getFieldCount(qr, versionDiscriminatorField);
        }
        return solr.getTotalHitNumber(params, false, additionalQuery, null, filterQuerySuffix);
    }

}
