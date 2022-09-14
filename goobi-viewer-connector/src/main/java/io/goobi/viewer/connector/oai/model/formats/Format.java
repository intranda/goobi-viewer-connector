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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.enums.Verb;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.oai.model.ResumptionToken;
import io.goobi.viewer.connector.oai.model.Set;
import io.goobi.viewer.connector.oai.model.language.Language;
import io.goobi.viewer.connector.utils.SolrConstants;
import io.goobi.viewer.connector.utils.SolrSearchIndex;
import io.goobi.viewer.connector.utils.SolrSearchTools;
import io.goobi.viewer.connector.utils.Utils;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * <p>
 * Abstract Format class.
 * </p>
 *
 */
public abstract class Format {

    private static final Logger logger = LogManager.getLogger(Format.class);

    /** Constant <code>XML</code> */
    protected static final Namespace XML = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");
    /** Constant <code>XSI</code> */
    protected static final Namespace XSI = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

    public static final String ACCESSCONDITION_OPENACCESS = "info:eu-repo/semantics/openAccess";
    public static final String ACCESSCONDITION_CLOSEDACCESS = "info:eu-repo/semantics/closedAccess";

    protected final String[] DATE_FIELDS = { SolrConstants.DATECREATED, SolrConstants.DATEUPDATED };
    protected final String[] IDENTIFIER_FIELDS = { SolrConstants.PI, SolrConstants.PI_TOPSTRUCT };

    /** Constant <code>expiration=259200000L</code> */
    protected static long expiration = 259200000L; // 3 days

    protected SolrSearchIndex solr = DataManager.getInstance().getSearchIndex();

