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

import java.util.Calendar;

public class LicenseType {

    private final String field;
    private final String value;
    private final String conditions;

    public LicenseType(String field, String value, String conditions) {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }
        this.field = field;
        this.value = value;
        this.conditions = conditions;
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the conditions
     */
    public String getConditions() {
        return conditions;
    }

    public String getProcessedConditions() {
        String conditions = this.conditions;
        if (conditions.contains("NOW/YEAR") && !conditions.contains("DATE_")) {
            // Hack for getting the current year as a number for non-date Solr fields
            conditions = conditions.replace("NOW/YEAR", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        }

        return conditions;
    }

}
