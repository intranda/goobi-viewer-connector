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
package io.goobi.viewer.connector.oai.model;

import java.util.ArrayList;
import java.util.List;

public class Set {

    private final String setName;
    private final String setSpec;
    private final String setQuery;
    private final List<String> values = new ArrayList<>();
    private boolean translate = false;

    public Set(String setName, String setSpec, String setQuery) {
        this.setName = setName;
        this.setSpec = setSpec;
        this.setQuery = setQuery;
    }

    public String getSetName() {
        return setName;
    }

    public String getSetSpec() {
        return setSpec;
    }

    public String getSetQuery() {
        return setQuery;
    }

    /**
     * @return the values
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * @return the translate
     */
    public boolean isTranslate() {
        return translate;
    }

    /**
     * @param translate the translate to set
     */
    public void setTranslate(boolean translate) {
        this.translate = translate;
    }

}
