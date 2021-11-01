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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.model.search.SearchHelper;

/**
 * <p>
 * SolrSearchIndex class.
 * </p>
 *
 */
public class SolrSearchIndex {

    /** Logger for this class. */
    final static Logger logger = LoggerFactory.getLogger(SolrSearchIndex.class);

    /** Constant <code>MAX_HITS=Integer.MAX_VALUE</code> */
    public static final int MAX_HITS = Integer.MAX_VALUE;
    private static final int TIMEOUT_SO = 300000;
    private static final int TIMEOUT_CONNECTION = 300000;
    private static final int RETRY_ATTEMPTS = 20;

    private SolrClient client;
    private final boolean testMode;

    /**
     * <p>
     * Constructor for SolrSearchIndex.
     * </p>
     *
     * @param client a {@link org.apache.solr.client.solrj.SolrServer} object.
     * @param testMode a boolean.
     */
    public SolrSearchIndex(SolrClient client, boolean testMode) {
        this.testMode = testMode;
        if (client == null) {
            if (StringUtils.isEmpty(DataManager.getInstance().getConfiguration().getIndexUrl())) {
                logger.error("Solr URL is not configured. Cannot instantiate the OAI-PMH interface.");
                return;
            }
            this.client = getNewHttpSolrClient(DataManager.getInstance().getConfiguration().getIndexUrl());
        } else {
            this.client = client;
        }
    }

