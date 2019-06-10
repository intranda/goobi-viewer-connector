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

/**
 * OAI metadata field configuration (e.g. for oai_dc).
 */
public class FieldConfiguration {

    private final String fieldName;
    private final String valueSource;
    private final boolean translate;
    private final boolean multivalued;
    private final boolean useTopstructValueIfNoneFound;
    private final String defaultValue;
    private final String prefix;
    private final String suffix;

    public FieldConfiguration(String fieldName, String valueSource, boolean translate, boolean multivalued, boolean useTopstructValueIfNoneFound,
            String defaultValue, String prefix, String suffix) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName may not be null");
        }

        this.fieldName = fieldName;
        this.valueSource = valueSource;
        this.translate = translate;
        this.multivalued = multivalued;
        this.useTopstructValueIfNoneFound = useTopstructValueIfNoneFound;
        this.defaultValue = defaultValue;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /**
     * @return the fieldName
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return the valueSource
     */
    public String getValueSource() {
        return valueSource;
    }

    /**
     * @return the translate
     */
    public boolean isTranslate() {
        return translate;
    }

    /**
     * @return the multivalued
     */
    public boolean isMultivalued() {
        return multivalued;
    }

    /**
     * @return the useTopstructValueIfNoneFound
     */
    public boolean isUseTopstructValueIfNoneFound() {
        return useTopstructValueIfNoneFound;
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return the suffix
     */
    public String getSuffix() {
        return suffix;
    }
}
