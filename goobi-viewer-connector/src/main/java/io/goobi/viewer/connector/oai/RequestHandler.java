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
package io.goobi.viewer.connector.oai;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.enums.Verb;

/**
 * <p>RequestHandler class.</p>
 *
 */
public class RequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    /** Constant <code>formatter</code> */
    public static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMddHHmmss").withZoneUTC();

    @XStreamAlias("verb")
    private Verb verb = null;
    @XStreamAlias("metadataPrefix")
    private Metadata metadataPrefix = null;
    @XStreamAlias("identifier")
    private String identifier = null;
    @XStreamAlias("from")
    private String from = null;
    @XStreamAlias("until")
    private String until = null;
    @XStreamAlias("set")
    private String set = null;

    /**
     * handles the request in servlet
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public RequestHandler(HttpServletRequest request) {
        if (request.getParameter("verb") != null) {
            verb = Verb.getByTitle(request.getParameterValues("verb")[0]);
        }
        if (request.getParameter("metadataPrefix") != null) {
            metadataPrefix = Metadata.getByMetadataPrefix(request.getParameterValues("metadataPrefix")[0]);
        }
        if (request.getParameter("identifier") != null) {
            identifier = request.getParameterValues("identifier")[0];
        }
        if (request.getParameter("from") != null) {
            from = request.getParameterValues("from")[0];
        }
        if (request.getParameter("until") != null) {
            until = request.getParameterValues("until")[0];
        }
        if (request.getParameter("set") != null) {
            set = request.getParameterValues("set")[0];
        }

    }

    /**
     * Empty constructor for XStream.
     */
    public RequestHandler() {
    }

    /**
     * <p>Getter for the field <code>verb</code>.</p>
     *
     * @return the verb
     */
    public Verb getVerb() {
        return verb;
    }

    /**
     * <p>Setter for the field <code>verb</code>.</p>
     *
     * @param verb the verb to set
     */
    public void setVerb(Verb verb) {
        this.verb = verb;
    }

    /**
     * <p>Getter for the field <code>metadataPrefix</code>.</p>
     *
     * @return the metadataPrefix
     */
    public Metadata getMetadataPrefix() {
        return metadataPrefix;
    }

    /**
     * <p>Setter for the field <code>metadataPrefix</code>.</p>
     *
     * @param metadataPrefix the metadataPrefix to set
     */
    public void setMetadataPrefix(Metadata metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
    }

    /**
     * <p>Getter for the field <code>identifier</code>.</p>
     *
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * <p>Setter for the field <code>identifier</code>.</p>
     *
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * <p>Getter for the field <code>from</code>.</p>
     *
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * <p>Setter for the field <code>from</code>.</p>
     *
     * @param from the from to set
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * <p>Getter for the field <code>until</code>.</p>
     *
     * @return the until
     */
    public String getUntil() {
        return until;
    }

    /**
     * <p>getFromTimestamp.</p>
     *
     * @param from a {@link java.lang.String} object.
     * @should convert date to timestamp correctly
     * @should set time to 000000 if none given
     * @return a long.
     */
    public static long getFromTimestamp(String from) {
        if (from == null) {
            from = "19700101000000";
        } else {
            from = from.replace("-", "").replace("T", "").replace(":", "").replace("Z", "");
            if (from.length() == 8) {
                from = from + "000000";
            }
        }
        try {
            return formatter.parseDateTime(from).getMillis();
        } catch (IllegalArgumentException e) {
            logger.debug(e.getMessage());
        }

        return -1;
    }

    /**
     * <p>getUntilTimestamp.</p>
     *
     * @param until a {@link java.lang.String} object.
     * @should convert date to timestamp correctly
     * @should set time to 235959 if none given
     * @return a long.
     */
    public static long getUntilTimestamp(String until) {
        if (until == null) {
            until = "99991231235959";
        } else {
            until = until.replace("-", "").replace("T", "").replace(":", "").replace("Z", "");
            if (until.length() == 8) {
                until = until + "235959";
            }
        }
        try {
            long timestamp = formatter.parseDateTime(until).getMillis() + 999;
            return timestamp;
        } catch (IllegalArgumentException e) {
            logger.debug(e.getMessage());
        }

        return -1;
    }

    /**
     * <p>Setter for the field <code>until</code>.</p>
     *
     * @param until the until to set
     */
    public void setUntil(String until) {
        this.until = until;
    }

    /**
     * <p>Getter for the field <code>set</code>.</p>
     *
     * @return the set
     */
    public String getSet() {
        return set;
    }

    /**
     * <p>Setter for the field <code>set</code>.</p>
     *
     * @param set the set to set
     */
    public void setSet(String set) {
        this.set = set;
    }
}
