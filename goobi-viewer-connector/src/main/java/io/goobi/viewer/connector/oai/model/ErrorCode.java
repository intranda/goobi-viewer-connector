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

import org.jdom2.Element;
import org.jdom2.Namespace;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.utils.XmlConstants;

/**
 * This class provides all error codes for the OAI protocol.
 */
public class ErrorCode {

    private Namespace xmlns = null;

    /**
     * <p>
     * Constructor for ErrorCode.
     * </p>
     */
    public ErrorCode() {
        xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
    }

    /**
     * required argument is missing, argument has wrong syntax or argument has invalid value
     *
     * @return a {@link org.jdom2.Element} object.
     */
    public Element getBadArgument() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "badArgument");
        error.setText(
                "The request includes illegal arguments, is missing required arguments, includes a repeated argument, or values for arguments have an illegal syntax.");
        return error;
    }

    /**
     * resumptionToken is expired, invalid, not found
     *
     * @return a {@link org.jdom2.Element} object.
     */
    public Element getBadResumptionToken() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "badResumptionToken");
        error.setText("The value of the resumptionToken argument is invalid or expired.");
        return error;
    }

    /**
     * the verb argument is missing or has invalid value
     *
     * @return a {@link org.jdom2.Element} object.
     */
    public Element getBadVerb() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "badVerb");
        error.setText("Value of the verb argument is not a legal OAI-PMH verb, the verb argument is missing, or the verb argument is repeated. ");
        return error;
    }

    /**
     * The metadataPrefix argument has invalid value
     *
     * @return a {@link org.jdom2.Element} object.
     */
    public Element getCannotDisseminateFormat() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "cannotDisseminateFormat");
        error.setText(
                "The metadata format identified by the value given for the metadataPrefix argument is not supported by the item or by the repository.");
        return error;
    }

    /**
     * identifier does not exist
     *
     * @return a {@link org.jdom2.Element} object.
     */
    public Element getIdDoesNotExist() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "idDoesNotExist");
        error.setText("The value of the identifier argument is unknown or illegal in this repository.");
        return error;
    }

    /**
     * query returns no hits
     *
     * @return a {@link org.jdom2.Element} object.
     */
    public Element getNoRecordsMatch() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "noRecordsMatch");
        error.setText("The combination of the values of the from, until, set and metadataPrefix arguments results in an empty list.");
        return error;
    }

    /**
     * for given identifier the metadata format used in metadataPrefix is not provided
     *
     * @return a {@link org.jdom2.Element} object.
     */
    public Element getNoMetadataFormats() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "noMetadataFormats");
        error.setText("There are no metadata formats available for the specified item.");
        return error;
    }

    /**
     * there are no sets in repository
     *
     * @return a {@link org.jdom2.Element} object.
     */
    public Element getNoSetHierarchy() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "noSetHierarchy");
        error.setText("The repository does not support sets.");
        return error;
    }
}
