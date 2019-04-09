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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;

import de.intranda.digiverso.m2m.DataManager;
import de.intranda.digiverso.m2m.messages.MessageResourceBundle;
import de.intranda.digiverso.m2m.oai.RequestHandler;
import de.intranda.digiverso.m2m.oai.enums.Metadata;
import de.intranda.digiverso.m2m.oai.enums.Verb;
import de.intranda.digiverso.m2m.oai.model.ErrorCode;
import de.intranda.digiverso.m2m.oai.model.ResumptionToken;
import de.intranda.digiverso.m2m.oai.model.Set;
import de.intranda.digiverso.m2m.oai.model.language.Language;
import de.intranda.digiverso.m2m.utils.SolrConstants;
import de.intranda.digiverso.m2m.utils.SolrSearchIndex;
import de.intranda.digiverso.m2m.utils.Utils;

public abstract class AbstractFormat {

    private final static Logger logger = LoggerFactory.getLogger(AbstractFormat.class);

    protected static final Namespace XML = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");
    protected static final Namespace XSI = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

    protected static long expiration = 259200000L; // 3 days

    protected SolrSearchIndex solr = DataManager.getInstance().getSearchIndex();

    /**
     * 
     * @param handler
     * @param firstVirtualRow
     * @param firstRawRow
     * @param numRows
     * @param versionDiscriminatorField
     * @return
     * @throws SolrServerException
     */
    public abstract Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows,
            String versionDiscriminatorField) throws IOException, SolrServerException;

    /**
     * 
     * @param handler
     * @return
     */
    public abstract Element createGetRecord(RequestHandler handler);

    /**
     * 
     * @param params
     * @param versionDiscriminatorField
     * @return
     * @throws IOException
     * @throws SolrServerException
     */
    public abstract long getTotalHits(Map<String, String> params, String versionDiscriminatorField) throws IOException, SolrServerException;

    /**
     * for the server request ?verb=Identify this method build the xml section in the Identify element
     * 
     * @return the identify Element for the xml tree
     * @throws SolrServerException
     */
    public static Element getIdentifyXML() throws SolrServerException {
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
        earliestDatestamp.setText(DataManager.getInstance().getSearchIndex().getEarliestRecordDatestamp());
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
     * @return
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
     * @param handler
     * @return
     * @throws SolrServerException
     */
    public static Element createListSets(Locale locale) throws SolrServerException {
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
     * for the server request ?verb=ListIdentifiers this method build the xml section
     * 
     * @param handler
     * @param firstVirtualRow
     * @param firstRawRow
     * @param numRows
     * @param fieldStatistics
     * @return
     * @throws SolrServerException
     */
    public static Element createListIdentifiers(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows,
            String versionDiscriminatorField) throws SolrServerException {
        Map<String, String> datestamp = Utils.filterDatestampFromRequest(handler);

        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element xmlListIdentifiers = new Element("ListIdentifiers", xmlns);

        QueryResponse qr;
        long totalVirtualHits;
        int fetchedVirtualHits = 0;
        long totalRawHits;
        if (StringUtils.isNotEmpty(versionDiscriminatorField)) {
            // One OAI record for each record version
            qr = DataManager.getInstance()
                    .getSearchIndex()
                    .getListIdentifiers(datestamp, firstRawRow, numRows, " AND " + versionDiscriminatorField + ":*",
                            Collections.singletonList(versionDiscriminatorField));
            if (qr.getResults().isEmpty()) {
                return new ErrorCode().getNoRecordsMatch();
            }
            totalVirtualHits = SolrSearchIndex.getFieldCount(qr, versionDiscriminatorField);
            totalRawHits = qr.getResults().getNumFound();
            for (SolrDocument doc : qr.getResults()) {
                List<String> versions = SolrSearchIndex.getMetadataValues(doc, versionDiscriminatorField);
                for (String version : versions) {
                    String iso3code = version;
                    // Make sure to add the ISO-3 language code
                    if (SolrConstants.LANGUAGE.equals(versionDiscriminatorField) && iso3code.length() == 2) {
                        Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(version);
                        if (lang != null) {
                            iso3code = lang.getIsoCode();
                        }
                    }
                    Element header = getHeader(doc, null, handler, iso3code);
                    xmlListIdentifiers.addContent(header);
                    fetchedVirtualHits++;
                }
            }
        } else {
            // One OAI record for each record proper
            qr = DataManager.getInstance().getSearchIndex().getListIdentifiers(datestamp, firstRawRow, numRows, null, null);
            if (qr.getResults().isEmpty()) {
                return new ErrorCode().getNoRecordsMatch();
            }
            totalVirtualHits = totalRawHits = qr.getResults().getNumFound();
            for (SolrDocument doc : qr.getResults()) {
                Element header = getHeader(doc, null, handler, null);
                xmlListIdentifiers.addContent(header);
            }
        }

        // Create resumption token
        if (totalRawHits > firstRawRow + numRows) {
            Element resumption = createResumptionTokenAndElement(totalVirtualHits, totalRawHits, firstVirtualRow + fetchedVirtualHits,
                    firstRawRow + numRows, xmlns, handler);
            xmlListIdentifiers.addContent(resumption);
        }

        return xmlListIdentifiers;
    }

    /**
     * creates root element for oai protocol
     * 
     * @param elementName
     * @return
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
     * @return
     * @throws SolrServerException
     */
    protected static Element getHeader(SolrDocument doc, SolrDocument topstructDoc, RequestHandler handler, String requestedVersion)
            throws SolrServerException {
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
            identifier.setText(DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier")
                    + (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT) + (StringUtils.isNotEmpty(requestedVersion) ? '_' + requestedVersion : ""));
            header.addContent(identifier);
        }
        // datestamp
        Element datestamp = new Element("datestamp", xmlns);
        long untilTimestamp = RequestHandler.getUntilTimestamp(handler.getUntil());
        long timestampModified = SolrSearchIndex.getLatestValidDateUpdated(topstructDoc != null ? topstructDoc : doc, untilTimestamp);
        datestamp.setText(Utils.parseDate(timestampModified));
        if (StringUtils.isEmpty(datestamp.getText()) && doc.getFieldValue(SolrConstants.ISANCHOR) != null) {
            datestamp.setText(Utils.parseDate(DataManager.getInstance().getSearchIndex().getLatestVolumeTimestamp(doc, untilTimestamp)));
        }
        header.addContent(datestamp);
        // setSpec
        if (StringUtils.isNotEmpty(handler.getSet())) {
            // setSpec from handler
            Element setSpec = new Element("setSpec", xmlns);
            setSpec.setText(handler.getSet());
            header.addContent(setSpec);
        } else if (handler.getMetadataPrefix() != null) {
            // setSpec from config
            List<String> setSpecFields =
                    DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(handler.getMetadataPrefix().name());
            if (!setSpecFields.isEmpty()) {
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

        return header;
    }

    /**
     * 
     * @param hits
     * @param cursor
     * @param xmlns
     * @param handler
     * @return
     */
    protected static Element createResumptionTokenAndElement(long hits, int cursor, Namespace xmlns, RequestHandler handler) {
        return createResumptionTokenAndElement(hits, hits, cursor, cursor, xmlns, handler);
    }

    /**
     * 
     * @param virtualHits
     * @param rawHits
     * @param virtualCursor
     * @param rawCursor
     * @param xmlns
     * @param handler
     * @return
     */
    protected static Element createResumptionTokenAndElement(long virtualHits, long rawHits, int virtualCursor, int rawCursor, Namespace xmlns,
            RequestHandler handler) {
        long now = System.currentTimeMillis();
        long time = now + expiration;
        ResumptionToken token =
                new ResumptionToken("oai_" + System.currentTimeMillis(), virtualHits, rawHits, virtualCursor, rawCursor, time, handler);
        try {
            saveToken(token);
        } catch (IOException e) {
            // do nothing
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
        try (BufferedWriter outfile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF8"))) {
            xStream.toXML(token, outfile);
            outfile.flush();
        }
    }

    /**
     * handle token
     * 
     * @param resumptionToken
     * @return
     */
    public static Element handleToken(String resumptionToken) {
        logger.debug("Loading resumption token {}", resumptionToken);
        File f = new File(DataManager.getInstance().getConfiguration().getResumptionTokenFolder(), resumptionToken);
        if (!f.exists()) {
            logger.warn("Requested resumption token not found: {}", f.getName());
            return new ErrorCode().getBadResumptionToken();
        }

        try (FileInputStream fis = new FileInputStream(f); InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader infile = new BufferedReader(isr);) {
            XStream xStream = new XStream(new DomDriver());
            xStream.processAnnotations(ResumptionToken.class);
            ResumptionToken token = (ResumptionToken) xStream.fromXML(infile);
            Map<String, String> params = Utils.filterDatestampFromRequest(token.getHandler());

            boolean urnOnly = false;
            long totalHits = 0;
            String versionDiscriminatorField = DataManager.getInstance()
                    .getConfiguration()
                    .getVersionDisriminatorFieldForMetadataFormat(token.getHandler().getMetadataPrefix().name());
            switch (token.getHandler().getMetadataPrefix()) {
                // Query the Goobi viewer for the total hits number
                case mets:
                case marcxml:
                    totalHits = new METSFormat().getTotalHits(params, versionDiscriminatorField);
                    break;
                case lido:
                    totalHits = new LIDOFormat().getTotalHits(params, versionDiscriminatorField);
                    break;
                case iv_crowdsourcing:
                case iv_overviewpage:
                    totalHits = new GoobiViewerUpdateFormat().getTotalHits(params, versionDiscriminatorField);
                    break;
                case epicur:
                    totalHits = new EpicurFormat().getTotalHits(params, versionDiscriminatorField);
                    break;
                case oai_dc:
                case ese:
                    totalHits = new OAIDCFormat().getTotalHits(params, versionDiscriminatorField);
                    break;
                case tei:
                case cmdi:
                    totalHits = new TEIFormat().getTotalHits(params, versionDiscriminatorField);
                    break;
                default:
                    totalHits = DataManager.getInstance().getSearchIndex().getTotalHitNumber(params, urnOnly, "", null);
                    break;
            }

            if (token.getHits() != totalHits) {
                logger.warn("Hits size in the token ({}) does not equal the reported total hits number ({}).", token.getHits(), totalHits);
                return new ErrorCode().getBadResumptionToken();
            }
            int hitsPerToken =
                    DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(token.getHandler().getMetadataPrefix().name());
            if (token.getHandler().getVerb().equals(Verb.ListIdentifiers)) {
                return createListIdentifiers(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(), hitsPerToken,
                        versionDiscriminatorField);
            } else if (token.getHandler().getVerb().equals(Verb.ListRecords)) {
                Metadata md = token.getHandler().getMetadataPrefix();
                switch (md) {
                    case oai_dc:
                        return new OAIDCFormat().createListRecords(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(), hitsPerToken,
                                versionDiscriminatorField);
                    case ese:
                        return new EuropeanaFormat().createListRecords(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(),
                                hitsPerToken, versionDiscriminatorField);
                    case epicur:
                        return new EpicurFormat().createListRecords(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(), hitsPerToken,
                                versionDiscriminatorField);
                    case mets:
                        return new METSFormat().createListRecords(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(), hitsPerToken,
                                versionDiscriminatorField);
                    case marcxml:
                        return new MARCXMLFormat().createListRecords(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(), hitsPerToken,
                                versionDiscriminatorField);
                    case lido:
                        return new LIDOFormat().createListRecords(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(), hitsPerToken,
                                versionDiscriminatorField);
                    case iv_overviewpage:
                    case iv_crowdsourcing:
                        return new GoobiViewerUpdateFormat().createListRecords(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(),
                                hitsPerToken, versionDiscriminatorField);
                    case tei:
                    case cmdi:
                        return new TEIFormat().createListRecords(token.getHandler(), token.getVirtualCursor(), token.getRawCursor(), hitsPerToken,
                                versionDiscriminatorField);
                    default:
                        return new ErrorCode().getCannotDisseminateFormat();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return new ErrorCode().getBadResumptionToken();
    }

    /**
     * 
     */
    public static void removeExpiredTokens() {
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
}
