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
package io.goobi.viewer.connector.utils;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;

public enum FileFormat {
    
    
    UNKNOWN,
    METS,
    METS_MARC,
    LIDO,
    DENKXWEB,
    DUBLINCORE,
    CMS,
    ALTO,
    ABBYYXML,
    TEI;

    public static FileFormat getByName(String name) {
        if (name == null) {
            return UNKNOWN;
        }

        switch (name.toUpperCase()) {
            case "METS":
                return METS;
            case "METS_MARC":
                return METS_MARC;
            case "LIDO":
                return LIDO;
            case "DENKXWEB":
                return DENKXWEB;
            case "DUBLINCORE":
                return DUBLINCORE;
            case "CMS":
                return CMS;
            case "ABBYY":
            case "ABBYYXML":
                return ABBYYXML;
            case "TEI":
                return TEI;
            default:
                return UNKNOWN;
        }
    }
    
    /**
     * Determines the format of the given XML file by checking for namespaces.
     *
     * @param file a {@link java.io.File} object.
     * @return a {@link io.goobi.viewer.indexer.helper.JDomXP.FileFormat} object.
     * @throws java.io.IOException
     * @should detect mets mods files correctly
     * @should detect mets marc files correctly
     */
    public static FileFormat determineFileFormat(Element rootElement) throws IOException {
        if (rootElement == null) {
            return FileFormat.UNKNOWN;
        }
        try {

            if (rootElement.getNamespace("mets") != null) {
                List<Element> elements = evaluateToElementsStatic("mets:dmdSec/mets:mdWrap[@MDTYPE='MARC']",rootElement);
                if (elements != null && !elements.isEmpty()) {
                    return FileFormat.METS_MARC;
                }
                return FileFormat.METS;
            }
            if (rootElement.getNamespace("lido") != null) {
                return FileFormat.LIDO;
            }
            if (rootElement.getNamespace() != null
                    && rootElement.getNamespace().getURI().equals("http://denkxweb.de/")) {
                return FileFormat.DENKXWEB;
            }
            if (rootElement.getNamespace("dc") != null) {
                return FileFormat.DUBLINCORE;
            }
            if (rootElement.getNamespace().getURI().contains("abbyy")) {
                return FileFormat.ABBYYXML;
            }
            if (rootElement.getName().equals("TEI.2")) {
                return FileFormat.TEI;
            }
            if (rootElement.getName().equals("cmsPage")) {
                return FileFormat.CMS;
            }
        } catch (JDOMException e) {
        }

        return FileFormat.UNKNOWN;
    }
}
