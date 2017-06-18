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
package de.intranda.digiverso.m2m.sru;

public enum SruOperation {

    SCAN("scan"),
    EXPLAIN("explain"),
    SEARCHRETRIEVE("searchRetrieve"),
    UNSUPPORTETPARAMETER("unsupported")
    
    ;
    private String title;

    /**
     * 
     */
    private SruOperation(String title) {
        this.title = title;
    }
    
    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    public static SruOperation getByTitle(String title) {
        for (SruOperation operation : SruOperation.values()) {
            if (operation.getTitle().equalsIgnoreCase(title)) {
                return operation;
            }
        }
        
        return UNSUPPORTETPARAMETER;
    }
}
