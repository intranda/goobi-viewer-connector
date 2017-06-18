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
package de.intranda.digiverso.m2m.sru;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.cli.MissingArgumentException;

import de.intranda.digiverso.m2m.oai.enums.Metadata;

public class SruRequestParameter {

    // mandatory   The string: 'searchRetrieve'.
    private SruOperation operation;

    // mandatory    The version of the request, and a statement by the client that it wants the response to be less than, or preferably equal to, that version. See http://www.loc.gov/standards/sru/specs/common.html#version.
    private String version = "1.2";

    // mandatory in 'query' Contains a query expressed in CQL to be processed by the server. See http://www.loc.gov/standards/sru/specs/cql.html.
    private String query;

    // optional    The position within the sequence of matched records of the first record to be returned. The first position in the sequence is 1. The value supplied MUST be greater than 0. The default value if not supplied is 1.
    private int startRecord = 1;

    // optional     The number of records requested to be returned. The value must be 0 or greater. Default value if not supplied is determined by the server. The server MAY return less than this number of records, for example if there are fewer matching records than requested, but MUST NOT return more than this number of records. 
    private int maximumRecords = 100;

    // optional    A string to determine how the record should be escaped in the response. Defined values are 'string' and 'xml'. The default is 'xml'. See http://www.loc.gov/standards/sru/specs/search-retrieve.html#records. 
    private String recordPacking = "xml";

    // optional     The schema in which the records MUST be returned. The value is the URI identifier for the schema or the short name for it published by the server. The default value if not supplied is determined by the server. See http://www.loc.gov/standards/sru/resources/schemas.html.
    private Metadata recordSchema = Metadata.mets;

    // (version 1.1 only)  optional  An XPath expression, to be applied to the records before returning them. It is to be applied relative to the schema supplied in the recordSchema parameter, and response records should assume the SRU XPath schema. 
    private String recordXPath = "";

    //    optional    The number of seconds for which the client requests that the result set created should be maintained. The server MAY choose not to fulfil this request, and may respond with a different number of seconds. If resultSetTTL is not supplied then the server will determine the value. See http://www.loc.gov/standards/sru/specs/search-retrieve.html#resultsets.
    private String resultSetTTL = "";

    // (version 1.1 only)  optional    Contains a sequence of sort keys to be applied to the results.
    private String sortKeys = "";

    //    optional    A URL for a stylesheet. The client requests that the server simply return this URL in the response
    private String stylesheet = "";

    //    optional    Provides additional information for the server to process.
    private String extraRequestData = "";

    //  mandatory in 'scan'  The index to be browsed and the start point within it, expressed as a complete index, relation, term clause in CQL.
    private String scanClause = "";

    // optional  The position within the list of terms returned where the client would like the start term to occur. If the position given is 0, then the term should be immediately before the first term in the response. If the position given is 1, then the term should be first in the list, and so forth up to the number of terms requested plus 1, meaning that the term should be immediately after the last term in the response, even if the number of terms returned is less than the number requested. The range of values is 0 to the number of terms requested plus 1. The default value is 1. 
    private int responsePosition = 1;

    // optional  The number of terms which the client requests be returned. The actual number returned may be less than this, for example if the end of the term list is reached, but may not be more. The explain record for the database may indicate the maximum number of terms which the server will return at once. All positive integers are valid for this parameter. If not specified, the default is server determined.
    private int maximumTerms = 0;

    /**
     * @throws MissingArgumentException
     * 
     */
    public SruRequestParameter(HttpServletRequest request) throws MissingArgumentException {
        if (request.getParameter("operation") != null) {
            operation = SruOperation.getByTitle(request.getParameter("operation"));
        } else {
            throw new MissingArgumentException("Parameter 'operation' is mandatory.");
        }

        if (request.getParameter("version") != null) {
            version = request.getParameter("version");
        } else {
            throw new MissingArgumentException("Parameter 'version' is mandatory.");
        }

        if (request.getParameter("query") != null) {
            try {
                query = new String(request.getParameter("query").getBytes("iso-8859-1"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                query = request.getParameter("query");
            }
        }

        if (request.getParameter("startRecord") != null) {
            try {
                startRecord = Integer.parseInt(request.getParameter("startRecord"));
                if (startRecord == 0) {
                    startRecord = 1;
                }
            } catch (NumberFormatException e) {
                startRecord = 1;
            }
        } else {
            startRecord = 1;
        }

        if (request.getParameter("maximumRecords") != null) {
            try {
                maximumRecords = Integer.parseInt(request.getParameter("maximumRecords"));
                if (maximumRecords == 0) {
                    maximumRecords = 100;
                }
            } catch (NumberFormatException e) {
                maximumRecords = 100;
            }
        } else {
            maximumRecords = 100;
        }

        if (request.getParameter("recordPacking") != null) {
            recordPacking = request.getParameter("recordPacking");
        }

        if (request.getParameter("recordSchema") != null) {
            recordSchema = Metadata.getByMetadataPrefix(request.getParameter("recordSchema"));
        }

        if (request.getParameter("recordXPath") != null) {
            recordXPath = request.getParameter("recordXPath");
        }

        if (request.getParameter("resultSetTTL") != null) {
            resultSetTTL = request.getParameter("resultSetTTL");
        }

        if (request.getParameter("sortKeys") != null) {
            sortKeys = request.getParameter("sortKeys");
        }

        if (request.getParameter("stylesheet") != null) {
            stylesheet = request.getParameter("stylesheet");
        }

        if (request.getParameter("extraRequestData") != null) {
            extraRequestData = request.getParameter("extraRequestData");
        }

        if (request.getParameter("scanClause") != null) {
            scanClause = request.getParameter("scanClause");
        }

        if (request.getParameter("responsePosition") != null) {
            responsePosition = Integer.parseInt(request.getParameter("responsePosition"));
        }

        if (request.getParameter("maximumTerms") != null) {
            maximumTerms = Integer.parseInt(request.getParameter("maximumTerms"));
        }
    }

    public SruOperation getOperation() {
        return operation;
    }

    public String getVersion() {
        return version;
    }

    public String getQuery() {
        return query;
    }

    public int getStartRecord() {
        return startRecord;
    }

    public int getMaximumRecords() {
        return maximumRecords;
    }

    public String getRecordPacking() {
        return recordPacking;
    }

    public Metadata getRecordSchema() {
        return recordSchema;
    }

    public String getRecordXPath() {
        return recordXPath;
    }

    public String getResultSetTTL() {
        return resultSetTTL;
    }

    public String getSortKeys() {
        return sortKeys;
    }

    public String getStylesheet() {
        return stylesheet;
    }

    public String getExtraRequestData() {
        return extraRequestData;
    }

    /**
     * @return the scanClause
     */
    public String getScanClause() {
        return scanClause;
    }

    /**
     * @return the maximumTerms
     */
    public int getMaximumTerms() {
        return maximumTerms;
    }

    /**
     * @return the responsePosition
     */
    public int getResponsePosition() {
        return responsePosition;
    }

    @Override
    public String toString() {
        return "SruRequestParameter [operation=" + operation + ", version=" + version + ", query=" + query + ", startRecord=" + startRecord
                + ", maximumRecords=" + maximumRecords + ", recordPacking=" + recordPacking + ", recordSchema=" + recordSchema + ", recordXPath="
                + recordXPath + ", resultSetTTL=" + resultSetTTL + ", sortKeys=" + sortKeys + ", stylesheet=" + stylesheet + ", extraRequestData="
                + extraRequestData + ", scanClause=" + scanClause + ", responsePosition=" + responsePosition + ", maximumTerms=" + maximumTerms + "]";
    }

}
