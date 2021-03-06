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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.enums.Verb;
import io.goobi.viewer.connector.oai.model.ErrorCode;
import io.goobi.viewer.connector.utils.SolrConstants;
import io.goobi.viewer.connector.utils.SolrSearchIndex;
import io.goobi.viewer.connector.utils.SolrSearchTools;
import io.goobi.viewer.connector.utils.Utils;

/**
 * Xepicur
 */
public class EpicurFormat extends Format {

    private static final Logger logger = LoggerFactory.getLogger(EpicurFormat.class);

    private static Namespace EPICUR = Namespace.getNamespace("epicur", "urn:nbn:de:1111-2004033116");

    private static String[] FIELDS =
            { SolrConstants.DATECREATED, SolrConstants.DATEUPDATED, SolrConstants.DATEDELETED, SolrConstants.PI, SolrConstants.PI_TOPSTRUCT,
                    SolrConstants.URN };

    private List<String> setSpecFields = DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.epicur.name());

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#createListRecords(io.goobi.viewer.connector.oai.RequestHandler, int, int, int, java.lang.String)
     */
    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     */
    @Override
    public Element createListRecords(RequestHandler handler, int firstVirtualRow, int firstRawRow, int numRows, String versionDiscriminatorField)
            throws SolrServerException, IOException {
        logger.trace("createListRecords");
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();

        String urnPrefixBlacklistSuffix =
                SolrSearchTools.getUrnPrefixBlacklistSuffix(DataManager.getInstance().getConfiguration().getUrnPrefixBlacklist());
        String querySuffix = urnPrefixBlacklistSuffix
                + SolrSearchTools.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes());
        List<String> fieldList = new ArrayList<>(Arrays.asList(FIELDS));
        fieldList.addAll(setSpecFields);
        QueryResponse qr =
                solr.getListRecords(Utils.filterDatestampFromRequest(handler), firstRawRow, numRows, true, querySuffix, fieldList, null);
        SolrDocumentList records = qr.getResults();
        if (records.isEmpty()) {
            return new ErrorCode().getNoRecordsMatch();
        }
        Element xmlListRecords = new Element("ListRecords", xmlns);
        if (records.size() < numRows) {
            numRows = records.size();
        }
        int pagecount = 0;
        for (SolrDocument doc : records) {
            long dateUpdated = SolrSearchTools.getLatestValidDateUpdated(doc, RequestHandler.getUntilTimestamp(handler.getUntil()));
            Long dateDeleted = (Long) doc.getFieldValue(SolrConstants.DATEDELETED);
            if (doc.getFieldValue(SolrConstants.URN) != null) {
                Element record = new Element("record", xmlns);
                Element header = generateEpicurHeader(doc, dateUpdated, setSpecFields);
                record.addContent(header);
                Element metadata = new Element("metadata", xmlns);
                record.addContent(metadata);
                boolean topstruct = doc.containsKey(SolrConstants.PI);
                metadata.addContent(generateEpicurElement((String) doc.getFieldValue(SolrConstants.URN),
                        (Long) doc.getFieldValue(SolrConstants.DATECREATED), dateUpdated, dateDeleted, topstruct));
                xmlListRecords.addContent(record);
            }

            if (dateDeleted == null) {
                // Page elements for existing record
                StringBuilder sbPageQuery = new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':')
                        .append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT))
                        .append(" AND ")
                        .append(SolrConstants.DOCTYPE)
                        .append(":PAGE")
                        .append(" AND ")
                        .append(SolrConstants.IMAGEURN)
                        .append(":*");
                sbPageQuery.append(urnPrefixBlacklistSuffix);
                QueryResponse qrInner = solr.search(sbPageQuery.toString(), 0, SolrSearchIndex.MAX_HITS,
                        Collections.singletonList(SolrConstants.ORDER), Collections.singletonList(SolrConstants.IMAGEURN), null);
                if (qrInner != null && !qrInner.getResults().isEmpty()) {
                    for (SolrDocument pageDoc : qrInner.getResults()) {
                        String imgUrn = (String) pageDoc.getFieldValue(SolrConstants.IMAGEURN);
                        Element pagerecord = new Element("record", xmlns);
                        Element pageheader = generateEpicurPageHeader(doc, imgUrn, dateUpdated, setSpecFields);
                        pagerecord.addContent(pageheader);
                        Element pagemetadata = new Element("metadata", xmlns);
                        pagerecord.addContent(pagemetadata);
                        pagemetadata.addContent(generateEpicurPageElement(imgUrn, (Long) doc.getFieldValue(SolrConstants.DATECREATED), dateUpdated,
                                (Long) doc.getFieldValue(SolrConstants.DATEDELETED)));
                        xmlListRecords.addContent(pagerecord);
                        pagecount++;
                    }
                    logger.trace("Found {} page records for {}", qrInner.getResults().size(), doc.getFieldValue(SolrConstants.PI_TOPSTRUCT));
                }
            } else {
                // Page elements for deleted record (only deleted record docs will have IMAGEURN_OAI!)
                Collection<Object> pageUrnValues = doc.getFieldValues(SolrConstants.IMAGEURN_OAI);
                if (pageUrnValues != null) {
                    for (Object obj : pageUrnValues) {
                        String imgUrn = (String) obj;
                        Element pagerecord = new Element("record", xmlns);
                        Element pageheader = generateEpicurPageHeader(doc, imgUrn, dateUpdated, setSpecFields);
                        pagerecord.addContent(pageheader);
                        Element pagemetadata = new Element("metadata", xmlns);
                        pagerecord.addContent(pagemetadata);
                        pagemetadata.addContent(
                                generateEpicurPageElement(imgUrn, (Long) doc.getFieldValue(SolrConstants.DATECREATED), dateUpdated, dateDeleted));
                        xmlListRecords.addContent(pagerecord);
                        pagecount++;
                    }
                }
            }
        }
        logger.debug("Found {} page records total", pagecount);

        // Create resumption token
        if (records.getNumFound() > firstRawRow + numRows) {
            Element resumption = createResumptionTokenAndElement(records.getNumFound(), firstRawRow + numRows, xmlns, handler);
            xmlListRecords.addContent(resumption);
        }

        return xmlListRecords;
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
        List<String> fieldList = new ArrayList<>(Arrays.asList(FIELDS));
        fieldList.addAll(setSpecFields);
        try {
            SolrDocument doc = solr.getListRecord(handler.getIdentifier(), fieldList);
            if (doc == null) {
                return new ErrorCode().getIdDoesNotExist();
            }
            Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
            Element getRecord = new Element("GetRecord", xmlns);
            Element record = new Element("record", xmlns);
            long dateupdated = SolrSearchTools.getLatestValidDateUpdated(doc, RequestHandler.getUntilTimestamp(handler.getUntil()));
            Element header = generateEpicurPageHeader(doc, handler.getIdentifier(), dateupdated, setSpecFields);
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

    /**
     * generates header for epicur format
     * 
     * @param doc
     * @param dateUpdated
     * @param setSpecFields
     * @return
     */
    private static Element generateEpicurHeader(SolrDocument doc, long dateUpdated, List<String> setSpecFields) {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element header = new Element("header", xmlns);
        Element identifier = new Element("identifier", xmlns);
        identifier.setText(
                DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier") + (String) doc.getFieldValue("URN"));
        header.addContent(identifier);

        Element datestamp = new Element("datestamp", xmlns);
        datestamp.setText(Utils.parseDate(dateUpdated));
        header.addContent(datestamp);
        // setSpec
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
     * @param urn
     * @param dateCreated
     * @param dateUpdated
     * @param dateDeleted
     * @param topstruct
     * @return
     */
    private static Element generateEpicurElement(String urn, Long dateCreated, Long dateUpdated, Long dateDeleted, boolean topstruct) {
        Namespace xmlns = Namespace.getNamespace("urn:nbn:de:1111-2004033116");

        Element epicur = new Element("epicur", xmlns);
        epicur.addNamespaceDeclaration(XSI);
        epicur.addNamespaceDeclaration(EPICUR);
        epicur.setAttribute("schemaLocation", "urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd", XSI);
        String status = "urn_new";

        // xsi:schemaLocation="urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd"
        // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        // xmlns:epicur="urn:nbn:de:1111-2004033116"
        // xmlns="urn:nbn:de:1111-2004033116"

        if (dateDeleted != null) {
            // "url_delete" is no longer allowed
            status = "url_update_general";
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
        // add no resource element if the record is deleted
        if (dateDeleted == null) {
            record.addContent(resource);
        }

        Element identifier = new Element("identifier", xmlns);
        identifier.setAttribute("origin", "original");
        identifier.setAttribute("role", "primary");
        identifier.setAttribute("scheme", "url");
        if (topstruct) {
            identifier.setAttribute("type", "frontpage");
        }

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
     * @param doc
     * @param urn
     * @param dateUpdated
     * @param setSpecFields
     * @return
     */
    private static Element generateEpicurPageHeader(SolrDocument doc, String urn, long dateUpdated, List<String> setSpecFields) {
        Namespace xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
        Element header = new Element("header", xmlns);

        Element identifier = new Element("identifier", xmlns);
        identifier.setText(DataManager.getInstance().getConfiguration().getOaiIdentifier().get("repositoryIdentifier") + urn);
        header.addContent(identifier);

        Element datestamp = new Element("datestamp", xmlns);
        datestamp.setText(Utils.parseDate(dateUpdated));
        header.addContent(datestamp);
        // setSpec
        if (setSpecFields != null && !setSpecFields.isEmpty()) {
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
     * @param urn
     * @param dateCreated
     * @param dateUpdated
     * @param dateDeleted
     * @return
     */
    private static Element generateEpicurPageElement(String urn, Long dateCreated, Long dateUpdated, Long dateDeleted) {
        Namespace xmlns = Namespace.getNamespace("urn:nbn:de:1111-2004033116");

        Element epicur = new Element("epicur", xmlns);
        epicur.addNamespaceDeclaration(XSI);
        epicur.addNamespaceDeclaration(EPICUR);
        epicur.setAttribute("schemaLocation", "urn:nbn:de:1111-2004033116 http://www.persistent-identifier.de/xepicur/version1.0/xepicur.xsd", XSI);
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
        //        identifier.setAttribute("type", "frontpage");

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
     * Generates administrative_data section in epicur.
     * 
     * @param status
     * @param xmlns
     * @return
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.connector.oai.model.formats.AbstractFormat#getTotalHits(java.util.Map, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public long getTotalHits(Map<String, String> params, String versionDiscriminatorField) throws IOException, SolrServerException {
        // Hit count may differ for epicur
        String querySuffix = SolrSearchTools.getUrnPrefixBlacklistSuffix(DataManager.getInstance().getConfiguration().getUrnPrefixBlacklist());
        if (!Verb.ListIdentifiers.getTitle().equals(params.get("verb"))) {
            querySuffix +=
                    SolrSearchTools.getAdditionalDocstructsQuerySuffix(DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes());
        }
        // Query Solr index for the total hits number
        return solr.getTotalHitNumber(params, true, querySuffix, null);
    }

}
