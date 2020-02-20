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
package io.goobi.viewer.connector.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.model.LicenseType;
import io.goobi.viewer.connector.oai.model.Set;

/**
 * <p>SolrSearchIndex class.</p>
 *
 */
public class SolrSearchIndex {

    /** Logger for this class. */
    final static Logger logger = LoggerFactory.getLogger(SolrSearchIndex.class);

    /** Constant <code>MAX_HITS=Integer.MAX_VALUE</code> */
    public static final int MAX_HITS = Integer.MAX_VALUE;
    private static final int TIMEOUT_SO = 10000;
    private static final int TIMEOUT_CONNECTION = 10000;

    private SolrServer server;
    private final boolean testMode;

    /**
     * <p>Constructor for SolrSearchIndex.</p>
     *
     * @param server a {@link org.apache.solr.client.solrj.SolrServer} object.
     * @param testMode a boolean.
     */
    public SolrSearchIndex(SolrServer server, boolean testMode) {
        this.testMode = testMode;
        if (server == null) {
            if (StringUtils.isEmpty(DataManager.getInstance().getConfiguration().getIndexUrl())) {
                logger.error("Solr URL is not configured. Cannot instantiate the OAI-PMH interface.");
                return;
            }
            this.server = getNewHttpSolrServer(DataManager.getInstance().getConfiguration().getIndexUrl());
        } else {
            this.server = server;
        }
    }

    /**
     * Checks whether the server's configured URL matches that in the config file. If not, a new server instance is created.
     */
    public void checkReloadNeeded() {
        if (!testMode && server != null && server instanceof HttpSolrServer) {
            HttpSolrServer httpSolrServer = (HttpSolrServer) server;
            if (!DataManager.getInstance().getConfiguration().getIndexUrl().equals(httpSolrServer.getBaseURL())) {
                logger.info("Solr URL has changed, re-initializing SolrHelper...");
                logger.trace("OLD: {}", httpSolrServer.getBaseURL());
                logger.trace("NEW: {}", DataManager.getInstance().getConfiguration().getIndexUrl());
                httpSolrServer.shutdown();
                server = getNewHttpSolrServer(DataManager.getInstance().getConfiguration().getIndexUrl());
            }
        }
    }

    /**
     * <p>getNewHttpSolrServer.</p>
     *
     * @param indexUrl a {@link java.lang.String} object.
     * @return a {@link org.apache.solr.client.solrj.impl.HttpSolrServer} object.
     */
    public static HttpSolrServer getNewHttpSolrServer(String indexUrl) {
        if (indexUrl == null) {
            throw new IllegalArgumentException("indexUrl may not be null");
        }
        logger.info("Initializing server with URL '{}'", indexUrl);
        HttpSolrServer server = new HttpSolrServer(indexUrl);
        server.setSoTimeout(TIMEOUT_SO); // socket read timeout
        server.setConnectionTimeout(TIMEOUT_CONNECTION);
        server.setDefaultMaxConnectionsPerHost(100);
        server.setMaxTotalConnections(100);
        server.setFollowRedirects(false); // defaults to false
        server.setAllowCompression(true);
        server.setMaxRetries(1); // defaults to 0. > 1 not recommended.
        // server.setParser(new XMLResponseParser()); // binary parser is used by default
        server.setRequestWriter(new BinaryRequestWriter());

        return server;
    }

    /**
     * <p>search.</p>
     *
     * @param query a {@link java.lang.String} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a {@link org.apache.solr.common.SolrDocumentList} object.
     */
    public SolrDocumentList search(String query) throws SolrServerException {
        SolrQuery solrQuery = new SolrQuery(query + getAllSuffixes());
        solrQuery.setRows(MAX_HITS);
        QueryResponse resp = server.query(solrQuery);

        return resp.getResults();
    }

