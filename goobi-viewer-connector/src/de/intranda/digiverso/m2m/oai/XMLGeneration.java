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
package de.intranda.digiverso.m2m.oai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.transform.XSLTransformException;
import org.jdom2.transform.XSLTransformer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;

import de.intranda.digiverso.m2m.DataManager;
import de.intranda.digiverso.m2m.messages.MessageResourceBundle;
import de.intranda.digiverso.m2m.oai.enums.Metadata;
import de.intranda.digiverso.m2m.oai.enums.Verb;
import de.intranda.digiverso.m2m.oai.model.ErrorCode;
import de.intranda.digiverso.m2m.oai.model.FieldConfiguration;
import de.intranda.digiverso.m2m.oai.model.ResumptionToken;
import de.intranda.digiverso.m2m.oai.model.Set;
import de.intranda.digiverso.m2m.utils.SolrConstants;
import de.intranda.digiverso.m2m.utils.SolrSearchIndex;
import de.intranda.digiverso.m2m.utils.Utils;

public class XMLGeneration {

    private final static Logger logger = LoggerFactory.getLogger(XMLGeneration.class);
    private SolrSearchIndex solr;
    private long expiration = 259200000L; // 3 days

    public XMLGeneration() {
        solr = DataManager.getInstance().getSearchIndex();
    }

