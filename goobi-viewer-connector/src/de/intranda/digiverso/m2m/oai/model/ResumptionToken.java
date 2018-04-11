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
package de.intranda.digiverso.m2m.oai.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import de.intranda.digiverso.m2m.oai.RequestHandler;

@XStreamAlias("ResumptionToken")
public class ResumptionToken {

    @XStreamAsAttribute
    @XStreamAlias("tokenName")
    private String tokenName;
    /** Total (virtual) hit number as communicated the client. */
    @XStreamAlias("hits")
    private long hits;
    /** Total actual record number in the index. */
    @XStreamAlias("rawHits")
    private long rawHits;
    @XStreamAlias("cursor")
    private int virtualCursor;
    @XStreamAlias("rawCursor")
    private int rawCursor;
    @XStreamAlias("expirationDate")
    private long expirationDate;
    @XStreamAlias("handler")
    private RequestHandler handler;

    /**
     * creates a unique resumption token when number of hits is greater than list size
     * 
     * @param tokenName
     * @param hits
     * @param rawHits
     * @param virtualCursor
     * @param rawCursor
     * @param expirationDate
     * @param handler
     */
    public ResumptionToken(String tokenName, long hits, long rawHits, int virtualCursor, int rawCursor, long expirationDate, RequestHandler handler) {
        this.tokenName = tokenName;
        this.hits = hits;
        this.rawHits = rawHits;
        this.virtualCursor = virtualCursor;
        this.rawCursor = rawCursor;
        this.expirationDate = expirationDate;
        this.handler = handler;
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() > expirationDate;
    }

    /**
     * @param hits the hits to set
     */
    public void setHits(long hits) {
        this.hits = hits;
    }

    /**
     * @return the hits
     */
    public long getHits() {
        return hits;
    }

    /**
     * @return the rawHits
     */
    public long getRawHits() {
        return rawHits;
    }

    /**
     * @param rawHits the rawHits to set
     */
    public void setRawHits(long rawHits) {
        this.rawHits = rawHits;
    }

    /**
     * @return the virtualCursor
     */
    public int getVirtualCursor() {
        return virtualCursor;
    }

    /**
     * @param virtualCursor the virtualCursor to set
     */
    public void setVirtualCursor(int virtualCursor) {
        this.virtualCursor = virtualCursor;
    }

    /**
     * @return the rawCursor
     */
    public int getRawCursor() {
        return rawCursor;
    }

    /**
     * @param rawCursor the rawCursor to set
     */
    public void setRawCursor(int rawCursor) {
        this.rawCursor = rawCursor;
    }

    /**
     * @param expirationDate the expirationDate to set
     */
    public void setExpirationDate(long expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * @return the date
     */
    public long getExpirationDate() {
        return expirationDate;
    }

    /**
     * @param tokenName the tokenName to set
     */
    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    /**
     * @return the tokenName
     */
    public String getTokenName() {
        return tokenName;
    }

    /**
     * @param handler the handler to set
     */
    public void setHandler(RequestHandler handler) {
        this.handler = handler;
    }

    /**
     * @return the handler
     */
    public RequestHandler getHandler() {
        return handler;
    }

}