    /**
     * Pure Solr search method.
     *
     * @param query {@link java.lang.String}
     * @param first {@link java.lang.Integer}
     * @param rows {@link java.lang.Integer}
     * @param sortFields a {@link java.util.List} object.
     * @param fieldList If not null, only the fields in the list will be returned.
     * @param params Additional query parameters.
     * @return {@link org.apache.solr.client.solrj.response.QueryResponse}
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @should return correct results
     * @should return correct number of rows
     * @should sort results correctly
     */
    public QueryResponse search(String query, int first, int rows, List<String> sortFields, List<String> fieldList, Map<String, String> params)
            throws SolrServerException {
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.setStart(first);
        solrQuery.setRows(rows - first);

        if (sortFields != null && !sortFields.isEmpty()) {
            for (String sortField : sortFields) {
                solrQuery.addSort(sortField, ORDER.asc);
            }
        }
        if (fieldList != null && !fieldList.isEmpty()) {
            for (String field : fieldList) {
                if (StringUtils.isNotEmpty(field)) {
                    solrQuery.addField(field);
                }
            }
        }
        if (params != null && !params.isEmpty())

        {
            for (String key : params.keySet()) {
                solrQuery.set(key, params.get(key));
            }
        }

        QueryResponse resp = server.query(solrQuery);

        return resp;

    }

    /**
     * searches and returns a list of {@link org.apache.solr.common.SolrDocument}
     *
     * @param from startdate
     * @param until enddate
     * @param setSpec a {@link java.lang.String} object.
     * @param metadataPrefix a {@link java.lang.String} object.
     * @param firstRow a int.
     * @param numRows a int.
     * @param urnOnly a boolean.
     * @param querySuffix a {@link java.lang.String} object.
     * @param fieldStatistics a {@link java.util.List} object.
     * @return list of hits as {@link org.apache.solr.common.SolrDocument}
     * @throws java.io.IOException
     * @throws org.apache.solr.client.solrj.SolrServerException
     */
    public QueryResponse search(String from, String until, String setSpec, String metadataPrefix, int firstRow, int numRows, boolean urnOnly,
            String querySuffix, List<String> fieldStatistics) throws IOException, SolrServerException {
        StringBuilder sbQuery = new StringBuilder(buildQueryString(from, until, setSpec, metadataPrefix, urnOnly, querySuffix));
        if (urnOnly) {
            sbQuery.append(" +(").append(SolrConstants.URN).append(":* ").append(SolrConstants.IMAGEURN_OAI).append(":*)");
        }
        sbQuery.append(getAllSuffixes());
        logger.debug("OAI query: {}", sbQuery.toString());
        logger.trace("start: {}, rows: {}", firstRow, numRows);
        SolrQuery solrQuery = new SolrQuery(sbQuery.toString());
        solrQuery.setStart(firstRow);
        solrQuery.setRows(numRows);
        solrQuery.addSort(SolrConstants.DATECREATED, ORDER.asc);
        if (fieldStatistics != null && !fieldStatistics.isEmpty()) {
            for (String field : fieldStatistics) {
                solrQuery.setGetFieldStatistics(field);
            }
        }
        QueryResponse resp = server.query(solrQuery);
        logger.debug("Total hits (Solr only): {}, fetched records {} - {}", resp.getResults().getNumFound(), firstRow,
                firstRow + resp.getResults().size() - 1);

        return resp;
    }

