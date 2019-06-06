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
package io.goobi.viewer.connector.sru;

import io.goobi.viewer.connector.utils.SolrConstants;

public enum SearchField {

    anywhere("any", null, "anywhere", "*", true, false, false),
    title("title", "dc.title", "title", SolrConstants.TITLE, true, false, true),
    creator("creator", "dc.creator", "per", SolrConstants.PERSON_ONEFIELD, true, false, false),
    collection("collection", "dc.subject", "collection", SolrConstants.DC, true, false, false),
    publisher("publisher", "dc.publisher", "publisher", SolrConstants.PUBLISHER, true, false, true),
    year("year", "dc.date", "year", SolrConstants.YEARPUBLISH, true, false, true),
    documentType("documentType", "dc.type", "type", SolrConstants.DOCSTRCT, true, false, false),
    format("format", "dc.format", null, null, false, false, false),
    URN("urn", "dc.identifier", "urn", SolrConstants.URN, true, false, true),
    identifier("identifier", "dc.identifier", "identifier", SolrConstants.PI, true, false, true);

    private String internalName;
    private String dcName;
    private String cqlName;
    private String solrName;
    boolean seachable;
    boolean scanable;
    boolean sortable;

    private SearchField(String internalName, String dcName, String cqlName, String solrName, boolean seachable, boolean scanable, boolean sortable) {
        this.internalName = internalName;
        this.dcName = dcName;
        this.cqlName = cqlName;
        this.solrName = solrName;
        this.seachable = seachable;
        this.scanable = scanable;
        this.sortable = sortable;
    }

    /**
     * @return the internalName
     */
    public String getInternalName() {
        return internalName;
    }

    /**
     * @return the dcName
     */
    public String getDcName() {
        return dcName;
    }

    /**
     * @return the cqlName
     */
    public String getCqlName() {
        return cqlName;
    }

    /**
     * @return the solrName
     */
    public String getSolrName() {
        return solrName;
    }

    /**
     * @return the seachable
     */
    public boolean isSeachable() {
        return seachable;
    }

    /**
     * @return the scanable
     */
    public boolean isScanable() {
        return scanable;
    }

    /**
     * @return the sortable
     */
    public boolean isSortable() {
        return sortable;
    }

    public static SearchField getFieldByDcName(String name) {
        for (SearchField field : SearchField.values()) {
            if (field.getDcName() != null && field.getDcName().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    public static SearchField getFieldByCqlName(String name) {
        for (SearchField field : SearchField.values()) {
            if (field.getCqlName() != null && field.getCqlName().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    public static SearchField getFieldBySolrName(String name) {
        for (SearchField field : SearchField.values()) {
            if (field.getSolrName() != null && field.getSolrName().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    public static SearchField getFieldByInternalName(String name) {
        for (SearchField field : SearchField.values()) {
            if (field.getInternalName().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

}