    /**
     * Checks whether the server's configured URL matches that in the config file. If not, a new server instance is created.
     */
    public void checkReloadNeeded() {
        if (!testMode && client != null && client instanceof HttpSolrClient) {
            HttpSolrClient httpSolrServer = (HttpSolrClient) client;
            if (!DataManager.getInstance().getConfiguration().getIndexUrl().equals(httpSolrServer.getBaseURL())) {
                logger.info("Solr URL has changed, re-initializing SolrHelper...");
                try {
                    httpSolrServer.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
                client = getNewHttpSolrClient(DataManager.getInstance().getConfiguration().getIndexUrl());
            }
        }
    }

    /**
     * <p>
     * getNewHttpSolrServer.
     * </p>
     *
     * @param indexUrl a {@link java.lang.String} object.
     * @return a {@link org.apache.solr.client.solrj.impl.HttpSolrServer} object.
     */
    public static HttpSolrClient getNewHttpSolrClient(String indexUrl) {
        if (indexUrl == null) {
            throw new IllegalArgumentException("indexUrl may not be null");
        }
        logger.info("Initializing server with URL '{}'", indexUrl);
        HttpSolrClient server = new HttpSolrClient.Builder()
                .withBaseSolrUrl(indexUrl)
                .withSocketTimeout(TIMEOUT_SO)
                .withConnectionTimeout(TIMEOUT_CONNECTION)
                .allowCompression(true)
                .build();
        //        server.setDefaultMaxConnectionsPerHost(100);
        //        server.setMaxTotalConnections(100);
        server.setFollowRedirects(false); // defaults to false
        //        server.setMaxRetries(1); // defaults to 0. > 1 not recommended.
        server.setRequestWriter(new BinaryRequestWriter());
        // Backwards compatibility mode for Solr 4 servers
        //        if (DataManager.getInstance().getConfiguration().isSolrBackwardsCompatible()) {
        //            server.setParser(new XMLResponseParser());
        //        }

        return server;
    }

    /**
     * Attempts multiple times to query the Solr client with the given query.
     * 
     * @param solrQuery Readily built SolrQuery object
     * @param tries Number of attempts to query Solr before giving up
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    QueryResponse querySolr(SolrQuery solrQuery, int tries) throws SolrServerException, IOException {
        if (solrQuery == null) {
            throw new IllegalArgumentException("solrQuery may not be null");
        }

        while (tries > 0) {
            tries--;
            try {
                return client.query(solrQuery);
            } catch (SolrServerException e) {
                if (tries == 0 || !(e.getMessage().toLowerCase().contains("timeout") || e.getMessage().toLowerCase().contains("timed out"))) {
                    throw e;
                }
                logger.error(e.getMessage());
            }
        }

        return new QueryResponse();
    }

    /**
     * <p>
     * search.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a {@link org.apache.solr.common.SolrDocumentList} object.
     * @throws IOException
     */
    public SolrDocumentList search(String query, String filterQuerySuffix) throws SolrServerException, IOException {
        String finalQuery = query + filterQuerySuffix;
        if(!finalQuery.startsWith("+")) {
            finalQuery = "+" + finalQuery;
        }
        SolrQuery solrQuery = new SolrQuery(finalQuery);
        // logger.trace("search: {}", finalQuery);
        solrQuery.setRows(MAX_HITS);

        return querySolr(solrQuery, RETRY_ATTEMPTS).getResults();
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
     * @throws IOException
     * @should return correct results
     * @should return correct number of rows
     * @should sort results correctly
     */
    public QueryResponse search(String query, int first, int rows, List<String> sortFields, List<String> fieldList, Map<String, String> params)
            throws SolrServerException, IOException {
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
        if (params != null && !params.isEmpty()) {
            for (String key : params.keySet()) {
                solrQuery.set(key, params.get(key));
            }
        }

        return querySolr(solrQuery, RETRY_ATTEMPTS);
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
     * @param additionalQuery a {@link java.lang.String} object.
     * @param fieldList Optional list of fields to return.
     * @param fieldStatistics a {@link java.util.List} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return list of hits as {@link org.apache.solr.common.SolrDocument}
     * @throws java.io.IOException
     * @throws org.apache.solr.client.solrj.SolrServerException
     */
    public QueryResponse search(String from, String until, String setSpec, String metadataPrefix, int firstRow, int numRows, boolean urnOnly,
            String additionalQuery, String filterQuerySuffix, List<String> fieldList, List<String> fieldStatistics)
            throws IOException, SolrServerException {
        StringBuilder sbQuery = new StringBuilder(SolrSearchTools.buildQueryString(from, until, setSpec, metadataPrefix, urnOnly, additionalQuery));
        if (urnOnly) {
            sbQuery.append(" +(").append(SolrConstants.URN).append(":* ").append(SolrConstants.IMAGEURN_OAI).append(":*)");
        }
        sbQuery.append(filterQuerySuffix);
        logger.debug("OAI query: {}", sbQuery.toString());
        logger.trace("start: {}, rows: {}", firstRow, numRows);
        SolrQuery solrQuery = new SolrQuery(sbQuery.toString());
        solrQuery.setStart(firstRow);
        solrQuery.setRows(numRows);
        solrQuery.addSort(SolrConstants.DATECREATED, ORDER.asc);
        if (fieldList != null && !fieldList.isEmpty()) {
            for (String field : fieldList) {
                if (StringUtils.isNotEmpty(field)) {
                    solrQuery.addField(field);
                }
            }
        }
        if (fieldStatistics != null && !fieldStatistics.isEmpty()) {
            for (String field : fieldStatistics) {
                solrQuery.setGetFieldStatistics(field);
            }
        }

        QueryResponse resp = querySolr(solrQuery, RETRY_ATTEMPTS);
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
     * @param additionalQuery a {@link java.lang.String} object.
     * @param fieldList Optional list of fields to return.
     * @param fieldStatistics a {@link java.util.List} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     */
    public QueryResponse getListIdentifiers(Map<String, String> params, int firstRawRow, int numRows, String additionalQuery, List<String> fieldList,
            List<String> fieldStatistics, String filterQuerySuffix) throws SolrServerException {
        try {
            return search(params.get("from"), params.get("until"), params.get("set"), params.get("metadataPrefix"), firstRawRow, numRows, false,
                    additionalQuery, filterQuerySuffix, fieldList, fieldStatistics);
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
     * @param additionalQuery a {@link java.lang.String} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @param fieldList Optional list of fields to return.
     * @param fieldStatistics a {@link java.util.List} object.
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     */
    public QueryResponse getListRecords(Map<String, String> params, int firstRow, int numRows, boolean urnOnly, String additionalQuery,
            String filterQuerySuffix, List<String> fieldList, List<String> fieldStatistics) throws SolrServerException {
        try {
            return search(params.get("from"), params.get("until"), params.get("set"), params.get("metadataPrefix"), firstRow, numRows, urnOnly,
                    additionalQuery, filterQuerySuffix, fieldList, fieldStatistics);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Searches for identifier and return {@link org.apache.solr.common.SolrDocument} identifier can be PPN or URN (doc or page)
     *
     * @param identifier Identifier to search
     * @param fieldList Optional list of fields to return.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return {@link org.apache.solr.common.SolrDocument}
     * @throws java.io.IOException
     * @throws org.apache.solr.client.solrj.SolrServerException
     */
    public SolrDocument getListRecord(final String identifier, List<String> fieldList, String filterQuerySuffix)
            throws IOException, SolrServerException {
        logger.trace("getListRecord");
        SolrDocumentList ret = queryForIdentifier(identifier, 1, fieldList, filterQuerySuffix);
        if (!ret.isEmpty()) {
            return ret.get(0);
        }

        return null;
    }

    /**
     * <p>
     * isRecordExists.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a boolean.
     * @throws IOException
     */
    public boolean isRecordExists(final String identifier, String filterQuerySuffix) throws SolrServerException, IOException {
        return !queryForIdentifier(identifier, 0, null, filterQuerySuffix).isEmpty();
    }

    /**
     * 
     * @param identifier Record identifier.
     * @param rows Number of hits to return.
     * @param fieldList Optional list of fields to return.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    private SolrDocumentList queryForIdentifier(final String identifier, int rows, List<String> fieldList, String filterQuerySuffix)
            throws SolrServerException, IOException {
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
                .append(')')
                .append(filterQuerySuffix);
        logger.debug(sb.toString());
        SolrQuery solrQuery = new SolrQuery(sb.toString());
        solrQuery.setRows(rows);
        if (fieldList != null && !fieldList.isEmpty()) {
            for (String field : fieldList) {
                if (StringUtils.isNotEmpty(field)) {
                    solrQuery.addField(field);
                }
            }
        }

        return querySolr(solrQuery, RETRY_ATTEMPTS).getResults();
    }

    /**
     * Creates a list with all available values for the given field (minus any blacklisted values).
     *
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @should return all values
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws IOException
     */
    public List<String> getSets(String field) throws SolrServerException, IOException {
        List<String> ret = new ArrayList<>();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(field + ":* " + SearchHelper.getCollectionBlacklistFilterSuffix(field));
        solrQuery.setStart(0);
        solrQuery.setRows(0);
        solrQuery.addFacetField(field);
        logger.trace("Set query: {}", solrQuery.getQuery());
        QueryResponse resp = querySolr(solrQuery, RETRY_ATTEMPTS);

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
     * <p>
     * getTotalHitNumber.
     * </p>
     *
     * @param params a {@link java.util.Map} object.
     * @param urnOnly a boolean.
     * @param additionalQuery a {@link java.lang.String} object.
     * @param fieldStatistics a {@link java.util.List} object.
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return size of search
     * @throws java.io.IOException
     * @throws org.apache.solr.client.solrj.SolrServerException
     */
    public long getTotalHitNumber(Map<String, String> params, boolean urnOnly, String additionalQuery, List<String> fieldStatistics,
            String filterQuerySuffix) throws IOException, SolrServerException {
        StringBuilder sbQuery = new StringBuilder(SolrSearchTools.buildQueryString(params.get("from"), params.get("until"), params.get("set"),
                params.get("metadataPrefix"), urnOnly, additionalQuery));
        if (urnOnly) {
            sbQuery.append(" AND (").append(SolrConstants.URN).append(":* OR ").append(SolrConstants.IMAGEURN_OAI).append(":*)");
        }
        sbQuery.append(filterQuerySuffix);
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
        QueryResponse resp = querySolr(solrQuery, RETRY_ATTEMPTS);
        long num = resp.getResults().getNumFound();
        logger.debug("Total hits: {}", num);
        return num;
    }

    /**
     * <p>
     * getEarliestRecordDatestamp.
     * </p>
     *
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @return a {@link java.lang.String} object.
     * @throws org.apache.solr.client.solrj.SolrServerException if any.
     * @throws IOException
     */
    public String getEarliestRecordDatestamp(String filterQuerySuffix) throws SolrServerException, IOException {
        try {
            String searchStr = SolrConstants.ISWORK + ":true" + filterQuerySuffix;

            SolrQuery solrQuery = new SolrQuery(searchStr);
            solrQuery.setRows(1);
            solrQuery.addField(SolrConstants.DATECREATED);
            solrQuery.addSort(SolrConstants.DATECREATED, ORDER.asc);
            QueryResponse resp = querySolr(solrQuery, RETRY_ATTEMPTS);
            if (resp.getResults().size() > 0) {
                SolrDocument doc = resp.getResults().get(0);
                if (doc.getFieldValue(SolrConstants.DATECREATED) != null) {
                    LocalDateTime ldt = Instant.ofEpochMilli((Long) doc.getFieldValue(SolrConstants.DATECREATED))
                            .atZone(ZoneOffset.UTC)
                            .toLocalDateTime();
                    String yearMonthDay = ldt.format(Utils.formatterISO8601Date);
                    String hourMinuteSeconde = ldt.format(Utils.formatterISO8601Time);

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
     * @param request {@link HttpServletRequest} for filter query suffix generation
     * @param filterQuerySuffix Filter query suffix for the client's session
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a long.
     * @throws IOException
     */
    public long getLatestVolumeTimestamp(SolrDocument anchorDoc, long untilTimestamp, String filterQuerySuffix)
            throws SolrServerException, IOException {
        if (anchorDoc.getFieldValue(SolrConstants.ISANCHOR) == null || (Boolean) anchorDoc.getFieldValue(SolrConstants.ISANCHOR) == false) {
            return -1;
        }

        SolrDocumentList volumes = search(
                SolrConstants.ISWORK + ":true AND " + SolrConstants.IDDOC_PARENT + ":" + (String) anchorDoc.getFieldValue(SolrConstants.IDDOC),
                filterQuerySuffix);
        if (volumes == null) {
            return -1;
        }

        long latest = 0;
        for (SolrDocument volume : volumes) {
            long volumeTimestamp = SolrSearchTools.getLatestValidDateUpdated(volume, untilTimestamp);
            if (latest < volumeTimestamp) {
                latest = volumeTimestamp;
            }
        }

        if (latest > 0) {
            return latest;
        }

        return -1;

    }

    /**
     * <p>
     * getFirstDoc.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @param fieldList a {@link java.util.List} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @should return correct doc
     * @return a {@link org.apache.solr.common.SolrDocument} object.
     * @throws IOException
     */
    public SolrDocument getFirstDoc(String query, List<String> fieldList) throws SolrServerException, IOException {
        logger.trace("getFirstDoc: {}", query);
        SolrDocumentList hits = search(query, 0, 1, null, fieldList, null).getResults();
        if (hits.getNumFound() > 0) {
            return hits.get(0);
        }

        return null;
    }

    /**
     * <p>
     * getFulltextFileNames.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @throws org.apache.solr.client.solrj.SolrServerException
     * @return a {@link java.util.Map} object.
     * @throws IOException
     */
    public Map<Integer, String> getFulltextFileNames(String pi) throws SolrServerException, IOException {
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