    /**
     * there is no difference between oai_dc or mets
     *
     * @param params a {@link java.util.Map} object.
     * @param firstRawRow a int.
     * @param numRows a int.
     * @param querySuffix a {@link java.lang.String} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @param fieldStatistics a {@link java.util.List} object.
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     */
    public QueryResponse getListIdentifiers(Map<String, String> params, int firstRawRow, int numRows, String querySuffix,
            List<String> fieldStatistics) throws SolrServerException {
        try {
            return search(params.get("from"), params.get("until"), params.get("set"), params.get("metadataPrefix"), firstRawRow, numRows, false,
                    querySuffix, fieldStatistics);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * listRecords display the whole meta-data of an element
     *
     * @param params a {@link java.util.Map} object.
     * @param firstRow a int.
     * @param numRows a int.
     * @param urnOnly a boolean.
     * @param querySuffix a {@link java.lang.String} object.
     * @param fieldStatistics a {@link java.util.List} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     */
    public QueryResponse getListRecords(Map<String, String> params, int firstRow, int numRows, boolean urnOnly, String querySuffix,
            List<String> fieldStatistics) throws SolrServerException {
        try {
            return search(params.get("from"), params.get("until"), params.get("set"), params.get("metadataPrefix"), firstRow, numRows, urnOnly,
                    querySuffix, fieldStatistics);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Searches for identifier and return {@link org.apache.solr.common.SolrDocument} identifier can be PPN or URN (doc or page)
     *
     * @param identifier Identifier to search
     * @return {@link org.apache.solr.common.SolrDocument}
     * @throws java.io.IOException
     * @throws org.apache.solr.client.solrj.SolrServerException
     */
    public SolrDocument getListRecord(final String identifier) throws IOException, SolrServerException {
        logger.trace("getListRecord");
        SolrDocumentList ret = query(identifier, 1);
        if (!ret.isEmpty()) {
            return ret.get(0);
        }

        return null;
    }

    /**
     * <p>isRecordExists.</p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a boolean.
     */
    public boolean isRecordExists(final String identifier) throws SolrServerException {
        return !query(identifier, 0).isEmpty();
    }

    /**
     * 
     * @param identifier
     * @param rows
     * @return
     * @throws SolrServerException
     */
    private SolrDocumentList query(final String identifier, int rows) throws SolrServerException {
        String useIdentifier = ClientUtils.escapeQueryChars(identifier);

        StringBuilder sb = new StringBuilder();
        sb.append('(')
                .append(SolrConstants.PI)
                .append(':')
                .append(useIdentifier)
                .append(" OR ")
                .append(SolrConstants.URN)
                .append(':')
                .append(useIdentifier)
                .append(" OR ")
                .append(SolrConstants.IMAGEURN)
                .append(':')
                .append(useIdentifier)
                .append(')');
        sb.append(getAllSuffixes());
        logger.debug(sb.toString());
        SolrQuery solrQuery = new SolrQuery(sb.toString());
        solrQuery.setRows(rows);
        QueryResponse resp = server.query(solrQuery);

        return resp.getResults();
    }

    /**
     * build the query String depending on the variables
     * 
     * @param from
     * @param until
     * @param setSpec
     * @param metadataPrefix
     * @param excludeAnchor
     * @param querySuffix
     * @return
     */
    private static String buildQueryString(String from, String until, String setSpec, String metadataPrefix, boolean excludeAnchor,
            String querySuffix) {
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("+(+(").append(SolrConstants.ISWORK).append(":true");
        if (!excludeAnchor) {
            sbQuery.append(' ').append(SolrConstants.ISANCHOR).append(":true");
        }
        sbQuery.append(' ').append(SolrConstants.DATEDELETED).append(":*)");
        if (StringUtils.isNotEmpty(querySuffix)) {
            sbQuery.append(querySuffix);
        }
        sbQuery.append(')');
        // Solr timestamp range is irrelevant for iv_* formats
        if (!Metadata.iv_overviewpage.name().equals(metadataPrefix) && !Metadata.iv_crowdsourcing.name().equals(metadataPrefix)
                && (from != null || until != null)) {
            long fromTimestamp = RequestHandler.getFromTimestamp(from);
            long untilTimestamp = RequestHandler.getUntilTimestamp(until);
            if (fromTimestamp == untilTimestamp) {
                untilTimestamp += 999;
            }
            sbQuery.append(" +")
                    .append(SolrConstants.DATEUPDATED)
                    .append(":[")
                    .append(normalizeDate(String.valueOf(fromTimestamp)))
                    .append(" TO ")
                    .append(normalizeDate(String.valueOf(untilTimestamp)))
                    .append(']');

        }
        if (setSpec != null) {
            boolean defaultSet = true;
            // Use DC as the set field by default 
            String setQuery = SolrConstants.DC + ":" + setSpec;

            // Check whether this is an additional set and if so, use its custom query
            {
                List<Set> additionalSetList = DataManager.getInstance().getConfiguration().getAdditionalSets();
                for (Set s : additionalSetList) {
                    if (s.getSetSpec().equals(setSpec)) {
                        defaultSet = false;
                        sbQuery = new StringBuilder(); // Replace query
                        setQuery = s.getSetQuery();
                        break;
                    }
                }
            }
            // Check whether this is an all-values set and if so, use its field
            if (defaultSet && setSpec.contains(":")) {
                List<Set> allValuesSetList = DataManager.getInstance().getConfiguration().getAllValuesSets();
                for (Set s : allValuesSetList) {
                    if (s.getSetName().equals(setSpec.substring(0, setSpec.indexOf(":")))) {
                        defaultSet = false;
                        setQuery = setSpec;
                        break;
                    }
                }
            }
            if (sbQuery.length() > 0) {
                sbQuery.append(" +");
            }
            sbQuery.append(setQuery);
        }

        return sbQuery.toString();
    }

    /**
     * Creates a list with all available values for the given field (minus any blacklisted values).
     *
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @should return all values
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<String> getSets(String field) throws SolrServerException {
        List<String> ret = new ArrayList<>();

        SolrQuery query = new SolrQuery();
        query.setQuery(field + ":* " + DataManager.getInstance().getConfiguration().getCollectionBlacklistFilterSuffix());
        query.setStart(0);
        query.setRows(0);
        query.addFacetField(field);
        logger.trace("Set query: {}", query.getQuery());
        QueryResponse resp = server.query(query);

        FacetField facetField = resp.getFacetField(field);
        if (facetField != null) {
            for (Count count : facetField.getValues()) {
                if (count.getCount() == 0) {
                    continue;
                }
                ret.add(count.getName());
            }
        }

        Collections.sort(ret);
        logger.trace("{} terms found for {}", ret.size(), field);
        return ret;
    }

    /**
     * <p>getTotalHitNumber.</p>
     *
     * @param params a {@link java.util.Map} object.
     * @param urnOnly a boolean.
     * @param querySuffix a {@link java.lang.String} object.
     * @param fieldStatistics a {@link java.util.List} object.
     * @return size of search
     * @throws java.io.IOException
     * @throws org.apache.solr.client.solrj.SolrServerException
     */
    public long getTotalHitNumber(Map<String, String> params, boolean urnOnly, String querySuffix, List<String> fieldStatistics)
            throws IOException, SolrServerException {
        StringBuilder sbQuery = new StringBuilder(
                buildQueryString(params.get("from"), params.get("until"), params.get("set"), params.get("metadataPrefix"), urnOnly, querySuffix));
        if (urnOnly) {
            sbQuery.append(" AND (").append(SolrConstants.URN).append(":* OR ").append(SolrConstants.IMAGEURN_OAI).append(":*)");
        }
        sbQuery.append(getAllSuffixes());
        logger.debug("OAI query: {}", sbQuery.toString());
        SolrQuery solrQuery = new SolrQuery(sbQuery.toString());
        solrQuery.setStart(0);
        solrQuery.setRows(0);
        solrQuery.addSort(SolrConstants.DATECREATED, ORDER.asc);
        if (fieldStatistics != null && !fieldStatistics.isEmpty()) {
            for (String field : fieldStatistics) {
                solrQuery.setGetFieldStatistics(field);
            }
        }
        QueryResponse resp = server.query(solrQuery);

        long num = resp.getResults().getNumFound();
        logger.debug("Total hits: {}", num);
        return num;
    }

    /**
     * <p>getEarliestRecordDatestamp.</p>
     *
     * @return a {@link java.lang.String} object.
     * @throws org.apache.solr.client.solrj.SolrServerException if any.
     */
    public String getEarliestRecordDatestamp() throws SolrServerException {
        try {
            String searchStr = SolrConstants.ISWORK + ":true" + getAllSuffixes();

            SolrQuery solrQuery = new SolrQuery(searchStr);
            solrQuery.setRows(1);
            solrQuery.addField(SolrConstants.DATECREATED);
            solrQuery.addSort(SolrConstants.DATECREATED, ORDER.asc);
            QueryResponse resp = server.query(solrQuery);

            if (resp.getResults().size() > 0) {
                SolrDocument doc = resp.getResults().get(0);
                if (doc.getFieldValue(SolrConstants.DATECREATED) != null) {
                    Date d = new Date((Long) doc.getFieldValue(SolrConstants.DATECREATED));
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");// ;YYYY-MM-DDThh:mm:ssZ
                    SimpleDateFormat hours = new SimpleDateFormat("HH:mm:ss");
                    format.setTimeZone(TimeZone.getTimeZone("GMT"));
                    hours.setTimeZone(TimeZone.getTimeZone("GMT"));
                    String yearMonthDay = format.format(d);
                    String hourMinuteSeconde = hours.format(d);

                    return yearMonthDay + "T" + hourMinuteSeconde + "Z";
                }
            }
        } catch (NullPointerException e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

    /**
     * If the given SolrDocument is an anchor, retrieve the latest DATEUPDATED timestamp value from its volumes.
     *
     * @param anchorDoc a {@link org.apache.solr.common.SolrDocument} object.
     * @param untilTimestamp a long.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a long.
     */
    public long getLatestVolumeTimestamp(SolrDocument anchorDoc, long untilTimestamp) throws SolrServerException {
        if (anchorDoc.getFieldValue(SolrConstants.ISANCHOR) != null && (Boolean) anchorDoc.getFieldValue(SolrConstants.ISANCHOR)) {
            SolrDocumentList volumes = search(
                    SolrConstants.ISWORK + ":true AND " + SolrConstants.IDDOC_PARENT + ":" + (String) anchorDoc.getFieldValue(SolrConstants.IDDOC));
            if (volumes != null) {
                long latest = 0;
                for (SolrDocument volume : volumes) {
                    long volumeTimestamp = getLatestValidDateUpdated(volume, untilTimestamp);
                    if (latest < volumeTimestamp) {
                        latest = volumeTimestamp;
                    }
                }

                if (latest > 0) {
                    return latest;
                }
            }
        }

        return -1;
    }

    /**
     * Returns the blacklist filter suffix (if enabled), followed by the user-agnostic access condition suffix.
     *
     * @return a {@link java.lang.String} object.
     */
    public static String getAllSuffixes() {
        StringBuilder sb = new StringBuilder();
        if (DataManager.getInstance().getConfiguration().isUseCollectionBlacklist()) {
            sb.append(DataManager.getInstance().getConfiguration().getCollectionBlacklistFilterSuffix());
        }
        List<LicenseType> restrictedLicenseTypes = DataManager.getInstance().getConfiguration().getRestrictedAccessConditions();
        if (!restrictedLicenseTypes.isEmpty()) {
            sb.append(" -(");
            boolean moreThanOne = false;
            for (LicenseType licenseType : restrictedLicenseTypes) {
                if (moreThanOne) {
                    sb.append(" OR ");
                }
                if (StringUtils.isNotEmpty(licenseType.getConditions())) {
                    if (licenseType.getConditions().charAt(0) == '-') {
                        // do not wrap the conditions in parentheses if it starts with a negation, otherwise it won't work
                        sb.append('(')
                                .append(licenseType.getField())
                                .append(":\"")
                                .append(licenseType.getValue())
                                .append("\" AND ")
                                .append(licenseType.getProcessedConditions())
                                .append(')');
                    } else {
                        sb.append('(')
                                .append(licenseType.getField())
                                .append(":\"")
                                .append(licenseType.getValue())
                                .append("\" AND (")
                                .append(licenseType.getProcessedConditions())
                                .append("))");
                    }
                } else {
                    sb.append(licenseType.getField()).append(':').append(licenseType.getValue());
                }
                moreThanOne = true;
            }
            sb.append(')');
        }
        String querySuffix = DataManager.getInstance().getConfiguration().getQuerySuffix();
        if (StringUtils.isNotEmpty(querySuffix)) {
            if (querySuffix.charAt(0) != ' ') {
                sb.append(' ');
            }
            sb.append(querySuffix);
        }

        return sb.toString();
    }

    /**
     * 
     * @param date
     * @return
     */
    private static String normalizeDate(String date) {
        if (date != null) {
            if (date.charAt(0) == '-' || date.length() > 12) {
                return date;
            }
            switch (date.length()) {
                case 8:
                    return "00000" + date;
                case 9:
                    return "0000" + date;
                case 10:
                    return "000" + date;
                case 11:
                    return "00" + date;
                case 12:
                    return "0" + date;
                default: // nothing
            }
        }

        return date;
    }

    /**
     * Returns the latest DATEUPDATED value on the given <code>SolrDocument</code> that is no larger than <code>untilTimestamp</code>. Returns 0 if no
     * such value is found.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param untilTimestamp a long.
     * @return Latest DATEUPDATED value is less than or equals untilTimestamp on doc; 0 if none found.
     * @should return correct value
     * @should return 0 if no valid value is found
     */
    public static Long getLatestValidDateUpdated(SolrDocument doc, long untilTimestamp) {
        long ret = 0;
        Collection<Object> dateUpdatedValues = doc.getFieldValues(SolrConstants.DATEUPDATED);
        if (dateUpdatedValues != null && !dateUpdatedValues.isEmpty()) {
            // Get latest DATEUPDATED values
            for (Object o : dateUpdatedValues) {
                long dateUpdated = (Long) o;
                if (dateUpdated > ret && dateUpdated <= untilTimestamp) {
                    ret = dateUpdated;
                }
            }
        }

        return ret;
    }

    /**
     * <p>getFirstDoc.</p>
     *
     * @param query a {@link java.lang.String} object.
     * @param fieldList a {@link java.util.List} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @should return correct doc
     * @return a {@link org.apache.solr.common.SolrDocument} object.
     */
    public SolrDocument getFirstDoc(String query, List<String> fieldList) throws SolrServerException {
        logger.trace("getFirstDoc: {}", query);
        SolrDocumentList hits = search(query, 0, 1, null, fieldList, null).getResults();
        if (hits.getNumFound() > 0) {
            return hits.get(0);
        }

        return null;
    }

    /**
     * <p>getSingleFieldValue.</p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
     */
    public static Object getSingleFieldValue(SolrDocument doc, String field) {
        Collection<Object> valueList = doc.getFieldValues(field);
        if (valueList != null && !valueList.isEmpty()) {
            return valueList.iterator().next();
        }

        return null;
    }

    /**
     * <p>getSingleFieldStringValue.</p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getSingleFieldStringValue(SolrDocument doc, String field) {
        return (String) getSingleFieldValue(doc, field);
    }

    /**
     * Returns a list with all (string) values for the given field name in the given SolrDocument.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param fieldName a {@link java.lang.String} object.
     * @should return all values for the given field
     * @return a {@link java.util.List} object.
     */
    public static List<String> getMetadataValues(SolrDocument doc, String fieldName) {
        if (doc != null) {
            Collection<Object> values = doc.getFieldValues(fieldName);
            if (values != null) {
                List<String> ret = new ArrayList<>(values.size());
                for (Object value : values) {
                    if (value instanceof String) {
                        ret.add((String) value);
                    } else {
                        ret.add(String.valueOf(value));
                    }
                }
                return ret;
            }
        }

        return Collections.emptyList();
    }

    /**
     * <p>getFieldCount.</p>
     *
     * @param queryResponse a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @param field a {@link java.lang.String} object.
     * @return a long.
     */
    public static long getFieldCount(QueryResponse queryResponse, String field) {
        if (queryResponse == null) {
            throw new IllegalArgumentException("queryResponse may not be null");
        }
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }

        long ret = 0;
        FieldStatsInfo info = queryResponse.getFieldStatsInfo().get(field);
        if (info != null) {
            Object count = info.getCount();
            if (count instanceof Long || count instanceof Integer) {
                ret = (long) count;
            } else if (count instanceof Double) {
                ret = ((Double) count).longValue();
            }
            logger.trace("Total hits via {} value count: {}", field, ret);
        }

        return ret;
    }

    /**
     * <p>getAdditionalDocstructsQuerySuffix.</p>
     *
     * @should build query suffix correctly
     * @param additionalDocstructTypes a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getAdditionalDocstructsQuerySuffix(List<String> additionalDocstructTypes) {
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
     * <p>getUrnPrefixBlacklistSuffix.</p>
     *
     * @should build query suffix correctly
     * @param urnPrefixBlacklist a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getUrnPrefixBlacklistSuffix(List<String> urnPrefixBlacklist) {
        StringBuilder sbQuerySuffix = new StringBuilder();
        if (urnPrefixBlacklist != null && !urnPrefixBlacklist.isEmpty()) {
            int count = 0;
            for (String urnPrefix : urnPrefixBlacklist) {
                if (StringUtils.isNotBlank(urnPrefix)) {
                    urnPrefix = ClientUtils.escapeQueryChars(urnPrefix);
                    urnPrefix += '*';
                    sbQuerySuffix.append(" -")
                            .append("URN_UNTOKENIZED:")
                            .append(urnPrefix)
                            .append(" -")
                            .append("IMAGEURN_UNTOKENIZED:")
                            .append(urnPrefix)
                            .append(" -")
                            .append("IMAGEURN_OAI_UNTOKENIZED:")
                            .append(urnPrefix);
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

    /**
     * <p>getFulltextFileNames.</p>
     *
     * @param pi a {@link java.lang.String} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a {@link java.util.Map} object.
     */
    public Map<Integer, String> getFulltextFileNames(String pi) throws SolrServerException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append(SolrConstants.PI_TOPSTRUCT)
                .append(':')
                .append(pi)
                .append(" AND ")
                .append(SolrConstants.DOCTYPE)
                .append(":PAGE")
                .append(" AND ")
                .append(SolrConstants.FULLTEXTAVAILABLE)
                .append(":true")
                .append(" AND ")
                .append(SolrConstants.ACCESSCONDITION)
                .append(':')
                .append(SolrConstants.OPEN_ACCESS_VALUE);

        String[] fields = new String[] { SolrConstants.ORDER, SolrConstants.FILENAME_FULLTEXT, SolrConstants.FILENAME };
        QueryResponse qr = search(sbQuery.toString(), 0, MAX_HITS, Collections.singletonList(SolrConstants.ORDER), Arrays.asList(fields), null);
        if (!qr.getResults().isEmpty()) {
            Map<Integer, String> ret = new HashMap<>(qr.getResults().size());
            for (SolrDocument doc : qr.getResults()) {
                if (doc.containsKey(SolrConstants.FILENAME_FULLTEXT)) {
                    ret.put((int) doc.getFieldValue(SolrConstants.ORDER), (String) doc.getFieldValue(SolrConstants.FILENAME_FULLTEXT));
                } else if (doc.containsKey(SolrConstants.FILENAME)) {
                    String baseName = FilenameUtils.getBaseName((String) doc.getFieldValue(SolrConstants.FILENAME));
                    ret.put((int) doc.getFieldValue(SolrConstants.ORDER), baseName + ".txt");
                }
            }
            return ret;
        }

        return Collections.emptyMap();
    }
}
