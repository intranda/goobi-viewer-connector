/**
 * This file is part of the Goobi M2M Interface Suite - OAI-PMH and SRU interfaces for digital objects.
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

import javax.servlet.http.HttpServletRequest;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.intranda.digiverso.m2m.oai.enums.Metadata;
import de.intranda.digiverso.m2m.oai.enums.Verb;

public class RequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(RequestHandler.class);

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
     * @param request
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

    /** Empty constructor for XStream. */
    public RequestHandler() {
    }

    /**
     * @return the verb
     */
    public Verb getVerb() {
        return verb;
    }

    /**
     * @param verb the verb to set
     */
    public void setVerb(Verb verb) {
        this.verb = verb;
    }

    /**
     * @return the metadataPrefix
     */
    public Metadata getMetadataPrefix() {
        return metadataPrefix;
    }

    /**
     * @param metadataPrefix the metadataPrefix to set
     */
    public void setMetadataPrefix(Metadata metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * @param from the from to set
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * @return the until
     */
    public String getUntil() {
        return until;
    }

    /**
     * 
     * @param from
     * @return
     * @should convert date to timestamp correctly
     * @should set time to 000000 if none given
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
     * 
     * @param until
     * @return
     * @should convert date to timestamp correctly
     * @should set time to 235959 if none given
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
     * @param until the until to set
     */
    public void setUntil(String until) {
        this.until = until;
    }

    /**
     * @return the set
     */
    public String getSet() {
        return set;
    }

    /**
     * @param set the set to set
     */
    public void setSet(String set) {
        this.set = set;
    }
}
