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
import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.connector.AbstractTest;
import io.goobi.viewer.connector.utils.XmlConstants;

public class ErrorCodeTest extends AbstractTest {

    /**
     * @see ErrorCode#getBadArgument()
     * @verifies construct element correctly
     */
    @Test
    public void getBadArgument_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getBadArgument();
        Assert.assertNotNull(ele);
        Assert.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assert.assertEquals("badArgument", ele.getAttributeValue("code"));
        Assert.assertEquals(ErrorCode.BODY_BAD_ARGUMENT, ele.getText());
    }

    /**
     * @see ErrorCode#getBadResumptionToken()
     * @verifies construct element correctly
     */
    @Test
    public void getBadResumptionToken_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getBadResumptionToken();
        Assert.assertNotNull(ele);
        Assert.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assert.assertEquals("badResumptionToken", ele.getAttributeValue("code"));
        Assert.assertEquals(ErrorCode.BODY_BAD_RESUMPTION_TOKEN, ele.getText());
    }

    /**
     * @see ErrorCode#getBadVerb()
     * @verifies construct element correctly
     */
    @Test
    public void getBadVerb_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getBadVerb();
        Assert.assertNotNull(ele);
        Assert.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assert.assertEquals("badVerb", ele.getAttributeValue("code"));
        Assert.assertEquals(ErrorCode.BODY_BAD_VERB, ele.getText());
    }

    /**
     * @see ErrorCode#getCannotDisseminateFormat()
     * @verifies construct element correctly
     */
    @Test
    public void getCannotDisseminateFormat_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getCannotDisseminateFormat();
        Assert.assertNotNull(ele);
        Assert.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assert.assertEquals("cannotDisseminateFormat", ele.getAttributeValue("code"));
        Assert.assertEquals(ErrorCode.BODY_CANNOT_DISSEMINATE_FORMAT, ele.getText());
    }

    /**
     * @see ErrorCode#getIdDoesNotExist()
     * @verifies construct element correctly
     */
    @Test
    public void getIdDoesNotExist_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getIdDoesNotExist();
        Assert.assertNotNull(ele);
        Assert.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assert.assertEquals("idDoesNotExist", ele.getAttributeValue("code"));
        Assert.assertEquals(ErrorCode.BODY_ID_DOES_NOT_EXIST, ele.getText());
    }

    /**
     * @see ErrorCode#getNoMetadataFormats()
     * @verifies construct element correctly
     */
    @Test
    public void getNoMetadataFormats_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getNoMetadataFormats();
        Assert.assertNotNull(ele);
        Assert.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assert.assertEquals("noMetadataFormats", ele.getAttributeValue("code"));
        Assert.assertEquals(ErrorCode.BODY_NO_METADATA_FORMATS, ele.getText());
    }

    /**
     * @see ErrorCode#getNoRecordsMatch()
     * @verifies construct element correctly
     */
    @Test
    public void getNoRecordsMatch_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getNoRecordsMatch();
        Assert.assertNotNull(ele);
        Assert.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assert.assertEquals("noRecordsMatch", ele.getAttributeValue("code"));
        Assert.assertEquals(ErrorCode.BODY_NO_RECORDS_MATCH, ele.getText());
    }

    /**
     * @see ErrorCode#getNoSetHierarchy()
     * @verifies construct element correctly
     */
    @Test
    public void getNoSetHierarchy_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getNoSetHierarchy();
        Assert.assertNotNull(ele);
        Assert.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assert.assertEquals("noSetHierarchy", ele.getAttributeValue("code"));
        Assert.assertEquals(ErrorCode.BODY_NO_SET_HIERARCHY, ele.getText());
    }
}