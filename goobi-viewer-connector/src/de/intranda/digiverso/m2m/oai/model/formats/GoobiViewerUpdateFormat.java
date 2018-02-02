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
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.m2m.DataManager;
import de.intranda.digiverso.m2m.oai.RequestHandler;
import de.intranda.digiverso.m2m.oai.enums.Metadata;
import de.intranda.digiverso.m2m.oai.model.ErrorCode;
import de.intranda.digiverso.m2m.utils.SolrConstants;
import de.intranda.digiverso.m2m.utils.Utils;

/**
 * Crowdsourcing and overview page data.
 */
public class GoobiViewerUpdateFormat extends AbstractFormat {

    private static final Logger logger = LoggerFactory.getLogger(GoobiViewerUpdateFormat.class);

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#createListRecords(de.intranda.digiverso.m2m.oai.RequestHandler, int, int)
     */
    @Override
    public Element createListRecords(RequestHandler handler, int firstRow, int numRows) throws IOException, SolrServerException {
        StringBuilder sbUrl = new StringBuilder(100);
        sbUrl.append(DataManager.getInstance()
                .getConfiguration()
                .getHarvestUrl())
                .append("?action=");
        switch (handler.getMetadataPrefix()) {
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
            sbUrl.append("&from=")
                    .append(handler.getFrom());
        }
        if (handler.getUntil() != null) {
            sbUrl.append("&until=")
                    .append(handler.getUntil());
        }
        sbUrl.append("&first=")
                .append(firstRow)
                .append("&pageSize=")
                .append(numRows);

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
            return generateGoobiViewerUpdates(jsonArray, totalHits, firstRow, numRows, handler, "ListRecords");
        } catch (JDOMException e) {
            throw new IOException(e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#createGetRecord(de.intranda.digiverso.m2m.oai.RequestHandler)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Element createGetRecord(RequestHandler handler) {
        logger.trace("createGetRecord: {}", handler.getIdentifier());
        if (handler.getIdentifier() == null) {
            return new ErrorCode().getBadArgument();
        }
        try {
            StringBuilder sbUrlRoot = new StringBuilder(DataManager.getInstance()
                    .getConfiguration()
                    .getHarvestUrl()).append('?');
            if (handler.getFrom() != null) {
                sbUrlRoot.append("&from=")
                        .append(handler.getFrom());
            }
            if (handler.getUntil() != null) {
                sbUrlRoot.append("&until=")
                        .append(handler.getUntil());
            }
            sbUrlRoot.append("&identifier=")
                    .append(handler.getIdentifier())
                    .append("&action=");
            String urlRoot = sbUrlRoot.toString();
            switch (handler.getMetadataPrefix()) {
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
                    logger.trace("Unknown metadata format: {}", handler.getMetadataPrefix()
                            .getMetadataPrefix());
                    return new ErrorCode().getCannotDisseminateFormat();
            }
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("id", handler.getIdentifier());
            jsonArray.add(jsonObj);
            return generateGoobiViewerUpdates(jsonArray, 1L, 0, 1, handler, "GetRecord");
        } catch (IOException e) {
            return new ErrorCode().getIdDoesNotExist();
        } catch (JDOMException e) {
            return new ErrorCode().getCannotDisseminateFormat();
        }
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
    private Element generateGoobiViewerUpdates(JSONArray jsonArray, long totalHits, int firstRow, final int numRows, RequestHandler handler,
            String recordType) throws JDOMException, IOException {
        if (jsonArray == null) {
            throw new IllegalArgumentException("jsonArray may not be null");
        }
        if (handler.getMetadataPrefix() == null) {
            throw new IllegalArgumentException("metadataPrefix may not be null");
        }
        logger.trace("generateIntrandaViewerUpdates: {}", handler.getMetadataPrefix()
                .getMetadataPrefix());

        Namespace xmlns = DataManager.getInstance()
                .getConfiguration()
                .getStandardNameSpace();
        Namespace nsOverviewPage =
                Namespace.getNamespace(Metadata.iv_overviewpage.getMetadataPrefix(), Metadata.iv_overviewpage.getMetadataNamespace());
        Namespace nsCrowdsourcingUpdates =
                Namespace.getNamespace(Metadata.iv_crowdsourcing.getMetadataPrefix(), Metadata.iv_crowdsourcing.getMetadataNamespace());
        Element xmlListRecords = new Element(recordType, xmlns);

        int useNumRows = numRows;
        if (jsonArray.size() < useNumRows) {
            useNumRows = jsonArray.size();
        }
        StringBuilder sbUrlRoot = new StringBuilder(DataManager.getInstance()
                .getConfiguration()
                .getHarvestUrl()).append('?');
        if (handler.getFrom() != null) {
            sbUrlRoot.append("&from=")
                    .append(handler.getFrom());
        }
        if (handler.getUntil() != null) {
            sbUrlRoot.append("&until=")
                    .append(handler.getUntil());
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
            eleDatestamp.setText(Utils.parseDate(timestamp));
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
                sb.append(SolrConstants.PI)
                        .append(':')
                        .append(identifier);
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
                switch (handler.getMetadataPrefix()) {
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

    /* (non-Javadoc)
     * @see de.intranda.digiverso.m2m.oai.model.formats.AbstractFormat#getTotalHits(java.util.Map)
     */
    @Override
    public long getTotalHits(Map<String, String> params) throws IOException, SolrServerException {
        String url = DataManager.getInstance()
                .getConfiguration()
                .getHarvestUrl() + "?action=getlist_"
                + params.get("metadataPrefix")
                        .substring(3);
        String rawJSON = Utils.getWebContent(url);
        if (StringUtils.isNotEmpty(rawJSON)) {
            try {
                JSONArray jsonArray = (JSONArray) new JSONParser().parse(rawJSON);
                return (long) jsonArray.get(0);
            } catch (ParseException e) {
                logger.error(e.getMessage());
                throw new IOException(e.getMessage());
            }
        }

        return 0;
    }

}