    /**
     * <p>
     * createListRecords.
     * </p>
     *
     * @param handler a {@link io.goobi.viewer.connector.oai.RequestHandler} object.
     * @param firstVirtualRow a int.
     * @param firstRawRow a int.
     * @param numRows a int.
     * @param versionDiscriminatorField a {@link java.lang.String} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a {@link org.jdom2.Element} object.
     * @throws java.io.IOException if any.
     */
    public abstract Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows,
            String versionDiscriminatorField, String filterQuerySuffix) throws IOException, SolrServerException;

    /**
     * <p>
     * createGetRecord.
     * </p>
     *
     * @param handler a {@link io.goobi.viewer.connector.oai.RequestHandler} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return a {@link org.jdom2.Element} object.
     */
    public abstract Element createGetRecord(RequestHandler handler, String filterQuerySuffix);

    /**
     * <p>
     * getTotalHits.
     * </p>
     *
     * @param params a {@link java.util.Map} object.
     * @param versionDiscriminatorField a {@link java.lang.String} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @throws java.io.IOException
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a long.
     */
    public abstract long getTotalHits(Map<String, String> params, String versionDiscriminatorField, String filterQuerySuffix)
            throws IOException, SolrServerException;

    /**
     * for the server request ?verb=Identify this method build the xml section in the Identify element
     *
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return the identify Element for the xml tree
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @throws IOException
     */
    public static Element getIdentifyXML(String filterQuerySuffix) throws SolrServerException, IOException {
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

        // TODO: protocol definition allow more than one email address, change away from HashMap
        Element adminEmail = new Element("adminEmail", xmlns);
        adminEmail.setText(identifyTags.get("adminEmail")); //
        identify.addContent(adminEmail);

        Element earliestDatestamp = new Element("earliestDatestamp", xmlns);
        earliestDatestamp.setText(DataManager.getInstance().getSearchIndex().getEarliestRecordDatestamp(filterQuerySuffix));
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
     * for the server request ?verb=ListMetadataFormats this method build the xml section
     *
     * @return a {@link org.jdom2.Element} object.
     */
    public static Element createMetadataFormats() {
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
                metadataNamespace.setText(m.getMetadataNamespaceUri());
                metadataFormat.addContent(metadataPrefix);
                metadataFormat.addContent(schema);
                metadataFormat.addContent(metadataNamespace);
                listMetadataFormats.addContent(metadataFormat);
            }
        }
        return listMetadataFormats;
    }

    /**
     * for the server request ?verb=ListSets this method build the xml section
     *
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link org.jdom2.Element} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @throws IOException
     */
    public static Element createListSets(Locale locale) throws SolrServerException, IOException {
        // Add all values sets (a set for each existing field value)
        List<Set> allValuesSetConfigurations = DataManager.getInstance().getConfiguration().getAllValuesSets();
        if (allValuesSetConfigurations != null && !allValuesSetConfigurations.isEmpty()) {
            for (Set set : allValuesSetConfigurations) {
                set.getValues().addAll(DataManager.getInstance().getSearchIndex().getSets(set.getSetName()));
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
                Element eleSetSpec = new Element("setSpec", xmlns);
                eleSetSpec.setText(set.getSetName() + ":" + value);
                eleSet.addContent(eleSetSpec);
                Element name = new Element("setName", xmlns);
                if (set.isTranslate()) {
                    name.setText(ViewerResourceBundle.getTranslation(value, locale));
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
     * for the server request ?verb=ListIdentifiers this method build the xml section
     *
     * @param handler a {@link io.goobi.viewer.connector.oai.RequestHandler} object.
     * @param firstVirtualRow a int.
     * @param firstRawRow a int.
     * @param numRows a int.
     * @param versionDiscriminatorField a {@link java.lang.String} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return a {@link org.jdom2.Element} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @throws IOException
     */
    public Element createListIdentifiers(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField,
            String filterQuerySuffix) throws SolrServerException, IOException {
        Map<String, String> datestamp = Utils.filterDatestampFromRequest(handler);

        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListIdentifiers = new Element("ListIdentifiers", xmlns);

        List<String> setSpecFields =
                DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(handler.getMetadataPrefix().name());

        QueryResponse qr;
        long totalVirtualHits;
        int virtualHitCount = 0;
        long totalRawHits;
        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            // One OAI record for each record version
            qr = DataManager.getInstance()
                    .getSearchIndex()
                    .getListIdentifiers(datestamp, firstRawRow, numRows, " AND " + versionDiscriminatorField + ":*", null,
                            Collections.singletonList(versionDiscriminatorField), filterQuerySuffix);
            if (qr.getResults().isEmpty()) {
                return new ErrorCode().getNoRecordsMatch();
            }
            totalVirtualHits = SolrSearchTools.getFieldCount(qr, versionDiscriminatorField);
            totalRawHits = qr.getResults().getNumFound();
            for (SolrDocument doc : qr.getResults()) {
                List<String> versions = SolrSearchTools.getMetadataValues(doc, versionDiscriminatorField);
                for (String version : versions) {
                    String iso3code = version;
                    // Make sure to add the ISO-3 language code
                    if (SolrConstants.LANGUAGE.equals(versionDiscriminatorField) && iso3code.length() == 2) {
                        Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(version);
                        if (lang != null) {
                            iso3code = lang.getIsoCode();
                        }
                    }
                    Element header = getHeader(doc, null, handler, iso3code, setSpecFields, filterQuerySuffix);
                    xmlListIdentifiers.addContent(header);
                    virtualHitCount++;
                }
            }
        } else {
            // One OAI record for each record proper
            qr = DataManager.getInstance().getSearchIndex().getListIdentifiers(datestamp, firstRawRow, numRows, null, null, null, filterQuerySuffix);
            if (qr.getResults().isEmpty()) {
                return new ErrorCode().getNoRecordsMatch();
            }
            totalVirtualHits = totalRawHits = qr.getResults().getNumFound();
            for (SolrDocument doc : qr.getResults()) {
                Element header = getHeader(doc, null, handler, null, setSpecFields, filterQuerySuffix);
                xmlListIdentifiers.addContent(header);
                virtualHitCount++;
            }
        }

        // Create resumption token
        if (totalRawHits > firstRawRow + numRows) {
            Element resumption = createResumptionTokenAndElement(totalVirtualHits, totalRawHits, firstVirtualRow + virtualHitCount,
                    firstRawRow + numRows, xmlns, handler);
            xmlListIdentifiers.addContent(resumption);
        }

        return xmlListIdentifiers;
    }

    /**
     * creates root element for oai protocol
     *
     * @param elementName a {@link java.lang.String} object.
     * @return a {@link org.jdom2.Element} object.
     */
    public static Element getOaiPmhElement(String elementName) {
        Element oaiPmh = new Element(elementName);

        oaiPmh.setNamespace(Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/"));
        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        oaiPmh.addNamespaceDeclaration(xsi);
        oaiPmh.setAttribute("schemaLocation", "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", xsi);
        return oaiPmh;
    }

    /**
     * create the header for listIdentifiers and ListRecords, because there are both the same
     *
     * @param doc Document from which to extract values.
     * @param topstructDoc If not null, the datestamp value will be determined from this instead.
     * @param handler a {@link io.goobi.viewer.connector.oai.RequestHandler} object.
     * @param requestedVersion a {@link java.lang.String} object.
     * @param setSpecFields
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return a {@link org.jdom2.Element} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @throws IOException
     * @throws IndexUnreachableException
     */
    protected static Element getHeader(SolrDocument doc, SolrDocument topstructDoc, RequestHandler handler, String requestedVersion,
            List<String> setSpecFields, String filterQuerySuffix) throws SolrServerException, IOException {
        // logger.trace("getHeader: {}", doc.getFieldValue(SolrConstants.PI));
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element header = new Element("header", xmlns);
        // identifier
        if (doc.getFieldValue(SolrConstants.URN) != null && ((String) doc.getFieldValue(SolrConstants.URN)).length() > 0) {
            Element urn_identifier = new Element("identifier", xmlns);
            urn_identifier.setText(DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier")
                    + (String) doc.getFieldValue(SolrConstants.URN));
            header.addContent(urn_identifier);
        } else {
            Element identifier = new Element("identifier", xmlns);
            String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
            if (pi == null) {
                pi = (String) doc.getFieldValue(SolrConstants.PI);
            }
            identifier.setText(DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier") + pi
                    + (StringUtils.isNotEmpty(requestedVersion) ? '_' + requestedVersion : ""));
            header.addContent(identifier);
        }
        // datestamp
        Element datestamp = new Element("datestamp", xmlns);
        long untilTimestamp = RequestHandler.getUntilTimestamp(handler.getUntil());
        logger.trace("untilTimestamp: {}", untilTimestamp);
        long timestampModified = SolrSearchTools.getLatestValidDateUpdated(topstructDoc != null ? topstructDoc : doc, untilTimestamp);
        logger.trace("timestampModified: {}", timestampModified);
        datestamp.setText(Utils.parseDate(timestampModified));
        if (StringUtils.isEmpty(datestamp.getText()) && doc.getFieldValue(SolrConstants.ISANCHOR) != null) {
            datestamp.setText(
                    Utils.parseDate(DataManager.getInstance().getSearchIndex().getLatestVolumeTimestamp(doc, untilTimestamp, filterQuerySuffix)));
        }
        header.addContent(datestamp);
        logger.trace("datestamp: {}", datestamp.getText());
        // setSpec
        if (StringUtils.isNotEmpty(handler.getSet())) {
            // setSpec from handler
            Element setSpec = new Element("setSpec", xmlns);
            setSpec.setText(handler.getSet());
            header.addContent(setSpec);
        } else if (handler.getMetadataPrefix() != null) {
            // setSpec from config
            if (setSpecFields != null && !setSpecFields.isEmpty()) {
                for (String setSpecField : setSpecFields) {
                    if (!doc.containsKey(setSpecField)) {
                        continue;
                    }
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
            datestamp.setText(Utils.parseDate(doc.getFieldValue(SolrConstants.DATEDELETED)));
        }

        // logger.trace("getHeader END");
        return header;
    }

    /**
     * <p>
     * createResumptionTokenAndElement.
     * </p>
     *
     * @param hits a long.
     * @param cursor a int.
     * @param xmlns a {@link org.jdom2.Namespace} object.
     * @param handler a {@link io.goobi.viewer.connector.oai.RequestHandler} object.
     * @return a {@link org.jdom2.Element} object.
     */
    protected static Element createResumptionTokenAndElement(long hits, int cursor, Namespace xmlns, RequestHandler handler) {
        return createResumptionTokenAndElement(hits, hits, cursor, cursor, xmlns, handler);
    }

    /**
     * <p>
     * createResumptionTokenAndElement.
     * </p>
     *
     * @param virtualHits a long.
     * @param rawHits a long.
     * @param virtualCursor a int.
     * @param rawCursor a int.
     * @param xmlns a {@link org.jdom2.Namespace} object.
     * @param handler a {@link io.goobi.viewer.connector.oai.RequestHandler} object.
     * @return a {@link org.jdom2.Element} object.
     */
    protected static Element createResumptionTokenAndElement(long virtualHits, long rawHits, int virtualCursor, int rawCursor, Namespace xmlns,
            RequestHandler handler) {
        long now = System.currentTimeMillis();
        long time = now + expiration;
        ResumptionToken token = new ResumptionToken(ResumptionToken.TOKEN_NAME_PREFIX + System.currentTimeMillis(), virtualHits, rawHits,
                virtualCursor, rawCursor, time, handler);
        try {
            saveToken(token);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        Element eleResumptionToken = new Element("resumptionToken", xmlns);
        eleResumptionToken.setAttribute("expirationDate", Utils.convertDate(time));
        eleResumptionToken.setAttribute("completeListSize", String.valueOf(virtualHits));
        eleResumptionToken.setAttribute("cursor", String.valueOf(virtualCursor));
        eleResumptionToken.setText(token.getTokenName());

        return eleResumptionToken;
    }

    /**
     * 
     * @param token
     * @throws IOException
     */
    private static void saveToken(ResumptionToken token) throws IOException {
        File f = new File(DataManager.getInstance().getConfiguration().getResumptionTokenFolder(), token.getTokenName());
        XStream xStream = new XStream(new DomDriver());
        xStream.allowTypesByWildcard(new String[] { "io.goobi.viewer.**" });
        try (BufferedWriter outfile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            xStream.toXML(token, outfile);
            outfile.flush();
        }
    }

    /**
     * handle token
     *
     * @param resumptionToken a {@link java.lang.String} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return a {@link org.jdom2.Element} object.
     * @should return error if resumption token name illegal
     */
    public static Element handleToken(String resumptionToken, String filterQuerySuffix) {
        if (resumptionToken == null) {
            throw new IllegalArgumentException("resumptionToken may not be null");
        }

        logger.debug("Loading resumption token {}", resumptionToken.replaceAll("[\n\r\t]", "_"));
        Matcher m = ResumptionToken.TOKEN_NAME_PATTERN.matcher(resumptionToken);
        if (!m.find()) {
            logger.warn("Illegal resumption token name: {}", resumptionToken);
            return new ErrorCode().getBadResumptionToken();
        }
        File f = new File(DataManager.getInstance().getConfiguration().getResumptionTokenFolder(), resumptionToken);
        if (!f.exists()) {
            logger.warn("Requested resumption token not found: {}", f.getName());
            return new ErrorCode().getBadResumptionToken();
        }

        try {
            ResumptionToken token = deserializeResumptionToken(f);
            Map<String, String> params = Utils.filterDatestampFromRequest(token.getHandler());

            //            boolean urnOnly = false;
            long totalHits = 0;
            String versionDiscriminatorField = DataManager.getInstance()
                    .getConfiguration()
                    .getVersionDisriminatorFieldForMetadataFormat(token.getHandler().getMetadataPrefix().name());

            Format format = Format.getFormatByMetadataPrefix(token.getHandler().getMetadataPrefix());
            if (format == null) {
                logger.error("Bad metadataPrefix: {}", token.getHandler().getMetadataPrefix());
                return new ErrorCode().getCannotDisseminateFormat();
            }
            totalHits = format.getTotalHits(params, versionDiscriminatorField, filterQuerySuffix);
            if (token.getHits() != totalHits) {
                logger.warn("Hits size in the token ({}) does not equal the reported total hits number ({}).", token.getHits(), totalHits);
                return new ErrorCode().getBadResumptionToken();
            }
            int hitsPerToken =
                    DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(token.getHandler().getMetadataPrefix().name());

            if (token.getHandler().getVerb().equals(Verb.ListIdentifiers)) {
                return format.createListIdentifiers(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(), hitsPerToken,
                        versionDiscriminatorField, filterQuerySuffix);
            } else if (token.getHandler().getVerb().equals(Verb.ListRecords)) {
                //                Metadata md = token.getHandler().getMetadataPrefix();
                return format.createListRecords(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(), hitsPerToken,
                        versionDiscriminatorField, filterQuerySuffix);
            }
        } catch (StreamException | ConversionException e) {
            // File cannot be de-serialized, so just delete it
            logger.warn("Token '{}' could not be read, deleting...", f.getName());
            FileUtils.deleteQuietly(f);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return new ErrorCode().getBadResumptionToken();
    }

    /**
     * 
     * @param tokenFile
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @should deserialize token correctly
     */
    static ResumptionToken deserializeResumptionToken(File tokenFile) throws FileNotFoundException, IOException {
        try (FileInputStream fis = new FileInputStream(tokenFile); InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader infile = new BufferedReader(isr);) {
            XStream xStream = new XStream(new DomDriver());
            xStream.allowTypesByWildcard(new String[] { "io.goobi.viewer.**" });
            xStream.processAnnotations(ResumptionToken.class);
            return (ResumptionToken) xStream.fromXML(infile);
        }
    }

    /**
     * <p>
     * removeExpiredTokens.
     * </p>
     */
    public static void removeExpiredTokens() {
        File tokenFolder = new File(DataManager.getInstance().getConfiguration().getResumptionTokenFolder());
        if (tokenFolder.isDirectory()) {
            int count = 0;
            XStream xStream = new XStream(new DomDriver());
            xStream.allowTypesByWildcard(new String[] { "io.goobi.viewer.**" });
            for (File tokenFile : tokenFolder.listFiles()) {
                if (tokenFile.isDirectory()) {
                    continue;
                }
                try {
                    ResumptionToken token = deserializeResumptionToken(tokenFile);
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
                    logger.error(e.getMessage());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (count > 0) {
                logger.info("{} expired resumption token(s) removed.", count);
            }
        }
    }

    /**
     * Returns an instance of a format matching the given metadata prefix.
     *
     * @param metadataPrefix a {@link io.goobi.viewer.connector.oai.enums.Metadata} object.
     * @return Format instance; null if prefix not recognized
     */
    public static Format getFormatByMetadataPrefix(Metadata metadataPrefix) {
        if (metadataPrefix == null) {
            return null;
        }

        switch (metadataPrefix) {
            case oai_dc:
                return new OAIDCFormat();
            case ese:
                return new EuropeanaFormat();
            case mets:
                return new METSFormat();
            case marcxml:
                return new MARCXMLFormat();
            case epicur:
                return new EpicurFormat();
            case lido:
                return new LIDOFormat();
            case iv_overviewpage:
            case iv_crowdsourcing:
                return new GoobiViewerUpdateFormat();
            case tei:
            case cmdi:
                return new TEIFormat();
            default:
                return null;
        }

    }
}