    /**
     * creates root element for oai protocol
     * 
     * @param elementName
     * @return
     */
    public Element getOaiPmhElement(String elementName) {
        Element oaiPmh = new Element(elementName);

        oaiPmh.setNamespace(Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/"));
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        oaiPmh.addNamespaceDeclaration(xsi);
        oaiPmh.setAttribute("schemaLocation", "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", xsi);
        return oaiPmh;
    }

    /**
     * for the server request ?verb=Identify this method build the xml section in the Identify element
     * 
     * @return the identify Element for the xml tree
     * @throws SolrServerException
     */
    public Element getIdentifyXML() throws SolrServerException {
        // TODO: optional parameter: compression is not implemented
        // TODO: optional parameter: description is not implemented
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Map<String, String> identifyTags = DataManager.getInstance().getConfiguration().getIdentifyTags();
        Element identify = new Element("Identify", xmlns);

        Element repositoryName = new Element("repositoryName", xmlns);
        repositoryName.setText(identifyTags.get("repositoryName"));
        identify.addContent(repositoryName);

        Element baseURL = new Element("baseURL", xmlns);
        baseURL.setText(identifyTags.get("baseURL"));
        identify.addContent(baseURL);

        Element protocolVersion = new Element("protocolVersion", xmlns);
        protocolVersion.setText(identifyTags.get("protocolVersion"));
        identify.addContent(protocolVersion);

        // TODO: protocol definition allow more than one email adress, change away from HashMap
        Element adminEmail = new Element("adminEmail", xmlns);
        adminEmail.setText(identifyTags.get("adminEmail")); //
        identify.addContent(adminEmail);

        Element earliestDatestamp = new Element("earliestDatestamp", xmlns);
        earliestDatestamp.setText(solr.getEarliestRecordDatestamp());
        identify.addContent(earliestDatestamp);

        Element deletedRecord = new Element("deletedRecord", xmlns);
        deletedRecord.setText(identifyTags.get("deletedRecord"));
        identify.addContent(deletedRecord);

        Element granularity = new Element("granularity", xmlns);
        granularity.setText(identifyTags.get("granularity"));
        identify.addContent(granularity);

        return identify;
    }

    /**
     * for the server request ?verb=ListIdentifiers this method build the xml section
     * 
     * @param handler
     * @param firstRow
     * @param numRows
     * @return
     * @throws SolrServerException
     */
    public Element createListIdentifiers(RequestHandler handler, int firstRow, int numRows) throws SolrServerException {
        Map<String, String> datestamp = filterDatestampFromRequest(handler);
        SolrDocumentList listIdentifiers = solr.getListIdentifiers(datestamp, firstRow, numRows, false);
        if (listIdentifiers == null || listIdentifiers.isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }

        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListIdentifiers = new Element("ListIdentifiers", xmlns);
        long totalHits = listIdentifiers.getNumFound();
        for (int i = 0; i < listIdentifiers.size(); i++) {
            SolrDocument doc = listIdentifiers.get(i);
            Element header = getHeader(doc, null, handler);
            xmlListIdentifiers.addContent(header);
        }

        // Create resumption token
        if (totalHits > firstRow + numRows) {
            Element resumption = createResumptionTokenAndElement(totalHits, firstRow + numRows, xmlns, handler);
            xmlListIdentifiers.addContent(resumption);
        }

        return xmlListIdentifiers;
    }

    /**
     * for the server request ?verb=ListRecords this method build the xml section if metadataPrefix is oai_dc
     * 
     * @param handler
     * @param firstRow
     * @param numRows
     * @return
     * @throws SolrServerException
     */
    public Element createListRecordsDC(RequestHandler handler, int firstRow, int numRows) throws SolrServerException {
        SolrDocumentList records = solr.getListRecords(filterDatestampFromRequest(handler), firstRow, numRows, false,
                getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes()));
        if (records.isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        return generateDC(records, records.getNumFound(), firstRow, numRows, handler, "ListRecords");
    }

    /**
     * for the server request ?verb=ListRecords this method build the xml section if metadataPrefix is oai_dc
     * 
     * @param handler
     * @param firstRow
     * @param numRows
     * @return
     * @throws SolrServerException
     */
    public Element createListRecordsESE(RequestHandler handler, int firstRow, int numRows) throws SolrServerException {
        SolrDocumentList records = solr.getListRecords(filterDatestampFromRequest(handler), firstRow, numRows, false,
                getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes()));
        if (records.isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        return generateESE(records, records.getNumFound(), firstRow, numRows, handler, "ListRecords");
    }

    /**
     * for the server request ?verb=ListSets this method build the xml section
     * 
     * @param handler
     * @return
     * @throws SolrServerException
     */
    public Element createListSets(Locale locale) throws SolrServerException {
        // Add all values sets (a set for each existing field value)
        List<Set> allValuesSetConfigurations = DataManager.getInstance().getConfiguration().getAllValuesSets();
        if (allValuesSetConfigurations != null && !allValuesSetConfigurations.isEmpty()) {
            for (Set set : allValuesSetConfigurations) {
                set.getValues().addAll(solr.getSets(set.getSetName()));
            }
        }

        boolean empty = true;
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element listSets = new Element("ListSets", xmlns);
        for (Set set : allValuesSetConfigurations) {
            if (set.getValues().isEmpty()) {
                continue;
            }
            for (String value : set.getValues()) {

                Element eleSet = new Element("set", xmlns);
                // TODO
                Element eleSetSpec = new Element("setSpec", xmlns);
                eleSetSpec.setText(value);
                eleSet.addContent(eleSetSpec);
                Element name = new Element("setName", xmlns);
                if (set.isTranslate()) {
                    name.setText(MessageResourceBundle.getTranslation(value, locale));
                } else {
                    name.setText(value);
                }
                eleSet.addContent(name);
                listSets.addContent(eleSet);
                empty = false;
            }
        }
        List<Set> additionalSets = DataManager.getInstance().getConfiguration().getAdditionalSets();
        if (additionalSets != null && !additionalSets.isEmpty()) {
            for (Set additionalSet : additionalSets) {
                Element set = new Element("set", xmlns);
                // TODO
                Element setSpec = new Element("setSpec", xmlns);
                setSpec.setText(additionalSet.getSetSpec());
                set.addContent(setSpec);
                Element name = new Element("setName", xmlns);
                name.setText(additionalSet.getSetName());
                set.addContent(name);
                listSets.addContent(set);
                empty = false;
            }
        }
        if (empty) {
            return new ErrorCode().getNoSetHierarchy();
        }

        return listSets;
    }

    /**
     * for the server request ?verb=ListMetadataFormats this method build the xml section
     * 
     * @return
     */
    public Element createMetadataFormats() {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        if (Metadata.values().length == 0) {
            return new ErrorCode().getNoMetadataFormats();
        }
        Element listMetadataFormats = new Element("ListMetadataFormats", xmlns);
        for (Metadata m : Metadata.values()) {
            // logger.trace("{}: {}", m.getMetadataPrefix(), DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(m.name()));
            if (m.isOaiSet() && DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(m.name())) {
                Element metadataFormat = new Element("metadataFormat", xmlns);
                Element metadataPrefix = new Element("metadataPrefix", xmlns);
                metadataPrefix.setText(m.getMetadataPrefix());
                Element schema = new Element("schema", xmlns);
                schema.setText(m.getSchema());
                Element metadataNamespace = new Element("metadataNamespace", xmlns);
                metadataNamespace.setText(m.getMetadataNamespace());
                metadataFormat.addContent(metadataPrefix);
                metadataFormat.addContent(schema);
                metadataFormat.addContent(metadataNamespace);
                listMetadataFormats.addContent(metadataFormat);
            }
        }
        return listMetadataFormats;
    }

    /**
     * TODO Method is obsolete.
     * 
     * @param string
     * @return
     */
    private static String format(String string) {

        return string;
    }

    /**
     * create the header for listIdentifiers and ListRecords, because there are both the same
     * 
     * @param doc Document from which to extract values.
     * @param topstructDoc If not null, the datestamp value will be determined from this instead.
     * @return
     * @throws SolrServerException
     */
    private Element getHeader(SolrDocument doc, SolrDocument topstructDoc, RequestHandler handler) throws SolrServerException {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element header = new Element("header", xmlns);
        // identifier
        if (doc.getFieldValue(SolrConstants.URN) != null && ((String) doc.getFieldValue(SolrConstants.URN)).length() > 0) {
            Element urn_identifier = new Element("identifier", xmlns);
            urn_identifier.setText(DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier") + (String) doc
                    .getFieldValue(SolrConstants.URN));
            header.addContent(urn_identifier);
        } else {
            Element identifier = new Element("identifier", xmlns);
            identifier.setText(DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier") + (String) doc
                    .getFieldValue(SolrConstants.PI));
            header.addContent(identifier);
        }
        // datestamp
        Element datestamp = new Element("datestamp", xmlns);
        long untilTimestamp = RequestHandler.getUntilTimestamp(handler.getUntil());
        long timestampModified = SolrSearchIndex.getLatestValidDateUpdated(topstructDoc != null ? topstructDoc : doc, untilTimestamp);
        datestamp.setText(parseDate(timestampModified));
        if (StringUtils.isEmpty(datestamp.getText()) && doc.getFieldValue(SolrConstants.ISANCHOR) != null) {
            datestamp.setText(parseDate(solr.getLatestVolumeTimestamp(doc, untilTimestamp)));
        }
        header.addContent(datestamp);
        // setSpec
        List<String> setSpecFields = DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(handler.getMetadataPrefix()
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

    private static String parseDate(Object datestring) {
        if (datestring instanceof Long) {
            return Utils.convertDate((Long) datestring);
        }
        return "";
    }

    /**
     * handle token
     * 
     * @param resumptionToken
     * @return
     */
    public Element handleToken(String resumptionToken) {
        logger.debug("Loading resumption token {}", resumptionToken);
        File f = new File(DataManager.getInstance().getConfiguration().getResumptionTokenFolder(), resumptionToken);
        if (!f.exists()) {
            logger.warn("Requested resumption token not found: {}", f.getName());
            return new ErrorCode().getBadResumptionToken();
        }

        try (FileInputStream fis = new FileInputStream(f); InputStreamReader isr = new InputStreamReader(fis); BufferedReader infile =
                new BufferedReader(isr);) {
            XStream xStream = new XStream(new DomDriver());
            xStream.processAnnotations(ResumptionToken.class);
            ResumptionToken token = (ResumptionToken) xStream.fromXML(infile);
            RequestHandler oldHandler = token.getHandler();
            Map<String, String> params = filterDatestampFromRequest(oldHandler);

            boolean urnOnly = false;
            String querySuffix = "";
            long totalHits = 0;

            switch (oldHandler.getMetadataPrefix()) {
                // Query the intranda viewer for the total hits number
                case iv_crowdsourcing:
                case iv_overviewpage:
                    String url = DataManager.getInstance().getConfiguration().getHarvestUrl() + "?action=getlist_" + oldHandler.getMetadataPrefix()
                            .name().substring(3);
                    String rawJSON = Utils.getWebContent(url);
                    if (StringUtils.isNotEmpty(rawJSON)) {
                        try {
                            JSONArray jsonArray = (JSONArray) new JSONParser().parse(rawJSON);
                            totalHits = (long) jsonArray.get(0);
                        } catch (ParseException e) {
                            logger.error(e.getMessage());
                            throw new IOException(e.getMessage());
                        }
                    }
                    break;
                case epicur:
                    // Hit count may differ for epicur
                    urnOnly = true;
                    querySuffix += getUrnPrefixBlacklistSuffix(DataManager.getInstance().getConfiguration().getUrnPrefixBlacklist());
                case oai_dc:
                case ese:
                    if (!Verb.ListIdentifiers.equals(token.getHandler().getVerb())) {
                        querySuffix += getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes());
                    }
                    // Query Solr index for the total hits number
                    totalHits = solr.getTotalHitNumber(params, urnOnly, querySuffix);
                    break;
                default:
                    totalHits = solr.getTotalHitNumber(params, urnOnly, querySuffix);
                    break;
            }

            if (token.getHits() != totalHits) {
                logger.warn("Hits size in the token ({}) does not equal the reported total hits number ({}).", token.getHits(), totalHits);
                return new ErrorCode().getBadResumptionToken();
            }
            int hitsPerToken = DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(oldHandler.getMetadataPrefix().name());
            if (token.getHandler().getVerb().equals(Verb.ListIdentifiers)) {
                return createListIdentifiers(oldHandler, token.getCursor(), hitsPerToken);
            } else if (token.getHandler().getVerb().equals(Verb.ListRecords)) {
                Metadata md = token.getHandler().getMetadataPrefix();
                switch (md) {
                    case oai_dc:
                        return createListRecordsDC(token.getHandler(), token.getCursor(), hitsPerToken);
                    case ese:
                        return createListRecordsESE(token.getHandler(), token.getCursor(), hitsPerToken);
                    case epicur:
                        return createListRecordsEpicur(token.getHandler(), token.getCursor(), hitsPerToken, "ListRecords");
                    case mets:
                        return createListRecordsMets(token.getHandler(), token.getCursor(), hitsPerToken);
                    case marcxml:
                        return createListRecordsMarc(token.getHandler(), token.getCursor(), hitsPerToken);
                    case lido:
                        return createListRecordsLido(token.getHandler(), token.getCursor(), hitsPerToken);
                    case iv_overviewpage:
                    case iv_crowdsourcing:
                        return createListRecordsIntrandaViewerUpdates(token.getHandler(), token.getCursor(), hitsPerToken, md);
                    default:
                        return new ErrorCode().getCannotDisseminateFormat();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return new ErrorCode().getBadResumptionToken();
    }

    public void removeExpiredTokens() {
        File tokenFolder = new File(DataManager.getInstance().getConfiguration().getResumptionTokenFolder());
        if (tokenFolder.isDirectory()) {
            int count = 0;
            XStream xStream = new XStream(new DomDriver());
            for (File tokenFile : tokenFolder.listFiles()) {
                try (FileInputStream fis = new FileInputStream(tokenFile)) {
                    ResumptionToken token = (ResumptionToken) xStream.fromXML(fis);
                    if (token.hasExpired() && FileUtils.deleteQuietly(tokenFile)) {
                        count++;
                    }
                } catch (StreamException | ConversionException e) {
                    // File cannot be de-serialized, so just delete it
                    logger.warn("Token '{}' could not be read, deleting...", tokenFile.getName());
                    if (FileUtils.deleteQuietly(tokenFile)) {
                        count++;
                    }
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (count > 0) {
                logger.info("{} expired resumption token(s) removed.", count);
            }
        }
    }

    // save token in folder
    private static void saveToken(ResumptionToken token) throws IOException {
        File f = new File(DataManager.getInstance().getConfiguration().getResumptionTokenFolder(), token.getTokenName());
        XStream xStream = new XStream(new DomDriver());
        try (BufferedWriter outfile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF8"))) {
            xStream.toXML(token, outfile);
            outfile.flush();
        }
    }

    /**
     * generates timestamp object from request
     * 
     * @param the request that was send to the server(servlet)
     * @return a HashMap with the values from, until and set as string
     */
    private static Map<String, String> filterDatestampFromRequest(RequestHandler request) {
        Map<String, String> datestamp = new HashMap<>();

        String from = null;
        if (request.getFrom() != null) {
            from = request.getFrom();
            from = from.replace("-", "").replace("T", "").replace(":", "").replace("Z", "");
            datestamp.put("from", from);
        }
        String until = null;
        if (request.getUntil() != null) {
            until = request.getUntil();
            until = until.replace("-", "").replace("T", "").replace(":", "").replace("Z", "");
            datestamp.put("until", until);
        }

        String set = null;
        if (request.getSet() != null) {
            set = request.getSet();
            datestamp.put("set", set);
        }

        if (request.getMetadataPrefix() != null) {
            datestamp.put("metadataPrefix", request.getMetadataPrefix().getMetadataPrefix());
        }

        return datestamp;
    }

    /**
     * for the server request ?verb=GetRecord this method build the xml section if metadataPrefix is oai_dc
     * 
     * @param handler
     * @return
     */
    public Element createGetRecordDC(RequestHandler handler) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), Metadata.oai_dc.name());
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            return generateDC(Collections.singletonList(doc), 1L, 0, 1, handler, "GetRecord");
        } catch (IOException e) {
            return new ErrorCode().getNoMetadataFormats();
        } catch (SolrServerException e) {
            return new ErrorCode().getNoMetadataFormats();
        }
    }

    /**
     * 
     * @param doc
     * @return
     */
    private String getAnchorTitle(SolrDocument doc) {
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
    private static Element generateDcSource(SolrDocument doc, SolrDocument topstructDoc, SolrDocument anchorDoc, Namespace namespace) {
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
        dc_source.setText(format(sbSourceString.toString()));

        return dc_source;
    }

    /**
     * generates oai_dc records
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
    private Element generateDC(List<SolrDocument> records, long totalHits, int firstRow, int numRows, RequestHandler handler, String recordType)
            throws SolrServerException {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Namespace nsOaiDoc = Namespace.getNamespace(Metadata.oai_dc.getMetadataPrefix(), Metadata.oai_dc.getMetadataNamespace());
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

            //            Map<String, String> collectedValues = new HashMap<>();

            // create the metadata element, special for dc
            Element metadata = new Element("metadata", xmlns);

            // creating Element <oai_dc:dc ....> </oai_dc:dc>
            Element oai_dc = new Element("dc", nsOaiDoc);
            Namespace nsDc = Namespace.getNamespace(Metadata.dc.getMetadataPrefix(), Metadata.dc.getMetadataNamespace());
            oai_dc.addNamespaceDeclaration(nsDc);
            Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            oai_dc.addNamespaceDeclaration(xsi);
            oai_dc.setAttribute("schemaLocation", "http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd", xsi);

            // Configured fields
            List<FieldConfiguration> fieldConfigurations = DataManager.getInstance().getConfiguration().getFieldForMetadataFormat(Metadata.oai_dc
                    .name());
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
                                        singleValue = DataManager.getInstance().getConfiguration().getUrnResolverUrl() + (String) doc.getFieldValue(
                                                SolrConstants.URN);
                                    } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI))) {
                                        singleValue = DataManager.getInstance().getConfiguration().getPiResolverUrl() + (String) doc.getFieldValue(
                                                SolrConstants.PI);
                                    } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))) {
                                        singleValue = DataManager.getInstance().getConfiguration().getPiResolverUrl() + (String) doc.getFieldValue(
                                                SolrConstants.PI_TOPSTRUCT);
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
                                            sbValue.append(MessageResourceBundle.getTranslation(fieldConfiguration.getPrefix(), DataManager
                                                    .getInstance().getConfiguration().getDefaultLocale()));
                                        }
                                        if (fieldConfiguration.isTranslate()) {
                                            sbValue.append(MessageResourceBundle.getTranslation(String.valueOf(fieldValue), DataManager.getInstance()
                                                    .getConfiguration().getDefaultLocale()));
                                        } else {
                                            sbValue.append(String.valueOf(fieldValue));
                                        }
                                        if (fieldConfiguration.getSuffix() != null) {
                                            sbValue.append(MessageResourceBundle.getTranslation(fieldConfiguration.getSuffix(), DataManager
                                                    .getInstance().getConfiguration().getDefaultLocale()));
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
                                if (fieldConfiguration.isUseTopstructValueIfNoneFound() && singleValue == null && topstructDoc != null && topstructDoc
                                        .getFieldValues(fieldConfiguration.getFieldName()) != null) {
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
                                eleField.setText(MessageResourceBundle.getTranslation(singleValue, DataManager.getInstance().getConfiguration()
                                        .getDefaultLocale()));
                            } else {
                                StringBuilder sbValue = new StringBuilder();
                                if (fieldConfiguration.getPrefix() != null) {
                                    sbValue.append(MessageResourceBundle.getTranslation(fieldConfiguration.getPrefix(), DataManager.getInstance()
                                            .getConfiguration().getDefaultLocale()));
                                }
                                sbValue.append(singleValue);
                                if (fieldConfiguration.getSuffix() != null) {
                                    sbValue.append(MessageResourceBundle.getTranslation(fieldConfiguration.getSuffix(), DataManager.getInstance()
                                            .getConfiguration().getDefaultLocale()));
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
            xmlListRecords.addContent(record);
        }

        // Create resumption token
        if (totalHits > firstRow + numRows) {
            Element resumption = createResumptionTokenAndElement(totalHits, firstRow + numRows, xmlns, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }

    /**
     * for the server request ?verb=GetRecord this method build the xml section if metadataPrefix is europeana
     * 
     * @param handler
     * @return
     */
    public Element createGetRecordESE(RequestHandler handler) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), Metadata.ese.name());
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
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
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
                eleDcIdentifier.setText(DataManager.getInstance().getConfiguration().getUrnResolverUrl() + urn);
                eleEuropeanaRecord.addContent(eleDcIdentifier);
            } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI))) {
                Element eleDcIdentifier = new Element("identifier", nsDc);
                identifier = (String) doc.getFieldValue(SolrConstants.PI);
                eleDcIdentifier.setText(DataManager.getInstance().getConfiguration().getPiResolverUrl() + format(identifier));
                eleEuropeanaRecord.addContent(eleDcIdentifier);
            } else if (StringUtils.isNotEmpty((String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))) {
                Element eleDcIdentifier = new Element("identifier", nsDc);
                identifier = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
                eleDcIdentifier.setText(DataManager.getInstance().getConfiguration().getPiResolverUrl() + format(identifier));
                eleEuropeanaRecord.addContent(eleDcIdentifier);
            }
            // <dc:language>
            {
                Element eleDcLanguage = new Element("language", nsDc);
                String language = "";
                if (doc.getFieldValues("MD_LANGUAGE") != null) {
                    language = (String) doc.getFieldValues("MD_LANGUAGE").iterator().next();
                    eleDcLanguage.setText(format(language));
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
                    title = (String) doc.getFieldValues(SolrConstants.TITLE).iterator().next();
                    // logger.debug("MD_TITLE : " + title);
                }
                if (isWork && doc.getFieldValue(SolrConstants.IDDOC_PARENT) != null) {
                    // If this is a volume, add anchor title in front
                    String anchorTitle = getAnchorTitle(doc);
                    if (anchorTitle != null) {
                        title = anchorTitle + "; " + title;
                    }
                }
                dc_title.setText(format(title));
                eleEuropeanaRecord.addContent(dc_title);
            }
            // <dc:description>
            {
                String value = null;
                if (doc.getFieldValues("MD_INFORMATION") != null) {
                    value = (String) doc.getFieldValues("MD_INFORMATION").iterator().next();

                } else if (doc.getFieldValues("MD_DATECREATED") != null) {
                    value = (String) doc.getFieldValues("MD_DATECREATED").iterator().next();

                }
                if (value != null) {
                    Element eleDcDescription = new Element("description", nsDc);
                    eleDcDescription.setText(format(value));
                    eleEuropeanaRecord.addContent(eleDcDescription);
                }
            }
            // <dc:date>
            {
                if (doc.getFieldValues("MD_YEARPUBLISH") != null) {
                    yearpublish = (String) doc.getFieldValues("MD_YEARPUBLISH").iterator().next();
                } else if (doc.getFieldValues("MD_DATECREATED") != null) {
                    yearpublish = (String) doc.getFieldValues("MD_DATECREATED").iterator().next();
                } else if (topstructDoc != null && topstructDoc.getFieldValues("MD_YEARPUBLISH") != null) {
                    yearpublish = (String) topstructDoc.getFieldValues("MD_YEARPUBLISH").iterator().next();
                } else if (topstructDoc != null && topstructDoc.getFieldValues("MD_DATECREATED") != null) {
                    yearpublish = (String) topstructDoc.getFieldValues("MD_DATECREATED").iterator().next();
                }

                if (yearpublish != null) {
                    Element eleDcDate = new Element("date", nsDc);
                    eleDcDate.setText(format(yearpublish));
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
                        dc_creator.setText(format(value));
                        eleEuropeanaRecord.addContent(dc_creator);
                    }
                }
            }
            // <dc:created>
            {
                if (doc.getFieldValues("MD_DATECREATED") != null) {
                    String created = (String) doc.getFieldValues("MD_DATECREATED").iterator().next();
                    Element eleDcCreated = new Element("created", nsDc);
                    eleDcCreated.setText(format(created));
                    eleEuropeanaRecord.addContent(eleDcCreated);
                }
            }
            // <dc:issued>
            {
                if (doc.getFieldValues("MD_DATEISSUED") != null) {
                    String created = (String) doc.getFieldValues("MD_DATEISSUED").iterator().next();
                    Element eleDcCreated = new Element("created", nsDc);
                    eleDcCreated.setText(format(created));
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
                publisher = (String) doc.getFieldValues("MD_PUBLISHER").iterator().next();
            } else if (topstructDoc != null && topstructDoc.getFieldValues("MD_PUBLISHER") != null) {
                publisher = (String) topstructDoc.getFieldValues("MD_PUBLISHER").iterator().next();
            }
            if (publisher != null) {
                Element dc_publisher = new Element("publisher", nsDc);
                dc_publisher.setText(format(publisher));
                eleEuropeanaRecord.addContent(dc_publisher);
            }

            if (doc.getFieldValues("MD_PLACEPUBLISH") != null) {
                placepublish = (String) doc.getFieldValues("MD_PLACEPUBLISH").iterator().next();
            } else if (topstructDoc != null && topstructDoc.getFieldValues("MD_PLACEPUBLISH") != null) {
                placepublish = (String) topstructDoc.getFieldValues("MD_PLACEPUBLISH").iterator().next();
            }

            // <dc:type>
            {
                Element eleDcType = new Element("type", nsDc);
                if (doc.getFieldValue(SolrConstants.DOCSTRCT) != null) {
                    type = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
                    eleDcType.setText(format(type));
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
                String value = DataManager.getInstance().getConfiguration().getEseDefaultProvider();
                String field = DataManager.getInstance().getConfiguration().getEseProviderField();
                if (doc.getFieldValues(field) != null) {
                    value = (String) doc.getFieldValues(field).iterator().next();
                }
                eleEuropeanaProvider.setText(format(value));
                eleEuropeanaRecord.addContent(eleEuropeanaProvider);
            }
            // MANDATORY: <europeana:type>
            {
                Element eleEuropeanaType = new Element("type", nsEuropeana);
                String europeanaType = "TEXT";
                if (type != null) {
                    // Retrieve coded ESE type, if available
                    Map<String, String> eseTypes = DataManager.getInstance().getConfiguration().getEseTypes();
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
                String value = DataManager.getInstance().getConfiguration().getEseDefaultRightsUrl();
                String field = DataManager.getInstance().getConfiguration().getEseRightsField();
                if (doc.getFieldValues(field) != null) {
                    value = (String) doc.getFieldValues(field).iterator().next();
                }
                eleEuropeanaRights.setText(value);
                eleEuropeanaRecord.addContent(eleEuropeanaRights);
            }
            // MANDATORY: <europeana:dataProvider>
            {
                Element eleEuropeanaDataProvider = new Element("dataProvider", nsEuropeana);
                String value = DataManager.getInstance().getConfiguration().getEseDefaultProvider();
                String field = DataManager.getInstance().getConfiguration().getEseDataProviderField();
                if (doc.getFieldValues(field) != null) {
                    value = (String) doc.getFieldValues(field).iterator().next();
                }
                eleEuropeanaDataProvider.setText(format(value));
                eleEuropeanaRecord.addContent(eleEuropeanaDataProvider);
            }
            // MANDATORY: <europeana:isShownBy> or <europeana:isShownAt>
            {
                Element eleEuropeanaIsShownAt = new Element("isShownAt", nsEuropeana);
                String isShownAt = "";
                if (urn != null) {
                    isShownAt = DataManager.getInstance().getConfiguration().getUrnResolverUrl() + urn;
                } else if (identifier != null) {
                    isShownAt = DataManager.getInstance().getConfiguration().getPiResolverUrl() + identifier;
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

    /**
     * for the server request ?verb=GetRecord this method build the xml section if metadataPrefix is mets
     * 
     * @param handler
     * @return
     */
    public Element createGetRecordMets(RequestHandler handler) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), "mets");
            if (doc == null) {
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
     * for the server request ?verb=GetRecord this method build the xml section if metadataPrefix is mets
     * 
     * @param handler
     * @param metadataPrefix
     * @return
     */
    @SuppressWarnings("unchecked")
    public Element createGetRecordIntrandaViewerUpdate(RequestHandler handler, Metadata metadataPrefix) {
        logger.trace("createGetRecordOverviewPage: {}", handler.getIdentifier());
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            StringBuilder sbUrlRoot = new StringBuilder(DataManager.getInstance().getConfiguration().getHarvestUrl()).append('?');
            if (handler.getFrom() != null) {
                sbUrlRoot.append("&from=").append(handler.getFrom());
            }
            if (handler.getUntil() != null) {
                sbUrlRoot.append("&until=").append(handler.getUntil());
            }
            sbUrlRoot.append("&identifier=").append(handler.getIdentifier()).append("&action=");
            String urlRoot = sbUrlRoot.toString();
            switch (metadataPrefix) {
                case iv_overviewpage: {
                    int status = Utils.getHttpResponseStatus(urlRoot + "snoop_overviewpage");
                    if (status != 200) {
                        logger.trace("Overview page not found for {}", handler.getIdentifier());
                        return new ErrorCode().getCannotDisseminateFormat();
                    }
                }
                    break;
                case iv_crowdsourcing: {
                    int status = Utils.getHttpResponseStatus(urlRoot + "snoop_cs");
                    if (status != 200) {
                        logger.trace("Crowdsourcing not found for {}", handler.getIdentifier());
                        return new ErrorCode().getCannotDisseminateFormat();
                    }
                }
                    break;
                default:
                    logger.trace("Unknown metadata format: {}", metadataPrefix.getMetadataPrefix());
                    return new ErrorCode().getCannotDisseminateFormat();
            }
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("id", handler.getIdentifier());
            jsonArray.add(jsonObj);
            return generateIntrandaViewerUpdates(jsonArray, 1L, 0, 1, handler, "GetRecord", metadataPrefix);
        } catch (IOException e) {
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        }
    }

    /**
     * for the server request ?verb=ListRecords this method build the xml section if metadataPrefix is mets
     * 
     * @param handler
     * @param firstRow
     * @param numRows
     * @return
     * @throws SolrServerException
     */
    public Element createListRecordsMets(RequestHandler handler, int firstRow, int numRows) throws SolrServerException {
        SolrDocumentList records = solr.getListRecords(filterDatestampFromRequest(handler), firstRow, numRows, false, null);
        if (records.isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        try {
            return generateMets(records, records.getNumFound(), firstRow, numRows, handler, "ListRecords");
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        }
    }

    /**
     * 
     * @param handler
     * @param first
     * @param pageSize
     * @param metadataPrefix
     * @return
     * @throws SolrServerException
     * @throws IOException
     * @throws UnsupportedOperationException
     */
    public Element createListRecordsIntrandaViewerUpdates(RequestHandler handler, int first, int pageSize, Metadata metadataPrefix)
            throws UnsupportedOperationException, IOException {
        StringBuilder sbUrl = new StringBuilder(100);
        sbUrl.append(DataManager.getInstance().getConfiguration().getHarvestUrl()).append("?action=");
        switch (metadataPrefix) {
            case iv_overviewpage:
                sbUrl.append("getlist_overviewpage");
                break;
            case iv_crowdsourcing:
                sbUrl.append("getlist_crowdsourcing");
                break;
            default:
                return new ErrorCode().getBadArgument();
        }
        if (handler.getFrom() != null) {
            sbUrl.append("&from=").append(handler.getFrom());
        }
        if (handler.getUntil() != null) {
            sbUrl.append("&until=").append(handler.getUntil());
        }
        sbUrl.append("&first=").append(first).append("&pageSize=").append(pageSize);

        String rawJSON = Utils.getWebContent(sbUrl.toString());
        JSONArray jsonArray = null;
        long totalHits = 0;
        if (StringUtils.isNotEmpty(rawJSON)) {
            try {
                jsonArray = (JSONArray) new JSONParser().parse(rawJSON);
                totalHits = (long) jsonArray.get(0);
                jsonArray.remove(0);
            } catch (ParseException e) {
                logger.error(e.getMessage());
                throw new IOException(e.getMessage());
            }
        }
        if (totalHits == 0) {
            return new ErrorCode().getNoRecordsMatch();
        }
        try {
            return generateIntrandaViewerUpdates(jsonArray, totalHits, first, pageSize, handler, "ListRecords", metadataPrefix);
        } catch (JDOMException e) {
            throw new IOException(e.getMessage());
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
     */
    private Element generateMets(List<SolrDocument> records, long totalHits, int firstRow, int numRows, RequestHandler handler, String recordType)
            throws JDOMException, IOException, SolrServerException {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListRecords = new Element(recordType, xmlns);

        Namespace mets = Namespace.getNamespace(Metadata.mets.getMetadataPrefix(), Metadata.mets.getMetadataNamespace());
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Namespace mods = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
        Namespace dv = Namespace.getNamespace("dv", "http://dfg-viewer.de/");
        Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

        if (records.size() < numRows) {
            numRows = records.size();
        }
        for (SolrDocument doc : records) {
            String url = new StringBuilder(DataManager.getInstance().getConfiguration().getDocumentResolverUrl()).append(doc.getFieldValue(
                    SolrConstants.PI_TOPSTRUCT)).toString();
            String xml = Utils.getWebContent(url);
            if (StringUtils.isEmpty(xml)) {
                xmlListRecords.addContent(new ErrorCode().getCannotDisseminateFormat());
                continue;
            }

            org.jdom2.Document metsFile = Utils.getDocumentFromString(xml, null);
            Element mets_root = metsFile.getRootElement();
            Element newmets = new Element(Metadata.mets.getMetadataPrefix(), mets);
            newmets.addNamespaceDeclaration(xsi);
            newmets.addNamespaceDeclaration(mods);
            newmets.addNamespaceDeclaration(dv);
            newmets.addNamespaceDeclaration(xlink);
            newmets.setAttribute("schemaLocation",
                    "http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-3.xsd http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/version17/mets.v1-7.xsd",
                    xsi);
            newmets.addContent(mets_root.cloneContent());

            Element record = new Element("record", xmlns);
            Element header = getHeader(doc, null, handler);
            record.addContent(header);
            Element metadata = new Element("metadata", xmlns);
            metadata.addContent(newmets);
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

    /**
     * Creates a list of overview page documents for the given Solr document list.
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
    private Element generateIntrandaViewerUpdates(JSONArray jsonArray, long totalHits, int firstRow, final int numRows, RequestHandler handler,
            String recordType, Metadata metadataPrefix) throws JDOMException, IOException {
        if (jsonArray == null) {
            throw new IllegalArgumentException("jsonArray may not be null");
        }
        if (metadataPrefix == null) {
            throw new IllegalArgumentException("metadataPrefix may not be null");
        }
        logger.trace("generateIntrandaViewerUpdates: {}", metadataPrefix.getMetadataPrefix());

        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Namespace nsOverviewPage = Namespace.getNamespace(Metadata.iv_overviewpage.getMetadataPrefix(), Metadata.iv_overviewpage
                .getMetadataNamespace());
        Namespace nsCrowdsourcingUpdates = Namespace.getNamespace(Metadata.iv_crowdsourcing.getMetadataPrefix(), Metadata.iv_crowdsourcing
                .getMetadataNamespace());
        Element xmlListRecords = new Element(recordType, xmlns);

        int useNumRows = numRows;
        if (jsonArray.size() < useNumRows) {
            useNumRows = jsonArray.size();
        }
        StringBuilder sbUrlRoot = new StringBuilder(DataManager.getInstance().getConfiguration().getHarvestUrl()).append('?');
        if (handler.getFrom() != null) {
            sbUrlRoot.append("&from=").append(handler.getFrom());
        }
        if (handler.getUntil() != null) {
            sbUrlRoot.append("&until=").append(handler.getUntil());
        }
        sbUrlRoot.append("&identifier=");
        String urlRoot = sbUrlRoot.toString();
        for (int i = 0; i < jsonArray.size(); ++i) {
            JSONObject jsonObj = (JSONObject) jsonArray.get(i);
            String identifier = (String) jsonObj.get("id");

            Element record = new Element("record", xmlns);
            xmlListRecords.addContent(record);

            // Header
            Element header = new Element("header", xmlns);

            Element eleIdentifier = new Element("identifier", xmlns);
            eleIdentifier.setText(identifier);
            header.addContent(eleIdentifier);

            // datestamp
            Long timestamp = (Long) jsonObj.get("du");
            if (timestamp == null) {
                timestamp = 0L;
            }
            Element eleDatestamp = new Element("datestamp", xmlns);
            // long untilTimestamp = RequestHandler.getUntilTimestamp(handler.getUntil());
            eleDatestamp.setText(parseDate(timestamp));
            header.addContent(eleDatestamp);

            record.addContent(header);

            Element metadata = new Element("metadata", xmlns);
            //            if (doc.getFieldValues(SolrConstants.TITLE) != null) {
            //            Element eleTitle = new Element("title", nsOverviewPage);
            //                eleTitle.setText((String) doc.getFieldValues(SolrConstants.TITLE).iterator().next());
            //                metadata.addContent(eleTitle);
            //            }

            try {
                // Add process ID, if available
                String processId = null;
                StringBuilder sb = new StringBuilder();
                sb.append(SolrConstants.PI).append(':').append(identifier);
                try {
                    SolrDocument doc = solr.getFirstDoc(sb.toString(), Collections.singletonList("MD_PROCESSID"));
                    if (doc != null) {
                        processId = (String) doc.getFirstValue("MD_PROCESSID");
                    }
                } catch (SolrServerException e) {
                    logger.error(e.getMessage(), e);
                }
                if (processId != null) {
                    Element eleId = new Element("processId", nsOverviewPage);
                    eleId.setText(processId);
                    metadata.addContent(eleId);
                }

                // Add dataset URL
                String url = urlRoot + identifier + "&action=";
                switch (metadataPrefix) {
                    case iv_overviewpage: {
                        Element eleUrl = new Element("url", nsOverviewPage);
                        eleUrl.setText(url + "get_overviewpage");
                        metadata.addContent(eleUrl);
                    }
                        break;
                    case iv_crowdsourcing: {
                        Element eleUrl = new Element("url", nsCrowdsourcingUpdates);
                        eleUrl.setText(url + "get_cs");
                        metadata.addContent(eleUrl);
                    }
                        break;
                    default:
                        break;
                }
                record.addContent(metadata);
            } catch (UnsupportedOperationException e) {
                logger.error(e.getMessage(), e);
                xmlListRecords.addContent(new ErrorCode().getCannotDisseminateFormat());
                continue;
            }
        }

        // Create resumption token
        if (totalHits > firstRow + useNumRows) {
            Element resumption = createResumptionTokenAndElement(totalHits, firstRow + useNumRows, xmlns, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }

    /**
     * generates header for epicur format
     * 
     * @param doc
     * @param dateUpdated
     * @return
     */
    private static Element generateEpicurHeader(SolrDocument doc, long dateUpdated) {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element header = new Element("header", xmlns);
        Element identifier = new Element("identifier", xmlns);
        identifier.setText(DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier") + (String) doc.getFieldValue(
                "URN"));
        header.addContent(identifier);

        Element datestamp = new Element("datestamp", xmlns);
        datestamp.setText(parseDate(dateUpdated));
        header.addContent(datestamp);
        // setSpec
        List<String> setSpecFields = DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.epicur.name());
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

    /**
     * 
     * @param doc
     * @param urn
     * @param dateUpdated
     * @return
     */
    private static Element generateEpicurPageHeader(SolrDocument doc, String urn, long dateUpdated) {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element header = new Element("header", xmlns);

        Element identifier = new Element("identifier", xmlns);
        identifier.setText(DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier") + urn);
        header.addContent(identifier);

        Element datestamp = new Element("datestamp", xmlns);
        datestamp.setText(parseDate(dateUpdated));
        header.addContent(datestamp);
        // setSpec
        List<String> setSpecFields = DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.epicur.name());
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

    /**
     * for the server request ?verb=ListRecords this method build the xml section if metadataPrefix is epicur
     * 
     * @param handler
     * @param firstRow
     * @param recordType
     * @return
     * @throws SolrServerException
     */
    public Element createListRecordsEpicur(RequestHandler handler, int firstRow, int numRows, String recordType) throws SolrServerException {
        logger.trace("createListRecordsEpicur");
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();

        String urnPrefixBlacklistSuffix = getUrnPrefixBlacklistSuffix(DataManager.getInstance().getConfiguration().getUrnPrefixBlacklist());
        String querySuffix = urnPrefixBlacklistSuffix + getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration()
                .getAdditionalDocstructTypes());
        SolrDocumentList records = solr.getListRecords(filterDatestampFromRequest(handler), firstRow, numRows, true, querySuffix);
        if (records.isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        Element xmlListRecords = new Element(recordType, xmlns);
        if (records.size() < numRows) {
            numRows = records.size();
        }
        int pagecount = 0;
        for (SolrDocument doc : records) {
            long dateUpdated = SolrSearchIndex.getLatestValidDateUpdated(doc, RequestHandler.getUntilTimestamp(handler.getUntil()));
            Long dateDeleted = (Long) doc.getFieldValue(SolrConstants.DATEDELETED);
            if (doc.getFieldValue(SolrConstants.URN) != null) {
                Element record = new Element("record", xmlns);
                Element header = generateEpicurHeader(doc, dateUpdated);
                record.addContent(header);
                Element metadata = new Element("metadata", xmlns);
                record.addContent(metadata);
                metadata.addContent(generateEpicurElement((String) doc.getFieldValue(SolrConstants.URN), (Long) doc.getFieldValue(
                        SolrConstants.DATECREATED), dateUpdated, dateDeleted));
                xmlListRecords.addContent(record);
                // logger.debug("record: " + new XMLOutputter().outputString(record));
            }

            if (dateDeleted == null) {
                // Page elements for existing record
                StringBuilder sbPageQuery = new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':').append(doc.getFieldValue(
                        SolrConstants.PI_TOPSTRUCT)).append(" AND ").append(SolrConstants.DOCTYPE).append(":PAGE").append(" AND ").append(
                                SolrConstants.IMAGEURN).append(":*");
                sbPageQuery.append(urnPrefixBlacklistSuffix);
                // logger.trace("pageQuery: {}", sbPageQuery.toString());
                QueryResponse qr = solr.search(sbPageQuery.toString(), 0, SolrSearchIndex.MAX_HITS, Collections.singletonList(SolrConstants.ORDER),
                        Collections.singletonList(SolrConstants.IMAGEURN), null);
                if (qr != null && !qr.getResults().isEmpty()) {
                    for (SolrDocument pageDoc : qr.getResults()) {
                        String imgUrn = (String) pageDoc.getFieldValue(SolrConstants.IMAGEURN);
                        Element pagerecord = new Element("record", xmlns);
                        Element pageheader = generateEpicurPageHeader(doc, imgUrn, dateUpdated);
                        pagerecord.addContent(pageheader);
                        Element pagemetadata = new Element("metadata", xmlns);
                        pagerecord.addContent(pagemetadata);
                        pagemetadata.addContent(generateEpicurPageElement(imgUrn, (Long) doc.getFieldValue(SolrConstants.DATECREATED), dateUpdated,
                                (Long) doc.getFieldValue(SolrConstants.DATEDELETED)));
                        xmlListRecords.addContent(pagerecord);
                        pagecount++;
                    }
                }
            } else {
                // Page elements for deleted record (only deleted record docs will have IMAGEURN_OAI!)
                Collection<Object> pageUrnValues = doc.getFieldValues(SolrConstants.IMAGEURN_OAI);
                if (pageUrnValues != null) {
                    for (Object obj : pageUrnValues) {
                        String imgUrn = (String) obj;
                        Element pagerecord = new Element("record", xmlns);
                        Element pageheader = generateEpicurPageHeader(doc, imgUrn, dateUpdated);
                        pagerecord.addContent(pageheader);
                        Element pagemetadata = new Element("metadata", xmlns);
                        pagerecord.addContent(pagemetadata);
                        pagemetadata.addContent(generateEpicurPageElement(imgUrn, (Long) doc.getFieldValue(SolrConstants.DATECREATED), dateUpdated,
                                dateDeleted));
                        xmlListRecords.addContent(pagerecord);
                        pagecount++;
                    }
                }
            }
        }
        logger.debug("Found {} page records", pagecount);

        // Create resumption token
        if (records.getNumFound() > firstRow + numRows) {
            Element resumption = createResumptionTokenAndElement(records.getNumFound(), firstRow + numRows, xmlns, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
    }

    /**
     * for the server request ?verb=GetRecord this method build the xml section if metadataPrefix is epicur
     * 
     * @param handler
     * @return
     */
    public Element getRecordEpicur(RequestHandler handler) {
        logger.trace("getRecordEpicur");
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), "epicur");
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
            Element getRecord = new Element("GetRecord", xmlns);
            Element record = new Element("record", xmlns);
            long dateupdated = SolrSearchIndex.getLatestValidDateUpdated(doc, RequestHandler.getUntilTimestamp(handler.getUntil()));
            Element header = generateEpicurPageHeader(doc, handler.getIdentifier(), dateupdated);
            record.addContent(header);
            Element metadata = new Element("metadata", xmlns);
            record.addContent(metadata);
            metadata.addContent(generateEpicurPageElement(handler.getIdentifier(), (Long) doc.getFieldValue(SolrConstants.DATECREATED), dateupdated,
                    (Long) doc.getFieldValue(SolrConstants.DATEDELETED)));
            getRecord.addContent(record);

            return getRecord;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        } catch (SolrServerException e) {
            return new ErrorCode().getIdDoesNotExist();
        }
    }

    // generates an epicur element
    private static Element generateEpicurElement(String urn, Long dateCreated, Long dateUpdated, Long dateDeleted) {
        Namespace xmlns = Namespace.getNamespace("urn:nbn:de:1111-2004033116");

        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Namespace epicurNamespace = Namespace.getNamespace("epicur", "urn:nbn:de:1111-2004033116");
        Element epicur = new Element("epicur", xmlns);
        epicur.addNamespaceDeclaration(xsi);
        epicur.addNamespaceDeclaration(epicurNamespace);
        epicur.setAttribute("schemaLocation", "urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd", xsi);
        String status = "urn_new";

        // xsi:schemaLocation="urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd"
        // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        // xmlns:epicur="urn:nbn:de:1111-2004033116"
        // xmlns="urn:nbn:de:1111-2004033116"

        if (dateDeleted != null) {
            status = "url_delete";
        } else {
            if (dateCreated != null && dateUpdated != null) {
                if (dateUpdated > dateCreated) {
                    status = "url_update_general";
                } else {
                    status = "urn_new";
                }
            } else {
                status = "urn_new";
            }
        }

        epicur.addContent(generateAdministrativeData(status, xmlns));

        Element record = new Element("record", xmlns);
        Element schemaIdentifier = new Element("identifier", xmlns);
        schemaIdentifier.setAttribute("scheme", "urn:nbn:de");
        schemaIdentifier.setText(urn);
        record.addContent(schemaIdentifier);

        Element resource = new Element("resource", xmlns);
        record.addContent(resource);

        Element identifier = new Element("identifier", xmlns);
        identifier.setAttribute("origin", "original");
        identifier.setAttribute("role", "primary");
        identifier.setAttribute("scheme", "url");
        identifier.setAttribute("type", "frontpage");

        identifier.setText(DataManager.getInstance().getConfiguration().getUrnResolverUrl() + urn);
        resource.addContent(identifier);
        Element format = new Element("format", xmlns);
        format.setAttribute("scheme", "imt");
        format.setText("text/html");
        resource.addContent(format);
        epicur.addContent(record);

        return epicur;
    }

    /**
     * 
     * @param urn
     * @param dateCreated
     * @param dateUpdated
     * @param dateDeleted
     * @return
     */
    private static Element generateEpicurPageElement(String urn, Long dateCreated, Long dateUpdated, Long dateDeleted) {
        Namespace xmlns = Namespace.getNamespace("urn:nbn:de:1111-2004033116");

        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Namespace epicurNamespace = Namespace.getNamespace("epicur", "urn:nbn:de:1111-2004033116");
        Element epicur = new Element("epicur", xmlns);
        epicur.addNamespaceDeclaration(xsi);
        epicur.addNamespaceDeclaration(epicurNamespace);
        epicur.setAttribute("schemaLocation", "urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd", xsi);
        String status = "urn_new";

        // xsi:schemaLocation="urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd"
        // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        // xmlns:epicur="urn:nbn:de:1111-2004033116"
        // xmlns="urn:nbn:de:1111-2004033116"

        // /*
        // TODO add this after dnb can handle status updates

        if (dateDeleted != null) {
            status = "url_delete";
        } else if (dateCreated != null && dateUpdated != null) {
            if (dateUpdated > dateCreated) {
                status = "url_update_general";
            } else {
                status = "urn_new";
            }
        }

        // */

        epicur.addContent(generateAdministrativeData(status, xmlns));

        Element record = new Element("record", xmlns);
        Element schemaIdentifier = new Element("identifier", xmlns);
        schemaIdentifier.setAttribute("scheme", "urn:nbn:de");
        schemaIdentifier.setText(urn);
        record.addContent(schemaIdentifier);

        Element resource = new Element("resource", xmlns);
        record.addContent(resource);

        Element identifier = new Element("identifier", xmlns);
        identifier.setAttribute("origin", "original");
        identifier.setAttribute("role", "primary");
        identifier.setAttribute("scheme", "url");
        identifier.setAttribute("type", "frontpage");

        identifier.setText(DataManager.getInstance().getConfiguration().getUrnResolverUrl() + urn);
        resource.addContent(identifier);
        Element format = new Element("format", xmlns);
        format.setAttribute("scheme", "imt");
        format.setText("text/html");
        resource.addContent(format);
        epicur.addContent(record);

        return epicur;
    }

    // generates administrative_data section in epicur
    private static Element generateAdministrativeData(String status, Namespace xmlns) {
        // Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element administrative_data = new Element("administrative_data", xmlns);
        Element delivery = new Element("delivery", xmlns);
        Element update_status = new Element("update_status", xmlns);
        update_status.setAttribute("type", status);
        delivery.addContent(update_status);
        Element transfer = new Element("transfer", xmlns);
        transfer.setAttribute("type", "oai");
        delivery.addContent(transfer);
        administrative_data.addContent(delivery);
        return administrative_data;
    }

    public Element createGetRecordMarc(RequestHandler handler) {
        Element mets = createGetRecordMets(handler);
        return generateMarc(mets, "GetRecord");
    }

    /**
     * 
     * @param handler
     * @param firstRow
     * @param numRows
     * @return
     * @throws SolrServerException
     */
    public Element createListRecordsMarc(RequestHandler handler, int firstRow, int numRows) throws SolrServerException {
        Element mets = createListRecordsMets(handler, firstRow, numRows);
        if (mets.getName().equals("error")) {
            return mets;
        }
        return generateMarc(mets, "ListRecords");
    }

    private static Element generateMarc(Element mets, String recordType) {
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Namespace xmlns = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");
        Namespace metsNamespace = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
        Namespace marc = Namespace.getNamespace("marc", "http://www.loc.gov/MARC21/slim");
        Namespace mods = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
        Element xmlListRecords = new Element(recordType, xmlns);
        Element token = mets.getChild("resumptionToken", xmlns);
        List<Element> records = mets.getChildren("record", xmlns);

        for (Element rec : records) {
            Element header = rec.getChild("header", xmlns);
            Element modsOnly = rec.getChild("metadata", xmlns).getChild("mets", metsNamespace).getChild("dmdSec", metsNamespace).getChild("mdWrap",
                    metsNamespace).getChild("xmlData", metsNamespace).getChild("mods", mods);

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
                answer.addNamespaceDeclaration(xsi);
                answer.setAttribute("schemaLocation", "http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd",
                        xsi);
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

    /**
     * For the server request ?verb=GetRecord this method build the XML section if metadataPrefix is 'lido'.
     * 
     * @param handler
     * @return
     */
    public Element createGetRecordLido(RequestHandler handler) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), Metadata.lido.getMetadataPrefix());
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
     * For the server request ?verb=ListRecords this method build the xml section if metadataPrefix is 'lido'
     * 
     * @param handler
     * @param firstRow
     * @param numRows
     * @return
     * @throws SolrServerException
     */
    public Element createListRecordsLido(RequestHandler handler, int firstRow, int numRows) throws SolrServerException {
        SolrDocumentList records = solr.getListRecords(filterDatestampFromRequest(handler), firstRow, numRows, false, null);
        if (records.isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        try {
            Element xmlListRecords = generateLido(records, records.getNumFound(), firstRow, numRows, handler, "ListRecords");
            return xmlListRecords;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
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
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListRecords = new Element(recordType, xmlns);

        Namespace lido = Namespace.getNamespace(Metadata.lido.getMetadataPrefix(), Metadata.lido.getMetadataNamespace());
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        if (records.size() < numRows) {
            numRows = records.size();
        }
        for (SolrDocument doc : records) {
            String url = new StringBuilder(DataManager.getInstance().getConfiguration().getDocumentResolverUrl()).append(doc.getFieldValue(
                    SolrConstants.PI_TOPSTRUCT)).toString();
            String xml = Utils.getWebContent(url);
            if (StringUtils.isEmpty(xml)) {
                xmlListRecords.addContent(new ErrorCode().getCannotDisseminateFormat());
                continue;
            }

            org.jdom2.Document xmlDoc = Utils.getDocumentFromString(xml, null);
            Element xmlRoot = xmlDoc.getRootElement();
            Element newLido = new Element(Metadata.lido.getMetadataPrefix(), lido);
            newLido.addNamespaceDeclaration(xsi);
            newLido.setAttribute(new Attribute("schemaLocation", "http://www.lido-schema.org http://www.lido-schema.org/schema/v1.0/lido-v1.0.xsd",
                    xsi));
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

    /**
     * For the server request ?verb=GetRecord this method build the XML section if metadataPrefix is 'tei'.
     * 
     * @param handler
     * @return
     */
    public Element createGetRecordTei(RequestHandler handler) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), Metadata.tei.getMetadataPrefix());
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            Element record = generateTei(Collections.singletonList(doc), 1L, 0, 1, handler, "GetRecord");
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
     * For the server request ?verb=ListRecords this method build the XML section if metadataPrefix is 'tei'.
     * 
     * @param handler
     * @param firstRow
     * @param numRows
     * @return
     * @throws SolrServerException
     */
    public Element createListRecordsTei(RequestHandler handler, int firstRow, int numRows) throws SolrServerException {
        SolrDocumentList records = solr.getListRecords(filterDatestampFromRequest(handler), firstRow, numRows, false, null);
        if (records.isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        try {
            Element xmlListRecords = generateTei(records, records.getNumFound(), firstRow, numRows, handler, "ListRecords");
            return xmlListRecords;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        }
    }

    /**
     * Creates TEI records.
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
    private Element generateTei(List<SolrDocument> records, long totalHits, int firstRow, int numRows, RequestHandler handler, String recordType)
            throws JDOMException, IOException, SolrServerException {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListRecords = new Element(recordType, xmlns);

        Namespace tei = Namespace.getNamespace(Metadata.tei.getMetadataPrefix(), Metadata.tei.getMetadataNamespace());
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        if (records.size() < numRows) {
            numRows = records.size();
        }
        for (SolrDocument doc : records) {
            String url = new StringBuilder(DataManager.getInstance().getConfiguration().getDocumentResolverUrl()).append(doc.getFieldValue(
                    SolrConstants.PI_TOPSTRUCT)).toString();
            String xml = Utils.getWebContent(url);
            if (StringUtils.isEmpty(xml)) {
                xmlListRecords.addContent(new ErrorCode().getCannotDisseminateFormat());
                continue;
            }

            org.jdom2.Document teiFile = Utils.getDocumentFromString(xml, null);
            Element teiRoot = teiFile.getRootElement();
            Element newTei = new Element(Metadata.tei.getMetadataPrefix(), tei);
            newTei.addNamespaceDeclaration(xsi);
            newTei.setAttribute(new Attribute("schemaLocation", Metadata.tei.getSchema(), xsi));
            newTei.addContent(teiRoot.cloneContent());

            Element record = new Element("record", xmlns);
            Element header = getHeader(doc, null, handler);
            record.addContent(header);
            Element metadata = new Element("metadata", xmlns);
            metadata.addContent(newTei);
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
    
    /**
     * For the server request ?verb=GetRecord this method build the XML section if metadataPrefix is 'cmdi'.
     * 
     * @param handler
     * @return
     */
    public Element createGetRecordCmdi(RequestHandler handler) {
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), Metadata.cmdi.getMetadataPrefix());
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            Element record = generateTei(Collections.singletonList(doc), 1L, 0, 1, handler, "GetRecord");
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
     * For the server request ?verb=ListRecords this method build the XML section if metadataPrefix is 'cmdi'.
     * 
     * @param handler
     * @param firstRow
     * @param numRows
     * @return
     * @throws SolrServerException
     */
    public Element createListRecordsCmdi(RequestHandler handler, int firstRow, int numRows) throws SolrServerException {
        SolrDocumentList records = solr.getListRecords(filterDatestampFromRequest(handler), firstRow, numRows, false, null);
        if (records.isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        try {
            Element xmlListRecords = generateCmdi(records, records.getNumFound(), firstRow, numRows, handler, "ListRecords");
            return xmlListRecords;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        }
    }

    /**
     * Creates CMDI records.
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
    private Element generateCmdi(List<SolrDocument> records, long totalHits, int firstRow, int numRows, RequestHandler handler, String recordType)
            throws JDOMException, IOException, SolrServerException {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListRecords = new Element(recordType, xmlns);

        Namespace tei = Namespace.getNamespace(Metadata.cmdi.getMetadataPrefix(), Metadata.cmdi.getMetadataNamespace());
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        if (records.size() < numRows) {
            numRows = records.size();
        }
        for (SolrDocument doc : records) {
            String url = new StringBuilder(DataManager.getInstance().getConfiguration().getDocumentResolverUrl()).append(doc.getFieldValue(
                    SolrConstants.PI_TOPSTRUCT)).toString();
            String xml = Utils.getWebContent(url);
            if (StringUtils.isEmpty(xml)) {
                xmlListRecords.addContent(new ErrorCode().getCannotDisseminateFormat());
                continue;
            }

            org.jdom2.Document file = Utils.getDocumentFromString(xml, null);
            Element teiRoot = file.getRootElement();
            Element newTei = new Element(Metadata.cmdi.getMetadataPrefix(), tei);
            newTei.addNamespaceDeclaration(xsi);
            newTei.setAttribute(new Attribute("schemaLocation", Metadata.cmdi.getSchema(), xsi));
            newTei.addContent(teiRoot.cloneContent());

            Element record = new Element("record", xmlns);
            Element header = getHeader(doc, null, handler);
            record.addContent(header);
            Element metadata = new Element("metadata", xmlns);
            metadata.addContent(newTei);
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

    /**
     * 
     * @param totalHits
     * @param cursor
     * @param xmlns
     * @param handler
     * @return
     */
    private Element createResumptionTokenAndElement(long totalHits, int cursor, Namespace xmlns, RequestHandler handler) {
        long now = System.currentTimeMillis();
        long time = now + expiration;
        ResumptionToken token = new ResumptionToken("oai_" + System.currentTimeMillis(), totalHits, cursor, time, handler);
        try {
            saveToken(token);
        } catch (IOException e) {
            // do nothing
        }
        Element eleResumptionToken = new Element("resumptionToken", xmlns);
        eleResumptionToken.setAttribute("expirationDate", Utils.convertDate(time));
        eleResumptionToken.setAttribute("completeListSize", String.valueOf(totalHits));
        eleResumptionToken.setAttribute("cursor", String.valueOf(cursor));
        eleResumptionToken.setText(token.getTokenName());

        return eleResumptionToken;
    }

    /**
     * 
     * @return
     * @should build query suffix correctly
     */
    static String getAdditionalDocstructsQuerySuffix(List<String> additionalDocstructTypes) {
        StringBuilder sbQuerySuffix = new StringBuilder();
        if (additionalDocstructTypes != null && !additionalDocstructTypes.isEmpty()) {
            sbQuerySuffix.append(" OR (").append(SolrConstants.DOCTYPE).append(":DOCSTRCT AND (");
            int count = 0;
            for (String docstructType : additionalDocstructTypes) {
                if (StringUtils.isNotBlank(docstructType)) {
                    if (count > 0) {
                        sbQuerySuffix.append(" OR ");
                    }
                    sbQuerySuffix.append(SolrConstants.DOCSTRCT).append(':').append(docstructType);
                    count++;
                } else {
                    logger.warn("Empty element found in <additionalDocstructTypes>.");
                }
            }
            sbQuerySuffix.append("))");
            // Avoid returning an invalid subquery if all configured values are blank
            if (count == 0) {
                return "";
            }
        }

        return sbQuerySuffix.toString();
    }

    /**
     * 
     * @return
     * @should build query suffix correctly
     */
    static String getUrnPrefixBlacklistSuffix(List<String> urnPrefixBlacklist) {
        StringBuilder sbQuerySuffix = new StringBuilder();
        if (urnPrefixBlacklist != null && !urnPrefixBlacklist.isEmpty()) {
            int count = 0;
            for (String urnPrefix : urnPrefixBlacklist) {
                if (StringUtils.isNotBlank(urnPrefix)) {
                    urnPrefix = ClientUtils.escapeQueryChars(urnPrefix);
                    urnPrefix += '*';
                    sbQuerySuffix.append(" -").append("URN_UNTOKENIZED:").append(urnPrefix).append(" -").append("IMAGEURN_UNTOKENIZED:").append(
                            urnPrefix).append(" -").append("IMAGEURN_OAI_UNTOKENIZED:").append(urnPrefix);
                    count++;
                } else {
                    logger.warn("Empty element found in <additionalDocstructTypes>.");
                }
            }
            // Avoid returning an invalid subquery if all configured values are blank
            if (count == 0) {
                return "";
            }
        }

        return sbQuerySuffix.toString();
    }

}
