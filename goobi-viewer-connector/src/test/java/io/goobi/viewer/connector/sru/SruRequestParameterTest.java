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

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.oai.enums.Metadata;

public class SruRequestParameterTest {

    /**
     * @see SruRequestParameter#SruRequestParameter(SruOperation,String,String,int,int,String,Metadata,String,String,String,String,String,String,int,int)
     * @verifies set params correctly
     */
    @Test
    public void SruRequestParameter_shouldSetParamsCorrectly() throws Exception {
        SruRequestParameter parameter = new SruRequestParameter(SruOperation.SEARCHRETRIEVE, "1.2", "identifier=123", 1, 10, "recordPacking_value",
                Metadata.MARCXML, "recordXPath_value", "resultSetTTL_value", "sortKeys_value", "stylesheet_value", "extraRequestData_value",
                "scanClause_value", 5, 10);
        Assert.assertEquals(SruOperation.SEARCHRETRIEVE, parameter.getOperation());
        Assert.assertEquals("1.2", parameter.getVersion());
        Assert.assertEquals("identifier=123", parameter.getQuery());
        Assert.assertEquals(1, parameter.getStartRecord());
        Assert.assertEquals(10, parameter.getMaximumRecords());
        Assert.assertEquals("recordPacking_value", parameter.getRecordPacking());
        Assert.assertEquals(Metadata.MARCXML, parameter.getRecordSchema());
        Assert.assertEquals("recordXPath_value", parameter.getRecordXPath());
        Assert.assertEquals("resultSetTTL_value", parameter.getResultSetTTL());
        Assert.assertEquals("sortKeys_value", parameter.getSortKeys());
        Assert.assertEquals("stylesheet_value", parameter.getStylesheet());
        Assert.assertEquals("extraRequestData_value", parameter.getExtraRequestData());
        Assert.assertEquals("scanClause_value", parameter.getScanClause());
        Assert.assertEquals(5, parameter.getResponsePosition());
        Assert.assertEquals(10, parameter.getMaximumTerms());
    }
}