/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
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
package io.goobi.viewer.connector.oai.model.language;

/**
 * <p>Language class.</p>
 *
 */
public class Language {

    private String isoCode;
    private String isoCodeOld;
    private String englishName;
    private String frenchName;
    private String germanName;

    /**
     * <p>Getter for the field <code>isoCode</code>.</p>
     *
     * @return the language code according to iso 639-2/B
     */
    public String getIsoCode() {
        return isoCode;
    }

    /**
     * <p>Setter for the field <code>isoCode</code>.</p>
     *
     * @param isoCode a {@link java.lang.String} object.
     */
    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    /**
     * <p>Getter for the field <code>isoCodeOld</code>.</p>
     *
     * @return the language code according to iso 639-1
     */
    public String getIsoCodeOld() {
        return isoCodeOld;
    }

    /**
     * <p>Setter for the field <code>isoCodeOld</code>.</p>
     *
     * @param isoCodeOld a {@link java.lang.String} object.
     */
    public void setIsoCodeOld(String isoCodeOld) {
        this.isoCodeOld = isoCodeOld;
    }

    /**
     * <p>Getter for the field <code>englishName</code>.</p>
     *
     * @return the englishName
     */
    public String getEnglishName() {
        return englishName;
    }

    /**
     * <p>Setter for the field <code>englishName</code>.</p>
     *
     * @param englishName the englishName to set
     */
    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    /**
     * <p>Getter for the field <code>frenchName</code>.</p>
     *
     * @return the frenchName
     */
    public String getFrenchName() {
        return frenchName;
    }

    /**
     * <p>Setter for the field <code>frenchName</code>.</p>
     *
     * @param frenchName the frenchName to set
     */
    public void setFrenchName(String frenchName) {
        this.frenchName = frenchName;
    }

    /**
     * <p>Getter for the field <code>germanName</code>.</p>
     *
     * @return the germanName
     */
    public String getGermanName() {
        return germanName;
    }

    /**
     * <p>Setter for the field <code>germanName</code>.</p>
     *
     * @param germanName the germanName to set
     */
    public void setGermanName(String germanName) {
        this.germanName = germanName;
    }

}
